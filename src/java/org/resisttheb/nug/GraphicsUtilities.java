package org.resisttheb.nug;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.List;

// cardinal spline code taken from prefuse's GraphicsLib
/**
 * 
 * 
 * <p>Note: this class uses code from <a href="prefuse.org">prefuse</a>'s 
 * <code>GraphicsLib</code> class.
 * </p>
 */
public class GraphicsUtilities {

	public static float[] augment(final float x0, final float y0, final float[] coords, final int n) {
		final int clength = n << 1;
		final float[] augCoords = new float[ clength + 2 ];
		augCoords[ 0 ] = x0;
		augCoords[ 1 ] = y0;
		System.arraycopy( coords, 0, augCoords, 2, clength );
		return augCoords;
	}
	
	public static float[] augment(final float[] coords, final int n, final float xn, final float yn) {
		final int clength = n << 1;
		final float[] augCoords = new float[ clength + 2 ];
		System.arraycopy( coords, 0, augCoords, 0, clength );
		augCoords[ augCoords.length - 2 ] = xn;
		augCoords[ augCoords.length - 1 ] = yn;
		return augCoords;
	}

	
	public static Rectangle2D.Float boundingBox(final float x0, final float y0, final float[] coords, final int n) {
		return boundingBox( augment( x0, y0, coords, n ), n + 1 );
	}
	
	public static Rectangle2D.Float boundingBox(final float[] coords, int n) {
		if ( n <= 0 )
			return null;
		n <<= 1;
		assert 0 == n % 2 : "The odd case does not make sense.";
		if ( 0 != n % 2 )
			return null;
		
		float
			minx = coords[ 0 ],
			miny = coords[ 1 ],
			maxx = minx,
			maxy = miny;
		for ( int i = 2; i < n; i += 2 ) {
			final float
				x = coords[ i ],
				y = coords[ i + 1 ];
			minx = Math.min( x, minx );
			miny = Math.min( y, miny );
			maxx = Math.max( x, maxx );
			maxy = Math.max( y, maxy );
		}
		return new Rectangle2D.Float( minx, miny, maxx - minx, maxy - miny );
	}
	
	public static float[] add(float[] coords, int i, final float x, final float y) {
		if ( coords.length <= i + 1 ) {
			final float[] _coords = new float[ 4 + coords.length << 1 ];
			System.arraycopy( coords, 0, _coords, 0, coords.length );
			coords = _coords;
		}
		coords[ i ] = x;
		coords[ i + 1 ] = y;
		return coords;
	}
	
	
	public static void reverseCoords(final float[] coords, final int offset, int npoints) {
		final int length = npoints << 1;
		for ( int i = 0, itrLength = length >> 1; itrLength > i; i += 2 ) {
			final int
				xi0 = offset + i,
				yi0 = xi0 + 1,
				xi1 = offset + length - 2 - i, 
				yi1 = xi1 + 1;
			
			final float
				x0 = coords[ xi0 ],
				y0 = coords[ yi0 ];
			
			coords[ xi0 ] = coords[ xi1 ];
			coords[ yi0 ] = coords[ yi1 ];
			
			coords[ xi1 ] = x0;
			coords[ yi1 ] = y0;
		}
	}
	
	
	public static float[] finterleave(final double[] xs, final double[] ys) {
		return finterleave( xs, ys, Math.min( xs.length, ys.length ) );
	}
	
	public static float[] finterleave(final double[] xs, final double[] ys, final int n) {
		final float[] coords = new float[ n << 1 ];
		for ( int i = 0, j = 0; i < n; i++, j += 2 ) {
			coords[ j ] = (float) xs[ i ];
			coords[ j + 1 ] = (float) ys[ i ];
		}
		return coords;
	}
	
	public static float[] interleave(final Point2D.Float ... points) {
		return interleave( points, 0, points.length );
	}
	
