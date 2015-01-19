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
package dane.runescape.mapeditor.event;

import java.util.EventObject;

/**
 *
 * @author Dane
 */
public class MapPanelEvent extends EventObject {

	private static final long serialVersionUID = 7048208543452742955L;

	public enum Type {

		TILE_CHANGE, ZOOM_ADJUST, ANGLE_CHANGE;
	}

	private int tileX, tileY;
	private int zoomAdjustment;
	private double angle;
	private Type type;

	public MapPanelEvent(Object source, int tileX, int tileY) {
		this(source, Type.TILE_CHANGE);
		this.tileX = tileX;
		this.tileY = tileY;
	}

	public MapPanelEvent(Object source, int zoomAdjustment) {
		this(source, Type.ZOOM_ADJUST);
		this.zoomAdjustment = zoomAdjustment;
	}

	public MapPanelEvent(Object source, double angle) {
		this(source, Type.ANGLE_CHANGE);
		this.angle = angle;
	}

	private MapPanelEvent(Object source, Type type) {
		super(source);
		this.type = type;
	}

	public Type getType() {
		return this.type;
	}

	public double getAngle() {
		return this.angle;
	}

	public int getTileX() {
		return this.tileX;
	}

	public int getTileY() {
		return tileY;
	}

	public int getZoomAdjustment() {
		return this.zoomAdjustment;
	}
}
