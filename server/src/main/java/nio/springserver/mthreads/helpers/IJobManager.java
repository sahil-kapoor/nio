package nio.springserver.mthreads.helpers;

import nio.springserver.exceptions.OperationCanceledException;
import nio.springserver.mthreads.Job;
import nio.springserver.mthreads.jchange.IJobChangeListener;
import nio.springserver.mthreads.locks.ILock;
import nio.springserver.mthreads.locks.LockListener;
import nio.springserver.mthreads.progress.IProgressMonitor;
import nio.springserver.mthreads.progress.ProgressProvider;

public interface IJobManager {
	/**
	 * A system property key indicating whether the job manager should create
	 * job threads as daemon threads. Set to <code>true</code> to force all
	 * worker threads to be created as daemon threads. Set to <code>false</code>
	 * to force all worker threads to be created as non-daemon threads.
	 * 
	 * @since 3.3
	 */
	public static final String PROP_USE_DAEMON_THREADS = "eclipse.jobs.daemon"; //$NON-NLS-1$

	/**
	 * Registers a job listener with the job manager. Has no effect if an
	 * identical listener is already registered.
	 * 
	 * @param listener
	 *            the listener to be added
	 * @see #removeJobChangeListener(IJobChangeListener)
	 * @see IJobChangeListener
	 */
	public void addJobChangeListener(IJobChangeListener listener);

	/**
	 * Begins applying this rule in the calling thread. If the rule conflicts
	 * with another rule currently running in another thread, this method blocks
	 * until there are no conflicting rules. Calls to <tt>beginRule</tt> must
	 * eventually be followed by a matching call to <tt>endRule</tt> in the same
	 * thread and with the identical rule instance.
	 * <p>
	 * Rules can be nested only if the rule for the inner <tt>beginRule</tt> is
	 * contained within the rule for the outer <tt>beginRule</tt>. Rule
	 * containment is tested with the API method
	 * <tt>ISchedulingRule.contains</tt>. Also, begin/end pairs must be strictly
	 * nested. Only the rule that has most recently begun can be ended at any
	 * given time.
	 * <p>
	 * A rule of <code>null</code> can be used, but will be ignored for
	 * scheduling purposes. The outermost non-null rule in the thread will be
	 * used for scheduling. A <code>null</code> rule that is begun must still be
	 * ended.
	 * <p>
	 * If this method is called from within a job that has a scheduling rule,
	 * the given rule must also be contained within the rule for the running
	 * job.
	 * <p>
	 * Note that <tt>endRule</tt> must be called even if <tt>beginRule</tt>
	 * fails. The recommended usage is:
	 * 
	 * <pre>
	 * final ISchedulingRule rule = ...;
	 * try {
	 * 	manager.beginRule(rule, monitor);
	 * } finally {
	 * 	manager.endRule(rule);
	 * }
	 * </pre>
	 * 
	 * @param rule
	 *            the rule to begin applying in this thread, or
	 *            <code>null</code>
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress reporting
	 *            and cancellation are not desired
	 * @throws IllegalArgumentException
	 *             if the rule is not strictly nested within all other rules
	 *             currently active for this thread
	 * @throws OperationCanceledException
	 *             if the supplied monitor reports cancelation before the rule
	 *             becomes available
	 * @see ISchedulingRule#contains(ISchedulingRule)
	 */
	public void beginRule(ISchedulingRule rule, IProgressMonitor monitor);

	/**
	 * Cancels all jobs in the given job family. Jobs in the family that are
	 * currently waiting will be removed from the queue. Sleeping jobs will be
	 * discarded without having a chance to wake up. Currently executing jobs
	 * will be asked to cancel but there is no guarantee that they will do so.
	 * 
	 * @param family
	 *            the job family to cancel, or <code>null</code> to cancel all
	 *            jobs
	 * @return
	 * @see Job#belongsTo(Object)
	 */
	public boolean cancel(Object family);

