package org.resisttheb.ere.signature;

import java.io.Serializable;

public class Signature implements Serializable {
	// each item is color,weight
	// weight on [0, 1] where 0 is best, 1 is worst
	// distance between colors is just euclidean / NORMALIZE
	//
	//
	
	
	// PARALLEL: {
	public final int[] colors;
	public final float[] weights;
	// computed: squared weights
	public final float[] sqweights;
	// }
	public final int count;
	
	
	public Signature(final int _count, final int[] _colors, final float[] _weights) {
		this.count = _count;
		
		if (_colors.length < count)
			throw new IllegalArgumentException();
		if (_weights.length < count)
			throw new IllegalArgumentException();
		
//		this.colors = new int[count];
//		this.weights = new float[count];
//		System.arraycopy(_colors, 0, colors, 0, count);
//		System.arraycopy(_weights, 0, weights, 0, count);
		this.colors = _colors;
		this.weights = _weights;
		
		sqweights = new float[count];
		for (int i = 0; i < count; ++i) {
			sqweights[i] = weights[i] * weights[i];
		}
	}
	
	private Signature() {
		colors = null;
		weights = null;
		sqweights = null;
		count = 0;
	}
	
}
