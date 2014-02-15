package org.resisttheb.ere.signature;

import java.awt.Color;

public final class WeightedInverseMetric implements SignatureMetric {
	public WeightedInverseMetric() {
	}
	
	/**************************
	 * SIGNATUREMETRIC IMPLEMEMENTATION
	 **************************/

	public float distance(final Signature _a, final Signature _b) {
		final Signature a;
		final Signature b;
		if (_a.count < _b.count) {
			a = _a;
			b = _b;
		}
		else {
			a = _b;
			b = _a;
		}
		
		if (a.count <= 0) {
			return 0;
		}
		
		final int acount 			= a.count;
		final int[] acolors 		= a.colors;
		final float[] asqweights 	= a.sqweights;
		
		final int bcount 			= b.count;
		final int[] bcolors 		= b.colors;
		final float[] bsqweights 	= b.sqweights;
		
		float net = 0;
		for (int ai = 0; ai < acount; ++ai) {
			final int acolor = acolors[ai];
			final int ab = acolor & 0xFF;
			final int ag = (acolor >> 8) & 0xFF;
			final int ar = (acolor >> 16) & 0xFF;
			final float asqweight = asqweights[ai];
			
			float maxs = 0;
//			int minbi = -1;
			for (int bi = 0; bi < bcount; ++bi) {
				final int bcolor = bcolors[bi];
				int db = ab - (bcolor & 0xFF);
				int dg = ag - ((bcolor >> 8) & 0xFF);
				int dr = ar - ((bcolor >> 16) & 0xFF);
				dr = 0xFF + (dr < 0 ? dr : -dr);
				dg = 0xFF + (dg < 0 ? dg : -dg);
				db = 0xFF + (db < 0 ? db : -db);
				
				
				final float s = 
//					(
						/*asqweight * bsqweights[bi] * */ (dr * dr + dg * dg + db * db)
						
//						) / NORMD;
						;
				if (maxs < s) {
					maxs = s;
//					minbi = bi;
				}
			}
			
			net += Math.sqrt(maxs / 3);
		}
		return 1.f - (net / acount) / (float) 255;
	}
	
	/**************************
	 * END SIGNATUREMETRIC IMPLEMEMENTATION
	 **************************/
	
	
	public static void main(final String[] in) {
		final Signature sig0 = new Signature(2, 
				new int[]{new Color(255, 0, 0).getRGB(), new Color(255, 255, 255).getRGB()},
				new float[]{0.2f, 0.2f});
		final Signature sig1 = new Signature(1, 
				new int[]{new Color(255, 255, 255).getRGB()},
				new float[]{1.f});
		
		final WeightedInverseMetric m = new WeightedInverseMetric();
		System.out.println(m.distance(sig0, sig1));
	}
}
