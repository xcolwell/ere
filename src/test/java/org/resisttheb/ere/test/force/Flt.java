package org.resisttheb.ere.test.force;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.resisttheb.ere.test.force.ForceLayout.Edge;
import org.resisttheb.ere.test.force.ForceLayout.Node;
import org.resisttheb.ere.ui2.CurveUtilities;
import org.resisttheb.nug.NumericUtilities;

import prefuse.util.force.ForceItem;

// force layout test
// draws bezier (cubic) edges
public class Flt {

	public static void main(final String[] in) {
		
		final ForceLayout fl = new ForceLayout();
		
		final ForceLayout.Node[] nodes = new ForceLayout.Node[100];
		
		
		final float nodeMass = 1.f;
		final float edgeMass = 0.01f;
		
		
		for (int i = 0; i < nodes.length; ++i) {
			nodes[i] = new ForceLayout.Node(new ForceItem(), nodeMass);
		}
		
		
		final List<Edge> edgesList = new ArrayList<Edge>();
		
		for (int i = 0; i < nodes.length; ++i) {
			for (int j = i + 1; j < nodes.length; ++j) {
				if (0.03 < Math.random()) {
					continue;
				}
				
				final ForceItem[] fitems = new ForceItem[2];
				for (int k = 0; k < fitems.length; ++k) {
					fitems[k] = new ForceItem();
				}
				
				
				edgesList.add(new ForceLayout.Edge(nodes[i], nodes[j], 
					fitems,
					edgeMass
				));
			}
		}
		
		final ForceLayout.Edge[] edges = edgesList.toArray(new Edge[edgesList.size()]);
		
		fl.setup(new Point2D.Float(300.f, 300.f), 
				nodes, edges);
		
		
		fl.run();
		
		
		vis(nodes, edges);
		
	}
	
	
	
