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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;


/*
 * - status string for each row    (string)
 * - opacity for each row status   (status opacity)
 * 
 * model for each row:  
 *    - position      (slide amount    [0, 1])
 *    - enabled/disabled    
 * 
 * a queue of rows that need to pop out
 */

// a moving 2d perlin noise map
public class CrazyBar {

	private static BufferedImage GET_PX() {
		try {
			final URL url = ClassLoader.getSystemClassLoader().getResource("ere.png");
			final InputStream is = url.openStream();
			try {
				return ImageIO.read(is);
			}
			finally {
				is.close();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static BufferedImage GET_PX2() {
		try {
				return ImageIO.read(new File("c:/temp/ere2.png"));
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static BufferedImage SAFE_COPY(final BufferedImage buffer) {
		final BufferedImage buffer2 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = (Graphics2D) buffer2.getGraphics();
		g2d.drawImage(buffer, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
		g2d.dispose();
		return buffer2;
	}
	
	
	
	public static void main(final String[] in) {
		final BufferedImage buffer = SAFE_COPY(GET_PX2());
		
		
		final int w = buffer.getWidth();
		final int h = buffer.getHeight();
		
		
		final int[][] colors = new int[w][h];
		for (int x = 0; x < w; ++x) {
			final int[] xcolors = colors[x];
			for (int y = 0; y < h; ++y) {
				final int rgb = buffer.getRGB(x, y);
				xcolors[y] = 0x00 == (rgb >> 24) ? 0x00 : rgb;
			}
		}
		
		final int[] outColors = new int[w * h];
		for (int i = 0; i < outColors.length; ++i) {
			outColors[i] = 0x00;
		}
		
		
		final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		
		final PerlinNoiseGenerator ngen = new PerlinNoiseGenerator(3, 3);
		final NoiseEvalState nes = ngen.createEvalState();
		
		
		final float s = .35f * 1.f / 11.f;
		final int[] i = {0};
		
		
		
		final JComponent cbc = new JComponent() {
			
			@Override
			protected void paintComponent(final Graphics _g) {
				super.paintComponent(_g);
				
				final Graphics2D g2d = (Graphics2D) _g;
				
				g2d.setPaint(Color.BLACK);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				
				
//				g2d.setPaint(Color.RED);
//				g2d.fillRect(20, 20, 300, 300);
				
				
				
				
				for (int x = 0; x < w; ++x) {
					final int[] xcolors = colors[x];
					
//					if ((x % 2) == 1) continue;
					for (int y = 0; y < h; ++y) {
//						if ((y % 2) == 1) continue;
						
						final int color = xcolors[y];
						
//						Color color = new Color(buffer.getRGB(x, y), true);
						
						if (0x00 == color) {
//							out.setRGB(x, y, 0x00);
							continue;
						}
						
						float n = ngen.noise(nes, SmoothingFunction.S_CURVE,
								s * x, s * y, 0.1337f + 0.2f * 0.331177434f * i[0]);
						
						n = (n + 1) / 2;
						
						final float m = (2 * n + 1) / 4;
						
						int r = color & 0xFF;
						int g = (color >> 8) & 0xFF;
						int b = (color >> 16) & 0xFF;
						int a = (color >> 24) & 0xFF;
						
						r *= m;
						g *= m;
						b *= m;
						
						//a = (int) ((a + 0xFF * n) / 2);
						a *= m;
						
						r *= 1.9f;
						g *= 1.9f;
						b *= 1.9f;
						
						if (255 < r) r = 255;
						if (255 < g) g = 255;
						if (255 < b) b = 255;
						
						
//						color = new Color(
//								Math.round(color.getRed() * ), 
//								Math.round(color.getGreen() * (2 * n + 1) / 3), 
//								Math.round(color.getBlue() * (2 * n + 1) / 3), 
//								Math.round((color.getAlpha() + (255 * ( n))) / 2)
//								
//							);
							//new Color(n, n, n, 1 - n);
						
//						g2d.setPaint(color);
//						final int cw = 1;
//						final int ch = 1;
//						final float bleed = 1;
							//5;
							//1.3f * 5f;
						//g2d.fill(new Rectangle2D.Float(50 + cw * x, 50 + ch * y, bleed * cw, bleed * ch));
//						g2d.fillRect(cw * x, ch * y, Math.round(bleed * cw), 
//								Math.round(bleed * ch));
						
						out.setRGB(x, y, 
								r | (g << 8) | (b << 16) | (a << 24)
						
						);
						
						//outColors[k] = r | (g << 8) | (b << 16) | (a << 24);
						
					}
				}
				
//				out.setRGB(0, 0, w, h, outColors, 0, w);
				
				g2d.drawImage(out, 
					50, 50, out.getWidth(), out.getHeight(), null);
			}
		};
		
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(cbc, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		
		final Timer timer = new Timer(1000 / 8, new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				++i[0];
				cbc.repaint();
			}
		});
		timer.start();
	}
}
