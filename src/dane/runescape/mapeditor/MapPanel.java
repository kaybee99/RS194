package dane.runescape.mapeditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import com.runescape.Canvas3D;
import com.runescape.Flo;
import com.runescape.Landscape;
import com.runescape.Scene;

import dane.runescape.mapeditor.event.CameraUpdateListener;
import dane.runescape.mapeditor.event.GameListener;
import dane.runescape.mapeditor.media.TileShape;

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

	/* Camera Event Listener */
	public CameraUpdateListener[] getCameraEventListeners() {
		return listenerList.getListeners(CameraUpdateListener.class);
	}

	public void addCameraEventListener(CameraUpdateListener l) {
		listenerList.add(CameraUpdateListener.class, l);
	}

	public void removeCameraEventListener(CameraUpdateListener l) {
		listenerList.remove(CameraUpdateListener.class, l);
	}

	/* Scene Load Listener */
	@Override
	public void onSceneCreation(int plane, Scene s, Landscape l) {
		byte[][] overlayTypes = s.planeOverlayTypes[plane];
		byte[][] overlayRotations = s.planeOverlayRotations[plane];
		byte[][] underlayFlos = s.planeUnderlayFloIndices[plane];
		byte[][] overlayFlos = s.planeOverlayFloIndices[plane];

		Arrays.fill(pixels, 0x282018);
		Graphics g = this.image.getGraphics();

		int h = (this.image.getHeight() - TILE_SIZE + 1);

		for (int x = 0; x < MAPSQUARE_SIZE; x++) {
			for (int z = 0; z < MAPSQUARE_SIZE; z++) {
				int underlayFloId = underlayFlos[x][z];
				int overlayFloId = overlayFlos[x][z];

				Flo underlayFlo = null;
				Flo overlayFlo = null;

				if (overlayFloId != 0) {
					underlayFlo = Flo.instances[overlayFloId - 1];
				}

				if (underlayFloId != 0) {
					overlayFlo = Flo.instances[underlayFloId - 1];
				}

				int drawX = (x * TILE_SIZE) + 1;
				int drawY = h - (z * TILE_SIZE);

				if (overlayFlo != null) {
					if (overlayFlo.textureIndex >= 0) {
						g.setColor(new Color(Canvas3D.getAverageTextureRGB(overlayFlo.textureIndex)));
					} else {
						g.setColor(new Color(overlayFlo.rgb));
					}

					g.fillRect(drawX, drawY, 14, 14);
				}

				if (underlayFlo != null) {
					if (underlayFlo.textureIndex >= 0) {
						g.setColor(new Color(Canvas3D.getAverageTextureRGB(underlayFlo.textureIndex)));
					} else {
						g.setColor(new Color(underlayFlo.rgb));
					}

					// they draw 1 pixel off so we gotta counteract that.
					drawX -= 1;
					drawY -= 1;

					g.translate(drawX, drawY);
					g.fillPolygon(TileShape.getPolygon(overlayTypes[x][z] + 1, overlayRotations[x][z]));
					g.translate(-drawX, -drawY);
				}
			}
		}

		g.dispose();
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.translate(this.imageOffsetX, this.imageOffsetY);
		g.drawImage(this.image, 0, 0, null);

		if (this.hoverX != -1 && this.hoverY != -1) {
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
			this.updateHover(e.getX(), e.getY());

			this.tileX = this.hoverX;
			this.tileY = this.hoverY;

			// TODO: send tile update
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
				// TOOD: send angle update
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
