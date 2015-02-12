package com.runescape;

public class Widget {

	public static final int TYPE_PARENT = 0;
	public static final int TYPE_UNUSED = 1;
	public static final int TYPE_INVENTORY = 2;
	public static final int TYPE_RECT = 3;
	public static final int TYPE_TEXT = 4;
	public static final int TYPE_IMAGE = 5;
	public static final int TYPE_MODEL = 6;
	public static final int TYPE_INVENTORY_TEXT = 7;

	public static final int OPTIONTYPE_OK = 1;
	public static final int OPTIONTYPE_SELECT = 4;
	public static final int OPTIONTYPE_SELECT2 = 5;
	public static final int OPTIONTYPE_CONTINUE = 6;

	public static Widget[] instances;

	private static LinkedList bitmapCache;
	private static LinkedList modelCache;

	private static Bitmap getBitmap(String name, Archive media, int index) {
		long l = (StringUtil.getHash(name) << 4) + (long) index;
		Bitmap b = (Bitmap) bitmapCache.get(l);

		if (b != null) {
			return b;
		}

		b = new Bitmap(media, name, index);
		bitmapCache.put(b, l);
		return b;
	}

	private static Model getModel(int index) {
		Model m = (Model) modelCache.get((long) index);

		if (m != null) {
			return m;
		}

		m = new Model(index);
		modelCache.put(m, (long) index);
		return m;
	}

	public static void load(BitmapFont[] fonts, Archive media, Archive interfaces) {
		bitmapCache = new LinkedList(50000);
		modelCache = new LinkedList(50000);

		Buffer b = new Buffer(interfaces.get("data", null));
		instances = new Widget[b.readUShort()];

		int parent = -1;
		while (b.position < b.data.length) {
			int index = b.readUShort();

			if (index == 65535) {
				parent = b.readUShort();
				index = b.readUShort();
			}

			Widget w = instances[index] = new Widget();
			w.index = index;
			w.parent = parent;
			w.type = b.read();
			w.optionType = b.read();
			w.action = b.readUShort();
			w.width = b.readUShort();
			w.height = b.readUShort();
			w.hoverParentIndex = b.read();

			if (w.hoverParentIndex != 0) {
				w.hoverParentIndex = ((w.hoverParentIndex - 1 << 8) + b.read());
			} else {
				w.hoverParentIndex = -1;
			}

			int comparatorCount = b.read();

			if (comparatorCount > 0) {
				w.cscriptComparator = new int[comparatorCount];
				w.cscriptCompareValue = new int[comparatorCount];

				for (int n = 0; n < comparatorCount; n++) {
					w.cscriptComparator[n] = b.read();
					w.cscriptCompareValue[n] = b.readUShort();
				}
			}

			int scriptCount = b.read();

			if (scriptCount > 0) {
				w.cscript = new int[scriptCount][];

				for (int script = 0; script < scriptCount; script++) {
					int opcodes = b.readUShort();
					w.cscript[script] = new int[opcodes];

					for (int opcode = 0; opcode < opcodes; opcode++) {
						w.cscript[script][opcode] = b.readUShort();
					}
				}
			}

			if (w.type == TYPE_PARENT) {
				w.scrollHeight = b.readUShort();
				w.hidden = b.read() == 1;

				int n = b.read();
				w.children = new int[n];
				w.childX = new int[n];
				w.childY = new int[n];

				for (int m = 0; m < n; m++) {
					w.children[m] = b.readUShort();
					w.childX[m] = b.readShort();
					w.childY[m] = b.readShort();
				}
			}

			if (w.type == TYPE_UNUSED) {
				w.unusedInt = b.readUShort();
				w.unusedBool = b.read() == 1;
			}

			if (w.type == TYPE_INVENTORY) {
				w.inventoryIndices = new int[w.width * w.height];
				w.inventoryAmount = new int[w.width * w.height];

				w.inventoryDummy = b.read() == 1;
				w.inventoryHasActions = b.read() == 1;
				w.inventoryIsUsable = b.read() == 1;
				w.inventoryMarginX = b.read();
				w.inventoryMarginY = b.read();
				w.inventoryOffsetX = new int[20];
				w.inventoryOffsetY = new int[20];
				w.inventoryBitmap = new Bitmap[20];

				for (int n = 0; n < 20; n++) {
					if (b.read() == 1) {
						w.inventoryOffsetX[n] = b.readShort();
						w.inventoryOffsetY[n] = b.readShort();

						String s = b.readString();

						if (media != null && s.length() > 0) {
							int j = s.lastIndexOf(",");
							w.inventoryBitmap[n] = getBitmap(s.substring(0, j), media, (Integer.parseInt(s.substring(j + 1))));
						}
					}
				}

				w.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					w.inventoryActions[n] = b.readString();

					if (w.inventoryActions[n].length() == 0) {
						w.inventoryActions[n] = null;
					}
				}
			}

			if (w.type == TYPE_RECT) {
				w.fill = b.read() == 1;
			}

			if (w.type == TYPE_TEXT || w.type == TYPE_UNUSED) {
				w.centered = b.read() == 1;
				w.font = fonts[b.read()];
				w.shadow = b.read() == 1;
			}

			if (w.type == TYPE_TEXT) {
				w.messageDisabled = b.readString();
				w.messageEnabled = b.readString();
			}

			if (w.type == TYPE_UNUSED || w.type == TYPE_RECT || w.type == TYPE_TEXT) {
				w.colorDisabled = b.readInt();
			}

			if (w.type == TYPE_RECT || w.type == TYPE_TEXT) {
				w.colorEnabled = b.readInt();
				w.hoverColor = b.readInt();
			}

			if (w.type == TYPE_IMAGE) {
				String s = b.readString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					w.bitmapDisabled = getBitmap(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}

				s = b.readString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					w.bitmapEnabled = getBitmap(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}
			}

			if (w.type == TYPE_MODEL) {
				index = b.read();

				if (index != 0) {
					w.modelDisabled = getModel(((index - 1 << 8) + b.read()));
				}

				index = b.read();

				if (index != 0) {
					w.modelEnabled = getModel(((index - 1 << 8) + b.read()));
				}

				index = b.read();

				if (index != 0) {
					w.animIndexDisabled = (index - 1 << 8) + b.read();
				} else {
					w.animIndexDisabled = -1;
				}

				index = b.read();

				if (index != 0) {
					w.seqIndexEnabled = (index - 1 << 8) + b.read();
				} else {
					w.seqIndexEnabled = -1;
				}

				w.modelZoom = b.readUShort();
				w.modelCameraPitch = b.readUShort();
				w.modelYaw = b.readUShort();
			}

			if (w.type == TYPE_INVENTORY_TEXT) {
				w.inventoryIndices = new int[w.width * w.height];
				w.inventoryAmount = new int[w.width * w.height];
				w.centered = b.read() == 1;

				int font = b.read();

				if (fonts != null) {
					w.font = fonts[font];
				}

				w.shadow = b.read() == 1;
				w.colorDisabled = b.readInt();
				w.inventoryMarginX = b.readShort();
				w.inventoryMarginY = b.readShort();
				w.inventoryHasActions = b.read() == 1;
				w.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					w.inventoryActions[n] = b.readString();

					if (w.inventoryActions[n].length() == 0) {
						w.inventoryActions[n] = null;
					}
				}
			}

