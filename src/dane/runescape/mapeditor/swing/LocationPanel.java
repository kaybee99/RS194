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

import dane.runescape.mapeditor.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.logging.*;
import javax.swing.*;

/**
 * Contains all the hotkey buttons for all the different location types.
 *
 * @author Dane
 */
// TODO: rename?
public class LocationPanel extends JPanel {

	private static final Logger logger = Logger.getLogger(LocationPanel.class.getName());

	private static final long serialVersionUID = -2913689793325251798L;

	/**
	 * Shortcut method to getting an image.
	 *
	 * @param name the image name.
	 * @return the image.
	 * @throws IOException
	 */
	private static BufferedImage getHotkeyImage(String name) throws IOException {
		return FileUtil.readImage("loctype/" + name + ".png");
	}

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
			add(new HotkeyButton(getHotkeyImage("wall"), KeyEvent.VK_1));
			add(new HotkeyButton(getHotkeyImage("wallcorner1"), KeyEvent.VK_2));
			add(new HotkeyButton(getHotkeyImage("wallcorner2"), KeyEvent.VK_3));
			add(new HotkeyButton(getHotkeyImage("wallcorner3"), KeyEvent.VK_4));
			add(new HotkeyButton(getHotkeyImage("walldiagonal"), KeyEvent.VK_5));
			add(new HotkeyButton(getHotkeyImage("walldecoration"), KeyEvent.VK_Q));
			add(new HotkeyButton(getHotkeyImage("walldecorationpadded"), KeyEvent.VK_W));
			add(new HotkeyButton(getHotkeyImage("walldecorationout"), KeyEvent.VK_E));
			add(new HotkeyButton(getHotkeyImage("walldecorationin"), KeyEvent.VK_R));
			add(new HotkeyButton(getHotkeyImage("walldecorationinout"), KeyEvent.VK_T));
			add(new HotkeyButton(getHotkeyImage("loc"), KeyEvent.VK_8));
			add(new HotkeyButton(getHotkeyImage("locdiagonal"), KeyEvent.VK_9));
			add(new HotkeyButton(getHotkeyImage("grounddecoration"), KeyEvent.VK_0));
			add(new HotkeyButton(getHotkeyImage("npc"), KeyEvent.VK_N));
			add(new HotkeyButton(getHotkeyImage("obj"), KeyEvent.VK_M));
			add(new HotkeyButton(getHotkeyImage("roofside"), KeyEvent.VK_A));
			add(new HotkeyButton(getHotkeyImage("roofdiagonalside"), KeyEvent.VK_S));
			add(new HotkeyButton(getHotkeyImage("roofdiagonal"), KeyEvent.VK_D));
			add(new HotkeyButton(getHotkeyImage("roofinner"), KeyEvent.VK_F));
			add(new HotkeyButton(getHotkeyImage("roofoutter"), KeyEvent.VK_G));
			add(new HotkeyButton(getHotkeyImage("roof"), KeyEvent.VK_H));
			add(new HotkeyButton(getHotkeyImage("roofedge"), KeyEvent.VK_Z));
			add(new HotkeyButton(getHotkeyImage("roofedgecorner"), KeyEvent.VK_X));
			add(new HotkeyButton(getHotkeyImage("roofedgeinnercorner1"), KeyEvent.VK_C));
			add(new HotkeyButton(getHotkeyImage("roofedgeinnercorner2"), KeyEvent.VK_V));

			for (Component c : getComponents()) {
				if (c instanceof HotkeyButton) {
					HotkeyButton h = (HotkeyButton) c;
					Hotkey.add(h, h.getKey(), h.getAction());
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error loading location panel", e);
		}
	}

}
