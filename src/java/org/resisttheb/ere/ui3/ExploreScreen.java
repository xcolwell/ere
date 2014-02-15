package org.resisttheb.ere.ui3;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongHashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.resisttheb.ere.persistence.Format0;
import org.resisttheb.ere.persistence.Format0.BlobData;
import org.resisttheb.ere.persistence.Format0.Data;
import org.resisttheb.ere.signature.SickMommaUtilities;
import org.resisttheb.ere.signature.Signature;
import org.resisttheb.ere.signature.SignatureMetric;
import org.resisttheb.ere.signature.WeightedInverseMetric;
import org.resisttheb.ere.ui.AbstractScreen;
import org.resisttheb.ere.ui.CompressingPathCallback;
import org.resisttheb.ere.ui.Ere;
import org.resisttheb.ere.ui.FontUtilities;
import org.resisttheb.ere.ui.PathCodecs;
import org.resisttheb.ere.ui.PathCodecs.PathCallback;
import org.resisttheb.ere.ui2.OutputPacket2;
import org.resisttheb.ere.ui3.ShapeUtilities.QuadStripCallback;
import org.resisttheb.ere.ui3.ShapeUtilities.ThicknessFunction;
import org.resisttheb.nug.RainbowsEdge;
import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;

import prefuse.util.force.ForceItem;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;


/*
 * rendering notes:
 * 
 * we take tri strips, tri fans, and triangles
 * and merge the indices into a single buffer
 * we then create a large index buffer to be stored as a VBO
 * 
 * we render the entire shappe using a single call to glDrawElements,
 * using the index VBO
 * 
 */

public class ExploreScreen extends AbstractScreen implements GLEventListener {
	
	private static final float _2_PI = (float) (2 * Math.PI);
	
	
	
	private static final float MIN_GRAPH_SCALE = 0.4f;
	private static final float MAX_GRAPH_SCALE = 2.4f;
	
	

	// scale per node
	// jitter per node
	// node outline
	// ability to render each blob with alpha   (have an alpha jitter when mouse is near?)
	// global camera (center and scale)
	// flag to render contours
	// flag to render outlines (shrink from centroid a bit before rendering)
	
	
	private static AffineTransform INV(final AffineTransform at) {
		try {
		return at.createInverse();
		}
		catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return at;
	}
	
	
	private static String TITLE(String title, String name) {
		if (null == title) title = "";
		if (null == name) name = "no name";
		final int MAX_LEN = 50;
		final int MAX_NAME_LEN = 20;
		if (MAX_LEN < title.length())
			title = title.substring(0, MAX_LEN);
		if (MAX_NAME_LEN < name.length())
			name = name.substring(0, MAX_NAME_LEN);
		return title + " (" + name + ")";
	}
	
	
	private static final float RAD_TO_DEG = 360.f / (float) (2 * Math.PI);
	
	/**************************
	 * DEBUGGING IO
	 **************************/
	
