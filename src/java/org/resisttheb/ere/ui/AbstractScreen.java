package org.resisttheb.ere.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;

public abstract class AbstractScreen extends JPanel implements Screen {
	protected ScreenHandler sh;
	
	
	public AbstractScreen() {
	}
	
	
	/**************************
	 * SCREEN IMPLEMENTATION
	 **************************/

	public JComponent getDisplay() {
		return this;
	}
	
	public void setScreenHandler(final ScreenHandler _sh) {
		this.sh = _sh;
	}
	
	/**************************
	 * END SCREEN IMPLEMENTATION
	 **************************/

	
}
