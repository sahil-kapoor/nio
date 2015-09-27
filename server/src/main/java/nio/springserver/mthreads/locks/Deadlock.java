package nio.springserver.mthreads.locks;

import nio.springserver.mthreads.helpers.ISchedulingRule;

public class Deadlock {
	// all the threads which are involved in the deadlock
	private Thread[] threads;
	// the thread whose locks will be suspended to resolve deadlock
	private Thread candidate;
	// the locks that will be suspended
	private ISchedulingRule[] locks;

	public Deadlock(Thread[] threads, ISchedulingRule[] locks,
			Thread candidate) {
		this.threads = threads;
		this.locks = locks;
		this.candidate = candidate;
	}

	public ISchedulingRule[] getLocks() {
		return locks;
	}

	public Thread getCandidate() {
		return candidate;
	}

	public Thread[] getThreads() {
		return threads;
	}
}
