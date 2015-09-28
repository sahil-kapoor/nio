package nio.springserver.mthreads.locks;

import java.util.HashMap;
import java.util.Stack;

import nio.springserver.exceptions.CoreException;
import nio.springserver.mthreads.helpers.ISchedulingRule;
import nio.springserver.mthreads.helpers.JobManager;
import nio.springserver.mthreads.status.IStatus;
import nio.springserver.mthreads.status.MultiStatus;
import nio.springserver.mthreads.status.Status;
import nio.springserver.mthreads.workers.Worker;

public class LockManager {
	/**
	 * This class captures the state of suspended locks. Locks are suspended if
	 * deadlock is detected.
	 */
	private static class LockState {
		private int depth;
		private OrderedLock lock;

		/**
		 * Suspends ownership of the given lock, and returns the saved state.
		 */
		protected static LockState suspend(OrderedLock lock) {
			LockState state = new LockState();
			state.lock = lock;
			state.depth = lock.forceRelease();
			return state;
		}

		/**
		 * Re-acquires a suspended lock and reverts to the correct lock depth.
		 */
		public void resume() {
			// spin until the lock is successfully acquired
			// NOTE: spinning here allows the UI thread to service pending
			// syncExecs
			// if the UI thread is waiting to acquire a lock.
			while (true) {
				try {
					if (lock.acquire(Long.MAX_VALUE))
						break;
				} catch (InterruptedException e) {
					// ignore and loop
				}
			}
			lock.setDepth(depth);
		}
	}

	// the lock listener for this lock manager
	protected LockListener lockListener;
	/*
	 * The internal data structure that stores all the relationships between the
	 * locks (or rules) and the threads that own them.
	 */
	private DeadlockDetector locks = new DeadlockDetector();
	/*
	 * Stores thread - stack pairs where every entry in the stack is an array of
	 * locks that were suspended while the thread was acquiring more locks (a
	 * stack is needed because when a thread tries to re-acquire suspended
	 * locks, it can cause deadlock, and some locks it owns can be suspended
	 * again)
	 */
	private HashMap suspendedLocks = new HashMap();

	public LockManager() {
		super();
	}

	/*
	 * (non-Javadoc) Method declared on LockListener
	 */
	public void aboutToRelease() {
		if (lockListener == null)
			return;
		try {
			lockListener.aboutToRelease();
		} catch (Exception e) {
			handleException(e);
		} catch (LinkageError e) {
			handleException(e);
		}
	}

	/*
	 * (non-Javadoc) Method declared on LockListener
	 */
	public boolean canBlock() {
		if (lockListener == null)
			return true;
		try {
			return lockListener.canBlock();
		} catch (Exception e) {
			handleException(e);
		} catch (LinkageError e) {
			handleException(e);
		}
		return false;
	}

	/*
	 * (non-Javadoc) Method declared on LockListener
	 */
	public boolean aboutToWait(Thread lockOwner) {
		if (lockListener == null)
			return false;
		try {
			return lockListener.aboutToWait(lockOwner);
		} catch (Exception e) {
			handleException(e);
		} catch (LinkageError e) {
			handleException(e);
		}
		return false;
	}

	/**
	 * This thread has just acquired a lock. Update graph.
	 */
	public void addLockThread(Thread thread, ISchedulingRule lock) {
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return;
		try {
			synchronized (tempLocks) {
				tempLocks.lockAcquired(thread, lock);
			}
		} catch (Exception e) {
			handleInternalError(e);
		}
	}

	/**
	 * This thread has just been refused a lock. Update graph and check for
	 * deadlock.
	 */
	public void addLockWaitThread(Thread thread, ISchedulingRule lock) {
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return;
		try {
			Deadlock found = null;
			synchronized (tempLocks) {
				found = tempLocks.lockWaitStart(thread, lock);
			}
			if (found == null)
				return;
			// if deadlock was detected, the found variable will contain all the
			// information about it,
			// including which locks to suspend for which thread to resolve the
			// deadlock.
			ISchedulingRule[] toSuspend = found.getLocks();
			LockState[] suspended = new LockState[toSuspend.length];
			for (int i = 0; i < toSuspend.length; i++)
				suspended[i] = LockState.suspend((OrderedLock) toSuspend[i]);
			synchronized (suspendedLocks) {
				Stack prevLocks = (Stack) suspendedLocks
						.get(found.getCandidate());
				if (prevLocks == null)
					prevLocks = new Stack();
				prevLocks.push(suspended);
				suspendedLocks.put(found.getCandidate(), prevLocks);
			}
		} catch (Exception e) {
			handleInternalError(e);
		}
	}