	public static float[] interleave(final Point2D.Float[] points, final int offset, final int length) {
		final float[] interleaved = new float[ length << 1 ];
		for ( int i = 0; i < length; i++ ) {
			final Point2D.Float point = points[ offset + i ];
			final int n = i << 1;
			interleaved[ n ] = point.x;
			interleaved[ n + 1 ] = point.y;
		}
		return interleaved;
	}
	
	
	
	// minMax should be an array of length 2
	// [0] = min
	// [1] = max
	public static void addToMinMax(final float[] minMax, final float value) {
		if ( null == minMax || minMax.length < 2 )
			throw new IllegalArgumentException();
		if ( value < minMax[ 0 ] )
			minMax[ 0 ] = value;
		if ( minMax[ 1 ] < value )
			minMax[ 1 ] = value;
	}
	
	
	public static Shape combineLines(final Line2D.Float ... lines) {
		final GeneralPath gpath = new GeneralPath();
		for ( Line2D.Float line : lines ) {
			gpath.append( line, true );
		}
		return gpath;
	}
	
	public static Line2D.Float[] extractLines(final Shape shape) {
		if ( shape instanceof RectangularShape ) {
			final RectangularShape r = (RectangularShape) shape;
			final float
				x0 = (float) r.getMinX(),
				x1 = (float) r.getMaxX(),
				y0 = (float) r.getMinY(),
				y1 = (float) r.getMaxY();
			
			// (x0, y0) -> (x1, y0)
			// (x1, y0) -> (x1, y1)
			// (x1, y1) -> (x0, y1)
			// (x0, y1) -> (x0, y0)
			return new Line2D.Float[]{ 
				new Line2D.Float( x0, y0, x1, y0 ),	
				new Line2D.Float( x1, y0, x1, y1 ),	
				new Line2D.Float( x1, y1, x0, y1 ),	
				new Line2D.Float( x0, y1, x0, y0 )	
			};
		}
		else {
			return extractLinesImpl( shape );
		}
	}
	
	private static Line2D.Float[] extractLinesImpl(final Shape shape) {
		final double flatness = 12.0;
		
		final List<Line2D.Float> lines = new ArrayList<Line2D.Float>();
		
		float x, y;
		float sx = -1, sy = -1;
		float px = -1, py = -1;
		final float[] coords = new float[ 6 ];
		for ( PathIterator pitr = shape.getPathIterator( null, flatness ); !pitr.isDone(); pitr.next() ) {
			final int type = pitr.currentSegment( coords );

			switch ( type ) {
				case PathIterator.SEG_CLOSE:
					lines.add( new Line2D.Float( px, py, sx, sy ) );
					break;
				case PathIterator.SEG_MOVETO:
					sx = px = coords[ 0 ];
					sy = py = coords[ 1 ];
					break;
				case PathIterator.SEG_LINETO:
					lines.add( new Line2D.Float( px, py, x = coords[ 0 ], y = coords[ 1 ] ) );
					px = x;
					py = y;
					break;
				default: assert false : type;
			}
		}
		
		return lines.toArray( new Line2D.Float[ lines.size() ] );
	}
	
	
	
	// i may be positive or negative
	public static Point2D.Float get(final float[] interleaved, int offset, int length, final int i) {
		offset <<= 1;
		length <<= 1;
		final int baseIndex;
		if ( i < 0 ) {
			baseIndex = offset + ( length & ~0x01 ) + 2 * i;
		}
		else {
			baseIndex = offset + 2 * i;
		}
		return offset <= baseIndex && baseIndex + 1 <= offset + length 
			? new Point2D.Float( interleaved[ baseIndex ], interleaved[ baseIndex + 1 ] )
			: null;
	}
	
	
	public static boolean isCcw(final Point2D.Float a, final Point2D.Float b, final Point2D.Float c) {
		// 0 < (ab)x(bc)
		return 0 <= ( b.x - a.x ) * ( c.y - b.y ) - ( b.y - a.y ) * ( c.x - b.x );
	}
	
	
	
