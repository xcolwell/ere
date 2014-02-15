package org.resisttheb.nug.noise;

import java.util.Random;

import org.resisttheb.nug.NumericUtilities;

/**
 * An n-dimensional Perlin noise generator.
 *
 *<p>References:
 *<a href="http://www.cs.cmu.edu/~mzucker/code/perlin-noise-math-faq.html">
 *http://www.cs.cmu.edu/~mzucker/code/perlin-noise-math-faq.html</a></p>
 */
public final class PerlinNoiseGenerator {
	private static final double _2_PI = 2 * Math.PI;
	
	
	public static final class NoiseEvalState {
		final int[][] corners;
		final float[] us;
		
		final float[] influences;
		final int[] grids;
		
		final float[] dv;
		
		
		NoiseEvalState(final int n, final int pown) {
			corners = new int[ n ][ 2 ];
			us = new float[ n ];
			
			influences = new float[ pown ];
			grids = new int[ n ];
			
			dv = new float[ n ];
		}
	}
	
	
	/**
	 * @see S_CURVE
	 * @see SIGMOID
	 * @see COS
	 */
	public static interface SmoothingFunction {
		public float eval(final float u);
		
		
		/**
		 * One of the Hermite interpolants;
		 * the most common smoothing function I've seen with Perlin noise.
		 */
		public static final SmoothingFunction S_CURVE = new SmoothingFunction() {
			public float eval(final float u) {
				return u * u * ( 3 - 2 * u );				
			}
		};
	}
	
	
	/**
	 * Random number generator resolution. We will have this many unique outputs. 
	 */
	private static final int B = 0x1000;
	
	
	private static float rfloat(final Random r) {
		return (float) ( ( r.nextInt() % ( B + B ) ) - B ) / B;
	}
	
	
	private static int[] populateShifts(final int[] p) {
		for ( int i = 0; p.length > i; i++ ) {
			p[ i ] = (int) ( Math.random() * p.length );
		}
		return p;
	}
	
	private static float[][] populateGradients1(final Random r, final float[][] g) {
		for ( int i = 0; g.length > i; i++ ) {
			g[ i ][ 0 ] = rfloat( r );
		}
		return g;
	}
	
	/**
	 * Generates random <code>n</code>-dimensional unit vectors
	 * to fill <code>g</code>.
	 * This algorithm uses <code>n - 1</code> dimensions of support for each vector
	 * (by constructing circles (n = 2), spheres (n = 3), and hyper-spheres (n &gt;= 4)).
	 * 
	 * @param n   Must be &gt;= 2
	 */
	private static float[][] populateGradients(final Random r, final float[][] g, final int n) {
		if ( n <= 1 ) {
			throw new IllegalArgumentException( "Can only handle unit gradients." );
		}
		
		final int d = n - 1;
		final double[] 
			ts = 	new double[ d ], 
			coss = 	new double[ d ], 
			sins = 	new double[ d ];
		for ( int i = 0; g.length > i; i++ ) {
			for ( int j = 0; ts.length > j; j++ ) {
				final double t = _2_PI * rfloat( r );
				ts[ j ] = t;
				coss[ j ] = Math.cos( t );
				sins[ j ] = Math.sin( t );
			}
			
			// Dimension k is
			// sin(k - 1) * prod( m = n - 2 to k / 2 + 1, cos( m) )
			
			final float[] gi = g[ i ];
			for ( int k = 0; n > k; k++ ) {
				double gk = k >= 1 ? sins[ k - 1 ] : 1.0;
				for ( int m = k; d > m; m++ )
					gk *= coss[ m ];
				gi[ k ] = (float) gk;
				
				// DEBUGGING: {
//					logger.debug( "  [populateGradients] g_k = " + gk );
				// }
			}
			
			assert NumericUtilities.fequal( 1.f, NumericUtilities.mag( gi ) );
		}
		return g;
	}
	
	
	private final int n, pown;
	private final int m, lgm, mmodmask;
	private final int[] p;
	private final float[][] g;
	
	
	public PerlinNoiseGenerator(final int n, final int lgm) {
		if ( lgm <= 0 ) {
			throw new IllegalArgumentException();
		}
		if ( n <= 0 ) {
			throw new IllegalArgumentException( "Can only generate noise for >= 1 dimension." );
		}
		
		this.n = n;
		this.pown = 0x01 << n;
		
		this.m = 0x01 << lgm;
		this.lgm = lgm;			
		mmodmask = ( 0x01 << lgm ) - 1;
		
		p = populateShifts( new int[ m ] );
		
		final Random r = new Random();
		g = 1 == n ? populateGradients1( r, new float[ m ][ 1 ] )
				: populateGradients( r, new float[ m ][ n ], n );
	}
	
	
	public NoiseEvalState createEvalState() {
		return new NoiseEvalState( n, pown );
	}
	
	
	public float noise(final int octave, final SmoothingFunction sf, final float ... coords) {
		return noise( octave, createEvalState(), sf, coords );
	}
	
	public float noise(final int octave, final NoiseEvalState nes, final SmoothingFunction sf, final float ... coords) {
		final int m = 0x01 << octave;
		for ( int i = 0; coords.length > i; i++ )
			coords[ i ] /= m;
		return noise( nes, sf, coords );
	}
	
	public float noise(final float octave, final SmoothingFunction sf, final float ... coords) {
		return noise( octave, createEvalState(), sf, coords );
	}
	
