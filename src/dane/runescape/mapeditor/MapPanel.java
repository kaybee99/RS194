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

import com.runescape.*;
import dane.runescape.mapeditor.event.*;
import dane.runescape.mapeditor.media.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/**
 * A panel that displays the two dimensional version of the scene.
 *
 * @author Dane
 */
public class MapPanel extends JPanel implements GameListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = 7632055336249358890L;

	public static final int MAPSQUARE_SIZE = 64;
	public static final int TILE_SIZE = 15;
	public static final int SIZE = MAPSQUARE_SIZE * TILE_SIZE;
	public static final Color COLOR_HOVER = new Color(1f, 1f, 1f, 0.5f);

	public BufferedImage image;
	public int[] pixels;

	public int hoverX, hoverY;
	public int grabX, grabY;
	public int tileX, tileY;

	public double angle = 0;
	public int zoom = 0;

	public boolean isDragging;

	private JScrollBar scrollbarVertical;
	private JScrollBar scrollbarHorizontal;

	private int imageOffsetX;
	private int imageOffsetY;

	private SceneGraph graph;

	public void assemble() {
		this.setBackground(Color.black);
		this.setSize(1200, 1200);
		this.setPreferredSize(getSize());
		this.setMinimumSize(getSize());
		this.setMaximumSize(getSize());
		this.setLayout(null);

		this.imageOffsetX = (getWidth() / 2) - (SIZE / 2);
		this.imageOffsetY = (getHeight() / 2) - (SIZE / 2);

		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);

		this.image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
		this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	}

	public void setScrollBars(JScrollBar vertical, JScrollBar horizontal) {
		this.scrollbarVertical = vertical;
		this.scrollbarHorizontal = horizontal;
	}

	public void fireTileChange(int newTileX, int newTileY) {
		for (MapPanelEventListener l : this.getListeners()) {
			l.onMapPanelEvent(new MapPanelEvent(this, newTileX, newTileY));
		}
	}

	public void fireAngleChange(double newAngle) {
		for (MapPanelEventListener l : this.getListeners()) {
			l.onMapPanelEvent(new MapPanelEvent(this, newAngle));
		}
	}

	public void fireZoomAdjustment(int zoomAdjustment) {
		for (MapPanelEventListener l : this.getListeners()) {
			l.onMapPanelEvent(new MapPanelEvent(this, zoomAdjustment));
		}
	}

	/* Camera Event Listener */
	public MapPanelEventListener[] getListeners() {
		return listenerList.getListeners(MapPanelEventListener.class);
	}

	public void addListener(MapPanelEventListener l) {
		listenerList.add(MapPanelEventListener.class, l);
	}

	public void removeListener(MapPanelEventListener l) {
		listenerList.remove(MapPanelEventListener.class, l);
	}

	/* Scene Load Listener */
	@Override
	public void onSceneLoaded(int plane, Scene scene, SceneGraph graph) {
		this.graph = graph;

		byte[][] overlayTypes = scene.planeOverlayTypes[plane];
		byte[][] overlayRotations = scene.planeOverlayRotations[plane];
		byte[][] underlayFlos = scene.planeUnderlayFloorIndices[plane];
		byte[][] overlayFlos = scene.planeOverlayFloorIndices[plane];

		Arrays.fill(pixels, 0x282018);
		Graphics g = this.image.getGraphics();

		int h = (this.image.getHeight() - TILE_SIZE + 1);

		for (int x = 0; x < MAPSQUARE_SIZE; x++) {
			for (int y = 0; y < MAPSQUARE_SIZE; y++) {
				int underlayFloId = underlayFlos[x][y];
				int overlayFloId = overlayFlos[x][y];

				FloorType underlayFlo = null;
				FloorType overlayFlo = null;

				if (overlayFloId != 0) {
					underlayFlo = FloorType.instances[overlayFloId - 1];
				}

				if (underlayFloId != 0) {
					overlayFlo = FloorType.instances[underlayFloId - 1];
				}

				int drawX = (x * TILE_SIZE) + 1;
				int drawY = h - (y * TILE_SIZE);

				if (overlayFlo != null) {
					if (overlayFlo.textureIndex >= 0) {
						g.setColor(new Color(Graphics3D.getTextureColor(overlayFlo.textureIndex)));
					} else {
						g.setColor(new Color(overlayFlo.rgb));
					}

					g.fillRect(drawX, drawY, 14, 14);
				}

				if (underlayFlo != null) {
					if (underlayFlo.textureIndex >= 0) {
						g.setColor(new Color(Graphics3D.getTextureColor(underlayFlo.textureIndex)));
					} else {
						g.setColor(new Color(underlayFlo.rgb));
					}

					// they draw 1 pixel off so we gotta counteract that.
					drawX -= 1;
					drawY -= 1;

					g.translate(drawX, drawY);
					g.fillPolygon(TileShape.getPolygon(overlayTypes[x][y] + 1, overlayRotations[x][y]));
					g.translate(-drawX, -drawY);
				}
			}
		}

		g.setColor(Color.BLACK);

		for (int x = 0; x < MAPSQUARE_SIZE; x++) {
			for (int y = 0; y < MAPSQUARE_SIZE; y++) {
				g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
		}

		g.dispose();
		this.repaint();
	}

	@Override
	public void onSceneTileClicked(Tile t) {
		graph.removeLocations(t.x, t.plane, t.z);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.translate(this.imageOffsetX, this.imageOffsetY);
		g.drawImage(this.image, 0, 0, null);

		if (this.hoverX >= 0 && this.hoverX < 64 && this.hoverY >= 0 && this.hoverY < 64) {
			g.setColor(COLOR_HOVER);

			int x = this.hoverX * TILE_SIZE;
			int y = ((63 - this.hoverY) * TILE_SIZE);

			g.fillRect(x, y, TILE_SIZE + 1, TILE_SIZE + 1);
			g.drawString(this.hoverX + ", " + this.hoverY, x, y);
		}

		if (this.tileX >= 0 && this.tileX <= 63 && this.tileY >= 0 && this.tileY <= 63) {
			g.setColor(Color.YELLOW);

			int x = this.tileX * TILE_SIZE;
			int y = (63 - this.tileY) * TILE_SIZE;

			g.drawRect(x, y, TILE_SIZE, TILE_SIZE);

			x += TILE_SIZE / 2;
			y += TILE_SIZE / 2;

			g.drawLine(x, y, (int) (x + (Math.sin(this.angle - 0.61) * 8)), (int) (y + (Math.cos(this.angle - 0.61) * 8)));
			g.drawLine(x, y, (int) (x + (Math.sin(this.angle + 0.61) * 8)), (int) (y + (Math.cos(this.angle + 0.61) * 8)));
			g.drawLine(x, y, (int) (x + (Math.sin(this.angle) * 16)), (int) (y + (Math.cos(this.angle) * 16)));
		}

		g.translate(-this.imageOffsetX, -this.imageOffsetY);
	}

	protected void updateHover(int x, int y) {
		int lastX = this.hoverX;
		int lastY = this.hoverY;

		this.hoverX = (x - this.imageOffsetX) / TILE_SIZE;
		this.hoverY = 63 - ((y - this.imageOffsetY) / TILE_SIZE);

		if (this.hoverX < 0) {
			this.hoverX = 0;
		} else if (this.hoverX > 63) {
			this.hoverX = 63;
		}

		if (this.hoverY < 0) {
			this.hoverY = 0;
		} else if (this.hoverY > 63) {
			this.hoverY = 63;
		}

		if (lastX != this.hoverX || lastY != this.hoverY) {
			this.repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		this.updateHover(e.getX(), e.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.grabX = e.getX();
		this.grabY = e.getY();

		if (SwingUtilities.isMiddleMouseButton(e)) {
			this.grabX = e.getXOnScreen();
			this.grabY = e.getYOnScreen();
			this.isDragging = true;
			this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			this.tileX = this.hoverX;
			this.tileY = this.hoverY;

			this.fireTileChange(this.tileX, this.tileY);
			this.repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.grabX = -1;
		this.grabY = -1;

		if (SwingUtilities.isMiddleMouseButton(e)) {
			this.isDragging = false;
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		this.updateHover(e.getX(), e.getY());
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		this.updateHover(e.getX(), e.getY());
	}

	@Override
	public void mouseExited(MouseEvent e) {
		this.updateHover(-1, -1);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (this.grabX != -1) {
				int x = this.imageOffsetX + (this.tileX * TILE_SIZE) + (TILE_SIZE / 2);
				int y = this.imageOffsetY + ((63 - this.tileY) * TILE_SIZE) + (TILE_SIZE / 2);
				this.angle = Math.atan2(e.getX() - x, e.getY() - y);
				this.fireAngleChange(this.angle);
				this.repaint();
			}
		} else if (SwingUtilities.isMiddleMouseButton(e) && this.isDragging) {
			int x = e.getXOnScreen();
			int y = e.getYOnScreen();

			int dx = this.grabX - x;
			int dy = this.grabY - y;

			if (dx != 0) {
				this.scrollbarHorizontal.setValue(this.scrollbarHorizontal.getValue() + dx);
			}

			if (dy != 0) {
				this.scrollbarVertical.setValue(this.scrollbarVertical.getValue() + dy);
			}

			this.grabX = x;
			this.grabY = y;
		}

		this.updateHover(e.getX(), e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		this.updateHover(e.getX(), e.getY());
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
	}

}
