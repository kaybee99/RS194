package com.runescape;

public final class Array {
	private int length;
	private Link[] array;

	public Array(int size) {
		length = size;
		array = new Link[size];

		for (int n = 0; n < size; n++) {
			Link l = array[n] = new Link();
			l.previous = l;
			l.next = l;
		}
	}

	public Link get(long id) {
		Link l = array[(int) (id & (long) (length - 1))];
		for (Link l1 = l.previous; l1 != l; l1 = l1.previous) {
			if (l1.unique == id)
				return l1;
		}
		return null;
	}

	public void put(long id, Link l) {
		if (l.next != null) {
			l.unlink();
		}
		Link l1 = array[(int) (id & (long) (length - 1))];
		l.next = l1.next;
		l.previous = l1;
		l.next.previous = l;
		l.previous.next = l;
		l.unique = id;
	}
}
