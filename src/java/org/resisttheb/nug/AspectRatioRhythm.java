package org.resisttheb.nug;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



public class AspectRatioRhythm {
	private static final float _2_PI = (float) ( 2 * Math.PI );
	
	// 8 pixels
	private static final float DEFAULT_RESOLUTION = 8.f;
	
	
	public static interface EuclideanIterator<T> extends Iterator<T> {
		public float getIteratedDistance();
	}
	
	// returns an iterator of boxes that represents the bounds of this shape.
	//  -- curved lines are converted using the given resolution, which is the max side of a box (in px) for a curve
	public static EuclideanIterator<Rectangle2D.Float> boxize(final Shape shape, final float resolution) {
		final PathIterator pathItr = new FlatteningPathIterator( shape.getPathIterator( new AffineTransform() ), resolution );
		return new EuclideanIterator<Rectangle2D.Float>() {
			private List<Rectangle2D.Float> queue = new LinkedList<Rectangle2D.Float>();
			private List<Float> segdQueue = new LinkedList<Float>();
			
			private float[] coords = new float[ 6 ];
			private float px, py;
			private float itrd = 0.f;
			private float syncd = 0.f;
			
			// TODO: linearize lines to have the given resolution
			private void pullNext() {
				for (; queue.isEmpty() && !pathItr.isDone(); pathItr.next() ) {
					System.out.println( "  [pullNext] loop" );
					switch ( pathItr.currentSegment( coords ) ) {
						case PathIterator.SEG_MOVETO:
							px = coords[ 0 ];
							py = coords[ 1 ];
							
							assert !Float.isNaN( px );
							assert !Float.isNaN( py );
							break;
							
						case PathIterator.SEG_CLOSE:
							break;
							
						case PathIterator.SEG_LINETO:
							final float x0 = px, y0 = py;
//							px = coords[ 0 ];
//							py = coords[ 1 ];
							final float 
								dx = coords[ 0 ] - x0,
								dy = coords[ 1 ] - y0;
							final float d = (float) Math.sqrt( dx * dx + dy * dy );
							final int isteps = (int) Math.ceil( d / resolution ) - 1;
							System.out.println( "  [pullNext] isteps: " + isteps );
							for ( int i = 0; i <= isteps; i++ ) {
								final float u = 0 == isteps ? 1.f : (float) i / isteps;
								final float 
									x = x0 + u * dx,
									y = y0 + u * dy;
								
								final Rectangle2D.Float f = new Rectangle2D.Float( 
										Math.min( x, px ), Math.min( y, py ), 
										Math.max( resolution, Math.abs( x - px ) ), Math.max( resolution, Math.abs( y - py ) ) );
								queue.add( f );
								segdQueue.add( d / ( isteps + 1 ) );
								
								px = x;
								py = y;
								
								assert !Float.isNaN( px );
								assert !Float.isNaN( py );
							}
							syncd += d;
//							totald += d;
							break;
						case PathIterator.SEG_CUBICTO:
						case PathIterator.SEG_QUADTO:
							assert false : "Path should be flat!";
						default:
							assert false : "Unknown segment type.";
					}
					
					assert !Float.isNaN( px );
					assert !Float.isNaN( py );
				}
			}
			
			
			// =========================
			// <code>EuclideanIterator</code> implementation
			
			public float getIteratedDistance() {
				return itrd;
			}
			
			// =========================
			
			// =========================
			// <code>Iterator</code> implementation
			
			public Rectangle2D.Float next() {
				final float d = segdQueue.remove( 0 );
				itrd = segdQueue.isEmpty() ? syncd : itrd + d; 
				return queue.remove( 0 );
			}
			
			public boolean hasNext() {
				pullNext();
				assert queue.size() == segdQueue.size();
				return !queue.isEmpty();
			}
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			// =========================
		};
	}
	
	public static boolean intersects(final Shape shape, final Shape lesserShape) {
		return intersects( shape, lesserShape, DEFAULT_RESOLUTION );
	}
	
	public static boolean intersects(final Shape shape, final Shape lesserShape, final float resolution) {
		for ( Iterator<Rectangle2D.Float> boxItr = boxize( lesserShape, resolution ); boxItr.hasNext();  ) {
			System.out.println( "  [intersects]" );
			final Rectangle2D box = boxItr.next();
			if ( shape.intersects( box ) )
				return true;
			assert !shape.contains( box );
		}
		
		return false;
	}
	
	
	
