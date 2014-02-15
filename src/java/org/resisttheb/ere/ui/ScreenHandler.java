package org.resisttheb.ere.ui;

public interface ScreenHandler {
	// resets back to the first screen
	public void requestReset();
	
	public void finish(final Object ... outPackets);
}
