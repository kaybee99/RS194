package dane.runescape.mapeditor.search;

public enum SearchMode {

	LOCATION, NPC, ITEM, FLOOR;

	@Override
	public String toString() {
		String s = name();
		if (this == NPC) {
			return s;
		}
		return s.charAt(0) + s.toLowerCase().substring(1);
	}
}
