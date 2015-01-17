package dane.runescape.mapeditor.util;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

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
