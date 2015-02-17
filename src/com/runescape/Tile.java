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
	public GroundDecorationLoc groundDecoration;
	public ObjectLocation obj;
	public int locationCount;
	public Location[] locs = new Location[5];
	public int[] locFlags = new int[5];
	public int flags;
	public int drawY;
	public boolean draw;
	public boolean isVisible;
	public boolean drawLocations;
	public int anInt985;
	public int anInt986;
	public int anInt987;
	public int drawFlags;
	public Tile bridge;

	public Tile(int plane, int x, int y) {
		this.renderPlane = this.plane = plane;
		this.x = x;
		this.z = y;
	}
}
