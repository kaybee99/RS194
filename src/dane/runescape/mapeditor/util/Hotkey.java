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
package dane.runescape.mapeditor.util;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Used to create global hotkeys.
 *
 * @author Dane
 */
public class Hotkey {

	public static final void add(JComponent src, int keyCode, Action action) {
		add(src, keyCode, KeyEvent.VK_UNDEFINED, action);
	}

	public static final void add(JComponent src, int keyCode, int modifiers, Action action) {
		KeyStroke key = KeyStroke.getKeyStroke(keyCode, modifiers);
		src.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, key.toString());
		src.getActionMap().put(key.toString(), action);
	}

	public static final void remove(JComponent src, KeyStroke key) {
		src.getInputMap().remove(key);
		src.getActionMap().remove(key.toString());
	}

}
