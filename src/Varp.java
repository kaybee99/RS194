
public class Varp {

	public static int total;
	public static Varp[] instance;
	public int type;

	public static void load(Archive a) {
		Buffer b = new Buffer(a.get("varp.dat", null));
		total = b.readUShort();

		if (instance == null) {
			instance = new Varp[total];
		}

		for (int n = 0; n < total; n++) {
			if (instance[n] == null) {
				instance[n] = new Varp();
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

			if (opcode == 5) {
				type = b.readUShort();
			} else {
				System.out.println("Error unrecognised config code: " + opcode);
			}
		}
	}
}
