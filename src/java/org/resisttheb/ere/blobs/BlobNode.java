package org.resisttheb.ere.blobs;

import java.awt.Color;
import java.util.Collection;

import com.infomatiq.jsi.Rectangle;

public interface BlobNode {
	public int getId();
	
	
	public int x0();
	public int y0();
	public int x1();
	public int y1();
	
	public Rectangle bounds();
	
	
	// clients should know which version to use.
	// most will use the array;
	// there are cases e.g. AgglomerateSelector where the second is more efficient
	public BlobNode[] getAdjacentModifiable();
	public Collection<BlobNode> getAdjacentModifiable2();
	
	public int adjCount();
	
	// may return null if columns are not retained in graph
	public Column[] getColumnsModifiable();
	
	public void getRepresentative(final byte[] cs);
	public int getRepresentative();
	public int getArea();
	
	// operations
	
	// note: other must be from the same graph
	// other must also be adjacent
//	public void consume(final BlobNode other);
}
