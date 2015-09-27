package nio.springserver.mthreads.things;

/**
 * Simple thread-safe long counter.
 * 
 * @ThreadSafe
 */
public class Counter {
	private long value = 0L;

	public Counter() {
		super();
	}

	public synchronized long increment() {
		return value++;
	}
}
