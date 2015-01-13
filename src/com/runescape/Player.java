package com.runescape;

final class Player extends Entity {

	String name;
	boolean visible = false;
	int gender;
	int headicons;
	int[] appearanceindex = new int[13];
	int[] appearanceColors = new int[5];
	int level;
	long uid;
	int sceneY;
	int locFirstCycle;
	int locLastCycle;
	int locSceneX;
	int locSceneY;
	int locSceneZ;
	Model locModel;
	int locMinTileX;
	int locMinTileY;
	int locMaxTileX;
	int locMaxTileY;
	boolean lowmemory = false;
	static List uniqueModelCache = new List(200);

	public final void read(Buffer b) {
		b.pos = 0;
		gender = b.read();
		headicons = b.read();

		for (int n = 0; n < 13; n++) {
			int msb = b.read();

			if (msb == 0) {
				appearanceindex[n] = 0;
			} else {
				int lsb = b.read();
				appearanceindex[n] = (msb << 8) + lsb;
			}
		}

		for (int n = 0; n < 5; n++) {
			int i = b.read();
			if (i < 0 || i >= Game.APPEARANCE_COLORS[n].length) {
				i = 0;
			}
			appearanceColors[n] = i;
		}

		seqStand = b.readUShort();
		seqTurn = b.readUShort();
		seqWalk = b.readUShort();
		seqRun = b.readUShort();
		seqTurnRight = b.readUShort();
		seqTurnLeft = b.readUShort();
		name = StringUtil.getFormatted(StringUtil.fromBase37(b.readLong()));
		level = b.read();
		visible = true;
		uid = 0L;

		for (int n = 0; n < 12; n++) {
			uid <<= 4;

			if (appearanceindex[n] >= 256) {
				uid += (long) (appearanceindex[n] - 256);
			}
		}

		for (int n = 0; n < 5; n++) {
			uid <<= 3;
			uid += (long) appearanceColors[n];
		}

		uid <<= 1;
		uid += (long) gender;
	}

	@Override
	public final Model getDrawModel() {
		if (!visible) {
			return null;
		}

		Model model = getModel();

		height = model.minBoundY;

		if (lowmemory) {
			return model;
		}

		if (spotanimIndex != -1 && spotanimFrame != -1) {
			SpotAnim s = SpotAnim.instance[spotanimIndex];
			Model m = new Model(s.getModel(), false, true, !s.disposeAlpha, true);
			m.translate(0, -spotanimOffsetY, 0);
			m.applyGroups();
			m.applyFrame(s.seq.primaryFrames[spotanimFrame]);
			m.skinTriangle = null;
			m.labelVertices = null;
			m.applyLighting(64, 850, -30, -50, -30, true);

			model = new Model(new Model[]{model, m}, 2, true, 20525);
		}

		if (locModel != null) {
			if (Game.cycle >= locLastCycle) {
				locModel = null;
			}

			if (Game.cycle >= locFirstCycle && Game.cycle < locLastCycle) {
				Model m = locModel;
				m.translate(locSceneX - sceneX, locSceneY - sceneY, locSceneZ - sceneZ);

				if (dstYaw == 512) {
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
				} else if (dstYaw == 1024) {
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
				} else if (dstYaw == 1536) {
					m.rotateCounterClockwise();
				}

				// merge player model with loc model (like chris said lel)
				model = new Model(new Model[]{model, m}, 2, true, 20525);

				if (dstYaw == 512) {
					m.rotateCounterClockwise();
				} else if (dstYaw == 1024) {
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
				} else if (dstYaw == 1536) {
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
					m.rotateCounterClockwise();
				}

				m.translate(sceneX - locSceneX, sceneY - locSceneY, sceneZ - locSceneZ);
			}
		}
		return model;
	}