	/*****************************************
	 * TAKEN FROM PREFUSE
	 *****************************************/
	
    /**
     * Compute a cardinal spline, a series of cubic Bezier splines smoothly
     * connecting a set of points. Cardinal splines maintain C(1)
     * continuity, ensuring the connected spline segments form a differentiable
     * curve, ensuring at least a minimum level of smoothness.
     * @param pts the points to interpolate with a cardinal spline
     * @param slack a parameter controlling the "tightness" of the spline to
     * the control points, 0.10 is a typically suitable value
     * @param closed true if the cardinal spline should be closed (i.e. return
     * to the starting point), false for an open curve
     * @return the cardinal spline as a Java2D {@link java.awt.geom.GeneralPath}
     * instance.
     */
    public static GeneralPath cardinalSpline(float pts[], float slack, boolean closed) {
        GeneralPath path = new GeneralPath();
        path.moveTo(pts[0], pts[1]);
        return cardinalSpline(path, pts, slack, closed, 0f, 0f);
    }
    
    /**
     * Compute a cardinal spline, a series of cubic Bezier splines smoothly
     * connecting a set of points. Cardinal splines maintain C(1)
     * continuity, ensuring the connected spline segments form a differentiable
     * curve, ensuring at least a minimum level of smoothness.
     * @param pts the points to interpolate with a cardinal spline
     * @param start the starting index from which to read points
     * @param npoints the number of points to consider
     * @param slack a parameter controlling the "tightness" of the spline to
     * the control points, 0.10 is a typically suitable value
     * @param closed true if the cardinal spline should be closed (i.e. return
     * to the starting point), false for an open curve
     * @return the cardinal spline as a Java2D {@link java.awt.geom.GeneralPath}
     * instance.
     */
    public static GeneralPath cardinalSpline(float pts[], int start, int npoints,
            float slack, boolean closed)
    {
        GeneralPath path = new GeneralPath();
        path.moveTo(pts[start], pts[start+1]);
        return cardinalSpline(path, pts, start, npoints, slack, closed, 0f, 0f, false, false);
    }
    
    /**
     * Compute a cardinal spline, a series of cubic Bezier splines smoothly
     * connecting a set of points. Cardinal splines maintain C(1)
     * continuity, ensuring the connected spline segments form a differentiable
     * curve, ensuring at least a minimum level of smoothness.
     * @param p the GeneralPath instance to use to store the result
     * @param pts the points to interpolate with a cardinal spline
     * @param slack a parameter controlling the "tightness" of the spline to
     * the control points, 0.10 is a typically suitable value
     * @param closed true if the cardinal spline should be closed (i.e. return
     * to the starting point), false for an open curve
     * @param tx a value by which to translate the curve along the x-dimension
     * @param ty a value by which to translate the curve along the y-dimension
     * @return the cardinal spline as a Java2D {@link java.awt.geom.GeneralPath}
     * instance.
     */
    public static GeneralPath cardinalSpline(GeneralPath p, 
            float pts[], float slack, boolean closed, float tx, float ty)
    {
        int npoints = 0;
        for ( ; npoints<pts.length; ++npoints )
            if ( Float.isNaN(pts[npoints]) ) break;
        return cardinalSpline(p, pts, 0, npoints/2, slack, closed, tx, ty, false, false);
    }
    
