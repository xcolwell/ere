package org.resisttheb.ere.ui;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.BitSet;

import org.resisttheb.nug.NumericUtilities;

public class PathCodecs {
	
//	private static final float DEFAULT_EPS = 0.01f;
//	
//	private static boolean fequal(final float a, final float b) {
//		return fequal(a, b, DEFAULT_EPS);
//	}
//	
//	private static boolean fequal(final float a, final float b, final float eps) {
//		final float d = a - b;
//		return d < 0 ? -d <= eps : d <= eps;
//	}
	

	public static float[][] flatten(final GeneralPath path) {
		// count the number of points by counting move-tos,
		//    then *tos after that
		// pass 1: count movetos
		// pass 2: count length of each run
		// pass 3: populate points
		
		final float[] coords = new float[6];
		int count = 0;
		for (PathIterator itr = path.getPathIterator(null); !itr.isDone(); itr.next()) {
			
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					++count;
					break;
			}
		}
		
		if (count <= 0) {
			return new float[0][];
		}
		
		
		int i = 0;
		final float[][] allPoints = new float[count][];
		count = 0;
		
		for (PathIterator itr = path.getPathIterator(null); !itr.isDone(); itr.next()) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					if (0 < count) {
						allPoints[i] = new float[count << 1];
						++i;
					}
					count = 0;
					break;
				default:
					// Everything else, use one point:
					++count;
					break;
			}
		}
		allPoints[i] = new float[count << 1];
		
		i = 0;
		float[] points = null;
		int j = 0;
		int k;
		
		for (PathIterator itr = path.getPathIterator(null); !itr.isDone(); itr.next()) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					points = allPoints[i];
					++i;
					j = 0;
					break;
				case PathIterator.SEG_LINETO:
					k = j << 1;
					points[k] = coords[0];
					points[k + 1] = coords[1];
					++j;
					break;
				case PathIterator.SEG_QUADTO:
					k = j << 1;
					points[k] = coords[2];
					points[k + 1] = coords[3];
					++j;
					break;
				case PathIterator.SEG_CUBICTO:
					k = j << 1;
					points[k] = coords[4];
					points[k + 1] = coords[5];
					++j;
					break;
				case PathIterator.SEG_CLOSE:
					k = j << 1;
					points[k] = points[0];
					points[k + 1] = points[1];
					++j;
					break;
			}
		}
		
		return allPoints;
	}
	
	public static void unflatten(final float[][] allPoints, final GeneralPath path) {
		// the first point of each run is the moveto, the rest are linetos
		for (int i = 0; i < allPoints.length; ++i) {
			final float[] points = allPoints[i];
			path.moveTo(points[0], points[1]);
			for (int j = 1; j < points.length; ++j) {
				final int k = j << 1;
				path.lineTo(points[k], points[k + 1]);
			}
		}
	}
	
	
	
	// the "pack2" codec works for movements of at most +-1.
	// since all of our traces fall in this category, this is good
	// pack2 format stores in 1/20th the space of the full pack format
	
	public static int[][] pack2(final GeneralPath path) {
		// 1/2: histogram, then allocate
		// 3: each run has first int as abs int coords (16 bits each)
		//    then each next point is encoded in "-10+1" format,
		//    where we don't have an encoding for x:0and y:0
		//    we pack across int boundaries,
		//    and just keep packing
		//    3bits per movement, 
		
		
		// uses a bit vector, then dumps bit vector to ints
		
		final float[] coords = new float[6];
		int count 		= 0;
		int maxRun 		= 0;
		int run 		= 0;
		for (PathIterator itr = path.getPathIterator(null); !itr.isDone(); itr.next()) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					++count;
					if (maxRun < run) {
						maxRun = run;
					}
					run = 0;
					break;
				default:
					++run;
					break;
			}
		}
		
		if (count <= 0) {
			return new int[0][];
		}
		
		
		int i = 0;
		final int[][] allPacked = new int[count][];
		count = 0;
		
		
		int biti = 0;
		BitSet bits = new BitSet(3 * maxRun);
		float x0 = 0.f;
		float y0 = 0.f;
		float px = 0.f;
		float py = 0.f;
		for (PathIterator itr = path.getPathIterator(null); !itr.isDone(); itr.next()) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					if (0 < biti) {
						allPacked[i] = pack(bits, biti, x0, y0);
						biti = 0;
						bits.clear();
						++i;
					}
					
					x0 = px = coords[0];
					y0 = py = coords[1];
					
					break;
				case PathIterator.SEG_LINETO:
					bit(bits, biti, px, py, coords[0], coords[1]);
					biti += 3;
					px = coords[0];
					py = coords[1];
