package org.resisttheb.ere.signature;

public class SignatureUtilities {

	
	private static final int NORMD = (3 * 255 * 255);
	
	public static float distance(final Signature a, final Signature b) {
		// a should have fewer elements
		
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
			final int ag = (acolor >> 0x08) & 0xFF;
			final int ar = (acolor >> 0x10) & 0xFF;
			final float asqweight = asqweights[ai];
			
			float mind = Integer.MAX_VALUE;
//			int minbi = -1;
			for (int bi = 0; bi < bcount; ++bi) {
				final int bcolor = bcolors[bi];
				int db = ab - (bcolor & 0xFF);
				int dg = ag - ((bcolor >> 0x08) & 0xFF);
				int dr = ar - ((bcolor >> 0x10) & 0xFF);
				dr = 0xFF + (dr < 0 ? dr : -dr);
				dg = 0xFF + (dg < 0 ? dg : -dg);
				db = 0xFF + (db < 0 ? db : -db);
				
				final float d = 
//					(
						asqweight * bsqweights[bi] * (dr * dr + dg * dg + db * db)
						
//						) / NORMD;
						;
				if (d < mind) {
					mind = d;
//					minbi = bi;
				}
			}
			
			net += mind;
		}
		return net / acount;
	}
	
	
	
	private SignatureUtilities() {
	}
}
