package com.runescape;

public class ObjectInfo {

	public static int count;
	private static int[] pointers;
	private static Buffer buffer;
	private static ObjectInfo[] cache;
	private static int cachepos;
	public static LinkedList uniqueModelCache = new LinkedList(50);
	static LinkedList spriteCache = new LinkedList(200);

	public int index = -1;
	public int modelIndex;
	public String name;
	public byte[] description;
	public int[] oldColors;
	public int[] newColors;
	public int iconZoom;
	public int iconCameraPitch;
	public int iconYaw;
	public int iconRoll;
	public int iconX;
	public int iconY;
	public boolean stackable;
	public int priority;
	public boolean members;
	public String[] groundActions;
	public String[] actions;
	public int maleModel0;
	public int maleModel1;
	public byte maleOffsetY;
	public int femaleModel0;
	public int femaleModel1;
	public byte femaleOffsetY;
	public int maleHeadModelA;
	public int maleHeadModelB;
	public int femaleHeadModelA;
	public int femaleHeadModelB;

	public static final int getCount() {
		return count;
	}

	public static final void load(Archive a) {
		buffer = new Buffer(a.get("obj.dat", null));
		Buffer b = new Buffer(a.get("obj.idx", null));
		count = b.readUShort();
		pointers = new int[count];

		int i = 2;
		for (int n = 0; n < count; n++) {
			pointers[n] = i;
			i += b.readUShort();
		}

		cache = new ObjectInfo[10];

		for (i = 0; i < 10; i++) {
			cache[i] = new ObjectInfo();
		}
	}

	public static final void unload() {
		uniqueModelCache = null;
		spriteCache = null;
		pointers = null;
		cache = null;
		buffer = null;
	}

	public static final ObjectInfo get(int i) {
		for (int n = 0; n < 10; n++) {
			if (cache[n].index == i) {
				return cache[n];
			}
		}
		cachepos = (cachepos + 1) % 10;
		ObjectInfo o = cache[cachepos];
		buffer.position = pointers[i];
		o.index = i;
		o.reset();
		o.read(buffer);
		return o;
	}

	public final void reset() {
		modelIndex = 0;
		name = null;
		description = null;
		oldColors = null;
		newColors = null;
		iconZoom = 2000;
		iconCameraPitch = 0;
		iconYaw = 0;
		iconRoll = 0;
		iconX = 0;
		iconY = 0;
		stackable = false;
		priority = 1;
		members = false;
		groundActions = null;
		actions = null;
		maleModel0 = -1;
		maleModel1 = -1;
		maleOffsetY = (byte) 0;
		femaleModel0 = -1;
		femaleModel1 = -1;
		femaleOffsetY = (byte) 0;
		maleHeadModelA = -1;
		maleHeadModelB = -1;
		femaleHeadModelA = -1;
		femaleHeadModelB = -1;
	}

	public final void read(Buffer b) {
		for (;;) {
			int opcode = b.read();

			if (opcode == 0) {
				break;
			}

			if (opcode == 1) {
				modelIndex = b.readUShort();
			} else if (opcode == 2) {
				name = b.readString();
			} else if (opcode == 3) {
				description = b.readStringBytes();
			} else if (opcode == 4) {
				iconZoom = b.readUShort();
			} else if (opcode == 5) {
				iconCameraPitch = b.readUShort();
			} else if (opcode == 6) {
				iconYaw = b.readUShort();
			} else if (opcode == 7) {
				iconX = b.readUShort();
				if (iconX > 32767) {
					iconX -= 65536;
				}
			} else if (opcode == 8) {
				iconY = b.readUShort();
				if (iconY > 32767) {
					iconY -= 65536;
				}
			} else if (opcode == 9) {
			} else if (opcode == 10) {
				b.readUShort();
			} else if (opcode == 11) {
				stackable = true;
			} else if (opcode == 12) {
				priority = b.readInt();
			} else if (opcode == 16) {
				members = true;
			} else if (opcode == 23) {
				maleModel0 = b.readUShort();
				maleOffsetY = b.readByte();
			} else if (opcode == 24) {
				maleModel1 = b.readUShort();
			} else if (opcode == 25) {
				femaleModel0 = b.readUShort();
				femaleOffsetY = b.readByte();
			} else if (opcode == 26) {
				femaleModel1 = b.readUShort();
			} else if (opcode >= 30 && opcode < 35) {
				if (groundActions == null) {
					groundActions = new String[5];
				}
				groundActions[opcode - 30] = b.readString();
			} else if (opcode >= 35 && opcode < 40) {
				if (actions == null) {
					actions = new String[5];
				}
				actions[opcode - 35] = b.readString();
			} else if (opcode == 40) {
				int n = b.read();
				oldColors = new int[n];
				newColors = new int[n];
				for (int i_5_ = 0; i_5_ < n; i_5_++) {
					oldColors[i_5_] = b.readUShort();
					newColors[i_5_] = b.readUShort();
				}
			} else if (opcode == 90) {
				maleHeadModelA = b.readUShort();
			} else if (opcode == 91) {
				femaleHeadModelA = b.readUShort();
			} else if (opcode == 92) {
				maleHeadModelB = b.readUShort();
			} else if (opcode == 93) {
				femaleHeadModelB = b.readUShort();
			} else if (opcode == 95) {
				iconRoll = b.readUShort();
			}
		}
	}

