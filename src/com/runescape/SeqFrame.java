package com.runescape;

public class SeqFrame {

	public static SeqFrame[] instance;

	public int delta;
	public SeqTransform transform;
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

		instance = new SeqFrame[totalFrames + 1];

		int[] groups = new int[500];
		int[] x = new int[500];
		int[] y = new int[500];
		int[] z = new int[500];

		for (int frame = 0; frame < frameCount; frame++) {
			SeqFrame f = instance[head.readUShort()] = new SeqFrame();
			f.delta = del.read();

			SeqTransform t = SeqTransform.instance[head.readUShort()];
			f.transform = t;

			int groupCount = head.read();
			int lastGroup = -1;
			int count = 0;

			for (int n = 0; n < groupCount; n++) {
				int flags = tran1.read();

				if (flags > 0) {
					if (t.types[n] != 0) {
						// group?? label?? group??
						for (int group = n - 1; group > lastGroup; group--) {
							if (t.types[group] == 0) {
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
					int last = 0;

					if (t.types[groups[count]] == 3) {
						last = 128;
					}

					if ((flags & 0x1) != 0) {
						x[count] = tran2.readSmart();
					} else {
						x[count] = last;
					}

					if ((flags & 0x2) != 0) {
						y[count] = tran2.readSmart();
					} else {
						y[count] = last;
					}

					if ((flags & 0x4) != 0) {
						z[count] = tran2.readSmart();
					} else {
						z[count] = last;
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
