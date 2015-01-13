
public final class List {

	private final int size;
	private int available;
	private final Array array = new Array(1024);
	private final Deque deque = new Deque();

	public List(int length) {
		size = length;
		available = length;
	}

	public CacheLink get(long id) {
		CacheLink l = (CacheLink) array.get(id);
		if (l != null) {
			deque.push(l);
		}
		return l;
	}

	public void put(CacheLink l, long id) {
		if (available == 0) {
			CacheLink l1 = deque.pull();
			l1.unlink();
			l1.uncache();
		} else {
			available--;
		}
		array.put(id, l);
		deque.push(l);
	}

	public void clear() {
		for (;;) {
			CacheLink l = deque.pull();
			if (l == null) {
				break;
			}
			l.unlink();
			l.uncache();
		}
		available = size;
	}
}
