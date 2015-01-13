package com.runescape;

/**
 * Couldn't think of a better name.
 *
 * @author Sven
 *
 */
public class CacheLink extends Link {

	CacheLink next;
	CacheLink previous;

	public void uncache() {
		if (previous != null) {
			previous.next = next;
			next.previous = previous;
			next = null;
			previous = null;
		}
	}
}
