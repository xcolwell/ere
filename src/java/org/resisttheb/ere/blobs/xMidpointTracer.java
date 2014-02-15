package org.resisttheb.ere.blobs;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

// this impl is runtime O(number of cols * number of branches)
// a faster impl would be O(number of cols),
// but this impl is more memory efficient. this impl uses O(width) memory,
// and can efficiently re-use the memory
//
// err, going for the graph approach
// once we have the graph,
// start from top,
//      while(exploreForward())   explores one path from this node at a time
//      
//  each link is marked explored. node is not marked explored while it has
//  unexplored links
//
// 

public class xMidpointTracer {
	
	
	private static final class Node {
		public Column[] forward;
		// the next index to explore
		public short visited;
	}
	
	
	
	
	
	
	// use a pseudo indexed blob
	
	// walk forward, taking first index
	// when cannot walk anymore, go back to last know frame with available
	
	private AdjacencyTest at;
	private Column[] cols;
	private int x0;
	private int x1;
	
	
	public xMidpointTracer(final AdjacencyTest _at) {
		this.at = _at;
	}
	
	
	public void setData(final Column ... _cols) {
		this.cols = _cols;
		x0 = cols[0].x;
		x1 = cols[cols.length - 1].x + 1;
	}

	
	
	// will put lineto and moveto
	public void trace(final GeneralPath gpath) {
		
		// columns should be sorted in standard ordinal
	
		
		// histogramming step, where we count the number of forward links per column
		// then we do another pass to populate
		
		
		
		
		// initialize nodes on all columns
		for (Column col : cols) {
			col.alg_ref_0 = new Node();
		}
		
		
		List<Column> pcols = new ArrayList<Column>(32);
		List<Column> ccols = new ArrayList<Column>(32);
		
		for (int i = 0, x = x0; x < x1; ++x) {
			// populate current column:
			for (; i < cols.length && x == cols[i].x; ++i) {
				ccols.add(cols[i]);
			}
			
			
			
			
			int pi;
			int ci;
			
			// advance left while less than right
			// advance right while less than left
			for (pi = 0, ci = 0; pi < pcols.size() && ci < ccols.size(); ) {
				Column pc;
				Column cc = ccols.get(ci);
				for (; pi < pcols.size() && 
				(pc = pcols.get(pi)).y1 <= cc.y1; 
				++pi) {
					if (at.crossXAdjacent(pc, cc)) {
						//sets.union(pc.node, cc.node);
						++((Node) pc.alg_ref_0).visited;
					}
				}
				
				if (pcols.size() <= pi)
					break;
				
				pc = pcols.get(pi);
				
				for (; ci < ccols.size() && 
				(cc = ccols.get(ci)).y1 <= pc.y1; 
				++ci) {
					if (at.crossXAdjacent(pc, cc)) {
						//sets.union(pc.node, cc.node);
						++((Node) pc.alg_ref_0).visited;
					}
				}
			}

			
			for (Column col : pcols) {
				final Node node = (Node) col.alg_ref_0;
				node.forward = new Column[node.visited];
				node.visited = 0;
			}
			
			
			// advance left while less than right
			// advance right while less than left
			for (pi = 0, ci = 0; pi < pcols.size() && ci < ccols.size(); ) {
				Column pc;
				Column cc = ccols.get(ci);
				for (; pi < pcols.size() && 
				(pc = pcols.get(pi)).y1 <= cc.y1; 
				++pi) {
					if (at.crossXAdjacent(pc, cc)) {
						//sets.union(pc.node, cc.node);
						final Node node = (Node) pc.alg_ref_0;
						node.forward[node.visited++] = cc;
					}
				}
				
				if (pcols.size() <= pi)
					break;
				
				pc = pcols.get(pi);
				
				for (; ci < ccols.size() && 
				(cc = ccols.get(ci)).y1 <= pc.y1; 
				++ci) {
					if (at.crossXAdjacent(pc, cc)) {
						//sets.union(pc.node, cc.node);
						final Node node = (Node) pc.alg_ref_0;
						node.forward[node.visited++] = cc;
					}
				}
			}
			
			
			for (Column col : pcols) {
				((Node) col.alg_ref_0).visited = 0;
			}
			
			
			
			// Swap:
			pcols.clear();
			final List<Column> _pcols = pcols;
			pcols = ccols;
			ccols = _pcols;
		}
		
		for (Column col : pcols) {
			final Node node = ((Node) col.alg_ref_0);
			node.forward = new Column[0];
			node.visited = 0;
		}
		
		
		// at this point we have all the forward links
		// now explore forward
		
		
		for (int i = 0; i < cols.length; ++i) {
			final Column col = cols[i];
			final Node node = (Node) col.alg_ref_0;
			if (node.visited < node.forward.length) {
				do {
					gpath.moveTo(col.x, (col.y0 + col.y1 - 1) / 2.f);
					// visit forward increments visited index on each node it hits
				}
				while (visitForward(gpath, col));
			}
		}
		
		
		
		// clean up ...
		for (Column col : cols) {
			col.alg_ref_0 = null;
		}
//		System.gc();
		
		
		
	}
	
	// returns true is any forward links remain to explore
	private boolean visitForward(final GeneralPath gpath, Column col) {
		final Column col0 = col;
		for (Node node; 
		(node = (Node) col.alg_ref_0).visited < node.forward.length;
		) {
			final Column fcol = node.forward[node.visited++];
			gpath.lineTo(fcol.x, (fcol.y0 + fcol.y1 - 1) / 2.f);
			col = fcol;
		}
		final Node node0 = (Node) col0.alg_ref_0;
		return node0.visited < node0.forward.length;
	}
}
