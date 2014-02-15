package org.resisttheb.ere.ui;

import gnu.trove.TIntArrayList;

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Container;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.resisttheb.ere.ui0.EntryScreen;
import org.resisttheb.ere.ui0.InPacket0;
import org.resisttheb.ere.ui1.OutputPacket1;
import org.resisttheb.ere.ui1.ProcessingScreen;
import org.resisttheb.ere.ui2.KickerScreen;
import org.resisttheb.ere.ui3.ExploreScreen;




// this class orchestrates the entire process
//
// starts with ui0, then ui1, then ui2 (the main vis)
//
//
// TODO: on init, call a function in context

public class Ere extends JApplet implements ScreenHandler {

// threads for all steps
	
	// the binary format for the server.
	// think of this as an interface, but it's not
	
	public static boolean DEBUG = false;
	static {
		assert DEBUG = true;
	}
	
	public static boolean IN_APPLET = false;
	

	/*
	 * DEFAULT USER INFO
	 */
	private static final String DEFAULT_PID 			= "214";
	private static final String DEFAULT_SECURITY_KEY 	= "6BgJN2FYVTVBAPHYqjJYjOAoVYYVIUDO";
	private static final String DEFAULT_SEEDS			= "2099,2098,2097,2096,2071,2070,2069,2068,2067,2066";
//	private static final String DEFAULT_SEEDS			= null;
	
	
	// current user information:
	private int pid 			= -1;
	private String securityKey 	= null;
	
	
	
	// the last known output packets
	//     for each of the screens
	private Map<Integer, Object[]> outPacketsMap = new HashMap<Integer, Object[]>(4);
	
	
	private int screenIndex 	= -1;
	private Screen screen 		= null;
	
	private ScreenManager manager;
	
	
	public Ere() {
		setManager(new DefaultScreenManager());
	}
	

	private void setManager(final ScreenManager _manager) {
		// TODO: dispose old
		this.manager = _manager;
	}
	
	
	/**************************
	 * JAVASCRIPT INTERFACE
	 **************************/
	
