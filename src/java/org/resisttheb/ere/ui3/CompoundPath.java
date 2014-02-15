package org.resisttheb.ere.ui3;

import java.util.Arrays;

public final class CompoundPath implements Path {
	private final Path[] paths;
	
	private final float[] markers;
	
	
	public CompoundPath(final Path ... _paths) {
		this.paths = _paths;
		
		markers = new float[paths.length];
		float netlen = 0.f;
		for (int i = 0; i < paths.length; ++i) {
			netlen += paths[i].length();
			markers[i] = netlen;
		}
	}
	
	
	/**************************
	 * PATH IMPLEMENTATION
	 **************************/
	
	public float length() {
		return markers.length <= 0 ? 0 : markers[markers.length - 1];
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
		paths[index].eval(su, coords);
	}
	
	/**************************
	 * END PATH IMPLEMENTATION
	 **************************/
}