	private static void DEBUG_SAVE(final OutputPacket2 packet) {
		// Serialize it to a file ...
		
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(
					new BufferedOutputStream(new FileOutputStream(
						new File("c:/temp/explore_debug.serialized")
					)));
			try {
				oos.writeObject(packet);
				oos.flush();
			}
			finally {
				oos.close();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static OutputPacket2 DEBUG_LOAD() {
		try {
			final File file = new File("c:/temp/explore_debug.serialized");
			if (! file.exists()) {
				return null;
			}
			
			final ObjectInputStream ois = new ObjectInputStream(
					new BufferedInputStream(new FileInputStream(
						file
					)));
			try {
				return (OutputPacket2) ois.readObject();
			}
			finally {
				ois.close();
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**************************
	 * END DEBUGGING IO
	 **************************/
	
	
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static int countBlobLinks(final int[][] allLinks) {
		int count = 0;
		for (int[] links : allLinks) {
			for (int link : links) {
				if (0 <= link) ++count;
			}
		}
		return count;
	}
	
	private static void norm(final float[] v) {
		final float mag = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1]);
		if (0 != mag) {
			v[0] /= mag;
			v[1] /= mag;
		}
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	

	
	
	
	
	
	
	
	
	// investigate ...
	// 1. tesselating into a vbo
	// 2. need "visual blob" that holds pointer to vbo,
	//    or raw geom if needed
	//    should also hold centerpoint of each geom,
	//    and centerpoint of each contour
	//    also vbos for contours
	//
	// 3. visual blob should have a bounding box ...
	//    we're going to use this when we do our own culling
	//
	// 
	

	private ThreadFactory gmtf;
	private BlockingQueue<Runnable> gmqueue;
	private ThreadPoolExecutor gmexec;
	
	{
		gmtf = new ExploreThreadFactory();
		gmqueue = new LinkedBlockingQueue<Runnable>();
		gmexec = new ThreadPoolExecutor(0, 1, 1000, TimeUnit.MILLISECONDS,
				gmqueue, gmtf);
	}
	
	
	
	private VisualNode closestNode(final float gx, final float gy) {
		float closestdsq = Float.MAX_VALUE;
		VisualNode closest = null;
		for (VisualNode node : nodes) {
			final float dx = node.cx - gmousex;
			final float dy = node.cy - gmousey;
			final float dsq = dx * dx + dy * dy;
			
			if (dsq < closestdsq) {
				closest = node;
				closestdsq = dsq;
			}
		}
		return closest;
	}
	
	
	// moves from current to ni
	private void queueGMovement(final int ni) {
//		gfocus(ni);
		
		// TODO: work this whole thing out ...
		
		
		gfocus(ni);
		
		/*
		final Runnable job = new Runnable() {public void run() {
//			final VisualNode node = nodes[ni];
			
			if (gni == ni) {
				SwingUtilities.invokeLater(new Runnable() {public void run() {
					gfocus(ni);
				}});
				return;
			}
		
			try {
			final int[][] allSteps = shortestPaths(gni, ni);
			if (0 < allSteps.length) {
				final int[] steps = allSteps[(int) Math.floor(Math.random() * allSteps.length)];
				final int[] useSteps = new int[1 + steps.length];
				useSteps[0] = gni;
				System.arraycopy(steps, 0, useSteps, 1, steps.length);
				final Path path = createPath(useSteps);
				
				// steps per length (px)
				final float RATE = 1.f / 10.f;
				final int nsteps = (int) Math.ceil(path.length() * RATE);
				if (6 <= nsteps) {
					final float m = 1.f / (nsteps - 1);
					final float[] coords = new float[2];
					for (int i = 0; i < nsteps; ++i) {
						path.eval(i * m, coords);
						
						SwingUtilities.invokeLater(new Runnable() {public void run() {
							gcx = coords[0];
							gcy = coords[1];
						}});
					}
				}
			}
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
			
			SwingUtilities.invokeLater(new Runnable() {public void run() {
				gfocus(ni);
			}});
		
		}};
		
		gmexec.submit(job);
		*/
	}
	
	
	private void gfocus(final int id) {
		final VisualNode node = nodes[id];
		gni = id;
		gcx = node.cx;
		gcy = node.cy;
	}
	
	
	private void target(final int id) {
		if (id < 0) {
			throw new IllegalArgumentException();
		}

//		rscale = 1.f;
		
		rni = id;
		rfocusIndex = 0;
		rfcx = 0.f;
		rfcy = 0.f;
		rftx = 0.f;
		rfty = 0.f;
		
		rpmx = -Float.MAX_VALUE;
		rpmy = -Float.MAX_VALUE;
		rcapmousex = -1;
		rcapmousey = -1;
		
		
//		gni = id;
	}
	
	
	
	private float zinc = 0.f;
	
	private int gni = 0;
//	private VisualNode cnode = null;
	private float gcx = 0.f;
	private float gcy = 0.f;
	private float gscale = 1.f;
	
	
	private TextRenderer titleRenderer;
	private FontMetrics titleMetrics;
	private float[] titleWidths;
	private float[] titleHeights;
	
	private TextRenderer branchRenderer;
	private FontMetrics branchMetrics;
	private float[] branchWidths;
	private float[] branchHeights;
	
	private String[] titles;
	
	
	private OutputPacket2 packet = null;
	
	private int[][] unfilteredBlobLinks;
	
	
	private VisualNode[] nodes;
	private VisualEdge[] edges;
	private VisualEdge[] wordEdges;
	private VisualEdge[] sigEdges;
	
	private boolean active = false;
	
	
	
	private int width;
	private int height;
	
	private GLCanvas canvas;
	private GLU glu;
	
	
	private boolean trail = true;
	private float trailAlpha = 0.3f;
	
	
	// these are for control point jiggle
	private NoiseEvalState edgeNes;
	
	private float vmousex = 0.f;
	private float vmousey = 0.f;
	
	private float gmousex = 0.f;
	private float gmousey = 0.f;
	
	private float rmousex = 0.f;
	private float rmousey = 0.f;
	
	
	
	private ParallelListSorter pls;
	private float[] sortWeights;
	private int[] sortIndices;
	
	
	private boolean vboSupported;
	
	
	
	public ExploreScreen() {
		
	}

	
	
	
	// finds all shortest paths
	// between two ids
	protected int[][] shortestPaths(final int id0, final int id1) {
		class PP {
			final int id;
			final PP parent;
			
			public PP(final int _id, final PP _parent) {
				this.id = _id;
				this.parent = _parent;
			}
			
			public int length() {
				if (null == parent)
					return 1;
				return 1 + parent.length();
			}
		}
		
		
		final TIntHashSet visited = new TIntHashSet(nodes.length / 2);
		
		final TIntHashSet otherIds = new TIntHashSet(64);
		
		Set<PP> fringe = new HashSet<PP>(nodes.length / 4);
		Set<PP> nextFringe = new HashSet<PP>(nodes.length / 4);
		
		fringe.add(new PP(id0, null));
		
		while (! fringe.isEmpty()) {
			for (PP pp : fringe) {
				visited.add(pp.id);
			}
			
			if (visited.contains(id1)) {
				break;
			}
			
			
			for (PP pp : fringe) {
				otherIds.clear();
				otherIds.addAll(nodes[pp.id].blobEdgeMap.keys());
				otherIds.addAll(nodes[pp.id].wordEdgeMap.keys());
				
				for (TIntIterator itr = otherIds.iterator(); itr.hasNext(); ) {
					final int otherId = itr.next();
					if (visited.contains(otherId)) {
						continue;
					}
					
					nextFringe.add(new PP(otherId, pp));
				}
			}
			
			
			// swap fringes:
			fringe.clear();
			final Set<PP> tfringe = fringe;
			fringe = nextFringe;
			nextFringe = tfringe;
		}
		
		

		// Collect all <code>PP</code> on the fringe that touch id1
		final Collection<PP> ends = new ArrayList<PP>(fringe.size());
		for (PP pp : fringe) {
			if (pp.id == id1) {
				ends.add(pp);
			}
		}
		
		final int[][] gps = new int[ends.size()][];
		int j = 0;
		
		for (PP end : ends) {
			final int len = end.length();
			final int[] steps = new int[len];
			
			int i = len - 1;
			PP pp = end;
			while (0 <= i && null != pp) {
				steps[i] = pp.id;
				pp = pp.parent;
			}
			
			gps[j] = steps;
			++j;
		}
		
		return gps;
	}
	
	
	
	// each step is a node
	private Path createPath(final int[] steps) {
		// we should have a hash of [int, int] -> visualedge
		// visualedge should have the beview properties
		
		final Path[] paths = new Path[steps.length - 1];
		for (int i = 1; i < steps.length; ++i) {
			VisualEdge edge = nodes[i - 1].blobEdgeMap.get(nodes[i].id);
			if (null == edge) {
				edge = nodes[i - 1].wordEdgeMap.get(nodes[i].id);
			}
			assert null != edge;
			paths[i - 1] = new CubicPath(edge.bounds);
		}
		return new CompoundPath(paths);
	}
	
	
	
	
	private void graphLayout() {
		final float NODE_MASS = 1.f;
		final float EDGE_MASS = 0.01f;
		final float SPRING_LENGTH = 1250.f;
		

		final float w0 = 130;
		final float h0 = 130;
		final float w1 = 130;
		final float h1 = 130;
		
		
		final ForceLayout fl = new ForceLayout();
		fl.setIterationCount(50);
		
		final ForceLayout.Node[] fnodes = new ForceLayout.Node[nodes.length];
		final ForceLayout.Edge[] fedges = new ForceLayout.Edge[edges.length];
		
		
		
		for (int i = 0; i < fnodes.length; ++i) {
			fnodes[i] = new ForceLayout.Node(new ForceItem(), NODE_MASS);
		}
		
		for (int i = 0; i < fedges.length; ++i) {
			final VisualEdge edge = edges[i];
			
			final ForceItem[] fitems = new ForceItem[2];
			for (int k = 0; k < fitems.length; ++k) {
				fitems[k] = new ForceItem();
			}
			
			float mass;
			float springc;
			float mainSpringc;
			if (null != edge.sig) {
				mass = 0.05f * EDGE_MASS;
				springc = 0.05f * 0.0001f;
				mainSpringc = 0.05f *  0.0001f;
			}
			else {
				mass = EDGE_MASS;
				springc = 0.0001f;
				mainSpringc = 1.4f * 0.0001f;
			}
			ForceLayout.Edge fedge = new ForceLayout.Edge(
				fnodes[edge.id0], fnodes[edge.id1], 
				fitems,
				mass
			);
			fedge.mainSlen = SPRING_LENGTH;
			
			fedge.mainSpringCoeff = mainSpringc;
			fedge.springCoeff = springc;
			fedges[i] = fedge;
		}
		
		
		fl.setup(new Point2D.Float(0.f, 0.f), 
				fnodes, fedges);
		fl.run();
		
		
		// Sync positions:
		for (int i = 0; i < fnodes.length; ++i) {
			final VisualNode node = nodes[i];
			final ForceLayout.Node fnode = fnodes[i];
			
			node.cx = fnode.fitem.location[0];
			node.cy = fnode.fitem.location[1];
		}
		for (int i = 0; i < fedges.length; ++i) {
			final VisualEdge edge = edges[i];
			final ForceLayout.Edge fedge = fedges[i];
			
			float[] bounds = new float[]{
				fedge.n0.fitem.location[0],
				fedge.n0.fitem.location[1],
				
				fedge.fitems[0].location[0],
				fedge.fitems[0].location[1],
				
				fedge.fitems[1].location[0],
				fedge.fitems[1].location[1],
				
				fedge.n1.fitem.location[0],
				fedge.n1.fitem.location[1]
			};
			
			
			final float uUp = CurveUtilities.search(true, bounds, 
					new Rectangle2D.Float(bounds[0] - w0 / 2.f, bounds[1] - h0 / 2.f, 
							w0, h0));
			final float uDown =
				CurveUtilities.search(false, bounds, 
						new Rectangle2D.Float(bounds[6] - w1 / 2.f, bounds[7] - h1 / 2.f, 
								w1, h1));
			
			final float[][] sbounds = CurveUtilities.split(bounds, 3, uUp, uDown);
			bounds = sbounds[1];
			edge.bounds = bounds;
		}
	}
	
	
	// GRAPHICS INIT
	
	private void initNode(final int ni, final VisualNode node) {
		final Data data = packet.data[ni];
		
		node.width = data.width;
		node.height = data.height;
	}
	
	
	private void initTexture(final int ni, final VisualNode node) {
		// 1. clear with black
		// 2. render 
		// 3. derive a texture object
		// 4. store texture object
		//     (we upload to memory on demand)
		final BufferedImage txBuffer = new BufferedImage(
				node.width, node.height,
//				256, 256,
//				BufferedImage.TYPE_USHORT_565_RGB
				BufferedImage.TYPE_3BYTE_BGR
			);
		
		final Graphics2D g2d = (Graphics2D) txBuffer.getGraphics();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
				RenderingHints.VALUE_RENDER_QUALITY);
		
		g2d.setPaint(Color.BLACK);
		g2d.fillRect(0, 0, txBuffer.getWidth(), txBuffer.getHeight());
		
		// TODO: render -- use the polys
		for (int i = 0; i < node.blobCount; ++i) {
			final int repr = node.blobReprs[i];
			
			g2d.setPaint(new Color(repr));
			g2d.fill(node.polys[i]);
		}
		
		
		g2d.dispose();
		
//		try {
//			ImageIO.write(txBuffer, "png", new File("c:/temp/buffer_out.png"));
//		}
//		catch (IOException e) {
//		}
		
		
//		final TextureData data = TextureIO.newTextureData(txBuffer, false);
//		node.txData = data;
//		node.txId = -1;
		node.tx = TextureIO.newTexture(txBuffer, false);
		
		node.tx.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		node.tx.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		
//		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
//		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		
		
	}
	
	// unpack polygons into either a vbo or the node for immediate mode
	private void initNode2(final int ni, final VisualNode node, final TessCallback tessc) {
		// create vvbos and display lists
		// for polygons
		
		
		
		
		
		final Format0.Data data = packet.data[node.id];
		
		
		final int N = data.blobs.length;

		int outlineOff = 0;
		int triOff = 0;
		
		
		float[][] triPoints = new float[N][];
		float[][] outlinePoints = new float[N][];
		
//		node.vbo = new int[N];
//		// Initialize to -1:
//		for (int i = 0; i < N; ++i) {
//			node.vbo[i] = -1;
//		}
//		node.buffer = new FloatBuffer[N];
//		node.modes = new int[N][];
//		node.allFirsts = new int[N][][];
//		node.allCounts = new int[N][][];
		node.empty = new boolean[N];
		
		node.indices = new ShortBuffer[N];
		node.ivbo = new int[N];
		for (int i = 0; i < N; ++i) {
			node.ivbo[i] = -1;
		}
		node.icount = new int[N];
		
		
		node.blobCount = N;
//		node.outlineBuffer = new FloatBuffer[N];
		node.outlineIndices = new ShortBuffer[N];
		node.outlineCounts = new int[N];
//		node.outlineVbo = new int[N];
//		for (int i = 0; i < N; ++i) {
//			node.outlineVbo[i] = -1;
//		}
		node.outlineIVbo = new int[N];
		for (int i = 0; i < N; ++i) {
			node.outlineIVbo[i] = -1;
		}
		
		
		node.start = new int[N];
		node.end = new int[N];
		node.outlineStart = new int[N];
		node.outlineEnd = new int[N];
		
		node.mms = new float[N];
		
		node.t0s = new float[N];
		for (int i = 0; i < N; ++i) {
			node.t0s[i] = _2_PI * (float) Math.random();
		}
		
		
		for (int i = 0; i < N; ++i) {
			// TODO: need a tesselator
			
			// TODO: unpack to a boundary
			
			// TODO: create rainbow boundary

			final RainbowsEdge rainbow = new RainbowsEdge(-2.f);
			
			final PathCallback callback = new PathCallback() {
				public void startPolygon(final int contourCount) {
				}
				
				public void endPolygon() {
				}
				
				public void startContour(final int lineCount) {
				}
				
				public void endContour() {
				}
				
				public void moveTo(final float x0, final float y0) {
					rainbow.moveTo(x0, y0);
				}
				
				public void lineTo(final float x, final float y) {
					rainbow.lineTo(x, y);
				}
			};

			PathCodecs.unpack2(data.blobs[i].points, 
					new CompressingPathCallback(callback));
			
			final TFloatArrayList rpoints = new TFloatArrayList(32);
			final TIntArrayList roffs = new TIntArrayList(4);
			final float[] coords = new float[6];
			float x0 = 0.f;
			float y0 = 0.f;
			for (PathIterator itr = rainbow.getPathIterator(null);
				!itr.isDone(); itr.next()
			) {
				switch (itr.currentSegment(coords)) {
					case PathIterator.SEG_MOVETO:
						if (! rpoints.isEmpty()) {
							roffs.add(rpoints.size() / 2);
						}
						x0 = coords[0];
						y0 = coords[1];
						rpoints.add(x0);
						rpoints.add(y0);
						break;
					case PathIterator.SEG_CLOSE:
						rpoints.add(x0);
						rpoints.add(y0);
						break;
					case PathIterator.SEG_LINETO:
						rpoints.add(coords[0]);
						rpoints.add(coords[1]);
						break;
					case PathIterator.SEG_QUADTO:
						rpoints.add(coords[2]);
						rpoints.add(coords[3]);
						break;
					case PathIterator.SEG_CUBICTO:
						rpoints.add(coords[4]);
						rpoints.add(coords[5]);
						break;
					
					default:
						throw new IllegalStateException();
				}
			}
			if (! rpoints.isEmpty()) {
				roffs.add(rpoints.size() / 2);
			}
			
			
			{
				// convert to draw elements, use GL_LINE_SEGMENTS
				final int[] offs = roffs.toNativeArray();
				final int[] firsts = new int[offs.length];
				final int[] counts = new int[offs.length];
				firsts[0] = 0;
				counts[0] = offs[0];
				for (int j = 1; j < offs.length; ++j) {
					firsts[j] = offs[j - 1];
					counts[j] = offs[j] - offs[j - 1];
				}
				
				// CONVERT TO INDICES:
				// 1 + (n - 2)
				int segCount = 0;
				for (int count : counts) {
					segCount += 1 + (count - 2);
				}
				
				final short[] indices = new short[segCount << 1];
				int ii = 0;
				for (int j = 0; j < firsts.length; ++j) {
					for (int k = firsts[j]; k + 1 < firsts[j] + counts[j]; 
					++k, ii += 2) {
						indices[ii] = (short) (outlineOff + k);
						indices[ii + 1] = (short) (outlineOff + k + 1);
					}
				}
				assert ii == indices.length;
				
				
				final float[] points = rpoints.toNativeArray();
//				final FloatBuffer buffer = BufferUtil.newFloatBuffer(points.length);
//				buffer.put(points);
//				buffer.flip();
				outlinePoints[i] = points;
				
				
				final ShortBuffer ibuffer = BufferUtil.newShortBuffer(indices.length);
				ibuffer.put(indices);
				ibuffer.flip();
				
				node.outlineStart[i] = outlineOff;
				outlineOff += points.length / 2;
				node.outlineEnd[i] = outlineOff;
				
//				node.outlineBuffer[i] = buffer;
				node.outlineIndices[i] = ibuffer;
				node.outlineCounts[i] = indices.length;
//				node.outlineVbo[i] = -1;
			}
			
			
			
			
			// Unpack to a tesellated poly:
			tessc.reset();
			PathCodecs.unpack2(data.blobs[i].points,
					// TODO: bring back the compressor in the future
					new CompressingPathCallback(tessc));
					//new CompressingPathCallback(callback));
			
			// TODO: must be on swing thread to store in vbo
			// TODO: attempt to store in vbo
			
			if (tessc.types.isEmpty()) {
				// TODO: what to do in this case?
				node.empty[i] = true;
				continue;
			}
			
			final int n = tessc.frames.size();
			final float[] points = tessc.frames.toNativeArray();
			triPoints[i] = points;
//			FloatBuffer buffer = BufferUtil.newFloatBuffer(n);
//			buffer.put();
//			buffer.flip();
			
			// Attempt to store in a vbo;
			// if can't store, we'll just keep a ref to the buffer
			
			
			final int[] types = tessc.types.toNativeArray();
			
			// map mode -> int list for firsts
			//     mode -> int list for counts
			// map mode -> count
			
			// 1. histogram the types
			//    (need a map of mode -> index)
			
			TIntIntHashMap counts = new TIntIntHashMap(8);
			for (int j = 0; j < types.length; j += 2) {
				final int mode = types[j];
				counts.put(mode, counts.get(mode) + 1);
			}
			
			final int mN = counts.size();
			int[] modes = counts.keys();

			int[][] allFirsts = new int[mN][];
			int[][] allCounts = new int[mN][];
			
			TIntIntHashMap indexMap = new TIntIntHashMap(mN);
			// Create map of type -> index
			for (int j = 0; j < modes.length; ++j) {
				final int mode = modes[j];
				indexMap.put(mode, j);
				final int count = counts.get(mode);
				allFirsts[j] = new int[count];
				allCounts[j] = new int[count];
			}
			
			int[] indices = new int[mN];
			// thanks to jvm for initializing to 0
			
			
			for (int tindex = 0, j = 0; j + 1 < types.length; j += 2) {
				final int mode = types[j];
				final int count = types[j + 1] - tindex;
				final int index = indexMap.get(mode);
				final int index1 = indices[index]++;
				allFirsts[index][index1] = tindex;
				allCounts[index][index1] = count;
				
				tindex += count;
			}
			
			
			final short[] tindices = OglUtilities.toTrianglesIndices(triOff, modes, allFirsts, allCounts);
			final int ilen = tindices.length;
			final ShortBuffer ibuffer = BufferUtil.newShortBuffer(ilen);
			ibuffer.put(tindices);
			ibuffer.flip();
			
			node.start[i] = triOff;
			triOff += points.length / 2;
			node.end[i] = triOff;
			
//			node.vbo[i] = -1;
//			node.buffer[i] = buffer;
//			node.modes[i] = modes;
//			node.allFirsts[i] = allFirsts;
//			node.allCounts[i] = allCounts;
			node.indices[i] = ibuffer;
			node.ivbo[i] = -1;
			node.icount[i] = ilen;
			
			node.empty[i] = false;
			// TODO: attempt to transfer into VBO.
			// if fail, store the buffer for immediate mode rendering
			// TODO: store these in the node
			
			
			
			
			
			
		}
		
		
		// Create tri and outline buffers:
		final FloatBuffer triBuffer = BufferUtil.newFloatBuffer(2 * triOff);
		for (int i = 0; i < N; ++i) {
			if (null == triPoints[i]) continue;
			triBuffer.put(triPoints[i]);
		}
		triBuffer.flip();
		node.buffer = triBuffer;
		node.vbo = -1;
		
		final FloatBuffer outlineBuffer = BufferUtil.newFloatBuffer(2 * outlineOff);
		for (int i = 0; i < N; ++i) {
			if (null == outlinePoints[i]) continue;
			outlineBuffer.put(outlinePoints[i]);
		}
		outlineBuffer.flip();
		node.outlineBuffer = outlineBuffer;
		node.outlineVbo = -1;
		
		
		
		/*
		// Contours
		final int[][][] allPackedCountours = data.contours;
		node.contourCenters = new float[allPackedCountours.length][][];
		for (int i = 0; i < allPackedCountours.length; ++i) {
			final float[][][] _contours = {null};
			
			final PathCallback callback = new PathCallback() {
				float[][] contours;
				private int ci = 0;
				private int pi = 0;
				
				public void startPolygon(final int contourCount) {
					contours = new float[contourCount][];
				}
				
				public void endPolygon() {
					assert ci == contours.length;
					_contours[0] = contours;
				}
				
				public void startContour(final int lineCount) {
					contours[ci] = new float[2 * (1 + lineCount)];
					pi = 0;
				}
				
				public void endContour() {
					++ci;
					
					assert ci <= contours.length;
				}
				
				public void moveTo(final float x0, final float y0) {
					assert ci < contours.length && pi + 1 < contours[ci].length;
					contours[ci][pi] = x0;
					contours[ci][pi + 1] = y0;
					pi += 2;
				}
				
				public void lineTo(final float x, final float y) {
					assert ci < contours.length && pi + 1 < contours[ci].length;
					contours[ci][pi] = x;
					contours[ci][pi + 1] = y;
					pi += 2;
				}
			};
			
			PathCodecs.unpack2(allPackedCountours[i],
					callback
			);
		
			node.contourCenters[i] = _contours[0];
		}
		*/
		
		/*
		node.outlineVbo = new int[N];
		// Initialize to -1:
		for (int i = 0; i < N; ++i) {
			node.outlineVbo[i] = -1;
		}
		node.outlineBuffer = new FloatBuffer[N];
		node.outlineCounts = new int[N];
		
		for (int i = 0; i < N; ++i) {
			final int _i = i;
			
			final PathCallback callback = new PathCallback() {
				@Override
				public void startContour(int lineCount) {
					// TODO: create buffer
				}
				
				@Override
				public void endContour() {
				}

				@Override
				public void startPolygon(int contourCount) {
				}
				
				@Override
				public void endPolygon() {
				}
				

				@Override
				public void moveTo(float x0, float y0) {
				}

				@Override
				public void lineTo(float x, float y) {
				}
			};
			
			PathCodecs.unpack2(data.blobs[i].points,
					callback);
		}
		
		*/
		
		
		triPoints = null;
		outlinePoints = null;
		
		node.noise = new Noise0(N);
		
		
		
		
		// Clean points:
		
		final float[][] cleanPoints = new float[N][];
		{
		final TFloatArrayList points = new TFloatArrayList(128);
		for (int i = 0; i < N; ++i) {
			points.clear();
			
			final PathCallback callback = new PathCallback() {
				public void startPolygon(final int contourCount) {
				}
				public void endPolygon() {
				}
				public void startContour(final int lineCount) {
				}
				public void endContour() {
				}
				
				public void moveTo(final float x0, final float y0) {
					points.add(x0);
					points.add(y0);
				}
				public void lineTo(final float x, final float y) {
					points.add(x);
					points.add(y);
				}
			};
			
			PathCodecs.unpack2(data.blobs[i].points, 
					new CompressingPathCallback(callback));
			
			cleanPoints[i] = points.toNativeArray();
		}
		
		// Check that all points are within bounds:
		for (int i = 0; i < N; ++i) {
			final float[] clean = cleanPoints[i];
			for (int j = 0; j < clean.length; j += 2) {
//				assert 0 <= clean[j] && clean[j] <= node.width;
//				assert 0 <= clean[j + 1] && clean[j + 1] <= node.height;
				if (clean[j] < 0) clean[j] = 0;
				else if (node.width < clean[j]) clean[j] = node.width;
				
				if (clean[j + 1] < 0) clean[j + 1] = 0;
				else if (node.height < clean[j + 1]) clean[j + 1] = node.height;
				
			}
		}
		}
		
		node.polys = new Polygon[N];
		for (int i = 0; i < N; ++i) {
			final float[] points = cleanPoints[i];
			final int n = points.length / 2;
			final int[] xs = new int[n];
			final int[] ys = new int[n];
			
			for (int j = 0; j < n; ++j) {
				final int k = j << 1;
				xs[j] = (int) points[k];
				ys[j] = (int) points[k + 1];
			}
			
			final Polygon poly = new Polygon(xs, ys, n);
			
			node.polys[i] = poly;
		}
		

		
		// TODO: create vbos for contours.
		// TODO: store break indices like types above
		// create for contours
		
//		packet.data[node.id].contours
		
		
		node.absBounds = new Rectangle2D.Float[N];
		node.fracBounds = new Rectangle2D.Float[N];
		
		for (int i = 0; i < N; ++i) {
			final Rectangle2D.Float bounds = computeBounds(cleanPoints[i]);
			
			node.absBounds[i] = bounds;
			node.fracBounds[i] = new Rectangle2D.Float(
				bounds.x / node.width,
				bounds.y / node.height,
				bounds.width / node.width,
				bounds.height / node.height
			);
		}
	}
	
	
	private static Rectangle2D.Float computeBounds(final float[] points) {
		float xmin = points[0];
		float xmax = points[0];
		float ymin = points[1];
		float ymax = points[1];
		
		for (int j = 2; j < points.length; j += 2) {
			final float x = points[j];
			final float y = points[j + 1];
			if (x < xmin) xmin = x;
			else if (xmax < x) xmax = x;
			if (y < ymin) ymin = y;
			else if (ymax < y) ymax = y;
		}
		
		return new Rectangle2D.Float(
			xmin, ymin,
			xmax - xmin,
			ymax - ymin
		);
	}
	
	
	private void initEdge(final int ei, final VisualEdge edge) {
		// vreate vbo and display list
		// for curve
		
	}
	
	// TODO: need code that converts segment chain to variable width
	// TODO: quad strip
	
	
	
	private void connectFromBlob(final VisualEdge edge) {
		nodes[edge.id0].blobEdgeMap.put(edge.id1, edge);
		nodes[edge.id1].blobEdgeMap.put(edge.id0, edge);
	}
	
	private void connectFromWord(final VisualEdge edge) {
		nodes[edge.id0].wordEdgeMap.put(edge.id1, edge);
		nodes[edge.id1].wordEdgeMap.put(edge.id0, edge);
	}
	
	
	
	private void initListeners() {
		final MouseInputAdapter mil = new MouseInputAdapter() {
			private void sync(final MouseEvent e) {
				vmousex = e.getX();
				vmousey = e.getY();
				
//				gmousex = ((vmousex - getWidth() / 2.f) - cx )/ scale;
//				gmousey =  /*-*/ ((vmousey - getHeight() / 2.f) - cy) / scale;
				
				{
					
					final AffineTransform m2v = basem2v();
					m2v.concatenate(gbasem2v());
					final AffineTransform v2m = INV(m2v);
					
					final Point2D.Float pt = new Point2D.Float(vmousex, vmousey);
					v2m.transform(pt, pt);
					
					gmousex = pt.x;
					gmousey = pt.y;
					
//					rmousex = ((vmousex - getWidth() / 2.f - rftx) / rscale - rx0) * node.width / (rx1 - rx0) - rfcx;
//					rmousey = ((vmousey - getHeight() / 2.f - rfty) / rscale - ry0) * node.height / (ry1 - ry0) - rfcy;
					
//					System.out.println("  [" + rmousex + ", " + rmousey + "]");
				}
				
				if (0 <= rni) {
//					final VisualNode node = nodes[rni];
					
					final AffineTransform m2v = basem2v();
					m2v.concatenate(rbasem2v());
					final AffineTransform v2m = INV(m2v);
					
					final Point2D.Float pt = new Point2D.Float(vmousex, vmousey);
					v2m.transform(pt, pt);
					
					rmousex = pt.x;
					rmousey = pt.y;
					
//					rmousex = ((vmousex - getWidth() / 2.f - rftx) / rscale - rx0) * node.width / (rx1 - rx0) - rfcx;
//					rmousey = ((vmousey - getHeight() / 2.f - rfty) / rscale - ry0) * node.height / (ry1 - ry0) - rfcy;
					
//					System.out.println("  [" + rmousex + ", " + rmousey + "]");
				}
			}
			
			
			@Override
			public void mouseMoved(final MouseEvent e) {
				sync(e);
			}
			
			private float startFlicx = 0.f;
			private float startFlicy = 0.f;
			
			@Override
			public void mousePressed(final MouseEvent e) {
				sync(e);
				
				if (MouseEvent.BUTTON1 == e.getButton()) {
				if (gtransu < G_TRANS_CUT) {
				
				// Find closest node:
				final VisualNode closest = closestNode(gmousex, gmousey);
				
				if (null != closest) {
					target(closest.id);
					
					queueGMovement(closest.id);
				}
				}
				}
				else {
					// Interpret as a start zoom flic:
					startFlicx = e.getX();
					startFlicy = e.getY();
				}
			}
			
			@Override
			public void mouseReleased(final MouseEvent e) {
				if (MouseEvent.BUTTON1 != e.getButton()) {
					final float flicx = e.getX();
					final float flicy = e.getY();
					
					final float dy = flicy - startFlicy;
					float inc = -dy / 50.f;
				
					inc = Math.signum(inc) * Math.min(9, Math.abs(inc));
					
					zinc += inc / 200.f;
				}
			}
			
			
		};
		
		final MouseWheelListener mwl = new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
//				System.out.println(e.getScrollAmount());
//				System.out.println(e.getUnitsToScroll());
//				System.out.println(e.getClickCount());

				final float inc = -e.getUnitsToScroll();
				zinc += inc / 200.f;
				
				if (0 < inc && gtransu < G_TRANS_CUT) {
					// focus on the node that we're pointing at ...
					final int id = sortIndices[nodes.length - 1];
					if (0.8f <= nodes[id].mm) {
					target(id);
					queueGMovement(id);
					}
				}
				
				
			}
		};
		
		canvas.addMouseListener(mil);
		canvas.addMouseMotionListener(mil);
		canvas.addMouseWheelListener(mwl);
	}
	
	
	
	/**************************
	 * SCREEN IMPLEMENTATION
	 **************************/
	
	@Override
	public void dispose() {
	}
	
	private static Signature MAX_SIG(final Signature sig, int n) {
		if (sig.count < n) {
			n = sig.count;
		}
		float netw = 0;
		for (int i = 0; i < n; ++i) {
			netw += sig.weights[i];
		}
		final int[] colors = new int[n];
		final float[] weights = new float[n];
		for (int i = 0; i < n; ++i) {
			colors[i] = sig.colors[i];
			weights[i] = sig.weights[i] / netw;
		}
		return new Signature(n, 
			colors,
			weights);
	}
	
	
	
	private static int[][] filterBlobLinks(final OutputPacket2 packet, final int[][] inputLinks) {
		
		// for each, if out link sig doesn't match main sig at least N,
		// then don't include it (-1)
		
		final float MAX_D = 0.65f;
		
		final SignatureMetric metric = new WeightedInverseMetric();

		final TLongHashSet linked = new TLongHashSet(inputLinks.length * 4);
		
		final int[][] outputLinks = new int[inputLinks.length][];
		for (int i = 0; i < inputLinks.length; ++i) {
			linked.clear();

			final int[] links = inputLinks[i];
			final int[] filteredLinks = new int[links.length];
			
			
			final Data data = packet.data[i];
			for (int j = 0; j < links.length; ++j) {
//				metric.distance(new Signature(1, new int[]{0xFFFFFFFF}, new float[]{1.f}),
//						new Signature(1, new int[]{0}, new float[]{1.f}));
				
				if (links[j] < 0) {
					filteredLinks[j] = -1;
					continue;
				}
				
				final int rgb = data.blobs[j].repr;
				if (SickMommaUtilities.isFilterGrey(rgb)) {
					filteredLinks[j] = -1;
					continue;
				}
				
				final long sig = (links[j] & 0xFFFFFFFFL) | ((rgb & 0xFFFFFFFF) << 32);
				if (linked.contains(sig)) {
					filteredLinks[j] = -1;
					continue;
				}
				
				linked.add(sig);
				
//				if (MAX_D < metric.distance(data.mainSig,
//						data.blobs[j].sig)) {
//					filteredLinks[j] = -1;
//					continue;
//				}
				
				filteredLinks[j] = links[j];
			}
			
			outputLinks[i] = filteredLinks;
		}
		
		return outputLinks;
	}
	

	@Override
	public void init(final Object ... inPackets) {
		packet = (OutputPacket2) inPackets[0];
		
		// TODO: debugging hook
		if (!Ere.IN_APPLET && null == DEBUG_LOAD()) {
			DEBUG_SAVE(packet);
		}
		
		// 1. build nodes
		// 2. initVbos -- create as many as we can
		
		
		/*
		// STATS
		
		TIntHashSet uls = new TIntHashSet(32);
		for (int[] links : packet.blobLinks) {
			uls.clear();
			for (int link : links) {
				uls.add(link);
			}
			System.out.println("    " + uls.size());
		}
		
		// END STATS
		*/
		
		
		unfilteredBlobLinks = packet.blobLinks;
		final int[][] blobLinks = filterBlobLinks(packet, packet.blobLinks);
		
		
		final int N = packet.data.length;
		
		// init strings:
		titles = new String[N];
		titleWidths = new float[N];
		titleHeights = new float[N];
		branchWidths = new float[N];
		branchHeights = new float[N];
		for (int i = 0; i < N; ++i) {
			titles[i] = TITLE(String.valueOf(packet.linkedItems[i][1]),
					String.valueOf(packet.linkedItems[i][3]));
		}
		
		
		nodes = new VisualNode[N];
		wordEdges = new VisualEdge[packet.wordLinks.length];
		sigEdges = new VisualEdge[countBlobLinks(blobLinks)];
		edges = new VisualEdge[wordEdges.length + sigEdges.length];
		int edgei = 0;
		int wedgei = 0;
		int sedgei = 0;
		
		// init nodes:
		// one node per data
		for (int i = 0; i < packet.data.length; ++i) {
			final VisualNode node = new VisualNode(i);
			final Signature mainSig = packet.data[i].mainSig;
			node.repr = mainSig.count <= 0 ? 0 : mainSig.colors[0];
			final int[] blobReprs = new int[packet.data[i].blobs.length];
			for (int j = 0; j < packet.data[i].blobs.length; ++j) {
				blobReprs[j] = packet.data[i].blobs[j].repr;
			}
			node.blobReprs = blobReprs;
			nodes[i] = node;
		}
		
		// init edges:
		for (int i = 0; i < blobLinks.length; ++i) {
			// index i is a link between blobi and the links[i] index
			final int[] links = blobLinks[i];
			for (int j = 0; j < links.length; ++j) {
				if (links[j] < 0) {
					continue;
				}
				final VisualEdge edge = new VisualEdge(i, links[j]);
				final BlobData blobData = packet.data[i].blobs[j]; 
				edge.sig = blobData.sig;
				edge.repr = blobData.repr;
				connectFromBlob(edge);
				edges[edgei++] = edge;
				sigEdges[sedgei++] = edge;
				
			}
		}
		for (int i = 0; i < packet.wordLinks.length; ++i) {
			final int[] link = packet.wordLinks[i];
			final VisualEdge edge = new VisualEdge(link[0], link[1]);
			connectFromWord(edge);
			edges[edgei++] = edge;
			wordEdges[wedgei++] = edge;
		}
		
		
		// ALLOCATE EDGE INDICES:
		final int[] c0s = new int[nodes.length];
		final int[] c1s = new int[nodes.length];
		// thanks JVM for initializing to 0s
		for (VisualEdge edge : sigEdges) {
			++c0s[edge.id0];
			++c1s[edge.id1];
		}
		for (int i = 0; i < nodes.length; ++i) {
			final VisualNode node = nodes[i];
			node.sedge0s = new int[c0s[i]];
			node.sedge1s = new int[c1s[i]];
			c0s[i] = 0;
			c1s[i] = 0;
		}
		for (int i = 0; i < sigEdges.length; ++i) {
			final VisualEdge edge = sigEdges[i];
			nodes[edge.id0].sedge0s[c0s[edge.id0]++] = i;
			nodes[edge.id1].sedge1s[c1s[edge.id1]++] = i;
		}
		
		// CHECK
		if (Ere.DEBUG)
		{
			for (int i = 0; i < nodes.length; ++i) {
				assert c0s[i] == nodes[i].sedge0s.length;
				assert c1s[i] == nodes[i].sedge1s.length;
			}
			TIntHashSet usedIndices = new TIntHashSet(sigEdges.length);
			for (VisualNode node : nodes) {
				for (int index : node.sedge0s) {
					assert !usedIndices.contains(index);
					usedIndices.add(index);
				}
			}
			for (VisualEdge edge : sigEdges) {
				assert usedIndices.contains(edge.id0);
			}
		}
		
		// SORT STATE:
		final int sN = nodes.length;
		pls = new ParallelListSorter(sN);
		sortWeights = new float[sN];
		sortIndices = new int[sN];

		// TODO: do a layout now.
		// TODO: launch tesselator on a separate thread and 
		// as we finish items, make them visible.
		// until they're visible, just
		// show a bouncy box
		
		graphLayout();
		

		// Edge noise:
//		if (0 < nodes.length) {
//			for (VisualNode node : nodes) {
//				node.ngen0 = new PerlinNoiseGenerator(1, 4);
//				node.ngen1 = new PerlinNoiseGenerator(1, 4);
//				node.ngen2 = new PerlinNoiseGenerator(1, 4);
//				node.ngen3 = new PerlinNoiseGenerator(1, 4);
//				
//			}
//			
//			
//		}
		
		if (0 < edges.length) {
			for (VisualEdge edge : edges) {
				edge.ngen0 = new PerlinNoiseGenerator(1, 4);
				edge.ngen1 = new PerlinNoiseGenerator(1, 4);
				edge.ngen2 = new PerlinNoiseGenerator(1, 4);
				edge.ngen3 = new PerlinNoiseGenerator(1, 4);
				
				final float[] bounds = edge.bounds;
				edge.u = new float[]{
					bounds[6] - bounds[0],
					bounds[7] - bounds[1],
				};
				norm(edge.u);
				edge.v = new float[]{
					-edge.u[1],
					edge.u[0]
				};
				edge.c = new float[]{
					(bounds[2] + bounds[4]) / 2.f,
					(bounds[3] + bounds[5]) / 2.f
				};
			}
			
			edgeNes = edges[0].ngen0.createEvalState();
		}
		
		for (int i = 0; i < nodes.length; ++i) {
			initNode(i, nodes[i]);
		}
		for (int i = 0; i < edges.length; ++i) {
			initEdge(i, edges[i]);
		}
		
		
		// Create GL environment:
		final GLCapabilities caps = new GLCapabilities();
		

		// Allocate enough space that so that we only
		// have to clear the stencil buffer
		// once per render for recursive rendering
		caps.setStencilBits(8);
		
		
		caps.setAccumAlphaBits(0);
		caps.setAccumRedBits(0);
		caps.setAccumGreenBits(0);
		caps.setAccumBlueBits(0);
		caps.setDepthBits(0);
		
//		caps.setAlphaBits(6);
//		caps.setRedBits(6);
//		caps.setGreenBits(6);
//		caps.setBlueBits(6);
		
		
		caps.setDoubleBuffered(true);		
		caps.setHardwareAccelerated(true);
		caps.setSampleBuffers(false);
		caps.setStereo(false);
		
		
		canvas = new GLCanvas(caps);
		
		canvas.addGLEventListener(this);
		
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);
		
		validate();
		
		initListeners();
		
		
		
		
		
		
		//// POMP 
		
		final VisualNode closest = closestNode(0, 0);
		final int startId = null == closest ? 0
				: closest.id;
		target(startId);
		gfocus(startId);
	}
	

