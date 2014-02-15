package org.resisttheb.ere.blobs;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;


// uses simple kmeans
// 
public class ClusterQuantizer implements PixelProcessor {
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static int centroid(final TIntHashSet set) {
		final int count = set.size();
		if (count <= 0)
			return 0x00;
		
		int netr = 0;
		int netg = 0;
		int netb = 0;
		for (TIntIterator itr = set.iterator(); itr.hasNext(); ) {
			final int color = itr.next();
			netr += color & 0xFF;
			netg += (color >> 8) & 0xFF;
			netb += (color >> 16) & 0xFF;
		}
		
		final int r = (netr / count);
		final int g = (netg / count);
		final int b = (netb / count);
		return r | (g << 8) | (b << 16) | (0xFF << 24);
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	private int maxItr = 8;
	private int k;
	
	
	public ClusterQuantizer(final int _k) {
		this.k = _k;
	}
	
	
	public void setMaxIterations(final int _maxItr) {
		this.maxItr = _maxItr;
	}
	
	
	@Override
	public void process(final Pixels src, final WritablePixels dst) {
		final int px_x0 = src.x0();
		final int px_y0 = src.y0();
		final int px_x1 = src.x1();
		final int px_y1 = src.y1();
		
		final int px_w = px_x1 - px_x0;
		final int px_h = px_y1 - px_y0;
		
		final TIntHashSet colors = new TIntHashSet(px_w * px_h / 32);
		
//		final Color[] rgb = {null};
		final int[] rgb = new int[1];
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1; ++y) {
				if (! src.px(x, y, rgb))
					continue;
				
				colors.add(rgb[0]);
			}
		}
		
		
		boolean centroidChanged 		= false;
		final int[] centroids 		= new int[k];
		final TIntHashSet[] clusters 	= new TIntHashSet[k];
		for (int i = 0; i < k; ++i) {
			clusters[i] = new TIntHashSet(colors.size() * 2 / k);
		}
		
		// Initial centroids:
		{
			int i = 0;
			for (TIntIterator itr = colors.iterator(); itr.hasNext() && i < k; i++) {
				centroids[i] = itr.next();
			}
			for (; i < k; ++i) {
				centroids[i] = centroids[i - 1];
			}
		}
		
		int itr = 0;
		do {
			centroidChanged = false;
			
			for (int i = 0; i < k; ++i) {
				clusters[i].clear();
			}
			
			// Assign clusters:
			for (TIntIterator citr = colors.iterator(); citr.hasNext();) {
				final int color = citr.next();
				
				final int cr = color & 0xFF;
				final int cg = (color >> 8) & 0xFF;
				final int cb = (color >> 16) & 0xFF;
				
				int mini = 0;
				int mind = Integer.MAX_VALUE;
					//BlobUtilities.distance(centroids[mini], color);
				for (int i = 0; i < k; ++i) {
					final int centroid = centroids[i];
					final int tr = centroid & 0xFF;
					final int tg = (centroid >> 8) & 0xFF;
					final int tb = (centroid >> 16) & 0xFF;
					
					final int dr = cr - tr;
					final int dg = cg - tg;
					final int db = cb - tb;
					
					final int d = dr * dr + dg * dg + db * db;
					
						//BlobUtilities.distance(centroids[i], color);
					if (d < mind) {
						mini = i;
						mind = d;
					}
				}
				
				clusters[mini].add(color);
			}
			
			// Recompute centroids:
			for (int i = 0; i < k; i++) {
				if (clusters[i].size() <= 0)
					continue;
				//final Color ncentroid = BlobUtilities.centroid(clusters[i]);
				int ncentroid = centroid(clusters[i]);
				centroidChanged |= centroids[i] != ncentroid;
					//!centroids[i].equals(ncentroid);
				centroids[i] = ncentroid;
			}
		}
		while (centroidChanged && ++itr < maxItr);
		
		// At this point, <code>centroids</code> contains our centroids;
		// and <code>clusters</code> contains all input colors
		
//		for (int centroid : centroids) {
//			final Color color = new Color(centroid);
//			System.out.println("r: " + color.getRed());
//			System.out.println("g: " + color.getGreen());
//			System.out.println("b: " + color.getBlue());
//		}
		
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1; ++y) {
				if (! src.px(x, y, rgb))
					continue;
				
//				final Color color = rgb[0];
				final int color = rgb[0];
//				Color useColor = null;
				int useColor = 0;
				
				for (int ci = 0; ci < k; ci++) {
					if (clusters[ci].contains(color)) {
						useColor = centroids[ci];
						break;
					}
				}
				
//				assert null != useColor;
//				if (null == useColor) continue;
				dst.setPx(x, y, useColor);
			}
		}
	}
}
