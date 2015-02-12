package com.runescape;

public class AnimationTransform {

	public static AnimationTransform[] instance;
	public int length;
	public int[] groupTypes;
	public int[][] groups;

	public static void load(Archive a) {
		Buffer bhead = new Buffer(a.get("base_head.dat", null));
		Buffer btype = new Buffer(a.get("base_type.dat", null));
		Buffer blabel = new Buffer(a.get("base_label.dat", null));

		int total = bhead.readUShort();

		instance = new AnimationTransform[bhead.readUShort() + 1];

		for (int i = 0; i < total; i++) {
			int index = bhead.readUShort();

			int length = bhead.read();
			int[] groupTypes = new int[length];
			int[][] groups = new int[length][];

			for (int n = 0; n < length; n++) {
				groupTypes[n] = btype.read();

				int groupCount = blabel.read();
				groups[n] = new int[groupCount];

				for (int g = 0; g < groupCount; g++) {
					groups[n][g] = blabel.read();
				}
			}

			instance[index] = new AnimationTransform();
			instance[index].length = length;
			instance[index].groupTypes = groupTypes;
			instance[index].groups = groups;
		}
	}
}
