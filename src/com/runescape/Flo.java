package com.runescape;

public class Flo {

	public static int count;
	public static Flo[] instances;

	public int rgb;
	public int textureIndex = -1;
	public boolean aBoolean47 = false;
	public boolean occlude = true;
	public String name;
	public int hue;
	public int saturation;
	public int lightness;
	public int hue2;
	public int hueDivisor;
	public int index;

	public static final int getCount() {
		return count;
	}

	public static final Flo get(int index) {
		if (index < 0 || index >= instances.length) {
			return null;
		}
		return instances[index];
	}

	public static void unpack(Archive a) {
		Buffer b = new Buffer(a.get("flo.dat", null));
		count = b.readUShort();

		if (instances == null) {
			instances = new Flo[count];
		}

		for (int n = 0; n < count; n++) {
			if (instances[n] == null) {
				instances[n] = new Flo();
			}
			instances[n].index = n;
			instances[n].read(b);
		}
	}

	// someone needs to enlighten me
	private void setColor(int rgb) {
		double r = (double) (rgb >> 16 & 0xff) / 256.0;
		double g = (double) (rgb >> 8 & 0xff) / 256.0;
		double b = (double) (rgb & 0xff) / 256.0;

		double r1 = r;

		if (g < r1) {
			r1 = g;
		}

		if (b < r1) {
			r1 = b;
		}

		double r2 = r;

		if (g > r2) {
			r2 = g;
		}

		if (b > r2) {
			r2 = b;
		}

		double hue1 = 0.0;
		double sat1 = 0.0;
		double light1 = (r1 + r2) / 2.0;

		if (r1 != r2) {
			if (light1 < 0.5) {
				sat1 = (r2 - r1) / (r2 + r1);
			}
			if (light1 >= 0.5) {
				sat1 = (r2 - r1) / (2.0 - r2 - r1);
			}

			if (r == r2) {
				hue1 = (g - b) / (r2 - r1);
			} else if (g == r2) {
				hue1 = 2.0 + (b - r) / (r2 - r1);
			} else if (b == r2) {
				hue1 = 4.0 + (r - g) / (r2 - r1);
			}
		}

		hue1 /= 6.0;

		this.hue = (int) (hue1 * 256.0);
		this.saturation = (int) (sat1 * 256.0);
		this.lightness = (int) (light1 * 256.0);

		if (this.saturation < 0) {
			this.saturation = 0;
		} else if (this.saturation > 255) {
			this.saturation = 255;
		}

		if (this.lightness < 0) {
			this.lightness = 0;
		} else if (this.lightness > 255) {
			this.lightness = 255;
		}

		if (light1 > 0.5) {
			this.hueDivisor = (int) ((1.0 - light1) * sat1 * 512.0);
		} else {
			this.hueDivisor = (int) (light1 * sat1 * 512.0);
		}

		if (this.hueDivisor < 1) {
			this.hueDivisor = 1;
		}

		this.hue2 = (int) (hue1 * (double) this.hueDivisor);
	}

	public void read(Buffer b) {
		for (;;) {
			int opcode = b.read();
			if (opcode == 0) {
				break;
			}
			if (opcode == 1) {
				rgb = b.readInt24();
				setColor(rgb);
			} else if (opcode == 2) {
				textureIndex = b.read();
			} else if (opcode == 3) {
				aBoolean47 = true;
			} else if (opcode == 5) {
				occlude = false;
			} else if (opcode == 6) {
				name = b.readString();
			} else {
				System.out.println("Error unrecognised config code: " + opcode);
			}
		}
	}

}
