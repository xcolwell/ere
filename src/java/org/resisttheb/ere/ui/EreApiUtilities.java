package org.resisttheb.ere.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class EreApiUtilities {

	public static List<List<Integer>> uniformExclusiveOffsets(final int[] sizes, int wordsCount) {
		// TODO: distribution for parents per row
		// TODO: then choose offsets 
		
		final SortedSet<Integer> usedOffsets = new TreeSet<Integer>();
		final List<List<Integer>> allOffs = new ArrayList<List<Integer>>(sizes.length);
		
		for (int i = 0; i < sizes.length; ++i) {
			final int parentCount = sizes[i];
			
			final List<Integer> parentOffs = new ArrayList<Integer>(parentCount);
			
			for (int j = 0; j < parentCount; ++j) {
				int off = (int) Math.floor(Math.random() * wordsCount);
				--wordsCount;
				
				int pre = 0;
				int headPre = 0;
				// Note that SortedSet#headSet is exclusive
				while (pre != (headPre = 1 + usedOffsets.headSet(off).size())) {
					off += (headPre - pre);
					pre = headPre;
				}
				
				usedOffsets.add(off);
				parentOffs.add(off);
			}
			
			allOffs.add(parentOffs);
		}
		
		return allOffs;
	}
	
	
	private EreApiUtilities() {
	}
}