	/**
	 * Returns a progress monitor that can be used to provide aggregated
	 * progress feedback on a set of running jobs. A user interface will
	 * typically group all jobs in a progress group together, providing progress
	 * feedback for individual jobs as well as aggregated progress for the
	 * entire group. Jobs in the group may be run sequentially, in parallel, or
	 * some combination of the two.
	 * <p>
	 * Recommended usage (this snippet runs two jobs in sequence in a single
	 * progress group):
	 * 
	 * <pre>
	 * Job parseJob, compileJob;
	 * IProgressMonitor pm = Platform.getJobManager().createProgressGroup();
	 * try {
	 * 	pm.beginTask("Building", 10);
	 * 	parseJob.setProgressGroup(pm, 5);
	 * 	parseJob.schedule();
	 * 	compileJob.setProgressGroup(pm, 5);
	 * 	compileJob.schedule();
	 * 	parseJob.join();
	 * 	compileJob.join();
	 * } finally {
	 * 	pm.done();
	 * }
	 * </pre>
	 * 
	 * @see Job#setProgressGroup(IProgressMonitor, int)
	 * @see IProgressMonitor
	 * @return a progress monitor
	 */
	public IProgressMonitor createProgressGroup();

	/**
	 * Returns the scheduling rule currently held by this thread, or
	 * <code>null</code> if the current thread does not hold any scheduling
	 * rule.
	 * <p>
	 * If this method is called from within the scope of a running job with a
	 * non-null scheduling rule, then this method is equivalent to calling
	 * <code>currentJob().getRule()</code>. Otherwise, this method will return
	 * the first scheduling rule obtained by this thread via
	 * {@link #beginRule(ISchedulingRule, IProgressMonitor)} that has not yet
	 * had a corresponding call to {@link #endRule(ISchedulingRule)}.
	 * </p>
	 * 
	 * @return the current rule or <code>null</code>
	 * @since 3.5
	 */
	public ISchedulingRule currentRule();

	/**
	 * Returns the job that is currently running in this thread, or
	 * <code>null</code> if there is no currently running job.
	 * 
	 * @return the job or <code>null</code>
	 */
	public Job currentJob();

	/**
	 * Ends the application of a rule to the calling thread. Calls to
	 * <tt>endRule</tt> must be preceded by a matching call to
	 * <tt>beginRule</tt> in the same thread with an identical rule instance.
	 * <p>
	 * Rules can be nested only if the rule for the inner <tt>beginRule</tt> is
	 * contained within the rule for the outer <tt>beginRule</tt>. Also,
	 * begin/end pairs must be strictly nested. Only the rule that has most
	 * recently begun can be ended at any given time.
	 * 
	 * @param rule
	 *            the rule to end applying in this thread
	 * @throws IllegalArgumentException
	 *             if this method is called on a rule for which there is no
	 *             matching begin, or that does not match the most recent begin.
	 * @see ISchedulingRule#contains(ISchedulingRule)
	 */
	public void endRule(ISchedulingRule rule);

	/**
	 * Returns all waiting, executing and sleeping jobs belonging to the given
	 * family. If no jobs are found, an empty array is returned.
	 * 
	 * @param family
	 *            the job family to find, or <code>null</code> to find all jobs
	 * @return the job array
	 * @see Job#belongsTo(Object)
	 */
	public Job[] find(Object family);

	/**
	 * Returns whether the job manager is currently idle. The job manager is
	 * idle if no jobs are currently running or waiting to run.
	 * 
	 * @return <code>true</code> if the job manager is idle, and
	 *         <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isIdle();

	/**
	 * Returns whether the job manager is currently suspended.
	 * 
	 * @return <code>true</code> if the job manager is suspended, and
	 *         <code>false</code> otherwise
	 * @since 3.4
	 * @see #suspend()
	 * @see #resume()
	 */
	public boolean isSuspended();

