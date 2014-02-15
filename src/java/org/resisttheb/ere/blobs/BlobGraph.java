package org.resisttheb.ere.blobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;


// uses an internal node representation for construction
// when finished constructing, dumps to a finished node representation
// can retain blob data,
//    for use with #getBlobData(id):Column[]
//    if flag is set
//    otherwise blob data is flushed
//
// TODO: BlobNode interface
public class BlobGraph {
	public static enum OrphanPolicy {
		DISCARD,
		IGNORE
	}
	
	
	private AdjacencyTest at 			= AdjacencyTest._4;
	private IgnoreTest it 				= IgnoreTest.NOOP;
	private UnionTest ut 				= UnionTest.EQUALITY;
	private AgglomerateSelector as 		= AgglomerateSelector.CLOSEST_COLOR_GREATER_AREA;
	
	private int minArea 				= 8;
	private OrphanPolicy orphanPolicy 	= OrphanPolicy.DISCARD;
	
	// performance and appearance.
	// merging as agglomerate will more evenly distribute the color.
	// off will create blockier regions but faster
	private boolean mergeAsAgglomerate	= false;
	
	
	private float maxCoverage			= 0.4f;
	
	
	// PERF
	// most applications don't need the column color --
	// can look at the source pixels. use this to free a bit of memory
//	private boolean retainColColor = false;
	
//	// retain columns if you'll use the blobs
//	// for tracing, midpoint tracing, or as pixels (BlobPixels)
//	// for another processing step
//	//    note that blobs are basically useless without the backing columns;
//	//    however, 
//	private boolean retainCols = false;
	// ::
	
	
	// DATA:
	private Pixels pixels 			= null;
	private Column[][] blobCols 	= new Column[0][];
	private BlobNode[] xblobs 		= new BlobNode[0];
	// ::
	
	
	public BlobGraph() {
	}
	
	
	/**************************
	 * CONFIGURATION
	 **************************/
	
	public void setAdjacencyTest(final AdjacencyTest _at) {
		this.at = _at;
	}
	
	public void setIgnoreTest(final IgnoreTest _it) {
		this.it = _it;
	}
	
	public void setUnionTest(final UnionTest _ut) {
		this.ut = _ut;
	}
	
	public void setAgglomerateSelector(final AgglomerateSelector _as) {
		this.as = _as;
	}
	
	public void setMinArea(final int _minArea) {
		this.minArea = _minArea;
	}
	
	public void setOrphanPolicy(final OrphanPolicy _orphanPolicy) {
		this.orphanPolicy = _orphanPolicy;
	}
	
	public void setMergeAsAgglomerate(final boolean _mergeAsAgglomerate) {
		this.mergeAsAgglomerate = _mergeAsAgglomerate;
	}
	
	public void setMaxCoverage(final float _maxCoverage) {
		this.maxCoverage = _maxCoverage;
	}
	
	/**************************
	 * END CONFIGURATION
	 **************************/
	
	
	
