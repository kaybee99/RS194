package com.runescape;

public final class SceneGraph {

	public static boolean lowmemory = true;

	public int maxPlane;
	public int tileCountX;
	public int tileCountZ;
	public int[][][] heightmap;
	public Tile[][][] planeTiles;
	public int minPlane;
	public int locCount;
	public Location[] locs = new Location[5000];
	public int[][][] levelTileCycle;
	public static int lastTileUpdateCount;
	public static int tileUpdateCount;
	public static int topPlane;
	public static int cycle;
	public static int minTileX;
	public static int maxTileX;
	public static int minTileZ;
	public static int maxTileZ;
	public static int cameraTileX;
	public static int cameraTileZ;
	public static int cameraX;
	public static int cameraY;
	public static int cameraZ;
	public static int pitchSin;
	public static int pitchCos;
	public static int yawSin;
	public static int yawCos;
	public static Location[] locationBuffer = new Location[100];

	public static final int[] DECO_TYPE1_OFFSET_X = {53, -53, -53, 53};
	public static final int[] DECO_TYPE1_OFFSET_Z = {-53, -53, 53, 53};
	public static final int[] DECO_TYPE2_OFFSET_X = {-45, 45, 45, -45};
	public static final int[] DECO_TYPE2_OFFSET_Z = {45, 45, -45, -45};

	public static final int MAX_OCCLUDER_PLANES = 4;
	public static int[] planeOccluderCount = new int[MAX_OCCLUDER_PLANES];
	public static Occluder[][] planeOccluders = new Occluder[MAX_OCCLUDER_PLANES][500];
	public static int activeOccluderCount;
	public static Occluder[] activeOcludders = new Occluder[500];
	public static LinkedQueue tileQueue = new LinkedQueue();

	// EAST NORTH WEST SOUTH
	//
	//WALL_ROTATION_TYPE1 = {0x1, 0x2, 0x4, 0x8} main piece
	//WALL_ROTATION_TYPE2 = {0x10, 0x20, 0x40, 0x80} extension piece
	//
	// [0] = cameraTileX > tileX && cameraTileZ < tileZ
	// [1] = cameraTileX == tileX && cameraTileZ < tileZ
	// [2] = cameraTileX < tileX && cameraTileZ < tileZ
	// [3] = cameraTileX > tileX && cameratileZ == tileZ
	// [4] = cameraTileX == tileX && cameraTileZ == tileZ
	// [5] = cameraTileX < tileX && cameraTileZ == tileZ
	// [6] = cameraTileX > tileX && cameraTileZ > tileZ
	// [7] = cameraTileX == tileX && cameraTileZ > tileZ
	// [8] = cameraTileX < tileX && cameraTileZ > tileZ
	//
	//
	// @formatter:off
	public static final int[] TILE_WALL_DRAW_FLAGS_0 = {
		0x10 | 0x2 | 0x1,
		0x20 | 0x10 | 0x4 | 0x2 | 0x1,
		0x20 | 0x4 | 0x2,
		0x80 | 0x10 | 0x8 | 0x2 | 0x1,
		0x80 | 0x40 | 0x20 | 0x10 | 0x8 | 0x4 | 0x2 | 0x1,
		0x40 | 0x20 | 0x8 | 0x4 | 0x2,
		0x80 | 0x8 | 0x1,
		0x80 | 0x40 | 0x8 | 0x4 | 0x1,
		0x40 | 0x8 | 0x4
	};

	public static final int[] WALL_DRAW_FLAGS = {
		0x80 | 0x20,
		0x80 | 0x40,
		0x40 | 0x10,
		0x40 | 0x20,
		0x0,
		0x80 | 0x10,
		0x40 | 0x10,
		0x20 | 0x10,
		0x80 | 0x20
	};

	public static final int[] TILE_WALL_DRAW_FLAGS_1 = {
		0x40 | 0x8 | 0x4,
		0x8,
		0x80 | 0x8 | 0x1,
		0x4,
		0x0,
		0x1,
		0x20 | 0x4 | 0x2,
		0x2,
		0x10 | 0x2 | 0x1
	};

	public static final int[] WALL_UNCULL_FLAGS_0 = {0, 0, 2, 0, 0, 2, 1, 1, 0};
	public static final int[] WALL_UNCULL_FLAGS_1 = {2, 0, 0, 2, 0, 0, 0, 4, 4};
	public static final int[] WALL_UNCULL_FLAGS_2 = {0, 4, 4, 8, 0, 0, 8, 0, 0};
	public static final int[] WALL_UNCULL_FLAGS_3 = {1, 1, 0, 0, 0, 8, 0, 0, 8};

	private static final String getHSLStatement(int hsl) {
		return String.format("(%s << 10) | (%s << 7) | %s", (hsl >> 10), (hsl >> 7) & 0x7, hsl & 0x7F);
	}

	public static final int[] TEXTURE_HSL = {
		41, 39248, 41, 4643, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
		43086, 41, 41, 41, 41, 41, 41, 41, 8602, 41, 28992, 41, 41, 41, 41,
		41, 5056, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
		3131, 41, 41, 41
	};
	// @formatter:on

	public int[] vertexAMergeIndex = new int[10000];
	public int[] vertexBMergeIndex = new int[10000];
	public int normalMergeIndex;

	/* @formatter:off */
	public static final int[][] TILE_MASK_2D = {new int[16], {
		1, 1, 1, 1,
		1, 1, 1, 1,
		1, 1, 1, 1,
		1, 1, 1, 1
	}, {
		1, 0, 0, 0,
		1, 1, 0, 0,
		1, 1, 1, 0,
		1, 1, 1, 1
	}, {
		1, 1, 0, 0,
		1, 1, 0, 0,
		1, 0, 0, 0,
		1, 0, 0, 0
	}, {
		0, 0, 1, 1,
		0, 0, 1, 1,
		0, 0, 0, 1,
		0, 0, 0, 1
	}, {
		0, 1, 1, 1,
		0, 1, 1, 1,
		1, 1, 1, 1,
		1, 1, 1, 1
	}, {
		1, 1, 1, 0,
		1, 1, 1, 0,
		1, 1, 1, 1,
		1, 1, 1, 1
	}, {
		1, 1, 0, 0,
		1, 1, 0, 0,
		1, 1, 0, 0,
		1, 1, 0, 0
	}, {
		0, 0, 0, 0,
		0, 0, 0, 0,
		1, 0, 0, 0,
		1, 1, 0, 0
	}, {
		1, 1, 1, 1,
		1, 1, 1, 1,
		0, 1, 1, 1,
		0, 0, 1, 1
	}, {
		1, 1, 1, 1,
		1, 1, 0, 0,
		1, 0, 0, 0,
		1, 0, 0, 0
	}, {
		0, 0, 0, 0,
		0, 0, 1, 1,
		0, 1, 1, 1,
		0, 1, 1, 1
	}, {
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 1, 1, 0,
		1, 1, 1, 1
	}};

	public static final int[][] TILE_ROTATION_2D = {{
		0, 1, 2, 3,
		4, 5, 6, 7,
		8, 9, 10, 11,
		12, 13, 14, 15
	}, {
		12, 8, 4, 0,
		13, 9, 5, 1,
		14, 10, 6, 2,
		15, 11, 7, 3
	}, {
		15, 14, 13, 12,
		11, 10, 9, 8,
		7, 6, 5, 4,
		3, 2, 1, 0
	}, {
		3, 7, 11, 15,
		2, 6, 10, 14,
		1, 5, 9, 13,
		0, 4, 8, 12
	}};
	/* @formatter:on */

	public static boolean[][][][] visibilityMaps = new boolean[8][32][Scene.VIEW_DIAMETER + 1][Scene.VIEW_DIAMETER + 1];
	public static boolean[][] visibilityMap;
	public static int viewportCenterX;
	public static int viewportCenterY;
	public static int viewportLeft;
	public static int viewportTop;
	public static int viewportRight;
	public static int viewportBottom;

	public SceneGraph(int width, int length, int height, int[][][] heightmap) {
		maxPlane = height;
		tileCountX = width;
		tileCountZ = length;
		planeTiles = new Tile[height][width][length];
		levelTileCycle = new int[height][width + 1][length + 1];
		this.heightmap = heightmap;
		reset();
	}

	public static void unload() {
		locationBuffer = null;
		planeOccluderCount = null;
		planeOccluders = null;
		tileQueue = null;
		visibilityMaps = null;
		visibilityMap = null;
	}

	public void reset() {
		for (int plane = 0; plane < maxPlane; plane++) {
			for (int x = 0; x < tileCountX; x++) {
				for (int z = 0; z < tileCountZ; z++) {
					planeTiles[plane][x][z] = null;
				}
			}
		}

		for (int p = 0; p < MAX_OCCLUDER_PLANES; p++) {
			for (int n = 0; n < planeOccluderCount[p]; n++) {
				planeOccluders[p][n] = null;
			}
			planeOccluderCount[p] = 0;
		}

		for (int i = 0; i < locCount; i++) {
			locs[i] = null;
		}

		locCount = 0;

		for (int i = 0; i < locationBuffer.length; i++) {
			locationBuffer[i] = null;
		}
	}

	public void setup(int plane) {
		this.minPlane = plane;
		for (int x = 0; x < tileCountX; x++) {
			for (int z = 0; z < tileCountZ; z++) {
				planeTiles[plane][x][z] = new Tile(plane, x, z);
			}
		}
	}

	public void setBridge(int x, int z) {
		Tile t = planeTiles[0][x][z];
		for (int plane = 0; plane < 3; plane++) {
			planeTiles[plane][x][z] = planeTiles[plane + 1][x][z];
			if (planeTiles[plane][x][z] != null) {
				planeTiles[plane][x][z].plane--;
			}
		}
		if (planeTiles[0][x][z] == null) {
			planeTiles[0][x][z] = new Tile(0, x, z);
		}
		planeTiles[0][x][z].bridge = t;
		planeTiles[3][x][z] = null;
	}

	public static void addOccluder(int type, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int plane) {
		Occluder o = new Occluder();
		o.minTileX = minX / 128;
		o.maxTileX = maxX / 128;
		o.minTileZ = minZ / 128;
		o.maxTileZ = maxZ / 128;
		o.type = type;
		o.minX = minX;
		o.maxX = maxX;
		o.minZ = minZ;
		o.maxZ = maxZ;
		o.minY = minY;
		o.maxY = maxY;
		planeOccluders[plane][planeOccluderCount[plane]++] = o;
	}

	public void setTileDrawPlane(int tilePlane, int tileX, int tileZ, int drawPlane) {
		Tile t = planeTiles[tilePlane][tileX][tileZ];
		if (t != null) {
			planeTiles[tilePlane][tileX][tileZ].drawPlane = drawPlane;
		}
	}

