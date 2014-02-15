package org.resisttheb.ere.signature;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;

import java.util.Arrays;
import java.util.Comparator;

import org.resisttheb.ere.blobs.BlobGraph;
import org.resisttheb.ere.blobs.BlobNode;

// computes signatures with weights based on area
public class AreaSignatureFactory_SickMomma implements SignatureFactory {
	// the signature size this factory creates
	private final int size;
	
	
	public AreaSignatureFactory_SickMomma(final int _size) {
		this.size = _size;
	}

	
	/**************************
	 * SIGNATUREFACTORY IMPLEMENTATION
	 **************************/
	
	public Signature generate(final BlobGraph graph) {
		// take the colors with the top /size/ area
		// weight on total represented area
		// so that sum(weights)=1
		
		// map rgb->area
		// 
		
		final BlobNode[] blobs = graph.getBlobsModifiable();
		final TIntIntHashMap rgbToAreaMap = new TIntIntHashMap(blobs.length);
		
		for (BlobNode blob : blobs) {
			final int rgb = blob.getRepresentative();
//			if (! SickMommaUtilities.isFilterGrey(rgb)) {
				rgbToAreaMap.put(rgb, rgbToAreaMap.get(rgb) + blob.getArea());
//			}
		}
		
		final int[][] entries = new int[rgbToAreaMap.size()][2];
		final int[] ei = {0};
		rgbToAreaMap.forEachEntry(new TIntIntProcedure() {
			public boolean execute(final int k, final int v) {
				final int[] entry = entries[ei[0]++];
				entry[0] = k;
				entry[1] = v;
				return true;
			}
		});
//		final Map.Entry<Integer, Integer>[] entries = 
//			rgbToAreaMap.entrySet().toArray((Map.Entry<Integer, Integer>[]) 
//					new Map.Entry[rgbToAreaMap.size()]);
//		
		// Sort ids based on area:
		Arrays.sort(entries, new Comparator<int[]>() {
			@Override
			public int compare(final int[] a, final int[] b) {
				final int rgba = a[0];
				final int rgbb = b[0];
				final int areaa = a[1];
				final int areab = b[1];
				
				final boolean fga = SickMommaUtilities.isFilterGrey(rgba);
				final boolean fgb = SickMommaUtilities.isFilterGrey(rgbb);
				
				if (fga && !fgb) {
					return 1;
				}
				if (!fga && fgb) {
					return -1;
				}
				
				int d = areaa - areab;
				// Note: /descending/ area
				if (0 != d)
					return -d;
				d = rgba - rgbb;
				return d;
			}
		});
		
		
		final int n = Math.min(size, entries.length);
		
		int netArea = 0;
		for (int i = 0; i < n; ++i) {
			netArea += entries[i][1];
		}
		
		final int[] rgbs = new int[n];
		final float[] weights = new float[n];
		
		for (int i = 0; i < n; ++i) {
			final int[] entry = entries[i];
			rgbs[i] = entry[0];
			weights[i] = entry[1] / (float) netArea;
		}
		
		return new Signature(n, rgbs, weights);
	}
	
	/**************************
	 * END SIGNATUREFACTORY IMPLEMENTATION
	 **************************/
}
