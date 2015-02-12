package com.runescape;

final class NPC extends Entity {

	NPCInfo config;

	@Override
	public final Model getDrawModel() {
		if (config == null) {
			return null;
		}

		if (spotanimIndex == -1 || spotanimFrame == -1) {
			return getModel();
		}

		SpotAnimation spotanim = SpotAnimation.instance[spotanimIndex];
		Model m = new Model(spotanim.getModel(), false, true, !spotanim.disposeAlpha, true);

		m.translate(0, -spotanimOffsetY, 0);

		m.applyGroups();
		m.applyFrame(spotanim.animation.primaryFrames[spotanimFrame]);

		m.skinTriangle = null;
		m.labelVertices = null;

		m.applyLighting(64, 850, -30, -50, -30, true);
		return new Model(new Model[]{getModel(), m}, 2, true, 20525);
	}

	public final Model getModel() {
		if (primaryAnimIndex >= 0 && primaryAnimDelay == 0) {
			int frame1 = Animation.instance[primaryAnimIndex].primaryFrames[primaryAnimFrame];
			int frame2 = -1;

			if (secondaryAnimIndex >= 0 && secondaryAnimIndex != animStand) {
				frame2 = (Animation.instance[secondaryAnimIndex].primaryFrames[secondaryAnimFrame]);
			}

			return config.getModel(frame1, frame2, Animation.instance[primaryAnimIndex].labelGroups);
		}

		int frame = -1;

		if (secondaryAnimIndex >= 0) {
			frame = Animation.instance[secondaryAnimIndex].primaryFrames[secondaryAnimFrame];
		}

		Model m = config.getModel(frame, -1, null);
		height = m.maxBoundY;
		return m;
	}

	public final boolean isValid() {
		return config != null;
	}
}
