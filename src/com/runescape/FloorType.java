package com.runescape;

import java.util.logging.*;

/**
 * Contains information on how a tile should appear on the scene, and minimap.
 *
 * @author Dane
 */
public class FloorType {

	private static final Logger logger = Logger.getLogger(FloorType.class.getName());

	/**
	 * The floor instance count.
	 */
	public static int count;

	/**
	 * The floor instances.
	 */
	public static FloorType[] instances;

	/**
	 * The color in INT24_RGB format.
	 */
	public int rgb;

	/**
	 * The texture pointer this floor uses.
	 */
	public int textureIndex = -1;

	/**
	 * Whether this floor hides the underlay or not.
	 */
	public boolean occlude = true;

	/**
	 * The name of the floor.
	 */
	public String name;

	/**
	 * The hue of the floor.
	 */
	public int hue;

	/**
	 * The saturation of the floor.
	 */
	public int saturation;

	/**
	 * The lightness of the floor.
	 */
	public int lightness;

	/**
	 * The hue used for blending on the landscape.
	 */
	public int blendHue;

	/**
	 * The hue multiplier used for blending on the landscape.
	 */
	public int blendHueMultiplier;

	/**
	 * The index of the floor.
	 */
	public int index;

	/**
	 * Unpacks the floors and stores them in a static array.
	 *
	 * @param archive the archive containing the floor data file.
	 */
	public static void unpack(Archive archive) {
		Buffer b;
		byte[] data = Signlink.loadFile("flo.dat");

		if (data != null) {
			b = new Buffer(data);
		} else {
			b = new Buffer(archive.get("flo.dat", null));
		}

		count = b.readUShort();

		if (instances == null) {
			instances = new FloorType[count];
		}

		for (int n = 0; n < count; n++) {
			if (instances[n] == null) {
				instances[n] = new FloorType();
			}
			instances[n].index = n;
			instances[n].read(b);
		}
	}

	/**
	 * Sets the color of the floor.
	 *
	 * @param rgb the color. (INT24_RGB format)
	 */
	private void setColor(int rgb) {
		double r = (double) (rgb >> 16 & 0xff) / 256.0;
		double g = (double) (rgb >> 8 & 0xff) / 256.0;
		double b = (double) (rgb & 0xff) / 256.0;

		double min = Math.min(Math.min(r, g), b);
		double max = Math.max(Math.max(r, g), b);

		double hue = 0.0;
		double saturation = 0.0;
		double lightness = (min + max) / 2.0;

		if (min != max) {
			if (lightness < 0.5) {
				saturation = (max - min) / (max + min);
			}
			if (lightness >= 0.5) {
				saturation = (max - min) / (2.0 - max - min);
			}

			if (r == max) {
				hue = (g - b) / (max - min);
			} else if (g == max) {
				hue = 2.0 + (b - r) / (max - min);
			} else if (b == max) {
				hue = 4.0 + (r - g) / (max - min);
			}
		}

		hue /= 6.0;

		this.hue = (int) (hue * 256.0);
		this.saturation = (int) (saturation * 256.0);
		this.lightness = (int) (lightness * 256.0);

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

		if (lightness > 0.5) {
			this.blendHueMultiplier = (int) ((1.0 - lightness) * saturation * 512.0);
		} else {
			this.blendHueMultiplier = (int) (lightness * saturation * 512.0);
		}

		if (this.blendHueMultiplier < 1) {
			this.blendHueMultiplier = 1;
		}

		this.blendHue = (int) (hue * (double) this.blendHueMultiplier);
	}

	/**
	 * Reads the floor data from the provided buffer.
	 *
	 * @param buffer the buffer.
	 */
	private void read(Buffer buffer) {
		for (;;) {
			int opcode = buffer.read();

			if (opcode == 0) {
				break;
			}

			if (opcode == 1) {
				this.setColor(rgb = buffer.readInt24());
			} else if (opcode == 2) {
				this.textureIndex = buffer.read();
			} else if (opcode == 3) {
				// dummy
			} else if (opcode == 5) {
				this.occlude = false;
			} else if (opcode == 6) {
				this.name = buffer.readString();
			} else {
				logger.log(Level.WARNING, "Error unrecognized config code: {0}", opcode);
			}
		}
	}

}
