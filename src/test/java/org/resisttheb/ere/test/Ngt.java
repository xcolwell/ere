package org.resisttheb.ere.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;

import com.jhlabs.image.GaussianFilter;

// a grid of squares
// noise on each square... how does it look?
public class Ngt {

	public static void main(final String[] in) {
		final int w = 10;
		final int h = 10;
		
		final PerlinNoiseGenerator[][][] ngens = new PerlinNoiseGenerator[w][h][4];
		for (int x = 0; x < w; ++x) {
			for (int y = 0; y < h; ++y) {
				ngens[x][y][0] = new PerlinNoiseGenerator(1, 7);
				ngens[x][y][1] = new PerlinNoiseGenerator(1, 7);
				ngens[x][y][2] = new PerlinNoiseGenerator(1, 7);
				ngens[x][y][3] = new PerlinNoiseGenerator(1, 7);
			}
		}
		final NoiseEvalState nes = ngens[0][0][0].createEvalState();
		final NoiseEvalState nes2 = ngens[0][0][2].createEvalState();
		
		final BufferedImage buffer = new BufferedImage(480, 480, BufferedImage.TYPE_INT_ARGB);
		
		final GaussianFilter blur = new GaussianFilter(5.f);
		
		final JComponent jc = new JComponent() {
			float[][] us = new float[w][h];
			float t = 0.f;
			int i = 0;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				
				//u += 0.113677f;
				t += 0.01f;
				
				final float ccx = buffer.getWidth() / 2.f * (1 + (float) Math.cos(t));
				final float ccy = buffer.getHeight() / 2.f * (1 + (float) Math.sin(t));
				
				
				final Graphics2D g2d = (Graphics2D) buffer.getGraphics();
				
				g2d.setPaint(new Color(97, 227, 39));
				g2d.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
				
				final float sw = 50;
				final float sh = 50;
				final float x0 = -10;
				final float y0 = -10;
				final float nax = 10;
				final float nay = 10;
				
				for (int x = 0; x < w; ++x) {
					for (int y = 0; y < h; ++y) {
						final float s = dist(ccx, ccy, sw * x, sh * y) / dist(0, 0, w * sw, h * sh);
						
						assert s <= 1.f;
						
						us[x][y] += s * 0.113677f;
						float u = us[x][y];
						
						float cx = x0 + x * sw + sw / 2.f;
						float cy = y0 + y * sh + sh / 2.f;
						
						float nx = ngens[x][y][0].noise(nes, 
								SmoothingFunction.S_CURVE, 
//								0.113775f + x / (float) w,
//								0.113775f + y / (float) h,
								(0.113775f + x / (float) w) + u
							);
						float ny = ngens[x][y][1].noise(nes, 
								SmoothingFunction.S_CURVE, 
//								0.113775f + x / (float) w,
//								0.113775f + y / (float) h,
								(0.113775f + y / (float) h) + u
							);
						
						nx *= nax;
						ny *= nay;
						
						cx += nx;
						cy += ny;
						
						float usw = sw + 3 * ngens[x][y][2].noise(nes2, 
								SmoothingFunction.S_CURVE, 
								u
							);
						float ush = sh + 3 * ngens[x][y][3].noise(nes2, 
								SmoothingFunction.S_CURVE, 
								u
							);
						
						g2d.setPaint(new Color(34, 59, 23));
						g2d.fill(new Rectangle2D.Float(
								cx - usw / 2.f,
								cy - ush / 2.f,
								usw,
								ush
						));
					}
				}
				
//				blur.filter(buffer, buffer);
				
//				try {
//				ImageIO.write(buffer, "png", new File("c:/temp/occ/" + i + ".png"));
//				}
//				catch (Throwable t) {
//					t.printStackTrace();
//				}
				++i;
				g.drawImage(buffer, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
			}
		};
		
		
		final JFrame frame = new JFrame();
		
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(jc, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		
		Timer t = new Timer(1000 / 24, new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				jc.repaint();
			}
		});
		t.start();
		
	}
	
	
	private static float dist(final float x0, final float y0, final float x1, final float y1) {
		final float dx = x1 - x0;
		final float dy = y1 - y0;
		
		return (float) Math.sqrt(dx * dx + dy * dy);
	}
}