	public final Model getModel() {
		Model m = (Model) uniqueModelCache.get((long) index);

		if (m != null) {
			return m;
		}

		m = new Model(modelIndex);

		if (oldColors != null) {
			for (int i = 0; i < oldColors.length; i++) {
				m.recolor(oldColors[i], newColors[i]);
			}
		}

		m.applyLighting(64, 768, -50, -10, -50, true);
		uniqueModelCache.put(m, (long) index);
		return m;
	}

	public static final Sprite getSprite(int index) {
		Sprite s = (Sprite) spriteCache.get((long) index);

		if (s != null) {
			return s;
		}

		ObjectInfo i = get(index);
		s = new Sprite(32, 32);

		int centerX = Graphics3D.centerX;
		int centerY = Graphics3D.centerY;
		int[] offsets = Graphics3D.offsets;
		int[] data = Graphics2D.target;
		int width = Graphics2D.targetWidth;
		int height = Graphics2D.targetHeight;

		Graphics3D.texturedShading = false;
		Graphics2D.prepare(s.pixels, 32, 32);
		Graphics2D.fillRect(0, 0, 32, 32, 0);
		Graphics3D.prepareOffsets();

		Model m = i.getModel();

		int cameraY = (Graphics3D.sin[i.iconCameraPitch] * i.iconZoom) >> 16;
		int cameraZ = (Graphics3D.cos[i.iconCameraPitch] * i.iconZoom) >> 16;

		m.draw(0, i.iconYaw, i.iconRoll, i.iconX, (cameraY + (m.maxBoundY / 2) + i.iconY), cameraZ + i.iconY, i.iconCameraPitch);

		for (int x = 31; x >= 0; x--) {
			for (int y = 31; y >= 0; y--) {
				if (s.pixels[x + y * 32] == 0) {
					if (x > 0 && (s.pixels[x - 1 + y * 32]) > 1) {
						s.pixels[(x + y * 32)] = 1;
					} else if (y > 0 && (s.pixels[x + (y - 1) * 32]) > 1) {
						s.pixels[(x + y * 32)] = 1;
					} else if (x < 31 && (s.pixels[x + 1 + y * 32]) > 1) {
						s.pixels[(x + y * 32)] = 1;
					} else if (y < 31 && (s.pixels[x + (y + 1) * 32]) > 1) {
						s.pixels[(x + y * 32)] = 1;
					}
				}
			}
		}

		for (int x = 31; x >= 0; x--) {
			for (int y = 31; y >= 0; y--) {
				if (s.pixels[x + y * 32] == 0 && x > 0 && y > 0 && (s.pixels[x - 1 + (y - 1) * 32]) > 0) {
					s.pixels[x + y * 32] = 0x302020;
				}
			}
		}

		spriteCache.put(s, (long) index);

		Graphics2D.prepare(data, width, height);
		Graphics3D.centerX = centerX;
		Graphics3D.centerY = centerY;
		Graphics3D.offsets = offsets;
		Graphics3D.texturedShading = true;

		if (i.stackable) {
			s.clipWidth = 33;
		} else {
			s.clipWidth = 32;
		}

		return s;
	}

	public final Model getWornModel(int gender) {
		int model0 = maleModel0;

		if (gender == 1) {
			model0 = femaleModel0;
		}

		if (model0 == -1) {
			return null;
		}

		int model1 = maleModel1;

		if (gender == 1) {
			model1 = femaleModel1;
		}

		Model m = new Model(model0);

		if (model1 != -1) {
			m = new Model(new Model[]{m, new Model(model1)}, 2);
		}

		if (gender == 0 && maleOffsetY != 0) {
			m.translate(0, maleOffsetY, 0);
		}

		if (gender == 1 && femaleOffsetY != 0) {
			m.translate(0, femaleOffsetY, 0);
		}

		if (oldColors != null) {
			for (int n = 0; n < oldColors.length; n++) {
				m.recolor(oldColors[n], newColors[n]);
			}
		}

		return m;
	}

	public final Model getHeadModel(int gender) {
		int modelA = maleHeadModelA;

		if (gender == 1) {
			modelA = femaleHeadModelA;
		}

		if (modelA == -1) {
			return null;
		}

		int modelB = maleHeadModelB;

		if (gender == 1) {
			modelB = femaleHeadModelB;
		}

		Model m = new Model(modelA);

		if (modelB != -1) {
			m = new Model(new Model[]{m, new Model(modelB)}, 2);
		}

		if (oldColors != null) {
			for (int n = 0; n < oldColors.length; n++) {
				m.recolor(oldColors[n], newColors[n]);
			}
		}

		return m;
	}

}
