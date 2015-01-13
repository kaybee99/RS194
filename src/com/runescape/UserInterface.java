package com.runescape;

public class UserInterface {

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

	public static UserInterface[] instances = new UserInterface[0];

	private static List bitmapCache;
	private static List modelCache;

	private static final Bitmap getBitmap(String name, Archive media, int index) {
		long l = (StringUtil.getHash(name) << 4) + (long) index;
		Bitmap b = (Bitmap) bitmapCache.get(l);
	
		if (b != null) {
			return b;
		}
	
		b = new Bitmap(media, name, index);
		bitmapCache.put(b, l);
		return b;
	}

	private static final Model getModel(int index) {
		Model m = (Model) modelCache.get((long) index);
	
		if (m != null) {
			return m;
		}
	
		m = new Model(index);
		modelCache.put(m, (long) index);
		return m;
	}

	public static UserInterface create() {
		// extend
		UserInterface[] old = instances;
		instances = new UserInterface[old.length + 1];
		System.arraycopy(old, 0, instances, 0, old.length);

		// create
		UserInterface i = new UserInterface();
		i.index = old.length;
		instances[i.index] = i;
		return i;
	}

	public static UserInterface createParent() {
		UserInterface i = create();
		i.type = TYPE_PARENT;
		i.children = new int[0];
		i.childX = new int[0];
		i.childY = new int[0];
		return i;
	}

	public static void load(BitmapFont[] fonts, Archive media, Archive interfaces) {
		bitmapCache = new List(50000);
		modelCache = new List(50000);

		Buffer b = new Buffer(interfaces.get("data", null));
		instances = new UserInterface[b.readUShort()];

		int parent = -1;
		while (b.pos < b.data.length) {
			int index = b.readUShort();

			if (index == 65535) {
				parent = b.readUShort();
				index = b.readUShort();
			}

			UserInterface i = instances[index] = new UserInterface();
			i.index = index;
			i.parent = parent;
			i.type = b.read();
			i.optionType = b.read();
			i.action = b.readUShort();
			i.width = b.readUShort();
			i.height = b.readUShort();
			i.hoverParentIndex = b.read();

			if (i.hoverParentIndex != 0) {
				i.hoverParentIndex = ((i.hoverParentIndex - 1 << 8) + b.read());
			} else {
				i.hoverParentIndex = -1;
			}

			int comparatorCount = b.read();

			if (comparatorCount > 0) {
				i.cscriptComparator = new int[comparatorCount];
				i.cscriptCompareValue = new int[comparatorCount];

				for (int n = 0; n < comparatorCount; n++) {
					i.cscriptComparator[n] = b.read();
					i.cscriptCompareValue[n] = b.readUShort();
				}
			}

			int scriptCount = b.read();

			if (scriptCount > 0) {
				i.cscript = new int[scriptCount][];

				for (int script = 0; script < scriptCount; script++) {
					int opcodes = b.readUShort();
					i.cscript[script] = new int[opcodes];

					for (int opcode = 0; opcode < opcodes; opcode++) {
						i.cscript[script][opcode] = b.readUShort();
					}
				}
			}

			if (i.type == TYPE_PARENT) {
				i.scrollHeight = b.readUShort();
				i.hidden = b.read() == 1;

				int n = b.read();
				i.children = new int[n];
				i.childX = new int[n];
				i.childY = new int[n];

				for (int m = 0; m < n; m++) {
					i.children[m] = b.readUShort();
					i.childX[m] = b.readShort();
					i.childY[m] = b.readShort();
				}
			}

			if (i.type == TYPE_UNUSED) {
				i.unusedInt = b.readUShort();
				i.unusedBool = b.read() == 1;
			}

			if (i.type == TYPE_INVENTORY) {
				i.inventoryIndices = new int[i.width * i.height];
				i.inventoryAmount = new int[i.width * i.height];

				i.inventoryDummy = b.read() == 1;
				i.inventoryHasActions = b.read() == 1;
				i.inventoryIsUsable = b.read() == 1;
				i.inventoryMarginX = b.read();
				i.inventoryMarginY = b.read();
				i.inventoryOffsetX = new int[20];
				i.inventoryOffsetY = new int[20];
				i.inventoryBitmap = new Bitmap[20];

				for (int n = 0; n < 20; n++) {
					if (b.read() == 1) {
						i.inventoryOffsetX[n] = b.readShort();
						i.inventoryOffsetY[n] = b.readShort();

						String s = b.readString();

						if (media != null && s.length() > 0) {
							int j = s.lastIndexOf(",");
							i.inventoryBitmap[n] = getBitmap(s.substring(0, j), media, (Integer.parseInt(s.substring(j + 1))));
						}
					}
				}

				i.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					i.inventoryActions[n] = b.readString();

					if (i.inventoryActions[n].length() == 0) {
						i.inventoryActions[n] = null;
					}
				}
			}

			if (i.type == TYPE_RECT) {
				i.fill = b.read() == 1;
			}

			if (i.type == TYPE_TEXT || i.type == TYPE_UNUSED) {
				i.centered = b.read() == 1;
				i.font = fonts[b.read()];
				i.shadow = b.read() == 1;
			}

			if (i.type == TYPE_TEXT) {
				i.messageDisabled = b.readString();
				i.messageEnabled = b.readString();
			}

			if (i.type == TYPE_UNUSED || i.type == TYPE_RECT || i.type == TYPE_TEXT) {
				i.colorDisabled = b.readInt();
			}

			if (i.type == TYPE_RECT || i.type == TYPE_TEXT) {
				i.colorEnabled = b.readInt();
				i.hoverColor = b.readInt();
			}

			if (i.type == TYPE_IMAGE) {
				String s = b.readString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					i.bitmapDisabled = getBitmap(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}

				s = b.readString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					i.bitmapEnabled = getBitmap(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}
			}

			if (i.type == TYPE_MODEL) {
				index = b.read();

				if (index != 0) {
					i.modelDisabled = getModel(((index - 1 << 8) + b.read()));
				}

				index = b.read();

				if (index != 0) {
					i.modelEnabled = getModel(((index - 1 << 8) + b.read()));
				}

				index = b.read();

				if (index != 0) {
					i.seqIndexDisabled = (index - 1 << 8) + b.read();
				} else {
					i.seqIndexDisabled = -1;
				}

				index = b.read();

				if (index != 0) {
					i.seqIndexEnabled = (index - 1 << 8) + b.read();
				} else {
					i.seqIndexEnabled = -1;
				}

				i.modelZoom = b.readUShort();
				i.modelCameraPitch = b.readUShort();
				i.modelYaw = b.readUShort();
			}

			if (i.type == TYPE_INVENTORY_TEXT) {
				i.inventoryIndices = new int[i.width * i.height];
				i.inventoryAmount = new int[i.width * i.height];
				i.centered = b.read() == 1;

				int font = b.read();

				if (fonts != null) {
					i.font = fonts[font];
				}

				i.shadow = b.read() == 1;
				i.colorDisabled = b.readInt();
				i.inventoryMarginX = b.readShort();
				i.inventoryMarginY = b.readShort();
				i.inventoryHasActions = b.read() == 1;
				i.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					i.inventoryActions[n] = b.readString();

					if (i.inventoryActions[n].length() == 0) {
						i.inventoryActions[n] = null;
					}
				}
			}