//					System.out.println("line to: " + px + ", " + py);
					break;
				case PathIterator.SEG_QUADTO:
					bit(bits, biti, px, py, coords[2], coords[3]);
					biti += 3;
					px = coords[2];
					py = coords[3];
					break;
				case PathIterator.SEG_CUBICTO:
					bit(bits, biti, px, py, coords[4], coords[5]);
					biti += 3;
					px = coords[4];
					py = coords[5];
					break;
				case PathIterator.SEG_CLOSE:
					bit(bits, biti, px, py, x0, y0);
					biti += 3;
					px = x0;
					py = y0;
					break;
			}
		}
		if (0 < biti) {
			allPacked[i] = pack(bits, biti, x0, y0);
		}
		
//		assert SELF_CHECK(allPacked, path);
		
		return allPacked;
	}
	
	
	private static void bit(final BitSet bits, final int biti, 
			final float px, final float py, final float x, final float y
	) {
		final float dx = x - px;
		final float dy = y - py;
		
//		if (!NumericUtilities.fequal(1.f, dx < 0 ? -dx : dx) && !NumericUtilities.fequal(0.f, dx)) {
//			throw new IllegalArgumentException("Movement must be +-1 or 0: " + dx);
//		}
//		if (!NumericUtilities.fequal(1.f, dy < 0 ? -dy : dy) && !NumericUtilities.fequal(0.f, dy)) {
//			throw new IllegalArgumentException("Movement must be +-1 or 0: " + dy);
//		}
		
		final int sx = NumericUtilities.fequal(0.f, dx) ? 0 : dx < 0 ? -1 : 1;
		final int sy = NumericUtilities.fequal(0.f, dy) ? 0 : dy < 0 ? -1 : 1;
		
		// sx, sy
		// -1, -1  000
		// -1, 0   001
		// -1, 1   010
		// 0, -1   011
		// 0, 1    100
		// 1, -1   101
		// 1, 0    110
		// 1, 1    111
		
		final int enc;
		if (-1 == sx && -1 == sy) {
			enc = 0;
		}
		else if (-1 == sx && 0 == sy) {
			enc = 1;
		}
		else if (-1 == sx && 1 == sy) {
			enc = 2;
		}
		else if (0 == sx && -1 == sy) {
			enc = 3;
		}
		else if (0 == sx && 1 == sy) {
			enc = 4;
		}
		else if (1 == sx && -1 == sy) {
			enc = 5;
		}
		else if (1 == sx && 0 == sy) {
			enc = 6;
		}
		else if (1 == sx && 1 == sy) {
			enc = 7;
		}
		else {
			throw new IllegalStateException();
		}
//		System.out.println(":   " + enc);
		bits.set(biti, 0x01 == (enc & 0x01));
		bits.set(biti + 1, 0x02 == (enc & 0x02));
		bits.set(biti + 2, 0x04 == (enc & 0x04));
	}
	
	
	private static int[] pack(final BitSet bits, final int biti, final float x0, final float y0) {
		final int header = ((short) x0) | (((short) y0) << 16);
		final int count = (int) Math.ceil(biti / 32.f);
		
		final int[] packed = new int[2 + count];
		packed[0] = biti / 3;
		packed[1] = header;
		
		for (int i = 0; i < count; ++i) {
			packed[i + 2] = pull(bits, biti, i * 32);
		}
		
		return packed;
	}
	
	private static int pull(final BitSet bits, final int biti, final int i) {
		int bitCount = biti - i;
		if (32 < bitCount)
			bitCount = 32;
		int out = 0;
		for (int j = 0; j < bitCount; ++j) {
			if (bits.get(i + j)) {
				out |= (0x01 << j);
			}
		}
		return out;
	}
	
	
	public static void unpack2(final int[][] allPacked, final GeneralPath path) {
		final PathCallback callback = new PathCallback() {
			@Override
			public void endContour() {
			}

			@Override
			public void startPolygon(final int contourCount) {
			}
			
			@Override
			public void endPolygon() {
			}

			@Override
			public void startContour(final int count) {
			}
			

			@Override
			public void moveTo(final float x0, final float y0) {
				path.moveTo(x0, y0);
			}
			
			@Override
			public void lineTo(final float x, final float y) {
				path.lineTo(x, y);
			}
		};
		
		unpack2(allPacked, callback);
	}
	
	public static void unpack2(final int[][] allPacked, final PathCallback callback) {
		callback.startPolygon(allPacked.length);
		
		for (int pi = 0; pi < allPacked.length; ++pi) {
			final int[] packed = allPacked[pi];
			final int count = packed[0];
			callback.startContour(count);
			final int header = packed[1];
			
			float px = header & 0xFFFF;
			float py = (header >> 16) & 0xFFFF;
			callback.moveTo(px, py);
			
			// expand to a bit set ...
			BitSet bits = new BitSet(3 * count);
			for (int k = 0, i = 2; i < packed.length; ++i) {
				int v = packed[i];
				for (int j = 0; j < 32 && k < 3 * count; ++j, ++k) {
					bits.set(k, 0x01 == ((v >> j) & 0x01));
				}
				
			}
			
			for (int i = 0; i < count; ++i) {
//				System.out.println(":   " + encode(bits, 3 * i));
				int dx; int dy;
				switch (encode(bits, 3 * i)) {
					case 0: dx = -1; dy = -1; break;
					case 1: dx = -1; dy = 0; break;
					case 2: dx = -1; dy = 1; break;
					case 3: dx = 0; dy = -1; break;
					case 4: dx = 0; dy = 1; break;
					case 5: dx = 1; dy = -1; break;
					case 6: dx = 1; dy = 0; break;
					case 7: dx = 1; dy = 1; break;
					default: 
						throw new IllegalStateException();
				}
				
				px += dx;
				py += dy;
				callback.lineTo(px, py);
//				System.out.println("line to: " + px + ", " + py);
			}
			
			callback.endContour();
		}
		callback.endPolygon();
	}
	
	private static int encode(final BitSet bits, final int k) {
		final boolean b0 = bits.get(k);
		final boolean b1 = bits.get(k + 1);
		final boolean b2 = bits.get(k + 2);
		
		return (b0 ? 0x01 : 0x00) | (b1 ? 0x02 : 0x00) | (b2 ? 0x04 : 0x00);
	}
	
	
	
	public static interface PathCallback {
		public void startPolygon(final int contourCount);
		public void endPolygon();
		public void startContour(final int lineCount);
		public void endContour();
		public void moveTo(final float x0, final float y0);
		public void lineTo(final float x, final float y);
	}
	
	
	
	
	
	// These check if our encodings are good:
	public static boolean SELF_CHECK(final int[][] packed, final GeneralPath path) {
		final GeneralPath path2 = new GeneralPath();
//		System.out.println("~~~~");
		PathCodecs.unpack2(packed, path2);
		return EQUALS(path, path2);
	}
	
	private static boolean EQUALS(final GeneralPath path0, final GeneralPath path1) {
		PathIterator itr0 = path0.getPathIterator(null);
		PathIterator itr1 = path1.getPathIterator(null);
		final float[] coords0 = new float[6];
		final float[] coords1 = new float[6];
		for (; !itr0.isDone() && !itr1.isDone(); itr0.next(), itr1.next()) {
			if (itr0.currentSegment(coords0)
					!= itr1.currentSegment(coords1)) {
				return false;
			}
			
			for (int i = 0; i < 6; ++i) {
				if (coords0[i] != coords1[i]) {
					return false;
				}
			}
		}
		if ( !itr0.isDone() || !itr1.isDone()) {
			return false;
		}
		return true;
	}
	
	
	
	
	private PathCodecs() {
	}
	
	
	
	
	public static void main(final String[] in) {
		final GeneralPath path = new GeneralPath();
		path.moveTo(10, 10);
		path.lineTo(11, 10);
		path.lineTo(11, 11);
		path.lineTo(12, 11);
		path.lineTo(13, 11);
		path.lineTo(14, 11);
		
		final int[][] allPacked = pack2(path);

		final PathCallback callback = new PathCallback() {
			public void startPolygon(final int contourCount) {
			}
			
			public void endPolygon() {
			}
			
			public void startContour(final int lineCount) {
			}
			
			public void endContour() {
			}
			
			public void moveTo(final float x0, final float y0) {
				System.out.println(x0 + ", " + y0);
			}
			
			public void lineTo(final float x, final float y) {
				System.out.println(x + ", " + y);
			}
		};
		
		unpack2(allPacked, callback);
	}
	
	
	
	
	
	
}
