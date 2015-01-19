package com.runescape;

public final class Scene {

	public static final int VIEW_DIAMETER = 104;
	public static final int VIEW_RADIUS = VIEW_DIAMETER / 2;
	public static final int FAR_Z = 128 * VIEW_RADIUS;
	public static final int NEAR_Z = 50;

	private static final int[] WALL_ROTATION_TYPE1 = {0x1, 0x2, 0x4, 0x8};
	private static final int[] WALL_ROTATION_TYPE2 = {0x10, 0x20, 0x40, 0x80};
	private static final int[] WALL_DECO_ROT_SIZE_X_DIR = {1, 0, -1, 0};
	private static final int[] WALL_DECO_ROT_SIZE_Y_DIR = {0, -1, 0, 1};

	public static int hoverRadius;
	public static int hoverRadiusSquared;

	public static int hoverColor = 0x708090;
	public static int hoverPlane = 0;
	public static int hoverTileX = -1;
	public static int hoverTileZ = -1;
	public static boolean highlightHover = false;
	public static boolean forceTileHover = false;

	public static boolean forceInteraction = false;
	public static boolean lowmemory = true;
	public static int builtPlane;

	public static boolean enableOcclusion = true;
	public static boolean checkClick;

	public static int clickX;
	public static int clickY;

	public static int mouseX;
	public static int mouseY;

	public static int clickedTileX = -1;
	public static int clickedTileZ = -1;

	public int tileSizeX;
	public int tileSizeZ;
	public int[][][] heightmap;
	public byte[][][] renderflags;
	public byte[][][] planeUnderlayFloIndices;
	public byte[][][] planeOverlayFloIndices;
	public byte[][][] planeOverlayTypes;
	public byte[][][] planeOverlayRotations;
	public byte[][][] shadowmap;
	public int[][] lightmap;
	public int[] blendedHue;
	public int[] blendedSaturation;
	public int[] blendedLightness;
	public int[] blendedHueDivisor;
	public int[] blendDirectionTracker;
	public int[][][] cullingflags;

	public Scene(int sizeX, int sizeY, byte[][][] renderFlags, int[][][] heightmap) {
		this.tileSizeX = sizeX;
		this.tileSizeZ = sizeY;
		this.heightmap = heightmap;
		this.renderflags = renderFlags;
		this.planeUnderlayFloIndices = new byte[4][tileSizeX][tileSizeZ];
		this.planeOverlayFloIndices = new byte[4][tileSizeX][tileSizeZ];
		this.planeOverlayTypes = new byte[4][tileSizeX][tileSizeZ];
		this.planeOverlayRotations = new byte[4][tileSizeX][tileSizeZ];
		this.cullingflags = new int[4][tileSizeX + 1][tileSizeZ + 1];
		this.shadowmap = new byte[4][tileSizeX + 1][tileSizeZ + 1];
		this.lightmap = new int[tileSizeX + 1][tileSizeZ + 1];
		this.blendedHue = new int[tileSizeZ];
		this.blendedSaturation = new int[tileSizeZ];
		this.blendedLightness = new int[tileSizeZ];
		this.blendedHueDivisor = new int[tileSizeZ];
		this.blendDirectionTracker = new int[tileSizeZ];
	}

	public final void clearLandscape(int tileX, int tileY, int width, int height) {
		byte water = 0;
		for (int n = 0; n < Flo.count; n++) {
			if (Flo.instances[n].name.equalsIgnoreCase("water")) {
				water = (byte) (n + 1);
				break;
			}
		}

		for (int y = tileY; y < tileY + height; y++) {
			for (int x = tileX; x < tileX + width; x++) {
				if (x >= 0 && x < tileSizeX && y >= 0 && y < tileSizeZ) {
					planeOverlayFloIndices[0][x][y] = water;

					for (int plane = 0; plane < 4; plane++) {
						heightmap[plane][x][y] = 0;
						renderflags[plane][x][y] = (byte) 0;
					}
				}
			}
		}
	}

