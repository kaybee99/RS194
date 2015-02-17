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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A simple button that is activated when you press a button. It has an image
 * too!
 *
 * @author Dane
 */
public class HotkeyButton extends JButton {

	private static final long serialVersionUID = 5722934058142427754L;

	public static final Border BORDER_NORMAL = new LineBorder(Color.GRAY, 2);
	public static final Border BORDER_HIGHLIGHTED = new LineBorder(Color.YELLOW, 2);
	public static final Font FONT = new Font("Helvetica", Font.BOLD, 16);

	private static HotkeyButton selected;

	public static final HotkeyButton getSelected() {
		return selected;
	}

	public static final void setSelected(HotkeyButton b) {
		if (b != selected) {
			if (selected != null) {
				selected.setBorder(BORDER_NORMAL);
			}
			b.setBorder(BORDER_HIGHLIGHTED);
			selected = b;
		}
	}

	private int key;

	public HotkeyButton(BufferedImage image, int key) {
		AbstractAction a = new AbstractAction() {
			private static final long serialVersionUID = 3068656327981819336L;

			@Override
			public void actionPerformed(ActionEvent e) {
				HotkeyButton.setSelected(HotkeyButton.this);
			}
		};

		this.key = key;

		setAction(a);

		setIcon(new ImageIcon(image));
		setText(KeyEvent.getKeyText(key));

		setSize(49, 42);
		setPreferredSize(getSize());
		setMaximumSize(getSize());
		setMinimumSize(getSize());

		setFont(FONT);
		setForeground(Color.WHITE);
		setBackground(Color.BLACK);
		setBorder(BORDER_NORMAL);
	}

	public int getKey() {
		return this.key;
	}
}
