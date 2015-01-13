
public class IdentityKit {

	public static int count;
	public static IdentityKit[] instance;
	public int type = -1;
	public int[] modelIndices;
	public int[] oldColors = new int[6];
	public int[] newColors = new int[6];
	public int[] headModelIndices = {-1, -1, -1, -1, -1};

	public static void load(Archive a) {
		Buffer b = new Buffer(a.get("idk.dat", null));
		count = b.readUShort();

		if (instance == null) {
			instance = new IdentityKit[count];
		}

		for (int n = 0; n < count; n++) {
			if (instance[n] == null) {
				instance[n] = new IdentityKit();
			}
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
				type = b.read();
			} else if (opcode == 2) {
				int n = b.read();
				modelIndices = new int[n];
				for (int m = 0; m < n; m++) {
					modelIndices[m] = b.readUShort();
				}
			} else if (opcode >= 40 && opcode < 50) {
				oldColors[opcode - 40] = b.readUShort();
			} else if (opcode >= 50 && opcode < 60) {
				newColors[opcode - 50] = b.readUShort();
			} else if (opcode >= 60 && opcode < 70) {
				headModelIndices[opcode - 60] = b.readUShort();
			} else {
				System.out.println("Error unrecognised config code: " + opcode);
			}
		}
	}

	public Model getModel() {
		if (modelIndices == null) {
			return null;
		}

		Model[] models = new Model[modelIndices.length];

		for (int i = 0; i < modelIndices.length; i++) {
			models[i] = new Model(modelIndices[i]);
		}

		Model m;

		if (models.length == 1) {
			m = models[0];
		} else {
			m = new Model(models, models.length);
		}

		for (int i = 0; i < 6; i++) {
			if (oldColors[i] == 0) {
				break;
			}
			m.recolor(oldColors[i], newColors[i]);
		}
		return m;
	}

	public Model getHeadModel() {
		Model[] models = new Model[5];
		int modelCount = 0;

		for (int n = 0; n < 5; n++) {
			if (headModelIndices[n] != -1) {
				models[modelCount++] = new Model(headModelIndices[n]);
			}
		}

		Model m = new Model(models, modelCount);

		for (int n = 0; n < 6; n++) {
			if (oldColors[n] == 0) {
				break;
			}
			m.recolor(oldColors[n], newColors[n]);
		}
		return m;
	}
}
