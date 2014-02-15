package org.resisttheb.ere.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.layout.LC;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.resisttheb.ere.ui0.ColorWalk;


// components for entering answers to questions
// each one is either active or deactive
// slide [0, 1] into view
public class Test0 {

	private static Font createFont(final String name) {
		try {
			final URL erasLightUrl = ClassLoader.getSystemClassLoader().getResource(name);
			final URLConnection conn = erasLightUrl.openConnection();
			final InputStream is = conn.getInputStream();
			try {
				return Font.createFont(Font.TRUETYPE_FONT, is);
			}
			finally {
				is.close();
			}
		}
		catch (FontFormatException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	private static Font createErasLight() {
		return createFont("ERASLGHT.TTF");
	}
	
	private static Font createErasBold() {
		return createFont("ERASBD.TTF");
	}
	
	private static Font createErasMedium() {
		return createFont("ERASMD.TTF");
	}
	
	
	private static Font erasLight;
	private static Font erasMedium;
	private static Font erasBold;
	
	
	public static void main(final String[] in) {
		//Font.TRUETYPE_FONT;
		
		String antialising = "swing.aatext";
        if (null == System.getProperty(antialising)) {
            System.setProperty(antialising, "true");
        }
		
		try {
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
		}
		catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		erasLight = createErasLight();
		erasBold = createErasBold();
		erasMedium = createErasMedium();
		
		
		final ColorWalk cw = new ColorWalk();
		final Color[] colors = cw.rspaced(4);
		
		
		
		final JPanel panel = new JPanel();
//		panel.setOpaque(false);
		panel.setBackground(Color.BLACK);
		
		final MigLayout ml = new MigLayout();
		final LC lcs = new LC();
		lcs.setInsets(new UnitValue[]{
				new UnitValue(0.f),
				new UnitValue(0.f),
				new UnitValue(0.f),
				new UnitValue(0.f)
			});
		ml.setLayoutConstraints(lcs);
		panel.setLayout(ml);
		
		
		
		final int maxw = 650;
		
		final JComponent row0 = createInputComponent(colors[0], 
				"ooh girl", "(perth, australia)", 
				maxw);
		
		final JComponent row1 = createInputComponent(colors[1], 
				"there are lots of clouds in the sky today and rain drops too", "(perth, australia)", 
				maxw);
		
		final JComponent row2 = createInputComponent(colors[2], 
				"ooh girl", "(perth, australia)", 
				maxw);
		
		final JComponent row3 = createInputComponent(colors[3], 
				"ooh girl", "(perth, australia)", 
				maxw);
		
		
		panel.add(row0, "growx, wrap, align left");
		panel.add(row1, "growx, wrap, align left");
		panel.add(row2, "growx, wrap, align left");
		panel.add(row3, "growx, wrap, align left");
		
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(panel, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(400, 400);
		frame.setVisible(true);
		
		
		
		
	}
	
	
	
	
	private static JComponent createInputComponent(final Color color, final String q, final String loc, final int maxw) {
	
		final Border b = new CompoundBorder(
				new MatteBorder(0, 8, 0, 8, color),
				new EmptyBorder(2, 20, 2, 20)
		);
		
		float mainSize = 24.f;
		JPanel panel = new JPanel();
		panel.setBorder(b);
		panel.setOpaque(false);
		
		
		final MigLayout ml = new MigLayout();
		final LC lcs = new LC();
		lcs.setInsets(new UnitValue[]{
				new UnitValue(2.f),
				new UnitValue(2.f),
				new UnitValue(2.f),
				new UnitValue(2.f)
			});
		ml.setLayoutConstraints(lcs);
		panel.setLayout(ml);
		
		
//		final Border b = new CompoundBorder(
//			new MatteBorder(1, 0, 1, 3, Color.BLACK),
//			new EmptyBorder(2, 2, 2, 5)
//		);
		
		final JLabel label0 = new JLabel(q);
		label0.setFont(erasMedium.deriveFont(mainSize));
		label0.setForeground(color);
//		label0.setBorder(b);
		
		final JLabel label1 = new JLabel(loc);
		label1.setFont(erasMedium.deriveFont(16.f));
		label1.setForeground(color);
		
//		final JLabel label1 = new JLabel("this is a test");
//		label1.setFont(erasLight.deriveFont(18.f));
//		label1.setBorder(b);
		
		final JTextField text = new JTextField();
		text.setFont(erasMedium.deriveFont(14.f));
		
		text.setCaretColor(color);
		text.setSelectionColor(color);
		text.setSelectedTextColor(Color.BLACK);
		
//		text.setBackground(Color.BLACK);
		text.setOpaque(false);
		text.setForeground(Color.WHITE);
		
		
		final JButton button = new JButton("ok");
		button.setFont(erasMedium.deriveFont(14.f));
		button.setOpaque(true);
//		button.setBorderPainted(false);
//		button.setBorder(null);
//		button.set
		button.setForeground(Color.BLACK);
		
		
		final JRadioButton b0 = new JRadioButton("search");
		b0.setFont(erasMedium.deriveFont(14.f));
		final JRadioButton b1 = new JRadioButton("upload");
		b1.setFont(erasMedium.deriveFont(14.f));
		
//		b0.setBackground(Color.BLACK);
//		b1.setBackground(Color.BLACK);
		b0.setOpaque(false);
		b1.setOpaque(false);
		b0.setForeground(Color.WHITE);
		b1.setForeground(Color.WHITE);
		
		final ButtonGroup bg = new ButtonGroup();
		bg.add(b0);
		bg.add(b1);
		
		b0.setSelected(true);
		
		
		panel.add(label0, "width 200::, split 2");
		panel.add(label1, "gapleft 20, wrap");
//		panel.add(label1, "growx, wrap");
		panel.add(text, "growx, split 2");
		panel.add(button, "gapleft 15, wrap");
		panel.add(b0, "split 2, align left");
		panel.add(b1);
		
		
		
		
		panel.validate();
		while (12 < mainSize && maxw < panel.getPreferredSize().width) {
			--mainSize;
			label0.setFont(erasMedium.deriveFont(mainSize));
			panel.validate();
		}
		
		return panel;
	}
	
	
}
