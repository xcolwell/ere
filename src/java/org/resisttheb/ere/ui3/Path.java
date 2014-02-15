package org.resisttheb.ere.ui3;


public interface Path {
	public float length();
	public void eval(final float u, final float[] coords);
}

