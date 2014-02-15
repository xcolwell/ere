package org.resisttheb.ere.blobs;


// a good pre-step for a cluster quantizer,
// as the number of unique colors in an image can be the area,
// this will divide that by the bucket size
//
// 
public class BucketQuantizer implements PixelProcessor {
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static int bucket(final int color, final int m) {
		final int r = ((color & 0xFF) / m) * m;
		final int g = (((color >> 8) & 0xFF) / m) * m;
		final int b = (((color >> 16) & 0xFF) / m) * m;
		return r | (g << 8) | (b << 16) | (0xFF << 24);
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	private int m;
	
	
	public BucketQuantizer(final int _m) {
		this.m = _m;
	}
	
	
	public void process(Pixels src, WritablePixels dst) {
		final int px_x0 = src.x0();
		final int px_y0 = src.y0();
		final int px_x1 = src.x1();
		final int px_y1 = src.y1();
		
		final int px_w = px_x1 - px_x0;
		final int px_h = px_y1 - px_y0;
		
//		final Color[] rgb = {null};
		final int[] rgb = new int[1];
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1; ++y) {
				if (! src.px(x, y, rgb))
					continue;
				
				dst.setPx(x, y, bucket(rgb[0], m));
			}
		}
	}
}
