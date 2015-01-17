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

public class SearchPanel extends JPanel implements ListSelectionListener, ActionListener {

	private static final long serialVersionUID = -2861032783438108415L;

	public SearchMode mode = null;
	public JPanel panelTop = new JPanel(new BorderLayout());
	public JLabel lblSearch = new JLabel("search");
	public DefaultComboBoxModel<SearchMode> cmbModel = new DefaultComboBoxModel<>(SearchMode.values());
	public JComboBox<SearchMode> cmbSearch = new JComboBox<>(cmbModel);

	public DefaultListModel<Searchable> listModel = new DefaultListModel<>();
	public JList<Searchable> list = new JList<>(listModel);
	public JScrollPane paneList = new JScrollPane(list);

	public SearchPanel() {
		setSize(256, 256);
		setPreferredSize(getSize());
		setLayout(new BorderLayout());
	}

	public void assemble() {
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
			listener.onSearchEvent(new SearchEvent(this, mode, string));
		}
	}

	protected void fireSelectEvent(Searchable value) {
		SearchEventListener[] listeners = getSearchListeners();
		for (SearchEventListener listener : listeners) {
			listener.onSearchEvent(new SearchEvent(this, mode, value));
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
				if (mode != type) {
					// reset selected so it doesn't show up in combobox
					cmbModel.setSelectedItem(null);
					// scroll up all the way
					paneList.getVerticalScrollBar().setValue(0);
					// apply
					mode = (SearchMode) o;
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
