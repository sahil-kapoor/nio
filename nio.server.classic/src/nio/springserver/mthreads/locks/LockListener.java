package nio.springserver.mthreads.locks;

import nio.springserver.mthreads.Job;
import nio.springserver.mthreads.helpers.JobManager;

public class LockListener {
	private final LockManager manager = ((JobManager) Job.getJobManager())
			.getLockManager();

	/**
	 * Notification that a thread is about to block on an attempt to acquire a
	 * lock. Returns whether the thread should be granted immediate access to
	 * the lock.
	 * <p>
	 * This default implementation always returns <code>false</code>. Subclasses
	 * may override.
	 * 
	 * @param lockOwner
	 *            the thread that currently owns the lock this thread is waiting
	 *            for, or <code>null</code> if unknown.
	 * @return <code>true</code> if the thread should be granted immediate
	 *         access, and <code>false</code> if it should wait for the lock to
	 *         be available
	 */
	public boolean aboutToWait(Thread lockOwner) {
		return false;
	}

	/**
	 * Notification that a thread is about to release a lock.
	 * <p>
	 * This default implementation does nothing. Subclasses may override.
	 */
	public void aboutToRelease() {
		// do nothing
	}

	/**
	 * Returns if it is safe for the calling thread to block while waiting to
	 * obtain a lock. When blocking in the calling thread is not safe, the
	 * caller will ensure that the thread is kept alive and responsive to
	 * cancellation while waiting.
	 * 
	 * @return <code>true</code> if this thread can block, and
	 *         <code>false</code> otherwise.
	 * 
	 * @since org.eclipse.core.jobs 3.5
	 */
	public boolean canBlock() {
		return true;
	}

	/**
	 * Returns whether this thread currently owns any locks
	 * 
	 * @return <code>true</code> if this thread owns any locks, and
	 *         <code>false</code> otherwise.
	 */
	protected final boolean isLockOwnerThread() {
		return manager.isLockOwner();
	}
}
