package org.resisttheb.nug;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;



/**
 * Maintains <code>N</code> parallel curves.
 * 
 * <p>This class was designed to handle discontinuities. TODO: how? extension
 * Simple perpendicalar projection would break down with c1 discontinuity.</p>
 * 
 * TODO: this was removed
 * <p>This class treats new curve segments specially. Rendering a <code>RainbowsEdge</code>
 * via {@link #paint}, not {@link Graphics2D#draw}, will give fading effects for the new segments.</p> 
 */
// TODO: could the fading renderer be a wrapper instead?
public class RainbowsEdge /*extends Path2D.Float*/ {
	
	// TODO: a ghostAppend that does the extensions without actually appending anything (also does not increment m)
	
	
	// TODO: close codes are re-interpreted as linear movement to the last MOVETO point
	
	// TODO: a function to clean self-intersections
	
	
	// TODO:
	// TODO: to make up for gaps, do "parameter stretching", which stretches the control points along a line
	// TODO:  (lines are stretched to be lines)
	// TODO: close may stretch the beginning of the first segment (detect a close as the appending of a coordinate that equals the start coord)
	
	// segments that are to be appended but have prev. segments still in transition should be chained to the prev. segment
	
	public static Line2D.Float[] extendToJoint(final Line2D.Float a, final Line2D.Float b) {
		// 1. the (x2,y2) of the first line should be the intersection
		// 2. the (x1,y1) of the second line should be the intersection
		
		final Point2D.Float r = AspectRatioRhythm.intersection( a, b );
		return null == r
			? new Line2D.Float[ 0 ]
			: new Line2D.Float[]{
				new Line2D.Float( a.x1, a.y1, r.x, r.y ),
				new Line2D.Float( r.x, r.y, b.x2, b.y2 )
			};
	}
	
	
	public static void transform(final float[] coords, final int n, final AffineTransform at) {
		assert 0 == n % 2 : "Warning: the odd case (n = " + n + ") isn't implemented correctly.";
		final Point2D.Float p = new Point2D.Float();
		for ( int i = 0; i < n; i += 2 ) {
			p.setLocation( coords[ i ], coords[ i + 1 ] );
			at.transform( p, p );
			coords[ i ] = p.x;
			coords[ i + 1 ] = p.y;
		}
	}
	
	public static void transform(final Line2D.Float line, final AffineTransform at) {
		final float[] lineCoords = { line.x1, line.y1, line.x2, line.y2 };
		transform( lineCoords, 4, at );
		line.setLine( lineCoords[ 0 ], lineCoords[ 1 ], lineCoords[ 2 ], lineCoords[ 3 ] );
	}
	
	// shifts coordinates; coordinates are expected in alternating (x, y) format
	public static void shift(final float[] coords, final int n, final float dx, final float dy) {
		for ( int i = 0; i < n; i++ ) 
			coords[ i ] += 0 == i % 2 ? dx : dy; 
	}
	
	
	public static void scale(final float[] coords, final Line2D.Float base, final Line2D.Float ext) {
		scale( coords, coords.length, base, ext );
	}
	
	public static void scale(final float[] coords, final int n, final Line2D.Float base, final Line2D.Float ext) {
		scale( coords, n, base.x1, base.y1, base.x2, base.y2, ext.x1, ext.y1, ext.x2, ext.y2 );
	}
	
