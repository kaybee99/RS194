package dane.runescape.mapeditor;

import java.awt.Point;

public class OrbitCamera {

	private int x;
	private int y;
	private int z;
	private int zoom = 600;
	private int yaw = 0;

	// below are interpolated
	private int currentX;
	private int currentY;
	private int currentZ;
	private int currentZoom = 128;
	private int currentYaw;

	public void update() {
		currentX += (x - currentX) / 8;
		currentY += (y - currentY) / 8;
		currentZ += (z - currentZ) / 8;

		int dy = yaw - currentYaw;

		// no mo snappin ;)
		if (dy >= 1024) {
			dy -= 2048;
		} else if (dy <= -1024) {
			dy += 2048;
		}

		currentYaw = (currentYaw + (dy / 4)) & 0x7FF;
		currentZoom += (zoom - currentZoom) / 8;
	}

	public void gotoTile(int tileX, int tileZ) {
		this.x = (tileX * 128) + 64;
		this.z = (tileZ * 128) + 64;
	}

	public Point getDestTile() {
		return new Point(x / 128, y / 128);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public int getYaw() {
		return yaw;
	}

	public void setYaw(int destYaw) {
		this.yaw = destYaw;
	}

	public int getCurrentX() {
		return currentX;
	}

	public void setCurrentX(int x) {
		this.currentX = x;
	}

	public int getCurrentY() {
		return currentY;
	}

	public void setCurrentY(int y) {
		this.currentY = y;
	}

	public int getCurrentZ() {
		return currentZ;
	}

	public void setCurrentZ(int z) {
		this.currentZ = z;
	}

	public int getCurrentZoom() {
		return currentZoom;
	}

	public void setCurrentZoom(int zoom) {
		this.currentZoom = zoom;
	}

	public int getCurrentYaw() {
		return currentYaw;
	}

	public void setCurrentYaw(int yaw) {
		this.currentYaw = yaw;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(16);
		sb.append("[OrbitCamera: ");
		sb.append(currentX).append(',').append(currentY).append(',').append(currentZ);
		sb.append(']');
		return sb.toString();
	}

}
