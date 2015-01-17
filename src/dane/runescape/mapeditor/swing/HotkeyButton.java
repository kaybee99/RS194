package dane.runescape.mapeditor.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import dane.runescape.mapeditor.util.Hotkey;

public class HotkeyButton extends JButton {

	private static final long serialVersionUID = 5722934058142427754L;

	public static final Border BORDER_NORMAL = new LineBorder(Color.GRAY, 2);
	public static final Border BORDER_HIGHLIGHTED = new LineBorder(Color.YELLOW, 2);
	public static final Font FONT = new Font("Helvetica", Font.BOLD, 16);

	public static HotkeyButton selected;

	public static final void setSelected(HotkeyButton b) {
		if (b != selected) {
			if (selected != null) {
				selected.setBorder(BORDER_NORMAL);
			}
			b.setBorder(BORDER_HIGHLIGHTED);
			selected = b;
		}
	}

	public HotkeyButton(BufferedImage image, int keyCode) {
		AbstractAction a = new AbstractAction() {
			private static final long serialVersionUID = 3068656327981819336L;

			@Override
			public void actionPerformed(ActionEvent e) {
				HotkeyButton.setSelected(HotkeyButton.this);
			}
		};

		Hotkey.add(this, keyCode, a);
		setAction(a);

		setIcon(new ImageIcon(image));
		setText(KeyEvent.getKeyText(keyCode));

		setSize(49, 42);
		setPreferredSize(getSize());
		setMaximumSize(getSize());
		setMinimumSize(getSize());

		setFont(FONT);
		setForeground(Color.WHITE);
		setBackground(Color.BLACK);
		setBorder(BORDER_NORMAL);
	}
}
