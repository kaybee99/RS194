package com.runescape;

public final class StringBuffer {

	public static char[] buffer = new char[100];
	private static final char[] CHAR_TABLE = {' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r', 'd', 'l', 'u', 'm', 'w', 'c', 'y', 'f', 'g', 'p', 'b', 'v', 'k', 'x', 'j', 'q', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '!', '?', '.', ',', ':', ';', '(', ')', '-', '&', '*', '\\', '\'', '@', '#', '+', '=', '\u00a3', '$', '%', '\"', '[', ']'};

	public static String read(Buffer b, int len) {
		int pos = 0;
		int msb = -1;

		for (int n = 0; n < len; n++) {
			int c = b.read();
			int lsb = c >> 4 & 0xF;

			if (msb == -1) {
				if (lsb < 13) {
					buffer[pos++] = CHAR_TABLE[lsb];
				} else {
					msb = lsb;
				}
			} else {
				buffer[pos++] = CHAR_TABLE[(msb << 4) + lsb - 195];
				msb = -1;
			}

			lsb = c & 0xf;

			if (msb == -1) {
				if (lsb < 13) {
					buffer[pos++] = CHAR_TABLE[lsb];
				} else {
					msb = lsb;
				}
			} else {
				buffer[pos++] = CHAR_TABLE[(msb << 4) + lsb - 195];
				msb = -1;
			}
		}

		boolean capitalize = true;

		for (int n = 0; n < pos; n++) {
			char c = buffer[n];

			if (capitalize && c >= 'a' && c <= 'z') {
				buffer[n] += -32;
				capitalize = false;
			}

			if (c == '.' || c == '!') {
				capitalize = true;
			}
		}
		return new String(buffer, 0, pos);
	}

	public static void write(Buffer b, String s) {
		if (s.length() > 80) {
			s = s.substring(0, 80);
		}

		s = s.toLowerCase();

		int msb = -1;
		for (int n = 0; n < s.length(); n++) {
			char c = s.charAt(n);
			int lsb = 0;

			for (int m = 0; m < CHAR_TABLE.length; m++) {
				if (c == CHAR_TABLE[m]) {
					lsb = m;
					break;
				}
			}

			if (lsb > 12) {
				lsb += 195;
			}

			if (msb == -1) {
				if (lsb < 13) {
					msb = lsb;
				} else {
					b.write(lsb);
				}
			} else if (lsb < 13) {
				b.write((msb << 4) + lsb);
				msb = -1;
			} else {
				b.write((msb << 4) + (lsb >> 4));
				msb = lsb & 0xf;
			}
		}

		if (msb != -1) {
			b.write(msb << 4);
		}
	}
}
