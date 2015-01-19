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
import dane.runescape.mapeditor.event.MapPanelEvent;
import dane.runescape.mapeditor.event.MapPanelEventListener;
import dane.runescape.mapeditor.event.GameListener;
import dane.runescape.mapeditor.util.MathUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An extension of the {@link Game} class. Used for handling the drawing and
 * updating of the 3d scene.
 *
 * @author Dane
 */
public class GameSub extends Game implements MapPanelEventListener {

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
			Game.setHighMemory();

			titleArchive = loadArchive("title", "title", 0, 10);

			loadFonts(titleArchive);

			Archive config = loadArchive("config", "config", archiveCRC[2], 20);
			Archive media = loadArchive("2d graphics", "media", archiveCRC[4], 40);
			Archive models = loadArchive("3d graphics", "models", archiveCRC[5], 50);
			Archive textures = loadArchive("textures", "textures", archiveCRC[6], 60);

			initSceneComponents();

			loadTextures(textures);
			loadModels(models);
			loadConfigs(config);

			drawProgress("Preparing game engine", 95);
			viewport = new ImageProducer(512, 334);
			viewportOffsets = Canvas3D.prepareOffsets();

			Landscape.init(512, 334, 500, 800);
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
		cameraOrbitPitch = cameraPitch;

		cameraYaw = camera.getCurrentYaw();
		cameraOrbitYaw = cameraYaw;

		updateCameraOrbit(camera.getCurrentX(), landY - 50, camera.getCurrentZ(), camera.getCurrentYaw(), cameraPitch, (cameraPitch * 3) + 1200);

		try {
			if ((renderflags[currentPlane][tileX][tileY] & 0x4) != 0) {
				return currentPlane;
			}
		} catch (Exception ignored) {

		}

		try {
			return getTopPlane(tileX, tileY);
		} catch (Exception ignored) {

		}
		return currentPlane;
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

		drawPlayers();
		drawNPCs();
		drawProjectiles();
		drawSpotanims();
		drawSequencedLocs();

		int topPlane = updateCamera(camera.getCurrentX() >> 7, camera.getCurrentY() >> 7);

		int startCycle = Canvas3D.cycle;
		Model.allowInput = true;
		Model.hoverCount = 0;
		Model.mouseX = mouseX;
		Model.mouseY = mouseY;

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

	@Override
	public void onMapPanelEvent(MapPanelEvent e) {
		switch (e.getType()) {
			case ANGLE_CHANGE: {
				this.camera.setYaw(MathUtil.toRuneDegree(e.getAngle()) & 0x7FF);
				break;
			}
			case TILE_CHANGE: {
				this.camera.setX((e.getTileX() * 128) + 64);
				this.camera.setZ((e.getTileY() * 128) + 64);
				break;
			}
			case ZOOM_ADJUST: {
				this.camera.setZoom(this.camera.getZoom() + e.getZoomAdjustment());
				break;
			}
		}
	}
}
