package com.runescape;

public class Link {
	long unique;
	Link previous;
	Link next;

	public void unlink() {
		if (next != null) {
			next.previous = previous;
			previous.next = next;
			previous = null;
			next = null;
		}
	}
}
