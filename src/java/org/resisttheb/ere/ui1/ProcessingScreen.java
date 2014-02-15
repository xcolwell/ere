package org.resisttheb.ere.ui1;

import gnu.trove.TIntHashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.resisttheb.ere.CannyEdgeDetector;
import org.resisttheb.ere.blobs.AdjacencyTest;
import org.resisttheb.ere.blobs.AgglomerateSelector;
import org.resisttheb.ere.blobs.BlobGraph;
import org.resisttheb.ere.blobs.BlobNode;
import org.resisttheb.ere.blobs.BlobPixels;
import org.resisttheb.ere.blobs.BucketQuantizer;
import org.resisttheb.ere.blobs.BufferPixels;
import org.resisttheb.ere.blobs.ClusterQuantizer;
import org.resisttheb.ere.blobs.MaskedPixels;
import org.resisttheb.ere.blobs.PseudoIndexedBlob;
import org.resisttheb.ere.blobs.SingleIgnoreTest;
import org.resisttheb.ere.blobs.xMarchingSquares;
import org.resisttheb.ere.blobs.xMidpointTracer;
import org.resisttheb.ere.blobs.BlobGraph.OrphanPolicy;
import org.resisttheb.ere.persistence.Format0;
import org.resisttheb.ere.persistence.Format0.BlobData;
import org.resisttheb.ere.persistence.Format0.Data;
import org.resisttheb.ere.signature.AreaSignatureFactory_SickMomma;
import org.resisttheb.ere.signature.Signature;
import org.resisttheb.ere.signature.SignatureFactory;
import org.resisttheb.ere.ui.AbstractScreen;
import org.resisttheb.ere.ui.Ere;
import org.resisttheb.ere.ui.FontUtilities;
import org.resisttheb.ere.ui.HttpUtilities;
import org.resisttheb.ere.ui.JsonCall;
import org.resisttheb.ere.ui.PathCodecs;
import org.resisttheb.ere.ui.Rgb24ImageCorrector;
import org.resisttheb.ere.ui.UiUtilities;
import org.resisttheb.ere.ui0.OutPacket0;


// contains a big text pane where status is put
// runs up to min(N, number  of cores)  jobs in parallel
// N = 2 is good for applet memory (64mb)
// 
// 
// each processor does:
// 0. read source image
// 1. compute main blob graph   (if we can't, we stop here and set the default)
// 2. compute signature for main blob graph
// 3. compute signature for each blob in the main blob graph
// 4. trace the blobs
// 5. ~~~ cleanup; we just want the traces, repr colors, and signatures ~~~
// 6. compute edge contours
// 7. ~~~ upload everything to server ~~~
// 8. when uploaded, we're done, start the next one
//

//
// start off by println "1234" in repective colors
//
// the end of the screen should load back everything, plus all the others,
// and run the linker
// the output should be a linked system, totally ready to be vised

public class ProcessingScreen extends AbstractScreen {
//	private static final int MAX_WIDTH = 500;
//	private static final int MAX_HEIGHT = 500;
	public static final int MAX_WIDTH = 256;
	public static final int MAX_HEIGHT = 256;
	

	
	
	
	/**************************
	 * BLOB POSTING FUNCTIONS
	 **************************/
	
	private static final class Blob {
		public final int wid;
		public final String securityKey;
		public final byte[] data;
		public final int length;
		
		public Blob(final int _wid, final String _securityKey, 
				final byte[] _data, final int _length) {
			this.wid = _wid;
			this.securityKey = _securityKey;
			this.data = _data;
			this.length = _length;
		}
	}
	
	// Based on "FilePart" ...
	private static final class BlobPart extends PartBase {
	    protected static final String FILE_NAME = "; filename=";
	    private static final byte FILE_NAME_BYTES[] = EncodingUtil.getAsciiBytes("; filename=");
	    
	    
	    private final byte[] data;
	    
	    
	    public BlobPart(final String name, final byte[] _data) {
	    	super(name, null, null, null);

	    	this.data = _data;
	    }
	    
		
	    /**************************
	     * PARTBASE OVERRIDES
	     **************************/
	    
	    @Override
		protected void sendDispositionHeader(final OutputStream out)
	        throws IOException
	    {
	        super.sendDispositionHeader(out);
            out.write(FILE_NAME_BYTES);
            out.write(QUOTE_BYTES);
            out.write(EncodingUtil.getAsciiBytes(getName()));
            out.write(QUOTE_BYTES);
	    }
	
