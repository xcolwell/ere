package org.resisttheb.ere.ui2;

import java.io.Serializable;

import org.resisttheb.ere.persistence.Format0;


// TODO: 
// TODO: links of both color and word cluster
public class OutputPacket2 implements Serializable {
	// blob data: -- parallel
	public final int[] wids;
	public final Format0.Data[] data;
	//byte[] allData;
	
	// [][parent, child]
	public final int[][] wordLinks;
	// [data index][blob index][linked to blob index]
	public final int[][] blobLinks; 
	
	public final Object[][] linkedItems;
	
	
	public OutputPacket2(final int[] _wids, final Format0.Data[] _data,
			final int[][] _wordLinks, final int[][] _blobLinks,
			final Object[][] _linkedItems
	) {
		this.wids = _wids;
		this.data = _data;
		this.wordLinks = _wordLinks;
		this.blobLinks = _blobLinks;
		this.linkedItems = _linkedItems;
	}
	
	private OutputPacket2() {
		wids = null;
		data = null;
		wordLinks = null;
		blobLinks = null;
		linkedItems = null;
	}
}
