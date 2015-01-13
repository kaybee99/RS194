package com.runescape;

public final class NPCConfig {

	public static int count;
	private static int[] pointers;
	private static Buffer data;
	private static NPCConfig[] cache;
	private static int cachepos;
	private int index = -1;
	public String name;
	public byte[] description;
	public byte size = 1;
	public int[] modelIndices;
	private int[] headModelIndices;
	public int seqStand = -1;
	public int seqWalk = -1;
	public int seqRun = -1;
	public int seqTurnRight = -1;
	public int seqTurnLeft = -1;
	private boolean disposeAlpha = false;
	public int[] oldColors;
	public int[] newColors;
	public String[] actions;
	public int anInt599 = -1;
	public int anInt600 = -1;
	public int anInt601 = -1;
	public boolean aBoolean602 = true;
	public int level = -1;
	private int scaleX = 128;
	private int scaleY = 128;
	public static List uniqueModelCache = new List(30);

	public static final int getCount() {
		return count;
	}

	public static final void load(Archive a) {
		data = new Buffer(a.get("npc.dat", null));
		Buffer idx = new Buffer(a.get("npc.idx", null));
		count = idx.readUShort();
		pointers = new int[count];

		int off = 2;
		for (int n = 0; n < count; n++) {
			pointers[n] = off;
			off += idx.readUShort();
		}

		cache = new NPCConfig[20];
		for (int n = 0; n < 20; n++) {
			cache[n] = new NPCConfig();
		}
	}

	public static final void unload() {
		uniqueModelCache = null;
		pointers = null;
		cache = null;
		data = null;
	}

	public static final NPCConfig get(int i) {
		for (int n = 0; n < 20; n++) {
			if (cache[n].index == i) {
				return cache[n];
			}
		}
		cachepos = (cachepos + 1) % 20;
		NPCConfig n = cache[cachepos] = new NPCConfig();
		data.pos = pointers[i];
		n.index = i;
		n.read(data);
		return n;
	}

	private final void read(Buffer b) {
		for (;;) {
			int opcode = b.read();
			if (opcode == 0) {
				break;
			}
			if (opcode == 1) {
				int n = b.read();
				modelIndices = new int[n];
				for (int m = 0; m < n; m++) {
					modelIndices[m] = b.readUShort();
				}
			} else if (opcode == 2) {
				name = b.readString();
			} else if (opcode == 3) {
				description = b.readStringBytes();
			} else if (opcode == 12) {
				size = b.readByte();
			} else if (opcode == 13) {
				seqStand = b.readUShort();
			} else if (opcode == 14) {
				seqWalk = b.readUShort();
			} else if (opcode == 16) {
				disposeAlpha = true;
			} else if (opcode == 17) {
				seqWalk = b.readUShort();
				seqRun = b.readUShort();
				seqTurnRight = b.readUShort();
				seqTurnLeft = b.readUShort();
			} else if (opcode >= 30 && opcode < 40) {
				if (actions == null) {
					actions = new String[5];
				}
				actions[opcode - 30] = b.readString();
			} else if (opcode == 40) {
				int n = b.read();
				oldColors = new int[n];
				newColors = new int[n];
				for (int i_8_ = 0; i_8_ < n; i_8_++) {
					oldColors[i_8_] = b.readUShort();
					newColors[i_8_] = b.readUShort();
				}
			} else if (opcode == 60) {
				int n = b.read();
				headModelIndices = new int[n];
				for (int m = 0; m < n; m++) {
					headModelIndices[m] = b.readUShort();
				}
			} else if (opcode == 90) {
				anInt599 = b.readUShort();
			} else if (opcode == 91) {
				anInt600 = b.readUShort();
			} else if (opcode == 92) {
				anInt601 = b.readUShort();
			} else if (opcode == 93) {
				aBoolean602 = false;
			} else if (opcode == 95) {
				level = b.readUShort();
			} else if (opcode == 97) {
				scaleX = b.readUShort();
			} else if (opcode == 98) {
				scaleY = b.readUShort();
			}
		}
	}

	public final Model getModel(int primaryFrame, int secondaryFrame, int[] labelGroups) {
		Model m = (Model) uniqueModelCache.get(index);

		if (m == null) {
			Model[] models = new Model[modelIndices.length];

			for (int n = 0; n < modelIndices.length; n++) {
				models[n] = new Model(modelIndices[n]);
			}

			if (models.length == 1) {
				m = models[0];
			} else {
				m = new Model(models, models.length);
			}

			if (oldColors != null) {
				for (int n = 0; n < oldColors.length; n++) {
					m.recolor(oldColors[n], newColors[n]);
				}
			}

			m.applyGroups();
			m.applyLighting(64, 850, -30, -50, -30, true);
			uniqueModelCache.put(m, index);
		}

		m = new Model(m, !disposeAlpha);

		if (primaryFrame != -1 && secondaryFrame != -1) {
			m.applyFrames(primaryFrame, secondaryFrame, labelGroups);
		} else if (primaryFrame != -1) {
			m.applyFrame(primaryFrame);
		}

		if (scaleX != 128 || scaleY != 128) {
			m.scale(scaleX, scaleY, scaleX);
		}

		m.setupBBoxDepth();
		m.skinTriangle = null;
		m.labelVertices = null;
		return m;
	}

	public final Model getHeadModel() {
		if (headModelIndices == null) {
			return null;
		}

		Model[] models = new Model[headModelIndices.length];

		for (int n = 0; n < headModelIndices.length; n++) {
			models[n] = new Model(headModelIndices[n]);
		}

		Model m;

		if (models.length == 1) {
			m = models[0];
		} else {
			m = new Model(models, models.length);
		}

		if (oldColors != null) {
			for (int n = 0; n < oldColors.length; n++) {
				m.recolor(oldColors[n], newColors[n]);
			}
		}
		return m;
	}

}
