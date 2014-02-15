package org.resisttheb.ere.blobs;

import java.awt.Color;

public interface WritablePixels extends Pixels {
	
	public boolean setPx(final int x, final int y, final int rgb);
	
	// optimized for ordinal access.
	public boolean isOrdinalWriteOptimized();
}