	public void addTile(int tilePlane, int tileX, int tileZ, int type, int rotation, int textureIndex, int southwestY, int southeastY, int northeastY, int northwestY, int southwestColor1, int southeastColor1, int northeastColor1, int northwestColor1, int southwestColor2, int southeastColor2, int northeastColor2, int northwestColor2, int rgb0, int rgb1) {
		if (type == 0) {
			TileUnderlay t = new TileUnderlay(southwestColor1, southeastColor1, northeastColor1, northwestColor1, -1, rgb0, false);

			for (int y = tilePlane; y >= 0; y--) {
				if (planeTiles[y][tileX][tileZ] == null) {
					planeTiles[y][tileX][tileZ] = new Tile(y, tileX, tileZ);
				}
			}

			planeTiles[tilePlane][tileX][tileZ].underlay = t;
		} else if (type == 1) {
			TileUnderlay t = new TileUnderlay(southwestColor2, southeastColor2, northeastColor2, northwestColor2, textureIndex, rgb1, (southwestY == southeastY && southwestY == northeastY && southwestY == northwestY));

			for (int p = tilePlane; p >= 0; p--) {
				if (planeTiles[p][tileX][tileZ] == null) {
					planeTiles[p][tileX][tileZ] = new Tile(p, tileX, tileZ);
				}
			}

			planeTiles[tilePlane][tileX][tileZ].underlay = t;
		} else {
			TileOverlay t = new TileOverlay(tileX, tileZ, northwestY, northeastY, southwestY, textureIndex, southwestColor1, southeastColor2, rotation, northeastColor1, northeastColor2, southwestColor2, northwestColor1, southeastY, southeastColor1, type, northwestColor2, (byte) -119, rgb0, rgb1);

			for (int p = tilePlane; p >= 0; p--) {
				if (planeTiles[p][tileX][tileZ] == null) {
					planeTiles[p][tileX][tileZ] = new Tile(p, tileX, tileZ);
				}
			}

			planeTiles[tilePlane][tileX][tileZ].overlay = t;
		}
	}

	public void addGroundDecoration(Model m, int tilePlane, int tileX, int tileZ, int sceneY, byte info, int bitset) {
		if (m == null) {
			return;
		}

		GroundDecorationLocation l = new GroundDecorationLocation();
		l.model = m;
		l.sceneX = tileX * 128 + 64;
		l.sceneZ = tileZ * 128 + 64;
		l.sceneY = sceneY;
		l.bitset = bitset;
		l.info = info;

		if (planeTiles[tilePlane][tileX][tileZ] == null) {
			planeTiles[tilePlane][tileX][tileZ] = new Tile(tilePlane, tileX, tileZ);
		}

		planeTiles[tilePlane][tileX][tileZ].groundDecoration = l;
	}

	public void addObject(Model m, int tilePlane, int tileX, int tileZ, int sceneY, int bitset) {
		ObjectLocation l = new ObjectLocation();
		l.model = m;
		l.sceneX = tileX * 128 + 64;
		l.sceneZ = tileZ * 128 + 64;
		l.sceneY = sceneY;
		l.bitset = bitset;

		int maxY = 0;
		Tile t = planeTiles[tilePlane][tileX][tileZ];
		if (t != null) {
			for (int n = 0; n < t.locationCount; n++) {
				int y = t.locs[n].model.objectOffsetY;
				if (y > maxY) {
					maxY = y;
				}
			}
		}

		l.offsetY = maxY;

		if (planeTiles[tilePlane][tileX][tileZ] == null) {
			planeTiles[tilePlane][tileX][tileZ] = new Tile(tilePlane, tileX, tileZ);
		}

		planeTiles[tilePlane][tileX][tileZ].object = l;
	}

	public void addWall(Model m1, Model m2, int tilePlane, int tileX, int tileZ, int sceneY, int bitset, byte info, int type1, int type2) {
		if (m1 != null || m2 != null) {
			WallLocation l = new WallLocation();
			l.bitset = bitset;
			l.info = info;
			l.sceneX = tileX * 128 + 64;
			l.sceneZ = tileZ * 128 + 64;
			l.sceneY = sceneY;
			l.model1 = m1;
			l.model2 = m2;
			l.type1 = type1;
			l.type2 = type2;

			for (int plane = tilePlane; plane >= 0; plane--) {
				if (planeTiles[plane][tileX][tileZ] == null) {
					planeTiles[plane][tileX][tileZ] = new Tile(plane, tileX, tileZ);
				}
			}

			planeTiles[tilePlane][tileX][tileZ].wall = l;
		}
	}

	public void addWallDecoration(Model model, int tileX, int tileZ, int sceneY, int sizeX, int sizeY, int tilePlane, int bitset, byte flags, int type, int rotation) {
		if (model != null) {
			WallDecorationLocation l = new WallDecorationLocation();
			l.bitset = bitset;
			l.info = flags;
			l.sceneX = (tileX * 128) + 64 + sizeX;
			l.sceneZ = (tileZ * 128) + 64 + sizeY;
			l.sceneY = sceneY;
			l.model = model;
			l.type = type;
			l.rotation = rotation;

			for (int plane = tilePlane; plane >= 0; plane--) {
				if (planeTiles[plane][tileX][tileZ] == null) {
					planeTiles[plane][tileX][tileZ] = new Tile(plane, tileX, tileZ);
				}
			}

			planeTiles[tilePlane][tileX][tileZ].wallDecoration = l;
		}
	}

	public boolean addTemporaryLoc(Model m, Renderable r, int minTileX, int minTileZ, int tileSizeX, int tileSizeZ, int sceneY, int plane, int yaw, int bitset, byte flags) {
		if (m == null && r == null) {
			return true;
		}
		int sceneX = (minTileX * 128) + (tileSizeX * 64);
		int sceneZ = (minTileZ * 128) + (tileSizeZ * 64);
		return add(r, m, minTileX, minTileZ, tileSizeX, tileSizeZ, plane, sceneX, sceneZ, sceneY, yaw, bitset, flags, true);
	}

	public boolean addLocation(Model m, Renderable r, int minTileX, int minTileZ, int tileSizeX, int tileSizeZ, int sceneY, int plane, int yaw, int bitset, byte flags) {
		if (m == null && r == null) {
			return true;
		}
		int sceneX = (minTileX * 128) + (tileSizeX * 64);
		int sceneZ = (minTileZ * 128) + (tileSizeZ * 64);
		return add(r, m, minTileX, minTileZ, tileSizeX, tileSizeZ, plane, sceneX, sceneZ, sceneY, yaw, bitset, flags, false);
	}

	public boolean add(Renderable r, Model m, int sceneX, int sceneY, int sceneZ, int plane, int yaw, int size, int bitset, int renderPadding) {
		if (m == null && r == null) {
			return true;
		}

		int minX = sceneX - size;
		int minZ = sceneZ - size;
		int maxX = sceneX + size;
		int maxZ = sceneZ + size;

		if (renderPadding > 0) {
			if (yaw > 768 && yaw < 1280) {
				maxZ += renderPadding;
			}

			if (yaw > 1280 && yaw < 1792) {
				maxX += renderPadding;
			}

			if (yaw > 1792 || yaw < 256) {
				minZ -= renderPadding;
			}

			if (yaw > 256 && yaw < 768) {
				maxX -= renderPadding;
			}
		}

		minX /= 128;
		minZ /= 128;
		maxX /= 128;
		maxZ /= 128;

		return add(r, m, minX, minZ, maxX - minX + 1, maxZ - minZ + 1, plane, sceneX, sceneZ, sceneY, yaw, bitset, (byte) 0, true);
	}

	public boolean add(Renderable r, Model m, int sceneX, int sceneY, int sceneZ, int minTileX, int minTileZ, int maxTileX, int maxTileZ, int tileZ, int yaw, int bitset) {
		if (m == null && r == null) {
			return true;
		}
		return add(r, m, minTileX, minTileZ, maxTileX - minTileX + 1, maxTileZ - minTileZ + 1, tileZ, sceneX, sceneZ, sceneY, yaw, bitset, (byte) 0, true);
	}

	public boolean add(Renderable r, Model m, int tileX, int tileZ, int sizeX, int sizeZ, int tilePlane, int sceneX, int sceneZ, int sceneY, int yaw, int bitset, byte info, boolean temporary) {
		if (m == null && r == null) {
			return false;
		}

		for (int x = tileX; x < tileX + sizeX; x++) {
			for (int z = tileZ; z < tileZ + sizeZ; z++) {
				if (x < 0 || z < 0 || x >= tileCountX || z >= tileCountZ) {
					return false;
				}

				Tile t = planeTiles[tilePlane][x][z];

				if (t != null && t.locationCount >= 5) {
					return false;
				}
			}
		}

		Location l = new Location();
		l.bitset = bitset;
		l.info = info;
		l.tilePlane = tilePlane;
		l.sceneX = sceneX;
		l.sceneZ = sceneZ;
		l.sceneY = sceneY;
		l.model = m;
		l.renderable = r;
		l.yaw = yaw;
		l.minTileX = tileX;
		l.minTileZ = tileZ;
		l.maxTileX = tileX + sizeX - 1;
		l.maxTileZ = tileZ + sizeZ - 1;

		for (int x = tileX; x < tileX + sizeX; x++) {
			for (int z = tileZ; z < tileZ + sizeZ; z++) {
				int flags = 0;

				if (x > tileX) {
					flags++;
				}

				if (x < tileX + sizeX - 1) {
					flags += 4;
				}

				if (z > tileZ) {
					flags += 8;
				}

				if (z < tileZ + sizeZ - 1) {
					flags += 2;
				}

				for (int plane = tilePlane; plane >= 0; plane--) {
					if (planeTiles[plane][x][z] == null) {
						planeTiles[plane][x][z] = new Tile(plane, x, z);
					}
				}

				Tile t = planeTiles[tilePlane][x][z];
				t.locs[t.locationCount] = l;
				t.locFlags[t.locationCount] = flags;
				t.flags |= flags;
				t.locationCount++;
			}
		}

		if (temporary) {
			locs[locCount++] = l;
		}
		return true;
	}

	public void clearFrameLocs() {
		for (int i = 0; i < locCount; i++) {
			Location l = locs[i];
			removeLocation(l);
			locs[i] = null;
		}
		locCount = 0;
	}

	private void removeLocation(Location l) {
		for (int x = l.minTileX; x <= l.maxTileX; x++) {
			for (int z = l.minTileZ; z <= l.maxTileZ; z++) {
				Tile t = planeTiles[l.tilePlane][x][z];

				if (t != null) {
					for (int n = 0; n < t.locationCount; n++) {
						if (t.locs[n] == l) {
							t.locationCount--;

							// shift all locs on this tile down an index
							for (int m = n; m < t.locationCount; m++) {
								t.locs[m] = t.locs[m + 1];
								t.locFlags[m] = t.locFlags[m + 1];
							}

							// then remove the reference to l
							t.locs[t.locationCount] = null;
							break;
						}
					}

					// clear flags
					t.flags = 0;

					// re-evaluate flags
					for (int n = 0; n < t.locationCount; n++) {
						t.flags |= t.locFlags[n];
					}
				}
			}
		}
	}

	public void setLocModel(Model m, int x, int z, int plane) {
		if (m != null) {
			Tile t = planeTiles[plane][x][z];
			if (t != null) {
				for (int n = 0; n < t.locationCount; n++) {
					Location l = t.locs[n];
					if ((l.bitset >> 29 & 0x3) == 2) {
						l.model = m;
						return;
					}
				}
			}
		}
	}

	public void setWallDecorationModel(Model m, int x, int z, int plane) {
		if (m != null) {
			Tile t = planeTiles[plane][x][z];
			if (t != null) {
				WallDecorationLocation l = t.wallDecoration;
				if (l != null) {
					l.model = m;
				}
			}
		}
	}

	public void removeWall(int x, int z, int plane) {
		Tile t = planeTiles[plane][x][z];
		if (t != null) {
			t.wall = null;
		}
	}

	public void removeWallDecoration(int x, int z, int plane) {
		Tile t = planeTiles[plane][x][z];
		if (t != null) {
			t.wallDecoration = null;
		}
	}

