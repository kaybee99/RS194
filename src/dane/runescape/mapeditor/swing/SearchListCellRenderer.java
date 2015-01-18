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

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Renders the {@link SearchPanel}'s list jagex style.
 *
 * @author Dane
 */
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