	private static void vis(final Node[] nodes, final Edge[] edges) {
		final boolean[] renderLines = {true};
		final JComponent visc = new JComponent() {
			
			
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				g2d.setPaint(Color.WHITE);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				
				
				g2d.translate(getWidth() / 2.f, getHeight() / 2.f);
				g2d.scale(0.8f, 0.8f);
				g2d.translate(-getWidth() / 2.f, -getHeight() / 2.f);
				
				g2d.setPaint(Color.BLACK);
				
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				
				
				// 1. draw edges
				// 2. draw nodes
				
				for (Edge edge : edges) {
					
					if (renderLines[0]) {
						final Path2D.Float path = new Path2D.Float();
						path.moveTo(edge.n0.fitem.location[0],
							edge.n0.fitem.location[1]);
						path.lineTo(edge.n1.fitem.location[0],
								edge.n1.fitem.location[1]);
						g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						
						g2d.draw(path);
					}
					else {
					
					
//					Point2D.Float boxc0 = boxc0(edge.n0.fitem.location[0],
//							edge.n0.fitem.location[1],
//					edge.fitems[1].location[0],
//					edge.fitems[1].location[1],
//					edge.fitems[1].location[0],
//					edge.fitems[1].location[1],
//					edge.n1.fitem.location[0],
//					edge.n1.fitem.location[1]);
					
//					Point2D.Float boxc1 = boxc1(edge.n0.fitem.location[0],
//							edge.n0.fitem.location[1],
//							edge.fitems[1].location[0],
//							edge.fitems[1].location[1],
//							edge.fitems[1].location[0],
//							edge.fitems[1].location[1],
//							edge.n1.fitem.location[0],
//							edge.n1.fitem.location[1]);
					
					float[] coords = {
							edge.n0.fitem.location[0],
							edge.n0.fitem.location[1],
					edge.fitems[1].location[0],
					edge.fitems[1].location[1],
					edge.fitems[1].location[0],
					edge.fitems[1].location[1],
					edge.n1.fitem.location[0],
					edge.n1.fitem.location[1]
					};
//					fscale(coords, boxc0, boxc1);
					
					
					final float uUp = search(true, coords, new Rectangle2D.Float(coords[0] - 10, coords[1] - 10, 20, 20));
					final float uDown =
						search(false, coords, new Rectangle2D.Float(coords[6] - 10, coords[7] - 10, 20, 20));
					
					final float[][] sbounds = CurveUtilities.split(coords, 3, uUp, uDown);
					coords = sbounds[1];
					
					final Path2D.Float path = new Path2D.Float();
					path.moveTo(coords[0], coords[1]);
					path.curveTo(
							coords[2], coords[3],
							coords[4], coords[5],
							coords[6], coords[7]
					);
					
//					path.lineTo(edge.n1.fitem.location[0],
//							edge.n1.fitem.location[1]);
					
					float len = 
						len(
							edge.n0.fitem.location[0],
							edge.n0.fitem.location[1],
					
					
							edge.fitems[0].location[0], 
							edge.fitems[0].location[1],
							edge.fitems[1].location[0],
							edge.fitems[1].location[1],
							edge.n1.fitem.location[0],
							edge.n1.fitem.location[1]
							                       );
					
					float t = 
//						len < 300 ? 0.5f
//							: len < 600 ? 1.f
//								: len < 750 ? 1.5f : 2.f;
						(float) Math.pow(len / 360.0, 2.5);
					g2d.setStroke(new BasicStroke(t, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					final Shape tclip = g2d.getClip();
//					Area area = new Area(new Rectangle2D.Float(0, 0, getWidth(), getHeight()));
//					area.subtract(new Area(new Rectangle2D.Float(
//							edge.n0.fitem.location[0] - 10,
//							edge.n0.fitem.location[1] - 10, 20, 20)));
//					area.subtract(new Area(new Rectangle2D.Float(
//							edge.n1.fitem.location[0] - 10,
//							edge.n1.fitem.location[1] - 10, 20, 20)));
//					g2d.setClip(area);
					g2d.draw(path);
//					g2d.setClip(tclip);
					
					g2d.fill(new Ellipse2D.Float(coords[0] - 1f, coords[1] - 1f, 2, 2));
					g2d.fill(new Ellipse2D.Float(coords[6] - 1f, coords[7] - 1f, 2, 2));
					}
//					g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//					g2d.draw(new Line2D.Float(boxc1[0], boxc1[1], edge.n1.fitem.location[0],
//							edge.n1.fitem.location[1]));
//					g2d.draw(new Line2D.Float(edge.n0.fitem.location[0],
//							edge.n0.fitem.location[1], boxc0[0], boxc0[1]));
				}
				
				
				for (Node node : nodes) {
					g2d.fill(new Rectangle2D.Float(node.fitem.location[0] - 5, node.fitem.location[1] - 5,
							10, 10));
				}
				
				
			}
		};
		
		visc.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				renderLines[0] = !renderLines[0];
				visc.repaint();
			}
		});
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(visc, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
	}
	
	
	private static float len(final float ... points) {
		float net = 0.f;
		float px = points[0];
		float py = points[1];
		for (int i = 2; i + 1 < points.length; i += 2) {
			float x = points[i];
			float y = points[i + 1];
			final float dx = x - px;
			final float dy = y - py;
			net += (float) Math.sqrt(dx * dx + dy * dy);
			px = x;
			py = y;
		}
		return net;
	}
	
	
	private static Point2D.Float boxc0(float x0, float y0, float cx0, float cy0, float cx1, float cy1,  float x1, float y1) {
		final float w = 20.f;
		final float h = 20.f;
		
		float ustep = 0.001f;
		
		final float dx = x1 - x0;
		final float dy = y1 - y0;
		
		float u = 1.f;
		while (! new Rectangle2D.Float(x0 - w/2, y0 - h/2, w, h).contains(
			eval(u - ustep, x0, y0, cx0, cy0, cx1, cy1, x1, y1))) {
			u -= ustep;
		}
		
		return eval(u, x0, y0, cx0, cy0, cx1, cy1, x1, y1);
	}
	
	// x1, y1 are the box center;
	private static Point2D.Float boxc1(float x0, float y0, float cx0, float cy0, float cx1, float cy1, float x1, float y1) {
		final float w = 20.f;
		final float h = 20.f;
		
		float ustep = 0.001f;
		
		final float dx = x0 - x1;
		final float dy = y0 - y1;
		
		float u = 1.f;
		while (! new Rectangle2D.Float(x1 - w/2, y1 - h/2, w, h).contains(
				eval(1 - (u - ustep), x0, y0, cx0, cy0, cx1, cy1, x1, y1))) {
			u -= ustep;
		}
		
		return eval(1 - (u - ustep), x0, y0, cx0, cy0, cx1, cy1, x1, y1);
	}
	
	
	
	private static float search(final boolean up, 
			final float[] bounds, final Rectangle2D.Float box) {
		final float[] out = new float[2];
		final Point2D.Float point = new Point2D.Float();
		
		float u;
		final float ustep0;
		final float ustep1;
		final float ustep2;
		if (up) {
			u = 0.f;
			ustep0 = 0.1f;
			ustep1 = 0.01f;
			ustep2 = 0.001f;
		}
		else {
			u = 1.f;
			ustep0 = -0.1f;
			ustep1 = -0.01f;
			ustep2 = -0.001f;
		}
		
		while (box.contains(eval3(bounds, u, out, point))) {
			u += ustep0;
		}
		u -= ustep1;
		while (! box.contains(eval3(bounds, u, out, point))) {
			u -= ustep1;
		}
		u += ustep2;
		while (box.contains(eval3(bounds, u, out, point))) {
			u += ustep2;
		}
		
		return u;
	}
	
	
	private static Point2D.Float eval3(final float[] bounds, final float u, 
			final float[] out, final Point2D.Float point
	) {
		CurveUtilities.eval3(bounds, u, out);
		point.x = out[0];
		point.y = out[1];
		return point;
	}
	
	
	private static Point2D.Float eval(float u, float x0, float y0, float cx0, float cy0, float cx1, float cy1, float x1, float y1) {
		
		float v = 1 - u;
		
		return new Point2D.Float(
			v * v * v * x0 + 3 * u * v * v * cx0 + 3 * u * u * v * cx1 + u * u * u * x1,
			v * v * v * y0 + 3 * u * v * v * cy0 + 3 * u * u * v * cy1 + u * u * u * y1
			);
		
	}
	
	
	
	
	public static void fscale(final float[] coords, Point2D.Float bx0, Point2D.Float bx1) {
		Line2D.Float base = new Line2D.Float(coords[0], coords[1], coords[6], coords[7]);
		
		float[] coords0 = {
				coords[2],
				coords[3],
				coords[4],
				coords[5],
				coords[6],
				coords[7]
		};
		
		
//		scale( coords0, 6, base, new Line2D.Float(bx0.x, bx0.y, bx1.x, bx1.y) );
		
		
		scale( coords0, 6, base, new Line2D.Float(base.x1, base.y1, bx1.x, bx1.y) );
		
		float[] coords1 = {
				coords0[2],
				coords0[3],
				coords0[0],
				coords0[1],
				coords[0],
				coords[1],
		};
		
		scale( coords1, 6, new Line2D.Float(bx1.x, bx1.y, base.x1, base.y1), 
				new Line2D.Float(bx1.x, bx1.y, bx0.x, bx0.y) );
		
		coords[0] = coords1[4];
		coords[1] = coords1[5];
		
		coords[2] = coords1[2];
		coords[3] = coords1[3];
		
		coords[4] = coords1[0];
		coords[5] = coords1[1];
		
		coords[6] = coords0[4];
		coords[7] = coords0[5];
		
	}
	
	
	
	
	public static void scale(final float[] coords, final Line2D.Float base, final Line2D.Float ext) {
		scale( coords, coords.length, base, ext );
	}
	
	public static void scale(final float[] coords, final int n, final Line2D.Float base, final Line2D.Float ext) {
		scale( coords, n, base.x1, base.y1, base.x2, base.y2, ext.x1, ext.y1, ext.x2, ext.y2 );
	}
	
	public static void scale(
			final float[] coords, final int n, final float x1, final float y1, final float x2, final float y2, 
			final float tx1, final float ty1, final float tx2, final float ty2
	) {
		
		
		
		
		final float xa, xb, ya, yb;
		if ( NumericUtilities.fequal( x2, x1 ) ) {
			xa = 0.f;
			xb = 1.f;
		}
		else {
			xa = ( tx1 * x2 - x1 * tx2 ) / ( x2 - x1 );
			xb = ( tx2 - tx1 ) / ( x2 - x1 );
		}
		if ( NumericUtilities.fequal( y2, y1 ) ) {
			ya = 0.f;
			yb = 1.f;
		}
		else {
			ya = ( ty1 * y2 - y1 * ty2 ) / ( y2 - y1 );
			yb = ( ty2 - ty1 ) / ( y2 - y1 );
		}
		
		
		System.out.println( "BEGIN SCALE (" + n + ") [" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ";  " + 
				tx1 + ", " + ty1 + ", " + tx2 + ", " + ty2 + "; ]" );
		for ( int i = 0; i < n; i++ ) { 
			final float preScaleCoord = coords[ i ];
			coords[ i ] = 0 == i % 2 ? xa + xb * coords[ i ] 
					: ya + yb * coords[ i ];
			System.out.println( "    " + preScaleCoord + " -> " + coords[ i ] );
		}
		System.out.println( "END SCALE" );
	}
	
}
