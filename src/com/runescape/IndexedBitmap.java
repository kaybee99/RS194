package com.runescape;

public final class IndexedBitmap extends Graphics2D {

	public byte[] data;
	public int[] palette;
	public int width;
	public int height;
	public int clipX;
	public int clipY;
	public int clipWidth;
	public int clipHeight;

	public IndexedBitmap(Archive archive, String name, int index) {
		Buffer dat = new Buffer(archive.get(name + ".dat", null));
		Buffer idx = new Buffer(archive.get("index.dat", null));

		idx.position = dat.readUShort();

		clipWidth = idx.readUShort();
		clipHeight = idx.readUShort();

		palette = new int[idx.read()];

		for (int n = 0; n < palette.length - 1; n++) {
			palette[n + 1] = idx.readInt24();
		}

		for (int n = 0; n < index; n++) {
			idx.position += 2;
			dat.position += (idx.readUShort() * idx.readUShort());
			idx.position++;
		}

		clipX = idx.read();
		clipY = idx.read();
		width = idx.readUShort();
		height = idx.readUShort();

		int type = idx.read();
		data = new byte[width * height];

		if (type == 0) {
			for (int n = 0; n < data.length; n++) {
				data[n] = dat.readByte();
			}
		} else if (type == 1) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					data[x + y * width] = dat.readByte();
				}
			}
		}
	}

	public void shrink() {
		clipWidth /= 2;
		clipHeight /= 2;

		byte[] newPixels = new byte[clipWidth * clipHeight];
		int off = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				newPixels[((x + clipX >> 1) + (y + clipY >> 1) * clipWidth)] = data[off++];
			}
		}
		data = newPixels;
		width = clipWidth;
		height = clipHeight;
		clipX = 0;
		clipY = 0;
	}

	public void crop() {
		if (width != clipWidth || height != clipHeight) {
			byte[] newPixels = new byte[clipWidth * clipHeight];
			int off = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					newPixels[x + clipX + (y + clipY) * clipWidth] = data[off++];
				}
			}
			data = newPixels;
			width = clipWidth;
			height = clipHeight;
			clipX = 0;
			clipY = 0;
		}
	}

	public void flipHorizontally() {
		byte[] flipped = new byte[width * height];
		int off = 0;
		for (int y = 0; y < height; y++) {
			for (int x = width - 1; x >= 0; x--) {
				flipped[off++] = data[x + y * width];
			}
		}
		data = flipped;
		clipX = clipWidth - width - clipX;
	}

	public void flipVertically() {
		byte[] flipped = new byte[width * height];
		int off = 0;
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				flipped[off++] = data[x + y * width];
			}
		}
		data = flipped;
		clipY = clipHeight - height - clipY;
	}

	public void draw(int x, int y) {
		x += clipX;
		y += clipY;
		int dstOff = x + y * Graphics2D.targetWidth;
		int srcOff = 0;
		int h = height;
		int w = width;
		int dstStep = Graphics2D.targetWidth - w;
		int srcStep = 0;
		if (y < Graphics2D.top) {
			int trim = Graphics2D.top - y;
			h -= trim;
			y = Graphics2D.top;
			srcOff += trim * w;
			dstOff += trim * Graphics2D.targetWidth;
		}
		if (y + h > Graphics2D.bottom) {
			h -= y + h - Graphics2D.bottom;
		}
		if (x < Graphics2D.left) {
			int trim = Graphics2D.left - x;
			w -= trim;
			x = Graphics2D.left;
			srcOff += trim;
			dstOff += trim;
			srcStep += trim;
			dstStep += trim;
		}
		if (x + w > Graphics2D.right) {
			int trim = x + w - Graphics2D.right;
			w -= trim;
			srcStep += trim;
			dstStep += trim;
		}
		if (w > 0 && h > 0) {
			copyImage(h, data, palette, w, dstOff, srcOff, Graphics2D.target, dstStep, srcStep);
		}
	}

	private void copyImage(int h, byte[] src, int[] palette, int w, int dstOff, int srcOff, int[] dst, int dstStep, int srcstep) {
		int hw = -(w >> 2);
		w = -(w & 0x3);
		for (int y = -h; y < 0; y++) {
			for (int x = hw; x < 0; x++) {
				int p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
			}
			for (int x = w; x < 0; x++) {
				int p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
			}
			dstOff += dstStep;
			srcOff += srcstep;
		}
	}
}
