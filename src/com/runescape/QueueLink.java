package com.runescape;

/**
 * Couldn't think of a better name.
 *
 * @author Dane
 *
 */
public class QueueLink extends Link {

	QueueLink nextQueue;
	QueueLink previousQueue;

	public void unlist() {
		if (previousQueue != null) {
			previousQueue.nextQueue = nextQueue;
			nextQueue.previousQueue = previousQueue;
			nextQueue = null;
			previousQueue = null;
		}
	}
}
