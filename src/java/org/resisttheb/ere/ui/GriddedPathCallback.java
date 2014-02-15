package org.resisttheb.ere.ui;

import gnu.trove.TShortArrayList;

import java.util.Arrays;
import java.util.Comparator;

import org.resisttheb.ere.ui.PathCodecs.PathCallback;
import org.resisttheb.nug.NumericUtilities;

// this class is a buffer between a delegate path callback
// it accumulates coordinates into grids,;'
// and dumps each grid as a new polygon to the
// delegate
// this is one way to break up a large region into manageable
// chunks, e.g. for a tesselator
//
// to be memory tight,
// this callback can enter a histogramming mode
// for two pass population
//
// can we also just pre-allocate a big grid that is 
// re-used for everything?
// histo can be used with this -- histo tracks the max
// from each pass. so pass all ontours froma ll blobs up front,
// and track max
//
// NOTE: this implementation is twice as slow as possible
// and uses twice as much memory ... ya, we'll refine it
// if this idea pans out
public class GriddedPathCallback implements PathCallback {
	
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static boolean intersection(
		final float x0, final float y0,
		final float x1, final float y1,
		final float x2, final float y2,
		final float x3, final float y3,
		final float[] point
	) {
		final float dx21 = x1 - x0;
		final float dy21 = y1 - y0;
		final float d = ( y3 - y2 ) * dx21 - ( x3 - x2 ) * dy21;
		if ( NumericUtilities.fequal( d, 0.f ) )
			return false;
		final float
			ua = ( ( x3 - x2 ) * ( y0 - y2 ) - ( y3 - y2 ) * ( x0 - x2 ) ) / d;
		
		point[0] = x0 + ua * dx21;
		point[1] = y0 + ua * dy21;
		return true;
	}
	
	// 2bits of fractional
	private static final float M = 4;
	
	private static short packf1(final float a) {
		return (short) (a * M);
	}
	
	private static float unpackf1(final short s) {
		return (s & 0xFFFF) / (float) M;
	}
	
	private static int packf2(final float a, final float b) {
		return (packf1(a) & 0xFFFF) | ((packf1(b) & 0xFFFF) << 16);
	}
	
