package com.runescape;

final class SpotAnimationEntity extends Renderable {

	private final SpotAnimation spotanim;
	public int firstCycle;
	public int plane;
	public int x;
	public int z;
	public int y;
	private int seqFrame;
	private int frameCycle;
	public boolean finished = false;

	public SpotAnimationEntity(int x, int y, int z, int plane, int spotanimIndex, int startCycle, int duration) {
		this.spotanim = SpotAnimation.instance[spotanimIndex];
		this.plane = plane;
		this.x = x;
		this.z = z;
		this.y = y;
		this.firstCycle = startCycle + duration;
		this.finished = false;
	}

	public final void update(int cycle) {
		frameCycle += cycle;

		while (frameCycle > spotanim.animation.frameDuration[seqFrame]) {
			frameCycle -= spotanim.animation.frameDuration[seqFrame] + 1;
			seqFrame++;
			if (seqFrame >= spotanim.animation.frameCount) {
				seqFrame = 0;
				finished = true;
			}
		}
	}

	@Override
	public final Model getDrawModel() {
		Model m = new Model(spotanim.getModel(), false, true, !spotanim.disposeAlpha, true);

		if (!finished) {
			m.applyGroups();
			m.applyFrame((spotanim.animation.primaryFrames[seqFrame]));
			m.skinTriangle = null;
			m.labelVertices = null;
		}

		m.applyLighting(64, 850, -30, -50, -30, true);
		return m;
	}
}
