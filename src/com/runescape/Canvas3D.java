package com.runescape;

import java.util.logging.Logger;

public class Canvas3D extends Canvas2D {

	private static final Logger logger = Logger.getLogger(Canvas3D.class.getName());

	/**
	 * Whether to use less intensive procedures and memory storage.
	 */
	public static boolean lowmemory = true;

	/**
	 * Set to true when a point on a triangle is off screen. Used to clip
	 * interpolation variables in case they go off screen.
	 */
	public static boolean verifyBounds;

	/**
	 * When set to true, the renderer ignores blending alpha composition.
	 */
	public static boolean opaque;

	/**
	 * Determines whether to use
	 */
	public static boolean texturedShading = true;

	/**
	 * The global alpha component for drawing triangles.
	 */
	public static int alpha;

	/**
	 * The horizontal center of the 3d canvas.
	 */
	public static int centerX;

	/**
	 * The vertical center of the 3d canvas.
	 */
	public static int centerY;

	/**
	 * Technically a lookup table for 17.15 fixed point fractions.
	 */
	public static int[] oneOverFixed1715 = new int[512];

	/**
	 * Technically a lookup table for 16.16 fixed point fractions.
	 */
	public static int[] oneOverFixed1616 = new int[2048];

	/**
	 * A sine lookup table. PI = 1024 (180 Degrees)
	 */
	public static int[] sin = new int[2048];

	/**
	 * A cosine lookup table. PI = 1024 (180 Degrees)
	 */
	public static int[] cos = new int[2048];

	/**
	 * A temporary storage for vertical (y) pixel offsets.
	 */
	public static int[] offsets;

	/**
	 * The amount of textures loaded.
	 */
	public static int loadedTextureCount;

	/**
	 * Unmodified textures. These should never be written to.
	 */
	public static IndexedBitmap[] textures;

	/**
	 * Will be true if a texture contains a pixel with the value 0x00000000.
	 */
	public static boolean[] textureHasTransparency;

	/**
	 * Contains the average INT24_RGB of a texture.
	 */
	public static int[] textureColors;

	public static int texelPoolPosition;
	public static int[][] texelBuffer1, texelBuffer2;
	public static int[] textureCycles;
	public static int cycle;

	/**
	 * Stores RGB values that can be looked up in the HSL
	 * format:<br/><code>(hue << 10) | (saturation << 7) | lightness</code>
	 */
	public static int[] palette;

	/**
	 * Contains the texture palettes.
	 */
	public static int[][] texturePalettes;

	/**
	 * Nullifies all objects apart of this class.
	 */
	public static final void unload() {
		oneOverFixed1715 = null;
		oneOverFixed1616 = null;
		sin = null;
		cos = null;
		offsets = null;
		textures = null;
		textureHasTransparency = null;
		textureColors = null;
		texelBuffer2 = null;
		texelBuffer1 = null;
		textureCycles = null;
		palette = null;
		texturePalettes = null;
	}

	/**
	 * Generates the vertical pixel offsets using the width and height set in
	 * {@link Canvas2D}.
	 *
	 * @return the int[] of y offsets.
	 */
	public static final int[] prepareOffsets() {
		offsets = new int[Canvas2D.dstH];
		for (int y = 0; y < Canvas2D.dstH; y++) {
			offsets[y] = Canvas2D.dstW * y;
		}
		centerX = Canvas2D.dstW / 2;
		centerY = Canvas2D.dstH / 2;
		return offsets;
	}

	/**
	 * Generates the vertical pixel offsets.
	 *
	 * @param w the width.
	 * @param h the height.
	 * @return the int[] of y offsets.
	 */
	public static final int[] prepareOffsets(int w, int h) {
		offsets = new int[h];
		for (int y = 0; y < h; y++) {
			offsets[y] = w * y;
		}
		centerX = w / 2;
		centerY = h / 2;
		return offsets;
	}

	/**
	 * Clears all temporary pixel and texel pools.
	 */
	public static final void clearPools() {
		texelBuffer2 = null;

		for (int i = 0; i < 50; i++) {
			texelBuffer1[i] = null;
		}
	}

	/**
	 * Sets up the temporary pixel and texel pools.
	 *
	 * @param size
	 */
	public static final void setupPools(int size) {
		if (texelBuffer2 == null) {
			texelPoolPosition = size;

			if (lowmemory) {
				texelBuffer2 = new int[texelPoolPosition][128 * 128];
			} else {
				texelBuffer2 = new int[texelPoolPosition][256 * 256];
			}

			for (int n = 0; n < 50; n++) {
				texelBuffer1[n] = null;
			}
		}
	}

	/**
	 * Unpacks the textures as {@link IndexedBitmap}'s and stores them.
	 *
	 * @param archive the archive containing the textures.
	 */
	public static final void unpackTextures(Archive archive) {
		loadedTextureCount = 0;

		for (int n = 0; n < 50; n++) {
			try {
				textures[n] = new IndexedBitmap(archive, String.valueOf(n), 0);

				if (lowmemory && textures[n].clipWidth == 128) {
					textures[n].shrink();
				} else {
					textures[n].crop();
				}

				loadedTextureCount++;
			} catch (Exception e) {
				/* empty */
			}
		}
	}

	/**
	 * Returns the average INT24_RGB of a texture.
	 *
	 * @param textureIndex the texture index.
	 * @return the int24_rgb value.
	 */
	public static final int getTextureColor(int textureIndex) {
		if (textureColors[textureIndex] != 0) {
			return textureColors[textureIndex];
		}

		int r = 0;
		int g = 0;
		int b = 0;
		int length = texturePalettes[textureIndex].length;

		// sum all the color channels
		for (int n = 0; n < length; n++) {
			r += texturePalettes[textureIndex][n] >> 16 & 0xff;
			g += texturePalettes[textureIndex][n] >> 8 & 0xff;
			b += texturePalettes[textureIndex][n] & 0xff;
		}

		// average each channel and bitpack
		int rgb = adjustColorLightness((r / length << 16) + (g / length << 8) + b / length, 1.4);

		// we use 0 to identify as unretrieved
		if (rgb == 0) {
			rgb = 1;
		}

		// store the value to avoid having to average again
		textureColors[textureIndex] = rgb;
		return rgb;
	}

	/**
	 * Flips the current texture onto the next texel buffer.
	 *
	 * @param textureIndex the texture index.
	 */
	public static final void updateTexture(int textureIndex) {
		if (texelBuffer1[textureIndex] != null) {
			texelBuffer2[texelPoolPosition++] = texelBuffer1[textureIndex];
			texelBuffer1[textureIndex] = null;
		}
	}

