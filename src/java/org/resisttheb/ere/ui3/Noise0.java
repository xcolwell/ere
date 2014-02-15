package org.resisttheb.ere.ui3;

import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;

public class Noise0 {

	
	private final int n;
	private final int k = 1;
	private final PerlinNoiseGenerator[][] ngens;
	private final NoiseEvalState nes;
	private final float[] us;
	private final float[] ratems;
	private float theta = 0.f;

	
	public float m = 0.35f * 0.113677f;
	public final float[][] noise;
	
	
	public Noise0(final int _n) {
		this.n = _n;
		noise = new float[n][k];
		us = new float[n];
		// thanks jvm fo initializing us to all zeros
		
		ngens = new PerlinNoiseGenerator[n][k];
		for (int i = 0; i < n; ++i) {
			ngens[i][0] = new PerlinNoiseGenerator(1, 7);
		}
		
		nes = ngens[0][0].createEvalState();
		
		// Noise offsets and rate multipliers:
		ratems = new float[n];
		for (int i = 0; i < n; ++i) {
			us[i] = (float) Math.random();
			ratems[i] = (float) Math.random();
		}
	}
	
	
	
	public void advance() {
		theta += 0.01f;
		
		for (int i = 0; i < n; ++i) {
			us[i] += ratems[i] * m;
			
			for (int j = 0; j < k; ++j) {
				noise[i][j] = 0.5f * (1 + ngens[i][j].noise(nes, SmoothingFunction.S_CURVE,
						us[i]));
			}
		}
	}
}
