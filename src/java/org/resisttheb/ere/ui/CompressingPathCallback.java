package org.resisttheb.ere.ui;

import org.resisttheb.ere.ui.PathCodecs.PathCallback;



// takes small runs along principal axes
// and converts them to larger runs
public class CompressingPathCallback implements PathCallback {
	private float sx = 0.f;
	private float sy = 0.f;
	private float ex = 0.f;
	private float ey = 0.f;
	
	private PathCallback delegate;
	
	
	public CompressingPathCallback(final PathCallback _delegate) {
		this.delegate = _delegate;
	}
	
	
	
	private void push() {
		if (sx == ex && sy == ey) {
			// Nothing
			return;
		}
		delegate.lineTo(ex, ey);
		sx = ex;
		sy = ey;
	}
	
	private void pushOrAccum(final float x, final float y) {
		if (x != sx && y != sy) {
			push();		
		}
		ex = x;
		ey = y;
	}
	
	
	/**************************
	 * PATHCALLBACK IMPLEMENTATION
	 **************************/
	
	public void startPolygon(final int contourCount) {
		delegate.startPolygon(contourCount);
	}
	
	public void endPolygon() {
		delegate.endPolygon();
	}
	
	public void startContour(final int lineCount) {
		// Note: this is a max bound
		delegate.startContour(lineCount);
	}
	
	public void endContour() {
		push();
		delegate.endContour();
	}
	
	public void moveTo(final float x0, final float y0) {
		push();
		ex = sx = x0;
		ey = sy = y0;
		delegate.moveTo(x0, y0);
	}
	
	public void lineTo(final float x, final float y) {
		pushOrAccum(x, y);
	}
	
	/**************************
	 * END PATHCALLBACK IMPLEMENTATION
	 **************************/
}
