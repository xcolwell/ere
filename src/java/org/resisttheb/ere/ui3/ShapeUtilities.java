package org.resisttheb.ere.ui3;

import javax.media.opengl.GL;


public class ShapeUtilities {
	private static float invSqrt(float x) {
		float xhalf = 0.5f*x;
	    int i = Float.floatToIntBits(x);
	    i = 0x5f3759df - (i>>1);
	    x = Float.intBitsToFloat(i);
	    x = x*(1.5f - xhalf*x*x);
	    return x;
	}
	

	// ccoords are center coordinates
	// th is the thickness
	public static int toQuadStrip(
		final float[] ccoords, final float th,
		final QuadStripCallback callback
	) {
		final float hth = th / 2.f;
		final float hhth = hth / 2.f;
		
		final int N = ccoords.length / 2;
		
		int count = 0;
		
		
		float ptopx; 	float ptopy;
		float pbotx; 	float pboty;
		float topx; 	float topy;
		float botx; 	float boty;
		
		
		
		float dx = ccoords[2] - ccoords[0];
		float dy = ccoords[3] - ccoords[1];
//		float n = invSqrt(dx * dx + dy * dy);
		float n = dx * dx + dy * dy;
		{
			// inverse square root
			float nhalf = 0.5f*n;
		    int ni = Float.floatToIntBits(n);
		    ni = 0x5f3759df - (ni>>1);
		    n = Float.intBitsToFloat(ni);
		    n = n*(1.5f - nhalf*n*n);
		}
		dx *= n;
		dy *= n;
		
		ptopx = dy; 	ptopy = -dx;
		pbotx = -dy;	pboty = dx;
		
		callback.start(
			ccoords[0] + ptopx * hth,
			ccoords[1] + ptopy * hth,
			ccoords[0] + pbotx * hth,
			ccoords[1] + pboty * hth
		);
		count += 4;
		
		for (int i = 2; i < N; ++i, 
			ptopx = topx, ptopy = topy, pbotx = botx, pboty = boty
		) {
			// j and k are indices into ccoords
			final int pi = (i - 1) << 1;
			final int ci = i << 1;
			
			dx = ccoords[ci] - ccoords[pi];
			dy = ccoords[ci + 1] - ccoords[pi + 1];
			n = dx * dx + dy * dy;
			{
				// inverse square root
				float nhalf = 0.5f*n;
			    int ni = Float.floatToIntBits(n);
			    ni = 0x5f3759df - (ni>>1);
			    n = Float.intBitsToFloat(ni);
			    n = n*(1.5f - nhalf*n*n);
			}
//			n = invSqrt(dx * dx + dy * dy);
			dx *= n;
			dy *= n;
			
			topx = dy;
			topy = -dx;
			botx = -dy;
			boty = dx;
			
			callback.side(
				ccoords[pi] + (ptopx + topx) * hhth,
				ccoords[pi + 1] + (ptopy + topy) * hhth,
				ccoords[pi] + (pbotx + botx) * hhth,
				ccoords[pi + 1] + (pboty + boty) * hhth
			);
			count += 4;
		}
		
		
		callback.end(
			ccoords[ccoords.length - 2] + ptopx * hth,
			ccoords[ccoords.length - 1] + ptopy * hth,
			ccoords[ccoords.length - 2] + pbotx * hth,
			ccoords[ccoords.length - 1] + pboty * hth
		);
		count += 4;
		
		return count / 2;
	}
	
	
	public static int toQuadStrip(
			final float[] ccoords, final float th,
			final GL gl
		) {
			final float hth = th / 2.f;
			final float hhth = hth / 2.f;
			
			final int N = ccoords.length / 2;
			
			int count = 0;
			
			
			float ptopx; 	float ptopy;
			float pbotx; 	float pboty;
			float topx; 	float topy;
			float botx; 	float boty;
			
			
			
			float dx = ccoords[2] - ccoords[0];
			float dy = ccoords[3] - ccoords[1];
//			float n = invSqrt(dx * dx + dy * dy);
			float n = dx * dx + dy * dy;
			{
				// inverse square root
				float nhalf = 0.5f*n;
			    int ni = Float.floatToIntBits(n);
			    ni = 0x5f3759df - (ni>>1);
			    n = Float.intBitsToFloat(ni);
			    n = n*(1.5f - nhalf*n*n);
			}
			dx *= n;
			dy *= n;
			
			ptopx = dy; 	ptopy = -dx;
			pbotx = -dy;	pboty = dx;
			
			gl.glVertex2f(
					ccoords[0] + ptopx * hth,
					ccoords[1] + ptopy * hth);
				gl.glVertex2f(ccoords[0] + pbotx * hth,
					ccoords[1] + pboty * hth
				);
			count += 4;
			
			for (int i = 2; i < N; ++i, 
				ptopx = topx, ptopy = topy, pbotx = botx, pboty = boty
			) {
				// j and k are indices into ccoords
				final int pi = (i - 1) << 1;
				final int ci = i << 1;
				
				dx = ccoords[ci] - ccoords[pi];
				dy = ccoords[ci + 1] - ccoords[pi + 1];
				n = dx * dx + dy * dy;
				{
					// inverse square root
					float nhalf = 0.5f*n;
				    int ni = Float.floatToIntBits(n);
				    ni = 0x5f3759df - (ni>>1);
				    n = Float.intBitsToFloat(ni);
				    n = n*(1.5f - nhalf*n*n);
				}
//				n = invSqrt(dx * dx + dy * dy);
				dx *= n;
				dy *= n;
				
				topx = dy;
				topy = -dx;
				botx = -dy;
				boty = dx;
				
				gl.glVertex2f(
						ccoords[pi] + (ptopx + topx) * hhth,
						ccoords[pi + 1] + (ptopy + topy) * hhth);
					gl.glVertex2f(ccoords[pi] + (pbotx + botx) * hhth,
						ccoords[pi + 1] + (pboty + boty) * hhth
					);
				count += 4;
			}
			
			
			gl.glVertex2f(
					ccoords[ccoords.length - 2] + ptopx * hth,
					ccoords[ccoords.length - 1] + ptopy * hth);
				gl.glVertex2f(ccoords[ccoords.length - 2] + pbotx * hth,
					ccoords[ccoords.length - 1] + pboty * hth
				);
			count += 4;
			
			return count / 2;
		}
	
	
	public static int toQuadStrip(
			final float[] ccoords, final float[] hths, final int N,
			final GL gl
		) {
//		final int N = ccoords.length / 2;
		
		int count = 0;
		
		
		float ptopx; 	float ptopy;
		float pbotx; 	float pboty;
		float topx; 	float topy;
		float botx; 	float boty;
		
		
		
		float dx = ccoords[2] - ccoords[0];
		float dy = ccoords[3] - ccoords[1];
//		float n = invSqrt(dx * dx + dy * dy);
		float n = dx * dx + dy * dy;
		{
			// inverse square root
			float nhalf = 0.5f*n;
		    int ni = Float.floatToIntBits(n);
		    ni = 0x5f3759df - (ni>>1);
		    n = Float.intBitsToFloat(ni);
		    n = n*(1.5f - nhalf*n*n);
		}
		dx *= n;
		dy *= n;
		
		ptopx = dy; 	ptopy = -dx;
		pbotx = -dy;	pboty = dx;
		
		float hth = hths[0];
		
		
		gl.glVertex2f(
			ccoords[0] + ptopx * hth,
			ccoords[1] + ptopy * hth);
		gl.glVertex2f(ccoords[0] + pbotx * hth,
			ccoords[1] + pboty * hth
		);
		count += 4;
		
		for (int i = 2; i < N; ++i, 
			ptopx = topx, ptopy = topy, pbotx = botx, pboty = boty
		) {
			// j and k are indices into ccoords
			final int pi = (i - 1) << 1;
			final int ci = i << 1;
			
			dx = ccoords[ci] - ccoords[pi];
			dy = ccoords[ci + 1] - ccoords[pi + 1];
//			n = invSqrt(dx * dx + dy * dy);
			n = dx * dx + dy * dy;
			{
				// inverse square root
				float nhalf = 0.5f*n;
			    int ni = Float.floatToIntBits(n);
			    ni = 0x5f3759df - (ni>>1);
			    n = Float.intBitsToFloat(ni);
			    n = n*(1.5f - nhalf*n*n);
			}
			dx *= n;
			dy *= n;
			
			topx = dy;
			topy = -dx;
			botx = -dy;
			boty = dx;
			

			hth = hths[i - 1];
			final float hhth = hth / 2.f;
			
			gl.glVertex2f(
				ccoords[pi] + (ptopx + topx) * hhth,
				ccoords[pi + 1] + (ptopy + topy) * hhth);
			gl.glVertex2f(ccoords[pi] + (pbotx + botx) * hhth,
				ccoords[pi + 1] + (pboty + boty) * hhth
			);
			count += 4;
		}
		

		hth = hths[N - 1];
		
		gl.glVertex2f(
			ccoords[ccoords.length - 2] + ptopx * hth,
			ccoords[ccoords.length - 1] + ptopy * hth);
		gl.glVertex2f(ccoords[ccoords.length - 2] + pbotx * hth,
			ccoords[ccoords.length - 1] + pboty * hth
		);
		count += 4;
		
		return count / 2;
		}
	
	
	public static int toQuadStrip(
			final float[] ccoords, final float[] hths, final int N,
			final float[] out
		) {
//		final int N = ccoords.length / 2;
		
		int count = 0;
		
		
		float ptopx; 	float ptopy;
		float pbotx; 	float pboty;
		float topx; 	float topy;
		float botx; 	float boty;
		
		
		
		float dx = ccoords[2] - ccoords[0];
		float dy = ccoords[3] - ccoords[1];
//		float n = invSqrt(dx * dx + dy * dy);
		float n = dx * dx + dy * dy;
		{
			// inverse square root
			float nhalf = 0.5f*n;
		    int ni = Float.floatToIntBits(n);
		    ni = 0x5f3759df - (ni>>1);
		    n = Float.intBitsToFloat(ni);
		    n = n*(1.5f - nhalf*n*n);
		}
		dx *= n;
		dy *= n;
		
		ptopx = dy; 	ptopy = -dx;
		pbotx = -dy;	pboty = dx;
		
		float hth = hths[0];
		
		
		out[0] = ccoords[0] + ptopx * hth;
		out[1] = ccoords[1] + ptopy * hth;
		out[2] = ccoords[0] + pbotx * hth;
		out[3] = ccoords[1] + pboty * hth;
		count += 4;
		
		for (int i = 2; i < N; ++i, 
			ptopx = topx, ptopy = topy, pbotx = botx, pboty = boty
		) {
			// j and k are indices into ccoords
			final int pi = (i - 1) << 1;
			final int ci = i << 1;
			
			dx = ccoords[ci] - ccoords[pi];
			dy = ccoords[ci + 1] - ccoords[pi + 1];
//			n = invSqrt(dx * dx + dy * dy);
			n = dx * dx + dy * dy;
			{
				// inverse square root
				float nhalf = 0.5f*n;
			    int ni = Float.floatToIntBits(n);
			    ni = 0x5f3759df - (ni>>1);
			    n = Float.intBitsToFloat(ni);
			    n = n*(1.5f - nhalf*n*n);
			}
			dx *= n;
			dy *= n;
			
			topx = dy;
			topy = -dx;
			botx = -dy;
			boty = dx;
			

			hth = hths[i - 1];
			final float hhth = hth / 2.f;
			
			out[count] =
				ccoords[pi] + (ptopx + topx) * hhth;
			out[count + 1] = ccoords[pi + 1] + (ptopy + topy) * hhth;
			out[count + 2] = ccoords[pi] + (pbotx + botx) * hhth;
			out[count + 3] = ccoords[pi + 1] + (pboty + boty) * hhth;
			count += 4;
		}
		

		hth = hths[N - 1];
		
		out[count] =
			ccoords[ccoords.length - 2] + ptopx * hth;
		out[count + 1] = ccoords[ccoords.length - 1] + ptopy * hth;
		out[count + 2] = ccoords[ccoords.length - 2] + pbotx * hth;
		out[count + 3] = ccoords[ccoords.length - 1] + pboty * hth;
		count += 4;
		
		return count / 2;
		}
	
	
	
