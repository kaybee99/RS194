package com.runescape;

public final class BitmapFont extends Graphics2D {

	public static int[] CHAR_LOOKUP = new int[256];

	public byte[][] data = new byte[94][];
	public int[] charWidth = new int[94];
	public int[] charHeight = new int[94];
	public int[] charOffsetX = new int[94];
	public int[] charOffsetY = new int[94];
	public int[] charSpace = new int[95];
	public int[] drawWidth = new int[256];
	public int height;

	public BitmapFont(String name, Archive archive) {
		Buffer dat = new Buffer(archive.get(name + ".dat", null));
		Buffer idx = new Buffer(archive.get("index.dat", null));
		idx.position = dat.readUShort() + 4;

		int off = idx.read();

		if (off > 0) {
			idx.position += (off - 1) * 3;
		}

		for (int n = 0; n < 94; n++) {
			charOffsetX[n] = idx.read();
			charOffsetY[n] = idx.read();

			int w = charWidth[n] = idx.readUShort();
			int h = charHeight[n] = idx.readUShort();

			int type = idx.read();
			int len = w * h;
			data[n] = new byte[len];

			if (type == 0) {
				for (int i = 0; i < len; i++) {
					data[n][i] = dat.readByte();
				}
			} else if (type == 1) {
				for (int x = 0; x < w; x++) {
					for (int y = 0; y < h; y++) {
						data[n][x + y * w] = dat.readByte();
					}
				}
			}

			if (h > height) {
				height = h;
			}

			charOffsetX[n] = 1;
			charSpace[n] = w + 2;

			int i = 0;

			for (int y = h / 7; y < h; y++) {
				i += data[n][y * w];
			}

			if (i <= h / 7) {
				charSpace[n]--;
				charOffsetX[n] = 0;
			}

			i = 0;

			for (int y = h / 7; y < h; y++) {
				i += data[n][w - 1 + y * w];
			}

			if (i <= h / 7) {
				charSpace[n]--;
			}
		}

		charSpace[94] = charSpace[8];

		for (int c = 0; c < 256; c++) {
			drawWidth[c] = charSpace[CHAR_LOOKUP[c]];
		}
	}

	public void drawCentered(String s, int x, int y, int rgb) {
		draw(s, x - stringWidth(s) / 2, y, rgb);
	}

	public void drawTaggableCentered(String s, int x, int y, int rgb, boolean shadow) {
		drawTaggable(s, x - stringWidth(s) / 2, y, rgb, shadow);
	}

	public int stringWidth(String s) {
		if (s == null) {
			return 0;
		}
		int w = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '@' && i + 4 < s.length() && s.charAt(i + 4) == '@') {
				i += 4;
			} else {
				w += drawWidth[s.charAt(i)];
			}
		}
		return w;
	}

	public void draw(String string, int x, int y, int rgb) {
		y -= height;
		for (int i = 0; i < string.length(); i++) {
			int c = CHAR_LOOKUP[string.charAt(i)];
			if (c != 94) {
				drawChar(data[c], x + charOffsetX[c], y + charOffsetY[c], charWidth[c], charHeight[c], rgb);
			}
			x += charSpace[c];
		}
	}

	public void drawWavy(String s, int x, int y, int rgb, int amplitude) {
		x -= stringWidth(s) / 2;
		y -= height;
		for (int i = 0; i < s.length(); i++) {
			int c = CHAR_LOOKUP[s.charAt(i)];
			if (c != 94) {
				drawChar(data[c], x + charOffsetX[c], (y + charOffsetY[c] + (int) (Math.sin((double) i / 2.0 + (double) amplitude / 5.0) * 5.0)), charWidth[c], charHeight[c], rgb);
			}
			x += charSpace[c];
		}
	}

	public void drawTaggable(String s, int x, int y, int rgb, boolean shadow) {
		if (s == null) {
			return;
		}

		y -= height;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '@' && i + 4 < s.length() && s.charAt(i + 4) == '@') {
				String tag = s.substring(i + 1, i + 4);
				if (tag.equals("red")) {
					rgb = 16711680;
				}
				if (tag.equals("gre")) {
					rgb = 65280;
				}
				if (tag.equals("blu")) {
					rgb = 255;
				}
				if (tag.equals("yel")) {
					rgb = 16776960;
				}
				if (tag.equals("cya")) {
					rgb = 65535;
				}
				if (tag.equals("mag")) {
					rgb = 16711935;
				}
				if (tag.equals("whi")) {
					rgb = 16777215;
				}
				if (tag.equals("bla")) {
					rgb = 0;
				}
				if (tag.equals("lre")) {
					rgb = 16748608;
				}
				if (tag.equals("dre")) {
					rgb = 8388608;
				}
				if (tag.equals("dbl")) {
					rgb = 128;
				}
				if (tag.equals("or1")) {
					rgb = 16756736;
				}
				if (tag.equals("or2")) {
					rgb = 16740352;
				}
				if (tag.equals("or3")) {
					rgb = 16723968;
				}
				if (tag.equals("gr1")) {
					rgb = 12648192;
				}
				if (tag.equals("gr2")) {
					rgb = 8453888;
				}
				if (tag.equals("gr3")) {
					rgb = 4259584;
				}
				i += 4;
			} else {
				int c = CHAR_LOOKUP[s.charAt(i)];
				if (c != 94) {
					if (shadow) {
						drawChar(data[c], x + charOffsetX[c] + 1, y + charOffsetY[c] + 1, charWidth[c], charHeight[c], 1);
					}
					drawChar(data[c], x + charOffsetX[c], y + charOffsetY[c], charWidth[c], charHeight[c], rgb);
				}
				x += charSpace[c];
			}
		}
	}

	private void drawChar(byte[] data, int x, int y, int w, int h, int rgb) {
		int dstOff = x + y * Graphics2D.targetWidth;
		int dstStep = Graphics2D.targetWidth - w;
		int srcStep = 0;
		int srcOff = 0;

		if (y < Graphics2D.top) {
			int cutoff = Graphics2D.top - y;
			h -= cutoff;
			y = Graphics2D.top;
			srcOff += cutoff * w;
			dstOff += cutoff * Graphics2D.targetWidth;
		}

		if (y + h >= Graphics2D.bottom) {
			h -= y + h - Graphics2D.bottom + 1;
		}

		if (x < Graphics2D.left) {
			int cutoff = Graphics2D.left - x;
			w -= cutoff;
			x = Graphics2D.left;
			srcOff += cutoff;
			dstOff += cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (x + w >= Graphics2D.right) {
			int cutoff = x + w - Graphics2D.right + 1;
			w -= cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (w > 0 && h > 0) {
			drawMask(Graphics2D.target, data, rgb, srcOff, dstOff, w, h, dstStep, srcStep);
		}
	}

	private void drawMask(int[] dst, byte[] src, int rgb, int srcOff, int dstOff, int w, int h, int dstStep, int srcStep) {
		int hw = -(w >> 2);
		w = -(w & 0x3);

		for (int y = -h; y < 0; y++) {
			for (int x = hw; x < 0; x++) {
				if (src[srcOff++] != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				if (src[srcOff++] != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				if (src[srcOff++] != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				if (src[srcOff++] != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}
			}

			for (int x = w; x < 0; x++) {
				if (src[srcOff++] != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}
			}

			dstOff += dstStep;
			srcOff += srcStep;
		}
	}

	static {
		String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";

		for (int i = 0; i < 256; i++) {
			int c = s.indexOf(i);
			if (c == -1) {
				c = 74;
			}
			CHAR_LOOKUP[i] = c;
		}
	}
}