	public void ERE_reset() {
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			reset();
		}});
	}
	
	public void ERE_setSeeds(final Object[] _seeds) {
		final Object[] seeds = null == _seeds ? new Object[0] : _seeds;
		
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			// if seeds, create a new screen manager for the given seeds,
			// if no seeds, create a default screen manager
			final ScreenManager _manager;
			if (seeds.length <= 0) {
				_manager = new DefaultScreenManager();
			}
			else {
				final TIntArrayList widList = new TIntArrayList(seeds.length);
				for (Object seed : seeds) {
					final String s = String.valueOf(seed);
					try {
						widList.add(Integer.parseInt(s));
					}
					catch (NumberFormatException e) {
						// Consume it
					}
				}
				_manager = new SeededScreenManager(widList.toNativeArray());
			}
			setManager(_manager);
			
//			reset();
		}});
	}
	
	public void ERE_setUser(final int _pid, final String _securityKey) {
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			Ere.this.pid = _pid;
			Ere.this.securityKey = _securityKey;
		}});
	}
	
	public void ERE_clearUser() {
	}
	
	
	private void status(final String key) {
		try {
			final AppletContext context = getAppletContext();
			if (null != context) {
				try {
					context.showDocument(new URL(
							String.format("javascript:ERE_%s()", key)), "_self");
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			// Consume ...
		}
	}
	
	private boolean testInApplet() {
		try {
			return null != getAppletContext();
		}
		catch (Throwable t) {
			return false;
		}
	}
	
	/**************************
	 * END JAVASCRIPT INTERFACE
	 **************************/
	
	
	private void reset() {
		outPacketsMap.clear();
		firstScreen();
	}
	
	
	/**************************
	 * JAPPLET OVERRIDES
	 **************************/
	
	@Override
	public void init() {
		IN_APPLET = testInApplet();
		
		try {
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
		}
		catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		// See: http://forum.java.sun.com/thread.jspa?threadID=294436&messageID=1165554
		UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());

		// If this needed?
		getRootPane().putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);
		
		status("initialized");
		
		
		// LAYOUT AND UI SETUP
		setLayout(new BorderLayout());
	}
	
	@Override
	public void start() {
		// FOR SAFETY:
		deactivateScreen();
		
		attemptParamReset(true);
	}
	
	@Override
	public void stop() {
		deactivateScreen();
	}
	
	/**************************
	 * END JAPPLET OVERRIDES
	 **************************/
	
	
	private boolean isAppletAccess() {
		try {
			getParameter("xxx");
			return true;
		}
		catch (Throwable t) {
			return false;
		}
	}
	

	private boolean attemptParamReset(final boolean forceDefaults)  {
		final boolean appletAccess = !forceDefaults && isAppletAccess();
		if (appletAccess && !"1".equals(getParameter("reset"))) {
			return false;
		}
		
//		'user_pid': pid,
//		'security_key' : securityKey,
//		'seeds' : !seeds ? '' : seeds.join(','),
		
		int pid 				= -1;
		String securityKey 		= null;
		Object[] seeds 			= null;
		
		final String pidStr = appletAccess ? getParameter("user_pid") : DEFAULT_PID;
		if (null != pidStr) {
			try {
				pid = Integer.parseInt(pidStr);
			}
			catch (NumberFormatException e) {
				// Consume it
			}
		}
		
		securityKey = appletAccess ? getParameter("security_key") : DEFAULT_SECURITY_KEY;
		
		String seedsStr = appletAccess ? getParameter("seeds") : DEFAULT_SEEDS;
		if (null != seedsStr && 0 < seedsStr.length()) {
			if (DEBUG) {
				System.out.println(String.format("##%s##", seedsStr));
			}
			final String[] seedsStrs = seedsStr.split(",");
			seeds = new Object[seedsStrs.length];
			for (int i = 0; i < seedsStrs.length; ++i) {
				try {
					seeds[i] = Integer.parseInt(seedsStrs[i]);
				}
				catch (NumberFormatException e) {
					// Consume it
				}
			}
		}
		
		ERE_setUser(pid, securityKey);
		ERE_setSeeds(seeds);
		ERE_reset();
		
		return true;
	}
	
	
	

	private void deactivateScreen() {
		if (null == screen) {
			return;
		}
		
		try {
			screen.stop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		
		remove(screen.getDisplay());
		screen.setScreenHandler(null);
		screen.dispose();
		
		screenIndex = -1;
		screen = null;
	}
	
	private void activateScreen(final int _screenIndex) {
		activateScreen(_screenIndex, manager.createScreen(_screenIndex));
	}
	
	private void activateScreen(final int _screenIndex, final Screen _screen) {
		deactivateScreen();
		
		this.screenIndex = _screenIndex;
		this.screen = _screen;
		
		
		add(screen.getDisplay());
		validate();
		
		try {
			screen.setScreenHandler(this);
			screen.init(manager.createInputPackets(screenIndex));
			screen.start();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	

	
	
	private void firstScreen() {
		activateScreen(0);
	}
	
	private void nextScreen() {
		if (manager.hasNextScreen(screenIndex)) {
			activateScreen(screenIndex + 1);
		}
		else {
			// Reset:
			firstScreen();
		}
	}
	
	
	
	
	/**************************
	 * SCREENHANDLER IMPLEMENTATION
	 **************************/
	
	public void requestReset() {
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			firstScreen();
		}});
	}
	
	public void finish(final Object[] outPackets) {
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			outPacketsMap.put(screenIndex, outPackets);
			
			nextScreen();
		}});
	}
	
	/**************************
	 * END SCREENHANDLER IMPLEMENTATION
	 **************************/
	
	
	
	
	
	private static interface ScreenManager {
		public Object[] createInputPackets(final int index);
		public boolean hasPreviousScreen(final int index);
		public boolean hasNextScreen(final int index);
		public Screen createScreen(final int index);
	}
	
	private class DefaultScreenManager implements ScreenManager {
		public DefaultScreenManager() {
		}
		
		/**************************
		 * <code>ScreenManager</code> IMPLEMENTATION
		 **************************/
		
		public Object[] createInputPackets(final int index) {
			Object[] packets = outPacketsMap.get(index - 1);
			
			if (null != packets)
				return packets;
				
			
			if (index <= 0) {
				// Create the first input packet:
				packets = new InPacket0[]{
					new InPacket0(pid, securityKey)
				};
			}
			
			return packets;
		}
		
		public boolean hasPreviousScreen(final int index) {
			return 0 < index;
		}
		
		public boolean hasNextScreen(final int index) {
			return index < 3;
		}
		
		public Screen createScreen(final int index) {
			switch (index) {
				case 0: return new EntryScreen();
				case 1: return new ProcessingScreen();
				case 2: return new KickerScreen();
				case 3: return new ExploreScreen();
				default:
					throw new IllegalArgumentException();
			}
		}
		
		/**************************
		 * END <code>ScreenManager</code> IMPLEMENTATION
		 **************************/
	}
	
	private class SeededScreenManager implements ScreenManager {
		private final int[] wids;
		
		public SeededScreenManager(final int ... _wids) {
			this.wids = _wids;
		}
		
		/**************************
		 * <code>ScreenManager</code> IMPLEMENTATION
		 **************************/
		
		public Object[] createInputPackets(final int index) {
			Object[] packets = outPacketsMap.get(index - 1);
			
			if (null != packets)
				return packets;
				
			
			if (index <= 0) {
				// Create the first input packet:
				packets = new OutputPacket1[]{
					new OutputPacket1(wids)
				};
			}
			
			return packets;
		}
		
		public boolean hasPreviousScreen(final int index) {
			return 0 < index;
		}
		
		public boolean hasNextScreen(final int index) {
			return index < 1;
		}
		
		public Screen createScreen(final int index) {
			switch (index) {
				case 0: return new KickerScreen();
				case 1: return new ExploreScreen();
				default:
					throw new IllegalArgumentException();
			}
		}
		
		/**************************
		 * END <code>ScreenManager</code> IMPLEMENTATION
		 **************************/
	}
	
	
	
	
	
	
	public static void main(final String[] in) {
		final Ere ere = new Ere();
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		
		c.setLayout(new BorderLayout());
		c.add(ere, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		ere.init();
		ere.start();
//		ere.ERE_setSeeds(new String[]{"1191", "1190"});
		ere.ERE_setSeeds(new String[0]);
		ere.ERE_setUser(37, "3EdGdLGdGP8MCELgxXTnOzyNMqi5zYa2");
		ere.ERE_reset();
//		ere.ERE_begin();
	}

}