	public final void readLandscape(byte[] src, int baseTileX, int baseTileY, int baseChunkTileX, int baseChunkTileY) {
		Buffer b = new Buffer(src);
		for (int plane = 0; plane < 4; plane++) {
			for (int tileX = 0; tileX < 64; tileX++) {
				for (int tileY = 0; tileY < 64; tileY++) {
					int x = tileX + baseTileX;
					int y = tileY + baseTileY;

					if (x >= 0 && x < 104 && y >= 0 && y < 104) {
						renderflags[plane][x][y] = (byte) 0;
						for (;;) {
							int type = b.read();

							if (type == 0) {
								if (plane == 0) {
									heightmap[0][x][y] = -getPerlinNoise((x + 932731 + baseChunkTileX), (y + 556238 + baseChunkTileY)) * 8;
								} else {
									heightmap[plane][x][y] = heightmap[plane - 1][x][y] - 240;
								}
								break;
							}

							if (type == 1) {
								int i = b.read();

								if (i == 1) {
									i = 0;
								}

								if (plane == 0) {
									heightmap[0][x][y] = -i * 8;
								} else {
									heightmap[plane][x][y] = heightmap[plane - 1][x][y] - (i * 8);
								}
								break;
							}

							if (type <= 49) {
								planeOverlayFloIndices[plane][x][y] = b.readByte();
								planeOverlayTypes[plane][x][y] = (byte) ((type - 2) / 4);
								planeOverlayRotations[plane][x][y] = (byte) (type - 2 & 0x3);
							} else if (type <= 81) {
								renderflags[plane][x][y] = (byte) (type - 49);
							} else {
								planeUnderlayFloIndices[plane][x][y] = (byte) (type - 81);
							}
						}
					} else {
						for (;;) {
							int i = b.read();
							if (i == 0) {
								break;
							}
							if (i == 1) {
								b.read();
								break;
							}
							if (i <= 49) {
								b.read();
							}
						}
					}
				}
			}
		}
	}

	public final void readLocs(byte[] src, int mapBaseX, int mapBaseY, Landscape landscape, CollisionMap[] planeCollisions, LinkedList sequencedLocs) {
		Buffer b = new Buffer(src);
		int locIndex = -1;

		for (;;) {
			int msb = b.readUSmart();

			if (msb == 0) {
				break;
			}

			locIndex += msb;
			int position = 0;

			for (;;) {
				int lsb = b.readUSmart();

				if (lsb == 0) {
					break;
				}

				position += lsb - 1;

				int x = position & 0x3f;
				int y = position >> 6 & 0x3f;
				int plane = position >> 12;

				int flags = b.read();
				int rotation = flags & 0x3;
				int type = flags >> 2;

				int tileX = y + mapBaseX;
				int tileY = x + mapBaseY;

				if (tileX > 0 && tileY > 0 && tileX < 103 && tileY < 103) {
					addLoc(locIndex, type, landscape, planeCollisions[plane], sequencedLocs, tileX, tileY, plane, rotation);
				}
			}
		}
	}