    /**
     * Compute a cardinal spline, a series of cubic Bezier splines smoothly
     * connecting a set of points. Cardinal splines maintain C(1)
     * continuity, ensuring the connected spline segments form a differentiable
     * curve, ensuring at least a minimum level of smoothness.
     * @param p the GeneralPath instance to use to store the result
     * @param pts the points to interpolate with a cardinal spline
     * @param start the starting index from which to read points
     * @param npoints the number of points to consider
     * @param slack a parameter controlling the "tightness" of the spline to
     * the control points, 0.10 is a typically suitable value
     * @param closed true if the cardinal spline should be closed (i.e. return
     * to the starting point), false for an open curve
     * @param tx a value by which to translate the curve along the x-dimension
     * @param ty a value by which to translate the curve along the y-dimension
     * @return the cardinal spline as a Java2D {@link java.awt.geom.GeneralPath}
     * instance.
     */
    public static GeneralPath cardinalSpline(GeneralPath p, 
            float pts[], int start, int npoints,
            float slack, boolean closed, float tx, float ty)
    {
    	return cardinalSpline(p, pts, start, npoints, slack, closed, tx, ty, false, false);
    }
    	
    	
    public static GeneralPath cardinalSpline(GeneralPath p, 
                float pts[], int start, int npoints,
                float slack, boolean closed, float tx, float ty, boolean hasInitial, boolean hasFinal
               	)
    {
        // compute the size of the path
        int len = 2*npoints;
        int end = start+len;
        
        if ( len < 6 ) {
            throw new IllegalArgumentException(
                    "To create spline requires at least 3 points");
        }
        
        float dx1, dy1, dx2, dy2;
        
        // compute first control point
        if ( closed ) {
            dx2 = pts[start+2]-pts[end-2];
            dy2 = pts[start+3]-pts[end-1];
        } 
        else if ( hasInitial ) {
        	dx2 = pts[start+2]-pts[start-2];
            dy2 = pts[start+3]-pts[start-1];
        }
        else {
            dx2 = pts[start+4]-pts[start];
            dy2 = pts[start+5]-pts[start+1];
        }
        
        // repeatedly compute next control point and append curve
        int i;
        for ( i=start+2; i<end-2; i+=2 ) {
            dx1 = dx2; dy1 = dy2;
            dx2 = pts[i+2]-pts[i-2];
            dy2 = pts[i+3]-pts[i-1];
            p.curveTo(tx+pts[i-2]+slack*dx1, ty+pts[i-1]+slack*dy1,
                      tx+pts[i]  -slack*dx2, ty+pts[i+1]-slack*dy2,
                      tx+pts[i],             ty+pts[i+1]);
        }
        
        // compute last control point
        if ( closed ) {
            dx1 = dx2; dy1 = dy2;
            dx2 = pts[start]-pts[i-2];
            dy2 = pts[start+1]-pts[i-1];
            p.curveTo(tx+pts[i-2]+slack*dx1, ty+pts[i-1]+slack*dy1,
                      tx+pts[i]  -slack*dx2, ty+pts[i+1]-slack*dy2,
                      tx+pts[i],             ty+pts[i+1]);
            
            dx1 = dx2; dy1 = dy2;
            dx2 = pts[start+2]-pts[end-2];
            dy2 = pts[start+3]-pts[end-1];
            p.curveTo(tx+pts[end-2]+slack*dx1, ty+pts[end-1]+slack*dy1,
                      tx+pts[0]    -slack*dx2, ty+pts[1]    -slack*dy2,
                      tx+pts[0],               ty+pts[1]);
            p.closePath();
        } 
        else if ( hasFinal ) {
        	dx1 = dx2; dy1 = dy2;
            dx2 = pts[i+2]-pts[i-2];
            dy2 = pts[i+3]-pts[i-1];
        	p.curveTo(tx+pts[i-2]+slack*dx1, ty+pts[i-1]+slack*dy1,
                    tx+pts[i]  -slack*dx2, ty+pts[i+1]-slack*dy2,
                    tx+pts[i],             ty+pts[i+1]);
        }
        else {
            p.curveTo(tx+pts[i-2]+slack*dx2, ty+pts[i-1]+slack*dy2,
                      tx+pts[i]  -slack*dx2, ty+pts[i+1]-slack*dy2,
                      tx+pts[i],             ty+pts[i+1]);
        }
        return p;
    }
    
    /*****************************************
	 * END TAKEN FROM PREFUSE
	 *****************************************/
	
    
    
    private GraphicsUtilities() {
    }
}