	public static int toQuadStrip(
			final float[] ccoords, final ThicknessFunction tf,
			final QuadStripCallback callback
		) {
			
			final int N = ccoords.length / 2;
			
			int count = 0;
			
			
			float ptopx; 	float ptopy;
			float pbotx; 	float pboty;
			float topx; 	float topy;
			float botx; 	float boty;
			
			
			
			float dx = ccoords[2] - ccoords[0];
			float dy = ccoords[3] - ccoords[1];
//			float n = invSqrt(dx * dx + dy * dy);
			float n = dx * dx + dy * dy;
			{
				// inverse square root
				float nhalf = 0.5f*n;
			    int ni = Float.floatToIntBits(n);
			    ni = 0x5f3759df - (ni>>1);
			    n = Float.intBitsToFloat(ni);
			    n = n*(1.5f - nhalf*n*n);
			}
			dx *= n;
			dy *= n;
			
			ptopx = dy; 	ptopy = -dx;
			pbotx = -dy;	pboty = dx;
			
			float th = tf.eval(0, ccoords[0], ccoords[1]);
			float hth = th / 2.f;
			
			
			callback.start(
				ccoords[0] + ptopx * hth,
				ccoords[1] + ptopy * hth,
				ccoords[0] + pbotx * hth,
				ccoords[1] + pboty * hth
			);
			count += 4;
			
			for (int i = 2; i < N; ++i, 
				ptopx = topx, ptopy = topy, pbotx = botx, pboty = boty
			) {
				// j and k are indices into ccoords
				final int pi = (i - 1) << 1;
				final int ci = i << 1;
				
				dx = ccoords[ci] - ccoords[pi];
				dy = ccoords[ci + 1] - ccoords[pi + 1];
//				n = invSqrt(dx * dx + dy * dy);
				n = dx * dx + dy * dy;
				{
					// inverse square root
					float nhalf = 0.5f*n;
				    int ni = Float.floatToIntBits(n);
				    ni = 0x5f3759df - (ni>>1);
				    n = Float.intBitsToFloat(ni);
				    n = n*(1.5f - nhalf*n*n);
				}
				dx *= n;
				dy *= n;
				
				topx = dy;
				topy = -dx;
				botx = -dy;
				boty = dx;
				

				th = tf.eval(i - 1, ccoords[ci], ccoords[ci + 1]);
				hth = th / 2.f;
				final float hhth = hth / 2.f;
				
				callback.side(
					ccoords[pi] + (ptopx + topx) * hhth,
					ccoords[pi + 1] + (ptopy + topy) * hhth,
					ccoords[pi] + (pbotx + botx) * hhth,
					ccoords[pi + 1] + (pboty + boty) * hhth
				);
				count += 4;
			}
			

			th = tf.eval(N - 1, 
			ccoords[ccoords.length - 2], ccoords[ccoords.length - 1]);
			hth = th / 2.f;
			
			callback.end(
				ccoords[ccoords.length - 2] + ptopx * hth,
				ccoords[ccoords.length - 1] + ptopy * hth,
				ccoords[ccoords.length - 2] + pbotx * hth,
				ccoords[ccoords.length - 1] + pboty * hth
			);
			count += 4;
			
			return count / 2;
		}
	
	
	
	public static interface QuadStripCallback {
		public void start(float x0, float y0, float x1, float y1);
		public void side(float x0, float y0, float x1, float y1);
		public void end(float x0, float y0, float x1, float y1);
	}
	
	public static interface ThicknessFunction {
		public float eval(final int i, final float x, final float y);
	}
	
	
	
	private ShapeUtilities() {
	}
	
	
	
	
	public static void main(final String[] in) {
		final float[] ccoords = new float[]{
				0, 0, 
				10, 0,
				20, 20, 
				30, -10, 
				40, 0
		};
		final QuadStripCallback callback = new QuadStripCallback() {
			@Override
			public void end(float x0, float y0, float x1, float y1) {
				System.out.println(String.format("[end] (%f, %f) (%f, %f)",
						x0, y0, x1, y1));
			}

			@Override
			public void side(float x0, float y0, float x1, float y1) {
				System.out.println(String.format("[side] (%f, %f) (%f, %f)",
						x0, y0, x1, y1));
			}

			@Override
			public void start(float x0, float y0, float x1, float y1) {
				System.out.println(String.format("[start] (%f, %f) (%f, %f)",
						x0, y0, x1, y1));
			}
		};
		
		toQuadStrip(ccoords, 4.f, callback);
	}
}
