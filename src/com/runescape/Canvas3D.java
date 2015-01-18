package com.runescape;

public class Canvas3D extends Canvas2D {

	public static boolean lowmemory = true;
	public static boolean verifyBounds;
	public static boolean opaque;
	public static boolean texturedShading = true;
	public static int alpha;
	public static int centerX;
	public static int centerY;
	public static int[] lightnessLerpArray = new int[512];
	public static int[] zLerpArray = new int[2048];
	public static int[] sin = new int[2048];
	public static int[] cos = new int[2048];
	public static int[] offsets;
	public static int textureN;
	public static IndexedBitmap[] textures;
	public static boolean[] textureHasTransparency;
	public static int[] averageTextureRGB;
	public static int pixelpoolN;
	public static int[][] pixelPool;
	public static int[][] texelPool;
	public static int[] textureCycles;
	public static int cycle;
	public static int[] palette;
	public static int[][] originalTexels;

	public static final void unload() {
		lightnessLerpArray = null;
		zLerpArray = null;
		sin = null;
		cos = null;
		offsets = null;
		textures = null;
		textureHasTransparency = null;
		averageTextureRGB = null;
		pixelPool = null;
		texelPool = null;
		textureCycles = null;
		palette = null;
		originalTexels = null;
	}

	public static final void prepareOffsets() {
		offsets = new int[Canvas2D.dstH];
		for (int y = 0; y < Canvas2D.dstH; y++) {
			offsets[y] = Canvas2D.dstW * y;
		}
		centerX = Canvas2D.dstW / 2;
		centerY = Canvas2D.dstH / 2;
	}

	public static final void prepareOffsets(int w, int h) {
		offsets = new int[h];
		for (int y = 0; y < h; y++) {
			offsets[y] = w * y;
		}
		centerX = w / 2;
		centerY = h / 2;
	}

	public static final void clearPools() {
		pixelPool = null;

		for (int i = 0; i < 50; i++) {
			texelPool[i] = null;
		}
	}

	public static final void setupPools(int size) {
		if (pixelPool == null) {
			pixelpoolN = size;

			if (lowmemory) {
				pixelPool = new int[pixelpoolN][128 * 128];
			} else {
				pixelPool = new int[pixelpoolN][256 * 256];
			}

			for (int n = 0; n < 50; n++) {
				texelPool[n] = null;
			}
		}
	}

	public static final void unpackTextures(Archive a) {
		textureN = 0;

		for (int n = 0; n < 50; n++) {
			try {
				textures[n] = new IndexedBitmap(a, String.valueOf(n), 0);

				if (lowmemory && textures[n].clipWidth == 128) {
					textures[n].shrink();
				} else {
					textures[n].crop();
				}

				textureN++;
			} catch (Exception e) {
				/* empty */
			}
		}
	}

	public static final int getAverageTextureRGB(int texture) {
		if (averageTextureRGB[texture] != 0) {
			return averageTextureRGB[texture];
		}

		int r = 0;
		int g = 0;
		int b = 0;
		int len = originalTexels[texture].length;

		for (int n = 0; n < len; n++) {
			r += originalTexels[texture][n] >> 16 & 0xff;
			g += originalTexels[texture][n] >> 8 & 0xff;
			b += originalTexels[texture][n] & 0xff;
		}

		int rgb = (r / len << 16) + (g / len << 8) + b / len;
		rgb = adjustRGBIntensity(rgb, 1.4);

		if (rgb == 0) {
			rgb = 1;
		}

		averageTextureRGB[texture] = rgb;
		return rgb;
	}

	public static final void updateTexture(int texture) {
		if (texelPool[texture] != null) {
			pixelPool[pixelpoolN++] = texelPool[texture];
			texelPool[texture] = null;
		}
	}

	public static final int[] getTexels(int texture) {
		textureCycles[texture] = cycle++;

		if (texelPool[texture] != null) {
			return texelPool[texture];
		}

		int[] pool;

		if (pixelpoolN > 0) {
			pool = pixelPool[--pixelpoolN];
			pixelPool[pixelpoolN] = null;
		} else {
			int tcycle = 0;
			int t = -1;
			for (int n = 0; n < textureN; n++) {
				if (texelPool[n] != null && (textureCycles[n] < tcycle || t == -1)) {
					tcycle = textureCycles[n];
					t = n;
				}
			}
			pool = texelPool[t];
			texelPool[t] = null;
		}

		texelPool[texture] = pool;

		IndexedBitmap t = textures[texture];
		int[] src = originalTexels[texture];

		if (lowmemory) {
			textureHasTransparency[texture] = false;

			for (int n = 0; n < (64 * 64); n++) {
				int rgb = (pool[n] = (src[t.data[n]] & 0xF8F8FF));

				if (rgb == 0) {
					textureHasTransparency[texture] = true;
				}

				pool[n + 4096] = rgb - (rgb >>> 3) & 0xF8F8FF;
				pool[n + 8192] = rgb - (rgb >>> 2) & 0xF8F8FF;
				pool[n + 12288] = rgb - (rgb >>> 2) - (rgb >>> 3) & 0xF8F8FF;
			}
		} else {
			if (t.width == 64) {
				for (int y = 0; y < 128; y++) {
					for (int x = 0; x < 128; x++) {
						pool[x + (y << 7)] = src[(t.data[(x >> 1) + (y >> 1 << 6)])];
					}
				}
			} else {
				for (int n = 0; n < (128 * 128); n++) {
					pool[n] = src[t.data[n]];
				}
			}

			textureHasTransparency[texture] = false;

			for (int n = 0; n < (128 * 128); n++) {
				pool[n] &= 0xF8F8FF;

				int rgb = pool[n];

				if (rgb == 0) {
					textureHasTransparency[texture] = true;
				}

				pool[n + 16384] = rgb - (rgb >>> 3) & 0xF8F8FF;
				pool[n + 32768] = rgb - (rgb >>> 2) & 0xF8F8FF;
				pool[n + 49152] = rgb - (rgb >>> 2) - (rgb >>> 3) & 0xF8F8FF;
			}
		}
		return pool;
	}

