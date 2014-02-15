package org.resisttheb.ere.ui2;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.resisttheb.ere.persistence.Format0;
import org.resisttheb.ere.persistence.Format0.Data;
import org.resisttheb.ere.signature.Linker;
import org.resisttheb.ere.signature.Signature;
import org.resisttheb.ere.signature.WeightedInverseMetric;
import org.resisttheb.ere.signature.Linker.InputFrame;
import org.resisttheb.ere.signature.Linker.OutputFrame;
import org.resisttheb.ere.ui.AbstractScreen;
import org.resisttheb.ere.ui.EreApiUtilities;
import org.resisttheb.ere.ui.FontUtilities;
import org.resisttheb.ere.ui.JsonCall;
import org.resisttheb.ere.ui.UiUtilities;
import org.resisttheb.ere.ui1.OutputPacket1;

public class KickerScreen extends AbstractScreen {
	private final int COMPONENT_SIZE = 75;
	
	
	
	
	private OutputPacket1[] packets;
	
	private ThreadFactory tf;
	private ThreadPoolExecutor ex;
	private BlockingQueue<Runnable> exQueue;
	
	private JTextPane text;

	private Format0 format;
	
	
	public KickerScreen() {
		format = new Format0();
		
		initProcessors();
		initUi();
	}
	
	
	private void initProcessors() {
		tf = new KickerThreadFactory();
		exQueue = new LinkedBlockingQueue<Runnable>();
		ex = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS,
				exQueue, tf);
//		ex.setMaximumPoolSize(n);
		
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
	
	
	public void setPackets(final OutputPacket1[] _packets) {
		this.packets = _packets;
	}
	
	
	
	/**************************
	 * FINISHING STEPS
	 **************************/
	private int[] linkedWids;
	private Format0.Data[] linkedData;
	private Object[][] linkedItems;
	
	private int[][] wordLinks;
	private int[][] blobLinks;
	