		@Override
	    protected void sendData(final OutputStream out)
	        throws IOException
	    {
			final InputStream is = new ByteArrayInputStream(data);
			try {
		    	final byte[] buffer = new byte[2048];
		    	for (int r; 0 < (r = is.read(buffer)); ) {
		    		out.write(buffer, 0, r);
		    	}
			}
			finally {
				is.close();
			}
	    }
	
	    @Override
	    protected long lengthOfData()
	        throws IOException
	    {
	        return data.length;
	    }
	    
	    /**************************
	     * END PARTBASE OVERRIDES
	     **************************/
	}
	
	
	
	private static String createImageName(final Blob blob) {
	    return String.format("%d:%s", blob.wid, blob.securityKey);
	}
	
	private static String createSnapshotName(final Blob blob) {
	    return String.format("%d:%s:snap", blob.wid, blob.securityKey);
	}
	
	private static String createSnapshotThumbName(final Blob blob) {
	    return String.format("%d:%s:snap_thumb", blob.wid, blob.securityKey);
	}
	
	
	
	private static boolean postBlobs(final Blob ... blobs) throws IOException {
		boolean success = false;
		
		final PostMethod filePost = new PostMethod(
				"http://resisttheb.org/ere/core/post_blobs.php"
		);
        
        filePost.getParams().setBooleanParameter(
                HttpMethodParams.USE_EXPECT_CONTINUE,
                true);
        
        try {
            final Part[] parts = new Part[blobs.length];
            for (int i = 0; i < blobs.length; ++i) {
            	final Blob blob = blobs[i];
            	parts[i] = new BlobPart(createImageName(blob), blob.data);
            }
            
            filePost.setRequestEntity(
                    new MultipartRequestEntity(parts, 
                    filePost.getParams())
                    );
            
            HttpClient client = HttpUtilities.getClient();
            
            final int status = client.executeMethod(filePost);
            
            
            success = (status == HttpStatus.SC_OK);
            
            if (! success) {
            	System.err.println(filePost.getResponseBodyAsString());
            }
        }
        finally {
            filePost.releaseConnection();
        }
        
        return success;
	}
	
