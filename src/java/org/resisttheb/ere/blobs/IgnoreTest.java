package org.resisttheb.ere.blobs;


public interface IgnoreTest {
	public static final IgnoreTest NOOP = new IgnoreTest() {
		@Override
		public boolean ignore(final int color) {
			return false;
		}
	};
	
	
	public boolean ignore(final int color);
}