	private static void unpackf2(final int i, final float[] out) {
		out[0] = unpackf1((short) i);
		out[1] = unpackf1((short) (i >> 16));
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	private static PathCallback _lineToMoveToWrapper(final PathCallback callback) {
		return new PathCallback() {
			public void startPolygon(final int contourCount) {
				throw new UnsupportedOperationException();
			}
			
			public void endPolygon() {
				throw new UnsupportedOperationException();
			}
			
			public void startContour(final int lineCount) {
				throw new UnsupportedOperationException();
			}
			
			public void endContour() {
				throw new UnsupportedOperationException();
			}
			
			public void moveTo(final float x0, final float y0) {
				callback.moveTo(x0, y0);
			}
			
			public void lineTo(final float x, final float y) {
				callback.moveTo(x, y);
			}
		};
	}
	
	
	
	private static final short BOUNDARY_X_SENTINEL = Short.MIN_VALUE;
	
	
	// strategy:
	// 1. points in grid are either [x][y]
	//     or [SENTINEL_X][boundary id] if it's an entry or exit point
	//
	// 2. on reconstruction, scan points in grid.
	//    whenever we encounter a boundary, advance the BSQ
	//    and pull back the advanced point
	
	
	private final PathCallback delegate;
	
	private InternalPathCallback idelegate;
	
	private boolean histo;
	
	
	private final int xcount;
	private final int ycount;
	
	// note -- we flush after each contour
	// [x][y]
	private short[][][][] gpoints;
	private short[][] gindices;
	private short[][] maxgindices;
	private BoundarySquare[][] bsqs;
	
	private final float gxoff;
	private final float gyoff;
	private final float gw;
	private final float gh;
	
	private int contouri = 0;
	private int contourCount;
	
	
	private float px;
	private float py;
	
	
	public GriddedPathCallback(
			final PathCallback _delegate,
			final int _xcount, final int _ycount,
			final float _gxoff, final float _gyoff, 
			final float _gw, final float _gh
	) {
		this.delegate = _delegate;
		
		this.xcount = _xcount;
		this.ycount = _ycount;
		
		this.gxoff = _gxoff;
		this.gyoff = _gyoff;
		this.gw = _gw;
		this.gh = _gh;
		
		gpoints = new short[xcount][ycount][][];
		gindices = new short[xcount][ycount];
		bsqs = new BoundarySquare[xcount][ycount];
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				gindices[x][y] = 0;
				bsqs[x][y] = new BoundarySquare(gxoff + x * gw, gyoff + y * gh,
						gxoff + (x + 1) * gw, gyoff + (y + 1) * gh);
			}
		}
		
	}
	
	
	

	public void beginHistogramming() {
		histo = true;
		
		maxgindices = new short[xcount][ycount];
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				maxgindices[x][y] = 0;
			}
		}
		
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				bsqs[x][y].beginHistogramming();
			}
		}
	}
	
	public void endHistogramming() {
		// Alloc points based on max:
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				gpoints[x][y] = new short[maxgindices[x][y]][2];
			}
		}
		maxgindices = null;
		
		histo = false;
		
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				bsqs[x][y].endHistogramming();
			}
		}
	}
	
	
	
	private void reset() {
		if (histo) {
			// Merge max counts:
			for (int x = 0; x < xcount; ++x) {
				for (int y = 0; y < ycount; ++y) {
					if (gindices[x][y] > maxgindices[x][y]) {
						maxgindices[x][y] = gindices[x][y]; 
					}
				}
			}
			
		}
		
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				gindices[x][y] = 0;
				bsqs[x][y].reset();
			}
		}
	}
	
	private void flush() {
		
		
		// TODO:
		// scan the grid and mark even and odd
		
		// 1. do XNEG, XPOS first
		// base it on previous
		
		for (int x = 0; x < xcount; ++x) {
			bsqs[x][0].mark(BoundarySquare.Boundary.X_NEG, false);
			bsqs[x][0].mark(BoundarySquare.Boundary.X_POS, false);
		}
		for (int y = 1; y < ycount; ++y) {
			for (int x = 0; x < xcount; ++x) {
				bsqs[x][y].mark(BoundarySquare.Boundary.X_NEG, bsqs[x][y - 1].isOdd(BoundarySquare.Boundary.X_NEG));
				bsqs[x][y].mark(BoundarySquare.Boundary.X_POS, bsqs[x][y - 1].isOdd(BoundarySquare.Boundary.X_POS));
			}
		}
		
		for (int y = 0; y < ycount; ++y) {
			bsqs[0][y].mark(BoundarySquare.Boundary.Y_NEG, false);
			bsqs[0][y].mark(BoundarySquare.Boundary.Y_POS, false);
		}
		for (int x = 1; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				bsqs[x][y].mark(BoundarySquare.Boundary.Y_NEG, bsqs[x - 1][y].isOdd(BoundarySquare.Boundary.Y_NEG));
				bsqs[x][y].mark(BoundarySquare.Boundary.Y_POS, bsqs[x - 1][y].isOdd(BoundarySquare.Boundary.Y_POS));
			}
		}
		
		
		// for each grid,
		// 1. startPolygon(# of non null contours)
		// 2. for each non null, start contour, move to, line tos, end contour
		// 3. end poly
		
		for (int x = 0; x < xcount; ++x) {
			for (int y = 0; y < ycount; ++y) {
				
				flush(x, y);
			}
		}
	}
	
	private boolean flush(final int x, final int y) {
		if (histo) {
			return false;
		}
		
		idelegate.select(x, y);
		
		
		
		idelegate.startContour(-1);
		
		
		final int count = gindices[x][y];
		if (count <= 0) {
			// this just checks if each boundary is even
			// if every boundary is even, we are contained
			// also note, if we have no points, the
			// "evenness" of all boundaries should be equal
			final BoundarySquare bsq = bsqs[x][y];
			if (bsq.isContained()) {
				idelegate.moveTo(bsq.x0, bsq.y0);
				idelegate.lineTo(bsq.x1, bsq.y0);
				idelegate.lineTo(bsq.x1, bsq.y1);
				idelegate.lineTo(bsq.x0, bsq.y1);
				idelegate.lineTo(bsq.x0, bsq.y0);
			}
		}
		else {
			
		//bsqs[x][y]
		
		final BoundarySquare bsq = bsqs[x][y];
		bsq.order();
		bsq.prepareForPass();
		
		final short[][] points = gpoints[x][y];
		for (int i = 0; i < count; ++i) {
			final PathCallback useDelegate = 0 == i ? _lineToMoveToWrapper(idelegate)
					: idelegate;
			
			final short[] point = points[i];
			if (BOUNDARY_X_SENTINEL == point[0]) {
				bsq.advancePoints(BoundarySquare.boundaries[point[1]], 
						useDelegate);
			}
			else {
				useDelegate.lineTo(unpackf1(point[0]), unpackf1(point[1]));
			}
		}
		
		}
		
		
		idelegate.endContour();
		
		
		return true;
	}
	
	
	
	
	
	
	
	private void append(final float x, final float y) {
		// TODO: we need to track what edges we missed
		// keep an "active this contour" set of (xi, yi) pairs
		// when we re-enter, check which neighbors have been active
		// since our last entry index
		
		
		// need to track previous point
		// from (px, py) to (x, y)
		// what grids do we cross?
		
		
		// our alg:
		// find bounding box
		// for each square, find closest point on seg to midpoint
		// if in bounds, include the intersection point
		//
		// sort all points by projection onto seg, from start to end
		// walk the sub segs
		// and take 0 and 1 grids
		
		// TODO: switch to account for whether we're in histogramming mode
		
		
		
		
		
		
		final int xi = (int) ((x - gxoff) / gw);
		final int yi = (int) ((y - gyoff) / gh);
		
		if (0 == gindices[xi][yi]) {
			if (! histo) {
				gpoints[xi][yi][0][0] = packf1(x);
				gpoints[xi][yi][0][1] = packf1(y);
			}
			
			++gindices[xi][yi];
		}
		else {
		
		final int pxi = (int) ((px - gxoff) / gw);
		final int pyi = (int) ((py - gyoff) / gh);
		
		if (xi == pxi && yi == pyi) {
			final int index = gindices[xi][yi];
			if (! histo) {
				gpoints[xi][yi][index][0] = packf1(x);
				gpoints[xi][yi][index][1] = packf1(y);
			}
			
			++gindices[xi][yi];
		}
		else
		// If there's a difference of 1, then this is easier:
		if (1 == Math.abs(xi - pxi) + Math.abs(yi - pyi)) {
			final int index = gindices[xi][yi];
			final BoundarySquare.Boundary b = bsqs[xi][yi].add(px, py, x, y);
			if (! histo) {
				gpoints[xi][yi][index][0] = BOUNDARY_X_SENTINEL;
				gpoints[xi][yi][index][1] = (short) b.id;
			}
			++gindices[xi][yi];
			
			final int pindex = gindices[pxi][pyi];
			final BoundarySquare.Boundary pb = bsqs[pxi][pyi].add(px, py, x, y);
			if (! histo) {
				gpoints[pxi][pyi][pindex][0] = BOUNDARY_X_SENTINEL;
				gpoints[pxi][pyi][pindex][1] = (short) b.id;
			}
			++gindices[pxi][pyi];
		}
		else {
			// We're spanning more than grid ...
			
			// TODO: use alg. in notebook
			
			throw new IllegalStateException("TODO: implement me");
		}
		}
		
		px = x;
		py = y;
	}
	
	
	
	
	/**************************
	 * PATHCALLBACK IMPLEMENTATION
	 **************************/
	
	public void startPolygon(final int _contourCount) {
		if (! histo) {
			idelegate = new InternalPathCallback();
		}
		
		this.contourCount = _contourCount;
		contouri = 0;
		reset();
	}
	
	public void endPolygon() {
		if (! histo) {
		// Everything out:
		idelegate.flush();
		idelegate = null;
		}
	}
	
	public void startContour(final int lineCount) {
		// Reset all indices:
//		reset();
	}
	
	public void endContour() {
		flush();
		reset();
		++contouri;
	}
	
	public void moveTo(final float x0, final float y0) {
		append(x0, y0);
	}
	
	public void lineTo(final float x, final float y) {
		append(x, y);
	}
	
	/**************************
	 * END PATHCALLBACK IMPLEMENTATION
	 **************************/
	
	
	
	
	
	// TODO: need seg-seg intersection
	
	
	// tracks boundary points for a square
	// can sort them ccw
	// can also mark each point as even or odd
	// and generate all "in" segments
	// has xi, yi -- can generate bounding box from that
	private static final class BoundarySquare {
		public static enum Boundary {
			X_NEG(0),
			X_POS(1),
			Y_NEG(2),
			Y_POS(3);
			
			public final int id;
			private Boundary(final int _id) {
				this.id = _id;
			}
		}
		
		private static Boundary ccwBoundary(final Boundary boundary) {
			switch (boundary) {
				case X_NEG: return Boundary.Y_POS;
				case X_POS: return Boundary.Y_NEG;
				case Y_NEG: return Boundary.X_NEG;
				case Y_POS: return Boundary.X_POS;
				default:
					throw new IllegalArgumentException();
			}
		}
		
		private static Boundary cwBoundary(final Boundary boundary) {
			switch (boundary) {
				case Y_POS: return Boundary.X_NEG; 
				case Y_NEG: return Boundary.X_POS;
				case X_NEG: return Boundary.Y_NEG;
				case X_POS: return Boundary.Y_POS;
				default:
					throw new IllegalArgumentException();
			}
		}
		
		
		private static final Comparator<short[]> XNC = new Comparator<short[]>() {
			@Override
			public int compare(final short[] a, final short[] b) {
				return a[1] - b[1];
			}
		};
		private static final Comparator<short[]> XPC = new Comparator<short[]>() {
			@Override
			public int compare(final short[] a, final short[] b) {
				return b[1] - a[1];
			}
		};
		private static final Comparator<short[]> YNC = new Comparator<short[]>() {
			@Override
			public int compare(final short[] a, final short[] b) {
				return b[0] - a[0];
			}
		};
		private static final Comparator<short[]> YPC = new Comparator<short[]>() {
			@Override
			public int compare(final short[] a, final short[] b) {
				return a[0] - b[0];
			}
		};
		
		private static final Comparator<short[]>[] boundaryComparators =
			(Comparator<short[]>[]) new Comparator[]{XNC, XPC, YNC, YPC};
		
		private static Boundary[] boundaries = new Boundary[4];
		static {
			for (Boundary boundary : Boundary.values()) {
				boundaries[boundary.id] = boundary;
			}
		}
		
		
		private final float x0, y0, x1, y1;
		// if histogramming, store current in point0, max in points 1
		// points are packed [short][short]
//		private int[] points0;
//		private int[] points1;
//		private int[] points2;
//		private int[] points3;
//		private byte adv0, adv1, adv2, adv3;
		// [x, y, index]
		private short[][][] points = new short[4][][];
		private short[] adv = new short[4];
		
		private short[][][] spoints = new short[4][][];
		
		// TODO: etc
		private boolean[] odd = new boolean[4];
		
		private boolean[][] visited = new boolean[4][];
		
		private boolean histo = false;
		
		
		public BoundarySquare(final float _x0, final float _y0, final float _x1, final float _y1) {
			this.x0 = _x0;
			this.y0 = _y0;
			this.x1 = _x1;
			this.y1 = _y1;
		}
		
		
		/**************************
		 * HISTOGRAM MODE SWITCHING
		 **************************/
		
		public void beginHistogramming() {
			histo = true;
			points[0] = new short[][]{{0, 0, 0, 0}};
			points[1] = new short[][]{{0, 0, 0, 0}};
		}
		
		public void endHistogramming() {
			histo = false;
			// TODO: allocate internal structures from histogram
			
			// max counts are in points[1]
			final short[] counts = points[1][0];
			for (int i = 0; i < 4; ++i) {
				final int count = counts[i];
				points[i] = new short[count][3];
				visited[i] = new boolean[count];
				for (int j = 0; j < count; ++j) {
					visited[i][j] = false;
				}
			}
			
			// Initialize the indices:
			for (int i = 0; i < 4; ++i) {
				for (int j = 0; j < points[i].length; ++j) {
					points[i][j][2] = (short) j;
				}
			}
			
			for (int i = 0; i < 4; ++i) {
				adv[i] = 0;
				odd[i] = false;
			}
		}
		
		/**************************
		 * END HISTOGRAM MODE SWITCHING
		 **************************/
		
		
		private void push(final Boundary boundary, final float x, final float y) {
			if (histo) {
				++points[0][0][boundary.id];
				return;
			}
			
			final short[] store = points[boundary.id][adv[boundary.id]++];
			store[0] = packf1(x);
			store[1] = packf1(y);
		}
		
		
		
		public void reset() {
			if (histo) {
				// Merge points0 (current) into point1 (max)
				for (int i = 0; i < 4; ++i) {
					if (points[1][0][i] < points[0][0][i]) {
						points[1][0][i] = points[0][0][i];
					}
					points[0][0][i] = 0;
				}
				return;
			}
			
			// Zero the indices:
			prepareForPass();
		}
		
		
		public Boundary add(final float px0, final float py0, 
			final float px1, final float py1
		) {
			// find straddle
			// px0 < x0, x1 <= px1
			// same for y
			// 
			
			final int xs = px0 < x0 ? -1
					: x1 <= px1 ? 1 :
						px1 <= x1 ? -1 
						: x0 < px0 ? 1
							: 0;
			final int ys = py0 < y0 ? -1
					: y1 <= py1 ? 1 
						: py1 <= y1 ? -1
							: y0 < py0 ? 1
							: 0;
			
			// There are eight possible configurations:
			// (-1, -1) (-1, 0) (-1, 1) (0, -1)
			// (0, 1) (1, -1) (1, 0) (1, 1)
			
			// TODO: finish this
			// for corners, store on the edge that has the corner on 
			// ccw side
			
			Boundary boundary;
			final float[] point = new float[2];
			if (-1 == xs && -1 == ys) {
				// span XNEG or YNEG
				if (intersection(px0, py0, px1, py1, 
					x0, y0, x1, y0, point)
				) {
					push(boundary = Boundary.Y_NEG, point[0], point[1]);
				}
				else if (intersection(px0, py0, px1, py1, 
					x0, y0, x0, y1, point)
				) {
					push(boundary = Boundary.X_NEG, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (-1 == xs && 0 == ys) {
				// span XNEG
				if (intersection(px0, py0, px1, py1, 
					x0, y0, x0, y1, point)
				) {
					push(boundary = Boundary.X_NEG, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (-1 == xs && 1 == ys) {
				// span XNEG or YPOS
				if (intersection(px0, py0, px1, py1, 
					x0, y0, x0, y1, point)
				) {
					push(boundary = Boundary.X_NEG, point[0], point[1]);
				}
				else if (intersection(px0, py0, px1, py1, 
					x0, y1, x1, y1, point)
				) {
					push(boundary = Boundary.Y_POS, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (0 == xs && -1 == ys) {
				// span YNEG
				if (intersection(px0, py0, px1, py1, 
					x0, y0, x1, y0, point)
				) {
					push(boundary = Boundary.Y_NEG, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (0 == xs && 1 == ys) {
				// span YPOS
				if (intersection(px0, py0, px1, py1, 
					x0, y1, x1, y1, point)
				) {
					push(boundary = Boundary.Y_POS, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (1 == xs && -1 == ys) {
				// span XPOS or YNEG
				if (intersection(px0, py0, px1, py1, 
					x0, y0, x1, y0, point)
				) {
					push(boundary = Boundary.Y_NEG, point[0], point[1]);
				}
				else if (intersection(px0, py0, px1, py1, 
					x1, y0, x1, y1, point)
				) {
					push(boundary = Boundary.X_POS, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
				
			}
			else if (1 == xs && 0 == ys) {
				// span XPOS
				if (intersection(px0, py0, px1, py1, 
					x1, y0, x1, y1, point)
				) {
					push(boundary = Boundary.X_POS, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else if (1 == xs && 1 == ys) {
				// span XPOS or YPOS
				if (intersection(px0, py0, px1, py1, 
					x1, y0, x1, y1, point)
				) {
					push(boundary = Boundary.X_NEG, point[0], point[1]);
				}
				else if (intersection(px0, py0, px1, py1, 
					x0, y1, x1, y1, point)
				) {
					push(boundary = Boundary.Y_POS, point[0], point[1]);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
			else {
				throw new IllegalArgumentException();
			}
			
			return boundary;
		}
		
		
		// TODO: by construction, I think the points will be naturally
		// ordered, with just an offset
		public void order() {
			// TODO: sort each edge
			
			// XNEG:   sort Y ASC
			// XPOS:   sort Y DESC
			// YNEG:   sort X DESC
			// YPOS:   sort X ASC
			
			// TODO: sort. use an inline quick sort
			// that takes an (int, int) procedure and returns an int (comparison)
			
			for (int i = 0; i < 4; ++i) {
				final int len = points[i].length;
				spoints[i] = new short[len][];
				System.arraycopy(points[i], 0, spoints[i], 0, len);
				Arrays.sort(spoints[i], boundaryComparators[i]);
			}
		}
		
		public int count(final Boundary boundary) {
			return adv[boundary.id];
		}
		
		public void mark(final Boundary boundary, final boolean oddf) {
			// sets the odd flag on the first element for
			// the given boundary
			
			odd[boundary.id] = oddf;
		}
		
		
		public void prepareForPass() {
			// reset all indices
			
			for (int i = 0; i < 4; ++i) {
				adv[i] = 0;
			}
		}
		
		
		
		public boolean isOdd(final Boundary boundary) {
			return isOdd(boundary, count(boundary));
		}
		
		public boolean isOdd(final Boundary boundary, final int index) {
			return 1 == ((odd[boundary.id] ? 1 : 0) + index) % 2;
		}
		
		private void visit(final Boundary boundary) {
			visited[boundary.id][adv[boundary.id]] = true;
		}
		
		
		// move ccw on the given boundary
		public boolean advancePoints(final Boundary boundary, 
				final PathCallback callback
		) {
			// return 1 for has and not visited,
			// -1 for has and visited,
			// 0 for does not have
			// tr.boundary
			// tr.index
			visit(boundary);
			final int[] info = new int[3];
			boolean on = hasCcwOn(boundary, info);
			++adv[boundary.id];
			if (! on) {
				return false;
			}
			
			if (visited[info[0]][info[1]]) {
				walkCcw(boundary, info[2], boundaries[info[0]], info[1], callback);
			}
			else {
				walkCw(boundaries[info[0]], info[1], boundary, info[2], callback);
			}
			return true;
		}
		
		
		
		
		/**************************
		 * PATH WALKING
		 **************************/
		
		// walks counter-clockwise from "0" to "1"
		private void walkCcw(Boundary b0, int index0, 
				final Boundary b1, final int index1, final PathCallback callback
		) {
			do {
				++index0;
				while (spoints[b0.id].length <= index0) {
					ccwCorner(b0, callback);
					
					b0 = ccwBoundary(b0);
					index0 = 0;
				}
			}
			while (b0 != b1 || index0 != index1);
			callback.lineTo(unpackf1(spoints[b1.id][index1][0]), 
					unpackf1(spoints[b1.id][index1][0]));
		}
		
		// walks clockwise from "0" to "1"
		private void walkCw(Boundary b0, int index0, 
				final Boundary b1, final int index1, final PathCallback callback
		) {
			do {
				--index0;
				while (index0 < 0) {
					cwCorner(b0, callback);
					
					b0 = ccwBoundary(b0);
					index0 = spoints[b0.id].length - 1;
				}
			}
			while (b0 != b1 || index0 != index1);
			callback.lineTo(unpackf1(spoints[b1.id][index1][0]), 
					unpackf1(spoints[b1.id][index1][0]));
		}
		
		
		private void ccwCorner(final Boundary boundary, final PathCallback callback) {
			switch (boundary) {
				case X_NEG: callback.lineTo(x0, y1); break;
				case X_POS: callback.lineTo(x1, y0); break;
				case Y_NEG: callback.lineTo(x0, y0); break;
				case Y_POS: callback.lineTo(x1, y1); break;
			}
		}
		
		private void cwCorner(final Boundary boundary, final PathCallback callback) {
			switch (boundary) {
				case X_NEG: callback.lineTo(x0, y0); break;
				case X_POS: callback.lineTo(x1, y1); break;
				case Y_NEG: callback.lineTo(x1, y0); break;
				case Y_POS: callback.lineTo(x0, y1); break;
			}
		}
		
		
		// returns [boundary, index]
		private boolean hasCcwOn(Boundary boundary, final int[] info) {
			// 1. find current point on boundary
			//     if it is odd (ODD[b]?1:0 + index)%2, then we are not on an inside edge
			// 2. walk +1 (boundaries are sorted in ccw order)
			//    if cannot walk, advance ccw until we find an
			//    edge that has a point, then take it
			// 3.
			int index = Arrays.binarySearch(spoints[boundary.id], 
					points[boundary.id][adv[boundary.id]], 
					boundaryComparators[boundary.id]);
			
			info[2] = index;
			assert 0 <= index;
			
			if (isOdd(boundary, index)) {
				return false;
			}
			
			++index;
			while (spoints[boundary.id].length <= index) {
				index = 0;
				boundary = ccwBoundary(boundary);
			}
			info[0] = boundary.id;
			info[1] = index;
			
			return true;
		}
		
		
		
		/**************************
		 * END PATH WALKING
		 **************************/
		
		
		
		// short format:
		// [2 bit enum][12 bit integer][2 bit frac]
		// store points in short[] that is pre-allocated.
		// must do a histogramming pass
		// (set a bit in the callback if it's a histogramming pass)
		
		// API:
		// - add boundary point (x0, y0, x1, y1) RETURN ENUM -- finds intersection with edge and adds it
		// - order()   ccw order
		// - XXX don't need this    getBoundaryPoints(ENUM): return ids
		// - count(ENUM) -- scans list
		// - mark(ENUM, bool) -- says what the start is -- store four field bools
		// - XXX getPoint(id, float[])
		//   tests next point for enum
		// - advanceCcwPoints(ENUM, PathCallback) -- adds in ccw lines TOWARDS THE ID POINT if they exist
		
		
		public boolean isContained() {
			for (boolean oddi : odd) {
				if (! oddi) return false;
			}
			return true;
		}
	}
	
	
	
	
	
	
	
	
	
	
	// TODO: buffer points in an internal delegate
	// this stores a SHORT_LIST for each contour at each (x, y)
	//
	// it can then dump each grid individually
	// with the correct line counts, etc
	
	private class InternalPathCallback implements PathCallback {
		private TShortArrayList[][][] gpointLists;
		private int cx;
		private int cy;
		
		
		public InternalPathCallback() {
			reset();
		}
		
		
		public void select(final int _cx, final int _cy) {
			this.cx = _cx;
			this.cy = _cy;
		}
		
		
		public void reset() {
			gpointLists = new TShortArrayList[xcount][ycount][contourCount];
		}
		
		public void flush() {
			for (int x = 0; x < xcount; ++x) {
				for (int y = 0; y < ycount; ++y) {
					flush(x, y);
				}
			}
		}
		
		private void flush(final int x, final int y) {
			final TShortArrayList[] pointLists = gpointLists[x][y];
			
			int contoutCount = 0;
			for (TShortArrayList pointList : pointLists) {
				if (null == pointList || pointList.isEmpty()) {
					continue;
				}
				++contoutCount;
			}
			
			delegate.startPolygon(contoutCount);
			
			for (TShortArrayList pointList : pointLists) {
				if (null == pointList || pointList.isEmpty()) {
					continue;
				}
				
				delegate.startContour((pointList.size() - 2) / 2);
				final int len = pointList.size();
				delegate.moveTo(
						unpackf1(pointList.get(0)),
						unpackf1(pointList.get(1)));
				for (int i = 2; i < len; i += 2) {
					delegate.lineTo(
							unpackf1(pointList.get(i)),
							unpackf1(pointList.get(i + 1)));
				}
				delegate.endContour();
			}
			
			delegate.endPolygon();
		}
		
		
		
		private void append(final float x, final float y) {
			TShortArrayList list = gpointLists[cx][cy][contouri];
			if (null == list) {
				list = new TShortArrayList(32);
				gpointLists[cx][cy][contouri] = list;
			}
			list.add(packf1(x));
			list.add(packf1(y));
		}
		
		
		/**************************
		 * PATHCALLBACK IMPLEMENTATION
		 **************************/
		
		public void startPolygon(final int contourCount) {
			// Ignore
		}
		
		public void endPolygon() {
			// Ignore
		}
		
		public void startContour(final int lineCount) {
			if (null != gpointLists[cx][cy][contouri]) {
				gpointLists[cx][cy][contouri].clear();
			}
		}
		
		public void endContour() {
		}
		
		public void moveTo(final float x0, final float y0) {
			append(x0, y0);
		}
		
		public void lineTo(final float x, final float y) {
			append(x, y);
		}
		
		/**************************
		 * END PATHCALLBACK IMPLEMENTATION
		 **************************/
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main(final String[] in) {
		final PathCallback callback = new PathCallback() {

			@Override
			public void endContour() {
				
			}

			@Override
			public void endPolygon() {
				
			}

			@Override
			public void lineTo(float x, float y) {
				
			}

			@Override
			public void moveTo(float x0, float y0) {
				
			}

			@Override
			public void startContour(int lineCount) {
				
			}

			@Override
			public void startPolygon(int contourCount) {
				
			}
			
		};
		
		final GriddedPathCallback gpc = new GriddedPathCallback(callback,
				20, 20, 0, 0, 10, 10);
		
		gpc.beginHistogramming();
		pass(gpc);
		gpc.endHistogramming();
		pass(gpc);
	}
	
	
	private static void pass(final PathCallback callback) {
		callback.startPolygon(2);
		callback.startContour(2);
		callback.moveTo(0, 0);
		callback.lineTo(1, 1);
		callback.lineTo(2, 2);
		callback.endContour();
		callback.startContour(2);
		callback.moveTo(9, 9);
		callback.lineTo(10, 9);
		callback.lineTo(11, 9);
		callback.endContour();
		callback.endPolygon();
	}
}