	public static void scale(
			final float[] coords, final int n, final float x1, final float y1, final float x2, final float y2, 
			final float tx1, final float ty1, final float tx2, final float ty2
	) {
		
		
		
		
		final float xa, xb, ya, yb;
		if ( NumericUtilities.fequal( x2, x1 ) ) {
			xa = 0.f;
			xb = 1.f;
		}
		else {
			xa = ( tx1 * x2 - x1 * tx2 ) / ( x2 - x1 );
			xb = ( tx2 - tx1 ) / ( x2 - x1 );
		}
		if ( NumericUtilities.fequal( y2, y1 ) ) {
			ya = 0.f;
			yb = 1.f;
		}
		else {
			ya = ( ty1 * y2 - y1 * ty2 ) / ( y2 - y1 );
			yb = ( ty2 - ty1 ) / ( y2 - y1 );
		}
		
		
//		System.out.println( "BEGIN SCALE (" + n + ") [" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ";  " + 
//				tx1 + ", " + ty1 + ", " + tx2 + ", " + ty2 + "; ]" );
		for ( int i = 0; i < n; i++ ) { 
//			final float preScaleCoord = coords[ i ];
			coords[ i ] = 0 == i % 2 ? xa + xb * coords[ i ] 
					: ya + yb * coords[ i ];
//			System.out.println( "    " + preScaleCoord + " -> " + coords[ i ] );
		}
//		System.out.println( "END SCALE" );
		
		
		if ( true )
			return;
		
		
		final float
		dx = x2 - x1,
		dy = y2 - y1,
		tdx = tx2 - tx1,
		tdy = ty2 - ty1;
	
	final float k = NumericUtilities.fsqrt( ( tdx * tdx + tdy * tdy ) / ( dx * dx + dy * dy ) );
	
	final float t = (float) Math.atan2( tdy, tdx );
	assert NumericUtilities.fequal( t, (float) Math.atan2( dy, dx ) );
	
	final AffineTransform at = new AffineTransform();
	at.rotate( -t );
	at.translate( -tx1, -ty1 );
	
	final AffineTransform iat = new AffineTransform();
	iat.translate( tx1, ty1 );
	iat.rotate( t );
	
	transform( coords, n, at );
	
//	final float[] xcoords = { x1, 0, x2, 0, tx1, 0, tx2, 0 };
//	transform( xcoords, 8, at );
	
	// Find the bounding box/
	// pinch around [xmin, xmax] and towards y=0
	
	
//	final float 
//		xcenter = ( tcoords[ 0 ] + tcoords[ 2 ] ) / 2.f,
//		ycenter = ( tcoords[ 1 ] + tcoords[ 3 ] ) / 2.f;
	
//	assert NumericUtilities.fequal( ycenter, 0.f );
	
//	final float xa, xb;
//	if ( NumericUtilities.fequal( xcoords[ 2 ], xcoords[ 0 ] ) ) {
//		xa = 0.f;
//		xb = 1.f;
//	}
//	else {
//		xa = ( xcoords[ 4 ] * xcoords[ 2 ] - xcoords[ 0 ] * xcoords[ 6 ] ) / ( xcoords[ 2 ] - xcoords[ 0 ] );
//		xb = ( xcoords[ 6 ] - xcoords[ 4 ] ) / ( xcoords[ 2 ] - xcoords[ 0 ] );
//	}
	
	for ( int i = 0; i < n; i += 2 ) {
//		coords[ i ] = xa + xb * coords[ i ];
		coords[ i + 1 ] = k * ( coords[ i + 1 ] );
	}
	
	transform( coords, n, iat );
		
	}
	
	
	
	
	private static float[] coords(final float ... coords) {
		return coords;
	}
	
	private static float[] coords(final double ... dcoords) {
		final float[] coords = new float[ dcoords.length ];
		for ( int i = 0; i < dcoords.length; i++ )
			coords[ i ] = (float) dcoords[ i ];
		return coords;
	}
	
	
	private static String toString(final Line2D.Float line) {
		return "[[ (" + line.x1 + ", " + line.y1 + ") :: (" + line.x2 + ", " + line.y2 + ") ]]";
	}
	
	
	
	// index 0 is always 0
	private final float[] offsets;
	
	private final float[] basep;
//	private final float[][] ps;
	
	
	private int[] segTypes;
	private float[][] baseCoords;
	private float[][][] pathCoords;
	
	// NO -- they are "lagging", or the lines closed by each new point
	// this should store the segement AFTER the target point, so the last seg is always null
	private Line2D.Float[][] segs;
	private int m = 0;
	
	private int runLength = 0;
	
	
	
	public RainbowsEdge(final float ... offsets) {
		if ( null == offsets )
			throw new IllegalArgumentException();
		
		final float[] copyOffsets = new float[ offsets.length ];
		System.arraycopy( offsets, 0, copyOffsets, 0, offsets.length );
		this.offsets = copyOffsets;
		
		basep = new float[ 2 ];
//		ps = new float[ offsets.length ][ 2 ];
		
		segTypes = new int[ 32 ];
		baseCoords = new float[ 32 ][];
		pathCoords = new float[ 32 ][ offsets.length ][];
		segs = new Line2D.Float[ 32 ][ offsets.length ];
	}
	
	
	
	protected void append(final int type, final float[] coords, final int n, final float tx, final float ty) {
		append( m, type, coords, n, tx, ty );
		m++;
	}
	
	
	private int[] assignSegType(int[] segTypes, final int k, final int type) {
		if ( segTypes.length <= k ) {
			// Grow:
			final int[] _segTypes = new int[ ( 1 + segTypes.length ) * 2 ];
			System.arraycopy( segTypes, 0, _segTypes, 0, segTypes.length );
			segTypes = _segTypes;
		}
		segTypes[ k ] = type;
		return segTypes;
	}
	
	private float[][] assignBaseCoords(float[][] baseCoords, final int k, final float[] coords) {
		if ( baseCoords.length <= k ) {
			// Grow:
			final float[][] _baseCoords = new float[ ( 1 + baseCoords.length ) * 2 ][];
			System.arraycopy( baseCoords, 0, _baseCoords, 0, baseCoords.length );
			baseCoords = _baseCoords;
		}
		baseCoords[ k ] = coords;
		return baseCoords;
	}
	
	private float[][][] assignPathCoords(float[][][] pathCoords, final int k, final int i, final float[] coords) {
		if ( pathCoords.length <= k ) {
			// Grow:
			final float[][][] _pathCoords = new float[ ( 1 + pathCoords.length ) * 2 ][ offsets.length ][];
			System.arraycopy( pathCoords, 0, _pathCoords, 0, pathCoords.length );
			pathCoords = _pathCoords;
		}
		pathCoords[ k ][ i ] = coords;
		return pathCoords;
	}
	
