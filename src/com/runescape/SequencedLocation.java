package com.runescape;

public final class SequencedLocation extends Link {

	public int plane;
	public int classtype;
	public int tileX;
	public int tileY;
	public int locIndex;
	public Sequence seq;
	public int seqFrame;
	public int seqCycle;

	public SequencedLocation(Sequence s, int locIndex, int type, int tileX, int tileY, int plane) {
		this.plane = plane;
		this.classtype = type;
		this.tileX = tileX;
		this.tileY = tileY;
		this.locIndex = locIndex;
		this.seq = s;
		this.seqFrame = -1;
		this.seqCycle = 0;
	}
}