	/**
	 * Waits until all jobs of the given family are finished. This method will
	 * block the calling thread until all such jobs have finished executing, or
	 * until this thread is interrupted. If there are no jobs in the family that
	 * are currently waiting, running, or sleeping, this method returns
	 * immediately. Feedback on how the join is progressing is provided to a
	 * progress monitor.
	 * <p>
	 * If this method is called while the job manager is suspended, only jobs
	 * that are currently running will be joined; Once there are no jobs in the
	 * family in the {@link Job#RUNNING} state, this method returns.
	 * </p>
	 * <p>
	 * Note that there is a deadlock risk when using join. If the calling thread
	 * owns a lock or object monitor that the joined thread is waiting for,
	 * deadlock will occur. This method can also result in starvation of the
	 * current thread if another thread continues to add jobs of the given
	 * family, or if a job in the given family reschedules itself in an infinite
	 * loop.
	 * </p>
	 * 
	 * @param family
	 *            the job family to join, or <code>null</code> to join all jobs.
	 * @param monitor
	 *            Progress monitor for reporting progress on how the wait is
	 *            progressing, or <code>null</code> if no progress monitoring is
	 *            required.
	 * @exception InterruptedException
	 *                if this thread is interrupted while waiting
	 * @exception OperationCanceledException
	 *                if the progress monitor is canceled while waiting
	 * @see Job#belongsTo(Object)
	 * @see #suspend()
	 */
	public void join(Object family, IProgressMonitor monitor)
			throws InterruptedException, OperationCanceledException;

	/**
	 * Creates a new lock object. All lock objects supplied by the job manager
	 * know about each other and will always avoid circular deadlock amongst
	 * themselves.
	 * 
	 * @return the new lock object
	 */
	public ILock newLock();

	/**
	 * Removes a job listener from the job manager. Has no effect if an
	 * identical listener is not already registered.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * @see #addJobChangeListener(IJobChangeListener)
	 * @see IJobChangeListener
	 */
	public void removeJobChangeListener(IJobChangeListener listener);

	/**
	 * Resumes execution of jobs after a previous <code>suspend</code>. All jobs
	 * that were sleeping or waiting prior to the suspension, or that were
	 * scheduled while the job manager was suspended, will now be eligible for
	 * execution.
	 * <p>
	 * Calling this method on a rule that is not suspended has no effect. If
	 * another thread also owns the rule at the time this method is called, then
	 * the rule will not be resumed until all threads have released the rule.
	 * 
	 * @deprecated This method is not safe and should not be used. Suspending a
	 *             scheduling rule violates the thread safety of clients that
	 *             use scheduling rules as a mutual exclusion mechanism, and can
	 *             result in concurrency problems in all clients that use the
	 *             suspended rule.
	 * @see #suspend(ISchedulingRule, IProgressMonitor)
	 */
	@Deprecated
	public void resume(ISchedulingRule rule);

	/**
	 * Resumes execution of jobs after a previous <code>suspend</code>. All jobs
	 * that were sleeping or waiting prior to the suspension, or that were
	 * scheduled while the job manager was suspended, will now be eligible for
	 * execution.
	 * <p>
	 * Calling <code>resume</code> when the job manager is not suspended has no
	 * effect.
	 * 
	 * @see #suspend()
	 * @see #isSuspended()
	 */
	public void resume();

	/**
	 * Provides a hook that is notified whenever a thread is about to wait on a
	 * lock, or when a thread is about to release a lock. This hook must only be
	 * set once.
	 * <p>
	 * This method is for internal use by the platform-related plug-ins. Clients
	 * should not call this method.
	 * </p>
	 * 
	 * @see LockListener
	 */
	public void setLockListener(LockListener listener);

	/**
	 * Registers a progress provider with the job manager. If there was a
	 * provider already registered, it is replaced.
	 * <p>
	 * This method is intended for use by the currently executing Eclipse
	 * application. Plug-ins outside the currently running application should
	 * not call this method.
	 * </p>
	 * 
	 * @param provider
	 *            the new provider, or <code>null</code> if no progress is
	 *            needed
	 */
	public void setProgressProvider(ProgressProvider provider);

	/**
	 * Suspends execution of all jobs. Jobs that are already running when this
	 * method is invoked will complete as usual, but all sleeping and waiting
	 * jobs will not be executed until the job manager is resumed.
	 * <p>
	 * The job manager will remain suspended until a subsequent call to
	 * <code>resume</code>. Further calls to <code>suspend</code> when the job
	 * manager is already suspended are ignored.
	 * <p>
	 * All attempts to join sleeping and waiting jobs while the job manager is
	 * suspended will return immediately.
	 * <p>
	 * Note that this very powerful function should be used with extreme
	 * caution. Suspending the job manager will prevent all jobs in the system
	 * from executing, which may have adverse affects on components that are
	 * relying on execution of jobs. The job manager should never be suspended
	 * without intent to resume execution soon afterwards.
	 * 
	 * @see #resume()
	 * @see #join(Object, IProgressMonitor)
	 * @see #isSuspended()
	 */
	public void suspend();

