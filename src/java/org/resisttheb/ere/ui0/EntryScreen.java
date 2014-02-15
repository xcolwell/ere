package org.resisttheb.ere.ui0;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import net.miginfocom.layout.LC;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingEventListener;
import org.jdesktop.animation.timing.TimingSource;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.SplineInterpolator;
import org.resisttheb.ere.ui.AbstractScreen;
import org.resisttheb.ere.ui.EreApiUtilities;
import org.resisttheb.ere.ui.FontUtilities;
import org.resisttheb.ere.ui.JsonCall;
import org.resisttheb.ere.ui.JsonCall.Callback;
import org.resisttheb.nug.noise.PerlinNoiseGenerator;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.NoiseEvalState;
import org.resisttheb.nug.noise.PerlinNoiseGenerator.SmoothingFunction;

// - each row has an 
//       . error status
//       . component
//       . setEnabled(.)
//       . slide(u)
//       . (question, location, id)
//       . progress
//
//
//
// TODO: still need a screen to collect name of visitor
// TODO: should pop over this screen if needed.
// TODO: should just post info back
public class EntryScreen extends AbstractScreen {
	private static enum ImageType {
		SEARCH,
		FILE
	}
	
	
	/*
	 * TODO: we should flip these on later. For now, we just do search.
	 */
	private static final boolean ENABLE_INPUT_OPTIONS = false;
	
	private static final float DEFAULT_MAIN_SIZE = 24.f;
	private static final float DEFAULT_LOC_SIZE = 16.f;
	
	private static final int ROW_COUNT = 4;
	
	private static final int SEARCH_N = 5;

	
	
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static String createHeaderStr(final Prompt prompt) {
		final Prompt.Parent[] parents = prompt.parents;
		
		if (parents.length <= 0) {
			return "What's on your mind?";
		}
		
		int netLen = 0;
		for (Prompt.Parent parent : parents) {
			netLen += parent.text.length();
		}
		
		final StringBuffer buffer = new StringBuffer(netLen + parents.length * 4);
		
		for (int i = 0; i < parents.length; ++i) {
			if (0 < i) {
				buffer.append(" & ");
			}
			buffer.append(parents[i].text);
		}
		
		return buffer.toString();
	}
	