	public final Model getModel() {
		long bitset = uid;
		int primaryFrame = -1;
		int secondaryFrame = -1;
		int shieldOverride = -1;
		int weaponOverride = -1;

		if (primarySeqIndex >= 0 && primarySeqDelay == 0) {
			Seq s = Seq.instance[primarySeqIndex];
			primaryFrame = s.primaryFrames[primarySeqFrame];

			if (secondarySeqIndex >= 0 && secondarySeqIndex != seqStand) {
				secondaryFrame = Seq.instance[secondarySeqIndex].primaryFrames[secondarySeqFrame];
			}

			if (s.shieldOverride >= 0) {
				shieldOverride = s.shieldOverride;
				bitset += (long) (shieldOverride - appearanceindex[5] << 8);
			}

			if (s.weaponOverride >= 0) {
				weaponOverride = s.weaponOverride;
				bitset += (long) (shieldOverride - appearanceindex[3] << 16);
			}
		} else if (secondarySeqIndex >= 0) {
			primaryFrame = Seq.instance[secondarySeqIndex].primaryFrames[secondarySeqFrame];
		}

		Model m = (Model) uniqueModelCache.get(bitset);

		if (m == null) {
			NPCConfig c = NPCConfig.get(666);

			if (c != null) {
				seqWalk = c.seqWalk;
				seqRun = c.seqRun;
				seqTurnLeft = c.seqTurnLeft;
				seqTurnRight = c.seqTurnRight;
				seqStand = c.seqStand;

				Model[] models = new Model[c.modelIndices.length];
				int n = 0;

				for (int i = 0; i < models.length; i++) {
					Model model = new Model(c.modelIndices[i]);

					if (model != null) {
						models[n++] = model;
					}
				}

				if (n > 0) {
					m = new Model(models, n);

					if (c.oldColors != null) {
						for (int i = 0; i < c.oldColors.length; i++) {
							m.recolor(c.oldColors[i], c.newColors[i]);
						}
					}

					m.applyGroups();
					m.applyLighting(64, 850, -30, -50, -30, true);
					uniqueModelCache.put(m, bitset);
				}
			}
		}

		if (m == null) {
			Model[] models = new Model[13];
			int n = 0;

			for (int i = 0; i < 13; i++) {
				int index = appearanceindex[i];

				if (weaponOverride >= 0 && i == 3) {
					index = weaponOverride;
				}

				if (shieldOverride >= 0 && i == 5) {
					index = shieldOverride;
				}

				if (index >= 256 && index < 512) {
					models[n++] = IdentityKit.instance[index - 256].getModel();
				}

				if (index >= 512) {
					ObjConfig o = ObjConfig.get(index - 512);
					Model objModel = o.getWornModel(gender);

					if (objModel != null) {
						models[n++] = objModel;
					}
				}
			}

			m = new Model(models, n);

			for (int part = 0; part < 5; part++) {
				if (appearanceColors[part] != 0) {
					m.recolor((Game.APPEARANCE_COLORS[part][0]), (Game.APPEARANCE_COLORS[part][appearanceColors[part]]));

					if (part == 1) {
						m.recolor(Game.BEARD_COLORS[0], (Game.BEARD_COLORS[appearanceColors[part]]));
					}
				}
			}

			m.applyGroups();
			m.applyLighting(64, 850, -30, -50, -30, true);
			uniqueModelCache.put(m, bitset);
		}

		if (lowmemory) {
			return m;
		}

		m = new Model(m, true);

		if (primaryFrame != -1 && secondaryFrame != -1) {
			m.applyFrames(primaryFrame, secondaryFrame, Seq.instance[primarySeqIndex].labelGroups);
		} else if (primaryFrame != -1) {
			m.applyFrame(primaryFrame);
		}

		m.setupBBoxDepth();
		m.skinTriangle = null;
		m.labelVertices = null;
		return m;
	}

	public final Model getHeadModel() {
		if (!visible) {
			return null;
		}

		Model[] models = new Model[13];
		int count = 0;
		for (int n = 0; n < 13; n++) {
			int i = appearanceindex[n];

			if (i >= 256 && i < 512) {
				models[count++] = IdentityKit.instance[i - 256].getHeadModel();
			}

			if (i >= 512) {
				Model m = ObjConfig.get(i - 512).getHeadModel(gender);

				if (m != null) {
					models[count++] = m;
				}
			}
		}

		Model m = new Model(models, count);
		for (int n = 0; n < 5; n++) {
			if (appearanceColors[n] != 0) {
				m.recolor((Game.APPEARANCE_COLORS[n][0]), (Game.APPEARANCE_COLORS[n][appearanceColors[n]]));
				if (n == 1) {
					m.recolor(Game.BEARD_COLORS[0], (Game.BEARD_COLORS[appearanceColors[n]]));
				}
			}
		}
		return m;
	}

	public final boolean isVisible() {
		if (!visible) {
			return false;
		}
		return true;
	}
}
