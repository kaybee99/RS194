package dane.runescape.mapeditor.event;

import dane.runescape.mapeditor.search.SearchMode;
import java.util.EventObject;

public class SearchEvent extends EventObject {

	private static final long serialVersionUID = 956225978549693971L;

	public enum Type {

		MODE_CHANGED, VALUE_SEARCHED, VALUE_SELECTED;
	}

	private Type type = Type.MODE_CHANGED;
	private SearchMode mode;
	private Object selected;
	private String searched;

	public SearchEvent(Object source, SearchMode mode, Object selected) {
		this(source, mode);
		this.selected = selected;
		this.type = Type.VALUE_SELECTED;
	}

	public SearchEvent(Object source, SearchMode mode, String searched) {
		this(source, mode);
		this.searched = searched;
		this.type = Type.VALUE_SEARCHED;
	}

	public SearchEvent(Object source, SearchMode mode) {
		super(source);
		this.mode = mode;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return the mode
	 */
	public SearchMode getMode() {
		return mode;
	}

	/**
	 * @return the selected
	 */
	public Object getSelected() {
		return selected;
	}

	/**
	 * @return the searched
	 */
	public String getSearched() {
		return searched;
	}

}
