package org.resisttheb.ere.blobs;


// based on columns. can be re-used. see #reserve(WIDTH)
// OPTIMIZED for ordinal access, e.g.
//    x ascending, y ascending
public class BlobPixels implements Pixels {

	private PseudoIndexedBlob pib;
	
	// The last accessed column:
	private Column col = null;
	
	
	public BlobPixels() {
	}
	
	
	public void setData(final PseudoIndexedBlob _pib) {
		this.pib = _pib;
	}
	
	
	// returns the last column accessed
	public Column getLastColumn() {
		return col;
	}
	
	public Column[] getColumnsModifiable() {
		return pib.cols;
	}
	
	
	/**************************
	 * PIXELS IMPLEMENTATION
	 **************************/
	
	public int x0() { return pib.x0; }
	public int x1() { return pib.x1; }
	public int y0() { return pib.y0; }
	public int y1() { return pib.y1; }
	
	public boolean px(final int x, final int y, final int[] rgb) {
		if (px(x, y)) {
			if (null != rgb) rgb[0] = pib.repr;
			return true;
		}
		return false;
	}
	
	public boolean px(final int x, final int y) {
		if (x < pib.x0 || pib.x1 <= x) return false;
		if (y < pib.y0 || pib.y1 <= y) return false;
		
		final int j = x - pib.x0;
		final int[] stats = pib.colStates[j];
		if (stats[2] < 0) {
			// TODO: binary search this beast
			stats[2] = stats[1];
		}
		
		final Column[] cols 		= pib.cols;
		final int[][] colStates 	= pib.colStates;
		
		col = cols[stats[2]];
		
		// Linear seekers -- work best for small movements ...
		if (y < col.y0) {
			final int min = colStates[j][0];
			while (min < stats[2]) {
				--stats[2];
				if ((col = cols[stats[2]]).y0 <= y) {
					return y < col.y1;
				}
			}
		}
		else if (col.y1 <= y) {
			final int max = colStates[j][1];
			while (stats[2] < max) {
				++stats[2];
				if (y < (col = cols[stats[2]]).y1) {
					return col.y0 <= y;
				}
			}
		}
		else {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean isOrdinalReadOptimized() {
		// Yes!
		return true;
	}
	
	/**************************
	 * END PIXELS IMPLEMENTATION
	 **************************/
}
