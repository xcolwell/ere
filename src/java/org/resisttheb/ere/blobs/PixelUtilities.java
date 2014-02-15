package org.resisttheb.ere.blobs;

import java.util.Arrays;

public class PixelUtilities {

	public static void onoff(final Pixels src, final WritablePixels dst, final int on, final int off) {
		final int px_x0 = src.x0();
		final int px_y0 = src.y0();
		final int px_x1 = src.x1();
		final int px_y1 = src.y1();
		
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1; ++y) {
				dst.setPx(x, y, src.px(x, y) ? on : off);
			}
		}
	}
	
	public void copy(final Pixels src, final WritablePixels dst) {
		final int px_x0 = src.x0();
		final int px_y0 = src.y0();
		final int px_x1 = src.x1();
		final int px_y1 = src.y1();
		
//		final Color[] rgb = {null};
		final int[] rgb = new int[1];
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1; ++y) {
				if (src.px(x, y, rgb))
					dst.setPx(x, y, rgb[0]);
			}
		}
	}
	
	public void copy(final BlobGraph src, final WritablePixels dst) {
		if (dst.isOrdinalWriteOptimized()) {
			// Assume  cost of non-ordinal writing is greater than sort
			// sort cheaper than n-way merge for unbounded n
			
			int ccount = 0;
			for (BlobNode blob : src.getBlobsModifiable()) {
				ccount += blob.getColumnsModifiable().length;
			}
			final Column[] allCols = new Column[ccount];
			int j = 0;
			for (BlobNode blob : src.getBlobsModifiable()) {
				final Column[] cols = blob.getColumnsModifiable();
				final int repr = blob.getRepresentative();
				for (Column col : cols) {
					col.alg_ref_0 = repr;
				}
				System.arraycopy(cols, 0, allCols, j, cols.length);
				j += cols.length;
			}
			
			Arrays.sort(allCols, BlobUtilities.COLUMN_COMPARATOR);
			
			for (int i = 0; i < allCols.length; ++i) {
				final Column col = allCols[i];
				final int x = col.x;
				final int y1 = col.y1;
				for (int y = col.y0; y < y1; ++y) {
					dst.setPx(x, y, (int) (Integer) col.alg_ref_0);
				}
			}
			
			// Cleanup:
			for (Column col : allCols) {
				col.alg_ref_0 = null;
			}
		}
		else {
			for (BlobNode blob : src.getBlobsModifiable()) {
				copy(blob, dst);
			}
		}
	}
	
	public void copy(final BlobNode src, final WritablePixels dst) {
		final Column[] cols = src.getColumnsModifiable();
		final int repr = src.getRepresentative();
		for (int i = 0; i < cols.length; ++i) {
			final Column col = cols[i];
			final int x = col.x;
			final int y1 = col.y1;
			for (int y = col.y0; y < y1; ++y) {
				dst.setPx(x, y, repr);
			}
		}
	}
	
	
	
	
	private PixelUtilities() {
	}
}
