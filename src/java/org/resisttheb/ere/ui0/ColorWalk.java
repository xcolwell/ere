package org.resisttheb.ere.ui0;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class ColorWalk {
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static float lerp(final float a, final float b, final float u) {
		return a + (b - a) * u;
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	private final Edge[] edges;
	
	
	public ColorWalk() {
		// A hilbert cube:
		final float[][] nodes = {
		//	{0, 0, 0},
			{0, 0, 1},
			{0, 1, 1},
			{0, 1, 0},
			{1, 1, 0},
		//	{1, 1, 1},
			{1, 0, 1},
			{1, 0, 0}
		};
		edges = new Edge[nodes.length];
		for (int i = 0; i < nodes.length; ++i) {
			final int j = (i + 1) % nodes.length;
			edges[i] = new Edge(nodes[i], nodes[j]);
		}
	}
	
	
	public Color at(final float u) {
		final float fi = u * edges.length;
		int i = (int) fi;
		if (edges.length <= i)
			i = edges.length - 1;
		return edges[i].at(fi - i);
	}
	
	
	public Color[] spaced(final int n) {
		final Color[] colors = new Color[n];
		final float s = 1.f / (n + 1);
		for (int i = 0; i < n; ++i) {
			colors[i] = at((i + 1) * s);
		}
		return colors;
	}
	
	public Color[] rspaced(final int n) {
		final Color[] colors = new Color[n];
		final float s = 1.f / (n + 1);
		final float off = s * 2 * ((float) Math.random() - 0.5f);
		for (int i = 0; i < n; ++i) {
			colors[i] = at(off + (i + 1) * s);
		}
		return colors;
	}
	
	
	private static final class Edge {
		public final float r0, g0, b0;
		public final float r1, g1, b1;

		
		public Edge(final float[] c0, final float[] c1) {
			this(c0[0], c0[1], c0[2], c1[0], c1[1], c1[2]);
		}
		
		public Edge(
			final float _r0, final float _g0, final float _b0,
			final float _r1, final float _g1, final float _b1
		) {
			this.r0 = _r0;
			this.g0 = _g0;
			this.b0 = _b0;
			this.r1 = _r1;
			this.g1 = _g1;
			this.b1 = _b1;
		}
		
		public Color at(final float u) {
			return new Color(
					lerp(r0, r1, u), 
					lerp(g0, g1, u), 
					lerp(b0, b1, u));
		}
	}
	
	
	
	public static void main(final String[] in) {
		final ColorWalk cw = new ColorWalk();
		final Color[] colors = cw.rspaced(4);
		
		final int w = 50;
		final int h = 100;
		
		final JComponent paintc = new JComponent() {
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				for (int i = 0; i < colors.length; ++i) {
					final int x = 50 + i * w;
					final int y = 50;
					g2d.setPaint(colors[i]);
					g2d.fillRect(x, y, w, h);
				}
			}
		};
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(paintc, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
	}
}
