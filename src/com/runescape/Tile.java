package com.runescape;

public final class Tile extends Link {

	public int plane;
	public int x;
	public int z;
	public int renderPlane;
	public TileUnderlay underlay;
	public TileOverlay overlay;
	public WallLocation wall;
	public WallDecorationLocation wallDecoration;
	public GroundDecorationLocation groundDecoration;
	public ObjectLocation object;
	public int locationCount;
	public Location[] locs = new Location[5];
	public int[] locFlags = new int[5];
	public int flags;
	public int drawPlane;
	public boolean draw;
	public boolean isVisible;
	public boolean drawLocations;
	public int wallCullDirection;
	public int wallUncullDirection;
	public int relativeWallCullDirection;
	public int tileWallDrawFlags;
	public Tile bridge;

	public Tile(int plane, int x, int y) {
		this.renderPlane = this.plane = plane;
		this.x = x;
		this.z = y;
	}
}