	public float noise(final float octave, final NoiseEvalState nes, final SmoothingFunction sf, final float ... coords) {
		final float m = (float) Math.pow( 2, octave );
		for ( int i = 0; coords.length > i; i++ )
			coords[ i ] /= m;
		return noise( nes, sf, coords );
	}
	
	public float noise(final SmoothingFunction sf, final float ... coords) {
		return noise( createEvalState(), sf, coords );
	}
	
	
//	private float[] grad(final int[] grids) {
//		assert grids.length == n : "Illegal coordinates; expected (" + n + ")";
//		
//		int index = 0;
//		for ( int i = n - 1; 0 <= i; i-- ) {
//			index = p[ ( index + grids[ i ] ) & mmodmask ];
//		}
//		return g[ index ];
//	}
	
	public float noise(final NoiseEvalState nes, final SmoothingFunction sf, final float ... coords) {
		assert coords.length == n : "Illegal coordinates; expected (" + n + ")";
		
//		if ( coords.length != n ) {
//			return 0.f;
//		}
		
		// There are <code>pown</code> gradients to the center
		// This scan algorithm repeatedly averages together the front and back,
		//   with step size <code>2</code>, <code>4</code>, <code>8</code>, etc.
		// The first step size corresponds to the dimension of <code>coords[ n - 1 ]</code> --
		//   this is clearer if you consider each bit position of <code>i</code> below as representing 
		//   either "front" (1) or "back" (0) of a particular dimension
		
		/// [orginial notes] {{{
		// 		scan algorithm:
		// 		start with smallest step size. average (using interpolation 0), store in front index
		// 		repeat with next largest step size (*2); average fronts (using interpolation 1), store in front;
		// 		repeat ...
		// }}}
		
		final int[][] corners = nes.corners;
		final float[] us = nes.us;			
		final float[] influences = nes.influences;
		final int[] grids = nes.grids;
		final float[] dv = nes.dv;
		
		
		for ( int i = 0; n > i; i++ ) {
			final float c = coords[ i ];
			final int g0 = (int) c;
			
			final int[] corner = corners[ i ];
			corner[ 0 ] = g0;
			corner[ 1 ] = 1 + g0;
			us[ i ] = sf.eval( c - g0 );
		}
		
		// Initialize influences:
		for ( int i = 0; pown > i; i++ ) {
			// The bits of <code>i</code> indicate the corner --
			for ( int j = 0; n > j; j++ ) {
				final int g = corners[ j ][ ( i >> j ) & 0x01 ];
				grids[ j ] = g;
				dv[ j ] = coords[ j ] - g;
			}
			
			
			
			//final float[] g = grad( grids );
			assert grids.length == n : "Illegal coordinates; expected (" + n + ")";
			
			
			int index;
			switch (n) {
			case 3: index = p[ ( p[ ( p[ ( grids[ 2 ] ) & mmodmask ] + grids[ 1 ] ) & mmodmask ] + grids[ 0 ] ) & mmodmask ];
				break;
			case 2: index = p[ ( p[ ( grids[ 1 ] ) & mmodmask ] + grids[ 0 ] ) & mmodmask ];
				break;
			case 1: index = p[ ( grids[ 0 ] ) & mmodmask ];
				break;
			default:
				index = 0;
				for ( int j = n - 1; 0 <= j; j-- ) {
					index = p[ ( index + grids[ j ] ) & mmodmask ];
				}
				break;
			}
			final float[] g = this.g[ index ];
			
			
			
			// Inner product:
			float ip;
			switch (n) {
			case 3: ip = dv[ 2 ] * g[ 2 ] + dv[ 1 ] * g[ 1 ] + dv[ 0 ] * g[ 0 ];
				break;
			case 2: ip = dv[ 1 ] * g[ 1 ] + dv[ 0 ] * g[ 0 ];
				break;
			case 1: ip = dv[ 0 ] * g[ 0 ];
				break;
			default:
				ip = 0.f;
				for ( int j = 0; n > j; j++ )
					ip += dv[ j ] * g[ j ];
				break;
			}
			// Note: do NOT normalize the <code>ip</code> vector!
			
//				assert -1.01f <= ip && ip <= 1.01f;
			
			// DEBUGGING: {
//				logger.debug( "  [noise] ip: " + ip );
			// }
			
			influences[ i ] = ip;
		}
		
		// Scan average:
		int step = 2;
		int halfStep = 1;
		for ( int d = 0; n > d; d++, halfStep = step, step <<= 1 ) {
			final float u = us[ d ];
			for ( int i = 0; pown > i; i += step ) {
//				final float a = influences[ i ];
//				final float b = influences[ i + halfStep ];
				influences[ i ] += (influences[ i + halfStep ] - influences[ i ]) * u;
//				influences[ i ] = lerp( influences[ i ], influences[ i + halfStep ],
//						us[ d ] );
			}
		}
		
		final float noise = influences[ 0 ];
		
		assert -1.01f <= noise && noise <= 1.01f 
			: "Noise is out of bounds. The calculations must be wrong.";
		
		// max( -1.f, min( 1.f, noise ) )
		return noise < -1.f ? -1.f : noise > 1.f ? 1.f : noise;
	}
	
	/**
	 * Returns noise that is periodic in the highest dimension with period <code>p</code>.
	 */
	public float periodicNoise(final float p, final SmoothingFunction sf, final float ... coords) {
		final float 
			hd = coords[ coords.length - 1 ],
			invhd = hd - p;
		final float noise0 = noise( sf, coords );
		coords[ coords.length - 1 ] = invhd;
		final float noise1 = noise( sf, coords );
		return ( invhd * noise0 + hd * noise1 ) / p;
	}
}