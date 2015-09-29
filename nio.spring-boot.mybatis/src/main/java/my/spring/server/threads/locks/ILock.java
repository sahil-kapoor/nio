package my.spring.server.threads.locks;

public interface ILock {
	/**
	 * Attempts to acquire this lock. If the lock is in use and the specified
	 * delay is greater than zero, the calling thread will block until one of
	 * the following happens:
	 * <ul>
	 * <li>This lock is available</li>
	 * <li>The thread is interrupted</li>
	 * <li>The specified delay has elapsed</li>
	 * </ul>
	 * <p>
	 * While a thread is waiting, locks it already owns may be granted to other
	 * threads if necessary to break a deadlock. In this situation, the calling
	 * thread may be blocked for longer than the specified delay. On returning
	 * from this call, the calling thread will once again have exclusive access
	 * to any other locks it owned upon entering the acquire method.
	 * 
	 * @param delay
	 *            the number of milliseconds to delay
	 * @return <code>true</code> if the lock was successfully acquired, and
	 *         <code>false</code> otherwise.
	 * @exception InterruptedException
	 *                if the thread was interrupted
	 */
	public boolean acquire(long delay) throws InterruptedException;

	/**
	 * Acquires this lock. If the lock is in use, the calling thread will block
	 * until the lock becomes available. If the calling thread owns several
	 * locks, it will be blocked until all threads it requires become available,
	 * or until the thread is interrupted. While a thread is waiting, its locks
	 * may be granted to other threads if necessary to break a deadlock. On
	 * returning from this call, the calling thread will have exclusive access
	 * to this lock, and any other locks it owned upon entering the acquire
	 * method.
	 * <p>
	 * This implementation ignores attempts to interrupt the thread. If response
	 * to interruption is needed, use the method <code>acquire(long)</code>
	 */
	public void acquire();

	/**
	 * Returns the number of nested acquires on this lock that have not been
	 * released. This is the number of times that release() must be called
	 * before the lock is freed.
	 * 
	 * @return the number of nested acquires that have not been released
	 */
	public int getDepth();

	/**
	 * Releases this lock. Locks must only be released by the thread that
	 * currently owns the lock.
	 */
	public void release();
}
