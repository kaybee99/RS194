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
package dane.runescape.mapeditor;

import com.runescape.Canvas2D;
import com.runescape.Game;
import com.runescape.ImageProducer;
import com.runescape.Landscape;
import com.runescape.Scene;
import dane.runescape.mapeditor.event.GameListener;

/**
 * An extension of the {@link Game} class. Used for handling the drawing and
 * updating of the 3d scene.
 *
 * @author Dane
 */
public class GameSub extends Game {

	@Override
	public void startup() {
		viewport = new ImageProducer(512, 334);
		Canvas2D.clear();
	}

	@Override
	public void update() {
	}

	@Override
	public void draw() {
	}

	@Override
	public Scene createScene() {
		Scene s = super.createScene();
		this.fireSceneCreated(this.currentPlane, s, this.landscape);
		return s;
	}

	protected void fireSceneCreated(int plane, Scene s, Landscape land) {
		for (GameListener l : this.listeners.getListeners(GameListener.class)) {
			l.onSceneCreation(plane, s, land);
		}
	}

	/**
	 * Adds a game listener.
	 *
	 * @param l the listener.
	 */
	public void addGameListener(GameListener l) {
		this.listeners.add(GameListener.class, l);
	}

	/**
	 * Removes a game listener.
	 *
	 * @param l the listener.
	 */
	public void removeGameListener(GameListener l) {
		this.listeners.remove(GameListener.class, l);
	}
}
