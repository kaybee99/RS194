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

import java.awt.Point;

/**
 * TODO: find a better place for this.
 *
 * @author Dane
 */
public class OrbitCamera {

	private int x;
	private int y;
	private int z;
	private int pitch = 128;
	private int yaw = 0;
	private int zoom = 600;

	// below are interpolated
	private int currentX;
	private int currentY;
	private int currentZ;
	private int currentPitch = 128;
	private int currentYaw;
	private int currentZoom = 128;

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

		// smoothness all around ;)
		currentPitch += (pitch - currentPitch) / 4;
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

	public int getPitch() {
		return pitch;
	}

	public int getCurrentPitch() {
		return currentPitch;
	}

	public void setPitch(int pitch) {
		this.pitch = pitch;
	}

	public void setCurrentPitch(int currentPitch) {
		this.currentPitch = currentPitch;
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
