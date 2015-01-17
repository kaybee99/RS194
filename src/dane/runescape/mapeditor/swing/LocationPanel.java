package dane.runescape.mapeditor.swing;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

import dane.runescape.mapeditor.util.FileUtil;

public class LocationPanel extends JPanel {

	private static final long serialVersionUID = -2913689793325251798L;

	public LocationPanel() {
		setSize(258, 230);
		setPreferredSize(getSize());
		setMinimumSize(getSize());
		setMaximumSize(getSize());
		setPreferredSize(getSize());
		setLayout(new GridLayout(5, 5, 0, 0));
		setBackground(Color.GRAY);
	}

	public void assemble() {
		try {
			add(new HotkeyButton(FileUtil.readImage("loctype/wall.png"), KeyEvent.VK_1));
			add(new HotkeyButton(FileUtil.readImage("loctype/wallcorner1.png"), KeyEvent.VK_2));
			add(new HotkeyButton(FileUtil.readImage("loctype/wallcorner2.png"), KeyEvent.VK_3));
			add(new HotkeyButton(FileUtil.readImage("loctype/wallcorner3.png"), KeyEvent.VK_4));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldiagonal.png"), KeyEvent.VK_5));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldecoration.png"), KeyEvent.VK_Q));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldecorationpadded.png"), KeyEvent.VK_W));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldecorationout.png"), KeyEvent.VK_E));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldecorationin.png"), KeyEvent.VK_R));
			add(new HotkeyButton(FileUtil.readImage("loctype/walldecorationinout.png"), KeyEvent.VK_T));
			add(new HotkeyButton(FileUtil.readImage("loctype/loc.png"), KeyEvent.VK_8));
			add(new HotkeyButton(FileUtil.readImage("loctype/locdiagonal.png"), KeyEvent.VK_9));
			add(new HotkeyButton(FileUtil.readImage("loctype/grounddecoration.png"), KeyEvent.VK_0));
			add(new HotkeyButton(FileUtil.readImage("loctype/npc.png"), KeyEvent.VK_N));
			add(new HotkeyButton(FileUtil.readImage("loctype/obj.png"), KeyEvent.VK_M));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofside.png"), KeyEvent.VK_A));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofdiagonalside.png"), KeyEvent.VK_S));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofdiagonal.png"), KeyEvent.VK_D));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofinner.png"), KeyEvent.VK_F));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofoutter.png"), KeyEvent.VK_G));
			add(new HotkeyButton(FileUtil.readImage("loctype/roof.png"), KeyEvent.VK_H));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofedge.png"), KeyEvent.VK_Z));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofedgecorner.png"), KeyEvent.VK_X));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofedgeinnercorner1.png"), KeyEvent.VK_C));
			add(new HotkeyButton(FileUtil.readImage("loctype/roofedgeinnercorner2.png"), KeyEvent.VK_V));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