	private Line2D.Float[][] assignSeg(Line2D.Float[][] segs, final int k, final int i, final Line2D.Float seg) {
		if ( segs.length <= k ) {
			// Grow:
			final Line2D.Float[][] _segs = new Line2D.Float[ ( 1 + segs.length ) * 2 ][ offsets.length ];
			System.arraycopy( segs, 0, _segs, 0, segs.length );
			segs = _segs;
		}
		segs[ k ][ i ] = seg;
		return segs;
	}
	
	
	
	
	protected void append(final int m, final int type, float[] coords, final int n, final float tx, final float ty) {
		assert n <= coords.length;
		
		runLength = PathIterator.SEG_MOVETO == type ? 0 : runLength + 1;
		
//		segTypes[ m ] = type;
		segTypes = assignSegType( segTypes, m, type );
		
		{
			// Copy the coords:
			final float[] _coords = new float[ n ];
			System.arraycopy( coords, 0, _coords, 0, n );
			coords = _coords;
		}
		
//		baseCoords[ m ] = coords;
		baseCoords = assignBaseCoords( baseCoords, m, coords );
		
		final float dx, dy;
		if ( 0 == runLength ) {
			dx = dy = 0;
		}
		else {
			dx = tx - basep[ 0 ];
			dy = ty - basep[ 1 ];
		}
		final float mag = NumericUtilities.fsqrt( dx * dx + dy * dy );
		final float u, v;
		if ( NumericUtilities.fequal( 0, mag ) ) {
			u = v = 0.f;
		}
		else {
			u = -dy / mag;
			v = dx / mag;
		}
		
		final float[] offsetCoords = new float[ n ];
		for ( int i = 0; i < offsets.length; i++ ) {
			final float
				xoffset = u * offsets[ i ], 
				yoffset = v * offsets[ i ];
			
			if ( 1 == runLength ) {
				// Go back and adjust the move-to:
				shift( pathCoords[ m - 1 ][ i ], 2, xoffset, yoffset );
			}
			
			System.arraycopy( coords, 0, offsetCoords, 0, n );
			shift( offsetCoords, n, xoffset, yoffset );
			
			// want
			// - the previous centerline
			// - this centerline (basepx, basepy) + offsets -> (tx, ty) + offsets
			
			append( i, m, offsetCoords, n, basep[ 0 ] + xoffset, basep[ 1 ] + yoffset, 
					tx + xoffset, ty + yoffset );
			
			
		}
		
		
		basep[ 0 ] = tx;
		basep[ 1 ] = ty;
	}
	
	// i is the offset index
	protected void append(
			final int i, final int m, final float[] coords, final int n, final float px, final float py, 
			final float tx, final float ty
	) {
		// copy the coords
		// update the px,py
		
		
		final float[] copyCoords = new float[ n ];
		System.arraycopy( coords, 0, copyCoords, 0, n );
		pathCoords = assignPathCoords( pathCoords, m, i, copyCoords );
		
		segs = assignSeg( segs, m, i, new Line2D.Float( px, py, tx, ty ) );
		if ( 1 < runLength ) {
			// Extend the current segment:
			extendForward( i, 0, m + 1, m - 1 );
		}
		
//		ps[ i ][ 0 ] = tx;
//		ps[ i ][ 1 ] = ty;
	}
	
	
	protected void extendForward(final int bottomCut, final int topCut, final int k) {
		extendForward( bottomCut, topCut, k, SegAssoc.REVERSE, SegAssoc.REVERSE );
	}
	
	protected void extendForward(final int bottomCut, final int topCut, final int k, final SegAssoc sa0, final SegAssoc sa1) {
		for ( int i = 0; i < offsets.length; i++ ) {
			extendForward( i, bottomCut, topCut, k, sa0, sa1 );
		}
	}
	
	private static enum SegAssoc {
		FORWARD( 1 ), 
		REVERSE( 0 );
		
		
		private int offset;
		
		SegAssoc(final int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return offset;
		}
	}
	
	protected void extendForward(final int i, final int bottomCut, final int topCut, final int k) {
		extendForward( i, bottomCut, topCut, k, /* storage format */ SegAssoc.REVERSE, SegAssoc.REVERSE );
	}
	
