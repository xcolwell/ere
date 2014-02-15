package org.resisttheb.nug;

// TODO: all NUH code should use this numeric utils ... 
// TODO: NUG is actually NUGN (nu graphics and numerics)
public class NumericUtilities {
	
	public static final float
		E = (float) Math.E,
		PI = (float) Math.PI,
		HALF_PI = PI / 2.f,
		_2_PI = (float) ( 2 * Math.PI );
	
	private static final int DEFAULT_NR_MAX_ITR = 32;
	
	
	// .1% tolerance
	public static final float DEFAULT_FLOAT_EQ_EPSILON = .001f;
	
	
	public static boolean fequal(final float a, final float b) {
		return fequal( a, b, DEFAULT_FLOAT_EQ_EPSILON );
	}
	
	public static boolean fequal(final float a, final float b, final float epsilon) {
		return ( a > b ? a - b : b - a ) < epsilon;
	}
	
	
	public static final double DEFAULT_DOUBLE_EQ_EPSILON = .001;
	
	
	public static boolean dequal(final double a, final double b) {
		return dequal( a, b, DEFAULT_DOUBLE_EQ_EPSILON );
	}
	
	public static boolean dequal(final double a, final double b, final double epsilon) {
		return ( a > b ? a - b : b - a ) < epsilon;
	}
	
	
	public static float mag(final float ... vs) {
		float m = 0;
		for ( float v : vs )
			m += v * v;
		return fsqrt( m );
	}
	
	
	public static float fsqrt(final float a) {
		return (float) Math.sqrt( a );
	}
	
	public static float fexp(final float a) {
		return (float) Math.pow( E, a );
	}
	
	public static float fpow(final float a, final float p) {
		return (float) Math.pow( a, p );
	}
	
	
	public static boolean isNonStrictlyAscending(final float[] values) {
		return isNonStrictlyAscending( values, 0, values.length );
	}
	
	public static boolean isNonStrictlyAscending(final float[] values, final int offset, final int length) {
		for ( int i = offset + 1; i < offset + length; i++ ) {
			if ( values[ i ] < values[ i - 1 ] )
				return false;
		}
		return true;
	}
	
	
	
	// Fourth degree approx. 
	private static final int ERF_M_TERMS = 4;
	
	/*
	// computes the standard error function ("guass error function") at the given argument
	public static float erf(final float x) {
		// need to separate into pos/neg series ... approx. each separately (to degree N) 
		
		if ( x <= -2 )
			return -1;
		else if ( x >= 2 )
			return 1;
		else {
			// approximate to M terms on each series
		}
	}
	*/
	
	
	
	
	
	public static int fcompare(final float a, final float b) {
		return (int) Math.signum( a - b );
	}
	
	
	
	
	public static int mod(final int i, final int n) {
		assert 0 < n;
		return 0 <= i ? i % n : n - ( -i % n );
	}
	
	public static int mod(final int i, final int n0, final int n1) {
		// n0 + [ ( i - n0 ) mod ( n1 - n0 ) ]
		assert 0 <= n0;
		assert n0 < n1;
		return n0 + mod( i - n0, n1 - n0 );
	}
	
	
	public static float mod(final float i, final float n) {
		assert 0 < n && !fequal( n, 0.f );
		return 0 <= i ? i % n : n - ( -i % n );
	}
	
	public static float mod(final float i, final float n0, final float n1) {
		// n0 + [ ( i - n0 ) mod ( n1 - n0 ) ]
		assert 0 <= n0;
		assert n0 < n1;
		return n0 + mod( i - n0, n1 - n0 );
	}
	
	
	public static float norm(final float t) {
		return mod( t, _2_PI );
	}
	
	
	public static float radsToDegs(final float rads) {
		return rads * ( 180.f / PI );
	}
	
	public static float degsToRads(final float degs) {
		return degs * ( PI / 180.f );
	}
	
	// returns an int on [fvalue_min * res, fvalue_max * res]
	public static int floatToInt(final float fvalue, final int res) {
		return (int) ( fvalue * res );
	}
	
	public static float intToFloat(final int ivalue, final int res) {
		return (float) ivalue / res;
	}
	
	
	private NumericUtilities() {
	}
	
	
	
	public static class TrendAngleAccumulator {
		private float sx, sy, sxx, sxy;
		private int n;
		
		public TrendAngleAccumulator() {
			reset();
		}
	
		
		public float getAngle() {
			return (float) Math.atan2( n * sxy - sx * sy, n * sxx - sx * sx );
		}
		
		
		public void add(final float x, final float y) {
			sx += x;
			sy += y;
			sxx += x * x;
			sxy += x * y;
			n++;
		}
		
		public void reset() {
			sx = sy = sxx = sxy = 0.f;
			n = 0;
		}
	}
	
	
	
	
	public static boolean vfequal(final float[] a, final float[] b) {
		final int n = Math.min( a.length, b.length );
		for ( int i = 0; i < n; i++ ) {
			if (! fequal( a[ i ], b[ i ] ) )
				return false;
		}
		return true;
	}
}