	@Override
	public void start() {
		active = true;
		
		// A background thread does the poly tesselation
		// and other heavy work for each node.
		// Nodes are not fully active until marked by this thread.
		// TODO:
//		launchInitNode2();
		
		// TODO: 8 
	    animator = new FPSAnimator(canvas, 8);
	    animator.start();
	}

	@Override
	public void stop() {
		active = false;
		if (null != animator) {
			animator.stop();
		}
		
		gmqueue.clear();
	}

	/**************************
	 * END SCREEN IMPLEMENTATION
	 **************************/
	
	
	
	private Thread ginit2Thread = null;
	
	private void launchInitNode2() {
//		final boolean vboSupported = gl.isFunctionAvailable("glGenBuffersARB") &&
//        gl.isFunctionAvailable("glBindBufferARB") &&
//        gl.isFunctionAvailable("glBufferDataARB") &&
//        gl.isFunctionAvailable("glDeleteBuffersARB");
//
//		final int[][] vbos;
//		if (vboSupported) {
//			vbos = new int[nodes.length][];
//			for (int i = 0; i < nodes.length; ++i) {
//				vbos[i] = new int[packet.data[i].blobs.length];
//				gl.glGenBuffersARB(vbos[i].length, vbos[i], 0);
//			}
//		}
//		else {
//			vbos = null;
//		}
		
		
//		if (true) {
//			return;
//		}
		
		final Runnable job = new Runnable() {public void run() {
			long time0 = System.nanoTime();
			// Initialize:
			final GLU glu = new GLU();
			final TessCallback tessc = new TessCallback(glu);
			try {
		//		final GriddedPathCallback gpc = new GriddedPathCallback(tessc,
		//				// TODO: magic numbers. pull these from the processing screen at least
		//				20, 20, -550, -550, 55, 55);
		//		gpc.beginHistogramming();
		//		for (VisualNode node : nodes) {
		//			final Format0.Data data = packet.data[node.id];
		//			
		//			for (int i = 0; i < data.blobs.length; ++i) {
		//				PathCodecs.unpack2(data.blobs[i].points,
		//						// TODO: bring back the compressor
		//						gpc);
		//						//new CompressingPathCallback(gpc));
		//			}
		//		}
		//		gpc.endHistogramming();
				
				
				
				
				for (int i = 0; i < nodes.length; ++i) {
					if (! active) {
						return;
					}
					final VisualNode node = nodes[i];
					// TODO: this function churns too much memory ...
					initNode2(i, node, tessc);
//					initTexture(i, node);
					
					SwingUtilities.invokeLater(new Runnable() {public void run() {
						node.ready = true;
					}});
				}
			}
			finally {
				tessc.dispose();
			}
			long time1 = System.nanoTime();
			System.out.println(time1 - time0);
			
			
			// All important information should now be stored ...
			packet = null;
			
			
			try {
				SwingUtilities.invokeAndWait(new Runnable() {public void run() {}});
			}
			catch (InvocationTargetException e) {
			}
			catch (InterruptedException e) {
			}
			
			System.gc();
			
//			animator.setRunAsFastAsPossible(true);
			SwingUtilities.invokeLater(new Runnable() {public void run() {
				animator.stop();
				animator = new FPSAnimator(canvas, 12);
				animator.start();
			}});
		}};
		
		ginit2Thread = new Thread(
			new Runnable() {public void run() {
				job.run();
				ginit2Thread = null;
			}},
			"ENTER & RE-EXIT GINIT2");
		ginit2Thread.setPriority(Thread.NORM_PRIORITY);
		ginit2Thread.setDaemon(true);
		ginit2Thread.start();
	}
	
	
	
	private FPSAnimator animator;
	private int frame = 0;
	
	final int N = 24;
//	final float[] bounds2 = new float[8];
	final float[] samples = new float[N << 1];
	final float[] ths = new float[N << 1];
	final float[] qsamples = new float[N << 2];
	final FloatBuffer qsamplesb = BufferUtil.newFloatBuffer(N << 2);
//	final float[] out = new float[2];
	
	private class QSCB implements QuadStripCallback {
		public GL gl;
		
		@Override
		public void start(float x0, float y0, float x1, float y1) {
			gl.glVertex2f(x0, y0);
			gl.glVertex2f(x1, y1);
		}
		
		@Override
		public void end(float x0, float y0, float x1, float y1) {
			gl.glVertex2f(x0, y0);
			gl.glVertex2f(x1, y1);
		}

		@Override
		public void side(float x0, float y0, float x1, float y1) {
			gl.glVertex2f(x0, y0);
			gl.glVertex2f(x1, y1);
		}

		
	};
	private final QSCB qscb = new QSCB();
	
	private class WTF implements ThicknessFunction {
		public int N;
		public float max;
		public float min;
		
		@Override
		public float eval(final int i, final float x, final float y) {
			final float u = i / (float) (N - 1);
			return max + (min - max) * u;
		}
	}
	private WTF wtf = new WTF();
	
	
	private class TF implements ThicknessFunction {
//		public int N;
//		public float max;
//		public float min;
		public float m = 1.f;
		
		@Override
		public float eval(final int i, final float x, final float y) {
			final float dx = gmousex - x;
			final float dy = gmousey - y;
			final float dsq = dx * dx + dy * dy;
//			2498.04 + 0.999118 * dsq;
			final float dsqsq = dsq * dsq;
//			final float t = 1 - 0.00010000500033334732f * dsq + 
//			5.0005000458363995e-9f * dsqsq
//			- 1.66692e-13f * dsqsq * dsq;
//			final float t = 1 - 0.0010005f * dsq + 
//			5.005e-7f * dsqsq
//			- 1.66917e-10f * dsqsq * dsq;
			final float t = 1 - 0.0000500013f * dsq + 
			1.25006e-9f * dsqsq
			- 2.08349e-14f * dsqsq * dsq;
			final float u = i / (float) (N - 1);
			
			return //(max + (min - max) * u) *
			(m + (t < 0 ? 1.f : 1.f + 1.5f * t));
			// 1.66692*10^-13*s*s*s
//			return 1 + 5.f * (2498.04f + 0.999118f * (dsq - 50 * 50));
			//(float) Math.pow(0.9999, dsq);
		}
	};
	
	private TF tf = new TF();
	
	
	
	private final float[] as, bs, cs, ds, us;
	
	{
		as = new float[N];
		bs = new float[N];
		cs = new float[N];
		ds = new float[N];
		us = new float[N];
		for (int i = 0; i < N; ++i) {
			final float u = i / (float) (N - 1);
			final float v = 1.f - u;
			
			final float vsq = v * v;
			final float usq = u * u;
			as[i] = v * vsq;
			bs[i] = 3 * u * vsq;
			cs[i] = 3 * usq * v;
			ds[i] = u * usq;
			us[i] = u;
		}
	}
	
	
	
	
	
	
	// MATRICES FOR CALCULATIONS
	// these are used by calc code, not the rendering code.
	// so we just copy it from the renderer to here
	
	private AffineTransform basem2v() {
		final AffineTransform at = new AffineTransform();
		
		// a graph thing:
//		at.translate(0.f, height / 2.f);
//        at.scale(1.f, -1.f);
//        at.translate(0.f, -height / 2.f);
        
        at.translate(width / 2.f, height / 2.f);
        return at;
	}
	
	private AffineTransform rbasem2v() {
		return rbasem2v(rscale);
	}
	
	private AffineTransform rbasem2v(final float scale) {
		final AffineTransform at = new AffineTransform();
		at.translate(rfcx + rftx, rfcy + rfty);
		at.scale(scale, scale);
		at.translate(-rfcx, -rfcy);
		return at;
	}
	
	private AffineTransform gbasem2v() {
		final AffineTransform at = new AffineTransform();
//		at.translate(cx, cy);
		at.scale(gscale, gscale);
		at.translate(-gcx, -gcy);
		return at;
	}
	
	private AffineTransform rnode2m() {
		// 0 -> rx0/ry0
		// w/h -> rx1/ry1
		
		
		
		final VisualNode node = nodes[rni];
		
		final float[] rbounds = rbounds(node);
		final float rx0 = rbounds[0];
		final float ry0 = rbounds[1];
		final float rx1 = rbounds[2];
		final float ry1 = rbounds[3];
		
		final float sx = (rx1 - rx0) / node.width;
		final float sy = (ry1 - ry0) / node.height;
		final float tx = rx0;
		final float ty = ry0;
		return new AffineTransform(sx, 0, 0, sy, tx, ty);
	}
	
	
	
	
	
	
	
	
	private static enum RenderMode {
		GRAPH,
		RECURSIVE
	}
	
	
	private RenderMode renderMode = RenderMode.RECURSIVE;
	