	// height change when switching between fitting the two shapes to the screen (maintains a fixed aspect ratio)
	public static float heightChange(final Shape from, final Shape to) {
		// bb(a), bb(b)
		// max(w_a,h_b) / max(w_b,h_b)
		
		final Rectangle2D
			fbb = from.getBounds2D(),
			tbb = to.getBounds2D();
		return (float) ( Math.max( fbb.getHeight(), fbb.getWidth() ) / 
				Math.max( tbb.getHeight(), tbb.getWidth() ) );
	}
	
	// TODO:
	// TODO: a version that just rotates, does a normal bounding box, then rotates the bounding ox back
	public static Shape boundingBox(final Shape shape, final float boxt) {
		// Rotate the shape; take its bounding box
		return AffineTransform.getRotateInstance( boxt ).createTransformedShape( 
				AffineTransform.getRotateInstance( -boxt ).createTransformedShape( shape ).getBounds2D() );
	}
	
	
	private static Point2D.Float[] initCorners(Point2D.Float[] corners) {
		if ( corners.length < 4 ) {
			final Point2D.Float[] _corners = new Point2D.Float[ 4 ];
			System.arraycopy( corners, 0, _corners, 0, 4 );
			corners = _corners;
		}
		
		for ( int i = 0; i < 4; i++ ) {
			if ( null == corners[ i ] )
				corners[ i ] = new Point2D.Float();
		}
		
		return corners;
	}
	
	public static Point2D.Float[] boundingBoxCorners(final Shape shape, final float boxt, Point2D.Float[] corners) {
		corners = initCorners( corners );
		
		final Rectangle2D bounds = AffineTransform.getRotateInstance( -boxt 
				).createTransformedShape( shape ).getBounds2D();
		
		final float
			x = (float) bounds.getX(),
			y = (float) bounds.getY(),
			w = (float) bounds.getWidth(),
			h = (float) bounds.getHeight();
		
		corners[ 0 ].setLocation( x, y );
		corners[ 1 ].setLocation( x, y + h );
		corners[ 2 ].setLocation( x + w, y + h );
		corners[ 3 ].setLocation( x + w, y );
		
		final AffineTransform bat = AffineTransform.getRotateInstance( boxt );
		
		for ( int i = 0; i < 4; i++ )
			bat.transform( corners[ i ], corners[ i ] );
		
		return corners;
	}
	
	
	/**
	 * A slower bounding box algorithm, but interesting to code.
	 * 
	 * @see #boundingBox
	 */
	public static Shape boundingBox2(final Shape shape, final float boxt) {
		final float 
			u = (float) Math.cos( boxt ),
			v = (float) Math.sin( boxt );
		
		final float resolution = 1.f;
		final Line2D.Float[] lines = {
				tightestLine( shape,  u,  v, resolution ),
				tightestLine( shape,  v, -u, resolution ),
				tightestLine( shape, -u, -v, resolution ),
				tightestLine( shape,  -v,  u, resolution )
		};
		
		final int n = lines.length;
		final Point2D.Float[] ipoints = new Point2D.Float[ n ];
		for ( int i = 0; i < n; i++ ) {
			ipoints[ i ] = intersection( lines[ i ], lines[ ( i + 1 ) % n ] );
			assert null != ipoints[ i ];
			
		}
		
		
		final GeneralPath path = new GeneralPath();
		path.moveTo( ipoints[ 0 ].x, ipoints[ 0 ].y );
		path.lineTo( ipoints[ 1 ].x, ipoints[ 1 ].y );
		path.lineTo( ipoints[ 2 ].x, ipoints[ 2 ].y );
		path.lineTo( ipoints[ 3 ].x, ipoints[ 3 ].y );
		path.lineTo( ipoints[ 0 ].x, ipoints[ 0 ].y );
		
		// DEBUGGING: {
//		path.append( lines[ 0 ], false );
//		path.append( lines[ 1 ], false );
//		path.append( lines[ 2 ], false );
//		path.append( lines[ 3 ], false );
		// }
		
		
		return path;
	}
	
	
	private static final float MAX_MAXD = Float.MAX_VALUE / 2.f;
	