	public static final void generatePalette(double brightness) {
		int off = 0;

		for (int y = 0; y < 512; y++) {
			double hue = (double) (y / 8) / 64.0 + 0.0078125;
			double saturation = (double) (y & 0x7) / 8.0 + 0.0625;

			for (int x = 0; x < 128; x++) {
				double lightness = (double) x / 128.0;
				double r = lightness;
				double g = lightness;
				double b = lightness;

				if (saturation != 0.0) {
					double d_36_;

					if (lightness < 0.5) {
						d_36_ = lightness * (1.0 + saturation);
					} else {
						d_36_ = lightness + saturation - lightness * saturation;
					}

					double d_37_ = 2.0 * lightness - d_36_;
					double d_38_ = hue + 0.3333333333333333;

					if (d_38_ > 1.0) {
						d_38_--;
					}

					double d_39_ = hue;
					double d_40_ = hue - 0.3333333333333333;

					if (d_40_ < 0.0) {
						d_40_++;
					}

					if (6.0 * d_38_ < 1.0) {
						r = d_37_ + (d_36_ - d_37_) * 6.0 * d_38_;
					} else if (2.0 * d_38_ < 1.0) {
						r = d_36_;
					} else if (3.0 * d_38_ < 2.0) {
						r = d_37_ + (d_36_ - d_37_) * (0.6666666666666666 - d_38_) * 6.0;
					} else {
						r = d_37_;
					}

					if (6.0 * d_39_ < 1.0) {
						g = d_37_ + (d_36_ - d_37_) * 6.0 * d_39_;
					} else if (2.0 * d_39_ < 1.0) {
						g = d_36_;
					} else if (3.0 * d_39_ < 2.0) {
						g = d_37_ + (d_36_ - d_37_) * (0.6666666666666666 - d_39_) * 6.0;
					} else {
						g = d_37_;
					}

					if (6.0 * d_40_ < 1.0) {
						b = d_37_ + (d_36_ - d_37_) * 6.0 * d_40_;
					} else if (2.0 * d_40_ < 1.0) {
						b = d_36_;
					} else if (3.0 * d_40_ < 2.0) {
						b = d_37_ + (d_36_ - d_37_) * (0.6666666666666666 - d_40_) * 6.0;
					} else {
						b = d_37_;
					}
				}

				int rgb = ((int) (r * 256.0) << 16) + ((int) (g * 256.0) << 8) + (int) (b * 256.0);
				rgb = adjustRGBIntensity(rgb, brightness);
				palette[off++] = rgb;
			}

			for (int n = 0; n < 50; n++) {
				if (textures[n] != null) {
					int[] palette = (textures[n].palette);
					originalTexels[n] = new int[palette.length];

					for (int m = 0; m < palette.length; m++) {
						originalTexels[n][m] = adjustRGBIntensity(palette[m], brightness);
					}
				}
			}

			for (int n = 0; n < 50; n++) {
				updateTexture(n);
			}
		}
	}

	public static int adjustRGBIntensity(int rgb, double d) {
		double r = (double) (rgb >> 16) / 256.0;
		double g = (double) (rgb >> 8 & 0xff) / 256.0;
		double b = (double) (rgb & 0xff) / 256.0;
		r = Math.pow(r, d);
		g = Math.pow(g, d);
		b = Math.pow(b, d);
		return ((int) (r * 256.0) << 16) + ((int) (g * 256.0) << 8) + (int) (b * 256.0);
	}

