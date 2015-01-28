package com.runescape;

import java.util.logging.*;

public class Graphics3D extends Graphics2D {

	private static final Logger logger = Logger.getLogger(Graphics3D.class.getName());

	/**
	 * Whether to use less intensive procedures and memory storage.
	 */
	public static boolean lowmemory = true;

	/**
	 * Set to true when a point on a triangle is off screen. Used to clip
	 * interpolation variables in case they go off screen.
	 */
	public static boolean testX;

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
	 * A lookup table for 17.15 fixed point fractions.
	 */
	public static int[] oneOverFixed1715 = new int[512];

	/**
	 * A lookup table for 16.16 fixed point fractions.
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
	 * Stores RGB values that can be looked up with an HSL
	 * value:<br/><code>(hue << 10) | (saturation << 7) | lightness</code>
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
	 * {@link Graphics2D}.
	 *
	 * @return the int[] of y offsets.
	 */
	public static final int[] prepareOffsets() {
		offsets = new int[Graphics2D.targetHeight];
		for (int y = 0; y < Graphics2D.targetHeight; y++) {
			offsets[y] = Graphics2D.targetWidth * y;
		}
		centerX = Graphics2D.targetWidth / 2;
		centerY = Graphics2D.targetHeight / 2;
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
				buffer[n] = texturePalette[texture.data[n]];

				// allow space to divide channels (loses some red and green)
				buffer[n] &= 0b1111_1000_1111_1000_1111_1111;

				int rgb = buffer[n];

				if (rgb == 0) {
					textureHasTransparency[textureIndex] = true;
				}

				// darker
				buffer[n + (64 * 64)] = (rgb - (rgb >>> 3)) & 0xF8F8FF;

				// darker!
				buffer[n + (64 * 128)] = (rgb - (rgb >>> 2)) & 0xF8F8FF;

				// and darker!
				buffer[n + (64 * 192)] = (rgb - (rgb >>> 2) - (rgb >>> 3)) & 0xF8F8FF;
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
				// allow space to divide channels (loses some red and green)
				buffer[n] &= 0b1111_1000_1111_1000_1111_1111;

				int rgb = buffer[n];

				if (rgb == 0) {
					textureHasTransparency[textureIndex] = true;
				}

				// darker
				buffer[n + (128 * 128)] = (rgb - (rgb >>> 3)) & 0xF8F8FF;

				//darker!
				buffer[n + (256 * 128)] = (rgb - (rgb >>> 2)) & 0xF8F8FF;

				//and darker!
				buffer[n + (384 * 128)] = (rgb - (rgb >>> 2) - (rgb >>> 3)) & 0xF8F8FF;
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
	 * @param xA first point x.
	 * @param yA first point y.
	 * @param xB second point x.
	 * @param yB second point y.
	 * @param xC third point x.
	 * @param yC third point y.
	 * @param colorA first point color in HSL format.
	 * @param colorB second point color in HSL format.
	 * @param colorC third point color in HSL format.
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
			if (yA >= Graphics2D.bottom) {
				return;
			}

			if (yB > Graphics2D.bottom) {
				yB = Graphics2D.bottom;
			}

			if (yC > Graphics2D.bottom) {
				yC = Graphics2D.bottom;
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
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);

						// approach xC to xA
						xC += slopeCA;
						colorC += lightSlopeCA;

						// approach xA to xB
						xA += slopeAB;
						colorA += lightSlopeAB;

						// move yA down a row of pixels.
						yA += Graphics2D.targetWidth;
					}

					// while we have a vertical gap between B and C
					while (--yC >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);

						xC += slopeCA;
						colorC += lightSlopeCA;

						xB += slopeBC;
						colorB += lightSlopeBC;

