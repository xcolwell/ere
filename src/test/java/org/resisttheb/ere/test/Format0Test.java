package org.resisttheb.ere.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.resisttheb.ere.persistence.Format0;
import org.resisttheb.ere.signature.Signature;

public class Format0Test {
	private Format0Test() {
	}
	
	
	
	public static void main(final String[] in) {
		
		final Format0 f0 = new Format0();
		
		final Format0.BlobData[] blobs = new Format0.BlobData[128];
		for (int i = 0; i < blobs.length; ++i) {
			final float[] points = new float[10 << 4];
			for (int j = 0; j < points.length; ++j) {
				points[j] = (float) Math.random();
			}
			blobs[i] = new Format0.BlobData(
					new Signature(2, new int[]{0, 0}, new float[]{0, 0}),
					points);
		}
		final Format0.Data data = new Format0.Data(
				new Signature(4, new int[]{0, 0, 0, 0}, new float[]{0, 0, 0, 0}),
				blobs,
				new float[0][]);
		
		final Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * 1024);
		final DataOutputStream os = new DataOutputStream(
				new DeflaterOutputStream(baos, def));
		
		try {
			f0.write(data, os);
			os.flush();
			os.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		def.end();
		
		final byte[] bdata = baos.toByteArray();
		System.out.println(bdata.length);
		
		try {
			final Format0.Data rdata = f0.read(new DataInputStream(
					new InflaterInputStream(new ByteArrayInputStream(bdata))));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
