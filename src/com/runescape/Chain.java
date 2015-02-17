package com.runescape;

public final class Chain {

	Link head = new Link();
	Link selected;

	public Chain() {
		head.previous = head;
		head.next = head;
	}

	public void push(Link l) {
		if (l.next != null) {
			l.unlink();
		}
		l.next = head.next;
		l.previous = head;
		l.next.previous = l;
		l.previous.next = l;
	}

	public Link poll() {
		Link last = head.previous;
		if (last == head) {
			return null;
		}
		last.unlink();
		return last;
	}

	public Link peekLast() {
		Link last = head.previous;
		if (last == head) {
			selected = null;
			return null;
		}
		selected = last.previous;
		return last;
	}

	public Link peekFirst() {
		Link first = head.next;
		if (first == head) {
			selected = null;
			return null;
		}
		selected = first.next;
		return first;
	}

	public Link getPrevious() {
		Link l = selected;
		if (l == head) {
			selected = null;
			return null;
		}
		selected = l.previous;
		return l;
	}

	public Link getNext() {
		Link l = selected;
		if (l == head) {
			selected = null;
			return null;
		}
		selected = l.next;
		return l;
	}

	public int size() {
		int i = 0;
		for (Link l = head.previous; l != head; l = l.previous) {
			i++;
		}
		return i;
	}

	public void clear() {
		for (;;) {
			Link l = head.previous;
			if (l == head) {
				break;
			}
			l.unlink();
		}
	}
}
