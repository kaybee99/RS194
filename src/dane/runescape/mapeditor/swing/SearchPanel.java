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
package dane.runescape.mapeditor.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import dane.runescape.mapeditor.event.SearchEvent;
import dane.runescape.mapeditor.event.SearchEventListener;
import dane.runescape.mapeditor.search.SearchMode;
import dane.runescape.mapeditor.search.Searchable;

/**
 * The panel containing all the search components.
 *
 * @author Dane
 */
public class SearchPanel extends JPanel implements ListSelectionListener, ActionListener {

	private static final long serialVersionUID = -2861032783438108415L;

	private SearchMode currentMode = null;
	
	private JPanel panelTop;
	private JLabel lblSearch;
	private DefaultComboBoxModel<SearchMode> cmbModel;
	private JComboBox<SearchMode> cmbSearch;

	private DefaultListModel<Searchable> listModel;
	private JList<Searchable> list;
	private JScrollPane paneList;

	public SearchPanel() {
		setSize(256, 256);
		setPreferredSize(getSize());
		setLayout(new BorderLayout());
	}

	public void assemble() {
		panelTop = new JPanel(new BorderLayout());
		lblSearch = new JLabel("search");
		
		cmbModel = new DefaultComboBoxModel<>(SearchMode.values());
		cmbSearch = new JComboBox<>(cmbModel);
		
		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		paneList = new JScrollPane(list);
		
		cmbSearch.setEditable(true);
		cmbSearch.addActionListener(this);
		cmbSearch.setSelectedItem(null);

		panelTop.add(lblSearch, BorderLayout.WEST);
		panelTop.add(cmbSearch, BorderLayout.CENTER);
		add(panelTop, BorderLayout.NORTH);

		list.setBackground(Color.GRAY);
		list.setCellRenderer(new SearchListCellRenderer());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(this);
		add(paneList, BorderLayout.CENTER);
	}

	public void add(Searchable v) {
		listModel.addElement(v);
	}

	public void add(List<Searchable> list) {
		list.stream().forEach(listModel::addElement);
	}

	public void clear() {
		listModel.clear();
	}

	protected void fireSearchEvent(String string) {
		SearchEventListener[] listeners = getSearchListeners();
		for (SearchEventListener listener : listeners) {
			listener.onSearchEvent(new SearchEvent(this, currentMode, string));
		}
	}

	protected void fireSelectEvent(Searchable value) {
		SearchEventListener[] listeners = getSearchListeners();
		for (SearchEventListener listener : listeners) {
			listener.onSearchEvent(new SearchEvent(this, currentMode, value));
		}
	}

	protected void fireModeChange(SearchMode mode) {
		SearchEventListener[] listeners = getSearchListeners();
		for (SearchEventListener listener : listeners) {
			listener.onSearchEvent(new SearchEvent(this, mode));
		}
	}

	/* Search Listeners */
	public SearchEventListener[] getSearchListeners() {
		return listenerList.getListeners(SearchEventListener.class);
	}

	public void addSearchListener(SearchEventListener l) {
		listenerList.add(SearchEventListener.class, l);
	}

	public void removeSearchListener(SearchEventListener l) {
		listenerList.remove(SearchEventListener.class, l);
	}

	/* Combo box action listener */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == cmbSearch) {
			Object o = cmbModel.getSelectedItem();

			if (o instanceof SearchMode) {
				SearchMode type = (SearchMode) o;

				// only do something if it's a new type
				if (currentMode != type) {
					// reset selected so it doesn't show up in combobox
					cmbModel.setSelectedItem(null);
					// scroll up all the way
					paneList.getVerticalScrollBar().setValue(0);
					// apply
					currentMode = (SearchMode) o;
					// notify listeners
					fireModeChange(type);
				}
			} else {
				// two events are sent, comboBoxChanged & comboBoxEdited.
				// we don't want to search twice.
				if (e.getActionCommand().equals("comboBoxChanged")) {
					fireSearchEvent(String.valueOf(o));
				}
			}
		} else {
			System.out.println("UNKOWN ACTION: " + e);
		}
	}

	/* List selection listener */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		// this event is called usually twice upon using your mouse, once with
		// isAdjusting = true and another with isAdjusting = false
		if (e.getValueIsAdjusting()) {
			fireSelectEvent(list.getSelectedValue());
		}
	}
}
