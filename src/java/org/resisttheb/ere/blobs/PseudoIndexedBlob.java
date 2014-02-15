package org.resisttheb.ere.blobs;

import java.awt.Color;


public class PseudoIndexedBlob {
	public int x0 			= -1;
	public int y0 			= -1;
	public int x1 			= -1;
	public int y1 			= -1;
	
	public Column[] cols 	= new Column[0];
	// [min, max, current]
	public int[][] colStates = new int[0][];
	
	public int repr;
	
	
	public PseudoIndexedBlob() {
	}
	
	
	public void reserve(final int w) {
		if (colStates.length < w) {
			colStates = new int[w][3];
		}
	}
	
	public void reset(final BlobNode blob) {
		this.cols = blob.getColumnsModifiable();
		this.repr = blob.getRepresentative();
		
		//
		//
		x0 = blob.x0();
		y0 = blob.y0();
		x1 = blob.x1();
		y1 = blob.y1();
		
		reserve(x1 - x0);
		
		for (int i = 0; i < cols.length; ) {
			final Column col0 = cols[i];

			final int j = col0.x - x0;
			colStates[j][0] = i;
			
			for (++i; i < cols.length && col0.x == cols[i].x; ) ++i;
			
			colStates[j][1] = i - 1;
			colStates[j][2] = -1;
		}		
	}
}