	public static final void fillShadedTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int hsl1, int hsl2, int hsl3) {
		int s1 = 0, s2 = 0, s3 = 0; // slope
		int ls1 = 0, ls2 = 0, ls3 = 0; // lightness slope

		if (y2 != y1) {
			s1 = (x2 - x1 << 16) / (y2 - y1);
			ls1 = (hsl2 - hsl1 << 15) / (y2 - y1);
		}

		if (y3 != y2) {
			s2 = (x3 - x2 << 16) / (y3 - y2);
			ls2 = (hsl3 - hsl2 << 15) / (y3 - y2);
		}

		if (y3 != y1) {
			s3 = (x1 - x3 << 16) / (y1 - y3);
			ls3 = (hsl1 - hsl3 << 15) / (y1 - y3);
		}

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= Canvas2D.bottom) {
				return;
			}

			if (y2 > Canvas2D.bottom) {
				y2 = Canvas2D.bottom;
			}

			if (y3 > Canvas2D.bottom) {
				y3 = Canvas2D.bottom;
			}

			if (y2 < y3) {
				x3 = x1 <<= 16;
				hsl3 = hsl1 <<= 15;

				if (y1 < 0) {
					x3 -= s3 * y1;
					x1 -= s1 * y1;
					hsl3 -= ls3 * y1;
					hsl1 -= ls1 * y1;
					y1 = 0;
				}

				x2 <<= 16;
				hsl2 <<= 15;

				if (y2 < 0) {
					x2 -= s2 * y2;
					hsl2 -= ls2 * y2;
					y2 = 0;
				}

				if (y1 != y2 && s3 < s1 || y1 == y2 && s3 > s2) {
					y3 -= y2;
					y2 -= y1;
					y1 = offsets[y1];

					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7);
						x3 += s3;
						x1 += s1;
						hsl3 += ls3;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}

					while (--y3 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7);
						x3 += s3;
						x2 += s2;
						hsl3 += ls3;
						hsl2 += ls2;
						y1 += Canvas2D.dstW;
					}
				} else {
					y3 -= y2;
					y2 -= y1;
					y1 = offsets[y1];

					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7);
						x3 += s3;
						x1 += s1;
						hsl3 += ls3;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}

					while (--y3 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7);
						x3 += s3;
						x2 += s2;
						hsl3 += ls3;
						hsl2 += ls2;
						y1 += Canvas2D.dstW;
					}
				}
			} else {
				x2 = x1 <<= 16;
				hsl2 = hsl1 <<= 15;

				if (y1 < 0) {
					x2 -= s3 * y1;
					x1 -= s1 * y1;
					hsl2 -= ls3 * y1;
					hsl1 -= ls1 * y1;
					y1 = 0;
				}

				x3 <<= 16;
				hsl3 <<= 15;

				if (y3 < 0) {
					x3 -= s2 * y3;
					hsl3 -= ls2 * y3;
					y3 = 0;
				}

				if (y1 != y3 && s3 < s1 || y1 == y3 && s2 > s1) {
					y2 -= y3;
					y3 -= y1;
					y1 = offsets[y1];

					while (--y3 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7);
						x2 += s3;
						x1 += s1;
						hsl2 += ls3;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}

					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7);
						x3 += s2;
						x1 += s1;
						hsl3 += ls2;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}
				} else {
					y2 -= y3;
					y3 -= y1;
					y1 = offsets[y1];

					while (--y3 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7);
						x2 += s3;
						x1 += s1;
						hsl2 += ls3;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}

					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y1, 0, 0, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7);
						x3 += s2;
						x1 += s1;
						hsl3 += ls2;
						hsl1 += ls1;
						y1 += Canvas2D.dstW;
					}
				}
			}
		} else if (y2 <= y3) {
			if (y2 < Canvas2D.bottom) {
				if (y3 > Canvas2D.bottom) {
					y3 = Canvas2D.bottom;
				}

				if (y1 > Canvas2D.bottom) {
					y1 = Canvas2D.bottom;
				}

				if (y3 < y1) {
					x1 = x2 <<= 16;
					hsl1 = hsl2 <<= 15;
					if (y2 < 0) {
						x1 -= s1 * y2;
						x2 -= s2 * y2;
						hsl1 -= ls1 * y2;
						hsl2 -= ls2 * y2;
						y2 = 0;
					}
					x3 <<= 16;
					hsl3 <<= 15;
					if (y3 < 0) {
						x3 -= s3 * y3;
						hsl3 -= ls3 * y3;
						y3 = 0;
					}
					if (y2 != y3 && s1 < s2 || y2 == y3 && s1 > s3) {
						y1 -= y3;
						y3 -= y2;
						y2 = offsets[y2];
						while (--y3 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7);
							x1 += s1;
							x2 += s2;
							hsl1 += ls1;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
						while (--y1 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7);
							x1 += s1;
							x3 += s3;
							hsl1 += ls1;
							hsl3 += ls3;
							y2 += Canvas2D.dstW;
						}
					} else {
						y1 -= y3;
						y3 -= y2;
						y2 = offsets[y2];
						while (--y3 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7);
							x1 += s1;
							x2 += s2;
							hsl1 += ls1;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
						while (--y1 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7);
							x1 += s1;
							x3 += s3;
							hsl1 += ls1;
							hsl3 += ls3;
							y2 += Canvas2D.dstW;
						}
					}
				} else {
					x3 = x2 <<= 16;
					hsl3 = hsl2 <<= 15;
					if (y2 < 0) {
						x3 -= s1 * y2;
						x2 -= s2 * y2;
						hsl3 -= ls1 * y2;
						hsl2 -= ls2 * y2;
						y2 = 0;
					}
					x1 <<= 16;
					hsl1 <<= 15;
					if (y1 < 0) {
						x1 -= s3 * y1;
						hsl1 -= ls3 * y1;
						y1 = 0;
					}
					if (s1 < s2) {
						y3 -= y1;
						y1 -= y2;
						y2 = offsets[y2];
						while (--y1 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7);
							x3 += s1;
							x2 += s2;
							hsl3 += ls1;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
						while (--y3 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7);
							x1 += s3;
							x2 += s2;
							hsl1 += ls3;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
					} else {
						y3 -= y1;
						y1 -= y2;
						y2 = offsets[y2];
						while (--y1 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7);
							x3 += s1;
							x2 += s2;
							hsl3 += ls1;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
						while (--y3 >= 0) {
							drawGradientScanline(Canvas2D.dst, y2, 0, 0, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7);
							x1 += s3;
							x2 += s2;
							hsl1 += ls3;
							hsl2 += ls2;
							y2 += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (y3 < Canvas2D.bottom) {
			if (y1 > Canvas2D.bottom) {
				y1 = Canvas2D.bottom;
			}
			if (y2 > Canvas2D.bottom) {
				y2 = Canvas2D.bottom;
			}
			if (y1 < y2) {
				x2 = x3 <<= 16;
				hsl2 = hsl3 <<= 15;
				if (y3 < 0) {
					x2 -= s2 * y3;
					x3 -= s3 * y3;
					hsl2 -= ls2 * y3;
					hsl3 -= ls3 * y3;
					y3 = 0;
				}
				x1 <<= 16;
				hsl1 <<= 15;
				if (y1 < 0) {
					x1 -= s1 * y1;
					hsl1 -= ls1 * y1;
					y1 = 0;
				}
				if (s2 < s3) {
					y2 -= y1;
					y1 -= y3;
					y3 = offsets[y3];
					while (--y1 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7);
						x2 += s2;
						x3 += s3;
						hsl2 += ls2;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7);
						x2 += s2;
						x1 += s1;
						hsl2 += ls2;
						hsl1 += ls1;
						y3 += Canvas2D.dstW;
					}
				} else {
					y2 -= y1;
					y1 -= y3;
					y3 = offsets[y3];
					while (--y1 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7);
						x2 += s2;
						x3 += s3;
						hsl2 += ls2;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7);
						x2 += s2;
						x1 += s1;
						hsl2 += ls2;
						hsl1 += ls1;
						y3 += Canvas2D.dstW;
					}
				}
			} else {
				x1 = x3 <<= 16;
				hsl1 = hsl3 <<= 15;
				if (y3 < 0) {
					x1 -= s2 * y3;
					x3 -= s3 * y3;
					hsl1 -= ls2 * y3;
					hsl3 -= ls3 * y3;
					y3 = 0;
				}
				x2 <<= 16;
				hsl2 <<= 15;
				if (y2 < 0) {
					x2 -= s1 * y2;
					hsl2 -= ls1 * y2;
					y2 = 0;
				}
				if (s2 < s3) {
					y1 -= y2;
					y2 -= y3;
					y3 = offsets[y3];
					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7);
						x1 += s2;
						x3 += s3;
						hsl1 += ls2;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
					while (--y1 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7);
						x2 += s1;
						x3 += s3;
						hsl2 += ls1;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
				} else {
					y1 -= y2;
					y2 -= y3;
					y3 = offsets[y3];
					while (--y2 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7);
						x1 += s2;
						x3 += s3;
						hsl1 += ls2;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
					while (--y1 >= 0) {
						drawGradientScanline(Canvas2D.dst, y3, 0, 0, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7);
						x2 += s1;
						x3 += s3;
						hsl2 += ls1;
						hsl3 += ls3;
						y3 += Canvas2D.dstW;
					}
				}
			}
		}
	}

	public static final void drawGradientScanline(int[] dst, int off, int rgb, int length, int x0, int x1, int hsl0, int hsl1) {
		if (texturedShading) {
			int lightSlope; // lightness slope

			if (verifyBounds) {
				if (x1 - x0 > 3) {
					lightSlope = (hsl1 - hsl0) / (x1 - x0);
				} else {
					lightSlope = 0;
				}
				if (x1 > Canvas2D.dstXBound) {
					x1 = Canvas2D.dstXBound;
				}

				if (x0 < 0) {
					hsl0 -= x0 * lightSlope;
					x0 = 0;
				}

				if (x0 >= x1) {
					return;
				}

				off += x0;
				length = x1 - x0 >> 2;
				lightSlope <<= 2;
			} else {
				if (x0 >= x1) {
					return;
				}

				off += x0;
				length = x1 - x0 >> 2;

				if (length > 0) {
					lightSlope = (hsl1 - hsl0) * lightnessLerpArray[length] >> 15;
				} else {
					lightSlope = 0;
				}
			}

			if (alpha == 0) {
				while (--length >= 0) {
					rgb = palette[hsl0 >> 8];
					hsl0 += lightSlope;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
				}

				length = x1 - x0 & 0x3;

				if (length > 0) {
					rgb = palette[hsl0 >> 8];
					do {
						dst[off++] = rgb;
					} while (--length > 0);
				}
			} else {
				int a0 = alpha;
				int a1 = 256 - alpha;

				while (--length >= 0) {
					rgb = palette[hsl0 >> 8];
					hsl0 += lightSlope;
					rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
				}

				length = x1 - x0 & 0x3;

				if (length > 0) {
					rgb = palette[hsl0 >> 8];
					rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));
					do {
						dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
						off++;
					} while (--length > 0);
				}
			}
		} else if (x0 < x1) {
			int cs = (hsl1 - hsl0) / (x1 - x0);

			if (verifyBounds) {
				if (x1 > Canvas2D.dstXBound) {
					x1 = Canvas2D.dstXBound;
				}

				if (x0 < 0) {
					hsl0 -= x0 * cs;
					x0 = 0;
				}

				if (x0 >= x1) {
					return;
				}
			}

			off += x0;
			length = x1 - x0;

			if (alpha == 0) {
				do {
					dst[off++] = palette[hsl0 >> 8];
					hsl0 += cs;
				} while (--length > 0);
			} else {
				int a0 = alpha;
				int a1 = 256 - alpha;
				do {
					rgb = palette[hsl0 >> 8];
					hsl0 += cs;
					rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));
					dst[off++] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
				} while (--length > 0);
			}
		}
	}

	public static final void drawTriangle(int y0, int y1, int y2, int x0, int x1, int x2, int rgb) {
		drawLine(x0, y0, x1, y1, rgb);
		drawLine(x1, y1, x2, y2, rgb);
		drawLine(x2, y2, x0, y0, rgb);
	}

	public static final void fillTriangle(int y0, int y1, int y2, int x0, int x1, int x2, int rgb) {
		int s0 = 0, s1 = 0, s2 = 0; // slopes

		if (y1 != y0) {
			s0 = (x1 - x0 << 16) / (y1 - y0);
		}

		if (y2 != y1) {
			s1 = (x2 - x1 << 16) / (y2 - y1);
		}

		if (y2 != y0) {
			s2 = (x0 - x2 << 16) / (y0 - y2);
		}

		if (y0 <= y1 && y0 <= y2) {
			if (y0 < Canvas2D.bottom) {
				if (y1 > Canvas2D.bottom) {
					y1 = Canvas2D.bottom;
				}

				if (y2 > Canvas2D.bottom) {
					y2 = Canvas2D.bottom;
				}

				if (y1 < y2) {
					x2 = x0 <<= 16;

					if (y0 < 0) {
						x2 -= s2 * y0;
						x0 -= s0 * y0;
						y0 = 0;
					}

					x1 <<= 16;

					if (y1 < 0) {
						x1 -= s1 * y1;
						y1 = 0;
					}

					if (y0 != y1 && s2 < s0 || y0 == y1 && s2 > s1) {
						y2 -= y1;
						y1 -= y0;
						y0 = offsets[y0];
						while (--y1 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x2 >> 16, x0 >> 16);
							x2 += s2;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x2 >> 16, x1 >> 16);
							x2 += s2;
							x1 += s1;
							y0 += Canvas2D.dstW;
						}
					} else {
						y2 -= y1;
						y1 -= y0;
						y0 = offsets[y0];
						while (--y1 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x0 >> 16, x2 >> 16);
							x2 += s2;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x1 >> 16, x2 >> 16);
							x2 += s2;
							x1 += s1;
							y0 += Canvas2D.dstW;
						}
					}
				} else {
					x1 = x0 <<= 16;
					if (y0 < 0) {
						x1 -= s2 * y0;
						x0 -= s0 * y0;
						y0 = 0;
					}
					x2 <<= 16;
					if (y2 < 0) {
						x2 -= s1 * y2;
						y2 = 0;
					}
					if (y0 != y2 && s2 < s0 || y0 == y2 && s1 > s0) {
						y1 -= y2;
						y2 -= y0;
						y0 = offsets[y0];
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x1 >> 16, x0 >> 16);
							x1 += s2;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
						while (--y1 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x2 >> 16, x0 >> 16);
							x2 += s1;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
					} else {
						y1 -= y2;
						y2 -= y0;
						y0 = offsets[y0];
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x0 >> 16, x1 >> 16);
							x1 += s2;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
						while (--y1 >= 0) {
							drawScaline(Canvas2D.dst, y0, rgb, 0, x0 >> 16, x2 >> 16);
							x2 += s1;
							x0 += s0;
							y0 += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (y1 <= y2) {
			if (y1 < Canvas2D.bottom) {
				if (y2 > Canvas2D.bottom) {
					y2 = Canvas2D.bottom;
				}

				if (y0 > Canvas2D.bottom) {
					y0 = Canvas2D.bottom;
				}

				if (y2 < y0) {
					x0 = x1 <<= 16;

					if (y1 < 0) {
						x0 -= s0 * y1;
						x1 -= s1 * y1;
						y1 = 0;
					}

					x2 <<= 16;

					if (y2 < 0) {
						x2 -= s2 * y2;
						y2 = 0;
					}

					if (y1 != y2 && s0 < s1 || y1 == y2 && s0 > s2) {
						y0 -= y2;
						y2 -= y1;
						y1 = offsets[y1];
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x0 >> 16, x1 >> 16);
							x0 += s0;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
						while (--y0 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x0 >> 16, x2 >> 16);
							x0 += s0;
							x2 += s2;
							y1 += Canvas2D.dstW;
						}
					} else {
						y0 -= y2;
						y2 -= y1;
						y1 = offsets[y1];
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x1 >> 16, x0 >> 16);
							x0 += s0;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
						while (--y0 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x2 >> 16, x0 >> 16);
							x0 += s0;
							x2 += s2;
							y1 += Canvas2D.dstW;
						}
					}
				} else {
					x2 = x1 <<= 16;
					if (y1 < 0) {
						x2 -= s0 * y1;
						x1 -= s1 * y1;
						y1 = 0;
					}
					x0 <<= 16;
					if (y0 < 0) {
						x0 -= s2 * y0;
						y0 = 0;
					}
					if (s0 < s1) {
						y2 -= y0;
						y0 -= y1;
						y1 = offsets[y1];
						while (--y0 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x2 >> 16, x1 >> 16);
							x2 += s0;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x0 >> 16, x1 >> 16);
							x0 += s2;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
					} else {
						y2 -= y0;
						y0 -= y1;
						y1 = offsets[y1];
						while (--y0 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x1 >> 16, x2 >> 16);
							x2 += s0;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
						while (--y2 >= 0) {
							drawScaline(Canvas2D.dst, y1, rgb, 0, x1 >> 16, x0 >> 16);
							x0 += s2;
							x1 += s1;
							y1 += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (y2 < Canvas2D.bottom) {
			if (y0 > Canvas2D.bottom) {
				y0 = Canvas2D.bottom;
			}
			if (y1 > Canvas2D.bottom) {
				y1 = Canvas2D.bottom;
			}
			if (y0 < y1) {
				x1 = x2 <<= 16;
				if (y2 < 0) {
					x1 -= s1 * y2;
					x2 -= s2 * y2;
					y2 = 0;
				}
				x0 <<= 16;
				if (y0 < 0) {
					x0 -= s0 * y0;
					y0 = 0;
				}
				if (s1 < s2) {
					y1 -= y0;
					y0 -= y2;
					y2 = offsets[y2];
					while (--y0 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x1 >> 16, x2 >> 16);
						x1 += s1;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
					while (--y1 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x1 >> 16, x0 >> 16);
						x1 += s1;
						x0 += s0;
						y2 += Canvas2D.dstW;
					}
				} else {
					y1 -= y0;
					y0 -= y2;
					y2 = offsets[y2];
					while (--y0 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x2 >> 16, x1 >> 16);
						x1 += s1;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
					while (--y1 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x0 >> 16, x1 >> 16);
						x1 += s1;
						x0 += s0;
						y2 += Canvas2D.dstW;
					}
				}
			} else {
				x0 = x2 <<= 16;
				if (y2 < 0) {
					x0 -= s1 * y2;
					x2 -= s2 * y2;
					y2 = 0;
				}
				x1 <<= 16;
				if (y1 < 0) {
					x1 -= s0 * y1;
					y1 = 0;
				}
				if (s1 < s2) {
					y0 -= y1;
					y1 -= y2;
					y2 = offsets[y2];
					while (--y1 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x0 >> 16, x2 >> 16);
						x0 += s1;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
					while (--y0 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x1 >> 16, x2 >> 16);
						x1 += s0;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
				} else {
					y0 -= y1;
					y1 -= y2;
					y2 = offsets[y2];
					while (--y1 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x2 >> 16, x0 >> 16);
						x0 += s1;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
					while (--y0 >= 0) {
						drawScaline(Canvas2D.dst, y2, rgb, 0, x2 >> 16, x1 >> 16);
						x1 += s0;
						x2 += s2;
						y2 += Canvas2D.dstW;
					}
				}
			}
		}
	}

	public static final void drawScaline(int[] dst, int off, int rgb, int length, int x0, int x1) {
		if (verifyBounds) {
			if (x1 > Canvas2D.dstXBound) {
				x1 = Canvas2D.dstXBound;
			}
			if (x0 < 0) {
				x0 = 0;
			}
		}

		if (x0 < x1) {
			off += x0;
			length = x1 - x0 >> 2;

			if (alpha == 0) {
				while (--length >= 0) {
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
				}
				length = x1 - x0 & 0x3;

				while (--length >= 0) {
					dst[off++] = rgb;
				}
			} else {
				int a0 = alpha;
				int a1 = 256 - alpha;
				rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));

				while (--length >= 0) {
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
				}

				length = x1 - x0 & 0x3;

				while (--length >= 0) {
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
				}
			}
		}
	}

	public static final void fillTexturedTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int l1, int l2, int l3, int xP, int xM, int xN, int yP, int yM, int yN, int zP, int zM, int zN, int textureIndex) {
		// INT24_RGB
		int[] texels = getTexels(textureIndex);

		opaque = !textureHasTransparency[textureIndex];

		xM = xP - xM;
		yM = yP - yM;
		zM = zP - zM;

		xN -= xP;
		yN -= yP;
		zN -= zP;

		int aO = ((zN * xP) - (xN * zP)) << 5;
		int aH = ((yN * zP) - (zN * yP)) << 8;
		int aV = ((xN * yP) - (yN * xP)) << 14;

		int bO = ((zM * xP) - (xM * zP)) << 5;
		int bH = ((yM * zP) - (zM * yP)) << 8;
		int bV = ((xM * yP) - (yM * xP)) << 14;

		int cO = ((xM * zN) - (zM * xN)) << 5;
		int cH = ((zM * yN) - (yM * zN)) << 8;
		int cV = ((yM * xN) - (xM * yN)) << 14;

		int abSlope = 0;
		int abLightSlope = 0;

		if (y2 != y1) {
			abSlope = (x2 - x1 << 16) / (y2 - y1);
			abLightSlope = (l2 - l1 << 16) / (y2 - y1);
		}

		int bcSlope = 0;
		int bcLightSlope = 0;

		if (y3 != y2) {
			bcSlope = (x3 - x2 << 16) / (y3 - y2);
			bcLightSlope = (l3 - l2 << 16) / (y3 - y2);
		}

		int caSlope = 0;
		int caLightSlope = 0;

		if (y3 != y1) {
			caSlope = (x1 - x3 << 16) / (y1 - y3);
			caLightSlope = (l1 - l3 << 16) / (y1 - y3);
		}

		if (y1 <= y2 && y1 <= y3) {
			if (y1 < Canvas2D.bottom) {
				if (y2 > Canvas2D.bottom) {
					y2 = Canvas2D.bottom;
				}

				if (y3 > Canvas2D.bottom) {
					y3 = Canvas2D.bottom;
				}

				if (y2 < y3) {
					x3 = x1 <<= 16;
					l3 = l1 <<= 16;

					if (y1 < 0) {
						x3 -= caSlope * y1;
						x1 -= abSlope * y1;

						l3 -= caLightSlope * y1;
						l1 -= abLightSlope * y1;

						y1 = 0;
					}

					x2 <<= 16;
					l2 <<= 16;

					if (y2 < 0) {
						x2 -= bcSlope * y2;
						l2 -= bcLightSlope * y2;
						y2 = 0;
					}

					int offsetY = y1 - centerY;
					aV += aO * offsetY;
					bV += bO * offsetY;
					cV += cO * offsetY;

					if (y1 != y2 && caSlope < abSlope || y1 == y2 && caSlope > bcSlope) {
						y3 -= y2;
						y2 -= y1;
						y1 = offsets[y1];

						while (--y2 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x3 >> 16, x1 >> 16, l3 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += caSlope;
							x1 += abSlope;

							l3 += caLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x3 >> 16, x2 >> 16, l3 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += caSlope;
							x2 += bcSlope;

							l3 += caLightSlope;
							l2 += bcLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					} else {
						y3 -= y2;
						y2 -= y1;
						y1 = offsets[y1];

						while (--y2 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x1 >> 16, x3 >> 16, l1 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += caSlope;
							x1 += abSlope;

							l3 += caLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x2 >> 16, x3 >> 16, l2 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += caSlope;
							x2 += bcSlope;

							l3 += caLightSlope;
							l2 += bcLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					}
				} else {
					x2 = x1 <<= 16;
					l2 = l1 <<= 16;

					if (y1 < 0) {
						x2 -= caSlope * y1;
						x1 -= abSlope * y1;

						l2 -= caLightSlope * y1;
						l1 -= abLightSlope * y1;

						y1 = 0;
					}

					x3 <<= 16;
					l3 <<= 16;

					if (y3 < 0) {
						x3 -= bcSlope * y3;
						l3 -= bcLightSlope * y3;
						y3 = 0;
					}

					int offsetY = y1 - centerY;
					aV += aO * offsetY;
					bV += bO * offsetY;
					cV += cO * offsetY;

					if (y1 != y3 && caSlope < abSlope || y1 == y3 && bcSlope > abSlope) {
						y2 -= y3;
						y3 -= y1;
						y1 = offsets[y1];

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x2 >> 16, x1 >> 16, l2 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x2 += caSlope;
							x1 += abSlope;

							l2 += caLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y2 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x3 >> 16, x1 >> 16, l3 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += bcSlope;
							x1 += abSlope;

							l3 += bcLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					} else {
						y2 -= y3;
						y3 -= y1;
						y1 = offsets[y1];

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x1 >> 16, x2 >> 16, l1 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
							x2 += caSlope;
							x1 += abSlope;

							l2 += caLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y2 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y1, x1 >> 16, x3 >> 16, l1 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += bcSlope;
							x1 += abSlope;

							l3 += bcLightSlope;
							l1 += abLightSlope;

							y1 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					}
				}
			}
		} else if (y2 <= y3) {
			if (y2 < Canvas2D.bottom) {
				if (y3 > Canvas2D.bottom) {
					y3 = Canvas2D.bottom;
				}

				if (y1 > Canvas2D.bottom) {
					y1 = Canvas2D.bottom;
				}

				if (y3 < y1) {
					x1 = x2 <<= 16;
					l1 = l2 <<= 16;

					if (y2 < 0) {
						x1 -= abSlope * y2;
						x2 -= bcSlope * y2;

						l1 -= abLightSlope * y2;
						l2 -= bcLightSlope * y2;

						y2 = 0;
					}

					x3 <<= 16;
					l3 <<= 16;

					if (y3 < 0) {
						x3 -= caSlope * y3;
						l3 -= caLightSlope * y3;
						y3 = 0;
					}

					int offsetY = y2 - centerY;
					aV += aO * offsetY;
					bV += bO * offsetY;
					cV += cO * offsetY;

					if (y2 != y3 && abSlope < bcSlope || y2 == y3 && abSlope > caSlope) {
						y1 -= y3;
						y3 -= y2;
						y2 = offsets[y2];

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x1 >> 16, x2 >> 16, l1 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += abSlope;
							x2 += bcSlope;
							l1 += abLightSlope;
							l2 += bcLightSlope;
							y2 += Canvas2D.dstW;
							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y1 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x1 >> 16, x3 >> 16, l1 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += abSlope;
							x3 += caSlope;
							l1 += abLightSlope;
							l3 += caLightSlope;
							y2 += Canvas2D.dstW;
							aV += aO;
							bV += bO;
							cV += cO;
						}
					} else {
						y1 -= y3;
						y3 -= y2;
						y2 = offsets[y2];

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x2 >> 16, x1 >> 16, l2 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += abSlope;
							x2 += bcSlope;

							l1 += abLightSlope;
							l2 += bcLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y1 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x3 >> 16, x1 >> 16, l3 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += abSlope;
							x3 += caSlope;

							l1 += abLightSlope;
							l3 += caLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					}
				} else {
					x3 = x2 <<= 16;
					l3 = l2 <<= 16;

					if (y2 < 0) {
						x3 -= abSlope * y2;
						x2 -= bcSlope * y2;

						l3 -= abLightSlope * y2;
						l2 -= bcLightSlope * y2;

						y2 = 0;
					}

					x1 <<= 16;
					l1 <<= 16;

					if (y1 < 0) {
						x1 -= caSlope * y1;
						l1 -= caLightSlope * y1;
						y1 = 0;
					}

					int offsetY = y2 - centerY;
					aV += aO * offsetY;
					bV += bO * offsetY;
					cV += cO * offsetY;

					if (abSlope < bcSlope) {
						y3 -= y1;
						y1 -= y2;
						y2 = offsets[y2];

						while (--y1 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x3 >> 16, x2 >> 16, l3 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += abSlope;
							x2 += bcSlope;

							l3 += abLightSlope;
							l2 += bcLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x1 >> 16, x2 >> 16, l1 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += caSlope;
							x2 += bcSlope;

							l1 += caLightSlope;
							l2 += bcLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					} else {
						y3 -= y1;
						y1 -= y2;
						y2 = offsets[y2];

						while (--y1 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x2 >> 16, x3 >> 16, l2 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
							x3 += abSlope;
							x2 += bcSlope;

							l3 += abLightSlope;
							l2 += bcLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}

						while (--y3 >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y2, x2 >> 16, x1 >> 16, l2 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
							x1 += caSlope;
							x2 += bcSlope;

							l1 += caLightSlope;
							l2 += bcLightSlope;

							y2 += Canvas2D.dstW;

							aV += aO;
							bV += bO;
							cV += cO;
						}
					}
				}
			}
		} else if (y3 < Canvas2D.bottom) {
			if (y1 > Canvas2D.bottom) {
				y1 = Canvas2D.bottom;
			}

			if (y2 > Canvas2D.bottom) {
				y2 = Canvas2D.bottom;
			}

			if (y1 < y2) {
				x2 = x3 <<= 16;
				l2 = l3 <<= 16;

				if (y3 < 0) {
					x2 -= bcSlope * y3;
					x3 -= caSlope * y3;

					l2 -= bcLightSlope * y3;
					l3 -= caLightSlope * y3;

					y3 = 0;
				}

				x1 <<= 16;
				l1 <<= 16;

				if (y1 < 0) {
					x1 -= abSlope * y1;
					l1 -= abLightSlope * y1;
					y1 = 0;
				}

				int offsetY = y3 - centerY;
				aV += aO * offsetY;
				bV += bO * offsetY;
				cV += cO * offsetY;

				if (bcSlope < caSlope) {
					y2 -= y1;
					y1 -= y3;
					y3 = offsets[y3];

					while (--y1 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x2 >> 16, x3 >> 16, l2 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += bcSlope;
						x3 += caSlope;

						l2 += bcLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}

					while (--y2 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x2 >> 16, x1 >> 16, l2 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += bcSlope;
						x1 += abSlope;

						l2 += bcLightSlope;
						l1 += abLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}
				} else {
					y2 -= y1;
					y1 -= y3;
					y3 = offsets[y3];

					while (--y1 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x3 >> 16, x2 >> 16, l3 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += bcSlope;
						x3 += caSlope;

						l2 += bcLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}

					while (--y2 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x1 >> 16, x2 >> 16, l1 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += bcSlope;
						x1 += abSlope;

						l2 += bcLightSlope;
						l1 += abLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}
				}
			} else {
				x1 = x3 <<= 16;
				l1 = l3 <<= 16;

				if (y3 < 0) {
					x1 -= bcSlope * y3;
					x3 -= caSlope * y3;

					l1 -= bcLightSlope * y3;
					l3 -= caLightSlope * y3;

					y3 = 0;
				}

				x2 <<= 16;
				l2 <<= 16;

				if (y2 < 0) {
					x2 -= abSlope * y2;
					l2 -= abLightSlope * y2;
					y2 = 0;
				}

				int offsetY = y3 - centerY;
				aV += aO * offsetY;
				bV += bO * offsetY;
				cV += cO * offsetY;

				if (bcSlope < caSlope) {
					y1 -= y2;
					y2 -= y3;
					y3 = offsets[y3];

					while (--y2 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x1 >> 16, x3 >> 16, l1 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
						x1 += bcSlope;
						x3 += caSlope;

						l1 += bcLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}

					while (--y1 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x2 >> 16, x3 >> 16, l2 >> 8, l3 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += abSlope;
						x3 += caSlope;

						l2 += abLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}
				} else {
					y1 -= y2;
					y2 -= y3;
					y3 = offsets[y3];

					while (--y2 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x3 >> 16, x1 >> 16, l3 >> 8, l1 >> 8, aV, bV, cV, aH, bH, cH);
						x1 += bcSlope;
						x3 += caSlope;

						l1 += bcLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}

					while (--y1 >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, y3, x3 >> 16, x2 >> 16, l3 >> 8, l2 >> 8, aV, bV, cV, aH, bH, cH);
						x2 += abSlope;
						x3 += caSlope;

						l2 += abLightSlope;
						l3 += caLightSlope;

						y3 += Canvas2D.dstW;

						aV += aO;
						bV += bO;
						cV += cO;
					}
				}
			}
		}
	}

	public static final void drawTexturedScanline(int[] dst, int[] texels, int u1, int v1, int off, int x1, int x2, int lightness0, int lightness1, int a, int b, int c, int aH, int bH, int cH) {
		if (x1 >= x2) {
			return;
		}

		int length;
		int lightSlope;

		if (verifyBounds) {
			lightSlope = (lightness1 - lightness0) / (x2 - x1);

			if (x2 > Canvas2D.dstXBound) {
				x2 = Canvas2D.dstXBound;
			}

			if (x1 < 0) {
				lightness0 -= x1 * lightSlope;
				x1 = 0;
			}

			if (x1 >= x2) {
				return;
			}

			length = x2 - x1 >> 3;
			lightSlope <<= 12;
			lightness0 <<= 9;
		} else {
			if (x2 - x1 > 7) {
				length = x2 - x1 >> 3;
				lightSlope = (lightness1 - lightness0) * lightnessLerpArray[length] >> 6;
			} else {
				length = 0;
				lightSlope = 0;
			}

			lightness0 <<= 9;
		}

		off += x1;

		if (lowmemory) {
			int u2 = 0;
			int v2 = 0;
			int deltaCenterX = x1 - centerX;

			a += (aH >> 3) * deltaCenterX;
			b += (bH >> 3) * deltaCenterX;
			c += (cH >> 3) * deltaCenterX;

			int realC = c >> 12;

			if (realC != 0) {
				u1 = a / realC;
				v1 = b / realC;

				if (u1 < 0) {
					u1 = 0;
				} else if (u1 > 4032) {
					u1 = 4032;
				}
			}

			a += aH;
			b += bH;
			c += cH;
			realC = c >> 12;

			if (realC != 0) {
				u2 = a / realC;
				v2 = b / realC;

				if (u2 < 7) {
					u2 = 7;
				} else if (u2 > 4032) {
					u2 = 4032;
				}
			}

			int uSlope = u2 - u1 >> 3;
			int vSlope = v2 - v1 >> 3;
			u1 += (lightness0 & 0x600000) >> 3;
			int i_155_ = lightness0 >> 23;

			if (opaque) {
				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0xFC0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 = u2;
					v1 = v2;

					a += aH;
					b += bH;
					c += cH;
					realC = c >> 12;

					if (realC != 0) {
						u2 = a / realC;
						v2 = b / realC;
						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 4032) {
							u2 = 4032;
						}
					}

					uSlope = u2 - u1 >> 3;
					vSlope = v2 - v1 >> 3;
					lightness0 += lightSlope;
					u1 += (lightness0 & 0x600000) >> 3;
					i_155_ = lightness0 >> 23;
				}
				length = x2 - x1 & 0x7;
				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;
				}
			} else {
				// ignore pure black texels

				while (length-- > 0) {
					int rgb;
					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 = u2;
					v1 = v2;

					a += aH;
					b += bH;
					c += cH;
					realC = c >> 12;

					if (realC != 0) {
						u2 = a / realC;
						v2 = b / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 4032) {
							u2 = 4032;
						}
					}

					uSlope = u2 - u1 >> 3;
					vSlope = v2 - v1 >> 3;
					lightness0 += lightSlope;
					u1 += (lightness0 & 0x600000) >> 3;
					i_155_ = lightness0 >> 23;
				}

				length = x2 - x1 & 0x7;

				while (length-- > 0) {
					int rgb;
					if ((rgb = texels[(v1 & 0xfc0) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;
				}
			}
		} else {
			int u2 = 0;
			int v2 = 0;
			int startX = x1 - centerX;

			a += (aH >> 3) * startX;
			b += (bH >> 3) * startX;
			c += (cH >> 3) * startX;
			int realC = c >> 14;

			if (realC != 0) {
				u1 = a / realC;
				v1 = b / realC;

				if (u1 < 0) {
					u1 = 0;
				} else if (u1 > 16256) {
					u1 = 16256;
				}
			}

			a += aH;
			b += bH;
			c += cH;
			realC = c >> 14;

			if (realC != 0) {
				u2 = a / realC;
				v2 = b / realC;

				if (u2 < 7) {
					u2 = 7;
				} else if (u2 > 16256) {
					u2 = 16256;
				}
			}

			int deltaU = u2 - u1 >> 3;
			int deltaV = v2 - v1 >> 3;
			u1 += lightness0 & 0x600000;
			int lightness = lightness0 >> 23;

			if (opaque) {
				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;

					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 = u2;
					v1 = v2;

					a += aH;
					b += bH;
					c += cH;
					realC = c >> 14;

					if (realC != 0) {
						u2 = a / realC;
						v2 = b / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 16256) {
							u2 = 16256;
						}
					}

					deltaU = u2 - u1 >> 3;
					deltaV = v2 - v1 >> 3;
					lightness0 += lightSlope;
					u1 += lightness0 & 0x600000;
					lightness = lightness0 >> 23;
				}

				length = x2 - x1 & 0x7;

				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness;
					u1 += deltaU;
					v1 += deltaV;
				}
			} else {
				while (length-- > 0) {
					int rgb;
					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;

					if ((rgb = (texels[(v1 & 0x3f80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 = u2;
					v1 = v2;

					a += aH;
					b += bH;
					c += cH;
					realC = (c >> 14);

					if (realC != 0) {
						u2 = a / realC;
						v2 = b / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 16256) {
							u2 = 16256;
						}
					}
					deltaU = u2 - u1 >> 3;
					deltaV = v2 - v1 >> 3;
					lightness0 += lightSlope;
					u1 += lightness0 & 0x600000;
					lightness = lightness0 >> 23;
				}

				length = x2 - x1 & 0x7;

				while (length-- > 0) {
					int rgb;
					if ((rgb = (texels[(v1 & 0x3F80) + (u1 >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += deltaU;
					v1 += deltaV;
				}
			}
		}
	}

	public static final void drawTexturedScanline2(int[] dst, int[] src, int i, int i_135_, int off, int x0, int x1, int l0, int l1, int i_141_, int i_142_, int i_143_, int i_144_, int i_145_, int i_146_) {
		if (x0 < x1) {
			int length;
			int sl0;
			if (verifyBounds) {
				sl0 = (l1 - l0) / (x1 - x0);

				if (x1 > Canvas2D.dstXBound) {
					x1 = Canvas2D.dstXBound;
				}

				if (x0 < 0) {
					l0 -= x0 * sl0;
					x0 = 0;
				}

				if (x0 >= x1) {
					return;
				}

				length = x1 - x0 >> 3;
				sl0 <<= 12;
				l0 <<= 9;
			} else {
				if (x1 - x0 > 7) {
					length = x1 - x0 >> 3;
					sl0 = (l1 - l0) * lightnessLerpArray[length] >> 6;
				} else {
					length = 0;
					sl0 = 0;
				}

				l0 <<= 9;
			}

			off += x0;

			if (lowmemory) {
				int i_149_ = 0;
				int i_150_ = 0;
				int i_151_ = x0 - centerX;
				i_141_ += (i_144_ >> 3) * i_151_;
				i_142_ += (i_145_ >> 3) * i_151_;
				i_143_ += (i_146_ >> 3) * i_151_;
				int i_152_ = i_143_ >> 12;
				if (i_152_ != 0) {
					i = i_141_ / i_152_;
					i_135_ = i_142_ / i_152_;
					if (i < 0) {
						i = 0;
					} else if (i > 4032) {
						i = 4032;
					}
				}
				i_141_ += i_144_;
				i_142_ += i_145_;
				i_143_ += i_146_;
				i_152_ = i_143_ >> 12;
				if (i_152_ != 0) {
					i_149_ = i_141_ / i_152_;
					i_150_ = i_142_ / i_152_;
					if (i_149_ < 7) {
						i_149_ = 7;
					} else if (i_149_ > 4032) {
						i_149_ = 4032;
					}
				}
				int i_153_ = i_149_ - i >> 3;
				int i_154_ = i_150_ - i_135_ >> 3;
				i += (l0 & 0x600000) >> 3;
				int i_155_ = l0 >> 23;
				if (opaque) {
					while (length-- > 0) {
						dst[off++] = src[(i_135_ & 0xFC0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i = i_149_;
						i_135_ = i_150_;
						i_141_ += i_144_;
						i_142_ += i_145_;
						i_143_ += i_146_;
						i_152_ = i_143_ >> 12;
						if (i_152_ != 0) {
							i_149_ = i_141_ / i_152_;
							i_150_ = i_142_ / i_152_;
							if (i_149_ < 7) {
								i_149_ = 7;
							} else if (i_149_ > 4032) {
								i_149_ = 4032;
							}
						}
						i_153_ = i_149_ - i >> 3;
						i_154_ = i_150_ - i_135_ >> 3;
						l0 += sl0;
						i += (l0 & 0x600000) >> 3;
						i_155_ = l0 >> 23;
					}
					length = x1 - x0 & 0x7;
					while (length-- > 0) {
						dst[off++] = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_;
						i += i_153_;
						i_135_ += i_154_;
					}
				} else {
					while (length-- > 0) {
						int i_156_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
						if ((i_156_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_156_;
						}
						off++;
						i = i_149_;
						i_135_ = i_150_;
						i_141_ += i_144_;
						i_142_ += i_145_;
						i_143_ += i_146_;
						i_152_ = i_143_ >> 12;
						if (i_152_ != 0) {
							i_149_ = i_141_ / i_152_;
							i_150_ = i_142_ / i_152_;
							if (i_149_ < 7) {
								i_149_ = 7;
							} else if (i_149_ > 4032) {
								i_149_ = 4032;
							}
						}
						i_153_ = i_149_ - i >> 3;
						i_154_ = i_150_ - i_135_ >> 3;
						l0 += sl0;
						i += (l0 & 0x600000) >> 3;
						i_155_ = l0 >> 23;
					}
					length = x1 - x0 & 0x7;
					while (length-- > 0) {
						int i_157_;
						if ((i_157_ = src[(i_135_ & 0xfc0) + (i >> 6)] >>> i_155_) != 0) {
							dst[off] = i_157_;
						}
						off++;
						i += i_153_;
						i_135_ += i_154_;
					}
				}
			} else {
				int i_158_ = 0;
				int i_159_ = 0;
				int i_160_ = x0 - centerX;
				i_141_ += (i_144_ >> 3) * i_160_;
				i_142_ += (i_145_ >> 3) * i_160_;
				i_143_ += (i_146_ >> 3) * i_160_;
				int i_161_ = i_143_ >> 14;
				if (i_161_ != 0) {
					i = i_141_ / i_161_;
					i_135_ = i_142_ / i_161_;
					if (i < 0) {
						i = 0;
					} else if (i > 16256) {
						i = 16256;
					}
				}
				i_141_ += i_144_;
				i_142_ += i_145_;
				i_143_ += i_146_;
				i_161_ = i_143_ >> 14;
				if (i_161_ != 0) {
					i_158_ = i_141_ / i_161_;
					i_159_ = i_142_ / i_161_;
					if (i_158_ < 7) {
						i_158_ = 7;
					} else if (i_158_ > 16256) {
						i_158_ = 16256;
					}
				}
				int i_162_ = i_158_ - i >> 3;
				int i_163_ = i_159_ - i_135_ >> 3;
				i += l0 & 0x600000;
				int i_164_ = l0 >> 23;
				if (opaque) {
					while (length-- > 0) {
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i = i_158_;
						i_135_ = i_159_;
						i_141_ += i_144_;
						i_142_ += i_145_;
						i_143_ += i_146_;
						i_161_ = i_143_ >> 14;
						if (i_161_ != 0) {
							i_158_ = i_141_ / i_161_;
							i_159_ = i_142_ / i_161_;
							if (i_158_ < 7) {
								i_158_ = 7;
							} else if (i_158_ > 16256) {
								i_158_ = 16256;
							}
						}
						i_162_ = i_158_ - i >> 3;
						i_163_ = i_159_ - i_135_ >> 3;
						l0 += sl0;
						i += l0 & 0x600000;
						i_164_ = l0 >> 23;
					}
					length = x1 - x0 & 0x7;
					while (length-- > 0) {
						dst[off++] = src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_;
						i += i_162_;
						i_135_ += i_163_;
					}
				} else {
					while (length-- > 0) {
						int i_165_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
						if ((i_165_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_165_;
						}
						off++;
						i = i_158_;
						i_135_ = i_159_;
						i_141_ += i_144_;
						i_142_ += i_145_;
						i_143_ += i_146_;
						i_161_ = i_143_ >> 14;
						if (i_161_ != 0) {
							i_158_ = i_141_ / i_161_;
							i_159_ = i_142_ / i_161_;
							if (i_158_ < 7) {
								i_158_ = 7;
							} else if (i_158_ > 16256) {
								i_158_ = 16256;
							}
						}
						i_162_ = i_158_ - i >> 3;
						i_163_ = i_159_ - i_135_ >> 3;
						l0 += sl0;
						i += l0 & 0x600000;
						i_164_ = l0 >> 23;
					}
					length = x1 - x0 & 0x7;
					while (length-- > 0) {
						int i_166_;
						if ((i_166_ = (src[(i_135_ & 0x3f80) + (i >> 7)] >>> i_164_)) != 0) {
							dst[off] = i_166_;
						}
						off++;
						i += i_162_;
						i_135_ += i_163_;
					}
				}
			}
		}
	}

	static {
		for (int i = 1; i < 512; i++) {
			lightnessLerpArray[i] = 32768 / i;
		}

		for (int i = 1; i < 2048; i++) {
			zLerpArray[i] = 65536 / i;
		}

		for (int i = 0; i < 2048; i++) {
			sin[i] = (int) (65536.0 * Math.sin((double) i * 0.0030679615));
			cos[i] = (int) (65536.0 * Math.cos((double) i * 0.0030679615));
		}

		textures = new IndexedBitmap[50];
		textureHasTransparency = new boolean[50];
		averageTextureRGB = new int[50];
		texelPool = new int[50][];
		textureCycles = new int[50];
		palette = new int[65536];
		originalTexels = new int[50][];
	}
}