						yA += Graphics2D.targetWidth;
					}
				} else {
					yC -= yB;
					yB -= yA;
					yA = offsets[yA];

					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xC += slopeCA;
						xA += slopeAB;
						colorC += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Graphics2D.targetWidth;
					}

					while (--yC >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xC += slopeCA;
						xB += slopeBC;
						colorC += lightSlopeCA;
						colorB += lightSlopeBC;
						yA += Graphics2D.targetWidth;
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
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
						xB += slopeCA;
						xA += slopeAB;
						colorB += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Graphics2D.targetWidth;
					}

					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
						xC += slopeBC;
						xA += slopeAB;
						colorC += lightSlopeBC;
						colorA += lightSlopeAB;
						yA += Graphics2D.targetWidth;
					}
				} else {
					yB -= yC;
					yC -= yA;
					yA = offsets[yA];

					while (--yC >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
						xB += slopeCA;
						xA += slopeAB;
						colorB += lightSlopeCA;
						colorA += lightSlopeAB;
						yA += Graphics2D.targetWidth;
					}

					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yA, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xC += slopeBC;
						xA += slopeAB;
						colorC += lightSlopeBC;
						colorA += lightSlopeAB;
						yA += Graphics2D.targetWidth;
					}
				}
			}
		} else if (yB <= yC) {
			if (yB < Graphics2D.bottom) {
				if (yC > Graphics2D.bottom) {
					yC = Graphics2D.bottom;
				}

				if (yA > Graphics2D.bottom) {
					yA = Graphics2D.bottom;
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
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
							xA += slopeAB;
							xB += slopeBC;
							colorA += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yA >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
							xA += slopeAB;
							xC += slopeCA;
							colorA += lightSlopeAB;
							colorC += lightSlopeCA;
							yB += Graphics2D.targetWidth;
						}
					} else {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
							xA += slopeAB;
							xB += slopeBC;
							colorA += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yA >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
							xA += slopeAB;
							xC += slopeCA;
							colorA += lightSlopeAB;
							colorC += lightSlopeCA;
							yB += Graphics2D.targetWidth;
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
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
							xC += slopeAB;
							xB += slopeBC;
							colorC += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yC >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
							xA += slopeCA;
							xB += slopeBC;
							colorA += lightSlopeCA;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
					} else {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
							xC += slopeAB;
							xB += slopeBC;
							colorC += lightSlopeAB;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yC >= 0) {
							drawGradientScanline(Graphics2D.target, yB, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
							xA += slopeCA;
							xB += slopeBC;
							colorA += lightSlopeCA;
							colorB += lightSlopeBC;
							yB += Graphics2D.targetWidth;
						}
					}
				}
			}
		} else if (yC < Graphics2D.bottom) {
			if (yA > Graphics2D.bottom) {
				yA = Graphics2D.bottom;
			}
			if (yB > Graphics2D.bottom) {
				yB = Graphics2D.bottom;
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
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xB += slopeBC;
						xC += slopeCA;
						colorB += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xB >> 16, xA >> 16, colorB >> 7, colorA >> 7);
						xB += slopeBC;
						xA += slopeAB;
						colorB += lightSlopeBC;
						colorA += lightSlopeAB;
						yC += Graphics2D.targetWidth;
					}
				} else {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
						xB += slopeBC;
						xC += slopeCA;
						colorB += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xA >> 16, xB >> 16, colorA >> 7, colorB >> 7);
						xB += slopeBC;
						xA += slopeAB;
						colorB += lightSlopeBC;
						colorA += lightSlopeAB;
						yC += Graphics2D.targetWidth;
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
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xA >> 16, xC >> 16, colorA >> 7, colorC >> 7);
						xA += slopeBC;
						xC += slopeCA;
						colorA += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yA >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xB >> 16, xC >> 16, colorB >> 7, colorC >> 7);
						xB += slopeAB;
						xC += slopeCA;
						colorB += lightSlopeAB;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
					}
				} else {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xC >> 16, xA >> 16, colorC >> 7, colorA >> 7);
						xA += slopeBC;
						xC += slopeCA;
						colorA += lightSlopeBC;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yA >= 0) {
						drawGradientScanline(Graphics2D.target, yC, 0, 0, xC >> 16, xB >> 16, colorC >> 7, colorB >> 7);
						xB += slopeAB;
						xC += slopeCA;
						colorB += lightSlopeAB;
						colorC += lightSlopeCA;
						yC += Graphics2D.targetWidth;
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

			if (testX) {
				if (xB - xA > 3) {
					// notice no fixed point transformations here?
					// that's because they're still fixed points!
					// At this point, colorA and colorB are 24.8 fixed points. :)
					lightnessSlope = (colorB - colorA) / (xB - xA);
				} else {
					lightnessSlope = 0;
				}

				if (xB > Graphics2D.rightX) {
					xB = Graphics2D.rightX;
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

			if (testX) {
				if (xB > Graphics2D.rightX) {
					xB = Graphics2D.rightX;
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

		// A is above B and C
		if (yA <= yB && yA <= yC) {

			// A is below the bottom of our drawing area.
			if (yA >= Graphics2D.bottom) {
				return;
			}

			// Clamp B's Y
			if (yB > Graphics2D.bottom) {
				yB = Graphics2D.bottom;
			}

			// Clamp C's Y
			if (yC > Graphics2D.bottom) {
				yC = Graphics2D.bottom;
			}

			// B is above C
			if (yB < yC) {
				// xC is now xA and they are both 16.16
				xC = xA <<= 16;

				// A is above our drawing area
				if (yA < 0) {
					xC -= slopeCA * yA;
					xA -= slopeAB * yA;
					yA = 0;
				}

				// 32.0 -> 16.16
				xB <<= 16;

				// B is above our drawing area
				if (yB < 0) {
					xB -= slopeBC * yB;
					yB = 0;
				}

				//
				// If A isn't in parallel horizontally with B and the slope from C to A is lower than the slope from A to B.
				// Or, if A and B are in parallel horizontally and the slope from C to A is greater than the slope from B to C.
				//
				// if statement:
				//
				// yA != yB && slopeCA < slopeAB:
				// A (3, 4)
				// |\
				// |  \
				// |    \B (8, 8)
				// |    /
				// |  /
				// |/
				// C (3, 12)
				// slopeAB = (8 - 3) / (8 - 4) = 5 / 4 = 1.25
				// slopeBC = (3 - 8) / (12 - 8) = -5 / 4 = -1.25
				// slopeCA = 0
				//
				//
				// if statement:
				//
				// yA == yB && slopeCA > slopeBC
				// A (3, 4)___B (8, 4)
				// |         /
				// |      /
				// |   /
				// |/
				// C (3, 8)
				//
				// slopeAB = (8 - 3) / (4 - 4) = 0
				// slopeBC = (3 - 8) / (8 - 4) = -5 / 4 = -1.25
				// slopeCA = (3 - 3) / (4 - 8) = 0 / -4 = 0
				//
				//
				// slopeAB = (xB - xA) / (yB - yA)
				// slopeBC = (xC - xB) / (yC - yB)
				// slopeCA = (xA - xC) / (yA - yC)
				if (yA != yB && slopeCA < slopeAB || yA == yB && slopeCA > slopeBC) {
					// yC is now the distance from yB to yC in pixels
					yC -= yB;

					// yB is now the distance from yA to yB in pixels
					yB -= yA;

					// yA is now the offset for our current Y position.
					yA = offsets[yA];

					// While we still have a vertical space between A and B
					while (--yB >= 0) {
						// Draw our scanline from xC (start) to xA (end) starting at the offset provided by yA
						drawScanline(Graphics2D.target, yA, color, 0, xC >> 16, xA >> 16);

						// approach xC to xA
						xC += slopeCA;

						// approach xA to xB
						xA += slopeAB;

						// Go down a line
						yA += Graphics2D.targetWidth;
					}

					// While we still have a vertical space between B and C
					while (--yC >= 0) {
						// Draw our scanline from xC (start) to xB (end) starting at the offset provided by yA
						drawScanline(Graphics2D.target, yA, color, 0, xC >> 16, xB >> 16);

						// Approach C to A horizontally
						xC += slopeCA;

						// Approach B to C horizontally
						xB += slopeBC;

						// Move down a line
						yA += Graphics2D.targetWidth;
					}
				} else {
					// yC is now the distance from yB to yC in pixels
					yC -= yB;

					// yB is now the distance from yA to yB in pixels
					yB -= yA;

					// yA is now the offset for our current Y position.
					yA = offsets[yA];

					// While we still have a vertical space between A and B
					while (--yB >= 0) {
						// Draw our scanline from xC (start) to xC (end) starting at the offset provided by yA
						drawScanline(Graphics2D.target, yA, color, 0, xA >> 16, xC >> 16);

						// Approach C to A horizontally
						xC += slopeCA;

						// Aproach A to B horizontally
						xA += slopeAB;

						// Move down a line
						yA += Graphics2D.targetWidth;
					}

					// While we still have a vertical space between B and C
					while (--yC >= 0) {
						// Draw our scanline from xB (start) to xC (end) starting at the offset provided by yA
						drawScanline(Graphics2D.target, yA, color, 0, xB >> 16, xC >> 16);

						// Approach C to A horizontally
						xC += slopeCA;

						// Approach B to C horizontally
						xB += slopeBC;

						// Move down a line
						yA += Graphics2D.targetWidth;
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
						drawScanline(Graphics2D.target, yA, color, 0, xB >> 16, xA >> 16);
						xB += slopeCA;
						xA += slopeAB;
						yA += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawScanline(Graphics2D.target, yA, color, 0, xC >> 16, xA >> 16);
						xC += slopeBC;
						xA += slopeAB;
						yA += Graphics2D.targetWidth;
					}
				} else {
					yB -= yC;
					yC -= yA;
					yA = offsets[yA];
					while (--yC >= 0) {
						drawScanline(Graphics2D.target, yA, color, 0, xA >> 16, xB >> 16);
						xB += slopeCA;
						xA += slopeAB;
						yA += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawScanline(Graphics2D.target, yA, color, 0, xA >> 16, xC >> 16);
						xC += slopeBC;
						xA += slopeAB;
						yA += Graphics2D.targetWidth;
					}
				}
			}
			// else A is below B or C, and B is above C.
		} else if (yB <= yC) {
			if (yB < Graphics2D.bottom) {
				if (yC > Graphics2D.bottom) {
					yC = Graphics2D.bottom;
				}

				if (yA > Graphics2D.bottom) {
					yA = Graphics2D.bottom;
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
							drawScanline(Graphics2D.target, yB, color, 0, xA >> 16, xB >> 16);
							xA += slopeAB;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yA >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xA >> 16, xC >> 16);
							xA += slopeAB;
							xC += slopeCA;
							yB += Graphics2D.targetWidth;
						}
					} else {
						yA -= yC;
						yC -= yB;
						yB = offsets[yB];
						while (--yC >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xB >> 16, xA >> 16);
							xA += slopeAB;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yA >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xC >> 16, xA >> 16);
							xA += slopeAB;
							xC += slopeCA;
							yB += Graphics2D.targetWidth;
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
							drawScanline(Graphics2D.target, yB, color, 0, xC >> 16, xB >> 16);
							xC += slopeAB;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yC >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xA >> 16, xB >> 16);
							xA += slopeCA;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
					} else {
						yC -= yA;
						yA -= yB;
						yB = offsets[yB];
						while (--yA >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xB >> 16, xC >> 16);
							xC += slopeAB;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
						while (--yC >= 0) {
							drawScanline(Graphics2D.target, yB, color, 0, xB >> 16, xA >> 16);
							xA += slopeCA;
							xB += slopeBC;
							yB += Graphics2D.targetWidth;
						}
					}
				}
			}
		} else if (yC < Graphics2D.bottom) {
			if (yA > Graphics2D.bottom) {
				yA = Graphics2D.bottom;
			}
			if (yB > Graphics2D.bottom) {
				yB = Graphics2D.bottom;
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
						drawScanline(Graphics2D.target, yC, color, 0, xB >> 16, xC >> 16);
						xB += slopeBC;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xB >> 16, xA >> 16);
						xB += slopeBC;
						xA += slopeAB;
						yC += Graphics2D.targetWidth;
					}
				} else {
					yB -= yA;
					yA -= yC;
					yC = offsets[yC];
					while (--yA >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xC >> 16, xB >> 16);
						xB += slopeBC;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yB >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xA >> 16, xB >> 16);
						xB += slopeBC;
						xA += slopeAB;
						yC += Graphics2D.targetWidth;
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
						drawScanline(Graphics2D.target, yC, color, 0, xA >> 16, xC >> 16);
						xA += slopeBC;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yA >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xB >> 16, xC >> 16);
						xB += slopeAB;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
					}
				} else {
					yA -= yB;
					yB -= yC;
					yC = offsets[yC];
					while (--yB >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xC >> 16, xA >> 16);
						xA += slopeBC;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
					}
					while (--yA >= 0) {
						drawScanline(Graphics2D.target, yC, color, 0, xC >> 16, xB >> 16);
						xB += slopeAB;
						xC += slopeCA;
						yC += Graphics2D.targetWidth;
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
		if (testX) {
			if (xB > Graphics2D.rightX) {
				xB = Graphics2D.rightX;
			}

			if (xA < 0) {
				xA = 0;
			}
		}

		if (xA >= xB) {
			return;
		}

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

	public static final void fillTexturedTriangle(int aY, int bY, int cY, int aX, int bX, int cX, int aL, int bL, int cL, int originX, int horizontalX, int verticalX, int originY, int horizontalY, int verticalY, int originZ, int horizontalZ, int verticalZ, int textureIndex) {
		// INT24_RGB array
		int[] texels = getTexels(textureIndex);

		opaque = !textureHasTransparency[textureIndex];

		// xM becomes the difference between xM and xP
		horizontalX = originX - horizontalX;

		// yM becomes the difference between yM and yP
		horizontalY = originY - horizontalY;

		// zM becomes the difference between zM and zP
		horizontalZ = originZ - horizontalZ;

		// xN becomes the difference between xP and xN
		verticalX -= originX;

		// yN becomes the difference between yP and yN
		verticalY -= originY;

		// zN becomes the difference between zP and zN
		verticalZ -= originZ;

		// named :3
		int originA = ((verticalZ * originX) - (verticalX * originZ)) << 5;
		int originC = ((horizontalX * verticalZ) - (horizontalZ * verticalX)) << 5;
		int originB = ((horizontalZ * originX) - (horizontalX * originZ)) << 5;

		int horizontalA = ((verticalY * originZ) - (verticalZ * originY)) << 8;
		int horizontalB = ((horizontalY * originZ) - (horizontalZ * originY)) << 8;
		int horizontalC = ((horizontalZ * verticalY) - (horizontalY * verticalZ)) << 8;

		int verticalA = ((verticalX * originY) - (verticalY * originX)) << 14;
		int verticalB = ((horizontalX * originY) - (horizontalY * originX)) << 14;
		int verticalC = ((horizontalY * verticalX) - (horizontalX * verticalY)) << 14;

		int slopeAB = 0;
		int lightSlopeAB = 0;

		if (bY != aY) {
			slopeAB = (bX - aX << 16) / (bY - aY);
			lightSlopeAB = (bL - aL << 16) / (bY - aY);
		}

		int slopeBC = 0;
		int lightSlopeBC = 0;

		if (cY != bY) {
			slopeBC = (cX - bX << 16) / (cY - bY);
			lightSlopeBC = (cL - bL << 16) / (cY - bY);
		}

		int slopeCA = 0;
		int lightSlopeCA = 0;

		if (cY != aY) {
			slopeCA = (aX - cX << 16) / (aY - cY);
			lightSlopeCA = (aL - cL << 16) / (aY - cY);
		}

		if (aY <= bY && aY <= cY) {
			if (aY < Graphics2D.bottom) {
				if (bY > Graphics2D.bottom) {
					bY = Graphics2D.bottom;
				}

				if (cY > Graphics2D.bottom) {
					cY = Graphics2D.bottom;
				}

				if (bY < cY) {
					cX = aX <<= 16;
					cL = aL <<= 16;

					if (aY < 0) {
						cX -= slopeCA * aY;
						aX -= slopeAB * aY;

						cL -= lightSlopeCA * aY;
						aL -= lightSlopeAB * aY;

						aY = 0;
					}

					bX <<= 16;
					bL <<= 16;

					if (bY < 0) {
						bX -= slopeBC * bY;
						bL -= lightSlopeBC * bY;
						bY = 0;
					}

					int offsetY = aY - centerY;
					verticalA += originA * offsetY;
					verticalB += originB * offsetY;
					verticalC += originC * offsetY;

					if (aY != bY && slopeCA < slopeAB || aY == bY && slopeCA > slopeBC) {
						cY -= bY;
						bY -= aY;
						aY = offsets[aY];

						while (--bY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, cX >> 16, aX >> 16, cL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeCA;
							aX += slopeAB;

							cL += lightSlopeCA;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, cX >> 16, bX >> 16, cL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeCA;
							bX += slopeBC;

							cL += lightSlopeCA;
							bL += lightSlopeBC;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					} else {
						cY -= bY;
						bY -= aY;
						aY = offsets[aY];

						while (--bY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, aX >> 16, cX >> 16, aL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeCA;
							aX += slopeAB;

							cL += lightSlopeCA;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, bX >> 16, cX >> 16, bL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeCA;
							bX += slopeBC;

							cL += lightSlopeCA;
							bL += lightSlopeBC;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					}
				} else {
					bX = aX <<= 16;
					bL = aL <<= 16;

					if (aY < 0) {
						bX -= slopeCA * aY;
						aX -= slopeAB * aY;

						bL -= lightSlopeCA * aY;
						aL -= lightSlopeAB * aY;

						aY = 0;
					}

					cX <<= 16;
					cL <<= 16;

					if (cY < 0) {
						cX -= slopeBC * cY;
						cL -= lightSlopeBC * cY;
						cY = 0;
					}

					int offsetY = aY - centerY;
					verticalA += originA * offsetY;
					verticalB += originB * offsetY;
					verticalC += originC * offsetY;

					if (aY != cY && slopeCA < slopeAB || aY == cY && slopeBC > slopeAB) {
						bY -= cY;
						cY -= aY;
						aY = offsets[aY];

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, bX >> 16, aX >> 16, bL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							bX += slopeCA;
							aX += slopeAB;

							bL += lightSlopeCA;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--bY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, cX >> 16, aX >> 16, cL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeBC;
							aX += slopeAB;

							cL += lightSlopeBC;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					} else {
						bY -= cY;
						cY -= aY;
						aY = offsets[aY];

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, aX >> 16, bX >> 16, aL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							bX += slopeCA;
							aX += slopeAB;

							bL += lightSlopeCA;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--bY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, aY, aX >> 16, cX >> 16, aL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeBC;
							aX += slopeAB;

							cL += lightSlopeBC;
							aL += lightSlopeAB;

							aY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					}
				}
			}
		} else if (bY <= cY) {
			if (bY < Graphics2D.bottom) {
				if (cY > Graphics2D.bottom) {
					cY = Graphics2D.bottom;
				}

				if (aY > Graphics2D.bottom) {
					aY = Graphics2D.bottom;
				}

				if (cY < aY) {
					aX = bX <<= 16;
					aL = bL <<= 16;

					if (bY < 0) {
						aX -= slopeAB * bY;
						bX -= slopeBC * bY;

						aL -= lightSlopeAB * bY;
						bL -= lightSlopeBC * bY;

						bY = 0;
					}

					cX <<= 16;
					cL <<= 16;

					if (cY < 0) {
						cX -= slopeCA * cY;
						cL -= lightSlopeCA * cY;
						cY = 0;
					}

					int offsetY = bY - centerY;
					verticalA += originA * offsetY;
					verticalB += originB * offsetY;
					verticalC += originC * offsetY;

					if (bY != cY && slopeAB < slopeBC || bY == cY && slopeAB > slopeCA) {
						aY -= cY;
						cY -= bY;
						bY = offsets[bY];

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, aX >> 16, bX >> 16, aL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeAB;
							bX += slopeBC;
							aL += lightSlopeAB;
							bL += lightSlopeBC;
							bY += Graphics2D.targetWidth;
							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--aY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, aX >> 16, cX >> 16, aL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeAB;
							cX += slopeCA;
							aL += lightSlopeAB;
							cL += lightSlopeCA;
							bY += Graphics2D.targetWidth;
							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					} else {
						aY -= cY;
						cY -= bY;
						bY = offsets[bY];

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, bX >> 16, aX >> 16, bL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeAB;
							bX += slopeBC;

							aL += lightSlopeAB;
							bL += lightSlopeBC;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--aY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, cX >> 16, aX >> 16, cL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeAB;
							cX += slopeCA;

							aL += lightSlopeAB;
							cL += lightSlopeCA;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					}
				} else {
					cX = bX <<= 16;
					cL = bL <<= 16;

					if (bY < 0) {
						cX -= slopeAB * bY;
						bX -= slopeBC * bY;

						cL -= lightSlopeAB * bY;
						bL -= lightSlopeBC * bY;

						bY = 0;
					}

					aX <<= 16;
					aL <<= 16;

					if (aY < 0) {
						aX -= slopeCA * aY;
						aL -= lightSlopeCA * aY;
						aY = 0;
					}

					int offsetY = bY - centerY;
					verticalA += originA * offsetY;
					verticalB += originB * offsetY;
					verticalC += originC * offsetY;

					if (slopeAB < slopeBC) {
						cY -= aY;
						aY -= bY;
						bY = offsets[bY];

						while (--aY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, cX >> 16, bX >> 16, cL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeAB;
							bX += slopeBC;

							cL += lightSlopeAB;
							bL += lightSlopeBC;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, aX >> 16, bX >> 16, aL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeCA;
							bX += slopeBC;

							aL += lightSlopeCA;
							bL += lightSlopeBC;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					} else {
						cY -= aY;
						aY -= bY;
						bY = offsets[bY];

						while (--aY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, bX >> 16, cX >> 16, bL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							cX += slopeAB;
							bX += slopeBC;

							cL += lightSlopeAB;
							bL += lightSlopeBC;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}

						while (--cY >= 0) {
							drawTexturedScanline(Graphics2D.target, texels, 0, 0, bY, bX >> 16, aX >> 16, bL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
							aX += slopeCA;
							bX += slopeBC;

							aL += lightSlopeCA;
							bL += lightSlopeBC;

							bY += Graphics2D.targetWidth;

							verticalA += originA;
							verticalB += originB;
							verticalC += originC;
						}
					}
				}
			}
		} else if (cY < Graphics2D.bottom) {
			if (aY > Graphics2D.bottom) {
				aY = Graphics2D.bottom;
			}

			if (bY > Graphics2D.bottom) {
				bY = Graphics2D.bottom;
			}

			if (aY < bY) {
				bX = cX <<= 16;
				bL = cL <<= 16;

				if (cY < 0) {
					bX -= slopeBC * cY;
					cX -= slopeCA * cY;

					bL -= lightSlopeBC * cY;
					cL -= lightSlopeCA * cY;

					cY = 0;
				}

				aX <<= 16;
				aL <<= 16;

				if (aY < 0) {
					aX -= slopeAB * aY;
					aL -= lightSlopeAB * aY;
					aY = 0;
				}

				int offsetY = cY - centerY;
				verticalA += originA * offsetY;
				verticalB += originB * offsetY;
				verticalC += originC * offsetY;

				if (slopeBC < slopeCA) {
					bY -= aY;
					aY -= cY;
					cY = offsets[cY];

					while (--aY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, bX >> 16, cX >> 16, bL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeBC;
						cX += slopeCA;

						bL += lightSlopeBC;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}

					while (--bY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, bX >> 16, aX >> 16, bL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeBC;
						aX += slopeAB;

						bL += lightSlopeBC;
						aL += lightSlopeAB;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}
				} else {
					bY -= aY;
					aY -= cY;
					cY = offsets[cY];

					while (--aY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, cX >> 16, bX >> 16, cL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeBC;
						cX += slopeCA;

						bL += lightSlopeBC;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}

					while (--bY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, aX >> 16, bX >> 16, aL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeBC;
						aX += slopeAB;

						bL += lightSlopeBC;
						aL += lightSlopeAB;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}
				}
			} else {
				aX = cX <<= 16;
				aL = cL <<= 16;

				if (cY < 0) {
					aX -= slopeBC * cY;
					cX -= slopeCA * cY;

					aL -= lightSlopeBC * cY;
					cL -= lightSlopeCA * cY;

					cY = 0;
				}

				bX <<= 16;
				bL <<= 16;

				if (bY < 0) {
					bX -= slopeAB * bY;
					bL -= lightSlopeAB * bY;
					bY = 0;
				}

				int offsetY = cY - centerY;
				verticalA += originA * offsetY;
				verticalB += originB * offsetY;
				verticalC += originC * offsetY;

				if (slopeBC < slopeCA) {
					aY -= bY;
					bY -= cY;
					cY = offsets[cY];

					while (--bY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, aX >> 16, cX >> 16, aL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						aX += slopeBC;
						cX += slopeCA;

						aL += lightSlopeBC;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}

					while (--aY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, bX >> 16, cX >> 16, bL >> 8, cL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeAB;
						cX += slopeCA;

						bL += lightSlopeAB;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}
				} else {
					aY -= bY;
					bY -= cY;
					cY = offsets[cY];

					while (--bY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, cX >> 16, aX >> 16, cL >> 8, aL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						aX += slopeBC;
						cX += slopeCA;

						aL += lightSlopeBC;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
					}

					while (--aY >= 0) {
						drawTexturedScanline(Graphics2D.target, texels, 0, 0, cY, cX >> 16, bX >> 16, cL >> 8, bL >> 8, verticalA, verticalB, verticalC, horizontalA, horizontalB, horizontalC);
						bX += slopeAB;
						cX += slopeCA;

						bL += lightSlopeAB;
						cL += lightSlopeCA;

						cY += Graphics2D.targetWidth;

						verticalA += originA;
						verticalB += originB;
						verticalC += originC;
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
	 * @param uA the initial u.
	 * @param vA the initial v.
	 * @param off the initial offset.
	 * @param xA the start x.
	 * @param xB the end x.
	 * @param lightnessA the start lightness.
	 * @param lightnessB the end lightness.
	 * @param verticalA the magic.
	 * @param verticalB the magic.
	 * @param verticalC the magic.
	 * @param horizontalA the magic.
	 * @param horizontalB the magic.
	 * @param horizontalC the magic.
	 */
	public static final void drawTexturedScanline(int[] dst, int[] texels, int uA, int vA, int off, int xA, int xB, int lightnessA, int lightnessB, int verticalA, int verticalB, int verticalC, int horizontalA, int horizontalB, int horizontalC) {
		if (xA >= xB) {
			return;
		}

		int length;
		int lightnessSlope;

		if (testX) {
			// we don't bitshift here because the lightness values are already 16.16 fixed points.
			lightnessSlope = (lightnessB - lightnessA) / (xB - xA);

			// Clamp the right if it's off screen.
			if (xB > Graphics2D.rightX) {
				xB = Graphics2D.rightX;
			}

			// Trim off the left if it's off screen.
			if (xA < 0) {
				lightnessA -= xA * lightnessSlope;
				xA = 0;
			}

			// If we start after our end point then just return.
			if (xA >= xB) {
				return;
			}

			// >> 3 is to give it that choppy textured look
			length = xB - xA >> 3;

			// fixed 20.12
			lightnessSlope <<= 12;

			// 7.25
			lightnessA <<= 9;
		} else {
			if (xB - xA > 7) {
				length = xB - xA >> 3;
				lightnessSlope = (lightnessB - lightnessA) * oneOverFixed1715[length] >> 6;
			} else {
				length = 0;
				lightnessSlope = 0;
			}

			// 7.25
			lightnessA <<= 9;
		}

		off += xA;

		if (lowmemory) {
			int uB = 0;
			int vB = 0;
			int delta = xA - centerX;

			verticalA += (horizontalA >> 3) * delta;
			verticalB += (horizontalB >> 3) * delta;
			verticalC += (horizontalC >> 3) * delta;

			int c = verticalC >> 12;

			if (c != 0) {
				uA = verticalA / c;
				vA = verticalB / c;

				if (uA < 0) {
					uA = 0;
				} else if (uA > (63 << 6)) {
					uA = (63 << 6);
				}
			}

			verticalA += horizontalA;
			verticalB += horizontalB;
			verticalC += horizontalC;
			c = verticalC >> 12;

			if (c != 0) {
				uB = verticalA / c;
				vB = verticalB / c;

				if (uB < 7) {
					uB = 7;
				} else if (uB > (63 << 6)) {
					uB = (63 << 6);
				}
			}

			int uStep = uB - uA >> 3;
			int vStep = vB - vA >> 3;

			uA += (lightnessA & (3 << 21)) >> 3;
			int lightness = lightnessA >> 23;

			if (opaque) {
				while (length-- > 0) {
					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;

					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA = uB;
					vA = vB;

					verticalA += horizontalA;
					verticalB += horizontalB;
					verticalC += horizontalC;
					c = verticalC >> 12;

					if (c != 0) {
						uB = verticalA / c;
						vB = verticalB / c;

						if (uB < 7) {
							uB = 7;
						} else if (uB > (63 << 6)) {
							uB = (63 << 6);
						}
					}

					uStep = uB - uA >> 3;
					vStep = vB - vA >> 3;
					lightnessA += lightnessSlope;
					uA += (lightnessA & (3 << 21)) >> 3;
					lightness = lightnessA >> 23;
				}

				length = xB - xA & 0x7;

				while (length-- > 0) {
					dst[off++] = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness;
					uA += uStep;
					vA += vStep;
				}
			} else {
				// ignore pure black texels

				while (length-- > 0) {
					int rgb;
					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += uStep;
					vA += vStep;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA = uB;
					vA = vB;

					verticalA += horizontalA;
					verticalB += horizontalB;
					verticalC += horizontalC;
					c = verticalC >> 12;

					if (c != 0) {
						uB = verticalA / c;
						vB = verticalB / c;

						if (uB < 7) {
							uB = 7;
						} else if (uB > (63 << 6)) {
							uB = (63 << 6);
						}
					}

					uStep = uB - uA >> 3;
					vStep = vB - vA >> 3;
					lightnessA += lightnessSlope;
					uA += (lightnessA & (3 << 21)) >> 3;
					lightness = lightnessA >> 23;
				}

				length = xB - xA & 0x7;

				while (length-- > 0) {
					int rgb;

					if ((rgb = texels[(vA & (63 << 6)) + (uA >> 6)] >>> lightness) != 0) {
						dst[off] = rgb;
					}

					off++;
					uA += uStep;
					vA += vStep;
				}
			}
		} else {
			int u2 = 0;
			int v2 = 0;
			int delta = xA - centerX;

			verticalA += (horizontalA >> 3) * delta;
			verticalB += (horizontalB >> 3) * delta;
			verticalC += (horizontalC >> 3) * delta;
			int realC = verticalC >> 14;

			if (realC != 0) {
				uA = verticalA / realC;
				vA = verticalB / realC;

				if (uA < 0) {
					uA = 0;
				} else if (uA > (127 << 7)) {
					uA = (127 << 7);
				}
			}

			verticalA += horizontalA;
			verticalB += horizontalB;
			verticalC += horizontalC;
			realC = verticalC >> 14;

			if (realC != 0) {
				u2 = verticalA / realC;
				v2 = verticalB / realC;

				if (u2 < 7) {
					u2 = 7;
				} else if (u2 > (127 << 7)) {
					u2 = (127 << 7);
				}
			}

			int deltaU = u2 - uA >> 3;
			int deltaV = v2 - vA >> 3;
			uA += lightnessA & (3 << 21);
			int lightness = lightnessA >> 23;

			if (opaque) {
				while (length-- > 0) {
					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;

					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA = u2;
					vA = v2;

					verticalA += horizontalA;
					verticalB += horizontalB;
					verticalC += horizontalC;
					realC = verticalC >> 14;

					if (realC != 0) {
						u2 = verticalA / realC;
						v2 = verticalB / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > (127 << 7)) {
							u2 = (127 << 7);
						}
					}

					deltaU = u2 - uA >> 3;
					deltaV = v2 - vA >> 3;
					lightnessA += lightnessSlope;
					uA += lightnessA & (3 << 21);
					lightness = lightnessA >> 23;
				}

				length = xB - xA & 0x7;

				while (length-- > 0) {
					dst[off++] = texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness;
					uA += deltaU;
					vA += deltaV;
				}
			} else {
				while (length-- > 0) {
					int rgb;
					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;

					if ((rgb = (texels[(vA & (127 << 7)) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA = u2;
					vA = v2;

					verticalA += horizontalA;
					verticalB += horizontalB;
					verticalC += horizontalC;
					realC = (verticalC >> 14);

					if (realC != 0) {
						u2 = verticalA / realC;
						v2 = verticalB / realC;

						if (u2 < 7) {
							u2 = 7;
						} else if (u2 > (127 << 7)) {
							u2 = (127 << 7);
						}
					}
					deltaU = u2 - uA >> 3;
					deltaV = v2 - vA >> 3;
					lightnessA += lightnessSlope;
					uA += lightnessA & (3 << 21);
					lightness = lightnessA >> 23;
				}

				length = xB - xA & 0x7;

				while (length-- > 0) {
					int rgb;
					if ((rgb = (texels[(vA & 0x3F80) + (uA >> 7)] >>> lightness)) != 0) {
						dst[off] = rgb;
					}
					off++;
					uA += deltaU;
					vA += deltaV;
				}
			}
		}
	}

	static {
		for (int i = 1; i < 512; i++) {
			oneOverFixed1715[i] = (1 << 15) / i;
		}

		for (int i = 1; i < 2048; i++) {
			oneOverFixed1616[i] = (1 << 16) / i;
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
