package dane.runescape.mapeditor.event;

import java.util.EventObject;

public class CameraEvent extends EventObject {

	private static final long serialVersionUID = 7048208543452742955L;

	public enum Type {

		TILE_CHANGE, ZOOM_ADJUST, ANGLE_CHANGE;
	}

	private int tileX, tileY;
	private int zoomAdjustment;
	private double angle;
	private Type type;

	public CameraEvent(Object source, int tileX, int tileY) {
		this(source, Type.TILE_CHANGE);
		this.tileX = tileX;
		this.tileY = tileY;
	}

	public CameraEvent(Object source, int zoomAdjustment) {
		this(source, Type.ZOOM_ADJUST);
		this.zoomAdjustment = zoomAdjustment;
	}

	public CameraEvent(Object source, double angle) {
		this(source, Type.ANGLE_CHANGE);
		this.angle = angle;
	}

	private CameraEvent(Object source, Type type) {
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
