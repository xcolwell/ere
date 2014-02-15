package org.resisttheb.ere.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

import net.sourceforge.jiu.geometry.Resample;
import net.sourceforge.jiu.gui.awt.BufferedRGB24Image;
import net.sourceforge.jiu.ops.MissingParameterException;
import net.sourceforge.jiu.ops.WrongParameterException;


// takes an input image and puts it into a normal form
// this is just a scale,
// using some interpolator that gives decent results (thanks JIU!)
public class Rgb24ImageCorrector {
	private int maxw;
	private int maxh;
	
	public Rgb24ImageCorrector(final int _maxw, final int _maxh) {
		this.maxw = _maxw;
		this.maxh = _maxh;
	}
	
	
	private int[] computeOutSize(final int inw, final int inh) {
		int outw;
		int outh;

			float wr = (float) inw / maxw;
			float hr = (float) inh / maxh;
			
			if (hr < wr) {
				// scale by wr
				outw = Math.round(inw / wr);
				outh = Math.round(inh / wr);
			}
			else {
				// scale by hr
				outw = Math.round(inw / hr);
				outh = Math.round(inh / hr);
			}
		
		return new int[]{outw, outh};
	}
	
	
	public BufferedImage correct(final BufferedImage in) {
		final int[] outSize = computeOutSize(in.getWidth(), in.getHeight());
		
		if (in.getWidth() == outSize[0] && in.getHeight() == outSize[1])
			return in;
		
		final BufferedImage out = new BufferedImage(outSize[0], outSize[1], BufferedImage.TYPE_INT_ARGB);
		return correct(in, out) ? out 
				: null;
	}
	
	public boolean correct(final BufferedImage in, final BufferedImage out) {
		final int[] outSize = computeOutSize(in.getWidth(), in.getHeight());
		
		Resample resample = new Resample();
		resample.setInputImage(new BufferedRGB24Image(in));
		resample.setOutputImage(new BufferedRGB24Image(out));
		resample.setSize(outSize[0], outSize[1]);
		resample.setFilter(Resample.FILTER_TYPE_LANCZOS3);
		try {
			resample.process();
//			resample.getOutputImage();
		}
		catch (MissingParameterException e) {
			e.printStackTrace();
			return false;
		}
		catch (WrongParameterException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	
	
	
	
	private static BufferedImage TEST() {
		try {
			return ImageIO.read(new File("c:/temp/tokyo01.jpg"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(final String[] in) {
		final BufferedImage buffer = TEST();
		
		final Rgb24ImageCorrector ic = new Rgb24ImageCorrector(400, 400);
		
		final BufferedImage outBuffer = ic.correct(buffer);
		for (int x = 0; x < outBuffer.getWidth(); ++x) {
			for (int y = 0; y < outBuffer.getHeight(); ++y) {
				outBuffer.setRGB(x, y, 
						outBuffer.getRGB(x, y) | (0xFF << 24));
			}
		}
		
		
		final JComponent paintc = new JComponent() {
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				g2d.drawImage(outBuffer, 0, 0, outBuffer.getWidth(), outBuffer.getHeight(), null);
				
				
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
