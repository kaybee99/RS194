/*
 * The MIT License
 *
 * Copyright 2015 Dane.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dane.runescape.mapeditor.event;

import dane.runescape.mapeditor.search.SearchMode;
import java.util.EventObject;

/**
 *
 * @author Dane
 */
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
