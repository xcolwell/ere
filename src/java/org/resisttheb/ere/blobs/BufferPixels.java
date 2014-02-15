package org.resisttheb.ere.blobs;

import java.awt.image.BufferedImage;

public class BufferPixels implements WritablePixels {
	private final BufferedImage buffer;
	
	private final int dx;
	private final int dy;
	
	
	public BufferPixels(final BufferedImage _buffer) {
		this(_buffer, 0, 0);
	}
	
	public BufferPixels(final BufferedImage _buffer, final int _dx, final int _dy) {
		if (null == _buffer)
			throw new IllegalArgumentException();
		this.buffer = _buffer;
		this.dx = _dx;
		this.dy = _dy;
	}
	
	
	/**************************
	 * PIXELS IMPLEMENTATION
	 **************************/
	
	public int x0() { return 0; }
	public int x1() { return buffer.getWidth(); }
	public int y0() { return 0; }
	public int y1() { return buffer.getHeight(); }
	
	public boolean px(final int x, final int y, final int[] rgb) {
		if (px(x, y)) {
			if (null != rgb) rgb[0] = buffer.getRGB(x + dx, y + dy);
			return true;
		}
		return false;
	}
	
	public boolean px(final int x, final int y) {
		return 0 <= x + dx && x + dx < buffer.getWidth() &&
			0 <= y + dy && y + dy < buffer.getHeight();
	}
	
	public boolean isOrdinalReadOptimized() {
		return false;
	}
	
	/**************************
	 * END PIXELS IMPLEMENTATION
	 **************************/
	
	/**************************
	 * WRITABLEPIXELS IMPLEMENTATION
	 **************************/
	
	public boolean setPx(final int x, final int y, final int rgb) {
		if (! px(x, y)) 
			return false;
		buffer.setRGB(x + dx, y + dy, rgb);
		return true;
	}
	
	public boolean isOrdinalWriteOptimized() {
		return false;
	}
	
	/**************************
	 * END WRITABLEPIXELS IMPLEMENTATION
	 **************************/
}
