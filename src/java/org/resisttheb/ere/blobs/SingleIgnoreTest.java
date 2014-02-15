package org.resisttheb.ere.blobs;


public class SingleIgnoreTest implements IgnoreTest {
	private int ignoreColor;
	
	public SingleIgnoreTest(final int _ignoreColor) {
//		if (null == _ignoreColor)
//			throw new IllegalArgumentException();
		this.ignoreColor = _ignoreColor;
	}
	
	/**************************
	 * IGNORETEST IMPLEMENTATION
	 **************************/
	
	public boolean ignore(final int color) {
		return ignoreColor == color;
	}
	
	/**************************
	 * END IGNORETEST IMPLEMENTATION
	 **************************/
}