	// discards any retained columns
	// note that the only way to get the columns back is to rebuild,
	// #rebuild
	public void discardColumns() {
		// throw out columns ...
		blobCols = new Column[0][];
	}
	
	
	// after this, blobs may not be modifiable
	// e.g. may not be able to merge
	// can uncompact to make them modifiable again
	public void compactBlobs() {
		final BlobNode[] blobs = xblobs;
		final ExternalBlobNode[] _xblobs = new ExternalBlobNode[blobs.length];
		for (int i = 0; i < blobs.length; ++i) {
			final BlobNode blob = blobs[i];
			final ExternalBlobNode xbn = new ExternalBlobNode(blob);
			xbn.adj = new BlobNode[blob.adjCount()];
			_xblobs[i] = xbn;
		}
		
		// Establish links:
		for (int i = 0; i < blobs.length; ++i) {
			final BlobNode blob = blobs[i];
			final BlobNode[] adj = _xblobs[i].adj;
			int j = 0;
			for (BlobNode adjInternalNode : blob.getAdjacentModifiable2()) {
				// Replace internal with external:
				try {
				adj[j++] = _xblobs[adjInternalNode.getId()];
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		
		xblobs = _xblobs;
	}
	
	
	public void uncompactBlobs() {
		final BlobNode[] blobs = xblobs;
		final InternalBlobNode[] _xblobs = new InternalBlobNode[blobs.length];
		for (int i = 0; i < blobs.length; ++i) {
			final BlobNode blob = blobs[i];
			final InternalBlobNode xbn = new InternalBlobNode(blob);
			xbn.adj = new HashSet<InternalBlobNode>(blob.adjCount());
			_xblobs[i] = xbn;
		}
		
		// Establish links:
		for (int i = 0; i < blobs.length; ++i) {
			final BlobNode blob = blobs[i];
			for (BlobNode adjInternalNode : blob.getAdjacentModifiable()) {
				// Replace internal with external:
				_xblobs[i].adj.add(_xblobs[adjInternalNode.getId()]);
			}
		}
		
		xblobs = _xblobs;
	}
	
	
	
	public Pixels getPixels() {
		return pixels;
	}
	
	// constructs the graph
	public void setPixels(final Pixels _pixels) {
		if (null == _pixels)
			throw new IllegalArgumentException();
		this.pixels = _pixels;
	}
	
	
	public BlobNode[] getBlobsModifiable() {
		return xblobs;
	}
	
	
	
	
	
	public boolean rebuild() {
		xDisjointSets<Column> sets = new xDisjointSets<Column>();
		
		final int px_w = pixels.x1() - pixels.x0();
		final int px_h = pixels.y1() - pixels.y0();
		//List<Column> allCols = new ArrayList<Column>(px_w * px_h / 8);
		
		xDisjointSets.Node<Column>[][] outNodes = new xDisjointSets.Node[1][];
		final int nN = buildSets(sets, outNodes);
		
		xDisjointSets.Node<Column>[] nodes = outNodes[0];
		outNodes = null;
		
		// at this point, we have all the sets
		// call DS.collect
		
		
		
		blobCols = sets.collect(Column.class, nodes, nN);
		
		final float coverage = blobCols.length / (float) (px_w * px_h);
		if (maxCoverage < coverage) {
			return false;
		}
		
		//CLEANUP
//		allNodes = null;
//		// Trash nodes; keep colors ...
//		for (Column col : allCols) {
//			final NodePlusColor npc = (NodePlusColor) col.alg_ref_0;
//			col.alg_ref_0 = new byte[]{
//				npc.r,
//				npc.g,
//				npc.b
//			};
//				//new Color(((NodePlusColor) col.alg_ref_0).rgb);
//		}
		
		
		
		// for each blob, compute the bounding box
		
		InternalBlobNode[] blobs = new InternalBlobNode[blobCols.length];
		for (int i = 0; i < blobCols.length; ++i) {
			blobs[i] = new InternalBlobNode(i);
		}

		
		// CLEANUP
		sets = null;
		// We can also throw out representative colors for each column now ...
//		if (! retainColColor) {
//		for (Column col : allCols) {
//			col.alg_ref_0 = null;
//		}
//		}
//		allCols = null;
		for (int i = 0; i < nN; ++i) {
			nodes[i].getValue().alg_ref_0 = null;
		}
		nodes = null;
		
		
		// links neighbors
		link(blobs);
		blobs = agglomerate(blobs);
		
		
		// at this point, the internal nodes are linked and agglomerated.
		// create the external nodes ...
		
		xblobs = blobs;
		
		return true;
	}
	
	private int buildSets(final xDisjointSets<Column> sets, final xDisjointSets.Node<Column>[][] outNodes) {
		
		
		final int px_x0 = pixels.x0();
		final int px_y0 = pixels.y0();
		final int px_x1 = pixels.x1();
		final int px_y1 = pixels.y1();
		
		final int px_w = px_x1 - px_x0;
		final int px_h = px_y1 - px_y0;
		
		// a list<Column> of the previous column
		// a list<Column> of the current column
		// 
		// steps:
		// 1. fill current column
		// 2. merge current with previous
		//    (using 4 or 8 adjacency, comparing the mean color)
		//    this just does set unions
		// 3. rotate columns,
		//    advance
		
		
//		final int estCols 		= px_w * (1 + px_h / 8);
		
		// These must be initialized false. Thanks JVM
		//final boolean[] hooked	= new boolean[px_h];
		
		//List<Column> pcols 		= new ArrayList<Column>(estCols);
		//List<Column> ccols 		= new ArrayList<Column>(estCols);
		Column[] pcols				= new Column[px_h];
		Column[] ccols				= new Column[px_h];
		
		final xDisjointSets.Node<Column>[] nodes = new xDisjointSets.Node[px_w * px_h];
		
		int nN = 0;
		int pN = 0;
		int cN = 0;
		
		int pbase = 0;
		int cbase = 0;
		
//		final Color[] rgb = {null};
		final int[] rgb = new int[1];
		for (int x = px_x0; x < px_x1; ++x) {
			for (int y = px_y0; y < px_y1;) {
				// BURN SKIP COLORS ...
				while (y < px_y1 && (!pixels.px(x, y, rgb) || it.ignore(rgb[0]))) {
					++y;
				}
				
				if (px_y1 <= y)
					continue;

				final int y0 = y;

//				Color pcolor = rgb[0];
				final int pcolor = rgb[0];
				byte pr = (byte) pcolor;
				byte pg = (byte) (pcolor >> 8);
				byte pb = (byte) (pcolor >> 16);
				
				int netr = pr & 0xFF;
					//pcolor.getRed();
				int netg = pg & 0xFF;
					//pcolor.getGreen();
				int netb = pb & 0xFF;
					//pcolor.getBlue();
				
				for (y = y0 + 1; y < px_y1 && (pixels.px(x, y, rgb) && !it.ignore(rgb[0])); ++y) {
//					final Color ccolor = rgb[0];
					final int ccolor = rgb[0];
					
					final byte cr = (byte) ccolor;
					final byte cg = (byte) (ccolor >> 8);
					final byte cb = (byte) (ccolor >> 16);
					
					if (! ut.union(pr, pg, pb, cr, cg, cb))
						break;
					
					netr += cr & 0xFF;
					netg += cg & 0xFF;
					netb += cb & 0xFF;
					
					//pcolor = ccolor;	
					pr = cr;
					pg = cg;
					pb = cb;
				}
				
				final int len = y - y0;
				netr /= len;
				netg /= len;
				netb /= len;
				
				final Column col 	= new Column(x, y0, y);
				final xDisjointSets.Node<Column> node = sets.makeSet(col);
				col.alg_ref_0		=  new byte[]{
						(byte) netr, (byte) netg, (byte) netb};
				
				//ccols.add(col);
				nodes[nN++] = node;
				ccols[cN++] = col;
			}
			
			
			// Adjacency testing:
			// - if overlap in y, then always adjacent
			// - if 8-adj enabled, test if endpoints are equal
			
			int pi = 0;
			int ci = 0;
			// advance left while less than right
			// advance right while less than left
			while (pi < pN && ci < cN) {
				Column pc;
				Column cc = ccols[ci];
				for (; pi < pN && 
				(pc = pcols[pi]).y1 <= cc.y1; 
				++pi) {
					final byte[] p = (byte[]) pc.alg_ref_0;
					final byte[] c = (byte[]) cc.alg_ref_0;
					if (ut.union(
							p[0], p[1], p[2], 
							c[0], c[1], c[2]
						) && at.crossXAdjacent(pc, cc)) {
						sets.union(
							nodes[pbase + pi], 
							nodes[cbase + ci]
						);
						//hooked[pi] = true;
					}
				}
				
				if (pN <= pi)
					break;
				
				pc = pcols[pi];
				
				for (; ci < cN && 
				(cc = ccols[ci]).y1 <= pc.y1; 
				++ci) {
					final byte[] p = (byte[]) pc.alg_ref_0;
					final byte[] c = (byte[]) cc.alg_ref_0;
					if (ut.union(
							p[0], p[1], p[2], 
							c[0], c[1], c[2]
						) && at.crossXAdjacent(pc, cc)) {
						sets.union(
							nodes[pbase + pi], 
							nodes[cbase + ci]
						);
						//hooked[pi] = true;
					}
				}
			}
			
			
			// Swap:
			//allCols.addAll(pcols);
//			for (int i = 0; i < pN; ++i) {
//				allCols.add(pcols[i]);
//			}
			
			//pcols.clear();
			pbase = cbase;
			cbase += cN;
			pN = cN;
			cN = 0;
//			final List<Column> _pcols = pcols;
			final Column[] _pcols = pcols;
			pcols = ccols;
			ccols = _pcols;
		}
		
//		pcols.clear();
//		ccols.clear();
		pcols = null;
		ccols = null;
		
		outNodes[0] = nodes;
		return nN;
	}
	
	
	// this initialized the "adj" field on each blob node
	// current version uses an R tree to test neighbors
	private void link(final InternalBlobNode[] blobs) {
		RTree rtree = new RTree();
		rtree.init(new Properties());

		for (int i = 0; i < blobs.length; ++i) {
			rtree.add(blobs[i].bounds(), i);
		}
		
		Multimap<InternalBlobNode, InternalBlobNode> adjMap = 
			new HashMultimap<InternalBlobNode, InternalBlobNode>();
		
		Rectangle xbounds = new Rectangle(new float[2], new float[2]);
		for (int i = 0; i < blobs.length; ++i) {
			final InternalBlobNode blobi = blobs[i];

			final int _i = i;
			final Multimap<InternalBlobNode, InternalBlobNode> _adjMap = adjMap;
			
			final IntProcedure ip = new IntProcedure() {
				public boolean execute(final int j) {
					if (j <= _i)
						return true;
					
					final InternalBlobNode blobj = blobs[j];
					
					if (BlobUtilities.isTouching(at, 
						blobi.getColumnsModifiable(), 
						blobs[j].getColumnsModifiable()
					)) {
						// Add an adjacency link:
						_adjMap.put(blobi, blobj);
						_adjMap.put(blobj, blobi);
					}
					
					return true;
				}
			};
			
			xbounds.min[0] = blobi.x0() - 1;
			xbounds.min[1] = blobi.y0() - 1;
			xbounds.max[0] = blobi.x1() + 1;
			xbounds.max[1] = blobi.y1() + 1;
			rtree.intersects(xbounds, ip);
		}
		
		
		// Done with the index ...
		rtree = null;
		
		// create edges ...
		for (InternalBlobNode node : adjMap.keySet()) {
			node.adj = new HashSet<InternalBlobNode>(adjMap.get(node));
		}

//		adjMap.clear();
		adjMap = null;
	}
	
	private InternalBlobNode[] agglomerate(final InternalBlobNode[] blobs) {
		assert all_ids_less_than(blobs, blobs.length);
		
		SortedSet<InternalBlobNode> blobTree = 
			new TreeSet<InternalBlobNode>(BlobUtilities.AREA_COMPARATOR_PRESERVE_ID);
		for (InternalBlobNode blob : blobs) {
			blobTree.add(blob);
		}
		
		final List<InternalBlobNode> alt = new ArrayList<InternalBlobNode>(8);
		for (InternalBlobNode min; !blobTree.isEmpty() && (min = blobTree.first()).area < minArea; ) {
			// find neighbor with min area greater than ours
			InternalBlobNode adj = (InternalBlobNode) as.select(min);
			
			// Note: <code>adj</code> must be adjacent to <code>min</code>
			
			if (null != adj) {
				blobTree.remove(min);
				blobs[min.id] = null;
				
				blobTree.remove(adj);
				
				adj.consume(min);
				
//				mergeIntoNeighbor(blobTree, adj);
				if (mergeAsAgglomerate) {
					for (InternalBlobNode adjAdj : adj.adj) {
						if (! ut.union(adj.r, adj.g, adj.b,
								adjAdj.r, adjAdj.g, adjAdj.b))
							continue;
						
						// Purge the previous adjacent:
						blobs[adj.id] = null;
						blobTree.remove(adjAdj);
						
						
						adjAdj.consume(adj);
						adj = adjAdj;
					}
				}
				blobTree.add(adj);
				
				//assert id_not_found(blobTree, min.id);
				
			}
			else {
				// No adjacent blob was selected ...
				switch (orphanPolicy) {
					case DISCARD:
						min.unlink();
						blobTree.remove(min);
						// Purge:
						blobs[min.id] = null;
						break;
					case IGNORE:
						blobTree.remove(min);
						alt.add(min);
						break;
				}
			}
		}
		
		// merge the ignored elements back ...
		blobTree.addAll(alt);
		alt.clear();
		
		
		// A final pass is to merge all nodes that should be unioned.
		// we do this walking up the nodes.
		
		final InternalBlobNode[] aggBlobs;
		if (! mergeAsAgglomerate) {
			final InternalBlobNode[] bt = blobTree.toArray(new InternalBlobNode[blobTree.size()]);
			blobTree = null;
			final InternalBlobNode[] _aggBlobs = new InternalBlobNode[bt.length];
			int j = 0;
			
			for (int i = 0; i < bt.length; ++i) {
				InternalBlobNode blob = bt[i];
				
				boolean merged = false;
				for (InternalBlobNode adj : blob.adj) {
					if (! ut.union(blob.r, blob.g, blob.b,
							adj.r, adj.g, adj.b))
						continue;
					
					// Purge the previous adjacent:
					blobs[blob.id] = null;
					
					adj.consume(blob);
					merged = true;
					break;
				}
				if (! merged) {
					_aggBlobs[j++] = blob;
				}
			}
			
			aggBlobs = new InternalBlobNode[j];
			System.arraycopy(_aggBlobs, 0, aggBlobs, 0, j);
		}
		else {
			aggBlobs = blobTree.toArray(new InternalBlobNode[blobTree.size()]);
		}
		blobTree = null;
		
		// Reset ids:
		Column[][] _blobCols = blobCols;
		blobCols = new Column[aggBlobs.length][];
		for (int i = 0; i < aggBlobs.length; ++i) {
			final InternalBlobNode aggBlob = aggBlobs[i];
			blobCols[i] = _blobCols[aggBlob.id];
			aggBlob.id = i;
		}
		_blobCols = null;
		
		assert all_ids_less_than(aggBlobs, aggBlobs.length);
				
		
		
		return aggBlobs;
	}
	
//	private boolean mergeIntoNeighbor(final Collection<InternalBlobNode> blobs, InternalBlobNode blob) {
//		boolean merged = false;
//		blobs.remove(blob);
//		for (InternalBlobNode adjBlob : blob.adj.toArray(new InternalBlobNode[blob.adj.size()])) {
//			if (! ut.union(blob.repr, adjBlob.repr))
//				continue;
//			
//			blobs.remove(adjBlob);
//			
//			adjBlob.consume(blob);
//			blob = adjBlob;
//			merged = true;
//		}
//		blobs.add(blob);
//		return merged;
//	}
	
	
	
	
	
	
	
	
	/**************************
	 * INTERNAL STRUCTURES
	 **************************/
	
	
	/*
	private static final class NodePlusColor {
		public xDisjointSets.Node<Column> node;
		public byte r, g, b;
		
		public NodePlusColor() {}
		public NodePlusColor(final xDisjointSets.Node<Column> _node, 
				final byte _r, final byte _g, final byte _b) {
			this.node 		= _node;
//			this.rgb 		= _rgb;
			this.r = _r;
			this.g = _g;
			this.b = _b;
		}
	}
	*/
	
	
	private abstract class BlobNodeBase implements BlobNode {
		public int id;
		
		public int area;
		
		public int x0;
		public int y0;
		public int x1;
		public int y1;
		
		//public Color repr;
		public byte r;
		public byte g;
		public byte b;
		
		
		public BlobNodeBase(final BlobNode src) {
			this.id 	= src.getId();
			this.area 	= src.getArea();
			this.x0 	= src.x0();
			this.y0 	= src.y0();
			this.x1 	= src.x1();
			this.y1 	= src.y1();
			//this.repr 	= src.getRepresentative();
			final byte[] cs = new byte[4];
			src.getRepresentative(cs);
			this.r = cs[0];
			this.g = cs[1];
			this.b = cs[2];
		}
		
		public BlobNodeBase() {
		}
		
		
		/**************************
		 * BLOBNODE IMPLEMENTATION
		 **************************/
		
		public int getId() {
			return id;
		}
				
		public int x0() { return x0; }
		public int y0() { return y0; }
		public int x1() { return x1; }
		public int y1() { return y1; }
		
		public Rectangle bounds() {
			return new Rectangle(x0, y0, x1, y1);
		}
		
		public Column[] getColumnsModifiable() {
			return id < blobCols.length
				? blobCols[id]
				: null;
		}
		
		public int getRepresentative() {
			return (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | (0xFF << 24);
		}
		
		public void getRepresentative(final byte[] cs) {
			cs[0] = r;
			cs[1] = g;
			cs[2] = b;
			cs[3] = (byte) 0xFF;
		}
		
		public int getArea() {
			return area;
		}
		
		/**************************
		 * END BLOBNODE IMPLEMENTATION
		 **************************/
	}
	
	// optimized for being changed, e.g. adding neighbors
	// 
	private final class InternalBlobNode extends BlobNodeBase {
		public Set<InternalBlobNode> adj = Collections.<InternalBlobNode>emptySet();

		
		public InternalBlobNode(final BlobNode src) {
			super(src);
		}
		
		
		public InternalBlobNode(final int _id) {
			if (_id < 0)
				throw new IllegalArgumentException();
			
			this.id = _id;
			
			final Column[] cols = id < blobCols.length
				? blobCols[id]
				: null;
			
			if (null == cols)
				throw new IllegalStateException("Cannot create internal node without backing columns.");
				
			
			assert BlobUtilities.isOrdinal(cols)
				: "Columns should be sorted by construction, " +
						"and that #collect maintains input order.";
			
			int netr = 0;
			int netg = 0;
			int netb = 0;
			int netlen = 0;
			for (Column col : cols) {
				final int len = col.y1 - col.y0;
				//final Color colRepr = (Color) col.alg_ref_0;
				final byte[] colRepr = (byte[]) col.alg_ref_0;
				netr += len * (colRepr[0] & 0xFF);
				netg += len * (colRepr[1] & 0xFF);
				netb += len * (colRepr[2] & 0xFF);
				netlen += len;
			}
			area = netlen;
			netr /= area;
			netg /= area;
			netb /= area;
			//repr = new Color(netr, netg, netb);
			r = (byte) netr;
			g = (byte) netg;
			b = (byte) netb;
			
			final Column col0 = cols[0];
			final Column col1 = cols[cols.length - 1];
			int miny = col0.y0;
			int maxy = col0.y1;
			for (int i = 1; i < cols.length; ++i) {
				final Column col = cols[i];
				if (col.y0 < miny) miny = col.y0;
				if (maxy < col.y1) maxy = col.y1;
			}
			
			x0 = col0.x;
			y0 = miny;
			x1 = col1.x + 1 /* exclusive */;
			y1 = maxy;
		}
		
		
		/**************************
		 * BLOBNODE IMPLEMENTATION
		 **************************/
		
		public BlobNode[] getAdjacentModifiable() {
			return adj.toArray(new BlobNode[adj.size()]);
		}
		
		public Collection<BlobNode> getAdjacentModifiable2() {
			return (Collection<BlobNode>) (Object) adj;
		}
		
		public int adjCount() {
			return adj.size();
		}
		
		/**************************
		 * END BLOBNODE IMPLEMENTATION
		 **************************/
		
		
		public void consume(final InternalBlobNode node) {
			// graph level:
			final boolean neighbor = adj.remove(node);
			if (! neighbor)
				throw new IllegalArgumentException("We can only remove neighbors with this function.");
			
			for (InternalBlobNode adjNode : node.adj) {
				if (this == adjNode)
					continue;
				
				adjNode.adj.remove(node);
				adjNode.adj.add(this);
				
				adj.add((InternalBlobNode) adjNode);
			}
			// TODO: really bad ... need to clarify this.
			//node.adj.clear();
			
			
			final Column[] cols = getColumnsModifiable();
			final Column[] nodeCols = node.getColumnsModifiable();
			
			// merge cols:
			// Note we preserve longest runs.
			Column[] _cols = new Column[cols.length + nodeCols.length];
			int _i = 0;
			int i = 0;
			int j = 0;
			Column ccol = null;
			Column rcol0 = BlobUtilities.COLUMN_COMPARATOR.compare(cols[i], nodeCols[j]) <= 0
				? cols[i++]
				: nodeCols[j++];
			Column rcol1 = rcol0;
			for (; i < cols.length && j < nodeCols.length;) {
				ccol = BlobUtilities.COLUMN_COMPARATOR.compare(cols[i], nodeCols[j]) <= 0
					? cols[i++]
					: nodeCols[j++];
					
				if (ccol.x == rcol1.x && ccol.y0 == rcol1.y1) {
					rcol1 = ccol;
				}
				else {
					// dump [rcol0, rcol1]
					// reset to ccol
					_cols[_i++] = rcol0 == rcol1
						? rcol0
						: new Column(rcol0.x, rcol0.y0, rcol1.y1);
					
					
					rcol0 = rcol1 = ccol;
				}
			}
			
			// Dump the last frame
			_cols[_i++] = rcol0 == rcol1
				? rcol0
				: new Column(rcol0.x, rcol0.y0, rcol1.y1);
			
			
			if (i < cols.length) {
				final int len = cols.length - i;
				System.arraycopy(cols, i, _cols, _i, len);
				_i += len;
			}
			else if (j < nodeCols.length) {
				final int len = nodeCols.length - j;
				System.arraycopy(nodeCols, j, _cols, _i, len);
				_i += len;
			}
			
			
			// Resize if needed:
			if (_i < _cols.length) {
				final Column[] trim = new Column[_i];
				System.arraycopy(_cols, 0, trim, 0, _i);
				_cols = trim;
			}
			
			
			assert _i == _cols.length;
			blobCols[id] = _cols;
			area += node.getArea();
			
			
			//bounds = bounds.union(node.bounds);
			if (node.x0() < x0) x0 = node.x0();
			if (x1 < node.x1()) x1 = node.x1();
			if (node.y0() < y0) y0 = node.y0();
			if (y1 < node.y1()) y1 = node.y1();
			
			// TODO: do we update mean color? .. for now, try without
		}
		
		
		public void unlink() {
			for (InternalBlobNode adjBlob : adj) {
				adjBlob.adj.remove(this);
			}
			// TODO: for symmetry, we leave this also bad. see consume above
//			adj.clear();
		}
	}
	
	
	
	private class ExternalBlobNode extends BlobNodeBase {
		public BlobNode[] adj;
		
		
		public ExternalBlobNode(final BlobNode src) {
			super(src);
		}
		
		
		/**************************
		 * BLOBNODE IMPLEMENTATION
		 **************************/
		
		public BlobNode[] getAdjacentModifiable() {
			return adj;
		}
		
		public Collection<BlobNode> getAdjacentModifiable2() {
			return Arrays.asList(adj);
		}
		
		public int adjCount() {
			return adj.length;
		}
		
		/**************************
		 * END BLOBNODE IMPLEMENTATION
		 **************************/
	}
	
	
	/**************************
	 * END INTERNAL STRUCTURES
	 **************************/
	
	
	
	
	
	
	
	
	/**************************
	 * DEBUGGING
	 **************************/
	
	private static boolean all_ids_less_than(final InternalBlobNode[] blobs, final int n) {
		for (InternalBlobNode blob : blobs) {
			if (n <= blob.id)
				return false;
			for (InternalBlobNode adjBlob : blob.adj) {
				if (n <= adjBlob.id)
					return false;
			}
		}
		return true;
	}
	
	private static boolean id_not_found(final Collection<InternalBlobNode> blobs, final int id) {
		for (InternalBlobNode blob : blobs) {
			if (id == blob.id)
				return false;
			for (InternalBlobNode adjBlob : blob.adj) {
				if (id == adjBlob.id)
					return false;
			}
		}
		return true;
	}
	
	/**************************
	 * END DEBUGGING
	 **************************/
}
