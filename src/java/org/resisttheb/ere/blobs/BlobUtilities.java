package org.resisttheb.ere.blobs;

import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;


public class BlobUtilities {
	public static final Comparator<Column> COLUMN_COMPARATOR = 
		new Comparator<Column>() {
			@Override
			public int compare(final Column a, final Column b) {
				int d = a.x - b.x;
				if (0 != d)
					return d;
				d = a.y0 - b.y0;
				return d;
			}
		};
		
	public static final Comparator<BlobNode> AREA_COMPARATOR =
		new Comparator<BlobNode>() {
			@Override
			public int compare(final BlobNode a, final BlobNode b) {
				return a.getArea() - b.getArea();
			}
		};
		
	public static final Comparator<BlobNode> AREA_COMPARATOR_PRESERVE_ID =
		new Comparator<BlobNode>() {
			@Override
			public int compare(final BlobNode a, final BlobNode b) {
				int d = a.getArea() - b.getArea();
				if (0 != d)
					return d;
				d = a.getId() - b.getId();
				return d;
			}
		};
		
		

	// tests whether two blobs are touching
	// merges in time linear to blob size
	public static boolean isTouching(final AdjacencyTest at, 
			final Column[] _acols, final Column[] _bcols
		) {
		Column[] acols = _acols;
		Column[] bcols = _bcols;
		
		// Condition: b should have greater maxx than a
		if (acols[acols.length - 1].x < 
				bcols[bcols.length - 1].x
		) {
			acols = _bcols;
			bcols = _acols;
		}
		
		// alternate walking in x
		int ai = 0;
		int bi = 0;
		
		int x = Math.max(acols[0].x, bcols[0].x);
		
		// prepare ai and bi so that they start on the same x
		while (ai < acols.length && acols[ai].x < x) ai++;
		while (bi < bcols.length && bcols[bi].x < x) bi++;
		
		
		while (ai < acols.length && bi < bcols.length) {
			int ai0 = ai;
			// condition: a and b are on same x
			// merge on this x    CROSS_Y_ADJ
			// then rewind ai to ai0
			
			while (ai < acols.length && bi < bcols.length &&
					x == acols[ai].x && x == bcols[bi].x
			) {
				// advance a
				// advance b
				if (at.crossYAdjacent(acols[ai], bcols[bi])) {
					return true;
				}
				
				if (acols[ai].y0 <= bcols[bi].y0) ++ai;
				else ++bi;
			}
			
			if (! (ai < acols.length && bi < bcols.length)) 
				return false;
			
			ai = ai0;
			++x;
			while (bi < bcols.length && bcols[bi].x < x) ++bi;
			int bi0 = bi;
			
			if (! (ai < acols.length && bi < bcols.length)) 
				return false;
			
			
			// merge while b is on x+1    CROSS_X_ADJ
			while (ai < acols.length && bi < bcols.length &&
					x - 1 == acols[ai].x && x == bcols[bi].x
			) {
				// advance a
				// advance b
				if (at.crossXAdjacent(acols[ai], bcols[bi])) {
					return true;
				}
				
				if (acols[ai].y0 <= bcols[bi].y0) ++ai;
				else ++bi;
			}
			
			
			bi = bi0;
			while (ai < acols.length && acols[ai].x < x) ++ai;
		}
		
		return false;
	}
	
	
	// returns if sorted x- and y-
	public static boolean isOrdinal(final Column ... cols) {
		for (int i = 1; i < cols.length; ++i) {
			if (0 < COLUMN_COMPARATOR.compare(cols[i - 1], cols[i])) return false;
		}		
		return true;
	}
	
	
	
	public static double distance(final Color a, final Color b) {
		final int dr 	= a.getRed() - b.getRed();
		final int dg 	= a.getGreen() - b.getGreen();
		final int db 	= a.getBlue() - b.getBlue();
		final int da 	= a.getAlpha() - b.getAlpha();
		return Math.sqrt(dr * dr + dg * dg + db * db + da * da);
	}
	
	public static Color centroid(final Collection<Color> colors) {
		int netr = 0;
		int netg = 0;
		int netb = 0;
		int neta = 0;
		for (Color color : colors) {
			netr += color.getRed();
			netg += color.getGreen();
			netb += color.getBlue();
			neta += color.getAlpha();
		}
		final int n = colors.size();
		return new Color(
			(int) Math.round(netr / (double) n), 
			(int) Math.round(netg / (double) n),
			(int) Math.round(netb / (double) n), 
			(int) Math.round(neta / (double) n)
		);
	}
}
