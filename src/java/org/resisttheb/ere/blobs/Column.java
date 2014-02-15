package org.resisttheb.ere.blobs;

import java.awt.Color;

// atomic piece of a blob
// represent a vertical run of pixels
// 
// NOT THREAD SAFE
// most algorithms operate and store state on columns
// 
// also note that the number of columns per pixel field
// is O(number of pixels), so the worst case is 1 column per pixel
// because of this we need to keep the size of each column minimal
public final class Column {
	public final short x;
	public final short y0;
	public final short y1;
	
	/**************************
	 * ALGORITHMIC STATE
	 **************************/
	
	// Fields for algorithms to use ...
	// more efficient to share here than to maintain an external mapping
	
	public Object alg_ref_0;
	
	/**************************
	 * END ALGORITHMIC STATE
	 **************************/
	
	
	public Column(final int _x, final int _y0, final int _y1) {
		this.x 		= (short) _x;
		this.y0 	= (short) _y0;
		this.y1 	= (short) _y1;
	}
}
