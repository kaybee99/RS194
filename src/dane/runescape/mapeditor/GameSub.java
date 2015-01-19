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

import com.runescape.Archive;
import com.runescape.Canvas2D;
import com.runescape.Canvas3D;
import com.runescape.Game;
import com.runescape.GameShell;
import com.runescape.ImageProducer;
import com.runescape.Landscape;
import com.runescape.Model;
import com.runescape.Scene;
import com.runescape.Signlink;
import dane.runescape.mapeditor.event.GameListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An extension of the {@link Game} class. Used for handling the drawing and
 * updating of the 3d scene.
 *
 * @author Dane
 */
public class GameSub extends Game {

	private static final Logger logger = Logger.getLogger(GameSub.class.getName());

	public OrbitCamera camera;

	public GameSub() {
		super();
	}

	@Override
	public Archive loadArchive(String archiveName, String archiveFile, int crc, int percent) {
		((GameShell) this).drawProgress("Requesting " + archiveName, percent);
		return new Archive(Signlink.loadFile(archiveFile));
	}

	@Override
	public void startup() {
		try {
			Archive config = loadArchive("config", "config", archiveCRC[2], 20);
			Archive media = loadArchive("2d graphics", "media", archiveCRC[4], 40);
			Archive models = loadArchive("3d graphics", "models", archiveCRC[5], 50);
			Archive textures = loadArchive("textures", "textures", archiveCRC[6], 60);

			initSceneComponents();

			unpackTextures(textures);
			unpackModels(models);
			unpackConfigs(config);

			viewport = new ImageProducer(512, 334);
			viewportOffsets = Canvas3D.prepareOffsets();

			loadRegion(50, 50);
		} catch (Exception e) {
			errorLoading = true;
			logger.log(Level.SEVERE, "Error starting game", e);
		}
	}

	@Override
	public void readLandscape(Scene s, byte[] src, int baseTileX, int baseTileY) {
		s.readLandscape(src, baseTileX, baseTileY, mapCenterChunkX * 8, mapCenterChunkY * 8);
	}

	public void loadRegion(int x, int y) {
		mapCenterChunkX = x << 3;
		mapCenterChunkY = y << 3;

		mapBaseX = mapCenterChunkX << 3;
		mapBaseY = mapCenterChunkY << 3;

		mapLandData = new byte[1][];
		mapLocData = new byte[1][];
		mapIndices = new int[]{(x << 8) | y};

		Signlink.setLoopRate(1);
		mapLandData[0] = Signlink.loadFile("maps/m" + x + "_" + y);
		mapLocData[0] = Signlink.loadFile("maps/l" + x + "_" + y);
		Signlink.setLoopRate(100);

		int deltaX = mapBaseX - mapLastBaseX;
		int deltaY = mapBaseY - mapLastBaseZ;
		mapLastBaseX = mapBaseX;
		mapLastBaseZ = mapBaseY;

		sceneState = 2;
		Scene.builtPlane = currentPlane;
		createScene();
	}

	@Override
	public void update() {
	}

	@Override
	public int updateCamera(int tileX, int tileY) {
		int landY = getLandY(0, 0, currentPlane);

		cameraPitch = camera.getCurrentPitch();

		updateCameraOrbit(camera.getCurrentX(), landY - 50, camera.getCurrentZ(), camera.getCurrentYaw(), camera.getCurrentPitch(), (camera.getCurrentPitch() * 3) + 600);

		try {
			if ((renderflags[currentPlane][tileX][tileY] & 0x4) != 0) {
				return currentPlane;
			}
		} catch (Exception ignored) {

		}

		return getTopPlane(tileX, tileY);
	}

	@Override
	public void draw() {
		if (sceneState == 2) {
			drawViewport();
		}
	}

	@Override
	public void drawViewport() {
		drawCycle++;

		//drawPlayers();
		//drawNPCs();
		//drawProjectiles();
		//drawSpotanims();
		drawSequencedLocs();

		int topPlane = updateCamera(camera.getCurrentX() >> 7, camera.getCurrentY() >> 7);

		int startCycle = Canvas3D.cycle;
		Model.allowInput = true;
		Model.hoverCount = 0;
		Model.mouseX = mouseX - 8;
		Model.mouseY = mouseY - 11;

		Canvas2D.clear();

		Scene.mouseX = Model.mouseX;
		Scene.mouseY = Model.mouseY;

		landscape.draw(cameraX, cameraY, cameraZ, cameraPitch, cameraOrbitYaw, topPlane);
		landscape.clearFrameLocs();

		drawViewport2d();
		drawCrosses();

		if (!optionMenuVisible) {
			updateInput();
			drawTooltip();
		} else if (optionMenuArea == 0) {
			drawOptionMenu();
		}

		updateAnimatedTextures(startCycle);
		viewport.draw(graphics, 0, 0);
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
