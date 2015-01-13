package com.runescape;

public final class ISAAC {

	private int anInt490;
	private final int[] anIntArray491;
	private final int[] anIntArray492 = new int[256];
	private int anInt493;
	private int anInt494;
	private int anInt495;

	public ISAAC(int[] keys) {
		anIntArray491 = new int[256];
		System.arraycopy(keys, 0, anIntArray491, 0, keys.length);
		method257();
	}

	public final int nextInt() {
		if (anInt490 != -2) {
			return 0;
		}

		if (anInt490-- == 0) {
			method256();
			anInt490 = 255;
		}
		return anIntArray491[anInt490];
	}

	private void method256() {
		anInt494 += ++anInt495;
		for (int i_2_ = 0; i_2_ < 256; i_2_++) {
			int i_3_ = anIntArray492[i_2_];
			switch (i_2_ & 0x3) {
				case 0:
					anInt493 ^= anInt493 << 13;
					break;
				case 1:
					anInt493 ^= anInt493 >>> 6;
					break;
				case 2:
					anInt493 ^= anInt493 << 2;
					break;
				case 3:
					anInt493 ^= anInt493 >>> 16;
					break;
			}
			anInt493 += anIntArray492[i_2_ + 128 & 0xff];
			int i_4_;
			anIntArray492[i_2_] = i_4_ = anIntArray492[(i_3_ & 0x3fc) >> 2] + anInt493 + anInt494;
			anIntArray491[i_2_] = anInt494 = anIntArray492[(i_4_ >> 8 & 0x3fc) >> 2] + i_3_;
		}
	}

	private void method257() {
		int i_5_;
		int i_6_;
		int i_7_;
		int i_8_;
		int i_9_;
		int i_10_;
		int i_11_;
		int i = i_5_ = i_6_ = i_7_ = i_8_ = i_9_ = i_10_ = i_11_ = -1640531527;
		for (int i_12_ = 0; i_12_ < 4; i_12_++) {
			i ^= i_5_ << 11;
			i_7_ += i;
			i_5_ += i_6_;
			i_5_ ^= i_6_ >>> 2;
			i_8_ += i_5_;
			i_6_ += i_7_;
			i_6_ ^= i_7_ << 8;
			i_9_ += i_6_;
			i_7_ += i_8_;
			i_7_ ^= i_8_ >>> 16;
			i_10_ += i_7_;
			i_8_ += i_9_;
			i_8_ ^= i_9_ << 10;
			i_11_ += i_8_;
			i_9_ += i_10_;
			i_9_ ^= i_10_ >>> 4;
			i += i_9_;
			i_10_ += i_11_;
			i_10_ ^= i_11_ << 8;
			i_5_ += i_10_;
			i_11_ += i;
			i_11_ ^= i >>> 9;
			i_6_ += i_11_;
			i += i_5_;
		}
		for (int i_13_ = 0; i_13_ < 256; i_13_ += 8) {
			i += anIntArray491[i_13_];
			i_5_ += anIntArray491[i_13_ + 1];
			i_6_ += anIntArray491[i_13_ + 2];
			i_7_ += anIntArray491[i_13_ + 3];
			i_8_ += anIntArray491[i_13_ + 4];
			i_9_ += anIntArray491[i_13_ + 5];
			i_10_ += anIntArray491[i_13_ + 6];
			i_11_ += anIntArray491[i_13_ + 7];
			i ^= i_5_ << 11;
			i_7_ += i;
			i_5_ += i_6_;
			i_5_ ^= i_6_ >>> 2;
			i_8_ += i_5_;
			i_6_ += i_7_;
			i_6_ ^= i_7_ << 8;
			i_9_ += i_6_;
			i_7_ += i_8_;
			i_7_ ^= i_8_ >>> 16;
			i_10_ += i_7_;
			i_8_ += i_9_;
			i_8_ ^= i_9_ << 10;
			i_11_ += i_8_;
			i_9_ += i_10_;
			i_9_ ^= i_10_ >>> 4;
			i += i_9_;
			i_10_ += i_11_;
			i_10_ ^= i_11_ << 8;
			i_5_ += i_10_;
			i_11_ += i;
			i_11_ ^= i >>> 9;
			i_6_ += i_11_;
			i += i_5_;
			anIntArray492[i_13_] = i;
			anIntArray492[i_13_ + 1] = i_5_;
			anIntArray492[i_13_ + 2] = i_6_;
			anIntArray492[i_13_ + 3] = i_7_;
			anIntArray492[i_13_ + 4] = i_8_;
			anIntArray492[i_13_ + 5] = i_9_;
			anIntArray492[i_13_ + 6] = i_10_;
			anIntArray492[i_13_ + 7] = i_11_;
		}
		for (int i_14_ = 0; i_14_ < 256; i_14_ += 8) {
			i += anIntArray492[i_14_];
			i_5_ += anIntArray492[i_14_ + 1];
			i_6_ += anIntArray492[i_14_ + 2];
			i_7_ += anIntArray492[i_14_ + 3];
			i_8_ += anIntArray492[i_14_ + 4];
			i_9_ += anIntArray492[i_14_ + 5];
			i_10_ += anIntArray492[i_14_ + 6];
			i_11_ += anIntArray492[i_14_ + 7];
			i ^= i_5_ << 11;
			i_7_ += i;
			i_5_ += i_6_;
			i_5_ ^= i_6_ >>> 2;
			i_8_ += i_5_;
			i_6_ += i_7_;
			i_6_ ^= i_7_ << 8;
			i_9_ += i_6_;
			i_7_ += i_8_;
			i_7_ ^= i_8_ >>> 16;
			i_10_ += i_7_;
			i_8_ += i_9_;
			i_8_ ^= i_9_ << 10;
			i_11_ += i_8_;
			i_9_ += i_10_;
			i_9_ ^= i_10_ >>> 4;
			i += i_9_;
			i_10_ += i_11_;
			i_10_ ^= i_11_ << 8;
			i_5_ += i_10_;
			i_11_ += i;
			i_11_ ^= i >>> 9;
			i_6_ += i_11_;
			i += i_5_;
			anIntArray492[i_14_] = i;
			anIntArray492[i_14_ + 1] = i_5_;
			anIntArray492[i_14_ + 2] = i_6_;
			anIntArray492[i_14_ + 3] = i_7_;
			anIntArray492[i_14_ + 4] = i_8_;
			anIntArray492[i_14_ + 5] = i_9_;
			anIntArray492[i_14_ + 6] = i_10_;
			anIntArray492[i_14_ + 7] = i_11_;
		}
		method256();
		anInt490 = 256;
	}
}