	// at this cutoff, zooming is locked to the current index,
	// the camera center begins to move towards the current index center,
	// and we begin to draw a full under the overlay
	private final float R_TRANS_CUT = 0.6f;
	private final float G_TRANS_CUT = 0.6f;
	
	
	// TODO: g transition form graph to recursive 
	
	
	private float gtransu = 0.5f;
	
	private int rni = 0;
	private int rfocusIndex = -1;
	private float rmx, rmy;
	private float rfcx = 0.f, rfcy = 0.f;
	private float rftx = 0.f, rfty = 0.f;
	private float rtransu = 0.f;
	private float rscale = 1.f;
	
	float rpmx = -1, rpmy = -1;
	float rcapmousex, rcapmousey;
	
	
	private static float ZUP(final float zu) {
		return (float) Math.pow(zu, 1.7);
	}
	
	
	private void syncRmm() {
		final VisualNode fnode = nodes[rni];
		fnode.mm = 1.f;
		
		// sync mm of all subs, one deep,
		// according to proximity to mouse
		
		
		
		for (int i = 0; i < fnode.blobCount; ++i) {
//			final VisualNode node = nodes[unfilteredBlobLinks[rni][i]];
			
			final Rectangle2D.Float b = fnode.absBounds[i];
			
			final float bcx = b.x + b.width / 2.f;
			final float bcy = b.y + b.height / 2.f;
			
//			System.out.println("   (" + rmx + ", " + rmy + ") -- [" + bcx + ", " + bcy + "]");
			
			final float dx = rmx - bcx;
			final float dy = rmy - bcy;
			// 6 is a fudge factor by eye
			final float dsq = 6 * (dx * dx + dy * dy);
			final float dsqsq = dsq * dsq;
			final float t = 1 - 0.0000500013f * dsq + 
				1.25006e-9f * dsqsq
				- 2.08349e-14f * dsqsq * dsq;
			
//			System.out.println(t);
			
			fnode.mms[i] = t < 0.f ? 0.f : 1 < t ? 1.f : t;
			
		}
	}
	
	
	private void syncRIndices() {
		// for the current zoom node, find the blob that rmousex/y are in
		// if there is none, use the closest (project mouse points back to the
		// corners of the rendered rect)
		
		
		
		if (rni < 0 || !nodes[rni].ready) {
			return;
		}

		final VisualNode node = nodes[rni];
		
		// point of no return -- can't switch focus any more
//		if (0.6f < rtransu) {
//			return;
//		}
		
		rcapmousex = rmousex;
		rcapmousey = rmousey;
		
		final AffineTransform n2m = rnode2m();
		final AffineTransform m2n = INV(n2m);
		
		final Point2D.Float pt = new Point2D.Float(rcapmousex, rcapmousey);
		m2n.transform(pt, pt);
		
		rmx = pt.x;
		rmy = pt.y;
		
		if (rmx < 0) rmx = 0;
		else if (node.width <= rmx) rmx = node.width - 1;
		
		if (rmy < 0) rmy = 0;
		else if (node.height <= rmy) rmy = node.height - 1;
	
		if (rmx == rpmx && rmy == rpmy) {
			return;
		}
		
		rpmx = rmx;
		rpmy = rmy;
		
		// find closest index
		
//		rfocusIndex = 0;
		for (int i = 0; i < node.blobCount; ++i) {
			if (node.polys[i].contains(rmx, rmy)) {
				rfocusIndex = i;
				break;
			}
		}
		
		
		
		
		
	}
	
	
	private float gzoomIn(float step) {
		
		if (1.f <= gtransu + step) {
			gtransu = 1.f;
			return gtransu + step - 1.f;
		}
		
		gtransu += step;
		
		return 0;
	}
	
	private void gzoomOut(float step) {
		if (gtransu - step <= 0.f) {
			gtransu = 0.f;
			return;
		}
		
		gtransu -= step;
	}
	
	
	
	private void rzoomIn(float step) {
		// while 
		
		if (rfocusIndex < 0) {
			return;
		}
		
		while (1.f <= rtransu + step) {
			step -= 1.f - rtransu;
			
			final int id = unfilteredBlobLinks[rni][rfocusIndex];
			target(id);
			gfocus(id);
			rtransu = 0.f;
			
			syncRIndices();
		}
		
		
//		if (1.f <= rtransu + step) {
//			// TODO: reset everything to the next node
//			return;
//		}
		
		
		
		
		final float rpscale = 1.f + rtransu * 1.f;
		rtransu += step;
		final float rnscale = 1.f + rtransu * 1.f;
		
		final boolean lock = R_TRANS_CUT <= rtransu;
		
		if (lock) {
			// zoom towards the center
			
			float zu = ZUP((rtransu - R_TRANS_CUT) / (1.f - R_TRANS_CUT));
			
			// transition rftXY to the bounds X/Y
			// transition rfcXY to 0
			
			// OR
			// keep rfxXY the same, and transition rftXY to put the bounds in the center
			
			final float rtscale = 1.f + R_TRANS_CUT * 1.f;
			
			
			// move from bounds at cut point
			// to (rx0, ry0, ...)
			// according to ZU
			
			// rfocusIndex;
			
			// mx0/my0 are the 0 corner of the blob bounds fit to (rx0, ry0, ...)
			
			final VisualNode node = nodes[rni];
			if (! node.ready) {
				return;
			}
			final Rectangle2D.Float fb = node.fracBounds[rfocusIndex];
			
			final float[] rbounds = rbounds(node);
			final float rx0 = rbounds[0];
			final float ry0 = rbounds[1];
			final float rx1 = rbounds[2];
			final float ry1 = rbounds[3];
			
			final float mx0 = rx0 + (rx1 - rx0) * fb.x;
			final float my0 = ry0 + (ry1 - ry0) * fb.y;
			
			final float vx0 = (mx0 - rfcx) * rtscale + rfcx;
			final float vy0 = (my0 - rfcy) * rtscale + rfcy;
			final float vx1 = rx0;
			final float vy1 = ry0;
			
			
			
			final float dx = vx1 - vx0;
			final float dy = vy1 - vy0;
			
			final float vx = vx0 + dx * zu;
			final float vy = vy0 + dy * zu;
			
			rftx = (vx - rfcx) - (mx0 - rfcx) * rnscale;
			rfty = (vy - rfcy) - (my0 - rfcy) * rnscale;
			
		}
		else if (1.f < rnscale)  {
			// keep the point at (rmx, rmy)  at the same screen location
			//

			rfcx = (rcapmousex * (rpscale - rnscale) + rfcx * (1 - rpscale)) / (1 - rnscale);
			rfcy = (rcapmousey * (rpscale - rnscale) + rfcy * (1 - rpscale)) / (1 - rnscale);
			
//			rfcx *= -1;
//			rfcy *= -1;
			
//			System.out.println(String.format("[%f, %f]", rfcx, rfcy));
		}
	}
	
	private float rzoomOut(final float step) {
		if (rfocusIndex < 0) {
			return 0.f;
		}
		
		if (rtransu - step <= 0) {
			rtransu = 0.f;
			return step - rtransu;
		}
		
		
//		if (1.f <= rtransu + step) {
//			// TODO: reset everything to the next node
//			return;
//		}
		
		
		
		final float rpscale = 1.f + rtransu * 1.f;
		rtransu -= step;
		final float rnscale = 1.f + rtransu * 1.f;
		
		final boolean lock = R_TRANS_CUT <= rtransu;
		
		
		if (lock) {
			// zoom towards the center
			
			float zu = ZUP((rtransu - R_TRANS_CUT) / (1.f - R_TRANS_CUT));
			
			// transition rftXY to the bounds X/Y
			// transition rfcXY to 0
			
			// OR
			// keep rfxXY the same, and transition rftXY to put the bounds in the center
			
			final float rtscale = 1.f + R_TRANS_CUT * 1.f;
			
			
			// move from bounds at cut point
			// to (rx0, ry0, ...)
			// according to ZU
			
			// rfocusIndex;
			
			// mx0/my0 are the 0 corner of the blob bounds fit to (rx0, ry0, ...)
			
			final VisualNode node = nodes[rni];
			if (! node.ready) {
				return 0.f;
			}
			final Rectangle2D.Float fb = node.fracBounds[rfocusIndex];
			
			final float[] rbounds = rbounds(node);
			final float rx0 = rbounds[0];
			final float ry0 = rbounds[1];
			final float rx1 = rbounds[2];
			final float ry1 = rbounds[3];
			
			final float mx0 = rx0 + (rx1 - rx0) * fb.x;
			final float my0 = ry0 + (ry1 - ry0) * fb.y;
			
			final float vx0 = (mx0 - rfcx) * rtscale + rfcx;
			final float vy0 = (my0 - rfcy) * rtscale + rfcy;
			final float vx1 = rx0;
			final float vy1 = ry0;
			
			
			
			final float dx = vx1 - vx0;
			final float dy = vy1 - vy0;
			
			final float vx = vx0 + dx * zu;
			final float vy = vy0 + dy * zu;
			
			rftx = (vx - rfcx) - (mx0 - rfcx) * rnscale;
			rfty = (vy - rfcy) - (my0 - rfcy) * rnscale;
			
		}
		else if (1.f < rnscale) {
			// keep the point at (rmx, rmy)  at the same screen location
			//

			rfcx = (rcapmousex * (rpscale - rnscale) + rfcx * (1 - rpscale)) / (1 - rnscale);
			rfcy = (rcapmousey * (rpscale - rnscale) + rfcy * (1 - rpscale)) / (1 - rnscale);
			
//			rfcx *= -1;
//			rfcy *= -1;
			
//			System.out.println(String.format("[%f, %f]", rfcx, rfcy));
		}
		
		return 0;
	}
	
	
	
	private boolean firstRender = true;
	
	private void render(final GL gl) {
		assert SwingUtilities.isEventDispatchThread();

		
		if (firstRender) {
			firstRender = false;
			
			vboSupported = gl.isFunctionAvailable("glGenBuffersARB") &&
	        gl.isFunctionAvailable("glBindBufferARB") &&
	        gl.isFunctionAvailable("glBufferDataARB") &&
	        gl.isFunctionAvailable("glDeleteBuffersARB");
			
			launchInitNode2();
		}
		
		
		
		// TODO: also store edges from different source sseparately
		// TODO: draw word link edges last
		// TODO: zsort color edges using parallel radix
		// TODO: using sum of node weights
		
		++frame;
		
		
//		gscale = 2.f;
		
		
		if (gtransu < 1.f) {
		renderMode = RenderMode.GRAPH;
		}
		else {
			if (rtransu < R_TRANS_CUT || rfocusIndex < 0) {
			syncRIndices();
			}
			
			renderMode = RenderMode.RECURSIVE;
		}
		
		
		switch (renderMode) {
			case GRAPH:
				if (0 < zinc) {
					float remaining = gzoomIn(zinc);
					if (0 < remaining && !(rni < 0 || !nodes[rni].ready)) {
						if (rtransu < R_TRANS_CUT || rfocusIndex < 0) {
							syncRIndices();
							}
							
							renderMode = RenderMode.RECURSIVE;
							rzoomIn(remaining);
						
					}
				}
				else if (zinc < 0) {
					gzoomOut(-zinc);
					if (gtransu <= 0) {
						zinc = 0;
					}
				}
				
				break;
			case RECURSIVE:
				if (rni < 0 || !nodes[rni].ready) {
					break;
				}
				
				if (0 < zinc) {
					rzoomIn(zinc);
				}
				else if (zinc < 0) {
					float remaining = rzoomOut(-zinc);
					if (0 < remaining) {
						gzoomOut(remaining);
					}
				}
				break;
		}
		
		
		
		switch (renderMode) {
			case GRAPH:
				gscale = 
					//0.4f + gtransu * 2.f;
					MIN_GRAPH_SCALE + gtransu * (MAX_GRAPH_SCALE - MIN_GRAPH_SCALE);
				
				renderGraph(gl);
				break;
			case RECURSIVE:
				
				if (rni < 0 || ! nodes[rni].ready) {
					break;
				}

				
				
	//			for (int i = 0; i < nodes.length; ++i)
//				if (null != cnode) {
				rscale = 1.f + rtransu * 1.f;
				//(rtransu - (R_TRANS_CUT - 1.f)) / (1.f - (R_TRANS_CUT - 1.f)) * 1.f;
				
				if (rtransu < R_TRANS_CUT) {
				syncRIndices();
				}
				syncRmm();
				
				
				
				gl.glMatrixMode(GL.GL_MODELVIEW);
				
				
				if (R_TRANS_CUT <= rtransu) {
//					final float rscale2 = 1.f + (rtransu - R_TRANS_CUT) / (1.f - (R_TRANS_CUT - 1.f)) * 1.f;
					
					
					float zu = ZUP((rtransu - R_TRANS_CUT) / (1.f - R_TRANS_CUT));
					
					gl.glPushMatrix();
					
					
//					final VisualNode node = nodes[rni];
//					final Rectangle2D.Float fb = node.fracBounds[rfocusIndex];
//					
//					final float mx0 = rx0 + (rx1 - rx0) * fb.x;
//					final float my0 = ry0 + (ry1 - ry0) * fb.y;
//					
//					final float vx0 = (mx0 - rfcx) * rscale + rfcx + rftx;
//					final float vy0 = (my0 - rfcy) * rscale + rfcy + rfty;
//					
//					gl.glTranslatef(
//						vx0 - rx0,
//						vy0 - ry0,
//						0.f
//					);
//					gl.glScalef(
//							1.f / (1.f + (1 - zu) * ((1.f / node.fracBounds[rfocusIndex].width) - 1)),
//							1.f / (1.f + (1 - zu) * ((1.f / node.fracBounds[rfocusIndex].height) - 1)),
//							1.f
//					);
					
//					gl.glScalef(rscale2, rscale2, 1.f);
					
					// negative trans:
					renderRecursive(gl, unfilteredBlobLinks[rni][rfocusIndex], -1, 
							1 - zu, 
							(float) Math.pow(zu, 0.7f));
					gl.glPopMatrix();
				}
				
				
				gl.glPushMatrix();
				
				assert !Float.isNaN(rfcx);
				assert !Float.isNaN(rfcy);
				assert !Float.isNaN(rftx);
				assert !Float.isNaN(rfty);
				gl.glTranslatef(rfcx + rftx, rfcy + rfty, 0.f);
				gl.glScalef(rscale, rscale, 1.f);
				gl.glTranslatef(-rfcx, -rfcy, 0.f);
				
				if (R_TRANS_CUT <= rtransu) {
					final float zu = ZUP((rtransu - R_TRANS_CUT) / (1.f - R_TRANS_CUT));
					
					// zoom around corner
					// by amount    1 + zu * (r1xy - r0xy) / node.wh
					
					final VisualNode node = nodes[rni];
					final Rectangle2D.Float fb = node.fracBounds[rfocusIndex];
					
					final float[] rbounds = rbounds(node);
							//nodes[unfilteredBlobLinks[rni][rfocusIndex]]);
					final float rx0 = rbounds[0];
					final float ry0 = rbounds[1];
					final float rx1 = rbounds[2];
					final float ry1 = rbounds[3];
					
					final float mx0 = rx0 + (rx1 - rx0) * fb.x;
					final float my0 = ry0 + (ry1 - ry0) * fb.y;
					
					
					gl.glTranslatef(mx0, my0, 0.f);
					gl.glScalef(
							(1.f + zu * ((1.f / node.fracBounds[rfocusIndex].width) / rscale - 1)),
							(1.f + zu * ((1.f / node.fracBounds[rfocusIndex].height) / rscale - 1)),
							1.f
					);
					gl.glTranslatef(-mx0, -my0, 0.f);
				}
				renderRecursive(gl, rni, -1, rtransu, 
						1.f);
				gl.glPopMatrix();
				
				renderRText(gl, rni, (float) Math.pow(1.f - rtransu, 0.7));
				renderTopText(gl, rni);
				
				
				
				
				
//				}
				break;
		}
	}

	
	
	// recursive node index
//	private int rni = 0;
	
	
//	private float rx0 = -256.f;
//	private float ry0 = -256.f;
//	private float rx1 = 256.f;
//	private float ry1 = 256.f;
	
	
	private float[] rbounds(final VisualNode node) {
		// ffit within a 512x512 box
		
		final float f = 0.9f;
		final float maxw = width * f;
		final float maxh = height * f;
		
		final float d = maxw < maxh ? maxw : maxh;
		
//		final float sx = maxw / node.width;
//		final float sy = maxh / node.height;
//		
//		final float s = sx < sy ? sx : sy;
//		
//		final float w = s * node.width;
//		final float h = s * node.height;
		
//		return new float[]{-w / 2.f, -h / 2.f, w / 2.f, h / 2.f};
		return new float[]{-d / 2.f, -d / 2.f, d / 2.f, d / 2.f};
	}
	
	
	
	
	// rendering strategy:
	// for each layer, render
	// blobs at first encounter (index 0)
	// this is due to the cost of binding the buffer -- only want to do it once
	//
	// the same rendering for both branches!
	// 
	
	
	// SSSSSSSSSTTTTTTTTTAAAAAAAAAATTTTTTTTTTTEEEEEEEEEEE
	// - blob of focus
	// - transition u
	
	
	
