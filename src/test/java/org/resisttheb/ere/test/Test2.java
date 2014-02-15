package org.resisttheb.ere.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import org.resisttheb.ere.CannyEdgeDetector;
import org.resisttheb.ere.blobs.AdjacencyTest;
import org.resisttheb.ere.blobs.BlobGraph;
import org.resisttheb.ere.blobs.BlobNode;
import org.resisttheb.ere.blobs.BlobPixels;
import org.resisttheb.ere.blobs.BucketQuantizer;
import org.resisttheb.ere.blobs.BufferPixels;
import org.resisttheb.ere.blobs.ClusterQuantizer;
import org.resisttheb.ere.blobs.Column;
import org.resisttheb.ere.blobs.PseudoIndexedBlob;
import org.resisttheb.ere.blobs.SingleIgnoreTest;
import org.resisttheb.ere.blobs.xMarchingSquares;
import org.resisttheb.ere.blobs.xMidpointTracer;
import org.resisttheb.ere.ui.Rgb24ImageCorrector;

import com.infomatiq.jsi.Rectangle;

// TODO: for performance, the blob package should not use Color objects
public class Test2 {

	
	public static void main(final String[] in) {
//		PropertyConfigurator.configure("log4j.properties");
		
		try {
			main2(in);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	
	private static BufferedImage KILLER_BUFFER() {
		final BufferedImage buffer = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		
		for (int i = 0; i < 400; ++i) {
			for (int j = 0; j < 400; ++j) {
				buffer.setRGB(i, j, 1 == (i + j) % 2 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
			}
		}
		
		return buffer;
	}
	
	
	private static void main2(final String[] in) throws IOException {
		final int M = 8;
		final int K = 8;
		
		BufferedImage buffer = SAFE_COPY(ImageIO.read(new File(
//				"c:/temp/green_egg.png"
//				"c:/temp/es/68.jpg"
//				"c:/temp/tree.png"
//				"c:/temp/buck65.png"
//				"c:/temp/cutandrun.png"
//				"c:/temp/white.png"
//				"c:/temp/tiger.jpg"
//				"c:/temp/city_hall_park_fountain.jpg"
//				"c:/temp/tokyo01.jpg"
//				"c:/temp/front.jpg"
//				"c:/temp/greek.jpg"
//				"c:/temp/2005_dime.jpg"
				"c:/temp/croq.jpg"
				)));
//				"c:/temp/tiger.jpg"));
		
//		BufferedImage buffer = KILLER_BUFFER();
		
		final Rgb24ImageCorrector corrector = new Rgb24ImageCorrector(400, 400);
		buffer = corrector.correct(buffer);
		
//		buffer = KILLER_BUFFER();
		
		
		CannyEdgeDetector ed = new CannyEdgeDetector();
//		ed.setContrastNormalized(false);
//		ed.setGaussianKernelRadius(10.f);
//		ed.setGaussianKernelWidth(5);
//		ed.setHighThreshold(50.f);
//		ed.setLowThreshold(5.f);
		
		ed.setSourceImage(buffer);
		//ed.setEdgesImage(new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB));
		ed.process();
		
		
		BufferedImage edgeBuffer = ed.getEdgesImage();
		
		final BufferPixels bp = new BufferPixels(buffer);
		
		final BucketQuantizer bq = new BucketQuantizer(M);
		final ClusterQuantizer cq = new ClusterQuantizer(K);
		
//		buffer = QImage.bucketQuantize(buffer, M);
//		buffer = QImage.kmeansQuantize(buffer, K);
		
		bq.process(bp, bp);
		cq.process(bp, bp);
		
		ImageIO.write(buffer, "png", new File("c:/temp/were_modding_this.png"));
		
		ed = null;
		// TODO:
		// TODO:
		// TODO: create blobs!!
		// TODO:
		// TODO:
		
		
		final PseudoIndexedBlob pib = new PseudoIndexedBlob();
		final BlobPixels pfbs = new BlobPixels();
		pfbs.setData(pib);
		final xMarchingSquares bms = new xMarchingSquares();
		bms.reset(pfbs);
		
		final xMidpointTracer mt = new xMidpointTracer(AdjacencyTest._8);
		
		
		
		
		BlobGraph ebg = new BlobGraph();
		ebg.setMinArea(0);
		ebg.setAdjacencyTest(AdjacencyTest._8);
		ebg.setIgnoreTest(new SingleIgnoreTest(Color.BLACK.getRGB()));
		
		ebg.setMergeAsAgglomerate(true);
		
		ebg.setPixels(new BufferPixels(edgeBuffer));
		ebg.rebuild();
		ebg.compactBlobs();
		edgeBuffer = null;
		
		
		final BlobNode[] eblobs = ebg.getBlobsModifiable();
		final GeneralPath[] contours = new GeneralPath[eblobs.length];
		for (int i = 0; i < eblobs.length; ++i) {
			mt.setData(eblobs[i].getColumnsModifiable());
			final GeneralPath gpath = new GeneralPath();
			mt.trace(gpath);
			contours[i] = gpath;
		}
		
		ebg = null;
		System.gc();
		
		
		final int area = buffer.getWidth() * buffer.getHeight();
		final float f = 0.0025f;
		
		final BlobGraph bg = new BlobGraph();
		// TODO: configure
		bg.setMinArea((int)Math.round(area * f));
		//bg.setMinArea(0);
		
		bg.setMergeAsAgglomerate(true);
		
		bg.setPixels(bp);
		long time0 = System.nanoTime();
		if (! bg.rebuild()) {
			throw new IllegalStateException("too fragemented!");
		}
		long time1 = System.nanoTime();
		
		bg.compactBlobs();
		
		System.out.println(" :: " + (time1 - time0));
		
		buffer = null;
		System.gc();
		
		
		
		
		
		final BlobNode[] blobs = bg.getBlobsModifiable();
		
		
		
		
		// visualize these suckers.
		// interactive ui highlights neighbors
		final int[] move = {0};
		
		final JComponent bvis = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				g2d.setPaint(Color.WHITE);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				
				
//				final double s = 2.0;
				g2d.scale(getWidth() / (float) (bp.x1() - bp.x0()),
						getHeight() / (float) (bp.y1() - bp.y0()));
				
				
//				for (BlobNode blob : blobs) {
//					g2d.setPaint(move[0] == /*blob.lit*/0 ? blob.getRepresentative().brighter() : blob.getRepresentative());
//					for (Column col : blob.getColumnsModifiable()) {
//						g2d.fillRect(col.x, col.y0, 1, col.y1 - col.y0);
//					}
//				}
				
				
				
				final GeneralPath gpath = new GeneralPath();
				for (int k = blobs.length - 1; 0 <= k; --k) {
//					for (int k = 0; k < blobs.length; ++k) {
					final BlobNode blob = blobs[k];
					
					g2d.setPaint(new Color(blob.getRepresentative()));
							//move[0] == /*blob.lit*/0 ? blob.getRepresentative().brighter() : blob.getRepresentative());
					
					
						pib.reset(blob);
//						if (k == 1530) {
//							System.out.println();
//						}
						
						gpath.reset();
						bms.identifyPerimeter(gpath);
						
					
						//area.getPathIterator(null).getWindingRule();
						
//						g2d.fill(area);
						g2d.fill(gpath);
//						break;
						
					
					
					/*
					// MIDPOINT:
					gpath.reset();
					mt.setData(blob.getColumnsModifiable());
					mt.trace(gpath);
					
					g2d.setPaint(new Color(255, 255, 255));
					g2d.setStroke(new BasicStroke(2.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.f, new float[]{1.f, 4.f}, 0.f));
					g2d.draw(gpath);
					*/
				}
				
				
				
				g2d.setPaint(new Color(255, 255, 255, 24));
				for (int i = 0; i < contours.length; ++i) {
					g2d.draw(contours[i]);
				}
				
			}
		};
		
		final MouseInputListener mil = new MouseInputAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				++move[0];
				
				final int x = e.getX();
				final int y = e.getY();
				
				// find blob:
				for (BlobNode blob : blobs) {
					if (blob.bounds().intersects(new Rectangle(x, y, x + 1, y + 1))) {
						boolean hit = false;
						for (Column col : blob.getColumnsModifiable()) {
							if (col.x == x && col.y0 <= y && y < col.y1) {
								hit = true;
								break;
							}
						}
						
//						if (hit) {
//							blob.lit = move[0];
//							for (BlobNode adjblob : blob.adj) {
//								adjblob.lit = move[0];
//							}
//						}
					}
				}
				
				bvis.repaint();
			}
		};
		
		bvis.addMouseListener(mil);
		bvis.addMouseMotionListener(mil);
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(bvis, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);	
		
		
		
		
		
		// STATS:
		
		System.out.println("  number of blobs: " + bg.getBlobsModifiable().length);
		
	}
	
	
	
	
	private static BufferedImage SAFE_COPY(final BufferedImage buffer) {
		final BufferedImage buffer2 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = (Graphics2D) buffer2.getGraphics();
		g2d.drawImage(buffer, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
		g2d.dispose();
		return buffer2;
	}
}
