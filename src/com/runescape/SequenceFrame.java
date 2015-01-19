package com.runescape;

public class SequenceFrame {

	public static SequenceFrame[] instance;

	public int delta;
	public SequenceTransform transform;
	public int groupCount;
	public int[] groups;
	public int[] x;
	public int[] y;
	public int[] z;

	public static void load(Archive a) {
		Buffer head = new Buffer(a.get("frame_head.dat", null));
		Buffer tran1 = new Buffer(a.get("frame_tran1.dat", null));
		Buffer tran2 = new Buffer(a.get("frame_tran2.dat", null));
		Buffer del = new Buffer(a.get("frame_del.dat", null));

		int frameCount = head.readUShort();
		int totalFrames = head.readUShort();

		instance = new SequenceFrame[totalFrames + 1];

		int[] groups = new int[500];
		int[] x = new int[500];
		int[] y = new int[500];
		int[] z = new int[500];

		for (int frame = 0; frame < frameCount; frame++) {
			SequenceFrame f = instance[head.readUShort()] = new SequenceFrame();
			f.delta = del.read();

			SequenceTransform t = SequenceTransform.instance[head.readUShort()];
			f.transform = t;

			int groupCount = head.read();
			int lastGroup = -1;
			int count = 0;

			for (int n = 0; n < groupCount; n++) {
				int flags = tran1.read();

				if (flags > 0) {
					if (t.groupTypes[n] != Model.TRANSFORM_CENTRALIZE) {
						// group?? label?? group??
						for (int group = n - 1; group > lastGroup; group--) {
							if (t.groupTypes[group] == Model.TRANSFORM_CENTRALIZE) {
								groups[count] = group;
								x[count] = 0;
								y[count] = 0;
								z[count] = 0;
								count++;
								break;
							}
						}
					}

					groups[count] = n;

					int defaultValue = 0;

					if (t.groupTypes[groups[count]] == Model.TRANSFORM_SCALE) {
						defaultValue = 128;
					}

					if ((flags & 0x1) != 0) {
						x[count] = tran2.readSmart();
					} else {
						x[count] = defaultValue;
					}

					if ((flags & 0x2) != 0) {
						y[count] = tran2.readSmart();
					} else {
						y[count] = defaultValue;
					}

					if ((flags & 0x4) != 0) {
						z[count] = tran2.readSmart();
					} else {
						z[count] = defaultValue;
					}

					lastGroup = n;
					count++;
				}
			}

			f.groupCount = count;
			f.groups = new int[count];
			f.x = new int[count];
			f.y = new int[count];
			f.z = new int[count];

			for (int j = 0; j < count; j++) {
				f.groups[j] = groups[j];
				f.x[j] = x[j];
				f.y[j] = y[j];
				f.z[j] = z[j];
			}
		}
	}
}
