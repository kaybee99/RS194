
public class SeqTransform {

	public static SeqTransform[] instance;
	public int length;
	public int[] types;
	public int[][] labels;

	public static void load(Archive a) {
		Buffer bhead = new Buffer(a.get("base_head.dat", null));
		Buffer btype = new Buffer(a.get("base_type.dat", null));
		Buffer blabel = new Buffer(a.get("base_label.dat", null));

		int total = bhead.readUShort();

		instance = new SeqTransform[bhead.readUShort() + 1];

		for (int i = 0; i < total; i++) {
			int index = bhead.readUShort();

			int length = bhead.read();
			int[] types = new int[length];
			int[][] skins = new int[length][];

			for (int s = 0; s < length; s++) {
				types[s] = btype.read();

				int skinN = blabel.read();
				skins[s] = new int[skinN];

				for (int l = 0; l < skinN; l++) {
					skins[s][l] = blabel.read();
				}
			}

			instance[index] = new SeqTransform();
			instance[index].length = length;
			instance[index].types = types;
			instance[index].labels = skins;
		}
	}
}
