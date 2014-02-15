package org.resisttheb.ere.blobs;


public interface AgglomerateSelector {
	public static final AgglomerateSelector NONE = new AgglomerateSelector() {
		public BlobNode select(final BlobNode blob) {
			return null;
		}
	};
	
	public static final AgglomerateSelector MIN_GREATER_AREA = new AgglomerateSelector() {
		public BlobNode select(final BlobNode blob) {
			final int area = blob.getArea();
			
			BlobNode min 		= null;
			int minArea 		= 0;
			for (BlobNode adj : blob.getAdjacentModifiable2()) {
				final int adjArea = adj.getArea();
				if (area <= adjArea && (null == min || minArea < area)) {
					min = adj;
					minArea = area;
				}
			}
			
			return min;
		}
	};
	
	public static final AgglomerateSelector CLOSEST_COLOR  = new AgglomerateSelector() {
		public BlobNode select(final BlobNode blob) {
			final byte[] cs = new byte[4];
			blob.getRepresentative(cs);
			final int r0 = cs[0] & 0xFF;
			final int g0 = cs[1] & 0xFF;
			final int b0 = cs[2] & 0xFF;
			
			BlobNode min 		= null;
			int minDistance 	= 0;
			for (BlobNode adj : blob.getAdjacentModifiable2()) {
				adj.getRepresentative(cs);
				final int r1 = cs[0] & 0xFF;
				final int g1 = cs[1] & 0xFF;
				final int b1 = cs[2] & 0xFF;
				
				final int dr = r0 - r1;
				final int dg = g0 - g1;
				final int db = b0 - b1;
				
				int distance = dr * dr + dg * dg + db * db;
				if (null == min || 
						distance < minDistance) {
					min = adj;
					minDistance = distance;
				}
			}
			
			return min;
		}
	};
	
	public static final AgglomerateSelector CLOSEST_COLOR_GREATER_AREA = new AgglomerateSelector() {
		public BlobNode select(final BlobNode blob) {
			final int area = blob.getArea();
			final byte[] cs = new byte[4];
			blob.getRepresentative(cs);
			final int r0 = cs[0] & 0xFF;
			final int g0 = cs[1] & 0xFF;
			final int b0 = cs[2] & 0xFF;
			
			BlobNode min 		= null;
			int minDistance 	= 0;
			for (BlobNode adj : blob.getAdjacentModifiable2()) {
				final int adjArea = adj.getArea();
				if (adjArea < area)
					continue;
				
				adj.getRepresentative(cs);
				final int r1 = cs[0] & 0xFF;
				final int g1 = cs[1] & 0xFF;
				final int b1 = cs[2] & 0xFF;
				
				final int dr = r0 - r1;
				final int dg = g0 - g1;
				final int db = b0 - b1;
				
				int distance = dr * dr + dg * dg + db * db;
				if (null == min || 
						distance < minDistance) {
					min = adj;
					minDistance = distance;
				}
			}
			
			return min;
		}
	};
	
	
	
	public BlobNode select(final BlobNode blob);
}