	private void renderRecursive(final GL gl, final int id, final int focusIndex, 
			final float transu, final float malpham) {
//		rtransu = 0.8f;
		
		
		if (id < 0 || !nodes[id].ready) {
			return;
		}
		
		// center in screen
		// TODO: TEST: use a stencil buffer to mask the center
		
		
		// Build up each layer.
		// we keep a "mix color" going forward, along with how much to mix.
		// 
		// also, there is the blob of focus
		// render outside the blob with less opac as we zoom in
		// render the under piece with increasing opac as we zoom in (use a NOT EQUAL test
		//     instead of an EQUAL test)
		//
		// 

		// the stencil buffer has 20 bit res
		// each off TOP or BOTTOM is [2 bit depth][8 bit index]
		
		
		// a list of DEPTH int[] links
		// and an index into each
		// pull the links from unfiltered links
		// 
		// at DEPTH D, use top if 0 == D % 2
		//
		// 
		
		
//		if (rni < 0 || !nodes[rni].ready) {
//			return;
//		}
		
		
//		final int TOP_MASK = 0x000FFC00;
//		final int BOT_MASK = 0x000003FF;
		
		
		
		
		VisualNode pnode = null;
		
		final int DEPTH = 1;
		
		final float[] alphas = {1.f, 0.1f, 0.05f};
		
		final float[][] components = new float[DEPTH][3];
		final int[][] links = new int[DEPTH][];
		final int[] indices = new int[DEPTH];
		final VisualNode[] fnodes = new VisualNode[DEPTH];
		// thanks to jvm for initializing to 0
		
		final int[] fref = new int[DEPTH];
		
		// successive bounds
		// nodes store bounds in abs and fractional
		// use fractional to derive next bounds
		final Rectangle2D.Float[] bounds = new Rectangle2D.Float[DEPTH];
		for (int i = 0; i < DEPTH; ++i) {
			bounds[i] = new Rectangle2D.Float();
		}
		
		final float[] rbounds = rbounds(nodes[id]);
		final float rx0 = rbounds[0];
		final float ry0 = rbounds[1];
		final float rx1 = rbounds[2];
		final float ry1 = rbounds[3];
		
		// TODO: initialize bottom links
		bounds[0].x = rx0;
		bounds[0].y = ry0;
		bounds[0].width = rx1 - rx0;
		bounds[0].height = ry1 - ry0;
//		bounds[0].x = -width / 2.f;
//		bounds[0].y = -height / 2.f;
//		bounds[0].width = width;
//		bounds[0].height = height;
		
		
		fnodes[0] = nodes[id];
		links[0] = unfilteredBlobLinks[id];
		
		// the initial stencil buffer:
		fref[0] = 0;
		

		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnable(GL.GL_STENCIL_TEST);
		
		

		
		// ADVANCE NOISE:
		fnodes[0].noise.advance();
		
		
		int d = 0;
		while (indices[d] < links[d].length) {
			// set up stencil for current blob
			
			// while no more remaining index at d {
			// --d,
			// increment INDEX[d]
			// }
			
			
			// if d == DEPTH - 1, 
			// then render the node at link[INDEX[d]]
			// in the current stencil
			// increment INDEX[d]
			
			// if d < DEPTH - 1,
			// set links at [d + 1]
			// set bounds at [d + 1]
			// increment d
			
			final int index = indices[d];
			
			
			
			
			if (DEPTH - 1 == d && 0 == index) {
//				gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
				
				// render all blobs up to the present,
				// then render 
				
				gl.glColorMask(false, false, false, false);
//				final boolean bot = 0 == d % 2;
				
				gl.glStencilFunc(GL.GL_ALWAYS, DEPTH == 2 ? 0x00 : 0x80, 
						0xFF);
				gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_REPLACE);
				
				{
					final Rectangle2D.Float b = bounds[d];
					gl.glBegin(GL.GL_QUADS);
						gl.glVertex2f(b.x, b.y);
						gl.glVertex2f(b.x + b.width, b.y);
						gl.glVertex2f(b.x + b.width, b.y + b.height);
						gl.glVertex2f(b.x, b.y + b.height);
					gl.glEnd();
				}
				
				
				
				
				
				if (2 == DEPTH) {
					gl.glMatrixMode(GL.GL_MODELVIEW);
					gl.glPushMatrix();
					
					final VisualNode node = fnodes[0];
					
					// render into bounds
					final Rectangle2D.Float b = bounds[0];
					
					gl.glTranslatef(b.x, b.y, 0.f);
					gl.glScalef(b.width / node.width,
							b.height / node.height, 1.f);
					
					
					if (node.vbo < 0 && vboSupported) {
						final int[] _vbo = {-1};
						
						gl.glGenBuffersARB(1, _vbo, 0);
						// TODO: check error state
						if (0 <= _vbo[0]) {
			            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
			            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, node.buffer.limit() * BufferUtil.SIZEOF_FLOAT, 
			            		node.buffer, GL.GL_STATIC_DRAW_ARB);

			            node.vbo = _vbo[0];
						node.buffer = null;
						}
					}
		            
		            if (0 <= node.vbo) {
		                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, node.vbo);
		                gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
		            } else {
		                gl.glVertexPointer(2, GL.GL_FLOAT, 0, node.buffer);
		            }
		            
		            final int j = indices[0];
		            
		           
						if (node.empty[j]) {
							continue;
						}
						
			            
			           
						if (node.ivbo[j] < 0 && vboSupported) {
							final int[] _vbo = {-1};
							
							gl.glGenBuffersARB(1, _vbo, 0);
							// TODO: check error state
							if (0 <= _vbo[0]) {
				            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, _vbo[0]);
				            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER, 
				            		node.indices[j].limit() * BufferUtil.SIZEOF_SHORT, 
				            		node.indices[j], GL.GL_STATIC_READ_ARB);
			
				            node.ivbo[j] = _vbo[0];
							node.indices[j] = null;
							}
						}
					
			            if (0 <= node.ivbo[j]) {
			            	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, node.ivbo[j]);
			            }
			            
			            
			            
			            int ref = 0x80;
						
						
						gl.glStencilFunc(GL.GL_ALWAYS, ref, 
								0xFF);
						gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_REPLACE);
			            
			            
			            if (0 <= node.ivbo[j]) {
			            gl.glDrawRangeElements(GL.GL_TRIANGLES, 
			            		node.start[j], node.end[j],
			            		node.icount[j], 
			            		GL.GL_UNSIGNED_SHORT,
			            		0);
			            }
			            else {
			            	gl.glDrawElements(GL.GL_TRIANGLES, node.icount[j], GL.GL_UNSIGNED_SHORT,
				            		node.indices[j]);
			            }
			            
					
					gl.glPopMatrix();
					}
				
				
				
				
				{
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glPushMatrix();
				
				final VisualNode node = fnodes[d];
				
				// render into bounds
				final Rectangle2D.Float b = bounds[d];
				
				gl.glTranslatef(b.x, b.y, 0.f);
				gl.glScalef(b.width / node.width,
						b.height / node.height, 1.f);
				
				
				if (node.vbo < 0 && vboSupported) {
					final int[] _vbo = {-1};
					
					gl.glGenBuffersARB(1, _vbo, 0);
					// TODO: check error state
					if (0 <= _vbo[0]) {
		            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
		            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, node.buffer.limit() * BufferUtil.SIZEOF_FLOAT, 
		            		node.buffer, GL.GL_STATIC_DRAW_ARB);

		            node.vbo = _vbo[0];
					node.buffer = null;
					}
				}
	            
	            if (0 <= node.vbo) {
	                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, node.vbo);
	                gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
	            } else {
	                gl.glVertexPointer(2, GL.GL_FLOAT, 0, node.buffer);
	            }
	            
	           
				for (int j = 0; j < node.blobCount; ++j) {
					if (node.empty[j]) {
						continue;
					}
					
		            
		           
					if (node.ivbo[j] < 0 && vboSupported) {
						final int[] _vbo = {-1};
						
						gl.glGenBuffersARB(1, _vbo, 0);
						// TODO: check error state
						if (0 <= _vbo[0]) {
			            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, _vbo[0]);
			            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER, 
			            		node.indices[j].limit() * BufferUtil.SIZEOF_SHORT, 
			            		node.indices[j], GL.GL_STATIC_READ_ARB);
		
			            node.ivbo[j] = _vbo[0];
						node.indices[j] = null;
						}
					}
				
		            if (0 <= node.ivbo[j]) {
		            	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, node.ivbo[j]);
		            }
		            
		            
		            
		            int ref = 0x80 | j;
					
//					gl.glStencilFunc(GL.GL_EQUAL, ref, 
//							0x80);
					gl.glStencilFunc(GL.GL_ALWAYS, ref, 
							0x80);
					gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_REPLACE);
		            
		            
		            if (0 <= node.ivbo[j]) {
		            gl.glDrawRangeElements(GL.GL_TRIANGLES, 
		            		node.start[j], node.end[j],
		            		node.icount[j], 
		            		GL.GL_UNSIGNED_SHORT,
		            		0);
		            }
		            else {
		            	gl.glDrawElements(GL.GL_TRIANGLES, node.icount[j], GL.GL_UNSIGNED_SHORT,
			            		node.indices[j]);
		            }
		            
				}
				
				gl.glPopMatrix();
				}
				
				
				
				gl.glColorMask(true, true, true, true);
			}
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			if (DEPTH - 1 == d) {
				
				
				
				++indices[d];
				final int ni = links[d][index];
				
				
				if (0 <= ni && nodes[ni].ready) {
					final VisualNode node = nodes[ni];
					

					final float x0;
					final float y0;
					final float x1;
					final float y1;
					
					{
					final Rectangle2D.Float fracb = fnodes[d].fracBounds[index];
					
					final Rectangle2D.Float b = bounds[d];
					
					
					x0 = b.x + fracb.x * b.width;
					y0 = b.y + fracb.y * b.height;
					x1 = x0 + b.width * fracb.width;
					y1 = y0 + b.height * fracb.height;
					}
					
					
					int ref = 0x80 | index;
					
					
					// stencil in the 
//					gl.glEnable(GL.GL_STENCIL_TEST);
//					gl.glStencilFunc(GL.GL_EQUAL, 777, 
//							0xFFFFFFFF);
//					ref & (bot ? BOT_MASK : TOP_MASK)
					gl.glStencilFunc(GL.GL_EQUAL, ref, 
							0xFF);
//					gl.glStencilFunc(GL.GL_ALWAYS, ref, 
//							0xFF);
					gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
					
					
					if (null == node.tx) {
						initTexture(ni, node);
					}
					
					       
					
					final TextureCoords tc = node.tx.getImageTexCoords();
					
					if (null == pnode || pnode.tx.getTarget() != node.tx.getTarget()) {
						if (null != pnode) {
							pnode.tx.disable();
						}
						
						node.tx.enable();
						
//						gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_BLEND);
						gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
					}

					node.tx.bind();
					
					

					final int rgb = fnodes[d].blobReprs[index];
					
					final float r = components[d][0] +
					alphas[d] * 
					(((rgb >> 16) & 0xFF) / 255.f - components[d][0]);
					final float g =
					components[d][1] +
				alphas[d] * 
					(((rgb >> 8) & 0xFF) / 255.f - components[d][1]);
					final float b =
					components[d][2] +
				alphas[d] * 
					(((rgb) & 0xFF) / 255.f - components[d][2]);
					
					
					final float alpham = malpham * (
							index == focusIndex 
								? 1.f
								: (0.3f + 0.7f * (float) Math.pow((1.f - transu), 0.2))
						) *
						(0.2f + 0.8f * fnodes[0].noise.noise[index][0]);
					
					
					
					
					gl.glEnable(GL.GL_BLEND);
	    			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	    			
					
					gl.glColor4f(
						r, g, b,				
						alpham
					);
					
					
					
					
//					assert -350 <= x0;
//					assert -350 <= y0;
//					assert x1 <= 350;
//					assert y1 <= 350;
					
					gl.glBegin(GL.GL_QUADS);
					
					gl.glTexCoord2f(tc.left(), tc.top());
					gl.glVertex2f(x0, y0);
					
					gl.glTexCoord2f(tc.right(), tc.top());
					gl.glVertex2f(x1, y0);
					
					gl.glTexCoord2f(tc.right(), tc.bottom());
					gl.glVertex2f(x1, y1);
					
					gl.glTexCoord2f(tc.left(), tc.bottom());
					gl.glVertex2f(x0, y1);
					
					gl.glEnd();
					
					
					
					
	    			
	    			gl.glColor4f(
							r, g, b,				
							alpham * 0.9f
						);
	    			
	    			gl.glBegin(GL.GL_QUADS);
					
					gl.glVertex2f(x0, y0);
					
					gl.glVertex2f(x1, y0);
					
					gl.glVertex2f(x1, y1);
					
					gl.glVertex2f(x0, y1);
					
					gl.glEnd();
	    			
	    			gl.glDisable(GL.GL_BLEND);
					
					
					pnode = node;
				}
				
			}
			else {
				assert d < DEPTH - 1;
				
				// set links at [d + 1]
	  			// set bounds at [d + 1]
	  			// increment d
				
				final int ni = links[d][index];
				if (ni < 0 || !nodes[ni].ready) {
					++indices[d];
				}
				else {
				final Rectangle2D.Float fracb = fnodes[d].fracBounds[index];
				final Rectangle2D.Float b = bounds[d];
				
				final int nd = d + 1;
				fnodes[nd] = nodes[ni];
				links[nd] = unfilteredBlobLinks[ni];
				indices[nd] = 0;
				final int rgb = fnodes[d].blobReprs[index];
				components[nd][0] = components[d][0];
				components[nd][1] = components[d][1];
				components[nd][2] = components[d][2];
				
				components[nd][0] += alphas[d] * 
					(((rgb >> 16) & 0xFF) / 255.f - components[d][0]);
				components[nd][1] += alphas[d] * 
					(((rgb >> 8) & 0xFF) / 255.f - components[d][1]);
				components[nd][2] += alphas[d] * 
					(((rgb) & 0xFF) / 255.f - components[d][2]);
		
				
				bounds[nd].x = b.x + fracb.x * b.width;
				bounds[nd].y = b.y + fracb.y * b.height;
				bounds[nd].width = b.width * fracb.width;
				bounds[nd].height = b.height * fracb.height;
				
//				assert -350 <= bounds[nd].x;
//				assert -350 <= bounds[nd].y;
//				assert bounds[nd].x + bounds[nd].width <= 350;
//				assert bounds[nd].y + bounds[nd].height <= 350;
				
				
				// RENDER INTO THE STENCIL BUFFER,
				// FOR BLOB "index"
				// USING THE PARENT SIG
				
				// the initial buffer is all zero
				// indices are shifted by one in the ref,
				// so they start at one (never use 0)
				
				
				final boolean bot = 0 == d % 2;
				int ref;
				if (bot) {
					ref = ((d & 0x3) << 8) | (((index + 1) & 0xFF));
				}
				else {
					ref = (((d & 0x3) << 8) | ((index + 1) & 0xFF)) << 10;
				}
				
				ref |= fref[d];
				
				fref[nd] = ref;
				
				// TODO: draw BLOB[index]
				
				
				
				d = nd;
				}
			}
			
			
			
			
			while (0 < d && links[d].length <= indices[d]) {
				--d;
				++indices[d];
			}
		}
		
		
		
		if (null != pnode) {
			pnode.tx.disable();
		}
		
		
		
		gl.glDisable(GL.GL_STENCIL_TEST);
		
		
		// Draw the outlines:
		final VisualNode node = fnodes[0];
		
		
		
		// TODO:
		final float outlineAlpha = 0.8f * malpham * 
		(0.3f + 0.7f * (float) Math.pow((1.f - transu), 2.0));
		
		if (outlineAlpha < 1) {
    		gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    	}
		
		gl.glPushMatrix();
		
		gl.glTranslatef(bounds[0].x, bounds[0].y, 0.f);
		gl.glScalef(bounds[0].width / node.width,
				bounds[0].height / node.height, 1.f);
		
		
		
		if (node.outlineVbo < 0 && vboSupported) {
			final int[] _vbo = {-1};
			
			gl.glGenBuffersARB(1, _vbo, 0);
			// TODO: check error state
			if (0 <= _vbo[0]) {
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, node.outlineBuffer.limit() * BufferUtil.SIZEOF_FLOAT, 
            		node.outlineBuffer, GL.GL_STATIC_DRAW_ARB);

            node.outlineVbo = _vbo[0];
			node.outlineBuffer = null;
			}
		}
		
		if (0 <= node.outlineVbo) {
            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, node.outlineVbo);
            gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);		// Set The Vertex Pointer To The Vertex Buffer
        } else {
            gl.glVertexPointer(2, GL.GL_FLOAT, 0, node.outlineBuffer); // Set The Vertex Pointer To Our Vertex Data
        }
         
		for (int j = 0; j < node.blobCount; ++j) {
			if (node.empty[j]) {
				continue;
			}
				            
            
			
			if (node.outlineIVbo[j] < 0 && vboSupported) {
				final int[] _vbo = {-1};
				
				gl.glGenBuffersARB(1, _vbo, 0);
				// TODO: check error state
				if (0 <= _vbo[0]) {
				gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, _vbo[0]);
	            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER, 
	            		node.outlineIndices[j].limit() * BufferUtil.SIZEOF_SHORT, 
	            		node.outlineIndices[j], GL.GL_STATIC_READ_ARB);

	            node.outlineIVbo[j] = _vbo[0];
				node.outlineIndices[j] = null;
				}
			}
            
			
            if (0 <= node.outlineIVbo[j]) {
            	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, node.outlineIVbo[j]);
            }
            
            
            
            final int rgb = node.blobReprs[j];
            gl.glColor4f(
        		((rgb >> 16) & 0xFF) / (float) 255,
        		((rgb >> 8) & 0xFF) / (float) 255,
        		(rgb & 0xFF) / (float) 255, 
        		outlineAlpha
    		);
            
            
            final float w = 
            	0.5f * (0.5f + 3.f * node.noise.noise[j][0]);
            
            // TODO:
            gl.glLineWidth(w < 1.f ? 1.f : w);
            
            if (0 <= node.outlineIVbo[j]) {
	            gl.glDrawRangeElements(GL.GL_LINES, 
	            		node.outlineStart[j], node.outlineEnd[j],	
	            		node.outlineCounts[j], 
            		GL.GL_UNSIGNED_SHORT,
            		0);
            }
            else {
            	gl.glDrawElements(GL.GL_LINES, node.outlineCounts[j], 
            			GL.GL_UNSIGNED_SHORT,
	            		node.outlineIndices[j]);
            }	            