			if (i.optionType == 2 || i.type == TYPE_INVENTORY) {
				i.optionPrefix = b.readString();
				i.optionSuffix = b.readString();
				i.optionFlags = b.readUShort();
			}

			if (i.optionType == 1 || i.optionType == 4 || i.optionType == 5 || i.optionType == 6) {
				i.option = b.readString();

				if (i.option.length() == 0) {
					if (i.optionType == OPTIONTYPE_OK) {
						i.option = "Ok";
					}

					if (i.optionType == OPTIONTYPE_SELECT || i.optionType == OPTIONTYPE_SELECT2) {
						i.option = "Select";
					}

					if (i.optionType == OPTIONTYPE_CONTINUE) {
						i.option = "Continue";
					}
				}
			}
		}
		bitmapCache = null;
	}

	public void setOptionType(int type) {
		if (type == OPTIONTYPE_OK) {
			option = "Ok";
		} else if (type == OPTIONTYPE_SELECT) {
			option = "Select";
		} else if (type == OPTIONTYPE_CONTINUE) {
			option = "Continue";
		}
		optionType = type;
	}

	public int[] inventoryIndices;
	public int[] inventoryAmount;
	public int seqFrame;
	public int seqCycle;
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
	public int seqIndexDisabled;
	public int seqIndexEnabled;
	public int modelZoom;
	public int modelCameraPitch;
	public int modelYaw;
	public String optionPrefix;
	public String optionSuffix;
	public int optionFlags;
	public String option;

	public UserInterface addRect(int x, int y, int w, int h, int color, boolean fill) {
		UserInterface rect = create();
		rect.type = TYPE_RECT;
		rect.width = w;
		rect.height = h;
		rect.fill = fill;
		rect.colorDisabled = color;
		addChild(rect, x, y);
		return rect;
	}

	public UserInterface addText(BitmapFont font, String s, int x, int y, int color, boolean shadow) {
		UserInterface text = create();
		text.type = TYPE_TEXT;
		text.width = font.stringWidth(s);
		text.height = font.height;
		text.font = font;
		text.colorDisabled = color;
		text.messageDisabled = s;
		text.shadow = shadow;
		addChild(text, x, y);
		return text;
	}

	public UserInterface addHoverText(BitmapFont font, String s, int x, int y, int color, boolean shadow) {
		UserInterface text = addText(font, s, x, y, color, shadow);
		text.hoverColor = 0xFFFFFF;
		return text;
	}

	public int addChild(UserInterface child, int x, int y) {
		int index = children.length;

		int[] old = children;
		children = new int[index + 1];
		System.arraycopy(old, 0, children, 0, old.length);

		old = childX;
		childX = new int[index + 1];
		System.arraycopy(old, 0, childX, 0, old.length);

		old = childY;
		childY = new int[index + 1];
		System.arraycopy(old, 0, childY, 0, old.length);

		children[index] = child.index;
		childX[index] = x;
		childY[index] = y;

		return index;
	}

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
