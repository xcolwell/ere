package org.resisttheb.ere.ui3;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.resisttheb.nug.GraphicsUtilities;
import org.resisttheb.nug.NumericUtilities;

public class CurveUtilities {
	public static void eval3(final float[] bounds, final float u, final float[] out) {
		final float v = 1.f - u;
		
		final float vsq = v * v;
		final float usq = u * u;
		final float a = v * vsq;
		final float b = 3 * u * vsq;
		final float c = 3 * usq * v;
		final float d = u * usq;
		
		out[0] = a * bounds[0] + b * bounds[2] + c * bounds[4] + d * bounds[6];
		out[1] = a * bounds[1] + b * bounds[3] + c * bounds[5] + d * bounds[7];
	}
	
	public static Point2D.Float eval3(final float[] bounds, final float u, final float[] out, final Point2D.Float point) {
		eval3(bounds, u, out);
		point.x = out[0];
		point.y = out[1];
		return point;
	}
	
	public static float search(final boolean up, 
			final float[] bounds, final Rectangle2D.Float box) {
		final float[] out = new float[2];
		final Point2D.Float point = new Point2D.Float();
		
		float u;
		final float ustep0;
		final float ustep1;
		final float ustep2;
		if (up) {
			u = 0.f;
			ustep0 = 0.1f;
			ustep1 = 0.01f;
			ustep2 = 0.001f;
		}
		else {
			u = 1.f;
			ustep0 = -0.1f;
			ustep1 = -0.01f;
			ustep2 = -0.001f;
		}
		
		while (box.contains(eval3(bounds, u, out, point))) {
			u += ustep0;
		}
		u -= ustep1;
		while (! box.contains(eval3(bounds, u, out, point))) {
			u -= ustep1;
		}
		u += ustep2;
		while (box.contains(eval3(bounds, u, out, point))) {
			u += ustep2;
		}
		
		return u;
	}
	
	
	
	private static float[][] divide(final float[] _controls, final int k, final float u) {
		final float iu = 1.f - u;
		// pass while i <= k
		// controls[0] = controls[0]*iu + controls[2]*u
		// controls[2] = controls[2]*iu + controls[4]*u
		// then pass again while i <= k - 1, there is one block of 2 left
		// at each pass, record the first pair (left controls) and the last pair (right controls)
		// reverse the right controls
		
		final float[] controls = new float[ ( k + 1 ) << 1 ];
		System.arraycopy( _controls, 0, controls, 0, controls.length );
		
		int n;
		final float[] lcontrols = new float[ ( k + 1 ) << 1 ];
		final float[] rcontrols = new float[ ( k + 1 ) << 1 ];
		lcontrols[ 0 ] = controls[ 0 ];
		lcontrols[ 1 ] = controls[ 1 ];
		n = ( k ) << 1;
		rcontrols[ 0 ] = controls[ n ];
		rcontrols[ 1 ] = controls[ n + 1 ];
	
		// Run de Casteljau's interpolation, and record the boundary points (in <code>lcontrols</code> and <code>rcontrols</code>):
		for ( int j = 1; j <= k; j++ ) {
			for ( int i = 0; i <= k - j; i++ ) {
				n = i << 1;
				controls[ n ] = iu * controls[ n ] + u * controls[ n + 2 ];
				controls[ n + 1 ] = iu * controls[ n + 1 ] + u * controls[ n + 3 ];
			}
			int m = j << 1;
			lcontrols[ m ] = controls[ 0 ];
			lcontrols[ m + 1 ] = controls[ 1 ];
			n = ( k - j ) << 1;
			rcontrols[ m ] = controls[ n ];
			rcontrols[ m + 1 ] = controls[ n + 1 ];
		}
		
		GraphicsUtilities.reverseCoords( rcontrols, 0, k + 1 );
		return new float[][]{
			lcontrols,
			rcontrols
		};
	}
	
	
	public static float[][] split(final float[] controls, final int k, final float ... us) {
//		if (! NumericUtilities.isNonStrictlyAscending( us ) ) {
//			throw new IllegalArgumentException();
//		}
		
		final float[][] splitControls = new float[ us.length + 1 ][];
		float[] pcontrols = controls;
		float alpha = 1.f;
		float beta = 0.f;
		for ( int i = 0; i < us.length; i++ ) {
			final float u = us[ i ];
			final float[][] dcontrols = divide( pcontrols, k, alpha * u + beta );
			splitControls[ i ] = dcontrols[ 0 ];
			pcontrols = dcontrols[ 1 ];
			alpha = alpha / ( 1.f - u );
			beta = ( beta - u ) / ( 1.f - u );
		}
		splitControls[ us.length ] = pcontrols;
		
		return splitControls;
	}
	
	
	private CurveUtilities() {
	}
}