	public final void addLoc(int locIndex, int type, Landscape landscape, CollisionMap collision, LinkedList sequencedLocs, int tileX, int tileY, int plane, int rotation) {
		if (lowmemory) {
			int p = plane;

			if (p > 0 && (renderflags[1][tileX][tileY] & 0x2) != 0) {
				p--;
			}

			if ((renderflags[plane][tileX][tileY] & 0x8) != 0) {
				p = 0;
			}

			if (p != builtPlane || (renderflags[plane][tileX][tileY] & 0x10) != 0) {
				return;
			}
		}

		int southwestY = heightmap[plane][tileX][tileY];
		int southeastY = heightmap[plane][tileX + 1][tileY];
		int northeastY = heightmap[plane][tileX + 1][tileY + 1];
		int northwestY = heightmap[plane][tileX][tileY + 1];
		int averageY = (southwestY + southeastY + northeastY + northwestY) >> 2;

		LocConfig l = LocConfig.get(locIndex);

		if (l == null) {
			return;
		}

		int bitset = tileX + (tileY << 7) + (locIndex << 14) + (0x2 << 29);

		if (!forceInteraction && !l.interactable) {
			bitset += Integer.MIN_VALUE;
		}

		byte info = (byte) ((rotation << 6) + type);

		if (type == 22) {
			if (!lowmemory || l.interactable) {
				Model m = l.getModel(22, rotation, southwestY, southeastY, northeastY, northwestY, -1);
				landscape.addGroundDecoration(m, plane, tileX, tileY, averageY, info, bitset);

				if (l.hasCollision && l.interactable) {
					collision.setBlocked(tileX, tileY);
				}
			}
		} else if (type == 10 || type == 11) {
			Model m = l.getModel(10, rotation, southwestY, southeastY, northeastY, northwestY, -1);

			if (m != null) {
				int yaw = 0;

				if (type == 11) {
					yaw += 256;
				}

				int sizeX;
				int sizeY;

				if (rotation == 1 || rotation == 3) {
					sizeX = l.sizeY;
					sizeY = l.sizeX;
				} else {
					sizeX = l.sizeX;
					sizeY = l.sizeY;
				}

				if (landscape.addLoc(m, null, tileX, tileY, sizeX, sizeY, averageY, plane, yaw, bitset, info) && l.hasShadow) {
					for (int x = 0; x <= sizeX; x++) {
						for (int y = 0; y <= sizeY; y++) {
							int darkness = m.boundHeight / 4;

							if (darkness > 30) {
								darkness = 30;
							}

							if (darkness > shadowmap[plane][tileX + x][tileY + y]) {
								shadowmap[plane][tileX + x][tileY + y] = (byte) darkness;
							}
						}
					}
				}
			}

			if (l.hasCollision) {
				collision.setLoc(tileX, tileY, l.sizeX, l.sizeY, rotation, l.isSolid);
			}

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 2, tileX, tileY, plane));
			}
		} else if (type >= 12) {
			Model m = l.getModel(type, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addLoc(m, null, tileX, tileY, 1, 1, averageY, plane, 0, bitset, info);

			if (type >= 12 && type <= 17 && type != 13 && plane > 0) {
				cullingflags[plane][tileX][tileY] |= 0x200 | 0x100 | 0x80 | 0x10 | 0x8 | 0x4;
			}

			if (l.hasCollision) {
				collision.setLoc(tileX, tileY, l.sizeX, l.sizeY, rotation, l.isSolid);
			}

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 2, tileX, tileY, plane));
			}
		} else if (type == 0) {
			Model m = l.getModel(0, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE1[rotation], 0);

			if (rotation == 0) {
				if (l.hasShadow) {
					shadowmap[plane][tileX][tileY] = (byte) 50;
					shadowmap[plane][tileX][tileY + 1] = (byte) 50;
				}

				if (l.culls) {
					cullingflags[plane][tileX][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
				}
			} else if (rotation == 1) {
				if (l.hasShadow) {
					shadowmap[plane][tileX][tileY + 1] = (byte) 50;
					shadowmap[plane][tileX + 1][tileY + 1] = (byte) 50;
				}

				if (l.culls) {
					cullingflags[plane][tileX][tileY + 1] |= 0x400 | 0x80 | 0x10 | 0x2;
				}
			} else if (rotation == 2) {
				if (l.hasShadow) {
					shadowmap[plane][tileX + 1][tileY] = (byte) 50;
					shadowmap[plane][tileX + 1][tileY + 1] = (byte) 50;
				}

				if (l.culls) {
					cullingflags[plane][tileX + 1][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
				}
			} else if (rotation == 3) {
				if (l.hasShadow) {
					shadowmap[plane][tileX][tileY] = (byte) 50;
					shadowmap[plane][tileX + 1][tileY] = (byte) 50;
				}

				if (l.culls) {
					cullingflags[plane][tileX][tileY] |= 0x400 | 0x80 | 0x10 | 0x2;
				}
			}

			if (l.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, l.isSolid);
			}
		} else if (type == 1) {
			Model m = l.getModel(1, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE2[rotation], 0);

			if (l.hasShadow) {
				if (rotation == 0) {
					shadowmap[plane][tileX][tileY + 1] = (byte) 50;
				} else if (rotation == 1) {
					shadowmap[plane][tileX + 1][tileY + 1] = (byte) 50;
				} else if (rotation == 2) {
					shadowmap[plane][tileX + 1][tileY] = (byte) 50;
				} else if (rotation == 3) {
					shadowmap[plane][tileX][tileY] = (byte) 50;
				}
			}

			if (l.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, l.isSolid);
			}
		} else if (type == 2) {
			int nextRotation = rotation + 1 & 0x3;
			Model model1 = l.getModel(2, rotation + 4, southwestY, southeastY, northeastY, northwestY, -1);
			Model model2 = l.getModel(2, nextRotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWall(model1, model2, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE1[rotation], WALL_ROTATION_TYPE1[nextRotation]);

			if (l.culls) {
				if (rotation == 0) {
					cullingflags[plane][tileX][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
					cullingflags[plane][tileX][tileY + 1] |= 0x400 | 0x80 | 0x10 | 0x2;
				} else if (rotation == 1) {
					cullingflags[plane][tileX][tileY + 1] |= 0x400 | 0x80 | 0x10 | 0x2;
					cullingflags[plane][tileX + 1][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
				} else if (rotation == 2) {
					cullingflags[plane][tileX + 1][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
					cullingflags[plane][tileX][tileY] |= 0x400 | 0x80 | 0x10 | 0x2;
				} else if (rotation == 3) {
					cullingflags[plane][tileX][tileY] |= 0x400 | 0x80 | 0x10 | 0x2;
					cullingflags[plane][tileX][tileY] |= 0x200 | 0x40 | 0x8 | 0x1;
				}
			}

			if (l.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, l.isSolid);
			}
		} else if (type == 3) {
			Model m = l.getModel(3, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE2[rotation], 0);

			if (l.hasShadow) {
				if (rotation == 0) {
					shadowmap[plane][tileX][tileY + 1] = (byte) 50;
				} else if (rotation == 1) {
					shadowmap[plane][tileX + 1][tileY + 1] = (byte) 50;
				} else if (rotation == 2) {
					shadowmap[plane][tileX + 1][tileY] = (byte) 50;
				} else if (rotation == 3) {
					shadowmap[plane][tileX][tileY] = (byte) 50;
				}
			}
			if (l.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, l.isSolid);
			}
		} else if (type == 9) {
			Model m = l.getModel(type, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addLoc(m, null, tileX, tileY, 1, 1, averageY, plane, 0, bitset, info);

			if (l.hasCollision) {
				collision.setLoc(tileX, tileY, l.sizeX, l.sizeY, rotation, l.isSolid);
			}
		} else if (type == 4) {
			Model m = l.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, WALL_ROTATION_TYPE1[rotation], rotation * 512);

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 1, tileX, tileY, plane));
			}
		} else if (type == 5) {
			int thickness = 16;
			int wallBitset = landscape.getWallBitset(tileX, tileY, plane);

			if (wallBitset > 0) {
				thickness = LocConfig.get(wallBitset >> 14 & 0x7fff).thickness;
			}

			Model m = l.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWallDecoration(m, tileX, tileY, averageY, WALL_DECO_ROT_SIZE_X_DIR[rotation] * thickness, WALL_DECO_ROT_SIZE_Y_DIR[rotation] * thickness, plane, bitset, info, WALL_ROTATION_TYPE1[rotation], rotation * 512);

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 1, tileX, tileY, plane));
			}
		} else if (type == 6) {
			Model m = l.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 256, rotation);

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 1, tileX, tileY, plane));
			}
		} else if (type == 7) {
			Model m = l.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 512, rotation);

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 1, tileX, tileY, plane));
			}
		} else if (type == 8) {
			Model m = l.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			landscape.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 768, rotation);

			if (l.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[l.seqIndex], locIndex, 1, tileX, tileY, plane));
			}
		}
	}

	public final void buildLandscape(CollisionMap[] planeCollisions, Landscape landscape) {
		CollisionMap lastCollisionMap = null;
		
		for (int plane = 0; plane < 4; plane++) {
			CollisionMap collisionMap = planeCollisions[plane];

			for (int x = 0; x < 104; x++) {
				for (int y = 0; y < 104; y++) {
					if ((renderflags[plane][x][y] & 0x1) == 1) {
						collisionMap.setBlocked(x, y);
					}

					// it's a bridge!
					if (plane > 0 && (renderflags[1][x][y] & 0x2) == 2) {
						lastCollisionMap.flags[x][y] = collisionMap.flags[x][y];
					}
				}
			}
			lastCollisionMap = collisionMap;
		}

		for (int y = 0; y < 4; y++) {
			byte[][] sm = shadowmap[y];
			int baseIntensity = 96;
			int lightSpecularFactor = 768;
			int lightX = -50;
			int lightY = -10;
			int lightZ = -50;
			int lightLength = (int) Math.sqrt((double) (lightX * lightX + lightY * lightY + lightZ * lightZ));
			int specularDistribution = (lightSpecularFactor * lightLength) >> 8;

			for (int z = 1; z < tileSizeZ - 1; z++) {
				for (int x = 1; x < tileSizeX - 1; x++) {
					int dx = heightmap[y][x + 1][z] - heightmap[y][x - 1][z];
					int dy = heightmap[y][x][z + 1] - heightmap[y][x][z - 1];
					int length = (int) Math.sqrt((double) ((dx * dx) + (256 * 256) + (dy * dy)));

					if (length == 0) {
						length = 256;
					}

					int vx = (dx << 8) / length;
					int vy = (256 << 8) / length;
					int vz = (dy << 8) / length;

					int lightIntensity = baseIntensity + (lightX * vx + lightY * vy + lightZ * vz) / specularDistribution;
					int shadowIntensity = (sm[x - 1][z] >> 2) + (sm[x + 1][z] >> 3) + (sm[x][z - 1] >> 2) + (sm[x][z + 1] >> 3) + (sm[x][z] >> 1);
					lightmap[x][z] = lightIntensity - shadowIntensity;
				}
			}

			for (int z = 0; z < tileSizeZ; z++) {
				blendedHue[z] = 0;
				blendedSaturation[z] = 0;
				blendedLightness[z] = 0;
				blendedHueDivisor[z] = 0;
				blendDirectionTracker[z] = 0;
			}

			for (int x = -5; x < tileSizeX + 5; x++) {
				for (int z = 0; z < tileSizeZ; z++) {
					int dx = x + 5;

					if (dx >= 0 && dx < tileSizeX) {
						int floIndex = (planeUnderlayFloIndices[y][dx][z] & 0xFF) - 1;

						if (floIndex >= 0 && floIndex < Flo.instances.length) {
							Flo f = Flo.instances[floIndex];
							blendedHue[z] += f.hue2;
							blendedSaturation[z] += f.saturation;
							blendedLightness[z] += f.lightness;
							blendedHueDivisor[z] += f.hueDivisor;
							blendDirectionTracker[z]++;
						}
					}

					dx = x - 5;

					if (dx >= 0 && dx < tileSizeX) {
						int flo = (planeUnderlayFloIndices[y][dx][z] & 0xFF) - 1;

						if (flo >= 0 && flo < Flo.instances.length) {
							Flo f = Flo.instances[flo];
							blendedHue[z] -= f.hue2;
							blendedSaturation[z] -= f.saturation;
							blendedLightness[z] -= f.lightness;
							blendedHueDivisor[z] -= f.hueDivisor;
							blendDirectionTracker[z]--;
						}
					}
				}

				if (x >= 1 && x < tileSizeX - 1) {
					int hue = 0;
					int saturation = 0;
					int lightness = 0;
					int hueDivisor = 0;
					int directionTracker = 0;

					for (int z = -5; z < tileSizeZ + 5; z++) {
						int dz = z + 5;

						if (dz >= 0 && dz < tileSizeZ) {
							hue += blendedHue[dz];
							saturation += blendedSaturation[dz];
							lightness += blendedLightness[dz];
							hueDivisor += blendedHueDivisor[dz];
							directionTracker += blendDirectionTracker[dz];
						}

						dz = z - 5;

						if (dz >= 0 && dz < tileSizeZ) {
							hue -= blendedHue[dz];
							saturation -= blendedSaturation[dz];
							lightness -= blendedLightness[dz];
							hueDivisor -= blendedHueDivisor[dz];
							directionTracker -= blendDirectionTracker[dz];
						}

						if (z >= 1 && z < tileSizeZ - 1) {
							if (lowmemory) {
								int p = y;

								// it's a bridge!
								if (y > 0 && (renderflags[1][x][z] & 0x2) != 0) {
									p--;
								}

								if (((renderflags[y][x][z]) & 0x8) != 0) {
									p = 0;
								}

								if (p != builtPlane || ((renderflags[y][x][z]) & 0x10) != 0) {
									continue;
								}
							}

							int underlay = planeUnderlayFloIndices[y][x][z] & 0xFF;
							int overlay = planeOverlayFloIndices[y][x][z] & 0xFF;

							if (underlay > 0 || overlay > 0) {
								int southwestY = heightmap[y][x][z];
								int southeastY = heightmap[y][x + 1][z];
								int northeastY = heightmap[y][x + 1][z + 1];
								int northwestY = heightmap[y][x][z + 1];

								int southwestLightness = lightmap[x][z];
								int southeastLightness = lightmap[x + 1][z];
								int northeastLightness = lightmap[x + 1][z + 1];
								int northwestLightness = lightmap[x][z + 1];

								int color = -1;

								if (underlay > 0) {
									if (hueDivisor != 0 && directionTracker != 0) {
										color = getColor((hue * 256) / hueDivisor, saturation / directionTracker, lightness / directionTracker);
									}
								}

								if (y > 0 && !lowmemory) {
									boolean hideUnderlay = true;

									if (underlay == 0 && planeOverlayTypes[y][x][z] != 0) {
										hideUnderlay = false;
									}

									if (overlay > 0 && overlay - 1 < Flo.count && !(Flo.instances[overlay - 1].occlude)) {
										hideUnderlay = false;
									}

									if (hideUnderlay && southwestY == southeastY && southwestY == northeastY && southwestY == northwestY) {
										cullingflags[y][x][z] |= 0x200 | 0x100 | 0x80 | 0x10 | 0x8 | 0x4;
									}
								}

								int minimapColor = 0;

								if (color != -1) {
									minimapColor = Canvas3D.palette[adjustColorLightness(color, 96)];
								}

								if (overlay == 0) {
									landscape.addTile(y, x, z, 0, 0, -1, southwestY, southeastY, northeastY, northwestY, adjustColorLightness(color, southwestLightness), adjustColorLightness(color, southeastLightness), adjustColorLightness(color, northeastLightness), adjustColorLightness(color, northwestLightness), 0, 0, 0, 0, minimapColor, 0);
								} else {
									int type = planeOverlayTypes[y][x][z] + 1;
									byte rotation = planeOverlayRotations[y][x][z];

									overlay--;

									if (overlay >= Flo.count) {
										overlay = 0;
									}

									Flo f = Flo.instances[overlay];
									int texture = f.textureIndex;
									int rgb;
									int hsl;

									if (texture >= 0) {
										rgb = Canvas3D.getTextureColor(texture);
										hsl = -1;
									} else if (f.rgb == 0xFF00FF) {
										rgb = 0;
										hsl = -2;
										texture = -1;
									} else {
										hsl = getColor(f.hue, f.saturation, f.lightness);
										rgb = Canvas3D.palette[adjustHSLLightness0(hsl, 96)];
									}

									landscape.addTile(y, x, z, type, rotation, texture, southwestY, southeastY, northeastY, northwestY, adjustColorLightness(color, southwestLightness), adjustColorLightness(color, southeastLightness), adjustColorLightness(color, northeastLightness), adjustColorLightness(color, northwestLightness), adjustHSLLightness0(hsl, southwestLightness), adjustHSLLightness0(hsl, southeastLightness), adjustHSLLightness0(hsl, northeastLightness), adjustHSLLightness0(hsl, northwestLightness), minimapColor, rgb);
								}
							}
						}
					}
				}
			}

			for (int z = 1; z < tileSizeZ - 1; z++) {
				for (int x = 1; x < tileSizeX - 1; x++) {
					int drawY = y;

					if (drawY > 0 && ((renderflags[1][x][z] & 0x2) != 0)) {
						drawY--;
					}

					if ((renderflags[y][x][z] & 0x8) != 0) {
						drawY = 0;
					}

					landscape.setDrawPlane(y, x, z, drawY);
				}
			}
		}

		landscape.applyLighting(-50, -10, -50, 64, 768);

		for (int tileX = 0; tileX < tileSizeX; tileX++) {
			for (int tileY = 0; tileY < tileSizeZ; tileY++) {
				if ((renderflags[1][tileX][tileY] & 0x2) == 2) {
					landscape.setBridge(tileX, tileY);
				}
			}
		}

		if (Scene.enableOcclusion) {
			int rule0 = 0x1;
			int rule1 = 0x2;
			int rule2 = 0x4;

			for (int plane = 0; plane < 4; plane++) {
				if (plane > 0) {
					rule0 <<= 3;
					rule1 <<= 3;
					rule2 <<= 3;
				}

				for (int p = 0; p <= plane; p++) {
					for (int tileY = 0; tileY <= tileSizeZ; tileY++) {
						for (int tileX = 0; tileX <= tileSizeX; tileX++) {

							if ((cullingflags[p][tileX][tileY] & rule0) != 0) {
								int minTileY = tileY;
								int maxTileY = tileY;
								int minPlane = p;
								int maxPlane = p;

								for (; minTileY > 0; minTileY--) {
									if ((cullingflags[p][tileX][minTileY - 1] & rule0) == 0) {
										break;
									}
								}

								for (; maxTileY < tileSizeZ; maxTileY++) {
									if ((cullingflags[p][tileX][maxTileY + 1] & rule0) == 0) {
										break;
									}
								}

								findMinPlane:
								for (; minPlane > 0; minPlane--) {
									for (int y = minTileY; y <= maxTileY; y++) {
										if ((cullingflags[minPlane - 1][tileX][y] & rule0) == 0) {
											break findMinPlane;
										}
									}
								}

								findMaxPlane:
								for (; maxPlane < plane; maxPlane++) {
									for (int x = minTileY; x <= maxTileY; x++) {
										if ((cullingflags[maxPlane + 1][tileX][x] & rule0) == 0) {
											break findMaxPlane;
										}
									}
								}

								int surface = ((maxPlane + 1 - minPlane) * (maxTileY - minTileY + 1));

								if (surface >= 8) {
									int minY = heightmap[maxPlane][tileX][minTileY] - 240;
									int maxY = heightmap[minPlane][tileX][minTileY];

									Landscape.addOccluder(1, tileX * 128, tileX * 128, minY, maxY, minTileY * 128, maxTileY * 128 + 128, plane);

									for (int p1 = minPlane; p1 <= maxPlane; p1++) {
										for (int y = minTileY; y <= maxTileY; y++) {
											cullingflags[p1][tileX][y] &= rule0 ^ 0xFFFFFFFF;
										}
									}
								}
							}

							if ((cullingflags[p][tileX][tileY] & rule1) != 0) {
								int minX = tileX;
								int maxX = tileX;
								int minPlane = p;
								int maxPlane = p;

								for (; minX > 0; minX--) {
									if (((cullingflags[p][minX - 1][tileY]) & rule1) == 0) {
										break;
									}
								}

								for (; maxX < tileSizeX; maxX++) {
									if (((cullingflags[p][maxX + 1][tileY]) & rule1) == 0) {
										break;
									}
								}

								findMinPlane:
								for (; minPlane > 0; minPlane--) {
									for (int x = minX; x <= maxX; x++) {
										if (((cullingflags[minPlane - 1][x][tileY]) & rule1) == 0) {
											break findMinPlane;
										}
									}
								}

								findMaxPlane:
								for (; maxPlane < plane; maxPlane++) {
									for (int x = minX; x <= maxX; x++) {
										if (((cullingflags[maxPlane + 1][x][tileY]) & rule1) == 0) {
											break findMaxPlane;
										}
									}
								}

								int surface = ((maxPlane + 1 - minPlane) * (maxX - minX + 1));

								if (surface >= 8) {
									int minY = heightmap[maxPlane][minX][tileY] - 240;
									int maxY = heightmap[minPlane][minX][tileY];

									Landscape.addOccluder(2, minX * 128, maxX * 128 + 128, minY, maxY, tileY * 128, tileY * 128, plane);

									for (int p1 = minPlane; p1 <= maxPlane; p1++) {
										for (int x = minX; x <= maxX; x++) {
											cullingflags[p1][x][tileY] &= rule1 ^ 0xffffffff;
										}
									}
								}
							}

							if (!lowmemory && (cullingflags[p][tileX][tileY] & rule2) != 0) {
								int minX = tileX;
								int maxX = tileX;
								int minY = tileY;
								int maxY = tileY;

								for (; minY > 0; minY--) {
									if (((cullingflags[p][tileX][minY - 1]) & rule2) == 0) {
										break;
									}
								}

								for (; maxY < tileSizeZ; maxY++) {
									if (((cullingflags[p][tileX][maxY + 1]) & rule2) == 0) {
										break;
									}
								}

								FIND_MIN_X:
								for (; minX > 0; minX--) {
									for (int y = minY; y <= maxY; y++) {
										if (((cullingflags[p][minX - 1][y]) & rule2) == 0) {
											break FIND_MIN_X;
										}
									}
								}

								FIND_MAX_X:
								for (; maxX < tileSizeX; maxX++) {
									for (int y = minY; y <= maxY; y++) {
										if (((cullingflags[p][maxX + 1][y]) & rule2) == 0) {
											break FIND_MAX_X;
										}
									}
								}

								if ((maxX - minX + 1) * (maxY - minY + 1) >= 4) {
									int z = heightmap[p][minX][minY];
									Landscape.addOccluder(4, minX * 128, maxX * 128 + 128, z, z, minY * 128, maxY * 128 + 128, plane);

									for (int x = minX; x <= maxX; x++) {
										for (int y = minY; y <= maxY; y++) {
											cullingflags[p][x][y] &= rule2 ^ 0xffffffff;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public static final int getPerlinNoise(int x, int y) {
		int v = (getSmoothNoise(x + 45365, y + 91923, 4) - 128 + (getSmoothNoise(x + 10294, y + 37821, 2) - 128 >> 1) + (getSmoothNoise(x, y, 1) - 128 >> 2));
		v = (int) ((double) v * 0.3) + 35;

		if (v < 10) {
			v = 10;
		} else if (v > 60) {
			v = 60;
		}

		return v;
	}

	private static int getSmoothNoise(int x, int y, int fraction) {
		int x1 = x / fraction;
		int x2 = x & fraction - 1;
		int y1 = y / fraction;
		int y2 = y & fraction - 1;
		int a = getSmoothNoise2D(x1, y1);
		int b = getSmoothNoise2D(x1 + 1, y1);
		int c = getSmoothNoise2D(x1, y1 + 1);
		int d = getSmoothNoise2D(x1 + 1, y1 + 1);
		int e = getCosineLerp(a, b, x2, fraction);
		int f = getCosineLerp(c, d, x2, fraction);
		return getCosineLerp(e, f, y2, fraction);
	}

	private static int getCosineLerp(int a, int b, int ft, int frac) {
		int f = (65536 - (Canvas3D.cos[ft * 1024 / frac]) >> 1);
		return (a * (65536 - f) >> 16) + (b * f >> 16);
	}

	private static int getSmoothNoise2D(int x, int y) {
		int corners = (getNoise(x - 1, y - 1) + getNoise(x + 1, y - 1) + getNoise(x - 1, y + 1) + getNoise(x + 1, y + 1));
		int sides = (getNoise(x - 1, y) + getNoise(x + 1, y) + getNoise(x, y - 1) + getNoise(x, y + 1));
		int center = getNoise(x, y);
		return corners / 16 + sides / 8 + center / 4;
	}

	private static int getNoise(int x, int y) {
		int z = x + y * 57;
		z = z << 13 ^ z;
		int v = (z * (z * z * 15731 + 789221) + 1376312589 & 0x7fffffff);
		return (v >> 19) & 0xFF;
	}

	private static int adjustColorLightness(int hsl, int lightness) {
		if (hsl == -1) {
			return 12345678;
		}

		lightness = lightness * (hsl & 0x7f) / 128;

		if (lightness < 2) {
			lightness = 2;
		} else if (lightness > 126) {
			lightness = 126;
		}

		return (hsl & 0xff80) + lightness;
	}

	private int adjustHSLLightness0(int hsl, int lightness) {
		if (hsl == -2) {
			return 12345678;
		}

		if (hsl == -1) {
			if (lightness < 0) {
				lightness = 0;
			} else if (lightness > 127) {
				lightness = 127;
			}
			lightness = 127 - lightness;
			return lightness;
		}

		lightness = lightness * (hsl & 0x7f) / 128;

		if (lightness < 2) {
			lightness = 2;
		} else if (lightness > 126) {
			lightness = 126;
		}
		return (hsl & 0xff80) + lightness;
	}

	private int getColor(int hue, int saturation, int lightness) {
		if (lightness > 179) {
			saturation /= 2;
		}
		if (lightness > 192) {
			saturation /= 2;
		}
		if (lightness > 217) {
			saturation /= 2;
		}
		if (lightness > 243) {
			saturation /= 2;
		}
		return (hue / 4 << 10) + (saturation / 32 << 7) + lightness / 2;
	}

	public static final void addLoc(int type, int index, int tileX, int tileY, int plane, int groundPlane, int rotation, int[][][] planeHeightmaps, Landscape land, CollisionMap collision, LinkedList sequencedLocs) {
		int southwestY = planeHeightmaps[groundPlane][tileX][tileY];
		int southeastY = planeHeightmaps[groundPlane][tileX + 1][tileY];
		int northeastY = planeHeightmaps[groundPlane][tileX + 1][tileY + 1];
		int northwestY = planeHeightmaps[groundPlane][tileX][tileY + 1];
		int averageY = southwestY + southeastY + northeastY + northwestY >> 2;

		LocConfig c = LocConfig.get(index);

		int bitset = tileX + (tileY << 7) + (index << 14) + 0x40000000;

		if (!c.interactable) {
			bitset += Integer.MIN_VALUE;
		}

		byte info = (byte) ((rotation << 6) + type);

		if (type == 22) {
			Model m = c.getModel(22, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addGroundDecoration(m, plane, tileX, tileY, averageY, info, bitset);

			if (c.hasCollision && c.interactable) {
				collision.setBlocked(tileX, tileY);
			}
		} else if (type == 10 || type == 11) {
			Model m = c.getModel(10, rotation, southwestY, southeastY, northeastY, northwestY, -1);

			if (m != null) {
				int yaw = 0;

				if (type == 11) {
					yaw += 256;
				}

				int sizeY;
				int sizeX;

				if (rotation == 1 || rotation == 3) {
					sizeY = c.sizeY;
					sizeX = c.sizeX;
				} else {
					sizeY = c.sizeX;
					sizeX = c.sizeY;
				}

				land.addLoc(m, null, tileX, tileY, sizeY, sizeX, averageY, plane, yaw, bitset, info);
			}

			if (c.hasCollision) {
				collision.setLoc(tileX, tileY, c.sizeX, c.sizeY, rotation, c.isSolid);
			}

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 2, tileX, tileY, plane));
			}
		} else if (type >= 12) {
			Model m = c.getModel(type, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addLoc(m, null, tileX, tileY, 1, 1, averageY, plane, 0, bitset, info);

			if (c.hasCollision) {
				collision.setLoc(tileX, tileY, c.sizeX, c.sizeY, rotation, c.isSolid);
			}

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 2, tileX, tileY, plane));
			}
		} else if (type == 0) {
			Model m = c.getModel(0, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE1[rotation], 0);

			if (c.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, c.isSolid);
			}
		} else if (type == 1) {
			Model m = c.getModel(1, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE2[rotation], 0);

			if (c.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, c.isSolid);
			}
		} else if (type == 2) {
			int nextRotation = rotation + 1 & 0x3;
			Model model1 = c.getModel(2, rotation + 4, southwestY, southeastY, northeastY, northwestY, -1);
			Model model2 = c.getModel(2, nextRotation, southwestY, southeastY, northeastY, northwestY, -1);

			land.addWall(model1, model2, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE1[rotation], WALL_ROTATION_TYPE1[nextRotation]);

			if (c.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, c.isSolid);
			}
		} else if (type == 3) {
			Model m = c.getModel(3, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWall(m, null, plane, tileX, tileY, averageY, bitset, info, WALL_ROTATION_TYPE2[rotation], 0);

			if (c.hasCollision) {
				collision.setWall(tileX, tileY, type, rotation, c.isSolid);
			}
		} else if (type == 9) {
			Model m = c.getModel(type, rotation, southwestY, southeastY, northeastY, northwestY, -1);
			land.addLoc(m, null, tileX, tileY, 1, 1, averageY, plane, 0, bitset, info);

			if (c.hasCollision) {
				collision.setLoc(tileX, tileY, c.sizeX, c.sizeY, rotation, c.isSolid);
			}
		} else if (type == 4) {
			Model m = c.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, WALL_ROTATION_TYPE1[rotation], rotation * 512);

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 1, tileX, tileY, plane));
			}
		} else if (type == 5) {
			int thickness = 16;
			int wallBitset = land.getWallBitset(tileX, tileY, plane);

			if (wallBitset > 0) {
				thickness = LocConfig.get(wallBitset >> 14 & 0x7fff).thickness;
			}

			Model m = c.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWallDecoration(m, tileX, tileY, averageY, WALL_DECO_ROT_SIZE_X_DIR[rotation] * thickness, WALL_DECO_ROT_SIZE_Y_DIR[rotation] * thickness, plane, bitset, info, WALL_ROTATION_TYPE1[rotation], rotation * 512);

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 1, tileX, tileY, plane));
			}
		} else if (type == 6) {
			Model m = c.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 0x100, rotation);

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 1, tileX, tileY, plane));
			}
		} else if (type == 7) {
			Model m = c.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 0x200, rotation);

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 1, tileX, tileY, plane));
			}
		} else if (type == 8) {
			Model m = c.getModel(4, 0, southwestY, southeastY, northeastY, northwestY, -1);
			land.addWallDecoration(m, tileX, tileY, averageY, 0, 0, plane, bitset, info, 0x300, rotation);

			if (c.seqIndex != -1) {
				sequencedLocs.push(new SequencedLoc(Seq.instance[c.seqIndex], index, 1, tileX, tileY, plane));
			}
		}
	}
}
