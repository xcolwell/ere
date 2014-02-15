package org.resisttheb.ere.blobs;

import java.awt.Color;

public interface Pixels {
	public int x0();
	public int x1();
	public int y0();
	public int y1();
	public boolean px(final int x, final int y);
	// if rgb is null, does not capture color value
	public boolean px(final int x, final int y, final int[] rgb);
	
	public boolean isOrdinalReadOptimized();
}