	/**
	 * Handles exceptions that occur while calling third party code from within
	 * the LockManager. This is essentially an in-lined version of
	 * Platform.run(ISafeRunnable)
	 */
	private static void handleException(Throwable e) {
		IStatus status;
		if (e instanceof CoreException) {
			// logged message should not be translated
			status = new MultiStatus(JobManager.PI_JOBS,
					JobManager.PLUGIN_ERROR, "LockManager.handleException", //$NON-NLS-1$
					e);
			((MultiStatus) status).merge(((CoreException) e).getStatus());
		} else {
			status = new Status(IStatus.ERROR, JobManager.PI_JOBS,
					JobManager.PLUGIN_ERROR, "LockManager.handleException", //$NON-NLS-1$
					e);
		}
	}

	/**
	 * There was an internal error in the deadlock detection code. Shut the
	 * entire thing down to prevent further errors. Recovery is too complex as
	 * it requires freezing all threads and inferring the present lock state.
	 */
	private void handleInternalError(Throwable t) {
		try {
			handleException(t);
			handleException(new Exception(locks.toDebugString()));
		} catch (Exception e2) {
			// ignore failure to log or to create the debug string
		}
		// discard the deadlock detector for good
		locks = null;
	}

	/**
	 * Returns true IFF the underlying graph is empty. For debugging purposes
	 * only.
	 */
	public boolean isEmpty() {
		return locks.isEmpty();
	}

	/**
	 * Returns true IFF this thread either owns, or is waiting for, any locks or
	 * rules.
	 */
	public boolean isLockOwner() {
		// all job threads have to be treated as lock owners because UI thread
		// may try to join a job
		Thread current = Thread.currentThread();
		if (current instanceof Worker)
			return true;
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return false;
		synchronized (tempLocks) {
			return tempLocks.contains(Thread.currentThread());
		}
	}

	/**
	 * Creates and returns a new lock.
	 */
	public synchronized OrderedLock newLock() {
		return new OrderedLock(this);
	}

	/**
	 * Releases all the acquires that were called on the given rule. Needs to be
	 * called only once.
	 */
	public void removeLockCompletely(Thread thread, ISchedulingRule rule) {
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return;
		try {
			synchronized (tempLocks) {
				tempLocks.lockReleasedCompletely(thread, rule);
			}
		} catch (Exception e) {
			handleInternalError(e);
		}
	}

	/**
	 * This thread has just released a lock. Update graph.
	 */
	public void removeLockThread(Thread thread, ISchedulingRule lock) {
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return;
		try {
			synchronized (tempLocks) {
				tempLocks.lockReleased(thread, lock);
			}
		} catch (Exception e) {
			handleInternalError(e);
		}
	}

	/**
	 * This thread has just stopped waiting for a lock. Update graph.
	 */
	public void removeLockWaitThread(Thread thread, ISchedulingRule lock) {
		DeadlockDetector tempLocks = locks;
		if (tempLocks == null)
			return;
		try {
			synchronized (tempLocks) {
				tempLocks.lockWaitStop(thread, lock);
			}
		} catch (Exception e) {
			handleInternalError(e);
		}
	}

	/**
	 * Resumes all the locks that were suspended while this thread was waiting
	 * to acquire another lock.
	 */
	public void resumeSuspendedLocks(Thread owner) {
		LockState[] toResume;
		synchronized (suspendedLocks) {
			Stack prevLocks = (Stack) suspendedLocks.get(owner);
			if (prevLocks == null)
				return;
			toResume = (LockState[]) prevLocks.pop();
			if (prevLocks.empty())
				suspendedLocks.remove(owner);
		}
		for (int i = 0; i < toResume.length; i++)
			toResume[i].resume();
	}

	public void setLockListener(LockListener listener) {
		this.lockListener = listener;
	}
}
