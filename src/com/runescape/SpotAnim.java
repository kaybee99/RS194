package com.runescape;

public class SpotAnim {

	public static int count;
	public static SpotAnim[] instance;

	public int index;
	public int modelIndex;
	public Seq seq;
	public boolean disposeAlpha = false;
	public int[] oldColors = new int[6];
	public int[] newColors = new int[6];
	public static List uniqueModelCache = new List(30);

	public static void load(Archive a) {
		Buffer b = new Buffer(a.get("spotanim.dat", null));
		count = b.readUShort();

		if (instance == null) {
			instance = new SpotAnim[count];
		}

		for (int n = 0; n < count; n++) {
			if (instance[n] == null) {
				instance[n] = new SpotAnim();
			}
			instance[n].index = n;
			instance[n].read(b);
		}
	}

	public void read(Buffer b) {
		for (;;) {
			int opcode = b.read();

			if (opcode == 0) {
				break;
			}

			if (opcode == 1) {
				modelIndex = b.readUShort();
			} else if (opcode == 2) {
				seq = Seq.instance[b.readUShort()];
			} else if (opcode == 3) {
				disposeAlpha = true;
			} else if (opcode >= 40 && opcode < 50) {
				oldColors[opcode - 40] = b.readUShort();
			} else if (opcode >= 50 && opcode < 60) {
				newColors[opcode - 50] = b.readUShort();
			} else {
				System.out.println("Error unrecognised spotanim config code: " + opcode);
			}
		}
	}

	public Model getModel() {
		Model m = (Model) uniqueModelCache.get((long) index);

		if (m != null) {
			return m;
		}

		m = new Model(modelIndex);

		for (int i = 0; i < 6; i++) {
			if (oldColors[0] != 0) {
				m.recolor(oldColors[i], newColors[i]);
			}
		}

		uniqueModelCache.put(m, (long) index);
		return m;
	}
}