//            final int error = gl.glGetError();
//            assert 0 == error : error + ": " + glu.gluGetString(error);
		}
		
		
		gl.glPopMatrix();
		
		
		if (outlineAlpha < 1) {
    		gl.glDisable(GL.GL_BLEND);
    	}
		
		
		
		if (vboSupported) {
			// Unbind:
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

        // Disable Pointers
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);

		
		
		// TODO: need to work out matrices
		// TODO: 
		
		
		
		
	}
	
	
	private void renderGraph(final GL gl) {
		
		

//		qscb.gl = gl;
		
		
		// TODO: mark nodes with distance to mouse
		// TODO: compute multipliers per node
		// TODO: each edge has thickness node0.m * node1.m * tf.VALUE
		// TODO: e.g. set thickness multiplier in tf
		
		for (VisualNode node : nodes) {
			final int id = node.id;
			
			final float dx = gmousex - node.cx;
			final float dy = gmousey - node.cy;
			final float dsq = dx * dx + dy * dy;
			final float dsqsq = dsq * dsq;
			final float t = 1 - 0.0000500013f * dsq + 
			1.25006e-9f * dsqsq
			- 2.08349e-14f * dsqsq * dsq;
			
			final float mm0 = t < 0.f ? 0.f : 1 < t ? 1.f : t;
			
			if (gni == id ) {
				final float mm1 = 1.f;
				
				node.mm = (float) Math.pow(mm0 + (mm1 - mm0) * gtransu, 0.3f);
			}
			else {
				final float mm1 = 0.f;
				
				node.mm = mm0 + (mm1 - mm0) * (float) Math.pow(gtransu, 1.7);
			}
		}
		
		
//		final int seN = sigEdges.length;
//		for (int i = 0; i < seN; ++i) {
//			final VisualEdge edge = sigEdges[i];
//			sortWeights[i] = nodes[edge.id0].mm;
//			//+ nodes[edge.id1].mm;
//			sortIndices[i] = i;
//		}
//		edgePls.sort(sortWeights, sortIndices, seN);
		
		for (int i = 0; i < nodes.length; ++i) {
			final VisualNode node = nodes[i];
			sortWeights[i] = node.mm;
			sortIndices[i] = i;
		}
		pls.sort(sortWeights, sortIndices, nodes.length);
		
//		gl.glLineWidth(1.f);
		
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		
		gl.glScalef(gscale, gscale, 1.f);
		gl.glTranslatef(-gcx, -gcy, 0.f);
//		gl.glTranslatef(-cx, -cy, 0.f);
		
		renderBlobEdges(gl);

		renderNodes(gl, false, false);
		renderWordEdges(gl, false, false);

		renderNodes(gl, true, false);
		renderWordEdges(gl, true, false);
		
		renderNodes(gl, false, true);
		
		renderWordEdges(gl, false, true);
		
		gl.glPopMatrix();
		
		
		final int error = gl.glGetError();
		if (0 != error) {
			System.out.println(error);
		}
		renderText(gl, sortIndices[nodes.length - 1]);		
		renderTopText(gl, sortIndices[nodes.length - 1]);
		
		
		/*
		// SPeed test:
		// test every poly for containment of mouse ...
		final VisualNode node = nodes[sortIndices[nodes.length - 1]];
		if (node.ready) {
		for (int i = 0; i < node.blobCount; ++i) {
			final Polygon poly = node.polys[i];
			
			poly.contains(mousex, mousey);
		}
		}
		*/
	}
	
	
	
	final float MIN_ACTIVE_MM = 0.3f;
	
	
	private void renderRText(final GL gl, final int fid, final float malpham) {
		final VisualNode fnode = nodes[fid];
		
		final float[] rbounds = rbounds(fnode);
		final float rx0 = rbounds[0];
		final float ry0 = rbounds[1];
		final float rx1 = rbounds[2];
		final float ry1 = rbounds[3];
		
		
		
		
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		
		
//		gl.glColor4f(1.f, 0.f, 0.f, 1.f);
//		gl.glPointSize(10.f);
//		gl.glBegin(GL.GL_POINTS);
//		gl.glVertex2f(rmousex, rmousey);
//		gl.glEnd();
		
		
		
		for (int i = 0; i < fnode.blobCount; ++i) {
			final int id = unfilteredBlobLinks[fid][i];
//			final VisualNode node = nodes[id];
			
			if (fnode.mms[i] < MIN_ACTIVE_MM) {
				continue;
			}
			if (null == titles[id]) {
				continue;
			}
			
			
			final Rectangle2D.Float fb = fnode.fracBounds[i];
			
			final float x0 = rx0 + (rx1 - rx0) * fb.x;
			final float y0 = ry0 + (ry1 - ry0) * fb.y;
			
			final float x1 = rx0 + (rx1 - rx0) * (fb.x + fb.width);
			final float y1 = ry0 + (ry1 - ry0) * (fb.y + fb.height);
			
			final float x = (x0 + x1) / 2.f;
			final float y = (y0 + y1) / 2.f;
			
			
			
			final float tu = (float) Math.pow((1.f - fnode.mms[i]), 0.6);
			final float t = RAD_TO_DEG * tu * _2_PI * 2.f * (fnode.noise.noise[i][0] - 0.5f);
			gl.glLoadIdentity();
			
			gl.glTranslatef(0.f, height / 2.f, 0.f);
	        gl.glScalef(1.f, -1.f, 1.f);
	        gl.glTranslatef(0.f, -height / 2.f, 0.f);
	        
	        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
			
			gl.glTranslatef(rfcx + rftx, (rfcy + rfty), 0.f);
			gl.glScalef(rscale, rscale, 1.f);
			gl.glTranslatef(-rfcx, -rfcy, 0.f);
			
			gl.glTranslatef(
					x, y,
					0.f);
			gl.glRotatef(t, 0, 0, 1.f);
		
	        gl.glScalef(1.f, -1.f, 1.f);
	        
	        
	        final int rgb = fnode.blobReprs[i];
			
			gl.glColor4f(
					((rgb >> 16) & 0xFF) / 255.f,
					((rgb >> 8) & 0xFF) / 255.f,
					(rgb & 0xFF) / 255.f,
					malpham * 0.45f * fnode.mms[i]);
//			gl.glColor4f(0.65f, 0.65f, 0.88f, malpham * 0.3f * fnode.mms[i]);
			
			final float bw = branchWidths[id];
			final float bh = branchHeights[id];
			
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex2f(-2.f, -bh / 2.f + 4.f);
			gl.glVertex2f(bw + 2.f, -bh / 2.f + 4.f);
			gl.glVertex2f(bw + 2.f, bh / 2.f + 4.f);
			gl.glVertex2f(-2.f, bh / 2.f + 4.f);
			gl.glEnd();
		}
		
		
		
		
		branchRenderer.beginRendering(width, height, true);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		
		
		for (int i = 0; i < fnode.blobCount; ++i) {
			final int id = unfilteredBlobLinks[fid][i];
//			final VisualNode node = nodes[id];
			
			if (fnode.mms[i] < MIN_ACTIVE_MM) {
				continue;
			}
			if (null == titles[id]) {
				continue;
			}
			
//			final int rgb = fnode.blobReprs[i];
			
//			branchRenderer.setColor(
//					((rgb >> 16) & 0xFF) / 255.f,
//					((rgb >> 8) & 0xFF) / 255.f,
//					(rgb & 0xFF) / 255.f,
//					malpham * 0.3f * fnode.mms[i]);
			branchRenderer.setColor(0.1f, 0.1f, 0.1f, malpham * 0.8f * fnode.mms[i]);
			
			final Rectangle2D.Float fb = fnode.fracBounds[i];
			
			final float x0 = rx0 + (rx1 - rx0) * fb.x;
			final float y0 = ry0 + (ry1 - ry0) * fb.y;
			
			final float x1 = rx0 + (rx1 - rx0) * (fb.x + fb.width);
			final float y1 = ry0 + (ry1 - ry0) * (fb.y + fb.height);
			
			final float x = (x0 + x1) / 2.f;
			final float y = (y0 + y1) / 2.f;
			
			
			
			final float tu = (float) Math.pow((1.f - fnode.mms[i]), 0.6);
			final float t = RAD_TO_DEG * tu * _2_PI * 2.f * (fnode.noise.noise[i][0] - 0.5f);
			gl.glLoadIdentity();
			
			gl.glTranslatef(0.f, height / 2.f, 0.f);
	        gl.glScalef(1.f, -1.f, 1.f);
	        gl.glTranslatef(0.f, -height / 2.f, 0.f);
	        
	        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
			
			gl.glTranslatef(rfcx + rftx, rfcy + rfty, 0.f);
			gl.glScalef(rscale, rscale, 1.f);
			gl.glTranslatef(-rfcx, -rfcy, 0.f);
			
			gl.glTranslatef(
					x, y,
					0.f);
			gl.glRotatef(t, 0, 0, 1.f);
		
	        gl.glScalef(1.f, -1.f, 1.f);
			
			final String btitle = titles[id];
			
			branchRenderer.draw(btitle, 
					0, 0);
			branchRenderer.flush();
		}
		
		branchRenderer.endRendering();
		
		
		
		gl.glPopMatrix();
		
		
		gl.glDisable(GL.GL_BLEND);
	}
	
	
	private void renderTopText(final GL gl, final int id) {
		final VisualNode node = nodes[id];
		
		if (node.mm < MIN_ACTIVE_MM) {
			return;
		}
		if (null == titles[id]) {
			return;
		}
		
		final String title = titles[id];
		
		final float twidth = titleWidths[id];
		final float theight = titleHeights[id];
//		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		
		
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
gl.glEnable(GL.GL_BLEND);
		
		// enable blend
		 gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//		gl.glColor4f(1.f, 1.f, 0.f, 0.3f * node.mm);
		gl.glColor4f(0.0f, 0.0f, 0.0f, 0.6f * node.mm);

			gl.glLoadIdentity();
			gl.glTranslatef(8.f, 16.f, 0.f);
		
			
			
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex2f(-2.f, -16.f - 2);
			gl.glVertex2f(twidth + 2.f, -16.f - 2);
			gl.glVertex2f(twidth + 2.f, theight - 16.f + 2);
			gl.glVertex2f(-2.f, theight - 16.f + 2);
			gl.glEnd();

		
		
		
		
		
		
		titleRenderer.beginRendering(width, height, true);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
//		gl.glTranslatef(0.f, height / 2.f, 0.f);
//        gl.glScalef(1.f, -1.f, 1.f);
//        gl.glTranslatef(0.f, -height / 2.f, 0.f);
        
		
//		gl.glPushMatrix();
//		
//		gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
//        gl.glScalef(1.2f, 1.2f, 1.f);
//        gl.glTranslatef(-width / 2.f, -height / 2.f, 0.f);
//		
//        gl.glTranslatef(width / 2.f - twidth / 2.f, height / 2.f - theight / 2.f, 0.f);
//        
//        
//        
//		
//		titleRenderer.setColor(0.f, 0.f, 0.f, 0.3f * node.mm);
////		textRenderer.setColor(
////				((rgb >> 16) & 0xFF) / (float) 255, 
////				((rgb >> 8) & 0xFF) / (float) 255, 
////				(rgb & 0xFF) / (float) 255, 
////				0.5f * node.mm);
//		titleRenderer.draw(String.valueOf(packet.linkedItems[id][1]), 
//				0, 0);
//		
//		gl.glPopMatrix();
		
		
//        gl.glTranslatef(width / 2.f - twidth / 2.f, height / 2.f - theight / 2.f, 0.f);
        
        
        
//		final int rgb = node.repr;
		
        /*
        gl.glColor4f(0.f, 0.f, 0.f, 0.3f * node.mm);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(0.f, 0.f);
        gl.glVertex2f(twidth, 0.f);
        gl.glVertex2f(twidth, theight);
        gl.glVertex2f(0.f, theight);
        gl.glEnd();
        */
		
		gl.glTranslatef(8.f, 16.f, 0.f);
        
//		titleRenderer.setColor(1.f, 1.f, 0.f, 0.4f * node.mm);
		titleRenderer.setColor(1.f, 1.f, 0.f, 0.8f * node.mm);
//		titleRenderer.setColor(
//				((rgb >> 16) & 0xFF) / (float) 255, 
//				((rgb >> 8) & 0xFF) / (float) 255, 
//				(rgb & 0xFF) / (float) 255, 
//				0.6f * node.mm);
		titleRenderer.draw(title, 
				0, 0);
		titleRenderer.flush();
		
		titleRenderer.endRendering();

		gl.glPopMatrix();
	}
	
	private void renderText(final GL gl, final int id) {
		
		final VisualNode node = nodes[id];
		
		if (node.mm < MIN_ACTIVE_MM) {
			return;
		}
		if (null == titles[id]) {
			return;
		}
		


		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
gl.glEnable(GL.GL_BLEND);
		
		// enable blend
		 gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//		gl.glColor4f(1.f, 1.f, 0.f, 0.3f * node.mm);
		gl.glColor4f(0.65f, 0.65f, 0.88f, 0.3f * node.mm);
		for (VisualEdge edge : wordEdges) {
			if (edge.id0 != id) {
				continue;
			}
			
			final float t = RAD_TO_DEG * (float) Math.atan2(edge.bounds[7] - edge.bounds[1],
					edge.bounds[6] - edge.bounds[0]);
			gl.glLoadIdentity();
			
			gl.glTranslatef(0.f, height / 2.f, 0.f);
	        gl.glScalef(1.f, -1.f, 1.f);
	        gl.glTranslatef(0.f, -height / 2.f, 0.f);
	        
	        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
			
			gl.glScalef(gscale, gscale, 1.f);
			gl.glTranslatef(-gcx, -gcy, 0.f);
			
			gl.glTranslatef(
				edge.bounds[0] + (edge.n0 + branchHeights[edge.id1]  / 2.f) * edge.v[0] + 5 * edge.u[0], 
				edge.bounds[1] + (edge.n0 + branchHeights[edge.id1]  / 2.f)* edge.v[1] + 5 * edge.u[1], 
					0.f);
			gl.glRotatef(t, 0, 0, 1.f);
		
			
			gl.glScalef(1.f, -1.f, 1.f);
			
			
			final float bw = branchWidths[edge.id1];
			final float bh = branchHeights[edge.id1];
			
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex2f(-2.f, -bh / 2.f + 4.f);
			gl.glVertex2f(bw + 2.f, -bh / 2.f + 4.f);
			gl.glVertex2f(bw + 2.f, bh / 2.f + 4.f);
			gl.glVertex2f(-2.f, bh / 2.f + 4.f);
			gl.glEnd();
		}
		gl.glDisable(GL.GL_BLEND);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		

		gl.glLoadIdentity();
		
		// Edges:
		branchRenderer.beginRendering(width, height, true);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		branchRenderer.setColor(0.1f, 0.1f, 0.1f, 0.7f * node.mm);
		
		for (VisualEdge edge : wordEdges) {
			if (edge.id0 != id) {
				continue;
			}
			
			final float t = RAD_TO_DEG * (float) Math.atan2(edge.bounds[7] - edge.bounds[1],
					edge.bounds[6] - edge.bounds[0]);
			gl.glLoadIdentity();
			
			gl.glTranslatef(0.f, height / 2.f, 0.f);
	        gl.glScalef(1.f, -1.f, 1.f);
	        gl.glTranslatef(0.f, -height / 2.f, 0.f);
	        
	        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
			
			gl.glScalef(gscale, gscale, 1.f);
			gl.glTranslatef(-gcx, -gcy, 0.f);
			
			gl.glTranslatef(
				edge.bounds[0] + (edge.n0 + branchHeights[edge.id1]  / 2.f) * edge.v[0] + 5 * edge.u[0], 
				edge.bounds[1] + (edge.n0 + branchHeights[edge.id1]  / 2.f)* edge.v[1] + 5 * edge.u[1], 
					0.f);
			gl.glRotatef(t, 0, 0, 1.f);
		
			gl.glScalef(1.f, -1.f, 1.f);
			
			final String btitle = titles[edge.id1];
			
			branchRenderer.draw(btitle, 
					0, 0);
			branchRenderer.flush();
		}
		
		branchRenderer.endRendering();
		
		gl.glPopMatrix();
	}
	
	
//	private int beivbo = -1;
//	private int beindexCount;
//	private ShortBuffer beindices; 
//	
//	{
//		// N - 1 quads
//		beindexCount = 4 * (N - 1);
//		final short[] abeindices = new short[beindexCount];
//		for (int i = 0, k = 0; i + 1 < N; k += 2, ++i) {
//			final int j = i << 2;
//			abeindices[j] = (short) k;
//			abeindices[j + 1] = (short) (k + 1);
//			abeindices[j + 2] = (short) (k + 2);
//			abeindices[j + 3] = (short) (k + 3);
//		}
//		
//		beindices = BufferUtil.newShortBuffer(abeindices.length);
//		beindices.put(abeindices);
//		beindices.flip();
//		
//	}
	
	
	private void renderBlobEdges(final GL gl) {
		gl.glPointSize(6.f);
		
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

		
		
		
//		if (beivbo < 0 && vboSupported) {
//			final int[] _vbo = {-1};
//			
//			gl.glGenBuffersARB(1, _vbo, 0);
//			// TODO: check error state
//			if (0 <= _vbo[0]) {
//            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
//            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, beindices.limit() * BufferUtil.SIZEOF_FLOAT, 
//            		beindices, GL.GL_STATIC_DRAW_ARB);
//
//            beivbo = _vbo[0];
//			beindices = null;
//			}
//		}
//		
//		if (0 <= beivbo) {
//        	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, beivbo);
//        }
		
		
		for (int si = 0; si < nodes.length; ++si) {
			final int ni = sortIndices[si];
			final VisualNode node = nodes[ni];
			
			
//			final float n0 = 25 * node.ngen0.noise(edgeNes, SmoothingFunction.S_CURVE,
//					node.noff + frame * 0.2f * node.nrate);
//			final float n3 = 25 * node.ngen3.noise(edgeNes, SmoothingFunction.S_CURVE,
//					node.noff + frame * 0.2f * node.nrate);
//			
//			final float n1 = 8 * node.ngen1.noise(edgeNes, SmoothingFunction.S_CURVE,
//					node.noff + frame * 0.5f * node.nrate);
//			
//			final float n2 = 8 * node.ngen2.noise(edgeNes, SmoothingFunction.S_CURVE,
//					node.noff + frame * 0.5f * node.nrate);
//			
			
			for (int eindex : node.sedge0s) {
				
			final VisualEdge edge = sigEdges[eindex];
			
			
			final float bx0, bxc0, bxc1, bx1;
			final float by0, byc0, byc1, by1;
			
			
			// Apply noise:
			
			final float n0 = 25 * edge.ngen0.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.2f * edge.nrate);
			final float n3 = 25 * edge.ngen3.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.2f * edge.nrate);
			
			final float n1 = 8 * edge.ngen1.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.5f * edge.nrate);
			
			final float n2 = 8 * edge.ngen2.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.5f * edge.nrate);
			
			
