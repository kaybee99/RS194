package com.runescape;

public final class AnimatedLocation extends Link {

	public int plane;
	public int classtype;
	public int tileX;
	public int tileY;
	public int locIndex;
	public Animation animation;
	public int animFrame;
	public int animCycle;

	public AnimatedLocation(Animation animation, int locIndex, int type, int tileX, int tileY, int plane) {
		this.plane = plane;
		this.classtype = type;
		this.tileX = tileX;
		this.tileY = tileY;
		this.locIndex = locIndex;
		this.animation = animation;
		this.animFrame = -1;
		this.animCycle = 0;
	}
}
