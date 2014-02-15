package org.resisttheb.ere.blobs;

import java.lang.reflect.Array;

// when make a set, return the node
// our external blob alg keeps the node in the column,
// so we can efficiently pass it back to do union operation
//
// i.e. the set does not maintain a map of value to node
// you have to do all operations on nodes,
// so external client must remember nodes
//
// not thread safe
// ids are allocated after #collect is called
//
//
public class xDisjointSets<T> {
	public static interface Node<T> {
		public T getValue();
		public int getId();
	}
	
	private static final class _Node<T> implements Node<T> {
		public _Node<T> parent;
		public int rank = 0;
		public final T value;
		
		public _Node(final T _value) {
			parent 		= this;
			this.value = _value;
		}
		
		/**************************
		 * NODE IMPLEMENTATION
		 **************************/
		
		public T getValue() {
			// no longer storing the value here,
			// to save memory
//			return null;
			return value;
		}
		
		public int getId() {
			return rank;
		}
		
		/**************************
		 * END NODE IMPLEMENTATION
		 **************************/
	}
	
	
	
	public xDisjointSets() {
	}

	
	public Node<T> makeSet(final T value) {
		final _Node<T> node = new _Node<T>(value);
		return node;
	}
	
	public Node<T> find(final Node<T> node) {
		return _find((_Node<T>) node);
	}
	
	public void union(final Node<T> a, final Node<T> b) {
		_union((_Node<T>) a, (_Node<T>) b);
	}
	
	
	// returns the number of ids
	// assigns internal ids.
	private int compactIds(final Node<T>[] nodes, final int length) {
		// 
		// reset all ids to -1
		// assign as we go
		
		int id = 0;
		
		for (int i = 0; i < length; ++i) {
			final _Node<T> _node = (_Node<T>) nodes[i];
			if (_node == _node.parent)
				_node.rank = id++;
		}
		
		for (int i = 0; i < length; ++i) {
			final _Node<T> _node = (_Node<T>) nodes[i];
			_node.rank = _find(_node).rank;
		}
		
		return id;
	}
	
	// histograms set sizes, for each root id
	private int[] histogram(final Node<T>[] nodes, final int length) {
		final int len = compactIds(nodes, length);
		
		final int[] histo = new int[len];
		for (int i = 0; i < length; ++i) {
			final Node<T> node = nodes[i];
			++histo[((_Node<T>) node).rank];
		}
		
		return histo;
	}
	
	// NOTE: this does a compaction of ids
	// NOTE: CANNOT ADD MORE TO SET STRUCTURE AFTER THIS IS CALLED
	// NOTE: -- RANK IS DESTROYED
	public T[][] collect(Class<T> clazz, final Node<T>[] nodes, final int length) {
		final int[] histo = histogram(nodes, length);
		
		final T[][] sets = (T[][]) Array.newInstance(clazz, new int[]{histo.length, 0});
		for (int i = 0; i < histo.length; ++i) {
			sets[i] = (T[]) Array.newInstance(clazz, histo[i]);
		}
		final int[] indices = new int[histo.length];
		// initialized to zero by JVM
		for (int i = 0; i < length; ++i) {
			final Node<T> node = nodes[i];
			final int id = ((_Node<T>) node).rank;
			sets[id][indices[id]++] = ((_Node<T>) node).value;
		}
		return sets;
	}
	
	
	/**************************
	 * INTERNAL
	 **************************/
	
	private _Node<T> _find(final _Node<T> node) {
		if (node.parent == node)
			return node;
		return node.parent = _find(node.parent);
	}
	
	private void _union(final _Node<T> a, final _Node<T> b) {
		final _Node<T> aRoot = _find(a);
		final _Node<T> bRoot = _find(b);
		
		if (aRoot.rank > bRoot.rank)
			bRoot.parent = aRoot;
		else if (aRoot.rank < bRoot.rank)
			aRoot.parent = bRoot;
		else if (aRoot != bRoot) {
			bRoot.parent = aRoot;
			++aRoot.rank;
		}
	}
	
	/**************************
	 * END INTERNAL
	 **************************/
}