//			final float n0 = 0.f;
//			final float n2 = 0.f;
//			final float n1 = 0.f;
//			final float n3 = 0.f;
			
//			bounds2[2] += n0 * edge.v[0] - n1 * edge.u[0];
//			bounds2[3] += n0 * edge.v[1] - n1 * edge.u[1];
//			bounds2[4] += n3 * edge.v[0] + n2 * edge.u[0];
//			bounds2[5] += n3 * edge.v[1] + n2 * edge.u[1];
//			
			
			
			{
				final float[] bounds = edge.bounds;
				bx0 = bounds[0];
				bxc0 = bounds[2] + n0 * edge.v[0] - n1 * edge.u[0];
				bxc1 = bounds[4] + n3 * edge.v[0] + n2 * edge.u[0];
				bx1 = bounds[6];
				by0 = bounds[1];
				byc0 = bounds[3] + n0 * edge.v[1] - n1 * edge.u[1];
				byc1 = bounds[5] + n3 * edge.v[1] + n2 * edge.u[1];
				by1 = bounds[7];
			}
			
			final float bv = nodes[edge.id0].mm * 3.f;
			
			for (int i = 0; i < N; ++i) {
				
				
//				out[0] = a * bounds[0] + b * bounds[2] + c * bounds[4] + d * bounds[6];
//				out[1] = a * bounds[1] + b * bounds[3] + c * bounds[5] + d * bounds[7];
				
				final int j = i << 1;
//				CurveUtilities.eval3(bounds2, i / (float) (N - 1), out);
				
				final float a = as[i];
				final float b = bs[i];
				final float c = cs[i];
				final float d = ds[i];
				
				final float x = a * bx0 + b * bxc0 + c * bxc1 + d * bx1;
				final float y = a * by0 + b * byc0 + c * byc1 + d * by1;
				
				samples[j] = x;
				samples[j + 1] = y;
				
				
				final float dx = gmousex - x;
				final float dy = gmousey - y;
				final float dsq = dx * dx + dy * dy;
//				2498.04 + 0.999118 * dsq;
				final float dsqsq = dsq * dsq;
//				final float t = 1 - 0.00010000500033334732f * dsq + 
//				5.0005000458363995e-9f * dsqsq
//				- 1.66692e-13f * dsqsq * dsq;
//				final float t = 1 - 0.0010005f * dsq + 
//				5.005e-7f * dsqsq
//				- 1.66917e-10f * dsqsq * dsq;
				final float t = 1 - 0.0000500013f * dsq + 
				1.25006e-9f * dsqsq
				- 2.08349e-14f * dsqsq * dsq;
				
				ths[i] = 0.5f * (bv + (t < 0 ? 1.f : 1.f + 3.5f * t));
				// 1.66692*10^-13*s*s*s
//				return 1 + 5.f * (2498.04f + 0.999118f * (dsq - 50 * 50));
				//(float) Math.pow(0.9999, dsq);
				
				
			}
			
				final int rgb = edge.repr;
				//sig.colors[0];
				gl.glColor4f(
						((rgb >> 16) & 0xFF) / (float) 0xFF,
						
						((rgb >> 8) & 0xFF) / (float) 0xFF,
						(rgb & 0xFF) / (float) 0xFF,
						1.f
				);
			
			
			
			// 0 is the origin

//			tf.m = 0.f;
//				tf.m = nodes[edge.id0].mm ;
//				tf.N = N;
//				tf.min = 1.f;
//				tf.max = 2.f;
				//+ nodes[edge.id1].mm;
//				ShapeUtilities.toQuadStrip(samples, tf, qscb);
				
			// TODO:
//			ShapeUtilities.toQuadStrip(samples, 1.f, qscb);
			

				ShapeUtilities.toQuadStrip(samples, ths, N, qsamples);
//				qsamplesb.rewind();
				qsamplesb.put(qsamples);
				qsamplesb.flip();
				gl.glVertexPointer(2, GL.GL_FLOAT, 0, qsamplesb);
				
				/*
				if (0 <= beivbo) {
				gl.glDrawElements(GL.GL_QUADS, 
	            		beindexCount, 
	            		GL.GL_UNSIGNED_SHORT,
	            		0);
				}
				else {
					gl.glDrawElements(GL.GL_QUADS, 
		            		beindexCount, 
		            		GL.GL_UNSIGNED_SHORT,
		            		beindices);
				}
				*/
//			quads(gl);
			gl.glDrawArrays(GL.GL_QUAD_STRIP, 0, N << 1);
			
//			gl.glBegin(GL.GL_QUAD_STRIP);
//			gl.glEnd();
			
			
			gl.glBegin(GL.GL_POINTS);
			gl.glVertex2f(bx0, by0);
			gl.glVertex2f(bx1, by1);
			gl.glEnd();
		}
		}
		
		
//		if (vboSupported) {
//			gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
//		}
		
		
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);

	}
	
	private void renderWordEdges(final GL gl, final boolean active, final boolean top) {
		
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

		
		gl.glEnable(GL.GL_BLEND);
		
		// enable blend
		 gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		for (VisualEdge edge : wordEdges) {
			final VisualNode node = nodes[edge.id0];
			
			if (active && node.mm < MIN_ACTIVE_MM) {
				continue;
			}
			if (top && node.id != sortIndices[nodes.length - 1]) {
				continue;
			}
			
			final float bx0, bxc0, bxc1, bx1;
			final float by0, byc0, byc1, by1;
			
			
			// Apply noise:
			
			final float n0 = 25 * edge.ngen0.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.2f * edge.nrate);
			final float n3 = 25 * edge.ngen3.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.2f * edge.nrate);
			
			final float n1 = 8 * edge.ngen1.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.5f * edge.nrate);
			
			final float n2 = 8 * edge.ngen2.noise(edgeNes, SmoothingFunction.S_CURVE,
					edge.noff + frame * 0.5f * edge.nrate);
			
			edge.n0 = n0;
			
//			final float n0 = 0.f;
//			final float n2 = 0.f;
//			final float n1 = 0.f;
//			final float n3 = 0.f;
			
//			bounds2[2] += n0 * edge.v[0] - n1 * edge.u[0];
//			bounds2[3] += n0 * edge.v[1] - n1 * edge.u[1];
//			bounds2[4] += n3 * edge.v[0] + n2 * edge.u[0];
//			bounds2[5] += n3 * edge.v[1] + n2 * edge.u[1];
//			
			
			
			{
				final float[] bounds = edge.bounds;
				bx0 = bounds[0];
				bxc0 = bounds[2] + n0 * edge.v[0] - n1 * edge.u[0];
				bxc1 = bounds[4] + n3 * edge.v[0] + n2 * edge.u[0];
				bx1 = bounds[6];
				by0 = bounds[1];
				byc0 = bounds[3] + n0 * edge.v[1] - n1 * edge.u[1];
				byc1 = bounds[5] + n3 * edge.v[1] + n2 * edge.u[1];
				by1 = bounds[7];
			}
			
			final float min = 12.f;
			final float max = 3.f;
			
			for (int i = 0; i < N; ++i) {
				
				
//				out[0] = a * bounds[0] + b * bounds[2] + c * bounds[4] + d * bounds[6];
//				out[1] = a * bounds[1] + b * bounds[3] + c * bounds[5] + d * bounds[7];
				
				final int j = i << 1;
//				CurveUtilities.eval3(bounds2, i / (float) (N - 1), out);
				
				final float u = us[i];
				final float a = as[i];
				final float b = bs[i];
				final float c = cs[i];
				final float d = ds[i];
				
				final float x = a * bx0 + b * bxc0 + c * bxc1 + d * bx1;
				final float y = a * by0 + b * byc0 + c * byc1 + d * by1;
				
				samples[j] = x;
				samples[j + 1] = y;
				
				
				final float dx = gmousex - x;
				final float dy = gmousey - y;
				final float dsq = dx * dx + dy * dy;
//				2498.04 + 0.999118 * dsq;
				final float dsqsq = dsq * dsq;
//				final float t = 1 - 0.00010000500033334732f * dsq + 
//				5.0005000458363995e-9f * dsqsq
//				- 1.66692e-13f * dsqsq * dsq;
//				final float t = 1 - 0.0010005f * dsq + 
//				5.005e-7f * dsqsq
//				- 1.66917e-10f * dsqsq * dsq;
				final float t = 1 - 0.0000500013f * dsq + 
				1.25006e-9f * dsqsq
				- 2.08349e-14f * dsqsq * dsq;
				
				ths[i] = 0.5f * (min + (max - min) * u) * (t < 0 ? 1.f : 1.f + 2.5f * t);
				// 1.66692*10^-13*s*s*s
//				return 1 + 5.f * (2498.04f + 0.999118f * (dsq - 50 * 50));
				//(float) Math.pow(0.9999, dsq);
				
				
			}
			
			// TODO:
//			nodes[edge.id0].repr;
			
//			gl.glColor4f(0.58f, 0.58f, 0.8f, 0.2f + nodes[edge.id0].mm * 0.5f);
			gl.glColor4f(
					0.0f + node.mm * 0.9f, 
					0.0f + node.mm * 0.9f, 
					0.0f, 
					top ? 1.f * node.mm : (0.25f + node.mm * node.mm * 0.2f));
//			final int rgb = nodes[edge.id0].repr;
//			//sig.colors[0];
//			gl.glColor4f(
//					((rgb >> 16) & 0xFF) / (float) 0xFF,
//					
//					((rgb >> 8) & 0xFF) / (float) 0xFF,
//					(rgb & 0xFF) / (float) 0xFF,
//					1.f
//			);
//			gl.glBegin(GL.GL_QUAD_STRIP);
			
//			qscb.gl = gl;
//			// 0 is the origin
//			
//			wtf.N = N;
//			wtf.min = 1.f;
//			wtf.max = 25.f + nodes[edge.id0].mm * -8.f;
//			ShapeUtilities.toQuadStrip(samples, wtf, qscb);
//			
//			gl.glEnd();
			
//			gl.glBegin(GL.GL_QUAD_STRIP);
////			quads(gl);
//			ShapeUtilities.toQuadStrip(samples, ths, N, gl);
//			gl.glEnd();
			
			
			ShapeUtilities.toQuadStrip(samples, ths, N, qsamples);
//			qsamplesb.rewind();
			qsamplesb.put(qsamples);
			qsamplesb.flip();
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, qsamplesb);
			
			gl.glDrawArrays(GL.GL_QUAD_STRIP, 0, N << 1);
			
//			gl.glBegin(GL.GL_POINTS);
//			gl.glVertex2f(bounds2[0], bounds2[1]);
//			gl.glVertex2f(bounds2[6], bounds2[7]);
//			gl.glEnd();
		}
		gl.glDisable(GL.GL_BLEND);

		
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);

	}
	
	
	
	private void renderNodes(final GL gl, final boolean active, final boolean top) {
//		gl.glEnable(GL.GL_BLEND);
		
		// enable blend
		// gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		
//		if (true) {
//			return;
//		}
		
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

gl.glMatrixMode(GL.GL_MODELVIEW);
		
		for (int ni = 0; ni < nodes.length; ++ni) {
			final VisualNode node = nodes[sortIndices[ni]];
			
			if (! node.ready) {
				continue;
			}
			
			if (active && node.mm < MIN_ACTIVE_MM) {
				continue;
			}
			if (top && node.id != sortIndices[nodes.length - 1]) {
				continue;
			}
			
			if (! node.ready) {
					gl.glLineWidth(2.f);
					gl.glColor4f(1.f, 1.f, 1.f, 0.2f);
					
					gl.glBegin(GL.GL_LINE_STRIP);
					gl.glVertex2f(node.cx - 10, node.cy - 10);
					gl.glVertex2f(node.cx + 10, node.cy - 10);
					gl.glVertex2f(node.cx + 10, node.cy + 10);
					gl.glVertex2f(node.cx - 10, node.cy + 10);
					gl.glVertex2f(node.cx - 10, node.cy - 10);
					gl.glEnd();
					
					continue;
			}
			
			
			node.noise.advance();
			
			
			gl.glPushMatrix();
			
			final float scale = 0.6f + 4 * (node.mm * node.mm * 0.20f);
            gl.glTranslatef(node.cx, node.cy, 0.f);
            gl.glScalef(scale, scale, 1.f);
            gl.glTranslatef(-node.cx, -node.cy, 0.f);
            
            

            gl.glTranslatef(node.cx - node.width / 2.f, node.cy - node.height / 2.f, 0.f);
            
            
            
            float fillAlpha;
            float outlineAlpha;
            if (0.8f <= node.mm) {
            	fillAlpha = 0.8f;
            	outlineAlpha = 0.7f;
            }
            else {
            	fillAlpha = 0.f;
            	outlineAlpha = 1.f;
            }
            
            
            // FILL RENDERING
            
            if (0 < fillAlpha) {
            	if (fillAlpha < 1) {
            		gl.glEnable(GL.GL_BLEND);
        			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            	}
            	
            if (node.vbo < 0 && vboSupported) {
				final int[] _vbo = {-1};
				
				gl.glGenBuffersARB(1, _vbo, 0);
				// TODO: check error state
				if (0 <= _vbo[0]) {
	            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
	            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, node.buffer.limit() * BufferUtil.SIZEOF_FLOAT, 
	            		node.buffer, GL.GL_STATIC_DRAW_ARB);

	            node.vbo = _vbo[0];
				node.buffer = null;
				}
			}
            
            if (0 <= node.vbo) {
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, node.vbo);
                gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
            } else {
                gl.glVertexPointer(2, GL.GL_FLOAT, 0, node.buffer);
            }
            
           
			for (int j = 0; j < node.blobCount; ++j) {
				if (node.empty[j]) {
					continue;
				}
				
	            
	           
				if (node.ivbo[j] < 0 && vboSupported) {
					final int[] _vbo = {-1};
					
					gl.glGenBuffersARB(1, _vbo, 0);
					// TODO: check error state
					if (0 <= _vbo[0]) {
		            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, _vbo[0]);
		            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER, 
		            		node.indices[j].limit() * BufferUtil.SIZEOF_SHORT, 
		            		node.indices[j], GL.GL_STATIC_READ_ARB);
	
		            node.ivbo[j] = _vbo[0];
					node.indices[j] = null;
					}
				}
			
	            if (0 <= node.ivbo[j]) {
	            	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, node.ivbo[j]);
	            }
	            
	            
	            final float alpha;
	            if (1.f <= fillAlpha) {
	            	alpha = fillAlpha;
	            }
	            else {
	            	alpha = fillAlpha * (0.2f + 0.8f * node.noise.noise[j][0]);
	            }
	            
	            final int rgb = node.blobReprs[j];
	            gl.glColor4f(
	            	((rgb >> 16) & 0xFF) / (float) 255,
            		((rgb >> 8) & 0xFF) / (float) 255,
            		(rgb & 0xFF) / (float) 255, 
            		alpha
            	);
	            
	            
	            if (0 <= node.ivbo[j]) {
	            gl.glDrawRangeElements(GL.GL_TRIANGLES, 
	            		node.start[j], node.end[j],
	            		node.icount[j], 
	            		GL.GL_UNSIGNED_SHORT,
	            		0);
	            }
	            else {
	            	gl.glDrawElements(GL.GL_TRIANGLES, node.icount[j], GL.GL_UNSIGNED_SHORT,
		            		node.indices[j]);
	            }
	            

//	            final int error = gl.glGetError();
//	            assert 0 == error : error + ": " + glu.gluGetString(error);
			}
			
			if (fillAlpha < 1) {
				gl.glDisable(GL.GL_BLEND);
			}
			
            }
			
			
			
			
			
			// OUTLINE RENDERING
			
			
            if (0 < outlineAlpha) {
            	if (outlineAlpha < 1) {
            		gl.glEnable(GL.GL_BLEND);
        			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            	}
			
			if (node.outlineVbo < 0 && vboSupported) {
				final int[] _vbo = {-1};
				
				gl.glGenBuffersARB(1, _vbo, 0);
				// TODO: check error state
				if (0 <= _vbo[0]) {
				gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, _vbo[0]);
	            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, node.outlineBuffer.limit() * BufferUtil.SIZEOF_FLOAT, 
	            		node.outlineBuffer, GL.GL_STATIC_DRAW_ARB);

	            node.outlineVbo = _vbo[0];
				node.outlineBuffer = null;
				}
			}
			
			if (0 <= node.outlineVbo) {
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, node.outlineVbo);
                gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);		// Set The Vertex Pointer To The Vertex Buffer
            } else {
                gl.glVertexPointer(2, GL.GL_FLOAT, 0, node.outlineBuffer); // Set The Vertex Pointer To Our Vertex Data
            }
			
			for (int j = 0; j < node.blobCount; ++j) {
				if (node.empty[j]) {
					continue;
				}
					            
	            
				
				if (node.outlineIVbo[j] < 0 && vboSupported) {
					final int[] _vbo = {-1};
					
					gl.glGenBuffersARB(1, _vbo, 0);
					// TODO: check error state
					if (0 <= _vbo[0]) {
					gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, _vbo[0]);
		            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER, 
		            		node.outlineIndices[j].limit() * BufferUtil.SIZEOF_SHORT, 
		            		node.outlineIndices[j], GL.GL_STATIC_READ_ARB);

		            node.outlineIVbo[j] = _vbo[0];
					node.outlineIndices[j] = null;
					}
				}
	            
				
	            if (0 <= node.outlineIVbo[j]) {
	            	gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, node.outlineIVbo[j]);
	            }
	            
	            
	            
	            final int rgb = node.blobReprs[j];
	            gl.glColor4f(
            		((rgb >> 16) & 0xFF) / (float) 255,
            		((rgb >> 8) & 0xFF) / (float) 255,
            		(rgb & 0xFF) / (float) 255, 
            		outlineAlpha
        		);
	            
	            
	            final float w = 
	            	0.5f * outlineAlpha * (0.5f + 3.f * node.noise.noise[j][0]);
	            
	            // TODO:
	            gl.glLineWidth(w < 1.f ? 1.f : w);
	            
	            if (0 <= node.outlineIVbo[j]) {
		            gl.glDrawRangeElements(GL.GL_LINES, 
		            		node.outlineStart[j], node.outlineEnd[j],	
		            		node.outlineCounts[j], 
	            		GL.GL_UNSIGNED_SHORT,
	            		0);
	            }
	            else {
	            	gl.glDrawElements(GL.GL_LINES, node.outlineCounts[j], 
	            			GL.GL_UNSIGNED_SHORT,
		            		node.outlineIndices[j]);
	            }	            

