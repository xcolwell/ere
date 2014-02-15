package org.resisttheb.ere.signature;

import java.awt.Color;

// how many comparisons per second?
public class ComparisonBench {

	
	private static int[] colors(final Color ... colors) {
		final int[] rgbs = new int[colors.length];
		for (int i = 0; i < colors.length; ++i) {
			rgbs[i] = colors[i].getRGB();
		}
		return rgbs;
	}
	
	private static float[] weights(final float ... weights) {
		return weights;
	}
	
	
	public static void main(final String[] in) {
		final int N = 50 * 50 * 50;
		final int M = 2;
		
		final Signature s0 = new Signature(2, 
				colors(Color.GREEN, Color.BLUE), 
				weights(0.9f, 0.7f));
		final Signature s1 = new Signature(5, 
				colors(Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN), 
				weights(0.95f, 0.77f, 0.56f, 0.53f, 0.402f));
		
		System.out.println(SignatureUtilities.distance(s0, s1));
		
		long net = 0;
		for (int j = 0; j < M; ++j) {
		long time0 = System.nanoTime();
		for (int i = 0; i < N; ++i) {
			SignatureUtilities.distance(s0, s1);
		}
		long time1 = System.nanoTime();
		
		net += time1 - time0;
		}
		
		System.out.println(net / M);
	}
	
	
	
	public static void main2(final String[] in) {
		final int N = 1000000000;
		final int M = N / 2;
		
		boolean test;
		
		final int[] junk = new int[4 * 1024];
		for (int i = 0; i < junk.length; ++i) {
			junk[i] = (int) Math.round(junk.length * Math.random());
		}
		
		long time0 = System.nanoTime();
		for (int i = 0; i < N; ++i) {
			test = junk[i % junk.length] < junk[(i + junk.length / 2) % junk.length];
		}
		long time1 = System.nanoTime();
		
		System.out.println(time1 - time0);
		
		
	}
}