	private static boolean postSnapshots(final Blob ... blobs) throws IOException {
		boolean success = false;
		
		final PostMethod filePost = new PostMethod(
				"http://resisttheb.org/ere/core/post.php"
		);
        
        filePost.getParams().setBooleanParameter(
                HttpMethodParams.USE_EXPECT_CONTINUE,
                true);
        

        
        final Format0 format = new Format0();
        
        try {
            final Part[] parts = new Part[2 * blobs.length];
            for (int i = 0, j = 0; i < blobs.length; ++i, j += 2) {
            	final Blob blob = blobs[i];
            	
            	final Data data;
            	final InflaterInputStream iis = new InflaterInputStream(
            			new ByteArrayInputStream(blob.data));
            	try {
            		data = format.read(new DataInputStream(iis));
            	}
            	finally {
            		iis.close();
            	}
            	
            	parts[j] = new BlobPart(createSnapshotName(blob),
            			renderSnapshot(data));
            	parts[j + 1] = new BlobPart(createSnapshotThumbName(blob),
            			renderSnapshotThumb(data));
            }
            
            filePost.setRequestEntity(
                    new MultipartRequestEntity(parts, 
                    filePost.getParams())
                    );
            
            HttpClient client = HttpUtilities.getClient();
            
            final int status = client.executeMethod(filePost);
            
            
            success = (status == HttpStatus.SC_OK);
            
            if (! success) {
            	System.err.println(filePost.getResponseBodyAsString());
            }
        }
        finally {
            filePost.releaseConnection();
        }
        
        return success;
	}
	
	
	private static byte[] toPngBytes(final BufferedImage buffer) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.getWidth() * buffer.getHeight() / 10);
		ImageIO.write(buffer, "png", baos);
		return baos.toByteArray();
	}
	
	private static byte[] renderSnapshot(final Data data) throws IOException {
		final BufferedImage rbuffer = new BufferedImage(data.width, data.height,
				BufferedImage.TYPE_INT_ARGB);
		render(rbuffer, data.blobs, 1.f);
		return toPngBytes(rbuffer);
	}
	
	private static byte[] renderSnapshotThumb(final Data data) throws IOException {
		final float scale = 0.4f;
		final BufferedImage rbuffer = new BufferedImage(
				Math.round(scale * data.width), 
				Math.round(scale * data.height),
				BufferedImage.TYPE_INT_ARGB);
		render(rbuffer, data.blobs, scale);
		return toPngBytes(rbuffer);
	}
	
	private static void render(final BufferedImage rbuffer, final BlobData[] blobDatas, final float scale) {
		final Graphics2D rg2d = (Graphics2D) rbuffer.getGraphics();
		
		// HINTS:
		rg2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		rg2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		
		rg2d.scale(scale, scale);
		
		final int N = blobDatas.length;
		for (int i = 0; i < N; ++i) {
			final BlobData blobData = blobDatas[i];
			final int rgb = blobData.repr;
			final int[][] trace = blobData.points;
			
			final GeneralPath path = new GeneralPath();
			PathCodecs.unpack2(trace, path);
			
			rg2d.setPaint(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
			rg2d.fill(path);
		}
		
		rg2d.dispose();
		
	}

	
	
	
	/**************************
	 * END BLOB POSTING FUNCTIONS
	 **************************/
	
	
	private static final Object FINISH_IT = new Object();
	
	
	
	
	private OutPacket0[] packets;
	
	
	private JTextPane text;
	
	private static final int MAX_PROCESSORS = 1;
	// number of processors depends on #of cores and internal limit for memory
	private Processor[] processors;
	
	
	
	private ThreadFactory tf;
	private ThreadPoolExecutor ex;
	private BlockingQueue<Runnable> exQueue;
	
	private BlockingQueue<Object> pQueue;
	
	private CountDownLatch doneLatch;
	
	
	private Format0 format;
	
	
	private List<Blob> blobsToPost = Collections.synchronizedList(new ArrayList<Blob>());
	
	
	
	public ProcessingScreen() {
		
		format = new Format0();
		
		initProcessors();
		initUi();
	}
	
	
	private void initProcessors() {
		final int physicalProcessors = Runtime.getRuntime().availableProcessors();
		final int n = Math.min(MAX_PROCESSORS, physicalProcessors);
		
		processors = new Processor[n];
		for (int i = 0; i < n; ++i) {
			processors[i] = new Processor(i);
		}
		
		tf = new ProcessorThreadFactory();
		exQueue = new LinkedBlockingQueue<Runnable>();
		ex = new ThreadPoolExecutor(n, n, 1000, TimeUnit.MILLISECONDS,
				exQueue, tf);
//		ex.setMaximumPoolSize(n);
		
		pQueue = new LinkedBlockingQueue<Object>();
	}
	
	private void initUi() {
		setBackground(Color.BLACK);
		
		text = new JTextPane();
		text.setOpaque(false);
		text.setEditable(false);
		text.setFocusable(false);
		text.setFont(FontUtilities.ERAS_LIGHT.deriveFont(UiUtilities.DEFAULT_FONT_SIZE));
		
		setLayout(new BorderLayout());
		add(text, BorderLayout.CENTER);
	}
	
	
	
	public void setPackets(final OutPacket0[] _packets) {
		this.packets = _packets;
	}
	
	
	
	
	
	// safe to call from any thread.
	private void _print(Color color, final String message) {
		final float size = UiUtilities.noiseSize();
		color = UiUtilities.lerp(
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 32), 
				color, 
				Math.min(1.f, size / (3f * UiUtilities.DEFAULT_FONT_SIZE)));
		_print(color, size, message);
	}
	
	private void _print(final Color color, final float fontSize, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final SimpleAttributeSet as = new SimpleAttributeSet();
				as.addAttribute(StyleConstants.Foreground, color);
				as.addAttribute(StyleConstants.FontSize, (int) fontSize);
				try {
					text.getDocument().insertString(0, message + "    ", as);
				}
				catch (BadLocationException e) {
					System.out.println("[!] We missed a message: " + message);
				}
			}
		});
	}
	
	
	
	private void printKickstartMessage() {
		_print(Color.WHITE, 3 * UiUtilities.DEFAULT_FONT_SIZE,
				"\nwe need to compute some values now");
		_print(Color.GRAY, 
				String.format("using %d processor%s", 
						processors.length, (1 == processors.length ? "" : "s")));
		
		for (OutPacket0 packet : packets) {
			_print(packet.color, packet.text);
		}
		
		_print(Color.LIGHT_GRAY, "let's go!");
	}
	
	/**************************
	 * SCREEN IMPLEMENTATION
	 **************************/
	
	@Override
	public void init(final Object ... _inPackets) {
		setPackets((OutPacket0[]) _inPackets);
	}
	
	
	@Override
	public void start() {
		printKickstartMessage();
		
		// start the processors
		
		for (OutPacket0 packet : packets) {
			pQueue.add(packet);
		}
		pQueue.add(FINISH_IT);
		
		doneLatch = new CountDownLatch(processors.length - 1);
		for (Processor processor : processors) {
			ex.submit(processor);
		}
	}
	
	@Override
	public void stop() {
		// NOTE: why is this a security problem?
//		ex.shutdownNow();
		pQueue.clear();
	}
	
	@Override
	public void dispose() {
		// TODO: ??
	}
	
	/**************************
	 * END SCREEN IMPLEMENTATION
	 **************************/
	
	
	
	
	/**************************
	 * FINISHING STEPS
	 **************************/
	
	private int[] activeWids;
	
	private void preFinish() {
		// 1. post blobs
		// 2. compute set of blobs to display
		// 3. link
		
		boolean success = false;
		try {
			success = _preFinish();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		
		if (! success) {
			_print(Color.RED, "We couldn't finish.");
			sh.requestReset();
		}

		
		final OutputPacket1 outp = new OutputPacket1(activeWids);
		sh.finish(new OutputPacket1[]{outp});
	}
	
	private boolean _preFinish() {
		try {
			
			
			if (! postBlobs()) {
				_print(Color.GRAY, "could not post blobs");
				return false;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	
	private boolean postBlobs() throws IOException {
		_print(Color.GRAY, "sharing work ... ");
		
		final Blob[] blobs = blobsToPost.toArray(new Blob[blobsToPost.size()]);
		postBlobs(blobs);
		postSnapshots(blobs);
		
		final int n = blobsToPost.size();
		activeWids = new int[n];
		for (int i = 0; i < n; ++i) {
			activeWids[i] = blobsToPost.get(i).wid;
		}
		
		blobsToPost.clear();

		
		_print(Color.GRAY, "activating");
		
		final TIntHashSet activeWidsSet = new TIntHashSet(activeWids.length);
		activeWidsSet.addAll(activeWids);
		
		final List<List<Object>> activateList = new ArrayList<List<Object>>(packets.length);
		for (OutPacket0 packet : packets) {
			if (! activeWidsSet.contains(packet.prompt.wid)) {
				continue;
			}
			
			final List<Object> al = new ArrayList<Object>(2);
			al.add(packet.prompt.wid);
			al.add(packet.prompt.securityKey);
			activateList.add(al);
		}
		final Map<?, ?> map = (Map<?, ?>) JsonCall.inlinePost("http://resisttheb.org/ere/core/activate.php",
				"list", activateList);
		final boolean success = 0 != ((Number) map.get("success")).intValue();
		
		if (! success) {
			_print(Color.RED, "failed to activate ... but don't worry, a cleanup script on the server will activate end of day");
		}
		
		_print(Color.LIGHT_GRAY, "done");
		return true;
	}
	
	/**************************
	 * END FINISHING STEPS
	 **************************/
	
	
	
	
	
	
	
	
	
	
	
	private class Processor implements Runnable {
		private float fontSize;
		private OutPacket0 packet;
		
		
		/**************************
		 * PROCESSING STATE
		 **************************/
		
		private BufferedImage buffer;
		
		private BlobGraph bg;
		private BufferPixels srcBfp;
//		private BufferPixels bfp;
		private PseudoIndexedBlob pib;
		private BlobPixels pfbs;
		private BlobNode[] blobs;
		private int blobCount;
		
		private Signature mainSig;
		private int[] reprs;
		private Signature[] blobSigs;
		private int[] dim;
		
		private int[][][] blobTraces;
		private int[][][] contours;
		
		/**************************
		 * END PROCESSING STATE
		 **************************/
		
		
		private final int pid;
		
		public Processor(final int _pid) {
			this.pid = _pid;
		}
		
		
		public void setPacket(final OutPacket0 _packet) {
			this.packet = _packet;
			nextFontSize();
		}
		
		private void nextFontSize() {
			// Set a random font size, ya:
			float u = (float) Math.random();
			fontSize = UiUtilities.DEFAULT_FONT_SIZE + (12.f) * u;
		}
		
		
		private void print(final String message) {
			_print(packet.color, message);
		}
		
		
		
		
		private void clearBuffer() {
			buffer = null;			
		}
		
		private void clearBlobs() {
			bg = null;
			srcBfp = null;
			pib = null;
			pfbs = null;
			blobs = null;
		}
		
		private void clear() {
			clearBuffer();
			clearBlobs();
			
			reprs = null;
			mainSig = null;
			blobSigs = null;
			
			blobTraces = null;
			contours = null;
			
		}
		
		//
		
		private boolean readBuffer() {
			print("reading source image");
			try {
				buffer = UiUtilities.ensureSafeType(ImageIO.read(new URL(
						"http://resisttheb.org/ere/core/read_pixels.php?id=" + packet.prompt.wid
						)));
			}
			catch (IOException e) {
				return false;
			}
			

			print("resizing ... got it");
			
			// Fit to the expected dimensions:
			final Rgb24ImageCorrector corrector = new Rgb24ImageCorrector(MAX_WIDTH, MAX_HEIGHT);
			buffer = corrector.correct(buffer);
			
			dim = new int[]{buffer.getWidth(), buffer.getHeight()};
			
			print("done");
			
			return true;
		}
		
		
		private boolean extractBlobs() {

			print("going to do the hardest part first ... sit tight");
			
			final int M = 8;
			final int K = 8;
			
			
			
			srcBfp = new BufferPixels(buffer);
			final BufferPixels bfp = 
				new BufferPixels(new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB));
			
			
			
			
			
			
			
			
			final BucketQuantizer bq = new BucketQuantizer(M);
			final ClusterQuantizer cq = new ClusterQuantizer(K);
			
			bq.process(srcBfp, bfp);
			cq.process(bfp, bfp);
			
			
			final int area = (bfp.x1() - bfp.x0()) * (bfp.y1() - bfp.y0());
			final float f = 0.0025f;
			
			bg = new BlobGraph();
			// TODO: configure
			bg.setMinArea((int)Math.round(area * f));
			//bg.setMinArea(0);
			
			bg.setMergeAsAgglomerate(true);
			
			bg.setPixels(bfp);
			long time0 = System.nanoTime();
			if (! bg.rebuild()) {
				
				print("this babes synthetic ...");
			
				return false;
			}
			long time1 = System.nanoTime();
			
			print("great news, nothing's synthetic ... we can continue");
			
			bg.compactBlobs();
			
			
//			buffer = null;
//			System.gc();
			
			
			
			blobs = bg.getBlobsModifiable();
			blobCount = blobs.length;
			blobTraces = new int[blobs.length][][];
			
			reprs = new int[blobs.length];
			for (int i = 0; i < blobs.length; ++i) {
				reprs[i] = blobs[i].getRepresentative();
			}
			
			print(blobs.length + " splotches");
			
			
			return true;
		}
		
		
		private boolean computeGraphSignature() {
			print("computing main signature");
			
			// TODO:
			// TODO:
//			System.out.println("TODO: compute graph signature");
			
//			mainSig = new Signature(0, new int[0], new float[0]);
			
			final SignatureFactory sigf = new AreaSignatureFactory_SickMomma(4);
			mainSig = sigf.generate(bg);
			
			return true;
		}
		
		
		private boolean computeBlobSignatures() {
			print("... computing little signatures");
			
			final BlobPixels bp = new BlobPixels();
			bp.setData(pib);
			
			final BufferPixels bfp = 
				new BufferPixels(new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB));
			
			
			final MaskedPixels msrcBfp = new MaskedPixels(bfp, srcBfp);
			
			
			final int M = 8;
			final int K = 4;
			
			
			
			final BucketQuantizer bq = new BucketQuantizer(M);
			final ClusterQuantizer cq = new ClusterQuantizer(K);
			cq.setMaxIterations(4);
			
			bq.process(msrcBfp, bfp);
			cq.process(bfp, bfp);
			
			final float f = 0.0025f;
			
			
			final BlobGraph bbg = new BlobGraph();
			bbg.setMergeAsAgglomerate(true);				
			bbg.setPixels(bp);
							

			final SignatureFactory sigf = new AreaSignatureFactory_SickMomma(2);
							
			blobSigs = new Signature[blobs.length];
			for (int k = blobs.length - 1; 0 <= k; --k) {
				final BlobNode blob = blobs[k];
				
				pib.reset(blob);

				// Run the whole blobbing sequence on the sub-image:				
				
				
				final int area = (bp.x1() - bp.x0()) * (bp.y1() - bp.y0());
				
				
				
				// TODO: configure
				bbg.setMinArea((int)Math.round(area * f));
				//bg.setMinArea(0);
				
				
//				long time0 = System.nanoTime();
				if (! bbg.rebuild()) {
					// TODO:
				
					return false;
				}
//				long time1 = System.nanoTime();
				
				// TODO: we don't need to do this ... we're going to ditch these blobs soon
				//bg.compactBlobs();
				
//				System.out.println(" :: " + (time1 - time0));
				
				print("..." + k);
				
				// TODO: need to complete
//				System.out.println("TODO: compute sig");
//				blobSigs[k] = new Signature(0, new int[0], new float[0]);
				
				blobSigs[k] = sigf.generate(bbg);
			}
			
			return true;
		}
		
		
		private boolean traceBlobs() {
			print("... should be fast. tracing.");
			
			
			final xMarchingSquares bms = new xMarchingSquares();
			bms.reset(pfbs);
						
			final GeneralPath gpath = new GeneralPath();
			blobTraces = new int[blobs.length][][];
			for (int k = blobs.length - 1; 0 <= k; --k) {
				final BlobNode blob = blobs[k];

				pib.reset(blob);

				gpath.reset();
				bms.identifyPerimeter(gpath);

				blobTraces[k] = PathCodecs.pack2(gpath);
				assert PathCodecs.SELF_CHECK(blobTraces[k], gpath);
			}
			
			// Let's hope we're not wrong ... 
			print("see?");
			
//			print("rendering");
			
//			if (! render()) {
//				return false;
//			}
			
			return true;
		}
		
		
		
		
		
		private boolean computeContours() {
			print("looking for curves");
			
			BufferedImage edgeBuffer = new BufferedImage(buffer.getWidth(), buffer.getHeight(), 
					BufferedImage.TYPE_INT_ARGB);
			
			CannyEdgeDetector ed = new CannyEdgeDetector();
//			ed.setContrastNormalized(false);
//			ed.setGaussianKernelRadius(10.f);
//			ed.setGaussianKernelWidth(5);
//			ed.setHighThreshold(50.f);
//			ed.setLowThreshold(5.f);
			
			ed.setSourceImage(buffer);
			ed.setEdgesImage(edgeBuffer);
			//ed.setEdgesImage(new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB));
			ed.process();
			
			
			
			final xMidpointTracer mt = new xMidpointTracer(AdjacencyTest._8);
			
			
			BlobGraph ebg = new BlobGraph();
			ebg.setMinArea(0);
			ebg.setAdjacencyTest(AdjacencyTest._8);
			ebg.setIgnoreTest(new SingleIgnoreTest(Color.BLACK.getRGB()));
			
			// Throw out all 1px blobs ...
			ebg.setMinArea(2);
			ebg.setAgglomerateSelector(AgglomerateSelector.NONE);
			ebg.setOrphanPolicy(OrphanPolicy.DISCARD);
			ebg.setMergeAsAgglomerate(true);
			
			print("tracing ... found some");
			
			
			ebg.setPixels(new BufferPixels(edgeBuffer));
			if (! ebg.rebuild()) {
				return false;
			}
			ebg.compactBlobs();
			edgeBuffer = null;
			
			
			final BlobNode[] eblobs = ebg.getBlobsModifiable();
			contours = new int[eblobs.length][][];
			for (int i = 0; i < eblobs.length; ++i) {
				mt.setData(eblobs[i].getColumnsModifiable());
				final GeneralPath gpath = new GeneralPath();
				mt.trace(gpath);
				contours[i] = PathCodecs.pack2(gpath);
			}
			
			ebg = null;
//			System.gc();
			
			// TODO: save
			
			return true;
		}
		
		
		private boolean share() {
			print("packaging the work");
			
			final BlobData[] blobDatas = new BlobData[blobCount];
			for (int i = 0; i < blobCount; ++i) {
				blobDatas[i] = new BlobData(blobSigs[i], reprs[i], blobTraces[i]);
			}
			
			final Data data = new Data(dim[0], dim[1], mainSig, blobDatas, contours);
			
			
			// TODO: write this out
			
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * 1024);
			try {
				final Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
				final DataOutputStream os = new DataOutputStream(new DeflaterOutputStream(baos, def));
			format.write(data, os);
			os.close();
//			def.finish();
			def.end();
			}
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			final byte[] bytes = baos.toByteArray();
			
			print((bytes.length / 1000) + "kb");
			
			blobsToPost.add(new Blob(packet.prompt.wid, packet.prompt.securityKey,
					bytes,
					bytes.length));
			
			
			return true;
		}
		
		
		//
		
		
		private boolean execute() {
			
			pib = new PseudoIndexedBlob();
			pfbs = new BlobPixels();
			pfbs.setData(pib);
			
			if (! readBuffer()) {
				return false;
			}
			
			
			if (! extractBlobs()) {
				return false;
			}
			
			System.gc();
			
			if (! computeGraphSignature()) {
				return false;
			}
			
			if (! computeBlobSignatures()) {
				return false;
			}
			
			// no -- edge tracing needs the buffer
//			clearBuffer();
			System.gc();			
			
			if (! traceBlobs()) {
				return false;
			}
			
			clearBlobs();
			System.gc();
			
			if (! computeContours()) {
				return false;
			}
			
			clearBuffer();
			System.gc();
			
			if (! share()) {
				return false;
			}
			
			print("one more down the pipe");
			
			// Final wipe out:
			clear();
			
			return true;
		}
		
		private void cleanupAfterFailure() {
			print("one or more stages failed ... " +
					"we're going to go for the fountain on this one (see FAQ)");
			// Note: since we didn't put an entry
			// in <code>blobsToPost</code>, the system will
			// ignore this wid.
		}
		
		
		/**************************
		 * RUNNABLE IMPLEMENTATION
		 **************************/
		
		public void run() {
			if (Ere.DEBUG) {
			System.out.println("  START PROCESSOR: " + pid);
			}
			
			// Try to find a packet:
			final Object _packet = pQueue.poll();
			if (null == _packet) {
				if (Ere.DEBUG) {
				System.out.println("  END EARLY PROCESSOR: " + pid);
				}
				
				doneLatch.countDown();
				return;
				// DONE!
			}
			
			if (FINISH_IT == _packet) {
				if (Ere.DEBUG) {
				System.out.println("  FINISH PROCESSOR: " + pid);
				}
				
				try {
					doneLatch.await();
					ProcessingScreen.this.preFinish();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					print("could not wait for finished jobs. aborting");
				}
				return;
				// DONE!
			}
			
			
			setPacket((OutPacket0) _packet);
			
			boolean success = false;
			try {
				success = execute();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
			
			clear();
			if (! success) {
				cleanupAfterFailure();
			}
			
			System.gc();
			
			if (Ere.DEBUG) {
			System.out.println("  END AND RESUBMIT PROCESSOR: " + pid);
			}
			
			// Queue up for the next one:
			ex.submit(this);
		}
		
		/**************************
		 * END RUNNABLE IMPLEMENTATION
		 **************************/
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static final class ProcessorThreadFactory implements ThreadFactory {
		public ProcessorThreadFactory() {
		}
		
		/**************************
		 * THREADFACTORY IMPLEMENTATION
		 **************************/
		
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread(r, "ENTER & RE-EXIT PROCESSOR -- DOIN HEAVY SHIT");
			// Don't want to hang the viewer's computer with our crunching,
			// so put the prio somewhere down there:
			//t.setPriority((Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
		
		/**************************
		 * END THREADFACTORY IMPLEMENTATION
		 **************************/
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
/*
	public static void main(final String[] in) {
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
		
		
		final ProcessingScreen ps = new ProcessingScreen();
		
		ps.setPackets(new OutPacket0[]{
			new OutPacket0(0, Color.BLUE, "test", 
					"file:///c:/temp/doodle.png"),
			new OutPacket0(0, Color.GREEN, "commodore",
					"file:///c:/temp/tree.jpg"),
			new OutPacket0(0, Color.RED, "64",
					"file:///c:/temp/tiger.jpg")
		});
	
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(ps, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		
		
		ps.start();
//		ps.processors[0].setInPacket(ps.packets[0]);
//		ps.processors[0].run();
//		ps.processors[0].setInPacket(ps.packets[1]);
//		ps.processors[0].run();
	}
	*/
}