//	            final int error = gl.glGetError();
//	            assert 0 == error : error + ": " + glu.gluGetString(error);
			}
				if (outlineAlpha < 1) {
					gl.glDisable(GL.GL_BLEND);
				}
            }
            
			gl.glPopMatrix();
		}
		
		
		if (vboSupported) {
			// Unbind:
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

        // Disable Pointers
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        
//		gl.glDisable(GL.GL_BLEND);
	}
	
	
	
	private void renderContours(final GL gl, final VisualNode node) {
//		gl.glEnable(GL.GL_BLEND);
//		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//		
		gl.glColor4f(1.f, 1.f, 1.f, 1.f);
		
		for (float[][] aContourCenters : node.contourCenters) {
			for (float[] centers : aContourCenters) {
				gl.glBegin(GL.GL_QUAD_STRIP);
				ShapeUtilities.toQuadStrip(centers, 1.f, gl);
				gl.glEnd();
			}
		}
		
//		gl.glDisable(GL.GL_BLEND);
	}
	
	
	
	/**************************
	 * GLEVENTLISTENER IMPLEMENTATION
	 **************************/
	

	@Override
	public void init(final GLAutoDrawable drawable) {
		
		GL gl = drawable.getGL();
        glu = new GLU();
        gl.glClearColor(0.f, 0.f, 0.f, 1.f);
        gl.glClearStencil(0);
        
        // We manage our own depth, so disable this test (big perf boost):
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_STENCIL_TEST);
        gl.glDisable(GL.GL_ACCUM);
        // Disable v-sync if we can:
	    gl.setSwapInterval(0);
        
	    final Font titleFont = FontUtilities.ERAS_LIGHT.deriveFont(42.f);
	    titleMetrics = getFontMetrics(titleFont);
	    titleRenderer = new TextRenderer(titleFont);
	    
	    
	    final Font branchFont = FontUtilities.ERAS_BOLD.deriveFont(20.f);
	    branchMetrics = getFontMetrics(branchFont);
	    branchRenderer = new TextRenderer(branchFont);
	    
	    
	    
	    for (int i = 0; i < titles.length; ++i) {
	    	final String title = titles[i];
			titleWidths[i] = titleMetrics.stringWidth(title);
			titleHeights[i] = titleMetrics.getHeight();
			branchWidths[i] = branchMetrics.stringWidth(title);
			branchHeights[i] = branchMetrics.getHeight();
	    }
	    
	}
	
	@Override
	public void reshape(final GLAutoDrawable drawable, 
			final int x, final int y, final int _width, final int _height
	) {
		this.width = _width;
		this.height = _height;
		
		// TODO:
		GL gl = drawable.getGL();

		gl.glViewport(x, y, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, (double) width, 0.0, (double) height);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        
        gl.glTranslatef(0.f, height / 2.f, 0.f);
        gl.glScalef(1.f, -1.f, 1.f);
        gl.glTranslatef(0.f, -height / 2.f, 0.f);
        
        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
        

//        if (null != textRenderer) {
//        	textRenderer.dispose();
//        }
	    
	}
	
	@Override
	public void display(final GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
        
		if (trail) {
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			final float hw = width / 2.f;
			final float hh = height / 2.f;
			
			gl.glColor4f(0.f, 0.f, 0.f, trailAlpha);
			gl.glBegin(GL.GL_QUADS);
				gl.glVertex2f(-hw, -hh);
				gl.glVertex2f(hw, -hh);
				gl.glVertex2f(hw, hh);
				gl.glVertex2f(-hw, hh);
			gl.glEnd();
			gl.glDisable(GL.GL_BLEND);
		}
		else {
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		}
		render(gl);
	}
	
	@Override
	public void displayChanged(final GLAutoDrawable _drawable, 
			final boolean modeChanged, final boolean deviceChanged
	) {
		// Do nothing
	}
	
	
	/**************************
	 * END GLEVENTLISTENER IMPLEMENTATION
	 **************************/
	
	
	
	
	
	private static int toInternalMode(final int mode) {
		switch (mode) {
			case GL.GL_TRIANGLES: return 0;
			case GL.GL_TRIANGLE_FAN: return 1;
			case GL.GL_TRIANGLE_STRIP: return 2;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	private static int fromInternalMode(final int internalMode) {
		switch (internalMode) {
			case 0: return GL.GL_TRIANGLES;
			case 1: return GL.GL_TRIANGLE_FAN;
			case 2: return GL.GL_TRIANGLE_STRIP;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	
	private static short encodeFrame(final int mode, final int count) {
		final int internalMode = toInternalMode(mode);
		// the internal mode is 2 bits
		return (short) ((internalMode & 0x03)
			| (count << 2));
	}
	
	private static void decodeFrame(final short frame, final int[] parts) {
		parts[0] = fromInternalMode((int) (frame & 0x03));
		parts[1] = (int) ((frame >> 2) & 0xFFFF);
	}
	
	
	
	// store [TYPE|COUNT] in a short
	// just maintain a list of these
	
	
	// a pointCallback
	// that drives a GL tesselator
	public static class TessCallback extends GLUtessellatorCallbackAdapter
			implements PathCallback {
		private final GLU glu;

		private GLUtessellator tobj;
//		private double[] dpoint = {0, 0, 0};
		
		private float x0;
		private float y0;
		private float lastx;
		private float lasty;

		public TessCallback(final GLU _glu) {
			this.glu = _glu;
			
			init();
		}

		private void init() {
			tobj = glu.gluNewTess();

			glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
			glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
			glu.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
			glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);

//			glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
//					GLU.GLU_TESS_WINDING_NONZERO);
			glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
					GLU.GLU_TESS_WINDING_NONZERO);

			// gl_useList = gl.glGenLists(1);
			// gl.glNewList(gl_useList, GL.GL_COMPILE);

			/*
			 * // gl.glShadeModel(GL.GL_FLAT); glu.gluTessBeginPolygon(tobj,
			 * null); glu.gluTessBeginContour(tobj); for (int i = 0; i <
			 * tessVertices.length; i++) { glu.gluTessVertex(tobj,
			 * tessVertices[i], 0, tessVertices[i]); } //
			 * glu.gluTessVertex(tobj, rect[1], 0, rect[1]); //
			 * glu.gluTessVertex(tobj, rect[2], 0, rect[2]); //
			 * glu.gluTessVertex(tobj, rect[3], 0, rect[3]);
			 * glu.gluTessEndContour(tobj); // glu.gluTessBeginContour(tobj); //
			 * glu.gluTessVertex(tobj, tri[0], 0, tri[0]); //
			 * glu.gluTessVertex(tobj, tri[1], 0, tri[1]); //
			 * glu.gluTessVertex(tobj, tri[2], 0, tri[2]); //
			 * glu.gluTessEndContour(tobj); glu.gluTessEndPolygon(tobj);
			 * gl.glEndList();
			 */
			/*
			 * glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
			 * glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
			 * glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
			 * glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);
			 * glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
			 */

			/*
			 * gl.glNewList(startList + 1, GL.GL_COMPILE);
			 * gl.glShadeModel(GL.GL_SMOOTH); glu.gluTessProperty(tobj,
			 * GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE);
			 * glu.gluTessBeginPolygon(tobj, null);
			 * glu.gluTessBeginContour(tobj); glu.gluTessVertex(tobj, star[0],
			 * 0, star[0]); glu.gluTessVertex(tobj, star[1], 0, star[1]);
			 * glu.gluTessVertex(tobj, star[2], 0, star[2]);
			 * glu.gluTessVertex(tobj, star[3], 0, star[3]);
			 * glu.gluTessVertex(tobj, star[4], 0, star[4]);
			 * glu.gluTessEndContour(tobj); glu.gluTessEndPolygon(tobj);
			 * gl.glEndList();
			 */

		}

		private void uninit() {
			glu.gluDeleteTess(tobj);
		}
		
		
		public void dispose() {
			uninit();
		}

		
		public TFloatArrayList frames = new TFloatArrayList(32);
		// TODO: a FloatBuffer for points
		// at the end, try to push this into a VBO;
		//    if we can't, just dump to a display list
		// of course if we have a vbo, call drawarrays from a DL
		public TIntArrayList types = new TIntArrayList(32);
		
		public void reset() {
			frames.clear();
			types.clear();
			
			uninit();
			init();
		}
		
		
		
		
		/***********************************************************************
		 * PATHCALLBACK IMPLEMENTATION
		 **********************************************************************/

		public void startPolygon(final int contourCount) {
			glu.gluTessBeginPolygon(tobj, null);
		}

		public void endPolygon() {
			glu.gluTessEndPolygon(tobj);
		}

		public void startContour(final int lineCount) {
			glu.gluTessBeginContour(tobj);
		}

		public void endContour() {
			_point(x0, y0);
			glu.gluTessEndContour(tobj);
		}

		public void moveTo(final float _x0, final float _y0) {
			this.x0 = _x0;
			this.y0 = _y0;
			_point(_x0, _y0);
		}

		public void lineTo(final float x, final float y) {
			_point(x, y);
		}

		private void _point(final float x, final float y) {
			if (lastx == x && lasty == y) {
				return;
			}
			lastx = x;
			lasty = y;
			final double[] dpoint = {x, y, 0};
//			dpoint[0] = x;
//			dpoint[1] = y;
			glu.gluTessVertex(tobj, dpoint, 0, dpoint);
		}

		/***********************************************************************
		 * END PATHCALLBACK IMPLEMENTATION
		 **********************************************************************/

		/***********************************************************************
		 * TESS CALLBACKS
		 **********************************************************************/

		@Override
		public void begin(int type) {
			// gl.glBegin(type);
			

//			if (! types.isEmpty()) {
//				types.add(frames.size());
//			}
			types.add(type);
			
			/*
			
			
			System.out.println("start: " + name);
			*/
		}

		@Override
		public void end() {
			// gl.glEnd();

			// point index:
			types.add(frames.size() / 2);
		}

		@Override
		public void vertex(Object data) {
			// if (data instanceof double[]) {
			// double[] d = (double[]) data;
			// if (d.length == 6) {
			// gl.glColor3dv(d, 3);
			// }
			// gl.glVertex3dv(d, 0);
			// }
//			System.out.println("    v");
			
			final double[] vertex = (double[]) data;
			frames.add((float) vertex[0]);
			frames.add((float) vertex[1]);
		}

		@Override
		public void error(int errnum) {
			// String estring;
			// estring = glu.gluErrorString(errnum);
			// System.out.println("Tessellation Error: " + estring);
			// // System.exit(0);
			// throw new RuntimeException();
		}

		// private final double[] REUSE_VERTEX = new double[6];
		@Override
		public void combine(double[] coords, Object[] data, float[] weight,
				Object[] outData) {
			final double[] vertex = new double[6];

			int i;
			vertex[0] = coords[0];
			vertex[1] = coords[1];
			vertex[2] = coords[2];
			for (i = 3; i < 6; i++) {
				vertex[i] = 
					weight[0] * ((double[]) data[0])[i] + 
					weight[1] * ((double[]) data[1])[i] +
					weight[2] * ((double[]) data[2])[i] + 
					weight[3] * ((double[]) data[3])[i];
			}
			outData[0] = vertex;
//			final double[] vertex = new double[3];
//            
//            vertex[0] = coords[0];
//            vertex[1] = coords[1];
//            vertex[2] = coords[2];
//            outData[0] = coords;
		}

		/***********************************************************************
		 * END TESS CALLBACKS
		 **********************************************************************/

	}
	
	
	
	
	
	
	/**************************
	 * VISUAL STRUCTURES
	 * 
	 * Graph items, etc.
	 **************************/
	

	private static final class VisualNode {
		
//		public TextureData txData;
//		public int txId;
		public Texture tx;
		
		// this is the id in the input data array
		// (can also map to a wid)
		public final int id;
		
		// map of target int to visual edge
		public final TIntObjectHashMap<VisualEdge> blobEdgeMap = new TIntObjectHashMap<VisualEdge>();
		public final TIntObjectHashMap<VisualEdge> wordEdgeMap = new TIntObjectHashMap<VisualEdge>();
		
		public PerlinNoiseGenerator ngen;
		
		
		// RENDER DATA STATE
		
		// if -1, that means the blob is in immediate mode
//		public int[] blobVbos;
//		// display lists, which draw each blob --
//		// note these may be immediate mode
//		// or use the vbo above.
//		// 
//		public int[] blobDls;
//		
//		// same as blobs
//		public int[] contourVbos;
//		public int[] contourDls;
		
		// END RENDER DATA STATE
		
		// RENDER STATE
		
		public float cx = 0.f;
		public float cy = 0.f;
		
		public int width;
		public int height;
		
		
		public Rectangle2D.Float boundingBox;
		
		// mouse prox multiplier
		public float mm;
		public float[] mms;
		public float[] t0s;
		
		public int[] sedge0s;
		public int[] sedge1s;
		
		
//		public boolean useVbo = false;
		public int vbo;
		public FloatBuffer buffer;
		public boolean[] empty;
//		public int[][] modes;
//		public int[][][] allFirsts;
//		public int[][][] allCounts;
		
		public ShortBuffer[] indices;
		public int[] ivbo;
		public int[] icount;
		
		public boolean ready = false;
		
		public int repr;
		public int[] blobReprs;
		
		
		public int blobCount = 0;
		
		public int outlineVbo;
		public FloatBuffer outlineBuffer;
		
		public int[] outlineIVbo;
		public ShortBuffer[] outlineIndices;
		public int[] outlineCounts;
		
		
		public int[] start;
		public int[] end;
		public int[] outlineStart;
		public int[] outlineEnd;
		
		
		public float[][][] contourCenters;
		
		
//		public PerlinNoiseGenerator ngen0;
//		public PerlinNoiseGenerator ngen1;
//		public PerlinNoiseGenerator ngen2;
//		public PerlinNoiseGenerator ngen3;
//		
//		public float noff = (float) Math.random();
//		public float nrate = 0.1337553111f;
//		
		
		public Noise0 noise;
		
		public Polygon[] polys;
		
		
		
		public Rectangle2D.Float[] absBounds;
		public Rectangle2D.Float[] fracBounds;
		
		
		
		public VisualNode(final int _id) {
			this.id  = _id;
		}
	}
	
	
	private static final class VisualEdge {
		// parent
		public final int id0;
		// child
		public final int id1;
		
		
		public int dl;
		
		
		public Signature sig;
		public int repr;
		
		
		// RENDER STATE
		
		public float[] bounds;
//		public float[] clippedBounds;
//		float bx0, bxc0, bxc1, bx1;
//		float by0, byc0, byc1, by1;
		
		
		// parallel to main axis
		public float[] u;
		// perpendicular to main axis
		public float[] v;
		// center of control points
		public float[] c;
		
		
		public Rectangle2D.Float boundingBox;
		
		public PerlinNoiseGenerator ngen0;
		public PerlinNoiseGenerator ngen1;
		public PerlinNoiseGenerator ngen2;
		public PerlinNoiseGenerator ngen3;
		
		public float noff = (float) Math.random();
		public float nrate = 0.1337553111f;
		
		public float n0;
		
		public VisualEdge(int _id0, int _id1) {
			if (_id1 < _id0) {
				int t = _id0;
				_id0 = _id1;
				_id1 = t;
			}
			this.id0 = _id0;
			this.id1 = _id1;
		}
		
		
	}
	

	
	/**************************
	 * END VISUAL STRUCTURES
	 **************************/
	
	
	
	
	
	
	
	
	public static void main(final String[] in) {
		// quick perlin noise test ...
		
// final PerlinNoiseGenerator ngen = new PerlinNoiseGenerator(1, 7);
// final NoiseEvalState nes = ngen.createEvalState();
// long time0 = System.nanoTime();
// for (int j = 0 ;j < 50; ++j) {
// final float[] coords = new float[1];
// for (int i = 0; i < 50 * 50 * 24; ++i) {
// ngen.noise(nes, SmoothingFunction.S_CURVE, coords);
// //ImprovedNoise.noise(0, 0, 0);
// }
// }
// long time1 = System.nanoTime();
// System.out.println((time1 - time0)/50);
		
		
		final OutputPacket2 packet = DEBUG_LOAD();
		final ExploreScreen es = new ExploreScreen();
		es.init(new Object[]{packet});
		
		// TODO: show
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(es, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		
		es.start();
	}
	
	
	
	
	private static final class ExploreThreadFactory implements ThreadFactory {
		public ExploreThreadFactory() {
		}
		
		/**************************
		 * THREADFACTORY IMPLEMENTATION
		 **************************/
		
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread(r, "ENTER & RE-EXIT EXPLORE HELPER");
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
}