	// returns the tightest line along the given 
	private static Line2D.Float tightestLine(final Shape shape, final float u, final float v, final float resolution) {
		// 1. for each edge, push from the center line outward -- keep doubling until we get a no hit,
		//      then bisect down
		// 
		
		// bisect. if middle is no hit, go down; otherwise go up;
		//    
		
		final Rectangle2D bbox = shape.getBounds2D();
		final float hlength = (float) Math.max( bbox.getWidth(), bbox.getHeight() ) / 2.f;
		
		final Line2D.Float line = new Line2D.Float();
		
		float maxd = resolution;
		// Need to use box-based intersection, like the ellipse
		while ( maxd < MAX_MAXD && intersects( shape, perp( u, v, maxd, hlength, line ) ) ) maxd *= 2;
		
//		maxd *= 2;
		
		// The interesting region is <code>(d / 2, d)</code>:
		float d = 0.75f * maxd;
		
		
		for ( float step = 0.25f * maxd; resolution <= step; step /= 2 ) {
			if ( intersects( shape, perp( u, v, d, hlength, line ) ) )
				d += step;
			else
				d -= step;
			
		}
		
//		assert line.equals( perp( u, v, d, hlength, new Line2D.Float() ) );
		return perp( u, v, d, hlength, line );
	}
	
	private static Line2D.Float perp(final float u, final float v, final float d, final float hlength, final Line2D.Float line) {
		final float 
			x0 = u * d,
			y0 = v * d;
		final float
			x1 = x0 - v * hlength,
			x2 = x0 + v * hlength,
			y1 = y0 + u * hlength,
			y2 = y0 - u * hlength;
		
		assert !Float.isNaN( x1 );
		assert !Float.isNaN( x2 );
		assert !Float.isNaN( y1 );
		assert !Float.isNaN( y2 );
		
		line.setLine( x1, y1, x2, y2 );
		return line;
	}
	
	
	
	public static Point2D.Float intersection(final Line2D.Float a, final Line2D.Float b) {
		final float
			x1 = a.x1,
			x2 = a.x2,
			x3 = b.x1,
			x4 = b.x2,
			y1 = a.y1,
			y2 = a.y2,
			y3 = b.y1,
			y4 = b.y2;
		final float d = ( y4 - y3 ) * ( x2 - x1 ) - ( x4 - x3 ) * ( y2 - y1 );
//		final float isqrtd = (float) ( 1.0 / Math.sqrt( Math.abs( d ) ) );
		if ( NumericUtilities.fequal( d, 0.f ) )
			return null;
		final float
//			ua = Math.signum( d ) * ( ( ( x4 - x3 ) * isqrtd ) * ( ( y1 - y3 ) * isqrtd ) - ( ( y4 - y3 ) * isqrtd ) * ( ( x1 - x3 ) * isqrtd ) );
			ua = ( ( x4 - x3 ) * ( y1 - y3 ) - ( y4 - y3 ) * ( x1 - x3 ) ) / d;
//			ub = ( ( x2 - x1 ) * ( y1 - y3 ) - ( y2 - y1 ) * ( x1 - x3 ) ) / d;
		final float
			x = x1 + ua * ( x2 - x1 ),
			y = y1 + ua * ( y2 - y1 );
		assert !Float.isNaN( x );
		assert !Float.isNaN( y );
		return new Point2D.Float( x, y );
	}
	
	public static boolean isOnSegment(final Line2D.Float seg, final Point2D.Float p) {
		final float u = lerpp( new Point2D.Float( seg.x1, seg.y1 ), new Point2D.Float( seg.x2, seg.y2 ), p );
		return 0.f <= u && u <= 1.f;
		//|| NumericUtilities.fequal( u, 0.f ) || NumericUtilities.fequal( u, 1.f );
	}
	
	
	
	
	public static <P extends Point2D.Float> P lerp(
			final Point2D.Float p0, final Point2D.Float p1, final float u, final P pu
	) {
		pu.setLocation( p0.x + ( p1.x - p0.x ) * u, p0.y + ( p1.y - p0.y ) * u );
		return pu;
	}
	
	public static float lerpp(final Point2D.Float p0, final Point2D.Float p1, final Point2D.Float pt) {
		final float
			dx = p1.x - p0.x,
			dy = p1.y - p0.y,
			pdx = pt.x - p0.x,
			pdy = pt.y - p0.y;
		
		final float
			rsq = dx * dx + dy * dy,
			prsq = pdx * pdx + pdy * pdy;
			
		
		final float dot = dx * pdx + dy * pdy;
		
		// dot * dot == rsq * prsq
		if (! NumericUtilities.fequal( dot / prsq, rsq / dot ) ) {
			// Not co-linear:
			return Float.NaN;
		}
		
		// project pd onto d
		return dot / rsq;
	}
}