	/**
	 * Defers execution of all jobs with scheduling rules that conflict with the
	 * given rule. The caller will be blocked until all currently executing jobs
	 * with conflicting rules are completed. Conflicting jobs that are sleeping
	 * or waiting at the time this method is called will not be executed until
	 * the rule is resumed.
	 * <p>
	 * While a rule is suspended, all calls to <code>beginRule</code> and
	 * <code>endRule</code> on a suspended rule will not block the caller. The
	 * rule remains suspended until a subsequent call to
	 * <code>resume(ISchedulingRule)</code> with the identical rule instance.
	 * Further calls to <code>suspend</code> with an identical rule prior to
	 * calling <code>resume</code> are ignored.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancelation are provided by the
	 * given progress monitor. In the case of cancelation, the rule will not be
	 * suspended.
	 * </p>
	 * Note: this very powerful function should be used with extreme caution.
	 * Suspending rules will prevent jobs in the system from executing, which
	 * may have adverse effects on components that are relying on execution of
	 * jobs. The job manager should never be suspended without intent to resume
	 * execution soon afterwards. Deadlock will result if the thread responsible
	 * for resuming the rule attempts to join a suspended job.
	 * 
	 * @deprecated This method is not safe and should not be used. Suspending a
	 *             scheduling rule violates the thread safety of clients that
	 *             use scheduling rules as a mutual exclusion mechanism, and can
	 *             result in concurrency problems in all clients that use the
	 *             suspended rule.
	 * @param rule
	 *            The scheduling rule to suspend. Must not be <code>null</code>.
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress reporting
	 *            is not desired
	 * @exception OperationCanceledException
	 *                if the operation is canceled. Cancelation can occur even
	 *                if no progress monitor is provided.
	 * @see #resume(ISchedulingRule)
	 */
	@Deprecated
	public void suspend(ISchedulingRule rule, IProgressMonitor monitor);

	/**
	 * Requests that all jobs in the given job family be suspended. Jobs
	 * currently waiting to be run will be removed from the queue and moved into
	 * the <code>SLEEPING</code> state. Jobs that have been put to sleep will
	 * remain in that state until either resumed or canceled. This method has no
	 * effect on jobs that are not currently waiting to be run.
	 * <p>
	 * Sleeping jobs can be resumed using <code>wakeUp</code>.
	 * 
	 * @param family
	 *            the job family to sleep, or <code>null</code> to sleep all
	 *            jobs.
	 * @return
	 * @see Job#belongsTo(Object)
	 */
	public boolean sleep(Object family);

	/**
	 * Transfers ownership of a scheduling rule to another thread. The identical
	 * scheduling rule must currently be owned by the calling thread as a result
	 * of a previous call to <code>beginRule</code>. The destination thread must
	 * not already own a scheduling rule.
	 * <p>
	 * Calling this method is equivalent to atomically calling
	 * <code>endRule</code> in the calling thread followed by an immediate
	 * <code>beginRule</code> in the destination thread. The destination thread
	 * is responsible for subsequently calling <code>endRule</code> when it is
	 * finished using the rule.
	 * <p>
	 * This method has no effect when the destination thread is the same as the
	 * calling thread.
	 * 
	 * @param rule
	 *            The scheduling rule to transfer
	 * @param destinationThread
	 *            The new owner for the transferred rule.
	 * @since 3.1
	 */
	public void transferRule(ISchedulingRule rule, Thread destinationThread);

	/**
	 * Resumes scheduling of all sleeping jobs in the given family. This method
	 * has no effect on jobs in the family that are not currently sleeping.
	 * 
	 * @param family
	 *            the job family to wake up, or <code>null</code> to wake up all
	 *            jobs
	 * @see Job#belongsTo(Object)
	 */
	public void wakeUp(Object family);
}
