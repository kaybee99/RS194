package dane.runescape.mapeditor.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class SearchListCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 5015587804593648963L;

	public static final Color COLOR_HIGHLIGHT = new Color(160, 160, 160);
	public static final Border BORDER_NORMAL = new LineBorder(new Color(108, 108, 108), 1);
	public static final Border BORDER_HIGHLIGHT = new LineBorder(new Color(218, 68, 68), 1);

	public SearchListCellRenderer() {
		super();
		setName("SearchList.cellRenderer");
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		setComponentOrientation(list.getComponentOrientation());

		JList.DropLocation l = list.getDropLocation();

		if (l != null && !l.isInsert() && l.getIndex() == index) {
			isSelected = true;
		}

		if (index == 0) {
			setForeground(Color.WHITE);
			setText(String.valueOf(value));
		} else {
			setForeground(Color.BLUE);
			setText(" " + value);
		}

		if (isSelected) {
			setBorder(BORDER_HIGHLIGHT);
			setBackground(COLOR_HIGHLIGHT);
		} else {
			setBorder(BORDER_NORMAL);
			setBackground(list.getBackground());
		}

		setIcon(null);
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		return this;
	}
}