			if (w.optionType == 2 || w.type == TYPE_INVENTORY) {
				w.optionPrefix = b.readString();
				w.optionSuffix = b.readString();
				w.optionFlags = b.readUShort();
			}

			if (w.optionType == 1 || w.optionType == 4 || w.optionType == 5 || w.optionType == 6) {
				w.option = b.readString();

				if (w.option.length() == 0) {
					if (w.optionType == OPTIONTYPE_OK) {
						w.option = "Ok";
					}

					if (w.optionType == OPTIONTYPE_SELECT || w.optionType == OPTIONTYPE_SELECT2) {
						w.option = "Select";
					}

					if (w.optionType == OPTIONTYPE_CONTINUE) {
						w.option = "Continue";
					}
				}
			}
		}
		bitmapCache = null;
	}

	public int[] inventoryIndices;
	public int[] inventoryAmount;
	public int animFrame;
	public int animCycle;
	public int index;
	public int parent;
	public int type;
	public int optionType;
	public int action;
	public int width;
	public int height;
	public int[][] cscript;
	public int[] cscriptComparator;
	public int[] cscriptCompareValue;
	public int hoverParentIndex = -1;
	public int scrollHeight;
	public int scrollAmount;
	public boolean hidden;
	public int[] children;
	public int[] childX;
	public int[] childY;
	public int unusedInt;
	public boolean unusedBool;
	public boolean inventoryDummy;
	public boolean inventoryHasActions;
	public boolean inventoryIsUsable;
	public int inventoryMarginX;
	public int inventoryMarginY;
	public Bitmap[] inventoryBitmap;
	public int[] inventoryOffsetX;
	public int[] inventoryOffsetY;
	public String[] inventoryActions;
	public boolean fill;
	public boolean centered;
	public boolean shadow;
	public BitmapFont font;
	public String messageDisabled;
	public String messageEnabled;
	public int colorDisabled;
	public int colorEnabled;
	public int hoverColor;
	public Bitmap bitmapDisabled;
	public Bitmap bitmapEnabled;
	public Model modelDisabled;
	public Model modelEnabled;
	public int animIndexDisabled;
	public int seqIndexEnabled;
	public int modelZoom;
	public int modelCameraPitch;
	public int modelYaw;
	public String optionPrefix;
	public String optionSuffix;
	public int optionFlags;
	public String option;

	public Model getModel(int primaryFrame, int secondaryFrame, boolean enabled) {
		Model m = modelDisabled;

		if (enabled) {
			m = modelEnabled;
		}

		if (m == null) {
			return null;
		}

		if (primaryFrame == -1 && secondaryFrame == -1 && m.unmodifiedTriangleColor == null) {
			return m;
		}

		m = new Model(m, false, true, true, true);

		if (primaryFrame != -1 || secondaryFrame != -1) {
			m.applyGroups();
		}

		if (primaryFrame != -1) {
			m.applyFrame(primaryFrame);
		}

		if (secondaryFrame != -1) {
			m.applyFrame(secondaryFrame);
		}

		m.applyLighting(64, 768, -50, -10, -50, true);
		return m;
	}
}