	public static final int[] getTexels(int textureIndex) {
		textureCycles[textureIndex] = cycle++;

		if (texelBuffer1[textureIndex] != null) {
			return texelBuffer1[textureIndex];
		}

		int[] buffer;

		// If we've updated a texture, use that one as our buffer.
		if (texelPoolPosition > 0) {
			buffer = texelBuffer2[--texelPoolPosition];
			texelBuffer2[texelPoolPosition] = null;
		} else {
			// select the oldest pushed buffer

			int oldestCycle = 0;
			int index = -1;

			// iterate through each texture
			for (int n = 0; n < loadedTextureCount; n++) {
				// if the buffer for this texture exists, and it hasn't updated in awhile or the current selected index is -1
				if (texelBuffer1[n] != null && (textureCycles[n] < oldestCycle || index == -1)) {
					oldestCycle = textureCycles[n];
					index = n;
				}
			}

			buffer = texelBuffer1[index];
			texelBuffer1[index] = null;
		}

		texelBuffer1[textureIndex] = buffer;

		IndexedBitmap texture = textures[textureIndex];
		int[] texturePalette = texturePalettes[textureIndex];

		// low memory uses 64x64 textures instead of 128x128.
		if (lowmemory) {
			textureHasTransparency[textureIndex] = false;

			// iterate through each pixel
			for (int n = 0; n < (64 * 64); n++) {
				buffer[n] = texturePalette[texture.data[n]] & 0xF8F8FF;

				int rgb = buffer[n];

				if (rgb == 0) {
					textureHasTransparency[textureIndex] = true;
				}

				buffer[n + (64 * 64)] = rgb - (rgb >>> 3) & 0xF8F8FF;
				buffer[n + (64 * 128)] = rgb - (rgb >>> 2) & 0xF8F8FF;
				buffer[n + (64 * 192)] = rgb - (rgb >>> 2) - (rgb >>> 3) & 0xF8F8FF;
			}
		} else {
			if (texture.width == 64) {
				// implying src is 128x128: rescale from 128x128 to 64x64
				for (int y = 0; y < 128; y++) {
					for (int x = 0; x < 128; x++) {
						buffer[x + (y << 7)] = texturePalette[(texture.data[(x >> 1) + ((y >> 1) << 6)])];
					}
				}
			} else {
				for (int n = 0; n < (128 * 128); n++) {
					buffer[n] = texturePalette[texture.data[n]];
				}
			}

			textureHasTransparency[textureIndex] = false;

			for (int n = 0; n < (128 * 128); n++) {
				// correlates with the 3 and 2 bitshifts below? A MYSTERY TO BE SOLVED!
				// &= 1111 1000 1111 1000 1111 1111
				buffer[n] &= 0xF8F8FF;

				int rgb = buffer[n];

				if (rgb == 0) {
					textureHasTransparency[textureIndex] = true;
				}

				// TODO: Think!
				// pos = x + (y * w)
				// ypos = y * w
				buffer[n + (128 * 128)] = rgb - (rgb >>> 3) & 0xF8F8FF;
				buffer[n + (256 * 128)] = rgb - (rgb >>> 2) & 0xF8F8FF;
				buffer[n + (384 * 128)] = rgb - (rgb >>> 2) - (rgb >>> 3) & 0xF8F8FF;
			}
		}
		return buffer;
	}

	/**
	 * Generates an HSL to RGB lookup table, also known as <i>palette</i>.
	 *
	 * @param exponent the brightness on a 0.0 to 1.0 scale.
	 */
	public static final void generatePalette(double exponent) {
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
				rgb = adjustColorLightness(rgb, exponent);
				palette[off++] = rgb;
			}

