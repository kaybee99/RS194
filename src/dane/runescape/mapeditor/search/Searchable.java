package dane.runescape.mapeditor.search;

public class Searchable implements Comparable<Searchable> {

	private final String name;
	private final int index;
	private final SearchMode mode;

	public Searchable(String name, int index, SearchMode mode) {
		this.name = name;
		this.index = index;
		this.mode = mode;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the mode
	 */
	public SearchMode getMode() {
		return mode;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int compareTo(Searchable s) {
		return name.compareTo(s.name);
	}

}