	private static String createLocationStr(final Prompt prompt) {
		final Prompt.Parent[] parents = prompt.parents;
		
		if (parents.length <= 0) {
			return "";
		}
		
		int netLen = 0;
		for (Prompt.Parent parent : parents) {
			netLen += parent.loc.length() + parent.name.length() + 2;
		}
		
		final StringBuffer buffer = new StringBuffer(2 + netLen + parents.length * 4);
		buffer.append("(");
		
		for (int i = 0; i < parents.length; ++i) {
			if (0 < i) {
				buffer.append(" & ");
			}
			buffer.append(parents[i].name);
			buffer.append("@");
			buffer.append(parents[i].loc);
		}
		
		buffer.append(")");
		
		return buffer.toString();
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	
	// -- screen progress --
	private void FINISHED() {
		// TODO: before we fire the event,
		// TODO: we need to check if a name/loc has been collected
		// TODO: if not, pop the dialog
		
		// fire an event to move onto the next stage.
		// we need to pass off our data packet,
		// which has the N ids we've collected here
	}
	
	
	/**************************
	 * JAVASCRIPT CALLBACKS
	 * 
	 * This interface is exposed to JS clients.
	 **************************/
	
	public void ERE_setError(final int id, final String message) {
	}
	

	public void ERE_setProgress(final int id, final float u) {
	}
	
	public void ERE_finish(final int id) {
		
	}
	
	/**************************
	 * END JAVASCRIPT CALLBACKS
	 **************************/
	
	
	
	

	private final float UPDATES_PER_SECOND = 8.f;
	
	private final int LINE_SPACE = 7;
	
	private final float LOGO_X = 2.f;
	private final float LOGO_Y = 2.f;
	
	private final int MAX_WIDTH = 600;
	

	private int linex = 107;
	private int liney = 60;
	
	private int N = 0;
	private Prompt[] prompts = new Prompt[0];
	
	private InPacket0[] inPackets = new InPacket0[0];
	
	
	// a common clock.
	private final SwingTimingSource timingSource = new SwingTimingSource(UPDATES_PER_SECOND);
	
	private Row[] rows = new Row[0];
	private Overlay overlay;
	
	private JPanel rowPanel;
	
	
	public EntryScreen() {
		initTiming();
		initUi();
		initListeners();
		initOthers();
		
		// note: the client must call #start
	}
	
	
	private void initTiming() {
		// Paint on trigger:
		timingSource.addEventListener(new TimingEventListener() {
			@Override
			public void timingSourceEvent(final TimingSource src) {
				repaint();
			}
		});
		
		
		
		
		//  timingSource.start();
	}
	
	private void initUi() {
		// layered pane
		// 
		
		// 1. create rows
		//    (one at a time. use the pref heigh of each to position the next)
		// 2. add rows to panel
		// 3. create overlay 
		//    add overlay to panel
		// 4. add panel to self
		
		setBackground(Color.BLACK);
		
		
		rowPanel = new JPanel();
		rowPanel.setOpaque(false);
		rowPanel.setLayout(null);
		
		overlay = new Overlay();

		final JLayeredPane pane = new JLayeredPane() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(x, y, width, height);
				
				for (Component child : getComponents()) {
					child.setBounds(0, 0, width, height);
				}
			}
		};
		pane.add(rowPanel, new Integer(1));
		pane.add(overlay, new Integer(0));
		
		
		setLayout(new BorderLayout());
		add(pane, BorderLayout.CENTER);
	}
	
	
	private void initListeners() {
		final MouseInputListener mil = new MouseInputAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				final int ai = findActiveRow();
				if (0 <= ai) {
					rows[ai].focus();
				}
			}
		};
		
		addMouseListener(mil);
		addMouseMotionListener(mil);
	}
	
	
	private void initOthers() {
//		for (int i = 0; i < rows.length; ++i) {
//			overlay.on(i);
//		}
	}
	
	
	
	
	private void finish(final int id) {
		rows[id].finished = true;
		rows[id].off();
		overlay.off(id);
		
		if (isAllFinished()) {
			schedule(2000, new Runnable() {public void run() {
				final OutPacket0[] outPackets = new OutPacket0[N];
				for (int i = 0; i < N; ++i) {
					outPackets[i] = new OutPacket0(rows[i].color, prompts[i],
							rows[i].getText());
				}
				sh.finish(outPackets);
			}});
		}
	}
	
	
	
	
	// simple internal validation
	private String validateRow(final Row row) {
		String text = row.text.getText();
		text = text.trim();
		if (text.length() <= 0) {
			return "u gotta say something";
		}
		return null;
	}
	
	
	// submits the content for the given id
	// 
	private void submit(final int id) {
		final Row row = rows[id];
		final String validateError = validateRow(row);
		if (null != validateError) {
			row.markError(validateError);
			return;
		}
		
		overlay.on(id);
		// this should also clear the error ...
		row.markInProgress();
		//row.clearError();
		row.off();
		
		
		switch (row.getImageType()) {
			case SEARCH:
				startSearch(id);
				break;
			case FILE:
				throw new IllegalStateException();
				//passControlForFile(id);
//				break;
		}
		
		
		// clear error
		
		// TODO: if file, call some javascript to upload file,
		// TODO: else just add a job to the queue that calls our webservice
		// if file, then control goes to javascript to call setProgress
		
		// start an animation to slide in row
		next();
	}
	
	
	private void startSearch(final int id) {
		final Row row = rows[id];
		final Prompt prompt = prompts[id];
		
		final String text = rows[id].getText();
		
		final Callback cb = new Callback() {public void run(final Object jsonObj) {
			final int success = ((Number) ((Map<?, ?>) jsonObj).get("success")).intValue();
			
			if (0 == success) {
				row.markError("try painting more word pictures");
				next();
			}
			else {
				finish(id);
			}
		}};
		
		JsonCall.call(cb,
				"http://resisttheb.org/ere/core/pixels_from_query.php",
				"id", prompt.wid,
				"key", prompt.securityKey,
				"query", text,
				"n", SEARCH_N
			);
	}
	
	
	
	public void next() {
		if (! slideNextRow()) {
			FINISHED();
		}
	}
	
	private boolean slideNextRow() {
		// 1. scan for a row that is not finished
		// pop it out (row.on(), row.off())
		
		final int nextId = findNextRow();
		
		if (nextId < 0) {
			return false;
		}
		
		overlay.off(nextId);
		// this should set active ...
		final Row row = rows[nextId];
		row.on();
		
		SwingUtilities.invokeLater(new Runnable() {public void run() {
			row.focus();
		}});
		
		return true;
	}
	
	private int findNextRow() {
		// if there is currently an active row, return it
		final int ai = findActiveRow();
		if (0 <= ai)
			return ai;
		
		// find the first not in progress
		for (int i = 0; i < rows.length; ++i) {
			if (! rows[i].inProgress)
				return i;
		}
		return -1;
	}
	
	private int findActiveRow() {
		for (int i = 0; i < rows.length; ++i) {
			if (rows[i].active)
				return i;
		}
		return -1;
	}
	
	
	private boolean isAllFinished() {
		for (int i = 0; i < rows.length; ++i) {
			if (! rows[i].finished)
				return false;
		}
		return true;
	}
	
	
	/**************************
	 * PROPERTIES
	 **************************/
	
	private int maxWidth() {
		return MAX_WIDTH;
	}
	
	/**************************
	 * END PROPERTIES
	 **************************/
	
	// generates a new parent count from the distribution
	private static int parentDist() {
		// 80% one parent
		// 16% 2 parents
		// 3% 3 parent
		// 1% 4 parent
		
		final int r = (int) Math.floor(100 * Math.random());
		
		if (r < 70) return 1;
		if (r < 85) return 2;
//		if (r < 99) return 3;
//		if (r < 100) return 4;
		if (r < 100) return 0;
		assert false;
		return 1;
	}
	
	private static List<List<Integer>> createParentOffs(final int count, int wordsCount) {
		final int[] sizes = new int[count];
		for (int i = 0; i < count; ++i) {
			sizes[i] = parentDist();
		}
		
		return EreApiUtilities.uniformExclusiveOffsets(sizes, wordsCount);
	}
	
	private static Prompt createPrompt(final List<?> jsonList) {
		// [wid, key, [[parent_id, parent name, parent loc]+]
		
		final int wid = ((Number) jsonList.get(0)).intValue();
		final String securityKey = (String) jsonList.get(1);
		
		final List<?> parentsList = (List<?>) jsonList.get(2);
		final Prompt.Parent[] parents = new Prompt.Parent[parentsList.size()];
		for (int i = 0; i < parentsList.size(); ++i) {
			final List<?> parentAsList = (List<?>) parentsList.get(i);
			final int parentWid = ((Number) parentAsList.get(0)).intValue();
			final String name = (String) parentAsList.get(1);
			final String loc = (String) parentAsList.get(2);
			final String text = (String) parentAsList.get(3);
			parents[i] = new Prompt.Parent(parentWid, text, loc, name);
		}
		
		return new Prompt(wid, securityKey, parents);
	}
	
	
	private void initPrompts() {
		// 1. alloc prompts on server
		// 2. setPrompts to the return
		// 3. on each of the overlay rows in some random order
		// 4. at the end, next()
		
		// TODO: implements submit logic
		
		final Callback cb1 = new Callback() {public void run(final Object jsonObj) {
			// return from "alloc_prompts"
			
			final List<?> array = (List<?>) ((Map<?, ?>) jsonObj).get("info");
			final int N = array.size();
			
			final Prompt[] prompts = new Prompt[N];
			for (int i = 0; i < N; ++i) {
				prompts[i] = createPrompt((List<?>) array.get(i));
			}
			
			setPrompts(prompts);
			fadeInPrompts();
		}};
		
		final Callback cb0 = new Callback() {public void run(final Object jsonObj) {
			// return from "get_words"
			
			final List<List<Integer>> parentOffs = createParentOffs(
					ROW_COUNT,
					((Number) ((Map<?, ?>) jsonObj).get("count")).intValue()
					);
			
			final InPacket0 inp = inPackets[0];
			
			JsonCall.call(cb1,
				"http://resisttheb.org/ere/core/alloc_prompts.php",
				"pid", inp.pid,
				"p_key", inp.securityKey,
				"p_offs", parentOffs
			);
		}};
		
		JsonCall.call(cb0,
			"http://resisttheb.org/ere/core/words_count.php"
		);
		
	}
	
	
	private void setPrompts(final Prompt ... _prompts) {
		this.prompts = _prompts;
		N = prompts.length;
		
		final ColorWalk cw = new ColorWalk();
		final Color[] colors = cw.rspaced(4);
		
		// ROWS
		
		rowPanel.removeAll();
		
		rows = new Row[N];
		int y = liney;
		for (int i = 0; i < N; ++i) {
			final Prompt prompt = prompts[i];
			
			final Row row = new Row(i, colors[i], 
					createHeaderStr(prompt), createLocationStr(prompt));
			rows[i] = row;

			rowPanel.add(row);
			row.validate();
			final Dimension d = row.getPreferredSize();
			row.setBounds(0, 0, d.width, d.height);
			
			row.line = y;
			row.slide(0);
			
			
			y += LINE_SPACE + row.getPreferredSize().height;
		}
		
		overlay.reserve();
		
		validate();
	}
	
	
	private void fadeInPrompts() {
		final int[] on = {0};
		
		for (int i = 0; i < N; ++i) {
			final int _i = i;
			schedule(250 + (int) Math.round(2000 * Math.random()),
				new Runnable() {public void run() {
					overlay.on(_i);
					
					++on[0];
					if (N <= on[0]) {
						schedule(500, new Runnable() {public void run() {
							next();
						}});
					}
				}}
			);
		}
	}
	
	
	
	private void schedule(final int delay, final Runnable r) {
		final Timer timer = new Timer(delay, new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				r.run();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	
	
	/**************************
	 * SCREEN IMPLEMENTATION
	 **************************/
	
	@Override
	public void init(Object ... _inPackets) {
		this.N = _inPackets.length;
		this.inPackets = new InPacket0[N];
		for (int i = 0; i < _inPackets.length; ++i) {
			inPackets[i] = (InPacket0) _inPackets[i];
		}
		
		initPrompts();
	}
	
	
	@Override
	public void start() {
		timingSource.sstart();
	}
	
	@Override
	public void stop() {
		timingSource.sstop();
	}
	
	@Override
	public void dispose() {
		// We don't have anything now ...
	}
	
	/**************************
	 * END SCREEN IMPLEMENTATION
	 **************************/
	
	
	
	
	
	
	
	private class Row extends JPanel {
		private final int ON_TIME = 1000;
		private final int OFF_TIME = 1000;
		
		private final SplineInterpolator ON_SPLINE = new SplineInterpolator(0.00f, 1.00f, 1.00f, 1.00f);
		private final SplineInterpolator OFF_SPLINE = new SplineInterpolator(0.00f, 0.00f, 1.00f, 0.36f);
		
		
		//
		// to be set by client
		private Animator anim;
		public boolean inProgress;
		public boolean active = false;
		//
		
		private int line = 0;
		
		
		public final int id;
		
		public float progress;
		public boolean finished;
		
		public boolean error;
		public String message = "pending";
		
		
		public final Color color;
		
		
		// ALL COMPONENTS
		private JLabel questionLabel;
		private JLabel locLabel;
		private JTextField text;
		private JButton button;
		private JRadioButton optSearch;
		private JRadioButton optUpload;
		
		
		
		private float slideu = 0;
		
		
		
		public Row(final int _id,
				final Color _color, final String _q, final String _loc
		) {
			this.id = _id;
			this.color = _color;
			
			initComponents(_q, _loc, maxWidth());
			initListeners();
			
			setOpaque(false);
		}

		
		private void initComponents(final String q, final String loc, final int maxw) {
			final Border b = new CompoundBorder(
					new MatteBorder(0, 0, 0, 0, color),
					new EmptyBorder(2, 20, 2, 20)
			);
			
			float mainSize = DEFAULT_MAIN_SIZE;
			float locSize = DEFAULT_LOC_SIZE;
			setBorder(b);
			setOpaque(false);
			
			
			questionLabel = new JLabel(q);
			questionLabel.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(mainSize));
			questionLabel.setForeground(color);
			
			locLabel = new JLabel(loc);
			locLabel.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(locSize));
			locLabel.setForeground(color);
			
			text = new JTextField();
			text.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(14.f));			
			text.setCaretColor(color);
			text.setSelectionColor(color);
			text.setSelectedTextColor(Color.BLACK);
			text.setOpaque(false);
			text.setForeground(Color.WHITE);
			
			button = new JButton("ok");
			button.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(14.f));
			button.setOpaque(true);
			button.setForeground(Color.BLACK);
			
			optSearch = new JRadioButton("search");
			optSearch.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(14.f));
			optUpload = new JRadioButton("upload");
			optUpload.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(14.f));
			optSearch.setOpaque(false);
			optUpload.setOpaque(false);
			optSearch.setForeground(Color.WHITE);
			optUpload.setForeground(Color.WHITE);
			
			final ButtonGroup bg = new ButtonGroup();
			bg.add(optSearch);
			bg.add(optUpload);
			
			
			// DEFAULT:
			optSearch.setSelected(true);
			
			
			// =========================
			// LAYOUT
			
			final MigLayout ml = new MigLayout();
			final LC lcs = new LC();
			lcs.setInsets(new UnitValue[]{
					new UnitValue(2.f),
					new UnitValue(2.f),
					new UnitValue(2.f),
					new UnitValue(2.f)
				});
			ml.setLayoutConstraints(lcs);
			setLayout(ml);
			
			
			add(questionLabel, "width 300::, split 2");
			add(locLabel, "gapleft 20, wrap");
			add(text, "growx, split 2");
			add(button, "gapleft 15, wrap");
			if (ENABLE_INPUT_OPTIONS) {
				add(optSearch, "split 2, align left");
				add(optUpload);
			}
			
			// END LAYOUT
			// =========================
			
			
			validate();
			while (12 < mainSize && maxw < getPreferredSize().width) {
				--mainSize;
				questionLabel.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(mainSize));
				if (10 < locSize) {
					--locSize;
					locLabel.setFont(FontUtilities.ERAS_MEDIUM.deriveFont(locSize));
				}
				
				validate();
			}
		}
		
		
		private void initListeners() {
			final ActionListener al = new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					submit(id);
				}
			};
			
			button.addActionListener(al);
			text.addActionListener(al);
		}
		
		
		
		public String getText() {
			return text.getText();
		}
		
		public ImageType getImageType() {
			if (optSearch.isSelected())
				return ImageType.SEARCH;
			if (optUpload.isSelected())
				return ImageType.FILE;
			return ImageType.SEARCH;
		}
		
		
		
		/**************************
		 * STATE MOVEMENT
		 **************************/
		
		public void markInProgress() {
			inProgress = true;
			error = false;
			message = "in progress";
		}
		
		public void markError(final String _message) {
			inProgress = false;
			error = true;
			message = _message;
		}
		
		public void focus() {
			text.requestFocus();
		}
		
		/**************************
		 * END STATE MOVEMENT
		 **************************/
		
		
		public void enableRow() {
			setRowEnabled(true);
		}
		
		public void disableRow() {
			setRowEnabled(false);
		}
		
		private void setRowEnabled(final boolean en) {
			text.setEnabled(en);
			button.setEnabled(en);
			optSearch.setEnabled(en);
			optUpload.setEnabled(en);
		}
		
		
		private void activate() {
			setActive(true);
		}
		
		private void deactivate() {
			setActive(false);
		}
		
		private void setActive(final boolean _active) {
			this.active = _active;
		}
		
		private void slide(final float u) {
			slideu = u;
			setVisible(0 != u);
			
			// Sync the position with the reality ...
			final int x0 = -getWidth();
			final int x1 = linex;
			
			setLocation(Math.round(x0 + (x1 - x0) * u), line); 
		}
		
		
		
		public void on() {
			if (null != anim) {
				anim.stop();
			}
			final TimingTarget target = new TimingTarget() {
				public void begin() {
				}

				public void end() {
				}

				public void repeat() {
				}

				public void timingEvent(final float u) {
					slide(u);
				}
			};
			anim = new Animator((int) (ON_TIME * (1.f - slideu)),
					target);
			anim.setInterpolator(ON_SPLINE);
			anim.setStartFraction(slideu);
			anim.setStartDirection(Animator.Direction.FORWARD);
			anim.setTimer(timingSource);
			
			activate();
			enableRow();
			
			anim.start();
		}
		
		public void off() {
			if (null != anim) {
				anim.stop();
			}
			final TimingTarget target = new TimingTarget() {
				public void begin() {
				}

				public void end() {
				}

				public void repeat() {
				}

				public void timingEvent(final float u) {
					slide(u);
				}
			};
			anim = new Animator((int) (OFF_TIME * (slideu)),
					target);
			anim.setInterpolator(OFF_SPLINE);
			anim.setStartFraction(slideu);
			anim.setStartDirection(Animator.Direction.BACKWARD);
			anim.setTimer(timingSource);
			anim.start();
			
			deactivate();
			disableRow();
		}
		
	}
	
	
	// draws logo
	// draws status logos next to non-showing rows
	//    (each row has a status alpha and animator -- status fades in and out)
	private class Overlay extends JComponent {
		private final int ON_TIME = 1000;
		private final int OFF_TIME = 1000;
		
		private final SplineInterpolator ON_SPLINE = new SplineInterpolator(0.00f, 1.00f, 1.00f, 1.00f);
		private final SplineInterpolator OFF_SPLINE = new SplineInterpolator(0.00f, 0.00f, 1.00f, 0.36f);
		
		
		private float[] statusAlpha;
		private Animator[] statusAnimators;
		
		private LogoWave wave;
		
		private BufferedImage dirImg;
		
		
		public Overlay() {
			reserve();

			wave = EntryScreen.this.new LogoWave();
			setOpaque(false);
			
//			initDirImg();
		}
		
		
		private void initDirImg(final Graphics2D g2d) {
			try {
				final InputStream is = EntryScreen.class.getResourceAsStream("/dir_scale.png");
				try {
					BufferedImage preDirImg = ImageIO.read(is);
					dirImg = new BufferedImage(preDirImg.getWidth(), preDirImg.getHeight(),
							BufferedImage.TYPE_INT_ARGB);
//						g2d.getDeviceConfiguration().createCompatibleImage(
//							preDirImg.getWidth(), preDirImg.getHeight(), Transparency.TRANSLUCENT);
					final Graphics2D dig2d = (Graphics2D) dirImg.getGraphics();
					dig2d.drawImage(preDirImg, 0, 0, 
							preDirImg.getWidth(), preDirImg.getHeight(), null);
					dig2d.dispose();
				}
				finally {
					is.close();
				}
			}
			catch (IOException e) {
			}
		}
		
		
		public void reserve() {
			// Stop all existing animators:
			if (null != statusAnimators) {
				for (Animator anim : statusAnimators) {
					anim.stop();
				}
			}
			
			statusAlpha = new float[N];
			statusAnimators = new Animator[N];
			
			for (int i = 0; i < N; ++i) {
				statusAlpha[i] = 0.f;
			}
		}
		
		
		public void on(final int id) {
			Animator anim = statusAnimators[id];
			if (null != anim) {
				anim.stop();
			}
			final TimingTarget target = new TimingTarget() {
				public void begin() {
				}

				public void end() {
				}

				public void repeat() {
				}

				public void timingEvent(final float u) {
					statusAlpha[id] = u;
				}
			};
			anim = new Animator((int) (ON_TIME * (1.f - statusAlpha[id])),
					target);
			anim.setInterpolator(ON_SPLINE);
			anim.setStartFraction(statusAlpha[id]);
			anim.setStartDirection(Animator.Direction.FORWARD);
			anim.setTimer(timingSource);
			statusAnimators[id] = anim;
			anim.start();
		}
		
		public void off(final int id) {
			Animator anim = statusAnimators[id];
			if (null != anim) {
				anim.stop();
			}
			final TimingTarget target = new TimingTarget() {
				public void begin() {
				}

				public void end() {
				}

				public void repeat() {
				}

				public void timingEvent(final float u) {
					statusAlpha[id] = u;
				}
			};
			anim = new Animator((int) (OFF_TIME * (statusAlpha[id])),
					target);
			anim.setInterpolator(OFF_SPLINE);
			anim.setStartFraction(statusAlpha[id]);
			anim.setStartDirection(Animator.Direction.BACKWARD);
			anim.setTimer(timingSource);
			statusAnimators[id] = anim;
			anim.start();
		}
		
		
		/**************************
		 * JCOMPONENT OVERRIDES
		 **************************/
		
		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			final Graphics2D g2d = (Graphics2D) g;
			
			
			// DIRECTION IMAGE:
			if (null == dirImg) {
				initDirImg(g2d);
			}
			g2d.drawImage(dirImg, 
					getWidth() - dirImg.getWidth() - 30,
					getHeight() - dirImg.getHeight() - 30,
					dirImg.getWidth(), dirImg.getHeight(),
					null
					);
			
			
			g2d.setFont(FontUtilities.ERAS_LIGHT.deriveFont(16.f));
			for (int i = 0; i < rows.length; ++i) {
				final float alphau = statusAlpha[i];
				final Row row = rows[i];
				
				if (row.error) {
					
					if (null == row.message)
						continue;
					
					final float x = linex;
					final float y = row.active
						? row.getY()
						: row.getY() + row.getHeight() / 2.f;
					
					final int alpha = 150;
					
						g2d.setPaint(new Color(0xFF, 0, 0, alpha));
					
					
					//g2d.fill(new Rectangle2D.Float(x, y, 20, 20));
					g2d.drawString("!  " + row.message, x, y);
				}
				else {
					if (0 == alphau)
						continue;
					
					if (null == row.message)
						continue;
					
					final float x = linex;
					final float y = row.getY() + row.getHeight() / 2.f;
					
					final int alpha = (int) (0xFF * alphau);
					
						// TODO: replace with meaningful status
					g2d.setPaint(new Color(
							row.color.getRed(), 
							row.color.getGreen(),
							row.color.getBlue(), 
							alpha));
					
					//g2d.fill(new Rectangle2D.Float(x, y, 20, 20));
					g2d.drawString(row.message, x, y);
				}
			}
			
			
			final AffineTransform tat = g2d.getTransform();
			final AffineTransform at = new AffineTransform(tat);
			at.translate(LOGO_X, LOGO_Y);
			g2d.setTransform(at);
			
			wave.paint(g2d);
			
			g2d.setTransform(tat);
		}
		
		/**************************
		 * END JCOMPONENT OVERRIDES
		 **************************/
		
	}
	
	
	
	
	// Logo wave --
	// goes on left side of screen,
	// is the logo. Just waves around
	private class LogoWave {
		public int w;
		public int h;
		
		private int[] colors;
		private BufferedImage out;
		
		
		private float sx = 1.5f * 0.3773f / 11.f;
		private float sy = 1.5f * 0.3773f / 11.f;
		private float za = 0.133117f;
		private float zb = 0.2f * 0.331177434f;
//		{
//			zb /= UPDATES_PER_SECOND;
//		}
		
		// NOISE STATE: {{{
		private PerlinNoiseGenerator ngen;
		private NoiseEvalState nes;
		// }}}
		
		private int incr = 0;
		
		private Timer timer = null;
		
		
		public LogoWave() {
			initNoise();
			initBuffers();
			
			// Put something in the output buffer:
			updateOutput();
			
			// HOOK UP TO TIMER:
			initTimer();
		}
		
		
		private void initNoise() {
			// 5 bits of noise
			ngen = new PerlinNoiseGenerator(3, 5);
			nes = ngen.createEvalState();
		}
		
		private void initBuffers() {
			// 1. load image
			// 2. create colors
			// 3. create output buffer
			
			BufferedImage logoBuffer = loadLogo();
			if (null == logoBuffer) {
				// TODO: should just use a blank logo
				throw new IllegalStateException();
			}
			
			w = logoBuffer.getWidth();
			h = logoBuffer.getHeight();
			
			colors = new int[w * h];
			for (int x = 0; x < w; ++x) {
				final int off = x * h;
				
				for (int y = 0; y < h; ++y) {
					final int rgb = logoBuffer.getRGB(x, y);
					colors[off + y] = 0x00 == (rgb >> 24) ? 0x00 : rgb;
				}
			}
			out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}
		
		private BufferedImage loadLogo() {
			try {
				final InputStream is = EntryScreen.class.getResourceAsStream("/ere_v.png");
//				final URL url = new URL("http://resisttheb.org/ere/lib/ere_v.png");
//				final InputStream is = url.openStream();
				try {
					return ImageIO.read(is);
				}
				finally {
					is.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		
		private void initTimer() {
			timingSource.addEventListener(new TimingEventListener() {
				@Override
				public void timingSourceEvent(final TimingSource src) {
					advance();
				}
			});
		}
		
		
		// TO BE CALLED BY TIMER
		public void advance() {
			++incr;
			updateOutput();
		}
		//
		
		
		private void updateOutput() {
			for (int x = 0; x < w; ++x) {
				final int off = x * h;
				
				for (int y = 0; y < h; ++y) {
					final int color = colors[off + y];
					
					if (0x00 == color) {
						continue;
					}
					
					float n = ngen.noise(nes, SmoothingFunction.S_CURVE,
							sx * x, sy * y, za + zb * incr);
					
					n = (n + 1) / 2;
					
					final float m = (2 * n + 1) / 4;
					final float xxb = 1.7f * m;
					final float xxa = 1.5f * m;
					int b = (int) (xxb * (color & 0xFF));
					int g = (int) (xxb * ((color >> 8) & 0xFF));
					int r = (int) (xxb * ((color >> 16) & 0xFF));
					int a = (int) (xxa * ((color >> 24) & 0xFF));
					if (0xFF < r) r = 0xFF;
					if (0xFF < g) g = 0xFF;
					if (0xFF < b) b = 0xFF;
					if (0xFF < a) a = 0xFF;
					/*
					r *= m;
					g *= m;
					b *= m;					
					//a = (int) ((a + 0xFF * n) / 2);
					a *= m;
					*/
					
					out.setRGB(x, y, b | (g << 8) | (r << 16) | (a << 24));
				}
			}
		}
		
		
		
		
		
		public void paint(final Graphics2D g2d) {
			g2d.drawImage(out, 0, 0, out.getWidth(), out.getHeight(), null);
		}
		
		
		
		
	}
	
	
	
	
	
	
	
	
	private static final class SwingTimingSource extends TimingSource {
		private float updatesPerSecond;
		private Timer timer;
		
		
		public SwingTimingSource(final float _updatesPerSecond) {
			if (_updatesPerSecond < 1) {
				throw new IllegalArgumentException();
			}
			this.updatesPerSecond = _updatesPerSecond;
		}
		
		
		/**************************
		 * PLAYBACK CONTROLS
		 **************************/
		
		public void sstart() {
			if (null == timer) {
				timer = new Timer(Math.round(1000.f / updatesPerSecond),
					new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							// Advance:
							timingEvent();
						}
					});
				
				timer.start();
			}
		}
		
		public void sstop() {
			if (null != timer) {
				timer.stop();
				timer = null;
			}
		}
		
		/**************************
		 * END PLAYBACK CONTROLS
		 **************************/
		
		
		
		/**************************
		 * TIMINGSOURCE IMPLEMENTATION
		 **************************/
		
		@Override
		public void setResolution(int res) {
			// Ignore
		}

		@Override
		public void setStartDelay(int delay) {
			// Ignore
		}

		@Override
		public void start() {
			// Ignore
		}

		@Override
		public void stop() {
			// Ignore
		}
		
		
		/**************************
		 * TIMINGSOURCE IMPLEMENTATION
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
		
		
		final InPacket0[] packets = {
			new InPacket0(37, "3EdGdLGdGP8MCELgxXTnOzyNMqi5zYa2")
		};
		final EntryScreen es = new EntryScreen();
		es.init(packets);
		
		es.start();
		
		
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(es, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				es.next();
			}
		});
		
		
		
	}
	*/
}