			// updates the texture palette brightness
			for (int n = 0; n < 50; n++) {
				if (textures[n] != null) {
					int[] texturePalette = textures[n].palette;
					texturePalettes[n] = new int[texturePalette.length];

					for (int i = 0; i < texturePalette.length; i++) {
						texturePalettes[n][i] = adjustColorLightness(texturePalette[i], exponent);
					}
				}

				updateTexture(n);
			}
		}
	}

	/**
	 * Adjusts the input RGB's brightness.
	 *
	 * @param rgb the input int24_rgb.
	 * @param exponent the exponent.
	 * @return rgb^exponent.
	 */
	public static int adjustColorLightness(int rgb, double exponent) {
		double r = (double) (rgb >> 16) / 256.0;
		double g = (double) (rgb >> 8 & 0xff) / 256.0;
		double b = (double) (rgb & 0xff) / 256.0;
		r = Math.pow(r, exponent);
		g = Math.pow(g, exponent);
		b = Math.pow(b, exponent);
		return ((int) (r * 256.0) << 16) + ((int) (g * 256.0) << 8) + (int) (b * 256.0);
	}

	/**
	 * Fills a triangle using the gouraud shading technique.<br/><b>Warning:</b>
	 * Only interpolates the <i>lightness</i> channel of the provided colors for
	 * each point. That means you cannot select a different hue or saturation
	 * between points!
	 *
	 * @param xA first point x
	 * @param yA first point y
	 * @param xB second point x
	 * @param yB second point y
	 * @param xC third point x
	 * @param yC third point y
	 * @param colorA first point color in HSL format
	 * @param colorB second point color in HSL format
	 * @param colorC third point color in HSL format
	 */
	public static final void fillShadedTriangle(int xA, int yA, int xB, int yB, int xC, int yC, int colorA, int colorB, int colorC) {
		// All slopes are 16.16 fixed points
		// All light slopes are 17.15 fixed points
		int slopeAB = 0;
		int lightSlopeAB = 0;

		// What's going on here:
		// The slopes are being transformed into 16.16 or 17.15 fixed points.
		if (yB != yA) {
			slopeAB = ((xB - xA) << 16) / (yB - yA);
			lightSlopeAB = (colorB - colorA << 15) / (yB - yA);
		}

		int slopeBC = 0;
		int lightSlopeBC = 0;

		if (yC != yB) {
			slopeBC = ((xC - xB) << 16) / (yC - yB);
			lightSlopeBC = (colorC - colorB << 15) / (yC - yB);
		}

		int slopeCA = 0;
		int lightSlopeCA = 0;

		if (yC != yA) {
			slopeCA = ((xA - xC) << 16) / (yA - yC);
			lightSlopeCA = (colorA - colorC << 15) / (yA - yC);
		}

		if (yA <= yB && yA <= yC) {
			if (yA >= Canvas2D.bottom) {
				return;
			}

			if (yB > Canvas2D.bottom) {
				yB = Canvas2D.bottom;
			}

			if (yC > Canvas2D.bottom) {
				yC = Canvas2D.bottom;
			}

			if (yB < yC) {
				// transform into 16.16 fixed point
				xC = xA <<= 16;

				// transform into 17.15 fixed point
				colorC = colorA <<= 15;

				if (yA < 0) {
					xC -= slopeCA * yA;
					xA -= slopeAB * yA;
					colorC -= lightSlopeCA * yA;
					colorA -= lightSlopeAB * yA;
					yA = 0;
				}

				// transform into 16.16 fixed point
				xB <<= 16;

				// transform into 17.15 fixed point
				colorB <<= 15;

				if (yB < 0) {
					xB -= slopeBC * yB;
					colorB -= lightSlopeBC * yB;
					yB = 0;
				}

				if (yA != yB && slopeCA < slopeAB || yA == yB && slopeCA > slopeBC) {
					// yC is now the difference between B and C vertically
					yC -= yB;

					// yB is now the difference between A and B vertically
					yB -= yA;

					// yA is now our vertical offset.
					yA = offsets[yA];

					// while we have a vertical gap between A and B
					while (--yB >= 0) {
						// Notice the right shifts of 7
						// Those are transforming the 17.15 fixed points to 24.8! How exciting!
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);

						// approach xC to xA
						xC += slopeCA;
						colorC += lightSlopeCA;

						// approach xA to xB
						xA += slopeAB;
						colorA += lightSlopeAB;

						// move yA down a row of pixels.
						yA += Canvas2D.dstW;
					}

					// while we have a vertical gap between B and C
					while (--yC >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);

						xC += slopeCA;
						colorC += lightSlopeCA;

						xB += slopeBC;
						colorB += lightSlopeBC;

						yA += Canvas2D.dstW;
					}
				} else {
					yC -= yB;
					yB -= yA;
					yA = offsets[yA];

					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xC += slopeCA;
						xA += slopeAB;
						colorC += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Canvas2D.dstW;
					}

					while (--yC >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xC += slopeCA;
						xB += slopeBC;
						colorC += lightSlopeCA;
						colorB += lightSlopeBC;
						yA += Canvas2D.dstW;
					}
				}
			} else {
				xB = xA <<= 16;
				colorB = colorA <<= 15;

				if (yA < 0) {
					xB -= slopeCA * yA;
					xA -= slopeAB * yA;
					colorB -= lightSlopeCA * yA;
					colorA -= lightSlopeAB * yA;
					yA = 0;
				}

				xC <<= 16;
				colorC <<= 15;

				if (yC < 0) {
					xC -= slopeBC * yC;
					colorC -= lightSlopeBC * yC;
					yC = 0;
				}

				if (yA != yC && slopeCA < slopeAB || yA == yC && slopeBC > slopeAB) {
					yB -= yC;
					yC -= yA;
					yA = offsets[yA];

					while (--yC >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
						xB += slopeCA;
						xA += slopeAB;
						colorB += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Canvas2D.dstW;
					}

					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
						xC += slopeBC;
						xA += slopeAB;
						colorC += lightSlopeBC;
						colorA += lightSlopeAB;
						yA += Canvas2D.dstW;
					}
				} else {
					yB -= yC;
					yC -= yA;
					yA = offsets[yA];

					while (--yC >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
						xB += slopeCA;
						xA += slopeAB;
						colorB += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Canvas2D.dstW;
					}

					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yA, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xC += slopeBC;
						xA += slopeAB;
						colorC += lightSlopeBC;
						colorA += lightSlopeAB;
						yA += Canvas2D.dstW;
					}
				}
			}
		} else if (yB <= yC) {
			if (yB < Canvas2D.bottom) {
				if (yC > Canvas2D.bottom) {
					yC = Canvas2D.bottom;
				}

				if (yA > Canvas2D.bottom) {
					yA = Canvas2D.bottom;
				}

				if (yC < yA) {
					xA = xB <<= 16;
					colorA = colorB <<= 15;
					if (yB < 0) {
						xA -= slopeAB * yB;
						xB -= slopeBC * yB;
						colorA -= lightSlopeAB * yB;
						colorB -= lightSlopeBC * yB;
						yB = 0;
					}
					xC <<= 16;
					colorC <<= 15;
					if (yC < 0) {
						xC -= slopeCA * yC;
						colorC -= lightSlopeCA * yC;
						yC = 0;
					}
					if (yB != yC && slopeAB < slopeBC || yB == yC && slopeAB > slopeCA) {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
							xA += slopeAB;
							xB += slopeBC;
							colorA += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yA >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
							xA += slopeAB;
							xC += slopeCA;
							colorA += lightSlopeAB;
							colorC += lightSlopeCA;
							yB += Canvas2D.dstW;
						}
					} else {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
							xA += slopeAB;
							xB += slopeBC;
							colorA += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yA >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
							xA += slopeAB;
							xC += slopeCA;
							colorA += lightSlopeAB;
							colorC += lightSlopeCA;
							yB += Canvas2D.dstW;
						}
					}
				} else {
					xC = xB <<= 16;
					colorC = colorB <<= 15;
					if (yB < 0) {
						xC -= slopeAB * yB;
						xB -= slopeBC * yB;
						colorC -= lightSlopeAB * yB;
						colorB -= lightSlopeBC * yB;
						yB = 0;
					}
					xA <<= 16;
					colorA <<= 15;
					if (yA < 0) {
						xA -= slopeCA * yA;
						colorA -= lightSlopeCA * yA;
						yA = 0;
					}
					if (slopeAB < slopeBC) {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
							xC += slopeAB;
							xB += slopeBC;
							colorC += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yC >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
							xA += slopeCA;
							xB += slopeBC;
							colorA += lightSlopeCA;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
					} else {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
							xC += slopeAB;
							xB += slopeBC;
							colorC += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yC >= 0) {
							drawGradientScanline(Canvas2D.dst, yB, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
							xA += slopeCA;
							xB += slopeBC;
							colorA += lightSlopeCA;
							colorB += lightSlopeBC;
							yB += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (yC < Canvas2D.bottom) {
			if (yA > Canvas2D.bottom) {
				yA = Canvas2D.bottom;
			}
			if (yB > Canvas2D.bottom) {
				yB = Canvas2D.bottom;
			}
			if (yA < yB) {
				xB = xC <<= 16;
				colorB = colorC <<= 15;
				if (yC < 0) {
					xB -= slopeBC * yC;
					xC -= slopeCA * yC;
					colorB -= lightSlopeBC * yC;
					colorC -= lightSlopeCA * yC;
					yC = 0;
				}
				xA <<= 16;
				colorA <<= 15;
				if (yA < 0) {
					xA -= slopeAB * yA;
					colorA -= lightSlopeAB * yA;
					yA = 0;
				}
				if (slopeBC < slopeCA) {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xB += slopeBC;
						xC += slopeCA;
						colorB += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
						xB += slopeBC;
						xA += slopeAB;
						colorB += lightSlopeBC;
						colorA += lightSlopeAB;
						yC += Canvas2D.dstW;
					}
				} else {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
						xB += slopeBC;
						xC += slopeCA;
						colorB += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
						xB += slopeBC;
						xA += slopeAB;
						colorB += lightSlopeBC;
						colorA += lightSlopeAB;
						yC += Canvas2D.dstW;
					}
				}
			} else {
				xA = xC <<= 16;
				colorA = colorC <<= 15;
				if (yC < 0) {
					xA -= slopeBC * yC;
					xC -= slopeCA * yC;
					colorA -= lightSlopeBC * yC;
					colorC -= lightSlopeCA * yC;
					yC = 0;
				}
				xB <<= 16;
				colorB <<= 15;
				if (yB < 0) {
					xB -= slopeAB * yB;
					colorB -= lightSlopeAB * yB;
					yB = 0;
				}
				if (slopeBC < slopeCA) {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xA += slopeBC;
						xC += slopeCA;
						colorA += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yA >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xB += slopeAB;
						xC += slopeCA;
						colorB += lightSlopeAB;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
				} else {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
						xA += slopeBC;
						xC += slopeCA;
						colorA += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yA >= 0) {
						drawGradientScanline(Canvas2D.dst, yC, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
						xB += slopeAB;
						xC += slopeCA;
						colorB += lightSlopeAB;
						colorC += lightSlopeCA;
						yC += Canvas2D.dstW;
					}
				}
			}
		}
	}

	/**
	 * Draws a scanline and linearly translates the lightness.
	 *
	 * @param dst the destination.
	 * @param off the initial offset.
	 * @param rgb the INT24_RGB.
	 * @param length the length.
	 * @param xA the start x.
	 * @param xB the end x.
	 * @param colorA the start color. (24.8)
	 * @param colorB the end color. (24.8)
	 */
	public static final void drawGradientScanline(int[] dst, int off, int rgb, int length, int xA, int xB, int colorA, int colorB) {
		if (texturedShading) {
			int lightnessSlope;

			if (verifyBounds) {
				if (xB - xA > 3) {
					// notice no fixed point transformations here?
					// that's because they're still fixed points!
					// At this point, colorA and colorB are 24.8 fixed points. :)
					lightnessSlope = (colorB - colorA) / (xB - xA);
				} else {
					lightnessSlope = 0;
				}

				if (xB > Canvas2D.dstXBound) {
					xB = Canvas2D.dstXBound;
				}

				// clip off screen part and recalculate initial color
				if (xA < 0) {
					colorA -= xA * lightnessSlope;
					xA = 0;
				}

				// if we start ahead of our end point, don't do anything.
				if (xA >= xB) {
					return;
				}

				off += xA;
				length = xB - xA >> 2;
				lightnessSlope <<= 2;
			} else {
				if (xA >= xB) {
					return;
				}

				off += xA;
				length = xB - xA >> 2;

				if (length > 0) {
					lightnessSlope = (colorB - colorA) * oneOverFixed1715[length] >> 15;
				} else {
					lightnessSlope = 0;
				}
			}

			if (alpha == 0) {
				while (--length >= 0) {
					rgb = palette[colorA >> 8];
					colorA += lightnessSlope;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
				}

				length = xB - xA & 0x3;

				if (length > 0) {
					rgb = palette[colorA >> 8];
					do {
						dst[off++] = rgb;
					} while (--length > 0);
				}
			} else {
				int a0 = alpha;
				int a1 = 256 - alpha;

				while (--length >= 0) {
					rgb = palette[colorA >> 8];
					colorA += lightnessSlope;
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

				length = xB - xA & 0x3;

				if (length > 0) {
					rgb = palette[colorA >> 8];
					rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));
					do {
						dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
						off++;
					} while (--length > 0);
				}
			}
		} else if (xA < xB) {
			int lightnessSlope = (colorB - colorA) / (xB - xA);

			if (verifyBounds) {
				if (xB > Canvas2D.dstXBound) {
					xB = Canvas2D.dstXBound;
				}

				if (xA < 0) {
					colorA -= xA * lightnessSlope;
					xA = 0;
				}

				if (xA >= xB) {
					return;
				}
			}

			off += xA;
			length = xB - xA;

			if (alpha == 0) {
				do {
					dst[off++] = palette[colorA >> 8];
					colorA += lightnessSlope;
				} while (--length > 0);
			} else {
				int a0 = alpha;
				int a1 = 256 - alpha;
				do {
					rgb = palette[colorA >> 8];
					colorA += lightnessSlope;
					rgb = (((rgb & 0xFF00FF) * a1 >> 8 & 0xFF00FF) + ((rgb & 0xFF00) * a1 >> 8 & 0xFF00));
					dst[off++] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
				} while (--length > 0);
			}
		}
	}

	/**
	 * Draws a triangle.
	 *
	 * @param xA the first point x.
	 * @param yA the first point y.
	 * @param xB the second point x.
	 * @param yB the second point y.
	 * @param xC the third point x.
	 * @param yC the third point y.
	 * @param color the color.
	 */
	public static final void drawTriangle(int xA, int yA, int xB, int yB, int xC, int yC, int color) {
		drawLine(xA, yA, xB, yB, color);
		drawLine(xB, yB, xC, yC, color);
		drawLine(xC, yC, xA, yA, color);
	}

	/**
	 * Fills a triangle.
	 *
	 * @param xA first point x
	 * @param yA first point y
	 * @param xB second point x
	 * @param yB second point y
	 * @param xC third point x
	 * @param yC third point y
	 * @param color the color of the triangle. (in INT24_RGB format)
	 */
	public static final void fillTriangle(int xA, int yA, int xB, int yB, int xC, int yC, int color) {
		int slopeAB = 0;

		if (yB != yA) {
			slopeAB = (xB - xA << 16) / (yB - yA);
		}

		int slopeBC = 0;

		if (yC != yB) {
			slopeBC = (xC - xB << 16) / (yC - yB);
		}

		int slopeCA = 0;

		if (yC != yA) {
			slopeCA = (xA - xC << 16) / (yA - yC);
		}

		if (yA <= yB && yA <= yC) {
			if (yA < Canvas2D.bottom) {
				if (yB > Canvas2D.bottom) {
					yB = Canvas2D.bottom;
				}

				if (yC > Canvas2D.bottom) {
					yC = Canvas2D.bottom;
				}

				if (yB < yC) {
					xC = xA <<= 16;

					if (yA < 0) {
						xC -= slopeCA * yA;
						xA -= slopeAB * yA;
						yA = 0;
					}

					xB <<= 16;

					if (yB < 0) {
						xB -= slopeBC * yB;
						yB = 0;
					}

					if (yA != yB && slopeCA < slopeAB || yA == yB && slopeCA > slopeBC) {
						yC -= yB;
						yB -= yA;
						yA = offsets[yA];

						while (--yB >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xC >> 16, xA >> 16);
							xC += slopeCA;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}

						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xC >> 16, xB >> 16);
							xC += slopeCA;
							xB += slopeBC;
							yA += Canvas2D.dstW;
						}
					} else {
						yC -= yB;
						yB -= yA;
						yA = offsets[yA];
						while (--yB >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xA >> 16, xC >> 16);
							xC += slopeCA;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xB >> 16, xC >> 16);
							xC += slopeCA;
							xB += slopeBC;
							yA += Canvas2D.dstW;
						}
					}
				} else {
					xB = xA <<= 16;
					if (yA < 0) {
						xB -= slopeCA * yA;
						xA -= slopeAB * yA;
						yA = 0;
					}
					xC <<= 16;
					if (yC < 0) {
						xC -= slopeBC * yC;
						yC = 0;
					}
					if (yA != yC && slopeCA < slopeAB || yA == yC && slopeBC > slopeAB) {
						yB -= yC;
						yC -= yA;
						yA = offsets[yA];
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xB >> 16, xA >> 16);
							xB += slopeCA;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}
						while (--yB >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xC >> 16, xA >> 16);
							xC += slopeBC;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}
					} else {
						yB -= yC;
						yC -= yA;
						yA = offsets[yA];
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xA >> 16, xB >> 16);
							xB += slopeCA;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}
						while (--yB >= 0) {
							drawScanline(Canvas2D.dst, yA, color, 0, xA >> 16, xC >> 16);
							xC += slopeBC;
							xA += slopeAB;
							yA += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (yB <= yC) {
			if (yB < Canvas2D.bottom) {
				if (yC > Canvas2D.bottom) {
					yC = Canvas2D.bottom;
				}

				if (yA > Canvas2D.bottom) {
					yA = Canvas2D.bottom;
				}

				if (yC < yA) {
					xA = xB <<= 16;

					if (yB < 0) {
						xA -= slopeAB * yB;
						xB -= slopeBC * yB;
						yB = 0;
					}

					xC <<= 16;

					if (yC < 0) {
						xC -= slopeCA * yC;
						yC = 0;
					}

					if (yB != yC && slopeAB < slopeBC || yB == yC && slopeAB > slopeCA) {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xA >> 16, xB >> 16);
							xA += slopeAB;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yA >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xA >> 16, xC >> 16);
							xA += slopeAB;
							xC += slopeCA;
							yB += Canvas2D.dstW;
						}
					} else {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xB >> 16, xA >> 16);
							xA += slopeAB;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yA >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xC >> 16, xA >> 16);
							xA += slopeAB;
							xC += slopeCA;
							yB += Canvas2D.dstW;
						}
					}
				} else {
					xC = xB <<= 16;
					if (yB < 0) {
						xC -= slopeAB * yB;
						xB -= slopeBC * yB;
						yB = 0;
					}
					xA <<= 16;
					if (yA < 0) {
						xA -= slopeCA * yA;
						yA = 0;
					}
					if (slopeAB < slopeBC) {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xC >> 16, xB >> 16);
							xC += slopeAB;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xA >> 16, xB >> 16);
							xA += slopeCA;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
					} else {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xB >> 16, xC >> 16);
							xC += slopeAB;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
						while (--yC >= 0) {
							drawScanline(Canvas2D.dst, yB, color, 0, xB >> 16, xA >> 16);
							xA += slopeCA;
							xB += slopeBC;
							yB += Canvas2D.dstW;
						}
					}
				}
			}
		} else if (yC < Canvas2D.bottom) {
			if (yA > Canvas2D.bottom) {
				yA = Canvas2D.bottom;
			}
			if (yB > Canvas2D.bottom) {
				yB = Canvas2D.bottom;
			}
			if (yA < yB) {
				xB = xC <<= 16;
				if (yC < 0) {
					xB -= slopeBC * yC;
					xC -= slopeCA * yC;
					yC = 0;
				}
				xA <<= 16;
				if (yA < 0) {
					xA -= slopeAB * yA;
					yA = 0;
				}
				if (slopeBC < slopeCA) {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xB >> 16, xC >> 16);
						xB += slopeBC;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yB >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xB >> 16, xA >> 16);
						xB += slopeBC;
						xA += slopeAB;
						yC += Canvas2D.dstW;
					}
				} else {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xC >> 16, xB >> 16);
						xB += slopeBC;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yB >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xA >> 16, xB >> 16);
						xB += slopeBC;
						xA += slopeAB;
						yC += Canvas2D.dstW;
					}
				}
			} else {
				xA = xC <<= 16;
				if (yC < 0) {
					xA -= slopeBC * yC;
					xC -= slopeCA * yC;
					yC = 0;
				}
				xB <<= 16;
				if (yB < 0) {
					xB -= slopeAB * yB;
					yB = 0;
				}
				if (slopeBC < slopeCA) {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xA >> 16, xC >> 16);
						xA += slopeBC;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yA >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xB >> 16, xC >> 16);
						xB += slopeAB;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
				} else {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xC >> 16, xA >> 16);
						xA += slopeBC;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
					while (--yA >= 0) {
						drawScanline(Canvas2D.dst, yC, color, 0, xC >> 16, xB >> 16);
						xB += slopeAB;
						xC += slopeCA;
						yC += Canvas2D.dstW;
					}
				}
			}
		}
	}

	/**
	 * Draws a scanline.
	 *
	 * @param dst the destination.
	 * @param off the initial offset.
	 * @param rgb the color.
	 * @param length the length.
	 * @param xA the start x.
	 * @param xB the end x.
	 */
	public static final void drawScanline(int[] dst, int off, int rgb, int length, int xA, int xB) {
		if (verifyBounds) {
			if (xB > Canvas2D.dstXBound) {
				xB = Canvas2D.dstXBound;
			}
			if (xA < 0) {
				xA = 0;
			}
		}

		if (xA < xB) {
			off += xA;
			length = xB - xA >> 2;

			if (alpha == 0) {
				while (--length >= 0) {
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
					dst[off++] = rgb;
				}
				length = xB - xA & 0x3;

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

				length = xB - xA & 0x3;

				while (--length >= 0) {
					dst[off] = (rgb + ((dst[off] & 0xFF00FF) * a0 >> 8 & 0xFF00FF) + ((dst[off] & 0xFF00) * a0 >> 8 & 0xFF00));
					off++;
				}
			}
		}
	}

	public static final void fillTexturedTriangle(int yA, int yB, int yC, int xA, int xB, int xC, int lA, int lB, int lC, int xP, int xM, int xN, int yP, int yM, int yN, int zP, int zM, int zN, int textureIndex) {
		// INT24_RGB array
		int[] texels = getTexels(textureIndex);

		opaque = !textureHasTransparency[textureIndex];

		// xM becomes the difference between xM and xP
		xM = xP - xM;

		// yM becomes the difference between yM and yP
		yM = yP - yM;

		// zM becomes the difference between zM and zP
		zM = zP - zM;

		// xN becomes the difference between xP and xN
		xN -= xP;

		// yN becomes the difference between yP and yN
		yN -= yP;

		// zN becomes the difference between zP and zN
		zN -= zP;

		// TODO: Learn what kind of math this is. Has something to do with vectors.
		int oA = ((zN * xP) - (xN * zP)) << 5;
		int hA = ((yN * zP) - (zN * yP)) << 8;
		int vA = ((xN * yP) - (yN * xP)) << 14;

		int oB = ((zM * xP) - (xM * zP)) << 5;
		int hB = ((yM * zP) - (zM * yP)) << 8;
		int vB = ((xM * yP) - (yM * xP)) << 14;

		int oC = ((xM * zN) - (zM * xN)) << 5;
		int hC = ((zM * yN) - (yM * zN)) << 8;
		int vC = ((yM * xN) - (xM * yN)) << 14;

		int slopeAB = 0;
		int lightSlopeAB = 0;

		if (yB != yA) {
			slopeAB = (xB - xA << 16) / (yB - yA);
			lightSlopeAB = (lB - lA << 16) / (yB - yA);
		}

		int slopeBC = 0;
		int lightSlopeBC = 0;

		if (yC != yB) {
			slopeBC = (xC - xB << 16) / (yC - yB);
			lightSlopeBC = (lC - lB << 16) / (yC - yB);
		}

		int slopeCA = 0;
		int lightSlopeCA = 0;

		if (yC != yA) {
			slopeCA = (xA - xC << 16) / (yA - yC);
			lightSlopeCA = (lA - lC << 16) / (yA - yC);
		}

		if (yA <= yB && yA <= yC) {
			if (yA < Canvas2D.bottom) {
				if (yB > Canvas2D.bottom) {
					yB = Canvas2D.bottom;
				}

				if (yC > Canvas2D.bottom) {
					yC = Canvas2D.bottom;
				}

				if (yB < yC) {
					xC = xA <<= 16;
					lC = lA <<= 16;

					if (yA < 0) {
						xC -= slopeCA * yA;
						xA -= slopeAB * yA;

						lC -= lightSlopeCA * yA;
						lA -= lightSlopeAB * yA;

						yA = 0;
					}

					xB <<= 16;
					lB <<= 16;

					if (yB < 0) {
						xB -= slopeBC * yB;
						lB -= lightSlopeBC * yB;
						yB = 0;
					}

					int offsetY = yA - centerY;
					vA += oA * offsetY;
					vB += oB * offsetY;
					vC += oC * offsetY;

					if (yA != yB && slopeCA < slopeAB || yA == yB && slopeCA > slopeBC) {
						yC -= yB;
						yB -= yA;
						yA = offsets[yA];

						while (--yB >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xC >> 16, xA >> 16, lC >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeCA;
							xA += slopeAB;

							lC += lightSlopeCA;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xC >> 16, xB >> 16, lC >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeCA;
							xB += slopeBC;

							lC += lightSlopeCA;
							lB += lightSlopeBC;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					} else {
						yC -= yB;
						yB -= yA;
						yA = offsets[yA];

						while (--yB >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xA >> 16, xC >> 16, lA >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeCA;
							xA += slopeAB;

							lC += lightSlopeCA;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xB >> 16, xC >> 16, lB >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeCA;
							xB += slopeBC;

							lC += lightSlopeCA;
							lB += lightSlopeBC;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					}
				} else {
					xB = xA <<= 16;
					lB = lA <<= 16;

					if (yA < 0) {
						xB -= slopeCA * yA;
						xA -= slopeAB * yA;

						lB -= lightSlopeCA * yA;
						lA -= lightSlopeAB * yA;

						yA = 0;
					}

					xC <<= 16;
					lC <<= 16;

					if (yC < 0) {
						xC -= slopeBC * yC;
						lC -= lightSlopeBC * yC;
						yC = 0;
					}

					int offsetY = yA - centerY;
					vA += oA * offsetY;
					vB += oB * offsetY;
					vC += oC * offsetY;

					if (yA != yC && slopeCA < slopeAB || yA == yC && slopeBC > slopeAB) {
						yB -= yC;
						yC -= yA;
						yA = offsets[yA];

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xB >> 16, xA >> 16, lB >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xB += slopeCA;
							xA += slopeAB;

							lB += lightSlopeCA;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yB >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xC >> 16, xA >> 16, lC >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeBC;
							xA += slopeAB;

							lC += lightSlopeBC;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					} else {
						yB -= yC;
						yC -= yA;
						yA = offsets[yA];

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xA >> 16, xB >> 16, lA >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
							xB += slopeCA;
							xA += slopeAB;

							lB += lightSlopeCA;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yB >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yA, xA >> 16, xC >> 16, lA >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeBC;
							xA += slopeAB;

							lC += lightSlopeBC;
							lA += lightSlopeAB;

							yA += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					}
				}
			}
		} else if (yB <= yC) {
			if (yB < Canvas2D.bottom) {
				if (yC > Canvas2D.bottom) {
					yC = Canvas2D.bottom;
				}

				if (yA > Canvas2D.bottom) {
					yA = Canvas2D.bottom;
				}

				if (yC < yA) {
					xA = xB <<= 16;
					lA = lB <<= 16;

					if (yB < 0) {
						xA -= slopeAB * yB;
						xB -= slopeBC * yB;

						lA -= lightSlopeAB * yB;
						lB -= lightSlopeBC * yB;

						yB = 0;
					}

					xC <<= 16;
					lC <<= 16;

					if (yC < 0) {
						xC -= slopeCA * yC;
						lC -= lightSlopeCA * yC;
						yC = 0;
					}

					int offsetY = yB - centerY;
					vA += oA * offsetY;
					vB += oB * offsetY;
					vC += oC * offsetY;

					if (yB != yC && slopeAB < slopeBC || yB == yC && slopeAB > slopeCA) {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xA >> 16, xB >> 16, lA >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeAB;
							xB += slopeBC;
							lA += lightSlopeAB;
							lB += lightSlopeBC;
							yB += Canvas2D.dstW;
							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yA >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xA >> 16, xC >> 16, lA >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeAB;
							xC += slopeCA;
							lA += lightSlopeAB;
							lC += lightSlopeCA;
							yB += Canvas2D.dstW;
							vA += oA;
							vB += oB;
							vC += oC;
						}
					} else {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xB >> 16, xA >> 16, lB >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeAB;
							xB += slopeBC;

							lA += lightSlopeAB;
							lB += lightSlopeBC;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yA >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xC >> 16, xA >> 16, lC >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeAB;
							xC += slopeCA;

							lA += lightSlopeAB;
							lC += lightSlopeCA;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					}
				} else {
					xC = xB <<= 16;
					lC = lB <<= 16;

					if (yB < 0) {
						xC -= slopeAB * yB;
						xB -= slopeBC * yB;

						lC -= lightSlopeAB * yB;
						lB -= lightSlopeBC * yB;

						yB = 0;
					}

					xA <<= 16;
					lA <<= 16;

					if (yA < 0) {
						xA -= slopeCA * yA;
						lA -= lightSlopeCA * yA;
						yA = 0;
					}

					int offsetY = yB - centerY;
					vA += oA * offsetY;
					vB += oB * offsetY;
					vC += oC * offsetY;

					if (slopeAB < slopeBC) {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];

						while (--yA >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xC >> 16, xB >> 16, lC >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeAB;
							xB += slopeBC;

							lC += lightSlopeAB;
							lB += lightSlopeBC;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xA >> 16, xB >> 16, lA >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeCA;
							xB += slopeBC;

							lA += lightSlopeCA;
							lB += lightSlopeBC;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					} else {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];

						while (--yA >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xB >> 16, xC >> 16, lB >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
							xC += slopeAB;
							xB += slopeBC;

							lC += lightSlopeAB;
							lB += lightSlopeBC;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}

						while (--yC >= 0) {
							drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yB, xB >> 16, xA >> 16, lB >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
							xA += slopeCA;
							xB += slopeBC;

							lA += lightSlopeCA;
							lB += lightSlopeBC;

							yB += Canvas2D.dstW;

							vA += oA;
							vB += oB;
							vC += oC;
						}
					}
				}
			}
		} else if (yC < Canvas2D.bottom) {
			if (yA > Canvas2D.bottom) {
				yA = Canvas2D.bottom;
			}

			if (yB > Canvas2D.bottom) {
				yB = Canvas2D.bottom;
			}

			if (yA < yB) {
				xB = xC <<= 16;
				lB = lC <<= 16;

				if (yC < 0) {
					xB -= slopeBC * yC;
					xC -= slopeCA * yC;

					lB -= lightSlopeBC * yC;
					lC -= lightSlopeCA * yC;

					yC = 0;
				}

				xA <<= 16;
				lA <<= 16;

				if (yA < 0) {
					xA -= slopeAB * yA;
					lA -= lightSlopeAB * yA;
					yA = 0;
				}

				int offsetY = yC - centerY;
				vA += oA * offsetY;
				vB += oB * offsetY;
				vC += oC * offsetY;

				if (slopeBC < slopeCA) {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];

					while (--yA >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xB >> 16, xC >> 16, lB >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeBC;
						xC += slopeCA;

						lB += lightSlopeBC;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}

					while (--yB >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xB >> 16, xA >> 16, lB >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeBC;
						xA += slopeAB;

						lB += lightSlopeBC;
						lA += lightSlopeAB;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}
				} else {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];

					while (--yA >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xC >> 16, xB >> 16, lC >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeBC;
						xC += slopeCA;

						lB += lightSlopeBC;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}

					while (--yB >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xA >> 16, xB >> 16, lA >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeBC;
						xA += slopeAB;

						lB += lightSlopeBC;
						lA += lightSlopeAB;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}
				}
			} else {
				xA = xC <<= 16;
				lA = lC <<= 16;

				if (yC < 0) {
					xA -= slopeBC * yC;
					xC -= slopeCA * yC;

					lA -= lightSlopeBC * yC;
					lC -= lightSlopeCA * yC;

					yC = 0;
				}

				xB <<= 16;
				lB <<= 16;

				if (yB < 0) {
					xB -= slopeAB * yB;
					lB -= lightSlopeAB * yB;
					yB = 0;
				}

				int offsetY = yC - centerY;
				vA += oA * offsetY;
				vB += oB * offsetY;
				vC += oC * offsetY;

				if (slopeBC < slopeCA) {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];

					while (--yB >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xA >> 16, xC >> 16, lA >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
						xA += slopeBC;
						xC += slopeCA;

						lA += lightSlopeBC;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}

					while (--yA >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xB >> 16, xC >> 16, lB >> 8, lC >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeAB;
						xC += slopeCA;

						lB += lightSlopeAB;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}
				} else {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];

					while (--yB >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xC >> 16, xA >> 16, lC >> 8, lA >> 8, vA, vB, vC, hA, hB, hC);
						xA += slopeBC;
						xC += slopeCA;

						lA += lightSlopeBC;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}

					while (--yA >= 0) {
						drawTexturedScanline(Canvas2D.dst, texels, 0, 0, yC, xC >> 16, xB >> 16, lC >> 8, lB >> 8, vA, vB, vC, hA, hB, hC);
						xB += slopeAB;
						xC += slopeCA;

						lB += lightSlopeAB;
						lC += lightSlopeCA;

						yC += Canvas2D.dstW;

						vA += oA;
						vB += oB;
						vC += oC;
					}
				}
			}
		}
	}

	/**
	 * Draws a textured scanline.
	 *
	 * @param dst the destination.
	 * @param texels the source texels.
	 * @param u1 the initial u.
	 * @param v1 the initial v.
	 * @param off the initial offset.
	 * @param xA the start x.
	 * @param xB the end x.
	 * @param lA the start lightness.
	 * @param lB the end lightness.
	 * @param vA the magic vA.
	 * @param vB the magic vB.
	 * @param vC the magic vC.
	 * @param hA the magic hA.
	 * @param hB the magic hB.
	 * @param hC the magic hC.
	 */
	public static final void drawTexturedScanline(int[] dst, int[] texels, int u1, int v1, int off, int xA, int xB, int lA, int lB, int vA, int vB, int vC, int hA, int hB, int hC) {
		if (xA >= xB) {
			return;
		}

		int length;
		int lightnessSlope;

		if (verifyBounds) {
			lightnessSlope = (lB - lA) / (xB - xA);

			if (xB > Canvas2D.dstXBound) {
				xB = Canvas2D.dstXBound;
			}

			if (xA < 0) {
				lA -= xA * lightnessSlope;
				xA = 0;
			}

			if (xA >= xB) {
				return;
			}

			length = xB - xA >> 3;
			lightnessSlope <<= 12;
			lA <<= 9;
		} else {
			if (xB - xA > 7) {
				length = xB - xA >> 3;
				lightnessSlope = (lB - lA) * oneOverFixed1715[length] >> 6;
			} else {
				length = 0;
				lightnessSlope = 0;
			}

			lA <<= 9;
		}

		off += xA;

		if (lowmemory) {
			int u2 = 0;
			int v2 = 0;
			int deltaCenterX = xA - centerX;

			vA += (hA >> 3) * deltaCenterX;
			vB += (hB >> 3) * deltaCenterX;
			vC += (hC >> 3) * deltaCenterX;

			int realC = vC >> 12;

			if (realC != 0) {
				u1 = vA / realC;
				v1 = vB / realC;

				if (u1 < 0) {
					u1 = 0;
				} else if (u1 > 4032) {
					u1 = 4032;
				}
			}

			vA += hA;
			vB += hB;
			vC += hC;
			realC = vC >> 12;

			if (realC != 0) {
				u2 = vA / realC;
				v2 = vB / realC;

				if (u2 < 7) {
					u2 = 7;
				} else if (u2 > 4032) {
					u2 = 4032;
				}
			}

			int uSlope = u2 - u1 >> 3;
			int vSlope = v2 - v1 >> 3;
			u1 += (lA & 0x60_00_00) >> 3;
			int i_155_ = lA >> 23;

			if (opaque) {
				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;

					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 = u2;
					v1 = v2;

					vA += hA;
					vB += hB;
					vC += hC;
					realC = vC >> 12;

					if (realC != 0) {
						u2 = vA / realC;
						v2 = vB / realC;
						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 4032) {
							u2 = 4032;
						}
					}

					uSlope = u2 - u1 >> 3;
					vSlope = v2 - v1 >> 3;
					lA += lightnessSlope;
					u1 += (lA & 0x600000) >> 3;
					i_155_ = lA >> 23;
				}
				length = xB - xA & 0x7;
				while (length-- > 0) {
					dst[off++] = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_;
					u1 += uSlope;
					v1 += vSlope;
				}
			} else {
				// ignore pure black texels

				while (length-- > 0) {
					int rgb;
					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 += uSlope;
					v1 += vSlope;

					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
						dst[off] = rgb;
					}
					off++;
					u1 = u2;
					v1 = v2;

					vA += hA;
					vB += hB;
					vC += hC;
					realC = vC >> 12;

					if (realC != 0) {
						u2 = vA / realC;
						v2 = vB / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 4032) {
							u2 = 4032;
						}
					}

					uSlope = u2 - u1 >> 3;
					vSlope = v2 - v1 >> 3;
					lA += lightnessSlope;
					u1 += (lA & 0x600000) >> 3;
					i_155_ = lA >> 23;
				}

				length = xB - xA & 0x7;

				while (length-- > 0) {
					int rgb;
					if ((rgb = texels[(v1 & 0b1111_1100_0000) + (u1 >> 6)] >>> i_155_) != 0) {
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
			int startX = xA - centerX;

			vA += (hA >> 3) * startX;
			vB += (hB >> 3) * startX;
			vC += (hC >> 3) * startX;
			int realC = vC >> 14;

			if (realC != 0) {
				u1 = vA / realC;
				v1 = vB / realC;

				if (u1 < 0) {
					u1 = 0;
				} else if (u1 > 16256) {
					u1 = 16256;
				}
			}

			vA += hA;
			vB += hB;
			vC += hC;
			realC = vC >> 14;

			if (realC != 0) {
				u2 = vA / realC;
				v2 = vB / realC;

				if (u2 < 7) {
					u2 = 7;
				} else if (u2 > 16256) {
					u2 = 16256;
				}
			}

			int deltaU = u2 - u1 >> 3;
			int deltaV = v2 - v1 >> 3;
			u1 += lA & 0x600000;
			int lightness = lA >> 23;

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

					vA += hA;
					vB += hB;
					vC += hC;
					realC = vC >> 14;

					if (realC != 0) {
						u2 = vA / realC;
						v2 = vB / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 16256) {
							u2 = 16256;
						}
					}

					deltaU = u2 - u1 >> 3;
					deltaV = v2 - v1 >> 3;
					lA += lightnessSlope;
					u1 += lA & 0x600000;
					lightness = lA >> 23;
				}

				length = xB - xA & 0x7;

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

					vA += hA;
					vB += hB;
					vC += hC;
					realC = (vC >> 14);

					if (realC != 0) {
						u2 = vA / realC;
						v2 = vB / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > 16256) {
							u2 = 16256;
						}
					}
					deltaU = u2 - u1 >> 3;
					deltaV = v2 - v1 >> 3;
					lA += lightnessSlope;
					u1 += lA & 0x600000;
					lightness = lA >> 23;
				}

				length = xB - xA & 0x7;

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

	static {
		for (int i = 1; i < 512; i++) {
			oneOverFixed1715[i] = 32768 / i;
		}

		for (int i = 1; i < 2048; i++) {
			oneOverFixed1616[i] = 65536 / i;
		}

		for (int i = 0; i < 2048; i++) {
			sin[i] = (int) (65536.0 * Math.sin((double) i * 0.0030679615));
			cos[i] = (int) (65536.0 * Math.cos((double) i * 0.0030679615));
		}

		textures = new IndexedBitmap[50];
		textureHasTransparency = new boolean[50];
		textureColors = new int[50];
		texelBuffer1 = new int[50][];
		textureCycles = new int[50];
		palette = new int[65536];
		texturePalettes = new int[50][];
	}
}
