package org.resisttheb.ere.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;

public class UiUtilities {
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	public static BufferedImage ensureSafeType(final BufferedImage buffer) {
		if (BufferedImage.TYPE_INT_ARGB == buffer.getType())
			return buffer;
		final int w = buffer.getWidth();
		final int h = buffer.getHeight();
		final BufferedImage buffer2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = (Graphics2D) buffer2.getGraphics();
		g2d.drawImage(buffer, 0, 0, w, h, null);
		g2d.dispose();
		return buffer2;
	}
	
	
	public static Color lerp(final Color a, final Color b, final float u) {
		return new Color(
				lerp(a.getRed(), b.getRed(), u),
				lerp(a.getGreen(), b.getGreen(), u),
				lerp(a.getBlue(), b.getBlue(), u),
				lerp(a.getAlpha(), b.getAlpha(), u)
			);
	}
	
	public static int lerp(final int a, final int b, final float u) {
		return a + Math.round((b - a) * u);
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	

	public static final float DEFAULT_FONT_SIZE = 16.f;
	
	/**************************
	 * NOISE SIZE
	 **************************/
	private static final PerlinNoiseGenerator ngen = new PerlinNoiseGenerator(1, 8);
	private static final NoiseEvalState nes = ngen.createEvalState();
	private static float nu = 0.3122488f;
	
	public static synchronized float noiseSize() {
		nu += 2 * 0.01337559;
		return 8 + DEFAULT_FONT_SIZE *(1 + ngen.noise(SmoothingFunction.S_CURVE, nu));
	}
	/**************************
	 * END NOISE SIZE
	 **************************/
	
	
	private UiUtilities() {
	}
}
