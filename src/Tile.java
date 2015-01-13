
public final class Tile extends Link {

	public int y;
	public int x;
	public int z;
	public int renderPlane;
	public TileUnderlay underlay;
	public TileOverlay overlay;
	public WallLoc wall;
	public WallDecorationLoc wallDecoration;
	public GroundDecorationLoc groundDecoration;
	public ObjLoc obj;
	public int locN;
	public Loc[] locs = new Loc[5];
	public int[] locFlags = new int[5];
	public int flags;
	public int drawY;
	public boolean draw;
	public boolean update;
	public boolean drawLocs;
	public int anInt985;
	public int anInt986;
	public int anInt987;
	public int anInt988;
	public Tile bridge;

	public Tile(int plane, int x, int y) {
		this.renderPlane = this.y = plane;
		this.x = x;
		this.z = y;
	}
}
