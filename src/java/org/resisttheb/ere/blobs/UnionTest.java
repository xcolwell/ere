package org.resisttheb.ere.blobs;


public interface UnionTest {
	public static final UnionTest EQUALITY = new UnionTest() {
		public boolean union(final byte r0, final byte g0, final byte b0,
				final byte r1, final byte g1, final byte b1) {
			return r0 == r1 && g0 == g1 && b0 == b1;
		}
	};
	
	
	public static final UnionTest NOOP = new UnionTest() {
		public boolean union(final byte r0, final byte g0, final byte b0,
				final byte r1, final byte g1, final byte b1) {
			return false;
		}
	};
	
	public static final UnionTest ALL = new UnionTest() {
		public boolean union(final byte r0, final byte g0, final byte b0,
				final byte r1, final byte g1, final byte b1) {
			return true;
		}
	};
	
	
	public boolean union(
		final byte r0, final byte g0, final byte b0,
		final byte r1, final byte g1, final byte b1
	);
}
