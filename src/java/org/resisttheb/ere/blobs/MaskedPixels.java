package org.resisttheb.ere.blobs;


public class MaskedPixels implements Pixels {
	private final int x0;
	private final int y0;
	private final int x1;
	private final int y1;
	
	private final Pixels mask;
	private final Pixels data;
	
	
	public MaskedPixels(final Pixels _mask, final Pixels _data) {
		this.mask = _mask;
		this.data = _data;
		
		x0 = Math.max(mask.x0(), data.x0());
		y0 = Math.max(mask.y0(), data.y0());
		x1 = Math.min(mask.x1(), data.x1());
		y1 = Math.min(mask.y1(), data.y1());
	}
	
	
	/**************************
	 * PIXELS IMPLEMENTATION
	 **************************/
	
	public int x0() { return x0; }
	public int x1() { return x1; }
	public int y0() { return y0; }
	public int y1() { return y1; }
	
	public boolean px(final int x, final int y) {
		return mask.px(x, y) && data.px(x, y);
	}
	
	public boolean px(final int x, final int y, final int[] rgb) {
		return mask.px(x, y) && data.px(x, y, rgb);
	}
	
	@Override
	public boolean isOrdinalReadOptimized() {
		return mask.isOrdinalReadOptimized() || data.isOrdinalReadOptimized();
	}
	
	/**************************
	 * END PIXELS IMPLEMENTATION
	 **************************/
}
