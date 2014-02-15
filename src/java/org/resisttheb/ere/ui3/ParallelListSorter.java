package org.resisttheb.ere.ui3;


/**
 * Input: 	a float[] and an int[], parallel arrays
 * Output: 	both arrays are sorted according to the values in the float[]
 *
 * This class keeps buffers resident in memory, to avoid repeated allocation.
 * Not thread safe.
 * 
 * Uses a three-pass (11-bit bucket) radix sort,
 * as described at http://www.stereopsis.com/radix.html .
 * 
 * FUTURE: the majority cost is getting the float bits.
 *         A tie in to a native implementation would be good.
 */
/*
 * Pieces taken from Michael Herf's radix sort demo, 
 *     http://www.stereopsis.com/radix.html
 */
public class ParallelListSorter {
	private static final int BUCKET_SIZE = 0x01 << 11;
	
	private final int[] _b0 	= new int[BUCKET_SIZE];
	private final int[] _b1 	= new int[BUCKET_SIZE];
	private final int[] _b2 	= new int[BUCKET_SIZE];
	
	private int[] _farray 		= null;
	private int[] _iarray 		= null;
	private int[] _fsort 		= null;
	private int[] _isort		= null;
	
	
	public ParallelListSorter(final int maxSize) {
		buffer(maxSize);
	}
	
	
	public void buffer(final int maxSize) {
		_farray 	= new int[maxSize];
		_iarray 	= new int[maxSize];
		_fsort 		= new int[maxSize];
		_isort 		= new int[maxSize];
	}
	
	
	public void sort(final float[] fin, final int[] iin, final int elements) {
		sort(fin, iin, elements, 
				_farray, _iarray, _fsort, _isort, _b0, _b1, _b2);
	}
	
	private void sort(final float[] fin, final int[] iin, final int elements,
			final int[] farray, final int[] iarray,
			final int[] fsort, final int[] isort, 
			final int[] b0, final int[] b1, final int[] b2
	)
	{
		int i;
		for (i = 0; i < BUCKET_SIZE; i++) {
			b0[i] = 0;
			b1[i] = 0;
			b2[i] = 0;
		}

		// 1.  parallel histogramming pass
		//
		for (i = 0; i < elements; i++) {
			int fi = Float.floatToRawIntBits(fin[i]);
			fi ^= -(fi >>> 31) | 0x80000000;
			farray[i] = fi;

			b0[fi & 0x7FF]++;
			b1[(fi >>> 11) & 0x7FF]++;
			b2[fi >>> 22]++;
		}
		
		// 2.  Sum the histograms -- each histogram entry records the number of values preceding itself.
		{
			int sum0 = 0, sum1 = 0, sum2 = 0;
			int tsum;
			for (i = 0; i < BUCKET_SIZE; i++) {
				tsum = b0[i] + sum0;
				b0[i] = sum0 - 1;
				sum0 = tsum;

				tsum = b1[i] + sum1;
				b1[i] = sum1 - 1;
				sum1 = tsum;

				tsum = b2[i] + sum2;
				b2[i] = sum2 - 1;
				sum2 = tsum;
			}
		}

		// byte 0: floatflip entire value, read/write histogram, write out flipped
		for (i = 0; i < elements; i++) {
			int fi = farray[i];
			int pos = ++b0[fi & 0x7FF];
			
			fsort[pos] = fi;
			isort[pos] = iin[i];
		}

		// byte 1: read/write histogram, copy
		//   sorted -> array
		for (i = 0; i < elements; i++) {
			int si = fsort[i];
			int pos = ++b1[(si >>> 11) & 0x7FF];
			
			farray[pos] = si;
			iarray[pos] = isort[i];
		}

		// byte 2: read/write histogram, copy & flip out
		//   array -> sorted
		for (i = 0; i < elements; i++) {
			int ai = farray[i];
			int pos = ++b2[ai >>> 22];

			fin[pos] = Float.intBitsToFloat(ai ^ (((ai >>> 31) - 1) | 0x80000000));
			iin[pos] = iarray[i];
		}
	}
	
	
	
	/**************************
	 * TEST DRIVER
	 **************************/
	
	public static void main(final String[] in) {
		final float[] values = new float[1000];
		final int[] ivalues = new int[values.length];
		
		final ParallelListSorter pls = new ParallelListSorter(values.length);
		
		final int N = 300;
		long netdt = 0;
		
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < values.length; i++) {
				values[i] = (float) Math.random();
				ivalues[i] = i;
			}
			
			final long time0 = System.nanoTime();
			pls.sort(values, ivalues, values.length);
			final long time1 = System.nanoTime();
			
			netdt += (time1 - time0);
		}
		System.out.println(netdt / N);
		
		
		for (int i = 1; i < values.length; i++) {
			if (values[i - 1] > values[i]) {
				System.out.println("inversion!");
			}
		}
	}
	
	/**************************
	 * END TEST DRIVER
	 **************************/
}
