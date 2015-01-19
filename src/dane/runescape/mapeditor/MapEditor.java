/*
 * The MIT License
 *
 * Copyright 2015 Dane.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

	private OrbitCamera orbitCamera = new OrbitCamera();

	private JMenuBar menubar;
	private JMenu menuFile, menuEdit, menuModes, menuMapControls, menu3DControls, menuOptions;

	private JPanel panel;

	private JPanel panel2d;
	private JPanel panel3d;

	private GameSub game;
	private MapPanel panelMap;
	private GamePanel gamePanel;

	private JScrollPane pane2d;
	private JScrollPane pane3d;

	private JPanel panelPreview;
	private JScrollPane panePreview;

	private JPanel panelLeft;
	private JPanel panelRight;
	public JSplitPane splitLeft;

	private JPanel panelTop;
	private JPanel panelCenter;
	private JSplitPane splitTop;

	private JPanel panelCenterTop;
	private JPanel panelCenterBottom;
	private JSplitPane splitCenter;

	private SearchPanel searchPanel;
	private LocationPanel locTypePanel;

	private JPanel panelBottomLeft;
	private JPanel panelBottomRight;
	private JSplitPane splitBottom;

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
		panel = new JPanel(new BorderLayout());

		panel2d = new JPanel(new BorderLayout());
		panel3d = new JPanel(null);

		game = new GameSub();

		pane2d = new JScrollPane(panel2d);
		pane3d = new JScrollPane(panel3d);

		panelPreview = new JPanel();
		panePreview = new JScrollPane(panelPreview);

		/* Splits */
		panelLeft = new JPanel(new BorderLayout());
		panelRight = new JPanel(new BorderLayout());
		splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panelLeft, panelRight);
		panel.add(splitLeft);

		panelCenterTop = new JPanel(new BorderLayout());
		panelCenterBottom = new JPanel(new BorderLayout());
		splitCenter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, panelCenterTop, panelCenterBottom);
		panelRight.add(splitCenter);

		panelTop = new JPanel(new BorderLayout());
		panelCenter = new JPanel(new BorderLayout());
		splitTop = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, panelTop, panelCenter);
		panelCenterTop.add(splitTop);

		panelBottomLeft = new JPanel(new BorderLayout());
		panelBottomRight = new JPanel(new BorderLayout());
		splitBottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panelBottomLeft, panelBottomRight);
		panelCenterBottom.add(splitBottom);

		/* Special panels */
		// Search Panel
		searchPanel = new SearchPanel();
		searchPanel.assemble();
		panelBottomLeft.add(searchPanel);

		// LOC Hotkey Panel
		locTypePanel = new LocationPanel();
		locTypePanel.assemble();
		panelBottomRight.add(locTypePanel);

		// 2D Map Panel
		panelMap = new MapPanel();
		panelMap.assemble();
		panel2d.add(panelMap, BorderLayout.CENTER);
		pane2d.getVerticalScrollBar().setUnitIncrement(16);
		pane2d.getHorizontalScrollBar().setUnitIncrement(16);

		// Game Panel
		gamePanel = new GamePanel(game);
		panel3d.add(gamePanel);
		panelLeft.add(pane2d, BorderLayout.CENTER);
		panelTop.add(pane3d, BorderLayout.CENTER);

		add(panel);
	}

	public void assembleMenubar() {
		setJMenuBar(menubar = new JMenuBar());

		menuFile = new JMenu("File");
		menuFile.add(new JMenuItem(new AbstractAction("Exit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				MapEditor.this.dispose();
			}
		}));
		menubar.add(menuFile);

		menuEdit = new JMenu("Edit");
		//menubar.add(menuEdit);

		menuModes = new JMenu("Modes");
		//menubar.add(menuModes);

		menuMapControls = new JMenu("Map Controls");
		//menubar.add(menuMapControls);

		menu3DControls = new JMenu("3D Controls");
		//menubar.add(menu3DControls);

		menuOptions = new JMenu("Options");
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