package com.runescape;

public final class Loc {

	/* @formatter:off */
	public static final int[] TYPE_TO_CLASS = {
		0,	// straight walls, fences 
		0,	// diagonal walls corner, fences etc connectors
		0,	// entire walls, fences etc corners
		0,	// straight wall corners, fences etc connectors
		1,	// straight inside wall decoration
		1,	// straight outside wall decoration
		1,	// diagonal outside wall decoration
		1,	// diagonal inside wall decoration
		1,	// diagonal in wall decoration
		2,	// diagonal walls, fences etc
		2,	// all kinds of objects, trees, statues, signs, fountains etc etc
		2,	// ground objects like daisies etc
		2,	// straight sloped roofs
		2,	// diagonal sloped roofs
		2,	// diagonal slope connecting roofs
		2,	// straight sloped corner connecting roofs
		2,	// straight sloped corner roof
		2,	// straight flat top roofs
		2,	// straight bottom egde roofs
		2,	// diagonal bottom edge connecting roofs
		2,	// straight bottom edge connecting roofs
		2,	// straight bottom edge connecting corner roofs
		3	// ground decoration + map signs (quests, water fountains, shops etc)
	};
	
	public static final int CLASS_WALL = 0;
	public static final int CLASS_WALL_DECORATION = 1;
	public static final int CLASS_NORMAL = 2;
	public static final int CLASS_GROUND_DECORATION = 3;
	
	public static final String[] CLASS_TO_STRING = {
		"Wall", "Wall Decoration", "Normal", "Ground Decoration"
	};
	
	/* @formatter:on */

	int tileY;
	int sceneY;
	int sceneX;
	int sceneZ;
	Model model;
	Renderable renderable;
	public int yaw;
	int minTileX;
	int maxTileX;
	int minTileZ;
	int maxTileZ;
	int drawPriority;
	int cycle;
	int bitset;
	byte info;
}
