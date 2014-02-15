package org.resisttheb.ere.blobs;


// TODO: quantizers are processors
public interface PixelProcessor {
	public void process(final Pixels src, final WritablePixels dst);
}
