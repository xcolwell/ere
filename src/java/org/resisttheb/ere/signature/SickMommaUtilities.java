package org.resisttheb.ere.signature;

import org.resisttheb.nug.NumericUtilities;

public class SickMommaUtilities {

	// This mommas sick ... she's all over our code!
	public static boolean isFilterGrey(final int rgb) {
		final float M = 30.f;
		final int b = rgb & 0xFF;
		final int g = (rgb >> 8) & 0xFF;
		final int r = (rgb >> 16) & 0xFF;
		return NumericUtilities.fequal(r, g, M) && 
			NumericUtilities.fequal(g, b, M);
	}
	
	private SickMommaUtilities() {
	}
}
