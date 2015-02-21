package com.runescape;

public final class LinkedList {

	private final int size;
	private int available;
	private final HashTable table = new HashTable(1024);
	private final Deque deque = new Deque();

	public LinkedList(int length) {
		size = length;
		available = length;
	}

	public QueueLink get(long id) {
		QueueLink l = (QueueLink) table.get(id);
		if (l != null) {
			deque.push(l);
		}
		return l;
	}

	public void put(QueueLink l, long id) {
		if (available == 0) {
			QueueLink l1 = deque.pull();
			l1.unlink();
			l1.unlist();
		} else {
			available--;
		}
		table.put(id, l);
		deque.push(l);
	}

	public void clear() {
		for (;;) {
			QueueLink l = deque.pull();
			if (l == null) {
				break;
			}
			l.unlink();
			l.unlist();
		}
		available = size;
	}
}
