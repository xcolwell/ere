package org.resisttheb.ere.ui3;

import javax.media.opengl.GL;

public class OglUtilities {
	// int[] modes
	// int[][] firsts
	// int[][] counts
	// convert to a single int[] for a triangles call
	
	public static String toModeName(final int mode) {
		String name;
		switch (mode) {
			case GL.GL_POINT: name = "gl_point"; break;
			case GL.GL_POINTS: name = "gl_points"; break;
			case GL.GL_POLYGON: name = "gl_polygon"; break;
			case GL.GL_QUADS: name = "gl_quads"; break;
			case GL.GL_QUAD_STRIP: name = "gl_quad_strip"; break;
			case GL.GL_TRIANGLES: name = "gl_triangles"; break;
			case GL.GL_TRIANGLE_STRIP: name = "gl_triangle_strip"; break;
			case GL.GL_TRIANGLE_FAN: name = "gl_triangle_fan"; break;
			case GL.GL_LINE: name = "gl_line"; break;
			case GL.GL_LINE_STRIP: name = "gl_line_strip"; break;
			default: name = "default"; break;
		}
		return name;
	}
	
	
	public static short[] toTrianglesIndices(final int off, final int[] modes, final int[][] allFirsts, final int[][] allCounts) {
		// for fan and strip, number of triangles is     1 + (count - 3)
		// 
		
		int ticount = 0;
		for (int i = 0; i < modes.length; ++i) {
			final int[] firsts = allFirsts[i];
			final int[] counts = allCounts[i];
			for (int j = 0; j < firsts.length; ++j) {
				switch (modes[i]) {
					case GL.GL_TRIANGLES:
						ticount += counts[j]; 
						break;
					case GL.GL_TRIANGLE_STRIP:
					case GL.GL_TRIANGLE_FAN: 
						ticount += 3 * (1 + (counts[j] - 3));
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		}
		
		
		final short[] tindices = new short[ticount];
		int tii = 0;
		
		for (int i = 0; i < modes.length; ++i) {
//			final int tii0 = tii;
			
			final int[] firsts = allFirsts[i];
			final int[] counts = allCounts[i];
			for (int j = 0; j < firsts.length; ++j) {
				switch (modes[i]) {
					case GL.GL_TRIANGLES:
						for (int k = firsts[j]; k + 2 < firsts[j] + counts[j]; k += 3, tii += 3) {
							tindices[tii] = (short) (off + k);
							tindices[tii + 1] = (short) (off + k + 1);
							tindices[tii + 2] = (short) (off + k + 2);
						}
						break;
					case GL.GL_TRIANGLE_STRIP:
						for (int k = firsts[j]; k + 2 < firsts[j] + counts[j]; k += 1, tii += 3) {
							tindices[tii] = (short) (off + k);
							tindices[tii + 1] = (short) (off + k + 1);
							tindices[tii + 2] = (short) (off + k + 2);
						}
						break;
					case GL.GL_TRIANGLE_FAN: 
						for (int k0 = firsts[j], k = k0 + 1; k + 1 < firsts[j] + counts[j]; k += 1, tii += 3) {
							tindices[tii] = (short) (off + k0);
							tindices[tii + 1] = (short) (off + k);
							tindices[tii + 2] = (short) (off + k + 1);
						}
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		}
		
		assert tii == tindices.length;
		
		return tindices;
	}
	
}
