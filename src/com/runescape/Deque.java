package com.runescape;

public final class Deque {

	QueueLink head = new QueueLink();

	public Deque() {
		head.nextQueue = head;
		head.previousQueue = head;
	}

	public void push(QueueLink l) {
		if (l.previousQueue != null) {
			l.unlist();
		}
		l.previousQueue = head.previousQueue;
		l.nextQueue = head;
		l.previousQueue.nextQueue = l;
		l.nextQueue.previousQueue = l;
	}

	public QueueLink pull() {
		QueueLink l = head.nextQueue;
		if (l == head) {
			return null;
		}
		l.unlist();
		return l;
	}
}