	// NOTE: may be called on any worker thread
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

		
		final OutputPacket2 outp = new OutputPacket2(
				linkedWids,
				linkedData,
				wordLinks,
				blobLinks,
				linkedItems
				);
		sh.finish(new OutputPacket2[]{outp});
	}
	
	private boolean _preFinish() {
		try {
			
			// this step finds linked words,
			// downloads the blob packet,
			// and decodes the Format0.Data packets
			if (! findLinkedWords()) {
				_print(Color.GRAY, "could not find linked words");
				return false;
			}
			
			if (! readAndParseData()) {
				_print(Color.GRAY, "power down");
				return false;
			}
			
			// TODO:
//			filterNullData();
			
			// reads Format0.Data packets and creates sig links
			// using the linker
			if (! linkSignatures()) {
				_print(Color.GRAY, "could not link colors");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	
	
	private boolean findLinkedWords() throws IOException {
		_print(Color.CYAN, "exploring the graph ... this could take a bit ...");
		
		assert 1 == packets.length : "Can't handle more than one input packet.";
		final int[] activeWids = packets[0].activeWids;
		final ExplorationResults er = exploreLinks(COMPONENT_SIZE, activeWids);
		
		
		final TIntIntHashMap widToIndexMap = new TIntIntHashMap(32);
		
		// CHECK:
		for (int activeWid : activeWids) {
			assert er.wids.contains(activeWid);
		}
		
		linkedWids = new int[er.wids.size()];
		{
			int i = 0;
			for (Integer wid : er.wids) {
				linkedWids[i] = wid;
				widToIndexMap.put(wid, i);
				++i;
			}
		}
		
		linkedItems = new Object[linkedWids.length][];
		{
			er.itemsMap.forEachEntry(new TIntObjectProcedure<Object[]>() {
				@Override
				public boolean execute(final int wid, final Object[] item) {
					linkedItems[widToIndexMap.get(wid)] = item;
					return true;
				}
			});
		}
		
//		int wordLinksCount = 0;
//		for (int[] links : er.links) {
//			for (int wid : links) {
//				if (widToIndexMap.containsKey(wid)) ++wordLinksCount;
//			}
//		}
		
		
		final List<int[]> links2 = new ArrayList<int[]>(er.links.size());
		
		// Translate wids to indices ...
		// we store as indices
		for (int[] link : er.links) {
			if (!widToIndexMap.contains(link[0]) || !widToIndexMap.contains(link[1])) {
				continue;
			}
			
			link[0] = widToIndexMap.get(link[0]);
			link[1] = widToIndexMap.get(link[1]);
			
			links2.add(link);
		}
		
		wordLinks =
			links2.toArray(new int[links2.size()][]);
		
		
		return true;
	}
	
	
	private static String strList(final int ... values) {
		final StringBuffer buffer = new StringBuffer(5 * values.length);
		for (int i = 0; i < values.length; ++i) {
			if (0 < i) {
				buffer.append(",");
			}
			buffer.append(values[i]);
		}
		return buffer.toString();
	}
	
	private boolean readAndParseData() throws IOException {

		// Download the data:
		// -- we can stream it ... but it should be at most 1mb, so
		// for now just buffer in memory
		_print(Color.YELLOW, "... now reading the wad");
		
		int i = 0;
		linkedData = new Format0.Data[linkedWids.length];
		
		// 50k is a good upper bound for data
		byte[] data = new byte[50 * 1024];
		
		final URL url = new URL(
				"http://resisttheb.org/ere/core/read_blobs.php?ids="
				+ URLEncoder.encode("[" + strList(linkedWids) + "]"));
		final DataInputStream is = new DataInputStream(new BufferedInputStream(url.openStream()));
		try {
			final int receivedLen = is.readInt();
			assert linkedWids.length == receivedLen;
			
			try {
				for (; true; ++i) {
					final int len = is.readInt();
					
					_print(Color.PINK, String.format("%d [%d]",
							i, linkedWids[i]));
					
					if (len <= 0) {
						continue;
					}
					
					if (data.length < len) {
						data = new byte[len];
					}
					
					for (int netr = 0, r; 0 < (r = is.read(data, netr, len - netr)); netr += r) {
					}
					
					final DataInputStream dis = new DataInputStream(new InflaterInputStream(
							new ByteArrayInputStream(data, 0, len)));
					try {
						linkedData[i] = format.read(dis);
					}
					finally {
						dis.close();
					}
				}
			}
			catch (EOFException e) {
				// That's fine; we expected this
			}
		}
		finally {
			is.close();
		}
		
		return true;
	}
	
	
	private boolean linkSignatures() {
		_print(Color.ORANGE, "linking!");
		
		final InputFrame[] input = new InputFrame[linkedData.length];
		for (int i = 0; i < input.length; ++i) {
			final Data data = linkedData[i];
			final InputFrame frame;
			if (null != data) {
				final Signature[] blobSigs = new Signature[data.blobs.length];
				for (int j = 0; j < data.blobs.length; ++j) {
					blobSigs[j] = data.blobs[j].sig;
				}
				frame = new InputFrame(data.mainSig, blobSigs);
			}
			else {
				frame = new InputFrame(new Signature(0, new int[0], new float[0]), new Signature[0]);
			}
			
			input[i] = frame;
		}
		
		final Linker linker = new Linker();
		linker.setAllowSelfLinks(false);
		linker.setMetric(new WeightedInverseMetric());
		linker.setInput(input);
		
		linker.link();
		
		final OutputFrame[] output = linker.getOutput();
		
//		final TLongHashSet[] linked = new TLongHashSet[output.length];
//		for (int i = 0; i < linked.length; ++i) {
//			linked[i] = new TLongHashSet(output[i].closestInputs.length);
//		}
		
		blobLinks = new int[output.length][];
		
		for (int i = 0; i < output.length; ++i) {
			
			final int[] closest = output[i].closestInputs;
			final int[] links = new int[closest.length];
			
			for (int j = 0; j < closest.length; ++j) {
				links[j] = closest[j];
			}
			blobLinks[i] = links;
		}
		
//		blobLinks = new int[output.length][];
//		for (int i = 0; i < linked.length; ++i) {
//			final int[] links = new int[linked[i].size()];
//			int j = 0;
//			for (TIntIterator itr = linked[i].iterator(); itr.hasNext(); ) {
//				links[j++] = itr.next();
//			}
//			blobLinks[i] = links;
//		}
		
		return true;
	}
	
	
	/**************************
	 * END FINISHING STEPS
	 **************************/
	
	
	

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
				"\ncomputing the graph");
		
		_print(Color.LIGHT_GRAY, "let's go!");
	}
	
	/**************************
	 * SCREEN IMPLEMENTATION
	 **************************/
	
	@Override
	public void init(final Object ... _inPackets) {
		setPackets((OutputPacket1[]) _inPackets);
	}
	
	
	@Override
	public void start() {
		printKickstartMessage();
		
		ex.submit(new Runnable() {public void run() {
			preFinish();
		}});
	}
	
	@Override
	public void stop() {
		// NOTE: why is this a security problem?
//		ex.shutdownNow();
	}
	
	@Override
	public void dispose() {
		// TODO: ??
	}
	
	/**************************
	 * END SCREEN IMPLEMENTATION
	 **************************/
	
	
	
	

	/**************************
	 * LINK EXPLORATION
	 * 
	 * These methods take a set of seed WIDs
	 * and explores around them to find a set of N wids.
	 **************************/
	
	private static final class ExplorationResults {
		public final Set<Integer> wids;
		public final List<int[]> links;
		public final TIntObjectHashMap<Object[]> itemsMap;
		
		public ExplorationResults(final Set<Integer> _wids, final List<int[]> _links,
				final TIntObjectHashMap<Object[]> _itemsMap) {
			this.wids = _wids;
			this.links = _links;
			this.itemsMap = _itemsMap;
		}
	}
	
	// this is a pseudo-random exploration.
	// if/when we exhaust the initial components, we hop to new components and explore some more
	private static ExplorationResults exploreLinks(int n, final int ... initialWids) 
		throws IOException
	{
		// 1. bfs around initial wids (maxcount=n)
		//    n -= results
		// 2. while (0 < n)
		//      select min(m, n) new items, where m = max(1,f * n);
		//      bfs around those, where max = n
		//      n -= result count
		
		final int wordsCount = getWordsCount();
		if (wordsCount < n) {
			n = wordsCount;
		}
		
		final float f 		= 0.5f;
		final float seedf 	= 0.3f;
		
		final Set<Integer> wids = new HashSet<Integer>();
		final List<int[]> links = new ArrayList<int[]>(32);
		final TIntObjectHashMap<Object[]> itemsMap = new TIntObjectHashMap<Object[]>(32);
		bfs(initialWids, wids, n, wids, links, itemsMap);
		while (wids.size() < n) {
			// Re-seed:
			final int d 	= n - wids.size();
			final int m 	= Math.max(1, (int) Math.ceil(f * d));
			assert wids.size() + m <= n;
			final int sm 	= Math.max(1, (int) Math.ceil(seedf * m));
			
			final int[] mwids = seedWids(sm, wordsCount - wids.size(), wids);
			
			if (mwids.length <= 0)
				break;
			
			bfs(mwids, wids, m, wids, links, itemsMap);
		}
		assert wids.size() == itemsMap.size();
		// Note: we may go over <code>n</code>
		// because the BFS uses a fuzzy upper limit
		
		// Take the first <code>n</code> wids according to the links
		final Set<Integer> filteredWids = new HashSet<Integer>(wids.size());
		final List<int[]> filteredLinks = new ArrayList<int[]>(links.size());
		final TIntObjectHashMap<Object[]> filteredItemsMap = new TIntObjectHashMap<Object[]>(itemsMap.size());
		
		for (int initialWid : initialWids) {
			filteredWids.add(initialWid);
		}
		for (int[] link : links) {
			final int wid0 = link[0];
			final int wid1 = link[1];
			
			if (! filteredWids.contains(wid0)) {
				if (filteredWids.size() < n)
					filteredWids.add(wid0);
				else
					continue;
			}
			if (! filteredWids.contains(wid1)) {
				if (filteredWids.size() < n)
					filteredWids.add(wid1);
				else
					continue;
			}
				
			filteredLinks.add(link);
		}
		for (int wid : filteredWids) {
			filteredItemsMap.put(wid, itemsMap.get(wid));
		}
		
		return new ExplorationResults(filteredWids, filteredLinks, filteredItemsMap);
	}
	
	// SERVER TIES
	// the graph logic is on the server, for speed
	
	private static int getWordsCount() throws IOException {
		final Map<Object, Object> map = (Map<Object, Object>) JsonCall.inlineCall(
				"http://resisttheb.org/ere/core/words_count.php"
				);
		if (1 != ((Number)map.get("success")).intValue()) {
			return -1;
		}
		return ((Number) map.get("count")).intValue();
	}
	
	private static int[] seedWids(final int count, final int wordsCount, final Set<Integer> excludes) 
		throws IOException
	{
		final List<Integer> offs = 
			EreApiUtilities.uniformExclusiveOffsets(new int[]{count}, wordsCount).get(0);
		
		final Map<Object, Object> map = (Map<Object, Object>) JsonCall.inlineGet(
				"http://resisttheb.org/ere/core/offsets_to_wids.php",
				"offs", offs,
				"excludes", excludes
				);
		
		if (1 != ((Number)map.get("success")).intValue()) {
			return null;
		}
		final List<Number> nwids = (List<Number>) map.get("wids");
		final int[] wids = new int[nwids.size()];
		int i = 0;
		for (Number nwid : nwids) {
			wids[i++] = nwid.intValue();
		}
		return wids;
	}
	
	private static boolean bfs(final int[] wids, final Set<Integer> excludes, final int maxCount,
			final Collection<Integer> updateWids, final Collection<int[]> updateLinks, 
			final TIntObjectHashMap<Object[]> itemsMap) 
		throws IOException
	{
		final List<Integer> widsList = new ArrayList<Integer>(wids.length);
		for (int wid : wids) {
			widsList.add(wid);
		}
		final Map<Object, Object> map = (Map<Object, Object>) JsonCall.inlineGet(
				"http://resisttheb.org/ere/core/bfs.php",
				"max_depth", -1,
				"max_count", maxCount,
				"wids", widsList,
				"expand_items", 1
				);
		if (1 != ((Number)map.get("success")).intValue()) {
			return false;
		}
		
		final List<Number> nwids = (List<Number>) map.get("all_wids");
		for (Number nwid : nwids) {
			updateWids.add(nwid.intValue());
		}
		
		final List<List<Number>> nlinks = (List<List<Number>>) map.get("links");
		for (List<Number> nlink : nlinks) {
			final int[] link = {
					nlink.get(0).intValue(),
					nlink.get(1).intValue()
			};
			updateLinks.add(link);
		}
		
		final Collection<Collection<Object>> nitems = (Collection<Collection<Object>>) map.get("items");
		assert nitems.size() == nwids.size();
		for (Collection<Object> c : nitems) {
			final Object[] a = c.toArray();
			itemsMap.put(((Number) a[0]).intValue(), a);
		}
		return true;
	}
	
	
	/**************************
	 * END LINK EXPLORATION
	 **************************/
	
	
	
	
	
	
	private static final class KickerThreadFactory implements ThreadFactory {
		public KickerThreadFactory() {
		}
		
		/**************************
		 * THREADFACTORY IMPLEMENTATION
		 **************************/
		
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread(r, "ENTER & RE-EXIT KICKER -- CONNECTING AND DOWNLOADING");
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
