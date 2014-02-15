package org.resisttheb.ere.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.resisttheb.ere.signature.Signature;

// persistence format 0
// reads and writes a "Data" object from/to a binary stream
// 
// it is possible to pack many formats into the same stream
//    and then read them back sequentially
// use a NULL signature to indicate an empty format
//
// TODO: need to also store contour information
public class Format0 {
	/**************************
	 * FORMAT0 STRUCTURES
	 **************************/
	
	public static final class Data implements Serializable {
		public final int width;
		public final int height;
		public final Signature mainSig;
		public final BlobData[] blobs;
		public final int[][][] contours;
		
		public Data(final int _width, final int _height,
				final Signature _mainSig, final BlobData[] _blobs, final int[][][] _contours) {
			this.width = _width;
			this.height = _height;
			this.mainSig = _mainSig;
			this.blobs = _blobs;
			this.contours = _contours;
		}
		
		private Data() {
			width = -1;
			height = -1;
			mainSig = null;
			blobs = null;
			contours = null;
		}
	}
	
	public static final class BlobData implements Serializable {
		public final Signature sig;
		public final int repr;
		public final int[][] points;
		
		public BlobData(final Signature _sig, final int _repr, final int[][] _points) {
			this.sig = _sig;
			this.repr = _repr;
			this.points = _points;
		}
		
		private BlobData() {
			sig = null;
			repr = 0;
			points = null;
		}
	}
	
	/**************************
	 * END FORMAT0 STRUCTURES
	 **************************/
	
	
	public static final byte NULL_BYTE = 0x00;
	
	
	public Format0() {
	}
	
	
	public Data read(final DataInputStream is) throws IOException {
		final byte header = is.readByte();
		if (NULL_BYTE == header) {
			return null;
		}
		
		final int width = is.readInt();
		final int height = is.readInt();
		
		final Signature mainSig = readSig(is);
		
		final int blobsLength = is.readInt();
		final BlobData[] blobs = new BlobData[blobsLength];
		for (int i = 0; i < blobsLength; ++i) {
			blobs[i] = readBlob(is);
		}
		
		final int contoursLength = is.readInt();
		final int[][][] contours = new int[contoursLength][][];
		for (int i = 0; i < contoursLength; ++i) {
			contours[i] = readMIPoints(is);
		}
		
		return new Data(width, height, mainSig, blobs, contours);
	}
	
	public void write(final Data data, final DataOutputStream os) throws IOException {
		if (null == data) {
			os.writeByte(NULL_BYTE);
			return;
		}
		
		os.writeByte(0x01);
		
		os.writeInt(data.width);
		os.writeInt(data.height);
		
		write(os, data.mainSig);
		
		final int blobsLength = data.blobs.length;
		os.writeInt(blobsLength);
		for (int i = 0; i < blobsLength; ++i) {
			write(os, data.blobs[i]);
		}
		
		final int contoursLength = data.contours.length;
		os.writeInt(contoursLength);
		for (int i = 0; i < contoursLength; ++i) {
			write(os, data.contours[i]);
		}
		
		os.flush();
	}
	
	
	/**************************
	 * FORMATTERS AND UNFORMATTERS
	 **************************/
	
	private Signature readSig(final DataInputStream is) throws IOException {
		// [length]([rgb][weight])+
		
		final int length = is.readInt();
		final int[] colors = new int[length];
		final float[] weights = new float[length];
		
		for (int i = 0; i < length; ++i) {
			colors[i] = is.readInt();
			weights[i] = is.readFloat();
		}
		
		return new Signature(length, colors, weights);
	}
	
	private BlobData readBlob(final DataInputStream is) throws IOException {
		// [sig][number of points][points]
		
		final Signature mainSig = readSig(is);
		final int repr = is.readInt();
		final int[][] points = readMIPoints(is);
		return new BlobData(mainSig, repr, points);
	}
	
	
	private int[] readIPoints(final DataInputStream is) throws IOException {
		final int length = is.readInt();
		final int[] points = new int[length << 1];
		for (int i = 0; i < length; ++i) {
			final int j = i << 1;
			points[j] = is.readInt();
			points[j + 1] = is.readInt();
		}
		return points;
	}
	
	private int[][] readMIPoints(final DataInputStream is) throws IOException {
		final int length = is.readInt();
		final int[][] points = new int[length][];
		for (int i = 0; i < length; ++i) {
			points[i] = readIPoints(is);
		}
		return points;
	}
	
	
	private float[] readPoints(final DataInputStream is) throws IOException {
		final int length = is.readInt();
		final float[] points = new float[length << 1];
		for (int i = 0; i < length; ++i) {
			final int j = i << 1;
			points[j] = Float.intBitsToFloat(is.readInt());
			points[j + 1] = Float.intBitsToFloat(is.readInt());
		}
		return points;
	}
	
	private float[][] readMPoints(final DataInputStream is) throws IOException {
		final int length = is.readInt();
		final float[][] points = new float[length][];
		for (int i = 0; i < length; ++i) {
			points[i] = readPoints(is);
		}
		return points;
	}
	
	
	private void write(final DataOutputStream os, final Signature sig) throws IOException {
		// [length][...]
	
		final int length = sig.count;
		os.writeInt(length);
		for (int i = 0; i < length; ++i) {
			os.writeInt(sig.colors[i]);
			os.writeInt(Float.floatToIntBits(sig.weights[i]));
		}
	}
	
	private void write(final DataOutputStream os, final BlobData blob) throws IOException {
		// [sig][#points][points]
		
		write(os, blob.sig);
		os.writeInt(blob.repr);
		write(os, blob.points);
		
	}
	
	
	private void write(final DataOutputStream os, final int[] points) throws IOException {
		final int length = points.length / 2;
		os.writeInt(length);
		for (int i = 0; i < length; ++i) {
			final int j = i << 1;
			os.writeInt(points[j]);
			os.writeInt(points[j + 1]);
		}
	}
	
	private void write(final DataOutputStream os, final int[][] points) throws IOException {
		os.writeInt(points.length);
		for (int i = 0; i < points.length; ++i) {
			write(os, points[i]);
		}
	}
	
	
	private void write(final DataOutputStream os, final float[] points) throws IOException {
		final int length = points.length / 2;
		os.writeInt(length);
		for (int i = 0; i < length; ++i) {
			final int j = i << 1;
			os.writeInt(Float.floatToIntBits(points[j]));
			os.writeInt(Float.floatToIntBits(points[j + 1]));
		}
	}
	
	private void write(final DataOutputStream os, final float[][] points) throws IOException {
		os.writeInt(points.length);
		for (int i = 0; i < points.length; ++i) {
			write(os, points[i]);
		}
	}
	
	/**************************
	 * END FORMATTERS AND UNFORMATTERS
	 **************************/
}
