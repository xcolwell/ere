package org.resisttheb.ere.ui3;

import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Arrays;


//uses a linear approximation to break up the path
//
public final class CubicPath implements Path {
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static PathIterator flatten(final PathIterator itr) {
		return new FlatteningPathIterator(itr,
				1.0, 10);
	}
	
	private static int countSegs(final GeneralPath path) {
		int count = 0;
		
		final float[] coords = new float[6];
		for (PathIterator itr = flatten(path.getPathIterator(null));
			!itr.isDone(); itr.next()
		) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					break;
					
				case PathIterator.SEG_LINETO:
					++count;
					break;
					
				case PathIterator.SEG_QUADTO:
				case PathIterator.SEG_CUBICTO:
				case PathIterator.SEG_CLOSE:
					break;
			}
		}
		
		return count;
	}
	
	
	private static float len(final float[] coords) {
		final float dx = coords[2] - coords[0];
		final float dy = coords[3] - coords[1];
		return (float) Math.sqrt(dx * dx + dy * dy);
	}
	
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	// these are length markers
	private final float[] markers;
	// segment coords. yes, we double coords, not a big deal
	// [x0, y0, x1, y1]
	private final float[][] segs;
	
	
	// [x0, y0, cx0, cy0, cx1, cy1, x1, y1]
	public CubicPath(final float ... bounds) {
		GeneralPath path = new GeneralPath();
		path.moveTo(bounds[0], bounds[1]);
		path.curveTo(bounds[2], bounds[3], 
				bounds[4], bounds[5], 
				bounds[6], bounds[7]);
		
		final int count = countSegs(path);
		
		markers = new float[count];
		segs = new float[count][];
		
		// initializes length + markers
		init(path);
	}
	
	
	private void init(final GeneralPath path) {
		// init markers and length
		
		final float[] coords = new float[6];
		int i = 0;
		float px = 0.f;
		float py = 0.f;
		for (PathIterator itr = flatten(path.getPathIterator(null));
			itr.isDone(); itr.next()
		) {
			switch (itr.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					px = coords[0];
					py = coords[1];
					break;
					
				case PathIterator.SEG_LINETO:
					final float[] seg = new float[]{
						px, py,
						coords[0], coords[1]
					};
					segs[i] = seg;
					markers[i] = len(seg);
					++i;
					px = coords[0];
					py = coords[1];
					break;
					
				case PathIterator.SEG_QUADTO:
				case PathIterator.SEG_CUBICTO:
				case PathIterator.SEG_CLOSE:
					break;
			}
		}
	}
	
	
	
	
	/**************************
	 * PATH IMPLEMENTATION
	 **************************/
	
	public float length() {
		return markers[markers.length - 1];
	}
	
	public void eval(float u, final float[] coords) {
		if (u < 0.f)
			u = 0.f;
		else if (1.f < u)
			u = 1.f;
		
		final float length = length();
		final float targetLength = u * length;
		int index = Arrays.binarySearch(markers, targetLength);
		if (index < 0) {
			index = ~index;
		}
		
		float su = (markers[index] - length) / (markers[index] - 
				(0 < index ? markers[index - 1] : 0));
		final float[] seg = segs[index];
		coords[0] = seg[0] + (seg[2] - seg[0]) * su;
		coords[1] = seg[1] + (seg[3] - seg[1]) * su;			
	}
	
	/**************************
	 * END PATH IMPLEMENTATION
	 **************************/
}

