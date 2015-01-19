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
package dane.runescape.mapeditor.search;

import com.runescape.FloorType;
import com.runescape.LocationInfo;
import com.runescape.NPCInfo;
import com.runescape.ObjectInfo;
import static dane.runescape.mapeditor.search.SearchMode.FLOOR;
import static dane.runescape.mapeditor.search.SearchMode.ITEM;
import static dane.runescape.mapeditor.search.SearchMode.LOCATION;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility to help keep track of searchable objects.
 *
 * @author Dane
 */
public class SearchManager {

	private static final Map<SearchMode, List<Searchable>> searchables = new HashMap<>();

	static {
		// create those lists homie
		for (SearchMode m : SearchMode.values()) {
			searchables.put(m, new ArrayList<>());
		}
	}

	/**
	 * Returns all of the available values for the provided mode.
	 *
	 * @param mode the mode
	 * @return the list of searchables
	 */
	public static final List<Searchable> getAll(SearchMode mode) {
		return searchables.get(mode);
	}

	/**
	 * Returns all of the available values for the provided mode that's name
	 * contains the provided string.
	 *
	 * @param mode the mode
	 * @param s the string
	 * @return the list of found results
	 */
	public static final List<Searchable> find(SearchMode mode, String s) {
		List<Searchable> found = new ArrayList<>();
		s = s.toLowerCase();

		List<Searchable> list = searchables.get(mode);

		if (list == null) {
			return found;
		}

		for (Searchable searchable : list) {
			if (searchable.getName().toLowerCase().contains(s)) {
				found.add(searchable);
			}
		}
		return found;
	}

	/**
	 * Clears and then re-populates the specified mode's list.
	 *
	 * @param mode the mode to repopulate
	 */
	public static final void populate(SearchMode mode) {
		List<Searchable> list = searchables.get(mode);
		list.clear();

		// I could automate this, but I cba atm.
		// The way I would: each mode points to a class that has an Object get(int index) an int getCount().
		switch (mode) {
			case FLOOR: {
				for (FloorType f : FloorType.instances) {
					list.add(new Searchable(f.name, f.index, FLOOR));
				}
				break;
			}
			case ITEM: {
				for (int i = 0; i < ObjectInfo.count; i++) {
					ObjectInfo c = ObjectInfo.get(i);
					list.add(new Searchable(c.name, i, ITEM));
				}
				break;
			}
			case LOCATION: {
				for (int i = 0; i < LocationInfo.count; i++) {
					LocationInfo c = LocationInfo.get(i);
					list.add(new Searchable(c.name, i, LOCATION));
				}
				break;
			}
			case NPC: {
				for (int i = 0; i < NPCInfo.count; i++) {
					NPCInfo c = NPCInfo.get(i);
					list.add(new Searchable(c.name, i, LOCATION));
				}
				break;
			}
		}
	}

}
