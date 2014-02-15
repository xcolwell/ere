package org.resisttheb.ere.ui3;

import java.awt.geom.Point2D;

import prefuse.util.force.DragForce;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

public class ForceLayout {
	public static final class Node {
		public ForceItem fitem;
		public float mass;

		public Node(final ForceItem _fitem, final float _mass) {
			this.fitem = _fitem;
			this.mass = _mass;
		}
	}

	public static final class Edge {
		public Node n0;
		public Node n1;
		public ForceItem[] fitems;
		public float mass;

		public float springCoeff = 1.4f * 0.0001f;
		public float slen = 75.f;

		public float mainSpringCoeff = 0.0001f;
		public float mainSlen = 150.f;
		
		public Edge(final Node _n0, final Node _n1, final ForceItem[] _fitems,
				final float _mass) {
			this.n0 = _n0;
			this.n1 = _n1;
			this.fitems = _fitems;
			this.mass = _mass;
		}
	}

	
	
	private Node[] nodes = new Node[0];
	private Edge[] edges = new Edge[0];

	private ForceSimulator fsim;
	private int iterationCount = 100;

	private Point2D.Float anchor = new Point2D.Float(0.f, 0.f);

	
	
	public ForceLayout() {
		fsim = new ForceSimulator();
		fsim.addForce(new NBodyForce());
        fsim.addForce(new SpringForce());
        fsim.addForce(new DragForce());
	}
	
	
	public void setIterationCount(final int _iterationCount) {
		this.iterationCount = _iterationCount;
	}
	
	
	public void setup(final Point2D.Float _anchor, 
			final Node[] _nodes, final Edge[] _edges) {
		this.anchor = _anchor;
		this.nodes = _nodes;
		this.edges = _edges;
		
		reset();
	}
	
	
	public void reset() {
		initSimulator(true);
	}
	
	
	
	
	
	
	
	
	
	public void run() {
		long timestep = 1000L;
		for (int i = 0; i < iterationCount; i++) {
			// use an annealing schedule to set time step
			timestep *= (1.0 - i / (double) iterationCount);
			long step = timestep + 50;
			// run simulator
			fsim.runSimulator(step);
		}
		center();
	}
	
	
	private void center() {
		// centroid of nodes
		float cx = 0.f;
		float cy = 0.f;
		for (Node node : nodes) {
			cx += node.fitem.location[0];
			cy += node.fitem.location[1];
		}
		
		cx /= nodes.length;
		cy /= nodes.length;
		
		cx -= anchor.x;
		cy -= anchor.y;
		
		for (Node node : nodes) {
			node.fitem.location[0] -= cx;
			node.fitem.location[1] -= cy;
		}
		for (Edge edge : edges) {
			for (ForceItem fitem : edge.fitems) {
				fitem.location[0] -= cx;
				fitem.location[1] -= cy;
			}
		}
	}

	
	private float ln() {
		return 3 * (float) (Math.random() - 0.5);
	}
	
	private void initSimulator(final boolean reset) {
		fsim.clear();
		for (Node node : nodes) {
			ForceItem fitem = node.fitem;
			fitem.mass = node.mass;
			if (reset) {
				fitem.location[0] = anchor.x + ln();
				fitem.location[1] = anchor.y + ln();
			}
			fsim.addItem(fitem);
		}
		
		for (Edge edge : edges) {
			for (ForceItem fitem : edge.fitems) {
				fitem.mass = edge.mass;
				if (reset) {
					fitem.location[0] = anchor.x + ln();
					fitem.location[1] = anchor.y + ln();
				}
				fsim.addItem(fitem);
			}
		}

		for (Edge edge : edges) {
			ForceItem f0 = edge.n0.fitem;
			ForceItem f1 = edge.fitems[0];
			ForceItem f2 = edge.fitems[1];
			ForceItem f3 = edge.n1.fitem;
			float coeff = edge.springCoeff;
			float slen = edge.slen;
			fsim.addSpring(f0, f1, (coeff >= 0 ? coeff : -1.f),
					(slen >= 0 ? slen : -1.f));
			fsim.addSpring(f1, f2, (coeff >= 0 ? coeff : -1.f),
					(slen >= 0 ? slen : -1.f));
			fsim.addSpring(f2, f3, (coeff >= 0 ? coeff : -1.f),
					(slen >= 0 ? slen : -1.f));
			
			float mainCoeff = edge.mainSpringCoeff;
			float mainSlen = edge.mainSlen;
			fsim.addSpring(f0, f3, (mainCoeff >= 0 ? mainCoeff : -1.f),
					(mainSlen >= 0 ? mainSlen : -1.f));
		}
	}
}
