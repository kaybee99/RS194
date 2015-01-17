package dane.runescape.mapeditor;

import com.runescape.Landscape;
import com.runescape.Scene;
import com.runescape.Signlink;
import dane.runescape.mapeditor.event.GameListener;
import dane.runescape.mapeditor.event.SearchEvent;
import static dane.runescape.mapeditor.event.SearchEvent.Type.MODE_CHANGED;
import static dane.runescape.mapeditor.event.SearchEvent.Type.VALUE_SEARCHED;
import static dane.runescape.mapeditor.event.SearchEvent.Type.VALUE_SELECTED;
import dane.runescape.mapeditor.event.SearchEventListener;
import dane.runescape.mapeditor.search.SearchManager;
import dane.runescape.mapeditor.search.SearchMode;
import dane.runescape.mapeditor.search.Searchable;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dane.runescape.mapeditor.swing.LocationPanel;
import dane.runescape.mapeditor.swing.SearchPanel;
import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.UnsupportedLookAndFeelException;

public final class MapEditor extends JFrame implements GameListener, SearchEventListener {

	private static final Logger logger = Logger.getLogger(MapEditor.class.getName());
	private static final long serialVersionUID = -343522878080909782L;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			logger.log(Level.SEVERE, "Error setting look and feel", e);
		}

		try {
			// we need signlink to be in private mode
			Signlink.startPrivate(InetAddress.getLocalHost());
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}

		MapEditor m = new MapEditor(1280, 800);
		m.assemble();
	}

	public OrbitCamera orbitCamera = new OrbitCamera();

	public JMenuBar menubar = new JMenuBar();
	public JMenu menuFile = new JMenu("File");
	public JMenu menuEdit = new JMenu("Edit");
	public JMenu menuModes = new JMenu("Modes");
	public JMenu menuMapControls = new JMenu("Map Controls");
	public JMenu menu3DControls = new JMenu("3D Controls");
	public JMenu menuOptions = new JMenu("Options");

	public JPanel panel = new JPanel(new BorderLayout());

	public JPanel panel2d = new JPanel(new BorderLayout());
	public JPanel panel3d = new JPanel(null);

	public GameSub game = new GameSub();
	public MapPanel panelMap = new MapPanel();
	public GamePanel gamePanel = new GamePanel(game);

	public JScrollPane pane2d = new JScrollPane(panel2d);
	public JScrollPane pane3d = new JScrollPane(panel3d);

	public JPanel panelPreview = new JPanel();
	public JScrollPane panePreview = new JScrollPane(panelPreview);

	public JPanel panelLeft = new JPanel(new BorderLayout());
	public JPanel panelRight = new JPanel(new BorderLayout());
	public JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panelLeft, panelRight);

	public JPanel panelTop = new JPanel(new BorderLayout());
	public JPanel panelCenter = new JPanel(new BorderLayout());
	public JSplitPane splitTop = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, panelTop, panelCenter);

	public JPanel panelCenterTop = new JPanel(new BorderLayout());
	public JPanel panelCenterBottom = new JPanel(new BorderLayout());
	public JSplitPane splitCenter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, panelCenterTop, panelCenterBottom);

	public SearchPanel searchPanel = new SearchPanel();
	public LocationPanel locTypePanel = new LocationPanel();

	public JPanel panelBottomLeft = new JPanel(new BorderLayout());
	public JPanel panelBottomRight = new JPanel(new BorderLayout());
	public JSplitPane splitBottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panelBottomLeft, panelBottomRight);

	public MapEditor(int w, int h) {
		setMinimumSize(new Dimension(w, h));
	}

	private void assemble() {
		setTitle("Suite");
		setResizable(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		assemblePanels();
		assembleMenubar();

		gamePanel.setSize(512, 334);
		gamePanel.setPreferredSize(gamePanel.getSize());
		gamePanel.setMinimumSize(gamePanel.getSize());
		gamePanel.setMaximumSize(gamePanel.getSize());
		gamePanel.init();
		
		addListeners();
		setColorCoated();
		setVisible(true);

		// still doesn't work properly. Googled it, turns out JSplitPane's are
		// just assholes.
		SwingUtilities.invokeLater(() -> {
			splitLeft.setDividerLocation(0.5);
			splitTop.setDividerLocation(0.5);
			splitCenter.setDividerLocation(0.5);
			splitBottom.setDividerLocation(0.5);
			panelMap.requestFocus();
		});
	}

	public void assemblePanels() {
		panel.add(splitLeft);
		panelRight.add(splitCenter);
		panelCenterTop.add(splitTop);
		panelCenterBottom.add(splitBottom);

		searchPanel.assemble();
		panelBottomLeft.add(searchPanel);

		locTypePanel.assemble();
		panelBottomRight.add(locTypePanel);

		panelMap.assemble();
		panel2d.add(panelMap, BorderLayout.CENTER);
		pane2d.getVerticalScrollBar().setUnitIncrement(16);
		pane2d.getHorizontalScrollBar().setUnitIncrement(16);

		panel3d.add(gamePanel);
		panelLeft.add(pane2d, BorderLayout.CENTER);
		panelTop.add(pane3d, BorderLayout.CENTER);
		add(panel);
	}

	@SuppressWarnings("serial")
	public void assembleMenubar() {
		setJMenuBar(menubar);

		menuFile.add(new JMenuItem(new AbstractAction("Exit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				MapEditor.this.dispose();
			}
		}));

		menuOptions.add(new JMenuItem(new AbstractAction("About") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Desktop desktop = Desktop.getDesktop();

				try {
					JOptionPane.showMessageDialog(MapEditor.this, new Object[]{new JLabel("Skype: the.dane.effect"),
						new JButton(new AbstractAction("Created by Dane") {
							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									desktop.browse(new URI("http://www.rune-server.org/profile.php?do=addlist&userlist=friend&u=225682"));
								} catch (URISyntaxException | IOException ex) {
									JOptionPane.showMessageDialog(MapEditor.this, ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
								}
							}
						}), new JButton(new AbstractAction("Project Github") {
						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								desktop.browse(new URI("https://github.com/thedaneeffect/RS194"));
							} catch (URISyntaxException | IOException ex) {
								JOptionPane.showMessageDialog(MapEditor.this, ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
							}
						}
					})}, "About", JOptionPane.INFORMATION_MESSAGE, null);
				} catch (Throwable t) {
					logger.log(Level.WARNING, "Error showing about", t);
				}
			}
		}));

		menubar.add(menuFile);
		//menubar.add(menuEdit);
		//menubar.add(menuModes);
		//menubar.add(menuMapControls);
		//menubar.add(menu3DControls);
		menubar.add(menuOptions);
	}

	public void addListeners() {
		//panelMap.addCameraEventListener(appletGame);
		panelMap.setScrollBars(pane2d.getVerticalScrollBar(), pane2d.getHorizontalScrollBar());
		game.addGameListener(this);
		searchPanel.addSearchListener(this);
	}

	/**
	 * Used to help identify the panels.
	 */
	public void setColorCoated() {
		panelTop.setBackground(Color.magenta);
		panelCenter.setBackground(Color.green);
		panelBottomLeft.setBackground(Color.red);
		panelBottomRight.setBackground(Color.blue);
		panelLeft.setBackground(Color.gray);
	}

	@Override
	public void dispose() {
		super.dispose();
		gamePanel.getGame().forceShutdown();
	}

	@Override
	public void onSceneCreation(int plane, Scene s, Landscape l) {
		for (SearchMode m : SearchMode.values()) {
			SearchManager.populate(m);
		}
	}

	@Override
	public void onSearchEvent(SearchEvent e) {
		SearchEvent.Type type = e.getType();
		SearchMode mode = e.getMode();

		switch (type) {
			case MODE_CHANGED: {
				searchPanel.clear();
				searchPanel.add(new Searchable("List of items for " + mode + " mode.", -1, mode));
				searchPanel.add(SearchManager.getAll(mode));
				break;
			}
			case VALUE_SEARCHED: {
				searchPanel.clear();
				searchPanel.add(new Searchable("List of items for " + mode + " mode.", -1, mode));
				searchPanel.add(SearchManager.find(mode, e.getSearched()));
				break;
			}
			case VALUE_SELECTED: {
				System.out.println(e.getSelected());
				break;
			}
		}
	}
}
