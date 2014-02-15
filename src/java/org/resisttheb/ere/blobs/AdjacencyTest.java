package org.resisttheb.ere.blobs;



// order or "a" and "b" should stanard ordinal
public interface AdjacencyTest {
	public static final AdjacencyTest _4 = new AdjacencyTest() {
		@Override
		public boolean crossXAdjacent(final Column a, final Column b) {
			return b.y0 < a.y1 && a.y0 < b.y1;
		}
		
		@Override
		public boolean crossYAdjacent(final Column a, final Column b) {
			return a.y0 == b.y1 || a.y1 == b.y0;
		}
	};
	
	public static final AdjacencyTest _8 = new AdjacencyTest() {
		@Override
		public boolean crossXAdjacent(final Column a, final Column b) {
			return b.y0 <= a.y1 && a.y0 <= b.y1;
		}
		
		@Override
		public boolean crossYAdjacent(final Column a, final Column b) {
			return a.y0 == b.y1 || a.y1 == b.y0;
		}
	};
	
	
	public boolean crossXAdjacent(final Column a, final Column b);
	public boolean crossYAdjacent(final Column a, final Column b);
}
