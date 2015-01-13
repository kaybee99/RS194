package com.runescape;

final class SpotAnimEntity extends Renderable {
	private SpotAnim spotanim;
	public int firstCycle;
	public int plane;
	public int x;
	public int y;
	public int z;
	private int seqFrame;
	private int frameCycle;
	public boolean finished = false;

	public SpotAnimEntity(int i, int i_0_, int duration, int spotanim, int plane, int i_4_, int i_5_, int firstCycle) {
		this.spotanim = SpotAnim.instance[spotanim];
		this.plane = plane;
		this.x = i_4_;
		this.y = i_5_;
		this.z = i;
		this.firstCycle = firstCycle + duration;
		this.finished = false;
	}

	public final void update(int cycle) {
		frameCycle += cycle;

		while (frameCycle > spotanim.seq.frameDuration[seqFrame]) {
			frameCycle -= spotanim.seq.frameDuration[seqFrame] + 1;
			seqFrame++;
			if (seqFrame >= spotanim.seq.frameCount) {
				seqFrame = 0;
				finished = true;
			}
		}
	}

	public final Model getDrawModel() {
		Model m = new Model(spotanim.getModel(), false, true, !spotanim.disposeAlpha, true);

		if (!finished) {
			m.applyGroups();
			m.applyFrame((spotanim.seq.primaryFrames[seqFrame]));
			m.skinTriangle = null;
			m.labelVertices = null;
		}

		m.applyLighting(64, 850, -30, -50, -30, true);
		return m;
	}
}