	// k is the segment index
	protected void extendForward(final int i, final int bottomCut, final int topCut, final int k, final SegAssoc sa0, final SegAssoc sa1) {
		// look at k and k+1
		// if both types are LINE,CLOSE,CURVE then extend the lines, shift the coords in place
		
		
		final int 
			k0 = NumericUtilities.mod( k, bottomCut, topCut ), 
			k1 = NumericUtilities.mod( k + 1, bottomCut, topCut );
		final int 
			segk0 = k0 + sa0.getOffset(),
			segk1 = k1 + sa1.getOffset();
		if ( k0 != k1 ) {
			final Line2D.Float
				seg0 = segs[ segk0 ][ i ], 
				seg1 = segs[ segk1 ][ i ];
			final Line2D.Float[] xlines = extendToJoint( seg0, seg1 );
			if ( 2 == xlines.length ) {
				final Line2D.Float
					xseg0 = xlines[ 0 ],
					xseg1 = xlines[ 1 ];
				
				assert NumericUtilities.fequal( xseg0.x2, xseg1.x1 );
				assert NumericUtilities.fequal( xseg0.y2, xseg1.y1 );
				
//				System.out.println( "  [extendForward] align:    " +
//						"(" + xseg0.x2 + ", " + xseg0.y2 + ")  (" + xseg1.x1 + ", " + xseg1.y1 + ")" );
				
//				System.out.println( "  [extendForward] (" + i + ") " + toString( seg0 ) + " -> " + toString( xseg0 ) + 
//						";    " + toString( seg1 ) + " -> " + toString( xseg1 ) );
				
				scale( pathCoords[ k0 ][ i ], seg0, xseg0 );
				if ( segk0 != k0 ) {
					scale( pathCoords[ segk0 ][ i ], seg0, xseg0 );
				}
				scale( pathCoords[ k1 ][ i ], seg1, xseg1 );
				if ( segk1 != k1 ) {
					scale( pathCoords[ segk1 ][ i ], seg1, xseg1 );
				}

				segs[ segk0 ][ i ] = xseg0;
				segs[ segk1 ][ i ] = xseg1;
				
				
				boolean check = false;
//				assert check = true;
				if ( check ) {
					final float tol = 1.f;
					assert NumericUtilities.fequal( pathCoords[ k0 ][ i ][ pathCoords[ k0 ][ i ].length - 2 ], xseg0.x2, tol );
					assert NumericUtilities.fequal( pathCoords[ k0 ][ i ][ pathCoords[ k0 ][ i ].length - 1 ], xseg0.y2, tol );
					
					switch ( segTypes[ k1 ] ) {
						case PathIterator.SEG_MOVETO:
							assert NumericUtilities.fequal( pathCoords[ k1 ][ i ][ pathCoords[ k1 ][ i ].length - 2 ], xseg1.x1, tol );
							assert NumericUtilities.fequal( pathCoords[ k1 ][ i ][ pathCoords[ k1 ][ i ].length - 1 ], xseg1.y1, tol );
							break;
						default:
							assert NumericUtilities.fequal( pathCoords[ k1 ][ i ][ pathCoords[ k1 ][ i ].length - 2 ], xseg1.x2, tol );
							assert NumericUtilities.fequal( pathCoords[ k1 ][ i ][ pathCoords[ k1 ][ i ].length - 1 ], xseg1.y2, tol );
							break;
					}
				}
				
				
				
			}
		}
	}
	
	
	
	
//	
//	// affects all segments appended after the call
//	public void setEstablishTime() {
//		
//	}
//	
//	public void forceEstablish() {
//		
//	}
	
	

	
	
