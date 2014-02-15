package org.resisttheb.ere.ui;

import javax.swing.JComponent;

public interface Screen {
	public JComponent getDisplay();
	
	public void init(final Object ... inPackets);
	public void start();
	public void stop();
	public void dispose();
	
	public void setScreenHandler(final ScreenHandler sh);
}
