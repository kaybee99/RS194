
public final class SequencedLoc extends Link {

	public int plane;
	public int classtype;
	public int tileX;
	public int tileY;
	public int locIndex;
	public Seq seq;
	public int seqFrame;
	public int seqCycle;

	public SequencedLoc(Seq s, int locIndex, int type, int tileX, int tileY, int plane) {
		this.plane = plane;
		this.classtype = type;
		this.tileX = tileX;
		this.tileY = tileY;
		this.locIndex = locIndex;
		this.seq = s;
		this.seqFrame = -1;
		this.seqCycle = 0;
	}
}