	public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
		append( PathIterator.SEG_CUBICTO, coords( x1, y1, x2, y2, x3, y3 ), 6, (float) x3, (float) y3 );		
	}

	public void lineTo(double x, double y) {
		// shift along the main diagonal -> all the shifted parts
		// for each
		//   - find intersection
		//   - scale previous section
		//   - scale this section
		
		// (there is a general form of this; we just need to know the dest x,y and the start x,y)
		
		append( PathIterator.SEG_LINETO, coords( x, y ), 2, (float) x, (float) y );
	}

	public void moveTo(double x, double y) {
		append( PathIterator.SEG_MOVETO, coords( x, y ), 2, (float) x, (float) y );
	}

	public void quadTo(double x1, double y1, double x2, double y2) {
		append( PathIterator.SEG_CUBICTO, coords( x1, y1, x2, y2 ), 4, (float) x2, (float) y2 );
	}
	
	// TODO: an intelligent close would check whether the two ends already
	// TODO: not append a line if the existing intersection would cause a shrink (indicates an existing overshoot)
	public void closePath() {
//		 find the first previous move-to;
		// LINETO that coord;
		// ALSO, extend from the new seg to the first seg
		
		if ( 1 <= runLength ) {
			final int k = m - 1 - runLength;
			
			boolean check = false;
			assert check = true;
			if ( check ) {
				int k0 = m - 1;
				for ( ; 0 <= k0; k0-- ) {
					if ( PathIterator.SEG_MOVETO == segTypes[ k0 ] ) {
						break;
					}
				}
				assert k == k0;
			}
			
			final float[] otherCoords = baseCoords[ k ];
//			append( PathIterator.SEG_LINETO, otherCoords, 2, otherCoords[ 0 ], otherCoords[ 1 ] );
//			extendForward( k, m, m - 1, SegAssoc.REVERSE, SegAssoc.FORWARD );
			extendForward( k, m, m - 1, SegAssoc.REVERSE, SegAssoc.FORWARD );
			
			// TODO: get the type in there somehow
//			append( PathIterator.SEG_CLOSE, otherCoords, 2, otherCoords[ 0 ], otherCoords[ 1 ] );
		}
	}


	public void append(final Shape shape, final boolean connect) {
		append( shape.getPathIterator( new AffineTransform() ), connect );
	}
	
	public void append(final PathIterator pi, boolean connect) {
		// CLOSE will lineto then extend the last-1 into the first (mod m-1)
		
		
		
		final float[] coords = new float[ 6 ];
		for ( ; !pi.isDone(); pi.next() ) {
			final int type = pi.currentSegment( coords );
			switch ( type ) {
				case PathIterator.SEG_CLOSE:
					closePath();
					break;
					
				case PathIterator.SEG_CUBICTO:
					append( type, coords, 6, coords[ 4 ], coords[ 5 ] );
					break;
				case PathIterator.SEG_LINETO:
					append( type, coords, 2, coords[ 0 ], coords[ 1 ] );
					break;
				case PathIterator.SEG_MOVETO:
					// TODO:
					/*
					if ( 0 < m && connect && 
							!NumericUtilities.fequal( coords[ 0 ], baseCoords[ m - 1 ][ 0 ] ) && 
							!NumericUtilities.fequal( coords[ 1 ], baseCoords[ m - 1 ][ 1 ] ) 
					) {
						append( PathIterator.SEG_LINETO, coords, 2, coords[ 0 ], coords[ 1 ] );
						connect = false;
					}
					*/
					if ( 0 < m && connect ) {
						connect = false;
					}
					else {
						append( type, coords, 2, coords[ 0 ], coords[ 1 ] );
					}
					
					break;
				case PathIterator.SEG_QUADTO:
					append( type, coords, 4, coords[ 2 ], coords[ 3 ] );
					break;
			}
		}
	}

	
	/*
	@Override
	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}
	*/
	
	
	public void transform(final AffineTransform at) {
		// TODO Auto-generated method stub
		// TODO: iterate over all coords, run through the transform
		
		for ( int k = 0; k < m; m++ ) {
			for ( int i = 0; i < offsets.length; i++ ) {
				final float[] coords = pathCoords[ k ][ i ];
				transform( coords, coords.length, at );
				transform( segs[ k ][ i ], at );
			}
		}
		
		transform( basep, basep.length, at );
//		for ( int i = 0; i < offsets.length; i++ ) {
//			final float[] p = ps[ i ];
//			transform( p, p.length, at );
//		}
	}

	
	public Rectangle2D getBounds2D() {
		final GeneralPath path = new GeneralPath();
		path.append( getPathIterator( new AffineTransform() ), false );
		return path.getBounds2D();
	}
	
	public Rectangle2D getBounds2D(final int k0, final int k1) {
		final GeneralPath path = new GeneralPath();
		path.append( getPathIterator( new AffineTransform(), k0, k1 ), false );
		return path.getBounds2D();
	}
	

	// TODO: each offset is iterated in full, then start the next
	public PathIterator getPathIterator(final AffineTransform at) {
		return getPathIterator( at, 0, m );
	}
	
	public PathIterator getPathIterator(final int i, final AffineTransform at) {
		return getPathIterator( i, at, 0, m );
	}
	
	// [k0, k1)
	public PathIterator getPathIterator(final AffineTransform at, final int k0, final int k1) {
		final PathIterator[] pitrs = getPathIterators( at, k0, k1 );
		return new PathIterator() {
			private int current = 0;
			
			public int getWindingRule() {
				return pitrs[ current ].getWindingRule();
			}

		    public boolean isDone() {
		    	while ( current < pitrs.length && pitrs[ current ].isDone() ) current++;
		    	return pitrs.length <= current;
		    }

		    public void next() {
		    	pitrs[ current ].next();
		    }

		    public int currentSegment(final float[] coords) {
		    	return pitrs[ current ].currentSegment( coords );
		    }

		    public int currentSegment(final double[] coords) {
		    	return pitrs[ current ].currentSegment( coords );
		    }
		};
	}
	
	
	
	public PathIterator[] getPathIterators(final AffineTransform at) {
		return getPathIterators( at, 0, m );
	}
	
	// [k0, k1)
	public PathIterator[] getPathIterators(final AffineTransform at, final int k0, final int k1) {
		final RainbowPathIterator[] pitrs = new RainbowPathIterator[ offsets.length /** 2*/ ];
		for ( int i = 0; i < pitrs.length; i += /*2*/ 1 ) {
			pitrs[ i ] = new RainbowPathIterator( k0, k1, i /*/ 2*/, segTypes, pathCoords, segs );
//			pitrs[ i + 1 ] = new RainbowPathIterator( k0, k1, i / 2, segTypes, pathCoords, segs );
//			pitrs[ i + 1 ].debug = true;
		}
		return pitrs;
	}
	
	public PathIterator getPathIterator(final int i, final AffineTransform at, final int k0, final int k1) {
		return new RainbowPathIterator( k0, k1, i, segTypes, pathCoords, segs );
	}
	
	
	
	private static Iterable<Point2D.Float> createControlPointIterable(final PathIterator pitr) {
		final Iterator<Point2D.Float> itr = new Iterator<Point2D.Float>() {
			private Queue<Point2D.Float> queue = new LinkedList<Point2D.Float>();
			private float[] coords = new float[ 6 ];
			
			private void pullNext() {
				if (! pitr.isDone() ) {
					switch ( pitr.currentSegment( coords ) ) {
						case PathIterator.SEG_CUBICTO:
							queue.add( new Point2D.Float( coords[ 4 ], coords[ 5 ] ) );
						case PathIterator.SEG_QUADTO:
							queue.add( new Point2D.Float( coords[ 2 ], coords[ 3 ] ) );
						case PathIterator.SEG_MOVETO:
						case PathIterator.SEG_LINETO:
							queue.add( new Point2D.Float( coords[ 0 ], coords[ 1 ] ) );
							break;
						default:
							break;
					}
					pitr.next();
				}
			}
			
			
			// =========================
			// <code>Iterator</code> implementation
			
			public boolean hasNext() {
				while ( !pitr.isDone() && queue.isEmpty() ) pullNext();
				return !queue.isEmpty();
			}
			
			public Point2D.Float next() {	
				return queue.isEmpty() ? null : queue.poll();
			}
			
			public void remove() {
				// Do nothing
			}
			
			// =========================
		};
		
		return new Iterable<Point2D.Float>() {
			public Iterator<Point2D.Float> iterator() {
				return itr;
			}
		};
	}
	
	public Iterable<Point2D.Float> getControlPointIterable() {
		return createControlPointIterable( getPathIterator( new AffineTransform() ) );
	}
	
	
	public GeneralPath export() {
		final GeneralPath path = new GeneralPath();
		export( path );
		return path;
	}
	
	public void export(final GeneralPath path) {
		path.append( getPathIterator( new AffineTransform() ), false );
//		path.append( new RainbowPathIterator( 0, m, 0, segTypes, pathCoords, segs ), false );
	}
	
	public GeneralPath export(final int ... is) {
		final GeneralPath path = new GeneralPath();
		export( path, is );
		return path;
	}
	
	public void export(final GeneralPath path, final int ... is) {
		for ( int i : is ) {
			path.append( getPathIterator( i, new AffineTransform() ), false );
		}
//		path.append( new RainbowPathIterator( 0, m, 0, segTypes, pathCoords, segs ), false );
	}
	
	
	
	private static class RainbowPathIterator implements PathIterator {
		private int i;
		private int[] segTypes;
		private float[][][] pathCoords;
		private Line2D.Float[][] segs;
		
		private int k;
		private int k1;
		
		public boolean debug = false;
		
		
		public RainbowPathIterator(
				final int k0, final int k1, final int i, final int[] segTypes, final float[][][] pathCoords, 
				// TEMP; TESTING
				final Line2D.Float[][] segs
		) {
			this.i = i;
			this.segTypes = segTypes;
			this.pathCoords = pathCoords;
			this.segs = segs;
			
			k = k0;
			this.k1 = k1;
		}

		// =========================
		// <code>PathIterator</code> implementation
		
		public int currentSegment(final double[] coords) {
			if (! debug ) {
				final float[] useCoords = pathCoords[ k ][ i ];
				for ( int j = 0; j < useCoords.length; j++ )
					coords[ j ] = useCoords[ j ];
				return segTypes[ k ];
			}
			else {
				final float[] fcoords = new float[ 6 ];
				final int type = currentSegment( fcoords );
				for ( int j = 0; j < coords.length; j++ )
					coords[ j ] = fcoords[ j ];
				return type;
			}
		}

		public int currentSegment(final float[] coords) {
			if (! debug ) {
				final float[] useCoords = pathCoords[ k ][ i ];
				System.arraycopy( useCoords, 0, coords, 0, useCoords.length );
				return segTypes[ k ];
			}
			else {
				// DEBUGGING:
				// iterate the center line segments instead of the path itself 
				switch ( segTypes[ k ] ) {
					case PathIterator.SEG_MOVETO:
					case PathIterator.SEG_CLOSE:
						coords[ 0 ] = pathCoords[ k ][ i ][ 0 ];
						coords[ 1 ] = pathCoords[ k ][ i ][ 1 ];
//						System.out.println( "    OPEN: (" + coords[ 0 ] + ", " + coords[ 1 ] + ")" );
						return segTypes[ k ];
					default:
						final Line2D.Float seg = segs[ k ][ i ];
						coords[ 0 ] = seg.x2;
						coords[ 1 ] = seg.y2;
//						System.out.println( "    LINE: (" + coords[ 0 ] + ", " + coords[ 1 ] + ")" );
						return PathIterator.SEG_LINETO;
				}
			}
		}

		public boolean isDone() {
			return k1 <= k;
		}

		public void next() {
			k++;
		}
		
		public int getWindingRule() {
			return PathIterator.WIND_EVEN_ODD;
		}
		
		// =========================
	}
	
	
	
	
	
	
	
	
	
	
	
	
	// TEST DRIVER
	
	
	public static void main(final String[] in) {

		
		
		
		
		
		final JComponent pathDisplay = new JPanel() {
			// each layer should fade out as it gets larger
			// new layers should be generated
			private int k = 0;
			
			@Override
			protected void paintComponent(final Graphics g) {

				super.paintComponent( g );
				
				final Graphics2D _g2d = (Graphics2D) g;
				final BufferedImage buffer = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB );
				final Graphics2D g2d2 = (Graphics2D) buffer.getGraphics();
				
//				final RainbowsEdge edge = new RainbowsEdge( 1, 5, 10 );
//				final RainbowsEdge edge = new RainbowsEdge( 0, -10, 10, 15, 20, 25, -15, -20, -25, -100 );
//				final RainbowsEdge edge = new RainbowsEdge( 0, 2, 4, 20, 30, 50, 100, -10, -15, -30 );
//				final RainbowsEdge edge = new RainbowsEdge( /*0,*/ /*-4, -8, -20,*/ -60, -20, 0, 5, 20, 15 + k/*++*/, 100, 200, 300, -50, -70, -90 );
//				final RainbowsEdge edge = new RainbowsEdge( 6, 4, 2, 0, -8, -16, -24, -32, -40, -48, -56, -64, -66, -68, -70 );
//				final RainbowsEdge edge = new RainbowsEdge( 0, 10, 20, -10, -20, -30 );
				final RainbowsEdge edge = new RainbowsEdge( 0, -3, -8, 12, -32 );
				
				final GeneralPath gpath = new GeneralPath();
				
				gpath.moveTo( 0, 0 );
				gpath.lineTo( 10, 10 );
				gpath.lineTo( 50, 100 );
				gpath.lineTo( 500, 100 );
				gpath.lineTo( 500, 500 );
				gpath.lineTo( 100, 500 );
				gpath.lineTo( 250, 250 );
				
				
//				edge.moveTo( 100, 100 );
//				edge.lineTo( 300, 100 );
//				edge.lineTo( 200, 300 );
//				edge.closePath();
//				edge.append( new Arc2D.Float( 100, 100, 200, 200, -(float) ( Math.PI / 2 ), (float) ( Math.PI / 2 ), Arc2D.OPEN ).getPathIterator( new AffineTransform() ), false );
//				edge.append( new Ellipse2D.Float( 200, 200, 200, 200 ).getPathIterator( new AffineTransform() ), false );
//				edge.append( new Arc2D.Float(100.0f, 100.0f, 200, 200, 210.0f, 130.0f, Arc2D.OPEN).getPathIterator( new AffineTransform() ), false );
//				edge.append( new Arc2D.Float(100.0f, 100.0f, 200, 200, 340.0f, 130.0f, Arc2D.OPEN).getPathIterator( new AffineTransform() ), true );
				
//				edge.append( NuhUi.getOpticbotFont().deriveFont( 200.f ).createGlyphVector( g2d.getFontRenderContext(), 
////						"Under Manilla Skies"
//						"hydrolous"
//						).getOutline( 200, 200 ).getPathIterator( new AffineTransform() ), false );
				
				/*
				edge.append( FontFactory.getPx10Font().deriveFont( 100.f ).createGlyphVector( g2d.getFontRenderContext(), 
//						"Under Manilla Skies"
						"enter & re-exit"
						).getOutline( 200, 200 ).getPathIterator( new AffineTransform() ), false );
				*/
				
//				edge.append( Utils.BAD_CLOUD_0, false );
				
				/*
				
				for ( int i = 0; i < 5; i++ ) {
//					gpath.append( new Arc2D.Double( 300 * i + 200 + 0, 200 + 0, 100, 100, 90, -90, Arc2D.OPEN ), true );
//					gpath.append( new Arc2D.Double( 300 * i + 200 + 100, 200 + -50, 200, 200, 180, 180, Arc2D.OPEN), true );
//					gpath.append( new Arc2D.Double( 300 * i + 200 + 300, 200 + 0, 100, 100, 180, -90, Arc2D.OPEN), true );
					
					gpath.append( new Arc2D.Double( 500 * i + 200 + 0, 200 + 0, 100, 100, 90, -90, Arc2D.OPEN ), true );
					gpath.append( new Arc2D.Double( 500 * i + 200 + 100, 100 + -50, 400, 400, 180, 180, Arc2D.OPEN), false );
					gpath.append( new Arc2D.Double( 500 * i + 200 + 500, 200 + 0, 100, 100, 180, -90, Arc2D.OPEN), false );
				}
				*/
				

				final AffineTransform at = AffineTransform.getScaleInstance(0.3f, 0.3f);
				at.translate(200, 200);
				edge.append( at.createTransformedShape(gpath), false );
				
				for (Graphics2D g2d : new Graphics2D[]{ _g2d, g2d2 }) {
					g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
					
					
					{
						final GeneralPath path = edge.export( 1 );
						
						
						g2d.setPaint( new Color( 230, 69, 2, 120 ) );
	//					g2d.setStroke( new BubbleStroke().setThickness( 2.f ) );
						g2d.draw( path );
					}
					
					{
						final GeneralPath path = edge.export( 2 );
						
						
						g2d.setPaint( new Color( 234, 209, 2, 70 ) );
	//					g2d.setStroke( new BubbleStroke().setThickness( 1.f ) );
						g2d.draw( path );
					}
					
					{
						final GeneralPath path = edge.export( 0 );
						
						
	//					g2d.setPaint( new Color( 234, 75, 2, 240 ) );
	//					g2d.setPaint( new Color( 234, 130, 2, 240 ) );
	//					g2d.setPaint( new Color( 208, 7, 232, 240 ) );
	//					g2d.setPaint( new Color( 154, 78, 163, 240 ) );
						g2d.setPaint( new Color( 166, 166, 227, 240 ) );
	//					g2d.setStroke( new BubbleStroke().setSlack( 2.f ).setThickness( 3.5f ) );
						g2d.draw( path );
					}
					
					{
						final GeneralPath path = edge.export( 3 );
						
						
	//					g2d.setPaint( new Color( 234, 75, 2, 240 ) );
						g2d.setPaint( new Color( 234, 130, 2, 240 ) );
	//					g2d.setPaint( new Color( 208, 7, 232, 240 ) );
	//					g2d.setPaint( new Color( 154, 78, 163, 240 ) );
	//					g2d.setPaint( new Color( 166, 166, 227, 240 ) );
	//					g2d.setStroke( new BubbleStroke().setSlack( 2.f ).setThickness( 3.5f ) );
						
						final Stroke tstroke = g2d.getStroke();
						g2d.setStroke(new BasicStroke(2.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						g2d.draw( path );
						g2d.setStroke(tstroke);
					}
					
					{
						final GeneralPath path = edge.export( 4 );
						
						
	//					g2d.setPaint( new Color( 234, 75, 2, 240 ) );
						g2d.setPaint( new Color( 234, 130, 2, 240 ) );
	//					g2d.setPaint( new Color( 208, 7, 232, 240 ) );
	//					g2d.setPaint( new Color( 154, 78, 163, 240 ) );
	//					g2d.setPaint( new Color( 166, 166, 227, 240 ) );
	//					g2d.setStroke( new BubbleStroke().setSlack( 2.f ).setThickness( 3.5f ) );
						
						final Stroke tstroke = g2d.getStroke();
						g2d.setStroke(new BasicStroke(4.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						g2d.draw( path );
						g2d.setStroke(tstroke);
					}
				}
				
				
				
//				g2d2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
//				
//				
//				{
//					final GeneralPath path = edge.export( 1 );
//					
//					
//					g2d2.setPaint( new Color( 230, 69, 2, 120 ) );
////					g2d2.setStroke( new BubbleStroke().setThickness( 2.f ) );
//					g2d2.draw( path );
//				}
//				
//				{
//					final GeneralPath path = edge.export( 2 );
//					
//					
//					g2d2.setPaint( new Color( 234, 209, 2, 70 ) );
////					g2d2.setStroke( new BubbleStroke().setThickness( 1.f ) );
//					g2d2.draw( path );
//				}
//				
//				{
//					final GeneralPath path = edge.export( 0 );
//					
//					
////					g2d.setPaint( new Color( 234, 75, 2, 240 ) );
////					g2d.setPaint( new Color( 234, 130, 2, 240 ) );
////					g2d.setPaint( new Color( 208, 7, 232, 240 ) );
////					g2d.setPaint( new Color( 154, 78, 163, 240 ) );
//					g2d2.setPaint( new Color( 166, 166, 227, 240 ) );
////					g2d2.setStroke( new BubbleStroke().setSlack( 2.f ).setThickness( 3.5f ) );
//					g2d2.draw( path );
//				}
//				
//				{
//					final GeneralPath path = edge.export( 3 );
//					
//					
////					g2d.setPaint( new Color( 234, 75, 2, 240 ) );
//					g2d2.setPaint( new Color( 234, 130, 2, 240 ) );
////					g2d.setPaint( new Color( 208, 7, 232, 240 ) );
////					g2d.setPaint( new Color( 154, 78, 163, 240 ) );
////					g2d.setPaint( new Color( 166, 166, 227, 240 ) );
////					g2d.setStroke( new BubbleStroke().setSlack( 2.f ).setThickness( 3.5f ) );
//					
//					final Stroke tstroke = g2d.getStroke();
//					g2d2.setStroke(new BasicStroke(2.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//					g2d2.draw( path );
//					g2d2.setStroke(tstroke);
//				}
				
				/*
				g2d.setPaint( Color.BLUE );
				for ( Point2D.Float point : edge.getControlPointIterable() ) {
					g2d.draw( new Rectangle2D.Float( point.x - 1, point.y - 1, 2, 2 ) );
				}
				*/
				
//				g2d.draw( new Arc2D.Double( 200 + 0, 200 + 0, 100, 100, 90, -90, Arc2D.OPEN ) );
//				g2d.draw( new Arc2D.Double( 200 + 100, 200 + -150, 400, 400, 180, 180, Arc2D.OPEN) );
//				g2d.draw( new Arc2D.Double( 200 + 500, 200 + 0, 100, 100, 180, -90, Arc2D.OPEN) );
				
				try {
					ImageIO.write( buffer, "png", new File( "c:/temp/capture.png" ) );
				}
				catch ( IOException e ) {
					
				}
			}
		};
		pathDisplay.setBackground( Color.WHITE );
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout( new BorderLayout() );
		c.add( pathDisplay, BorderLayout.CENTER );
		
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 500, 500 );
		frame.setVisible( true );
	}
}
