package com.runescape;

public final class TemporaryLoc extends Link {

	public int plane;
	public int classtype;
	public int tileX;
	public int tileY;
	public int locIndex;
	public int rotation;
	public int type;
	public int lastCycle;

	public TemporaryLoc(int locIndex, int tileX, int tileY, int plane, int type, int rotation, int classtype, int lastCycle) {
		this.plane = plane;
		this.classtype = classtype;
		this.tileX = tileX;
		this.tileY = tileY;
		this.locIndex = locIndex;
		this.rotation = rotation;
		this.type = type;
		this.lastCycle = lastCycle;
	}
}