	public void removeLocations(int x, int z, int plane) {
		Tile t = planeTiles[plane][x][z];
		if (t != null) {
			for (int n = 0; n < t.locationCount; n++) {
				Location l = t.locs[n];
				if (l.bitset >> 29 == 2 && l.minTileX == x && l.minTileZ == z) {
					removeLocation(l);
					return;
				}
			}
		}
	}

	public void removeGroundDecoration(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t != null) {
			t.groundDecoration = null;
		}
	}

	public void removeObject(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t != null) {
			t.object = null;
		}
	}

	public int getBitset(int type, int x, int z, int y) {
		if (type == LocationInfo.CLASS_WALL) {
			return getWallBitset(x, z, y);
		} else if (type == LocationInfo.CLASS_WALL_DECORATION) {
			return getWallDecorationBitset(x, z, y);
		} else if (type == LocationInfo.CLASS_NORMAL) {
			return getLocationBitset(x, z, y);
		} else if (type == LocationInfo.CLASS_GROUND_DECORATION) {
			return getGroundDecorationBitset(x, z, y);
		}
		return 0;
	}

	public int getWallBitset(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t == null || t.wall == null) {
			return 0;
		}
		return t.wall.bitset;
	}

	public int getWallDecorationBitset(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t == null || t.wallDecoration == null) {
			return 0;
		}
		return t.wallDecoration.bitset;
	}

	public int getLocationBitset(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t == null) {
			return 0;
		}
		for (int n = 0; n < t.locationCount; n++) {
			Location l = t.locs[n];
			if ((l.bitset >> 29 & 0x3) == 2 && l.minTileX == x && l.minTileZ == z) {
				return l.bitset;
			}
		}
		return 0;
	}

	public int getGroundDecorationBitset(int x, int z, int y) {
		Tile t = planeTiles[y][x][z];
		if (t == null || t.groundDecoration == null) {
			return 0;
		}
		return t.groundDecoration.bitset;
	}

	public int getInfo(int x, int z, int y, int bitset) {
		Tile t = planeTiles[y][x][z];

		if (t == null) {
			return -1;
		}

		if (t.wall != null && t.wall.bitset == bitset) {
			return t.wall.info & 0xFF;
		}

		if (t.wallDecoration != null && t.wallDecoration.bitset == bitset) {
			return t.wallDecoration.info & 0xFF;
		}

		if (t.groundDecoration != null && t.groundDecoration.bitset == bitset) {
			return t.groundDecoration.info & 0xFF;
		}

		for (int n = 0; n < t.locationCount; n++) {
			if (t.locs[n].bitset == bitset) {
				return t.locs[n].info & 0xFF;
			}
		}
		return -1;
	}

	public void applyLighting(int lightX, int lightY, int lightZ, int lightness, int baseIntensity) {
		int length = (int) Math.sqrt((double) (lightX * lightX + lightY * lightY + lightZ * lightZ));
		int intensity = (baseIntensity * length) >> 8;

		for (int plane = 0; plane < maxPlane; plane++) {
			for (int tileX = 0; tileX < tileCountX; tileX++) {
				for (int tileZ = 0; tileZ < tileCountZ; tileZ++) {
					Tile t = planeTiles[plane][tileX][tileZ];

					if (t != null) {
						WallLocation w = t.wall;

						if (w != null && w.model1 != null && w.model1.normals != null) {
							mergeLocNormals(w.model1, tileX, tileZ, plane, 1, 1);

							if (w.model2 != null && w.model2.normals != null) {
								mergeLocNormals(w.model2, tileX, tileZ, plane, 1, 1);
								mergeNormals(w.model1, w.model2, 0, 0, 0, false);
								w.model2.calculateLighting(lightness, intensity, lightX, lightY, lightZ);
							}

							w.model1.calculateLighting(lightness, intensity, lightX, lightY, lightZ);
						}

						for (int n = 0; n < t.locationCount; n++) {
							Location l = t.locs[n];

							if (l != null && l.model != null && l.model.normals != null) {
								mergeLocNormals(l.model, tileX, tileZ, plane, (l.maxTileX - l.minTileX + 1), (l.maxTileZ - l.minTileZ + 1));
								l.model.calculateLighting(lightness, intensity, lightX, lightY, lightZ);
							}
						}

						GroundDecorationLocation d = t.groundDecoration;

						if (d != null && d.model.normals != null) {
							mergeGroundDecorationNormals(d.model, tileX, tileZ, plane);
							d.model.calculateLighting(lightness, intensity, lightX, lightY, lightZ);
						}
					}
				}
			}
		}
	}

	private void mergeGroundDecorationNormals(Model m, int x, int z, int y) {
		if (x < tileCountX) {
			Tile t = planeTiles[y][x + 1][z];

			if (t != null && t.groundDecoration != null && t.groundDecoration.model.normals != null) {
				mergeNormals(m, t.groundDecoration.model, 128, 0, 0, true);
			}
		}

		if (z < tileCountZ) {
			Tile t = planeTiles[y][x][z + 1];

			if (t != null && t.groundDecoration != null && t.groundDecoration.model.normals != null) {
				mergeNormals(m, t.groundDecoration.model, 0, 0, 128, true);
			}
		}

		if (x < tileCountX && z < tileCountZ) {
			Tile t = planeTiles[y][x + 1][z + 1];

			if (t != null && t.groundDecoration != null && t.groundDecoration.model.normals != null) {
				mergeNormals(m, t.groundDecoration.model, 128, 0, 128, true);
			}
		}

		if (x < tileCountX && z > 0) {
			Tile t = planeTiles[y][x + 1][z - 1];

			if (t != null && t.groundDecoration != null && t.groundDecoration.model.normals != null) {
				mergeNormals(m, t.groundDecoration.model, 128, 0, -128, true);
			}
		}
	}

	private void mergeLocNormals(Model m, int tileX, int tileZ, int tilePlane, int locTileSizeX, int locTileSizeZ) {
		boolean hideTriangles = true;

		int minTileX = tileX;
		int maxTileX = tileX + locTileSizeX;

		int minTileZ = tileZ - 1;
		int maxTileZ = tileZ + locTileSizeZ;

		int baseAverageY = (heightmap[tilePlane][tileX][tileZ] + heightmap[tilePlane][tileX + 1][tileZ] + heightmap[tilePlane][tileX][tileZ + 1] + heightmap[tilePlane][tileX + 1][tileZ + 1]) / 4;

		for (int plane = tilePlane; plane <= tilePlane + 1; plane++) {
			if (plane == maxPlane) {
				continue;
			}

			for (int x = minTileX; x <= maxTileX; x++) {
				if (x < 0 || x >= tileCountX) {
					continue;
				}

				for (int z = minTileZ; z <= maxTileZ; z++) {
					if (z < 0 || z >= tileCountZ) {
						continue;
					}

					if (!hideTriangles || x >= maxTileX || z >= maxTileZ || z < tileZ && x != tileX) {
						Tile t = planeTiles[plane][x][z];

						if (t != null) {
							int averageY = ((heightmap[plane][x][z] + heightmap[plane][x + 1][z] + heightmap[plane][x][z + 1] + heightmap[plane][x + 1][z + 1]) / 4) - baseAverageY;

							WallLocation wall = t.wall;

							if (wall != null && wall.model1 != null && wall.model1.normals != null) {
								mergeNormals(m, wall.model1, ((x - tileX) * 128 + (1 - locTileSizeX) * 64), averageY, ((z - tileZ) * 128 + (1 - locTileSizeZ) * 64), hideTriangles);
							}

							if (wall != null && wall.model2 != null && wall.model2.normals != null) {
								mergeNormals(m, wall.model2, ((x - tileX) * 128 + (1 - locTileSizeX) * 64), averageY, ((z - tileZ) * 128 + (1 - locTileSizeZ) * 64), hideTriangles);
							}

							for (int n = 0; n < t.locationCount; n++) {
								Location l = t.locs[n];

								if (l != null && l.model != null && l.model.normals != null) {
									int tileSizeX = (l.maxTileX - l.minTileX + 1);
									int tileSizeZ = (l.maxTileZ - l.minTileZ + 1);
									mergeNormals(m, l.model, (((l.minTileX - tileX) * 128) + (tileSizeX - locTileSizeX) * 64), averageY, (((l.minTileZ - tileZ) * 128) + (tileSizeZ - locTileSizeZ) * 64), hideTriangles);
								}
							}
						}
					}
				}
			}

			minTileX--;
			hideTriangles = false;
		}
	}

	private void mergeNormals(Model a, Model b, int offsetX, int offsetY, int offsetZ, boolean hideTriangles) {
		this.normalMergeIndex++;
		int counter = 0;

		for (int vertexA = 0; vertexA < a.vertexCount; vertexA++) {
			Normal normalA = a.normals[vertexA];
			Normal unmodifiedNormalA = a.unmodifiedNormals[vertexA];

			if (unmodifiedNormalA.magnitude == 0) {
				continue;
			}

			int vertexYA = a.vertexY[vertexA] - offsetY;

			if (vertexYA > b.minBoundY) {
				continue;
			}

			int vertexXA = a.vertexX[vertexA] - offsetX;

			if (vertexXA < b.minBoundX || vertexXA > b.maxBoundX) {
				continue;
			}

			int vertexZA = a.vertexZ[vertexA] - offsetZ;

			if (vertexZA < b.minBoundZ || vertexZA > b.maxBoundZ) {
				continue;
			}

			for (int vertexB = 0; vertexB < b.vertexCount; vertexB++) {

				Normal normalB = b.normals[vertexB];
				Normal unmodifiedNormalB = b.unmodifiedNormals[vertexB];

				if (vertexXA == b.vertexX[vertexB] && vertexZA == b.vertexZ[vertexB] && vertexYA == b.vertexY[vertexB] && unmodifiedNormalB.magnitude != 0) {
					normalA.x += unmodifiedNormalB.x;
					normalA.y += unmodifiedNormalB.y;
					normalA.z += unmodifiedNormalB.z;
					normalA.magnitude += unmodifiedNormalB.magnitude;

					normalB.x += unmodifiedNormalA.x;
					normalB.y += unmodifiedNormalA.y;
					normalB.z += unmodifiedNormalA.z;
					normalB.magnitude += unmodifiedNormalA.magnitude;

					counter++;
					this.vertexAMergeIndex[vertexA] = this.normalMergeIndex;
					this.vertexBMergeIndex[vertexB] = this.normalMergeIndex;
				}
			}
		}

		if (counter >= 3 && hideTriangles) {
			for (int t = 0; t < a.triangleCount; t++) {
				if (this.vertexAMergeIndex[a.triangleVertexA[t]] == this.normalMergeIndex && this.vertexAMergeIndex[a.triangleVertexB[t]] == this.normalMergeIndex && this.vertexAMergeIndex[a.triangleVertexC[t]] == this.normalMergeIndex) {
					a.triangleInfo[t] = -1; // do not draw this triangle
				}
			}

			for (int t = 0; t < b.triangleCount; t++) {
				if (this.vertexBMergeIndex[b.triangleVertexA[t]] == this.normalMergeIndex && this.vertexBMergeIndex[b.triangleVertexB[t]] == this.normalMergeIndex && this.vertexBMergeIndex[b.triangleVertexC[t]] == this.normalMergeIndex) {
					b.triangleInfo[t] = -1;// do not draw this triangle
				}
			}
		}
	}

	public void drawMinimapTile(int[] dst, int dstOff, int dstStep, int plane, int x, int z) {
		Tile t = planeTiles[plane][x][z];

		if (t != null) {
			TileUnderlay underlay = t.underlay;

			if (underlay != null) {
				int rgb = underlay.rgb;
				if (rgb != 0) {
					for (int i = 0; i < 4; i++) {
						dst[dstOff] = rgb;
						dst[dstOff + 1] = rgb;
						dst[dstOff + 2] = rgb;
						dst[dstOff + 3] = rgb;
						dstOff += dstStep;
					}
				}
			} else {
				TileOverlay overlay = t.overlay;

				if (overlay != null) {
					int shape = overlay.shape;
					int rotation = overlay.rotation;
					int underlayRGB = overlay.underlayRGB;
					int overlayRGB = overlay.overlayRGB;
					int[] mask = TILE_MASK_2D[shape];
					int[] rotated = TILE_ROTATION_2D[rotation];
					int srcOff = 0;

					if (underlayRGB != 0) {
						for (int i = 0; i < 4; i++) {
							dst[dstOff] = (mask[rotated[srcOff++]] == 0 ? underlayRGB : overlayRGB);
							dst[dstOff + 1] = (mask[rotated[srcOff++]] == 0 ? underlayRGB : overlayRGB);
							dst[dstOff + 2] = (mask[rotated[srcOff++]] == 0 ? underlayRGB : overlayRGB);
							dst[dstOff + 3] = (mask[rotated[srcOff++]] == 0 ? underlayRGB : overlayRGB);
							dstOff += dstStep;
						}
					} else {
						for (int n = 0; n < 4; n++) {
							if (mask[rotated[srcOff++]] != 0) {
								dst[dstOff] = overlayRGB;
							}
							if (mask[rotated[srcOff++]] != 0) {
								dst[dstOff + 1] = overlayRGB;
							}
							if (mask[rotated[srcOff++]] != 0) {
								dst[dstOff + 2] = overlayRGB;
							}
							if (mask[rotated[srcOff++]] != 0) {
								dst[dstOff + 3] = overlayRGB;
							}
							dstOff += dstStep;
						}
					}
				}
			}
		}
	}

	public static void init(int width, int height, int minZ, int maxZ) {
		viewportLeft = 0;
		viewportTop = 0;
		viewportRight = width;
		viewportBottom = height;
		viewportCenterX = width / 2;
		viewportCenterY = height / 2;

		int[] pitchZ = new int[9];

		for (int n = 0; n < 9; n++) {
			int angle = (n * 32) + 128 + 15;
			int zoom = (angle * 3) + 600;
			pitchZ[n] = (zoom * Graphics3D.sin[angle]) >> 16;
		}

		// some padding?
		final int diameter = Scene.VIEW_DIAMETER + 3;
		final int radius = Scene.VIEW_DIAMETER / 2;

		boolean[][][][] tileVisible = new boolean[9][32][diameter][diameter];

		for (int pitch = 128; pitch <= 384; pitch += 32) {
			for (int yaw = 0; yaw < 2048; yaw += 64) {
				pitchSin = Model.sin[pitch];
				pitchCos = Model.cos[pitch];
				yawSin = Model.sin[yaw];
				yawCos = Model.cos[yaw];

				int pitchIndex = (pitch - 128) / 32;
				int yawIndex = yaw / 64;

				for (int x = -radius; x <= radius; x++) {
					for (int y = -radius; y <= radius; y++) {
						int sceneX = x * 128;
						int sceneY = y * 128;
						boolean visible = false;

						for (int sceneZ = -minZ; sceneZ <= maxZ; sceneZ += 128) {
							if (isPointVisible(sceneX, sceneY, pitchZ[pitchIndex] + sceneZ)) {
								visible = true;
								break;
							}
						}

						tileVisible[pitchIndex][yawIndex][x + Scene.VIEW_RADIUS + 1][y + Scene.VIEW_RADIUS + 1] = visible;
					}
				}
			}
		}

		for (int pitch = 0; pitch < 8; pitch++) {
			for (int yaw = 0; yaw < 32; yaw++) {
				for (int x = -Scene.VIEW_RADIUS; x < Scene.VIEW_RADIUS; x++) {
					for (int y = -Scene.VIEW_RADIUS; y < Scene.VIEW_RADIUS; y++) {
						boolean visible = false;

						LOOP:
						{
							for (int dx = -1; dx <= 1; dx++) {
								for (int dy = -1; dy <= 1; dy++) {
									if (tileVisible[pitch][yaw][x + dx + Scene.VIEW_RADIUS + 1][y + dy + Scene.VIEW_RADIUS + 1]) {
										visible = true;
										break LOOP;
									}

									if (tileVisible[pitch][(yaw + 1) % 31][x + dx + Scene.VIEW_RADIUS + 1][y + dy + Scene.VIEW_RADIUS + 1]) {
										visible = true;
										break LOOP;
									}

									if (tileVisible[pitch + 1][yaw][x + dx + Scene.VIEW_RADIUS + 1][y + dy + Scene.VIEW_RADIUS + 1]) {
										visible = true;
										break LOOP;
									}

									if (tileVisible[pitch + 1][(yaw + 1) % 31][x + dx + Scene.VIEW_RADIUS + 1][y + dy + Scene.VIEW_RADIUS + 1]) {
										visible = true;
										break LOOP;
									}
								}
							}
						}

						SceneGraph.visibilityMaps[pitch][yaw][x + Scene.VIEW_RADIUS][y + Scene.VIEW_RADIUS] = visible;
					}
				}
			}
		}
	}

	public static boolean isPointVisible(int sceneX, int sceneY, int sceneZ) {
		int x = sceneY * yawSin + sceneX * yawCos >> 16;
		int w = sceneY * yawCos - sceneX * yawSin >> 16;
		int z = sceneZ * pitchSin + w * pitchCos >> 16;
		int y = sceneZ * pitchCos - w * pitchSin >> 16;

		if (z < Scene.NEAR_Z || z > Scene.FAR_Z) {
			return false;
		}

		int screenX = viewportCenterX + (x << 9) / z;
		int screenY = viewportCenterY + (y << 9) / z;

		return !(screenX < viewportLeft || screenX > viewportRight || screenY < viewportTop || screenY > viewportBottom);
	}

	public void sendClick(int clickX, int clickY) {
		Scene.checkClick = true;
		Scene.clickX = clickX;
		Scene.clickY = clickY;
		Scene.clickedTileX = -1;
		Scene.clickedTileZ = -1;
	}

	public void draw(int cameraX, int cameraY, int cameraZ, int pitch, int yaw, int topPlane) {
		if (cameraX < 0) {
			cameraX = 0;
		} else if (cameraX >= tileCountX * 128) {
			cameraX = tileCountX * 128 - 1;
		}

		if (cameraZ < 0) {
			cameraZ = 0;
		} else if (cameraZ >= tileCountZ * 128) {
			cameraZ = tileCountZ * 128 - 1;
		}

		cycle++;
		SceneGraph.pitchSin = Model.sin[pitch];
		SceneGraph.pitchCos = Model.cos[pitch];
		SceneGraph.yawSin = Model.sin[yaw];
		SceneGraph.yawCos = Model.cos[yaw];
		SceneGraph.visibilityMap = visibilityMaps[(pitch - 128) / 32][yaw / 64];
		SceneGraph.cameraX = cameraX;
		SceneGraph.cameraY = cameraY;
		SceneGraph.cameraZ = cameraZ;
		SceneGraph.cameraTileX = cameraX / 128;
		SceneGraph.cameraTileZ = cameraZ / 128;
		SceneGraph.topPlane = topPlane;

		minTileX = cameraTileX - Scene.VIEW_RADIUS;
		maxTileX = cameraTileX + Scene.VIEW_RADIUS;

		minTileZ = cameraTileZ - Scene.VIEW_RADIUS;
		maxTileZ = cameraTileZ + Scene.VIEW_RADIUS;

		if (minTileX < 0) {
			minTileX = 0;
		}

		if (minTileZ < 0) {
			minTileZ = 0;
		}

		if (maxTileX > tileCountX) {
			maxTileX = tileCountX;
		}

		if (maxTileZ > tileCountZ) {
			maxTileZ = tileCountZ;
		}

		updateOccluders();

		tileUpdateCount = 0;

		for (int plane = minPlane; plane < maxPlane; plane++) {
			Tile[][] tiles = planeTiles[plane];

			for (int x = minTileX; x < maxTileX; x++) {
				for (int z = minTileZ; z < maxTileZ; z++) {
					Tile t = tiles[x][z];

					if (t != null) {
						if (t.drawPlane > topPlane || !visibilityMap[x - cameraTileX + Scene.VIEW_RADIUS][z - cameraTileZ + Scene.VIEW_RADIUS] && heightmap[plane][x][z] - cameraY < 2000) {
							t.draw = false;
							t.isVisible = false;
							t.wallCullDirection = 0;
						} else {
							t.draw = true;
							t.isVisible = true;
							t.drawLocations = t.locationCount > 0;
							tileUpdateCount++;
						}
					}
				}
			}
		}

		lastTileUpdateCount = tileUpdateCount;

		for (int plane = minPlane; plane < maxPlane; plane++) {
			Tile[][] tiles = planeTiles[plane];

			for (int x = -Scene.VIEW_RADIUS; x <= 0; x++) {
				int x0 = cameraTileX + x;
				int x1 = cameraTileX - x;

				if (x0 >= minTileX || x1 < maxTileX) {
					for (int z = -Scene.VIEW_RADIUS; z <= 0; z++) {
						int z0 = cameraTileZ + z;
						int z1 = cameraTileZ - z;

						if (x0 >= minTileX) {
							if (z0 >= minTileZ) {
								Tile t = tiles[x0][z0];

								if (t != null && t.draw) {
									draw(t, true);
								}
							}

							if (z1 < maxTileZ) {
								Tile t = tiles[x0][z1];

								if (t != null && t.draw) {
									draw(t, true);
								}
							}
						}

						if (x1 < maxTileX) {
							if (z0 >= minTileZ) {
								Tile t = tiles[x1][z0];

								if (t != null && t.draw) {
									draw(t, true);
								}
							}

							if (z1 < maxTileZ) {
								Tile t = tiles[x1][z1];

								if (t != null && t.draw) {
									draw(t, true);
								}
							}
						}

						if (tileUpdateCount == 0) {
							Scene.checkClick = false;
							return;
						}
					}
				}
			}
		}

		for (int plane = minPlane; plane < maxPlane; plane++) {
			Tile[][] tiles = planeTiles[plane];

			for (int x = -Scene.VIEW_RADIUS; x <= 0; x++) {
				int x0 = cameraTileX + x;
				int x1 = cameraTileX - x;

				if (x0 >= minTileX || x1 < maxTileX) {
					for (int z = -Scene.VIEW_RADIUS; z <= 0; z++) {
						int z0 = cameraTileZ + z;
						int z1 = cameraTileZ - z;

						if (x0 >= minTileX) {
							if (z0 >= minTileZ) {
								Tile t = tiles[x0][z0];
								if (t != null && t.draw) {
									draw(t, false);
								}
							}
							if (z1 < maxTileZ) {
								Tile t = tiles[x0][z1];
								if (t != null && t.draw) {
									draw(t, false);
								}
							}
						}

						if (x1 < maxTileX) {
							if (z0 >= minTileZ) {
								Tile t = tiles[x1][z0];
								if (t != null && t.draw) {
									draw(t, false);
								}
							}
							if (z1 < maxTileZ) {
								Tile t = tiles[x1][z1];
								if (t != null && t.draw) {
									draw(t, false);
								}
							}
						}

						if (tileUpdateCount == 0) {
							Scene.checkClick = false;
							return;
						}
					}
				}
			}
		}
	}

	public void draw(Tile tile, boolean bool) {
		tileQueue.push(tile);

		for (;;) {
			tile = (Tile) tileQueue.poll();

			if (tile == null) {
				break;
			}

			if (!tile.isVisible) {
				continue;
			}

			int tileX = tile.x;
			int tileZ = tile.z;
			int tilePlane = tile.plane;
			int tileRenderPlane = tile.renderPlane;
			Tile[][] tiles = planeTiles[tilePlane];

			if (tile.draw) {
				if (bool) {
					if (tilePlane > 0) {
						Tile t = planeTiles[tilePlane - 1][tileX][tileZ];

						if (t != null && t.isVisible) {
							continue;
						}
					}

					if (tileX <= cameraTileX && tileX > minTileX) {
						Tile t = tiles[tileX - 1][tileZ];

						if (t != null && t.isVisible && (t.draw || ((tile.flags & 0x1) == 0))) {
							continue;
						}
					}

					if (tileX >= cameraTileX && tileX < maxTileX - 1) {
						Tile t = tiles[tileX + 1][tileZ];

						if (t != null && t.isVisible && (t.draw || ((tile.flags & 0x4) == 0))) {
							continue;
						}
					}

					if (tileZ <= cameraTileZ && tileZ > minTileZ) {
						Tile t = tiles[tileX][tileZ - 1];

						if (t != null && t.isVisible && (t.draw || ((tile.flags & 0x8) == 0))) {
							continue;
						}
					}

					if (tileZ >= cameraTileZ && tileZ < maxTileZ - 1) {
						Tile t = tiles[tileX][tileZ + 1];

						if (t != null && t.isVisible && (t.draw || ((tile.flags & 0x2) == 0))) {
							continue;
						}
					}
				} else {
					bool = true;
				}

				tile.draw = false;

				if (tile.bridge != null) {
					Tile t = tile.bridge;

					if (t.underlay != null) {
						drawTileUnderlay(t.underlay, 0, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
					} else if (t.overlay != null) {
						drawTileOverlay(t.overlay, 0, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
					} else {
						drawTileUnderlay(null, 0, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
					}

					WallLocation wall = t.wall;

					if (wall != null) {
						wall.model1.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
					}

					for (int n = 0; n < t.locationCount; n++) {
						Location loc = t.locs[n];

						if (loc != null) {
							Model m = loc.model;

							if (m == null) {
								m = loc.renderable.getDrawModel();
							}

							m.draw(loc.yaw, pitchSin, pitchCos, yawSin, yawCos, loc.sceneX - cameraX, loc.sceneY - cameraY, loc.sceneZ - cameraZ, loc.bitset);
						}
					}
				}

				boolean tileVisible = false;

				if (tile.underlay != null) {
					if (!isTileOccluded(tileRenderPlane, tileX, tileZ)) {
						tileVisible = true;
						drawTileUnderlay(tile.underlay, tileRenderPlane, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
					}
				} else if (tile.overlay != null) {
					if (!isTileOccluded(tileRenderPlane, tileX, tileZ)) {
						tileVisible = true;
						drawTileOverlay(tile.overlay, 0, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
					}
				} else {
					drawTileUnderlay(null, tileRenderPlane, tileX, tileZ, pitchSin, pitchCos, yawSin, yawCos);
				}

				int direction = 0;
				int tileWallDrawFlags = 0;
				WallLocation wall = tile.wall;
				WallDecorationLocation decoration = tile.wallDecoration;

				if (wall != null || decoration != null) {
					if (cameraTileX == tileX) {
						direction += 1;
					} else if (cameraTileX < tileX) {
						direction += 2;
					}

					if (cameraTileZ == tileZ) {
						direction += 3;
					} else if (cameraTileZ > tileZ) {
						direction += 6;
					}

					tileWallDrawFlags = TILE_WALL_DRAW_FLAGS_0[direction];
					tile.tileWallDrawFlags = TILE_WALL_DRAW_FLAGS_1[direction];

					// 0[0] = cameraTileX > tileX && cameraTileZ < tileZ
					// 0[1] = cameraTileX == tileX && cameraTileZ < tileZ
					// 2[2] = cameraTileX < tileX && cameraTileZ < tileZ
					// 0[3] = cameraTileX > tileX && cameratileZ == tileZ
					// 0[4] = cameraTileX == tileX && cameraTileZ == tileZ
					// 2[5] = cameraTileX < tileX && cameraTileZ == tileZ
					// 1[6] = cameraTileX > tileX && cameraTileZ > tileZ
					// 1[7] = cameraTileX == tileX && cameraTileZ > tileZ
					// 0[8] = cameraTileX < tileX && cameraTileZ > tileZ
				}

				if (wall != null) {
					if ((wall.type1 & WALL_DRAW_FLAGS[direction]) != 0) {
						// these are clearly all directional flags. But I need to figure out correct naming for them.
						//
						//
						// nibble = delta
						// 0001 = x > minX
						// 0010 = z < maxZ
						// 0100 = x < maxX
						// 1000 = z > minZ
						//
						// CAMERA_DELTA_FLAGS:
						//
						// 0[0] = cameraTileX > tileX && cameraTileZ < tileZ
						// 0[1] = cameraTileX == tileX && cameraTileZ < tileZ
						// 2[2] = cameraTileX < tileX && cameraTileZ < tileZ
						// 0[3] = cameraTileX > tileX && cameratileZ == tileZ
						// 0[4] = cameraTileX == tileX && cameraTileZ == tileZ
						// 2[5] = cameraTileX < tileX && cameraTileZ == tileZ
						// 1[6] = cameraTileX > tileX && cameraTileZ > tileZ
						// 1[7] = cameraTileX == tileX && cameraTileZ > tileZ
						// 0[8] = cameraTileX < tileX && cameraTileZ > tileZ
						//
						//

						if (wall.type1 == 16) {
							// the direction the wall can cull
							tile.wallCullDirection = 0b0011; // x > minX && z < maxZ

							// the directions the wall can't cull relative to the camera
							tile.wallUncullDirection = WALL_UNCULL_FLAGS_0[direction];

							// the direction the wall is culling
							tile.relativeWallCullDirection = 0b0011 - tile.wallUncullDirection;
						} else if (wall.type1 == 32) {
							tile.wallCullDirection = 0b0110; // x < maxX && z < maxZ
							tile.wallUncullDirection = WALL_UNCULL_FLAGS_1[direction];
							tile.relativeWallCullDirection = 0b0110 - tile.wallUncullDirection;
						} else if (wall.type1 == 64) {
							tile.wallCullDirection = 0b1100; // x < maxX && z > minZ
							tile.wallUncullDirection = WALL_UNCULL_FLAGS_2[direction];
							tile.relativeWallCullDirection = 0b1100 - tile.wallUncullDirection;
						} else {
							tile.wallCullDirection = 0b1001; // x > minX && z > minZ
							tile.wallUncullDirection = WALL_UNCULL_FLAGS_3[direction];
							tile.relativeWallCullDirection = 0b1001 - tile.wallUncullDirection;
						}
					} else {
						tile.wallCullDirection = 0b0000;
					}

					if ((wall.type1 & tileWallDrawFlags) != 0 && !isWallOccluded(tileRenderPlane, tileX, tileZ, wall.type1)) {
						wall.model1.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
					}

					if ((wall.type2 & tileWallDrawFlags) != 0 && !isWallOccluded(tileRenderPlane, tileX, tileZ, wall.type2)) {
						wall.model2.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
					}
				}

				if (decoration != null && !isOccluded(tileRenderPlane, tileX, tileZ, decoration.model.maxBoundY)) {
					if ((decoration.type & tileWallDrawFlags) != 0) {
						decoration.model.draw(decoration.rotation, pitchSin, pitchCos, yawSin, yawCos, decoration.sceneX - cameraX, decoration.sceneY - cameraY, decoration.sceneZ - cameraZ, decoration.bitset);
					} else if ((decoration.type & 0x300) != 0) {
						int sceneX = decoration.sceneX - cameraX;
						int sceneY = decoration.sceneY - cameraY;
						int sceneZ = decoration.sceneZ - cameraZ;
						int rotation = decoration.rotation;

						int x;

						if (rotation == 1 || rotation == 2) {
							x = -sceneX;
						} else {
							x = sceneX;
						}

						int z;

						if (rotation == 2 || rotation == 3) {
							z = -sceneZ;
						} else {
							z = sceneZ;
						}

						if ((decoration.type & 0x100) != 0 && z < x) {
							int drawX = sceneX + DECO_TYPE1_OFFSET_X[rotation];
							int drawZ = sceneZ + DECO_TYPE1_OFFSET_Z[rotation];
							decoration.model.draw(rotation * 512 + 256, pitchSin, pitchCos, yawSin, yawCos, drawX, sceneY, drawZ, decoration.bitset);
						}

						if ((decoration.type & 0x200) != 0 && z > x) {
							int drawX = sceneX + DECO_TYPE2_OFFSET_X[rotation];
							int drawZ = sceneZ + DECO_TYPE2_OFFSET_Z[rotation];
							decoration.model.draw(rotation * 512 + 1280 & 0x7ff, pitchSin, pitchCos, yawSin, yawCos, drawX, sceneY, drawZ, decoration.bitset);
						}
					}
				}

				if (tileVisible) {
					GroundDecorationLocation d = tile.groundDecoration;

					if (d != null) {
						d.model.draw(0, pitchSin, pitchCos, yawSin, yawCos, d.sceneX - cameraX, d.sceneY - cameraY, d.sceneZ - cameraZ, d.bitset);
					}

					ObjectLocation o = tile.object;

					if (o != null && o.offsetY == 0) {
						o.model.draw(0, pitchSin, pitchCos, yawSin, yawCos, o.sceneX - cameraX, o.sceneY - cameraY, o.sceneZ - cameraZ, o.bitset);
					}
				}

				int flags = tile.flags;

				if (flags != 0) {
					if (tileX < cameraTileX && (flags & 0x4) != 0) {
						Tile t = tiles[tileX + 1][tileZ];

						if (t != null && t.isVisible) {
							tileQueue.push(t);
						}
					}

					if (tileZ < cameraTileZ && (flags & 0x2) != 0) {
						Tile t = tiles[tileX][tileZ + 1];

						if (t != null && t.isVisible) {
							tileQueue.push(t);
						}
					}

					if (tileX > cameraTileX && (flags & 0x1) != 0) {
						Tile t = tiles[tileX - 1][tileZ];

						if (t != null && t.isVisible) {
							tileQueue.push(t);
						}
					}

					if (tileZ > cameraTileZ && (flags & 0x8) != 0) {
						Tile t = tiles[tileX][tileZ - 1];

						if (t != null && t.isVisible) {
							tileQueue.push(t);
						}
					}
				}
			}

			if (tile.wallCullDirection != 0) {
				boolean visible = true;

				for (int n = 0; n < tile.locationCount; n++) {
					if (tile.locs[n].cycle != cycle && ((tile.locFlags[n] & tile.wallCullDirection) == tile.wallUncullDirection)) {
						visible = false;
						break;
					}
				}

				if (visible) {
					WallLocation wall = tile.wall;

					if (!isWallOccluded(tileRenderPlane, tileX, tileZ, wall.type1)) {
						wall.model1.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
					}

					tile.wallCullDirection = 0;
				}
			}

			if (tile.drawLocations) {
				int count = tile.locationCount;
				tile.drawLocations = false;
				int locationBufferSize = 0;

				LOOP:
				for (int n = 0; n < count; n++) {
					Location l = tile.locs[n];

					// this location hasn't been drawn this cycle yet.
					if (l.cycle != cycle) {
						for (int x = l.minTileX; x <= l.maxTileX; x++) {
							for (int z = l.minTileZ; z <= l.maxTileZ; z++) {
								Tile t = tiles[x][z];

								// one of the tiles this location belongs to are
								// null, therefor do not draw it.
								if (t == null) {
									continue;
								}

								if (t.draw) {
									tile.drawLocations = true;
									continue LOOP;
								}

								if (t.wallCullDirection != 0) {
									int flags = 0;

									if (x > l.minTileX) {
										flags += 0b0001;
									}

									if (x < l.maxTileX) {
										flags += 0b0100;
									}

									if (z > l.minTileZ) {
										flags += 0b1000;
									}

									if (z < l.maxTileZ) {
										flags += 0b0010;
									}

									// 0001 = x > minX
									// 0010 = z < maxZ
									// 0100 = x < maxX
									// 1000 = z > minZ
									if ((flags & t.wallCullDirection) == tile.relativeWallCullDirection) {
										tile.drawLocations = true;
										continue LOOP;
									}
								}
							}
						}

						locationBuffer[locationBufferSize++] = l;

						int dx0 = cameraTileX - l.minTileX;
						int dx1 = l.maxTileX - cameraTileX;

						if (dx1 > dx0) {
							dx0 = dx1;
						}

						int dz0 = cameraTileZ - l.minTileZ;
						int dz1 = l.maxTileZ - cameraTileZ;

						if (dz1 > dz0) {
							l.drawPriority = dx0 + dz1;
						} else {
							l.drawPriority = dx0 + dz0;
						}
					}
				}

				while (locationBufferSize > 0) {
					int maxPriority = -50;
					int index = -1;

					for (int n = 0; n < locationBufferSize; n++) {
						Location l = locationBuffer[n];

						if (l.drawPriority > maxPriority && l.cycle != cycle) {
							maxPriority = l.drawPriority;
							index = n;
						}
					}

					if (index == -1) {
						break;
					}

					Location l = locationBuffer[index];
					l.cycle = cycle;
					Model m = l.model;

					if (m == null) {
						m = l.renderable.getDrawModel();
					}

					if (!isAreaOccluded(tileRenderPlane, l.minTileX, l.maxTileX, l.minTileZ, l.maxTileZ, m.maxBoundY)) {
						m.draw(l.yaw, pitchSin, pitchCos, yawSin, yawCos, l.sceneX - cameraX, l.sceneY - cameraY, l.sceneZ - cameraZ, l.bitset);
					}

					for (int x = l.minTileX; x <= l.maxTileX; x++) {
						for (int z = l.minTileZ; z <= l.maxTileZ; z++) {
							Tile t = tiles[x][z];

							if (t == null) {
								continue;
							}

							if (t.wallCullDirection != 0) {
								tileQueue.push(t);
							} else if ((x != tileX || z != tileZ) && t.isVisible) {
								tileQueue.push(t);
							}
						}
					}
				}

				if (tile.drawLocations) {
					continue;
				}
			}

			if (tile.isVisible && tile.wallCullDirection == 0) {
				if (tileX <= cameraTileX && tileX > minTileX) {
					Tile t = tiles[tileX - 1][tileZ];

					if (t != null && t.isVisible) {
						continue;
					}
				}

				if (tileX >= cameraTileX && tileX < maxTileX - 1) {
					Tile t = tiles[tileX + 1][tileZ];

					if (t != null && t.isVisible) {
						continue;
					}
				}

				if (tileZ <= cameraTileZ && tileZ > minTileZ) {
					Tile t = tiles[tileX][tileZ - 1];

					if (t != null && t.isVisible) {
						continue;
					}
				}

				if (tileZ >= cameraTileZ && tileZ < maxTileZ - 1) {
					Tile t = tiles[tileX][tileZ + 1];

					if (t != null && t.isVisible) {
						continue;
					}
				}

				tile.isVisible = false;
				tileUpdateCount--;

				ObjectLocation object = tile.object;

				if (object != null && object.offsetY != 0) {
					object.model.draw(0, pitchSin, pitchCos, yawSin, yawCos, object.sceneX - cameraX, object.sceneY - cameraY - object.offsetY, object.sceneZ - cameraZ, object.bitset);
				}

				if (tile.tileWallDrawFlags != 0) {
					WallDecorationLocation decoration = tile.wallDecoration;

					if (decoration != null && !isOccluded(tileRenderPlane, tileX, tileZ, decoration.model.maxBoundY)) {
						if ((decoration.type & tile.tileWallDrawFlags) != 0) {
							decoration.model.draw(decoration.rotation, pitchSin, pitchCos, yawSin, yawCos, decoration.sceneX - cameraX, decoration.sceneY - cameraY, decoration.sceneZ - cameraZ, decoration.bitset);
						} else if ((decoration.type & 0x300) != 0) {
							int drawX = decoration.sceneX - cameraX;
							int drawY = decoration.sceneY - cameraY;
							int drawZ = decoration.sceneZ - cameraZ;
							int rotation = decoration.rotation;

							int absoluteX; // postiive draw x
							int absoluteZ; // positive draw y

							if (rotation == 1 || rotation == 2) {
								absoluteX = -drawX;
							} else {
								absoluteX = drawX;
							}

							if (rotation == 2 || rotation == 3) {
								absoluteZ = -drawZ;
							} else {
								absoluteZ = drawZ;
							}

							if ((decoration.type & 0x100) != 0 && absoluteZ >= absoluteX) {
								drawX += DECO_TYPE1_OFFSET_X[rotation];
								drawZ += DECO_TYPE1_OFFSET_Z[rotation];
								decoration.model.draw(rotation * 512 + 256, pitchSin, pitchCos, yawSin, yawCos, drawX, drawY, drawZ, decoration.bitset);
							}

							if ((decoration.type & 0x200) != 0 && absoluteZ <= absoluteX) {
								drawX += DECO_TYPE2_OFFSET_X[rotation];
								drawZ += DECO_TYPE2_OFFSET_Z[rotation];
								decoration.model.draw(rotation * 512 + 1280 & 0x7ff, pitchSin, pitchCos, yawSin, yawCos, drawX, drawY, drawZ, decoration.bitset);
							}
						}
					}

					WallLocation wall = tile.wall;

					if (wall != null) {
						if ((wall.type2 & tile.tileWallDrawFlags) != 0 && !isWallOccluded(tileRenderPlane, tileX, tileZ, wall.type2)) {
							wall.model2.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
						}

						if ((wall.type1 & tile.tileWallDrawFlags) != 0 && !isWallOccluded(tileRenderPlane, tileX, tileZ, wall.type1)) {
							wall.model1.draw(0, pitchSin, pitchCos, yawSin, yawCos, wall.sceneX - cameraX, wall.sceneY - cameraY, wall.sceneZ - cameraZ, wall.bitset);
						}
					}
				}

				if (tilePlane < maxPlane - 1) {
					Tile t = (planeTiles[tilePlane + 1][tileX][tileZ]);

					if (t != null && t.isVisible) {
						tileQueue.push(t);
					}
				}

				if (tileX < cameraTileX) {
					Tile t = tiles[tileX + 1][tileZ];

					if (t != null && t.isVisible) {
						tileQueue.push(t);
					}
				}

				if (tileZ < cameraTileZ) {
					Tile t = tiles[tileX][tileZ + 1];

					if (t != null && t.isVisible) {
						tileQueue.push(t);
					}
				}

				if (tileX > cameraTileX) {
					Tile t = tiles[tileX - 1][tileZ];

					if (t != null && t.isVisible) {
						tileQueue.push(t);
					}
				}

				if (tileZ > cameraTileZ) {
					Tile t = tiles[tileX][tileZ - 1];

					if (t != null && t.isVisible) {
						tileQueue.push(t);
					}
				}
			}
		}
	}

	public void drawTileUnderlay(TileUnderlay u, int plane, int tileX, int tileZ, int pitchSin, int pitchCos, int yawSin, int yawCos) {
		int sceneX3;
		int sceneX0 = sceneX3 = (tileX << 7) - cameraX;
		int sceneZ1;
		int sceneZ0 = sceneZ1 = (tileZ << 7) - cameraZ;
		int sceneX2;
		int sceneX1 = sceneX2 = sceneX0 + 128;
		int sceneZ3;
		int sceneZ2 = sceneZ3 = sceneZ0 + 128;

		int sceneY0 = heightmap[plane][tileX][tileZ] - cameraY;
		int sceneY1 = heightmap[plane][tileX + 1][tileZ] - cameraY;
		int sceneY2 = heightmap[plane][tileX + 1][tileZ + 1] - cameraY;
		int sceneY3 = heightmap[plane][tileX][tileZ + 1] - cameraY;

		int w = sceneZ0 * yawSin + sceneX0 * yawCos >> 16;
		sceneZ0 = sceneZ0 * yawCos - sceneX0 * yawSin >> 16;
		sceneX0 = w;

		w = sceneY0 * pitchCos - sceneZ0 * pitchSin >> 16;
		sceneZ0 = sceneY0 * pitchSin + sceneZ0 * pitchCos >> 16;
		sceneY0 = w;

		if (sceneZ0 < 50) {
			return;
		}

		w = sceneZ1 * yawSin + sceneX1 * yawCos >> 16;
		sceneZ1 = sceneZ1 * yawCos - sceneX1 * yawSin >> 16;
		sceneX1 = w;

		w = sceneY1 * pitchCos - sceneZ1 * pitchSin >> 16;
		sceneZ1 = sceneY1 * pitchSin + sceneZ1 * pitchCos >> 16;
		sceneY1 = w;

		if (sceneZ1 < 50) {
			return;
		}

		w = sceneZ2 * yawSin + sceneX2 * yawCos >> 16;
		sceneZ2 = sceneZ2 * yawCos - sceneX2 * yawSin >> 16;
		sceneX2 = w;

		w = sceneY2 * pitchCos - sceneZ2 * pitchSin >> 16;
		sceneZ2 = sceneY2 * pitchSin + sceneZ2 * pitchCos >> 16;
		sceneY2 = w;

		if (sceneZ2 < 50) {
			return;
		}

		w = sceneZ3 * yawSin + sceneX3 * yawCos >> 16;
		sceneZ3 = sceneZ3 * yawCos - sceneX3 * yawSin >> 16;
		sceneX3 = w;

		w = sceneY3 * pitchCos - sceneZ3 * pitchSin >> 16;
		sceneZ3 = sceneY3 * pitchSin + sceneZ3 * pitchCos >> 16;
		sceneY3 = w;

		if (sceneZ3 < 50) {
			return;
		}

		int x0 = (Graphics3D.centerX + (sceneX0 << 9) / sceneZ0);
		int y0 = (Graphics3D.centerY + (sceneY0 << 9) / sceneZ0);
		int x1 = (Graphics3D.centerX + (sceneX1 << 9) / sceneZ1);
		int y1 = (Graphics3D.centerY + (sceneY1 << 9) / sceneZ1);
		int x2 = (Graphics3D.centerX + (sceneX2 << 9) / sceneZ2);
		int y2 = (Graphics3D.centerY + (sceneY2 << 9) / sceneZ2);
		int x3 = (Graphics3D.centerX + (sceneX3 << 9) / sceneZ3);
		int y3 = (Graphics3D.centerY + (sceneY3 << 9) / sceneZ3);

		Graphics3D.alpha = 0;

		if (((x2 - x3) * (y1 - y3) - (y2 - y3) * (x1 - x3)) > 0) {
			Graphics3D.testX = false;

			if (x2 < 0 || x3 < 0 || x1 < 0 || x2 > Graphics2D.rightX || x3 > Graphics2D.rightX || x1 > Graphics2D.rightX) {
				Graphics3D.testX = true;
			}

			if (Scene.checkClick) {
				if (withinTriangle(Scene.mouseX, Scene.mouseY, y2, y3, y1, x2, x3, x1)) {
					Scene.clickedTileX = tileX;
					Scene.clickedTileZ = tileZ;
				}
			}

			if (u != null) {
				if (u.textureIndex == -1) {
					if (u.northeastColor != 12345678) {
						Graphics3D.fillShadedTriangle(x2, y2, x3, y3, x1, y1, u.northeastColor, u.northwestColor, u.southeastColor);
					}
				} else if (!lowmemory) {
					if (u.isFlat) {
						Graphics3D.fillTexturedTriangle(y2, y3, y1, x2, x3, x1, u.northeastColor, u.northwestColor, u.southeastColor, sceneX0, sceneX1, sceneX3, sceneY0, sceneY1, sceneY3, sceneZ0, sceneZ1, sceneZ3, u.textureIndex);
					} else {
						Graphics3D.fillTexturedTriangle(y2, y3, y1, x2, x3, x1, u.northeastColor, u.northwestColor, u.southeastColor, sceneX2, sceneX3, sceneX1, sceneY2, sceneY3, sceneY1, sceneZ2, sceneZ3, sceneZ1, u.textureIndex);
					}
				} else {
					int hsl = TEXTURE_HSL[u.textureIndex];
					Graphics3D.fillShadedTriangle(x2, y2, x3, y3, x1, y1, adjustHSLLightness(hsl, u.northeastColor), adjustHSLLightness(hsl, u.northwestColor), adjustHSLLightness(hsl, u.southeastColor));
				}
			}
		}

		if (((x0 - x1) * (y3 - y1) - (y0 - y1) * (x3 - x1)) > 0) {
			Graphics3D.testX = false;

			if (x0 < 0 || x1 < 0 || x3 < 0 || x0 > Graphics2D.rightX || x1 > Graphics2D.rightX || x3 > Graphics2D.rightX) {
				Graphics3D.testX = true;
			}

			if (Scene.checkClick) {
				if (withinTriangle(Scene.mouseX, Scene.mouseY, y0, y1, y3, x0, x1, x3)) {
					Scene.clickedTileX = tileX;
					Scene.clickedTileZ = tileZ;
				}
			}

			if (u != null) {
				if (u.textureIndex == -1) {
					if (u.southwestColor != 12345678) {
						Graphics3D.fillShadedTriangle(x0, y0, x1, y1, x3, y3, u.southwestColor, u.southeastColor, u.northwestColor);
					}
				} else if (!lowmemory) {
					Graphics3D.fillTexturedTriangle(y0, y1, y3, x0, x1, x3, u.southwestColor, u.southeastColor, u.northwestColor, sceneX0, sceneX1, sceneX3, sceneY0, sceneY1, sceneY3, sceneZ0, sceneZ1, sceneZ3, u.textureIndex);
				} else {
					int hsl = TEXTURE_HSL[u.textureIndex];
					Graphics3D.fillShadedTriangle(x0, y0, x1, y1, x3, y3, adjustHSLLightness(hsl, u.southwestColor), adjustHSLLightness(hsl, u.southeastColor), adjustHSLLightness(hsl, u.northwestColor));
				}
			}
		}
	}

	public void drawTileOverlay(TileOverlay o, int plane, int tileX, int tileZ, int pitchSin, int pitchCos, int yawSin, int yawCos) {
		int count = o.vertexX.length;

		for (int v = 0; v < count; v++) {
			int sceneX = o.vertexX[v] - cameraX;
			int sceneY = o.vertexY[v] - cameraY;
			int sceneZ = o.vertexZ[v] - cameraZ;

			int w = sceneZ * yawSin + sceneX * yawCos >> 16;
			sceneZ = sceneZ * yawCos - sceneX * yawSin >> 16;
			sceneX = w;

			w = sceneY * pitchCos - sceneZ * pitchSin >> 16;
			sceneZ = sceneY * pitchSin + sceneZ * pitchCos >> 16;
			sceneY = w;

			if (sceneZ < Scene.NEAR_Z) {
				return;
			}

			if (o.triangleTextureIndex != null) {
				TileOverlay.vertexSceneX[v] = sceneX;
				TileOverlay.vertexSceneY[v] = sceneY;
				TileOverlay.vertexSceneZ[v] = sceneZ;
			}

			TileOverlay.tmpScreenX[v] = Graphics3D.centerX + (sceneX << 9) / sceneZ;
			TileOverlay.tmpScreenY[v] = Graphics3D.centerY + (sceneY << 9) / sceneZ;
		}

		Graphics3D.alpha = 0;
		count = o.triangleVertexA.length;

		for (int t = 0; t < count; t++) {
			int a = o.triangleVertexA[t];
			int b = o.triangleVertexB[t];
			int c = o.triangleVertexC[t];

			int x0 = TileOverlay.tmpScreenX[a];
			int x1 = TileOverlay.tmpScreenX[b];
			int x2 = TileOverlay.tmpScreenX[c];

			int y0 = TileOverlay.tmpScreenY[a];
			int y1 = TileOverlay.tmpScreenY[b];
			int y2 = TileOverlay.tmpScreenY[c];

			if (((x0 - x1) * (y2 - y1) - (y0 - y1) * (x2 - x1)) > 0) {
				Graphics3D.testX = false;

				if (x0 < 0 || x1 < 0 || x2 < 0 || x0 > Graphics2D.rightX || x1 > Graphics2D.rightX || x2 > Graphics2D.rightX) {
					Graphics3D.testX = true;
				}

				if (Scene.checkClick) {
					if (withinTriangle(Scene.mouseX, Scene.mouseY, y0, y1, y2, x0, x1, x2)) {
						Scene.clickedTileX = tileX;
						Scene.clickedTileZ = tileZ;
					}
				}

				if (o.triangleTextureIndex == null || o.triangleTextureIndex[t] == -1) {
					if (o.triangleColorA[t] != 12345678) {
						Graphics3D.fillShadedTriangle(x0, y0, x1, y1, x2, y2, o.triangleColorA[t], o.triangleColorB[t], o.triangleColorC[t]);
					}
				} else if (!lowmemory) {
					if (o.isFlat) {
						Graphics3D.fillTexturedTriangle(y0, y1, y2, x0, x1, x2, o.triangleColorA[t], o.triangleColorB[t], o.triangleColorC[t], TileOverlay.vertexSceneX[0], TileOverlay.vertexSceneX[1], TileOverlay.vertexSceneX[3], TileOverlay.vertexSceneY[0], TileOverlay.vertexSceneY[1], TileOverlay.vertexSceneY[3], TileOverlay.vertexSceneZ[0], TileOverlay.vertexSceneZ[1], TileOverlay.vertexSceneZ[3], o.triangleTextureIndex[t]);
					} else {
						Graphics3D.fillTexturedTriangle(y0, y1, y2, x0, x1, x2, o.triangleColorA[t], o.triangleColorB[t], o.triangleColorC[t], TileOverlay.vertexSceneX[a], TileOverlay.vertexSceneX[b], TileOverlay.vertexSceneX[c], TileOverlay.vertexSceneY[a], TileOverlay.vertexSceneY[b], TileOverlay.vertexSceneY[c], TileOverlay.vertexSceneZ[a], TileOverlay.vertexSceneZ[b], TileOverlay.vertexSceneZ[c], o.triangleTextureIndex[t]);
					}
				} else {
					int hsl = TEXTURE_HSL[o.triangleTextureIndex[t]];
					Graphics3D.fillShadedTriangle(x0, y0, x1, y1, x2, y2, adjustHSLLightness(hsl, o.triangleColorA[t]), adjustHSLLightness(hsl, o.triangleColorB[t]), adjustHSLLightness(hsl, o.triangleColorC[t]));
				}
			}
		}
	}

	public static final int adjustHSLLightness(int hsl, int lightness) {
		lightness = 127 - lightness;
		lightness = lightness * (hsl & 0x7f) / 160;
		if (lightness < 2) {
			lightness = 2;
		} else if (lightness > 126) {
			lightness = 126;
		}
		return (hsl & 0xff80) + lightness;
	}

	public static final boolean withinTriangle(int x, int y, int y0, int y1, int y2, int x0, int x1, int x2) {
		if (y < y0 && y < y1 && y < y2) {
			return false;
		}

		if (y > y0 && y > y1 && y > y2) {
			return false;
		}

		if (x < x0 && x < x1 && x < x2) {
			return false;
		}

		if (x > x0 && x > x1 && x > x2) {
			return false;
		}

		int a = ((y - y0) * (x1 - x0) - (x - x0) * (y1 - y0));
		int b = ((y - y2) * (x0 - x2) - (x - x2) * (y0 - y2));
		int c = ((y - y1) * (x2 - x1) - (x - x1) * (y2 - y1));

		return a * c > 0 && c * b > 0;
	}

	private void updateOccluders() {
		int count = planeOccluderCount[topPlane];
		Occluder[] occluders = planeOccluders[topPlane];

		activeOccluderCount = 0;

		if (!Scene.occlusionEnabled) {
			return;
		}

		for (int n = 0; n < count; n++) {
			Occluder o = occluders[n];

			if (o.type == 1) {
				int tileX = o.minTileX - cameraTileX + Scene.VIEW_RADIUS;

				if (tileX >= 0 && tileX <= Scene.VIEW_DIAMETER) {
					int minTileZ = o.minTileZ - cameraTileZ + Scene.VIEW_RADIUS;

					if (minTileZ < 0) {
						minTileZ = 0;
					}

					int maxTileZ = o.maxTileZ - cameraTileZ + Scene.VIEW_RADIUS;

					if (maxTileZ > Scene.VIEW_DIAMETER) {
						maxTileZ = Scene.VIEW_DIAMETER;
					}

					boolean visible = false;

					while (minTileZ <= maxTileZ) {
						if (visibilityMap[tileX][minTileZ++]) {
							visible = true;
							break;
						}
					}

					if (visible) {
						int dx = cameraX - o.minX;

						if (dx > 32) {
							o.testDirection = 1;
						} else {
							if (dx >= -32) {
								continue;
							}
							o.testDirection = 2;
							dx = -dx;
						}

						o.minNormalZ = (o.minZ - cameraZ << 8) / dx;
						o.maxNormalZ = (o.maxZ - cameraZ << 8) / dx;
						o.minNormalY = (o.minY - cameraY << 8) / dx;
						o.maxNormalY = (o.maxY - cameraY << 8) / dx;
						activeOcludders[activeOccluderCount++] = o;
					}
				}
			} else if (o.type == 2) {
				int tileZ = o.minTileZ - cameraTileZ + Scene.VIEW_RADIUS;

				if (tileZ >= 0 && tileZ <= Scene.VIEW_DIAMETER) {
					int minTileX = o.minTileX - cameraTileX + Scene.VIEW_RADIUS;

					if (minTileX < 0) {
						minTileX = 0;
					}

					int maxTileX = o.maxTileX - cameraTileX + Scene.VIEW_RADIUS;

					if (maxTileX > Scene.VIEW_DIAMETER) {
						maxTileX = Scene.VIEW_DIAMETER;
					}

					boolean visible = false;
					while (minTileX <= maxTileX) {
						if (visibilityMap[minTileX++][tileZ]) {
							visible = true;
							break;
						}
					}

					if (visible) {
						int z = cameraZ - o.minZ;

						if (z > 32) {
							o.testDirection = 3;
						} else {
							if (z >= -32) {
								continue;
							}
							o.testDirection = 4;
							z = -z;
						}

						o.minNormalX = (o.minX - cameraX << 8) / z;
						o.maxNormalX = (o.maxX - cameraX << 8) / z;
						o.minNormalY = (o.minY - cameraY << 8) / z;
						o.maxNormalY = (o.maxY - cameraY << 8) / z;
						activeOcludders[activeOccluderCount++] = o;
					}
				}
			} else if (o.type == 4) {
				int y = o.minY - cameraY;

				if (y > 128) {
					int minTileZ = o.minTileZ - cameraTileZ + Scene.VIEW_RADIUS;

					if (minTileZ < 0) {
						minTileZ = 0;
					}

					int maxTileZ = o.maxTileZ - cameraTileZ + Scene.VIEW_RADIUS;

					if (maxTileZ > Scene.VIEW_DIAMETER) {
						maxTileZ = Scene.VIEW_DIAMETER;
					}

					if (minTileZ <= maxTileZ) {
						int minTileX = o.minTileX - cameraTileX + Scene.VIEW_RADIUS;

						if (minTileX < 0) {
							minTileX = 0;
						}

						int maxTileX = o.maxTileX - cameraTileX + Scene.VIEW_RADIUS;

						if (maxTileX > Scene.VIEW_DIAMETER) {
							maxTileX = Scene.VIEW_DIAMETER;
						}

						boolean visible = false;

						LOOP:
						for (int x = minTileX; x <= maxTileX; x++) {
							for (int z = minTileZ; z <= maxTileZ; z++) {
								if (visibilityMap[x][z]) {
									visible = true;
									break LOOP;
								}
							}
						}

						if (visible) {
							o.testDirection = 5;
							o.minNormalX = (o.minX - cameraX << 8) / y;
							o.maxNormalX = (o.maxX - cameraX << 8) / y;
							o.minNormalZ = (o.minZ - cameraZ << 8) / y;
							o.maxNormalZ = (o.maxZ - cameraZ << 8) / y;
							activeOcludders[activeOccluderCount++] = o;
						}
					}
				}
			}
		}
	}

	private boolean isTileOccluded(int plane, int tileX, int tileZ) {
		int cycle = levelTileCycle[plane][tileX][tileZ];

		if (cycle == -SceneGraph.cycle) {
			return false;
		}

		if (cycle == SceneGraph.cycle) {
			return true;
		}

		int sceneX = tileX << 7;
		int sceneZ = tileZ << 7;

		if (isOccluded(sceneX + 1, heightmap[plane][tileX][tileZ], sceneZ + 1) && isOccluded(sceneX + 128 - 1, heightmap[plane][tileX + 1][tileZ], sceneZ + 1) && isOccluded(sceneX + 128 - 1, heightmap[plane][tileX + 1][tileZ + 1], sceneZ + 128 - 1) && isOccluded(sceneX + 1, heightmap[plane][tileX][tileZ + 1], sceneZ + 128 - 1)) {
			levelTileCycle[plane][tileX][tileZ] = SceneGraph.cycle;
			return true;
		}

		levelTileCycle[plane][tileX][tileZ] = -SceneGraph.cycle;
		return false;
	}

	private boolean isWallOccluded(int plane, int tileX, int tileZ, int type) {
		if (!isTileOccluded(plane, tileX, tileZ)) {
			return false;
		}

		int sceneX = tileX << 7;
		int sceneZ = tileZ << 7;
		int sceneY = heightmap[plane][tileX][tileZ] - 1;

		int planeY0 = sceneY - 120;
		int planeY1 = sceneY - 230;
		int planeY2 = sceneY - 238;

		if (type < 16) {
			if (type == 1) {
				if (sceneX > cameraX) {
					if (!isOccluded(sceneX, sceneY, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX, sceneY, sceneZ + 128)) {
						return false;
					}
				}

				if (plane > 0) {
					if (!isOccluded(sceneX, planeY0, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX, planeY0, sceneZ + 128)) {
						return false;
					}
				}

				if (!isOccluded(sceneX, planeY1, sceneZ)) {
					return false;
				}

				return isOccluded(sceneX, planeY1, sceneZ + 128);
			}

			if (type == 2) {
				if (sceneZ < cameraZ) {
					if (!isOccluded(sceneX, sceneY, sceneZ + 128)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, sceneY, sceneZ + 128)) {
						return false;
					}
				}
				if (plane > 0) {
					if (!isOccluded(sceneX, planeY0, sceneZ + 128)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, planeY0, sceneZ + 128)) {
						return false;
					}
				}
				if (!isOccluded(sceneX, planeY1, sceneZ + 128)) {
					return false;
				}
				return isOccluded(sceneX + 128, planeY1, sceneZ + 128);
			}

			if (type == 4) {
				if (sceneX < cameraZ) {
					if (!isOccluded(sceneX + 128, sceneY, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, sceneY, sceneZ + 128)) {
						return false;
					}
				}

				if (plane > 0) {
					if (!isOccluded(sceneX + 128, planeY0, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, planeY0, sceneZ + 128)) {
						return false;
					}
				}

				if (!isOccluded(sceneX + 128, planeY1, sceneZ)) {
					return false;
				}

				return isOccluded(sceneX + 128, planeY1, sceneZ + 128);
			}

			if (type == 8) {
				if (sceneZ > cameraZ) {
					if (!isOccluded(sceneX, sceneY, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, sceneY, sceneZ)) {
						return false;
					}
				}

				if (plane > 0) {
					if (!isOccluded(sceneX, planeY0, sceneZ)) {
						return false;
					}
					if (!isOccluded(sceneX + 128, planeY0, sceneZ)) {
						return false;
					}
				}

				if (!isOccluded(sceneX, planeY1, sceneZ)) {
					return false;
				}

				return isOccluded(sceneX + 128, planeY1, sceneZ);
			}
		}

		if (!isOccluded(sceneX + 64, planeY2, sceneZ + 64)) {
			return false;
		}

		if (type == 16) {
			return isOccluded(sceneX, planeY1, sceneZ + 128);
		} else if (type == 32) {
			return isOccluded(sceneX + 128, planeY1, sceneZ + 128);
		} else if (type == 64) {
			return isOccluded(sceneX + 128, planeY1, sceneZ);
		} else if (type == 128) {
			return isOccluded(sceneX, planeY1, sceneZ);
		}

		System.out.println("Warning unsupported wall type");
		return true;
	}

	private boolean isOccluded(int plane, int tileX, int tileZ, int height) {
		if (!isTileOccluded(plane, tileX, tileZ)) {
			return false;
		}

		int sceneX = tileX << 7;
		int sceneZ = tileZ << 7;

		return isOccluded(sceneX + 1, heightmap[plane][tileX][tileZ] - height, sceneZ + 1) && isOccluded(sceneX + 128 - 1, (heightmap[plane][tileX + 1][tileZ] - height), sceneZ + 1) && isOccluded(sceneX + 128 - 1, (heightmap[plane][tileX + 1][tileZ + 1] - height), sceneZ + 128 - 1) && isOccluded(sceneX + 1, (heightmap[plane][tileX][tileZ + 1] - height), sceneZ + 128 - 1);
	}

	private boolean isAreaOccluded(int plane, int minTileX, int maxTileX, int minTileZ, int maxTileZ, int height) {
		if (minTileX == maxTileX && minTileZ == maxTileZ) {
			if (!isTileOccluded(plane, minTileX, minTileZ)) {
				return false;
			}

			int sceneX = minTileX << 7;
			int sceneZ = minTileZ << 7;

			return isOccluded(sceneX + 1, heightmap[plane][minTileX][minTileZ] - height, sceneZ + 1) && isOccluded(sceneX + 128 - 1, (heightmap[plane][minTileX + 1][minTileZ] - height), sceneZ + 1) && isOccluded(sceneX + 128 - 1, (heightmap[plane][minTileX + 1][minTileZ + 1]) - height, sceneZ + 128 - 1) && isOccluded(sceneX + 1, (heightmap[plane][minTileX][minTileZ + 1] - height), sceneZ + 128 - 1);
		}

		for (int x = minTileX; x <= maxTileX; x++) {
			for (int z = minTileZ; z <= maxTileZ; z++) {
				if (levelTileCycle[plane][x][z] == -cycle) {
					return false;
				}
			}
		}

		int minSceneX = (minTileX << 7) + 1;
		int minSceneZ = (minTileZ << 7) + 2;
		int minSceneY = heightmap[plane][minTileX][minTileZ] - height;

		if (!isOccluded(minSceneX, minSceneY, minSceneZ)) {
			return false;
		}

		int maxSceneX = (maxTileX << 7) - 1;

		if (!isOccluded(maxSceneX, minSceneY, minSceneZ)) {
			return false;
		}

		int maxSceneY = (maxTileZ << 7) - 1;

		if (!isOccluded(minSceneX, minSceneY, maxSceneY)) {
			return false;
		}

		return isOccluded(maxSceneX, minSceneY, maxSceneY);
	}

	private boolean isOccluded(int x, int y, int z) {
		for (int n = 0; n < activeOccluderCount; n++) {
			Occluder o = activeOcludders[n];

			if (o.testDirection == 1) {
				int dx = o.minX - x;

				if (dx > 0) {
					int minZ = o.minZ + (o.minNormalZ * dx >> 8);
					int maxZ = o.maxZ + (o.maxNormalZ * dx >> 8);
					int minY = o.minY + (o.minNormalY * dx >> 8);
					int maxY = o.maxY + (o.maxNormalY * dx >> 8);

					if (z >= minZ && z <= maxZ && y >= minY && y <= maxY) {
						return true;
					}
				}
			} else if (o.testDirection == 2) {
				int dx = x - o.minX;

				if (dx > 0) {
					int minZ = o.minZ + (o.minNormalZ * dx >> 8);
					int maxZ = o.maxZ + (o.maxNormalZ * dx >> 8);
					int minY = o.minY + (o.minNormalY * dx >> 8);
					int maxY = o.maxY + (o.maxNormalY * dx >> 8);

					if (z >= minZ && z <= maxZ && y >= minY && y <= maxY) {
						return true;
					}
				}
			} else if (o.testDirection == 3) {
				int dz = o.minZ - z;

				if (dz > 0) {
					int minX = o.minX + (o.minNormalX * dz >> 8);
					int maxX = o.maxX + (o.maxNormalX * dz >> 8);
					int minY = o.minY + (o.minNormalY * dz >> 8);
					int maxY = o.maxY + (o.maxNormalY * dz >> 8);

					if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
						return true;
					}
				}
			} else if (o.testDirection == 4) {
				int dz = z - o.minZ;

				if (dz > 0) {
					int minX = o.minX + (o.minNormalX * dz >> 8);
					int maxX = o.maxX + (o.maxNormalX * dz >> 8);
					int minY = o.minY + (o.minNormalY * dz >> 8);
					int maxY = o.maxY + (o.maxNormalY * dz >> 8);

					if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
						return true;
					}
				}
			} else if (o.testDirection == 5) {
				int dy = y - o.minY;

				if (dy > 0) {
					int minX = o.minX + (o.minNormalX * dy >> 8);
					int maxX = o.maxX + (o.maxNormalX * dy >> 8);
					int minZ = o.minZ + (o.minNormalZ * dy >> 8);
					int maxZ = o.maxZ + (o.maxNormalZ * dy >> 8);

					if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
