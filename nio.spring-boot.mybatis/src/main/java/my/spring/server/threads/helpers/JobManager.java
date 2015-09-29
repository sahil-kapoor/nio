package my.spring.server.threads.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.spring.server.exceptions.OperationCanceledException;
import my.spring.server.threads.InternalJob;
import my.spring.server.threads.Job;
import my.spring.server.threads.jchange.IJobChangeEvent;
import my.spring.server.threads.jchange.IJobChangeListener;
import my.spring.server.threads.jchange.JobChangeAdapter;
import my.spring.server.threads.jchange.JobChangeEvent;
import my.spring.server.threads.locks.ILock;
import my.spring.server.threads.locks.LockListener;
import my.spring.server.threads.locks.LockManager;
import my.spring.server.threads.progress.IProgressMonitor;
import my.spring.server.threads.progress.IProgressMonitorWithBlocking;
import my.spring.server.threads.progress.NullProgressMonitor;
import my.spring.server.threads.progress.ProgressMonitorWrapper;
import my.spring.server.threads.progress.ProgressProvider;
import my.spring.server.threads.status.IStatus;
import my.spring.server.threads.status.JobStatus;
import my.spring.server.threads.status.Status;
import my.spring.server.threads.things.Counter;
import my.spring.server.threads.things.JobMessages;
import my.spring.server.threads.things.JobQueue;
import my.spring.server.threads.things.Semaphore;
import my.spring.server.threads.workers.InternalWorker;
import my.spring.server.threads.workers.Worker;
import my.spring.server.threads.workers.WorkerPool;
import my.spring.server.utils.Assert;

public class JobManager implements IJobManager {
    private static Logger log = LoggerFactory.getLogger(JobManager.class);

    /**
     * The unique identifier constant of this plug-in.
     */
    public static final String PI_JOBS = JobManager.class.getPackage().getName();

    /**
     * Status code constant indicating an error occurred while running a
     * plug-in. For backward compatibility with Platform.PLUGIN_ERROR left at
     * (value = 2).
     */
    public static final int PLUGIN_ERROR = 2;

    @SuppressWarnings("unused")
    private static final String OPTION_DEADLOCK_ERROR = PI_JOBS + "/jobs/errorondeadlock";
    @SuppressWarnings("unused")
    private static final String OPTION_DEBUG_BEGIN_END = PI_JOBS + "/jobs/beginend";
    @SuppressWarnings("unused")
    private static final String OPTION_DEBUG_YIELDING = PI_JOBS + "/jobs/yielding";
    @SuppressWarnings("unused")
    private static final String OPTION_DEBUG_YIELDING_DETAILED = PI_JOBS + "/jobs/yielding/detailed";
    @SuppressWarnings("unused")
    private static final String OPTION_DEBUG_JOBS = PI_JOBS + "/jobs";
    @SuppressWarnings("unused")
    private static final String OPTION_DEBUG_JOBS_TIMING = PI_JOBS + "/jobs/timing";
    @SuppressWarnings("unused")
    private static final String OPTION_LOCKS = PI_JOBS + "/jobs/locks";
    @SuppressWarnings("unused")
    private static final String OPTION_SHUTDOWN = PI_JOBS + "/jobs/shutdown";

    public static boolean DEBUG = true;
    public static boolean DEBUG_BEGIN_END = DEBUG;
    public static boolean DEBUG_YIELDING = DEBUG;
    public static boolean DEBUG_YIELDING_DETAILED = DEBUG;
    public static boolean DEBUG_DEADLOCK = DEBUG;
    public static boolean DEBUG_LOCKS = DEBUG;
    public static boolean DEBUG_TIMING = DEBUG;
    public static boolean DEBUG_SHUTDOWN = DEBUG;
    // private static DateFormat DEBUG_FORMAT;

    /**
     * The singleton job manager instance. It must be a singleton because all
     * job instances maintain a reference (as an optimization) and have no way
     * of updating it.
     */
    private static JobManager instance;
    /**
     * Scheduling rule used for validation of client-defined rules.
     */
    private static final ISchedulingRule nullRule = new ISchedulingRule() {
	@Override
	public boolean contains(ISchedulingRule rule) {
	    return rule == this;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
	    return rule == this;
	}
    };

    /**
     * True if this manager is active, and false otherwise. A job manager starts
     * out active, and becomes inactive if it has been shutdown.
     */
    private volatile boolean active = true;

    public final ImplicitJobs implicitJobs = new ImplicitJobs(this);

    private final JobListeners jobListeners = new JobListeners();

    /**
     * The lock for synchronizing all activity in the job manager. To avoid
     * deadlock, this lock must never be held for extended periods, and must
     * never be held while third party code is being
     * called. @GuardedBy("itself")
     */
    private final Object lock = new Object();

    private final LockManager lockManager = new LockManager();

    /**
     * The pool of worker threads.
     */
    private WorkerPool pool;

    /**
     * @GuardedBy("lock")
     */
    private ProgressProvider progressProvider = null;
    /**
     * Jobs that are currently running. Should only be modified from
     * changeState @GuardedBy("lock")
     */
    private final HashSet<InternalJob> running;

    /**
     * Jobs that are currently yielding. Should only be modified from
     * changeState @GuardedBy("lock")
     */
    private final HashSet<InternalJob> yielding;

    /**
     * Jobs that are sleeping. Some sleeping jobs are scheduled to wake up at a
     * given start time, while others will sleep indefinitely until woken.
     * Should only be modified from changeState @GuardedBy("lock")
     */
    private final JobQueue sleeping;
    /**
     * True if this manager has been suspended, and false otherwise. A job
     * manager starts out not suspended, and becomes suspended when
     * <code>suspend</code> is invoked. Once suspended, no jobs will start
     * running until <code>resume</code> is called. @GuardedBy("lock")
     */
    private boolean suspended = false;

    /**
     * jobs that are waiting to be run. Should only be modified from
     * changeState @GuardedBy("lock")
     */
    private final JobQueue waiting;

    /**
     * ThreadJobs that are waiting to be run. Should only be modified from
     * changeState @GuardedBy("lock")
     */
    final JobQueue waitingThreadJobs;

    /**
     * Counter to record wait queue insertion order. @GuardedBy("lock")
     */
    Counter waitQueueCounter = new Counter();

    /**
     * A set of progress monitors we must track cancellation requests
     * for. @GuardedBy("itself")
     */
    public final List<Object[]> monitorStack = new ArrayList<Object[]>();

    private final InternalWorker internalWorker;

    public static void debug(String msg) {
	StringBuffer msgBuf = new StringBuffer(msg.length() + 40);
	if (DEBUG_TIMING) {
	    // lazy initialize to avoid overhead when not debugging
	    // if (DEBUG_FORMAT == null)
	    // DEBUG_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
	    // DEBUG_FORMAT.format(new Date(), msgBuf, new FieldPosition(0));
	    // msgBuf.append('-');
	}
	msgBuf.append('[').append(Thread.currentThread()).append(']').append(msg);
	log.info(msgBuf.toString());
    }

    /**
     * Returns the job manager singleton. For internal use only.
     */
    public static synchronized JobManager getInstance() {
	if (instance == null)
	    new JobManager();
	return instance;
    }

    /**
     * For debugging purposes only
     */
    private static String printJobName(Job job) {
	if (job instanceof ThreadJob) {
	    Job realJob = ((ThreadJob) job).realJob;
	    if (realJob != null)
		return realJob.getClass().getName();
	    return "ThreadJob on rule: " + job.getRule();
	}
	return job.getClass().getName();
    }

    /**
     * For debugging purposes only
     */
    public static String printState(Job job) {
	return printState(((InternalJob) job).internalGetState());
    }

    /**
     * For debugging purposes only
     */
    public static String printState(int state) {
	switch (state) {
	case Job.NONE:
	    return "NONE";
	case Job.WAITING:
	    return "WAITING";
	case Job.SLEEPING:
	    return "SLEEPING";
	case Job.RUNNING:
	    return "RUNNING";
	case InternalJob.BLOCKED:
	    return "BLOCKED";
	case InternalJob.YIELDING:
	    return "YIELDING";
	case InternalJob.ABOUT_TO_RUN:
	    return "ABOUT_TO_RUN";
	case InternalJob.ABOUT_TO_SCHEDULE:
	    return "ABOUT_TO_SCHEDULE";
	}
	return "UNKNOWN";
    }

    /**
     * Note that although this method is not API, clients have historically used
     * it to force jobs shutdown in cases where OSGi shutdown does not occur.
     * For this reason, this method should be considered near-API and should not
     * be changed if at all possible.
     */
    public static void shutdown() {
	if (instance != null) {
	    instance.doShutdown();
	    instance = null;
	}
    }

    private JobManager() {
	instance = this;
	initDebugOptions();
	synchronized (lock) {
	    waiting = new JobQueue(false);
	    waitingThreadJobs = new JobQueue(false, false);
	    sleeping = new JobQueue(true);
	    running = new HashSet<InternalJob>(10);
	    yielding = new HashSet<InternalJob>(10);
	    pool = new WorkerPool(this);
	}
	// TODO change here daemons if need
	pool.setDaemon(false);
	internalWorker = new InternalWorker(this);
	// TODO change here if need
	internalWorker.setDaemon(false);
	internalWorker.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.jobs.IJobManager#addJobListener(org.eclipse.core
     * .runtime.jobs.IJobChangeListener)
     */
    @Override
    public void addJobChangeListener(IJobChangeListener listener) {
	jobListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.jobs.IJobManager#beginRule(org.eclipse.core.
     * runtime.jobs.ISchedulingRule, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void beginRule(ISchedulingRule rule, IProgressMonitor monitor) {
	validateRule(rule);
	implicitJobs.begin(rule, monitorFor(monitor), false);
    }

    /**
     * Cancels a job
     */
    protected boolean cancel(InternalJob job) {
	IProgressMonitor monitor = null;
	boolean runCanceling = false;
	synchronized (lock) {
	    switch (job.getState()) {
	    case Job.NONE:
		return true;
	    case Job.RUNNING:
		// cannot cancel a job that has already started (as opposed
		// to ABOUT_TO_RUN)
		if (job.internalGetState() == Job.RUNNING) {
		    monitor = job.getProgressMonitor();
		    runCanceling = !job.isRunCanceled();
		    if (runCanceling)
			job.setRunCanceled(true);
		    break;
		}
		// signal that the job should be canceled before it gets a
		// chance to run
		job.setAboutToRunCanceled(true);
		return false;
	    default:
		changeState(job, Job.NONE);
	    }
	}
	// call monitor and canceling outside sync block
	if (monitor != null) {
	    if (runCanceling) {
		if (!monitor.isCanceled())
		    monitor.setCanceled(true);
		job.canceling();
	    }
	    return false;
	}
	// only notify listeners if the job was waiting or sleeping
	jobListeners.done((Job) job, Status.CANCEL_STATUS, false);
	return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#cancel(java.lang.String)
     */
    @Override
    public boolean cancel(Object family) {
	// don't synchronize because cancel calls listeners
	for (Iterator<InternalJob> it = select(family).iterator(); it.hasNext();)
	    cancel(it.next());
	return true;
    }

    /**
     * Atomically updates the state of a job, adding or removing from the
     * necessary queues or sets.
     */
    private void changeState(InternalJob job, int newState) {
	boolean blockedJobs = false;
	synchronized (lock) {
	    synchronized (job.jobStateLock) {
		job.jobStateLock.notifyAll();
		int oldState = job.internalGetState();
		switch (oldState) {
		case InternalJob.YIELDING:
		    yielding.remove(job);
		case Job.NONE:
		case InternalJob.ABOUT_TO_SCHEDULE:
		    break;
		case InternalJob.BLOCKED:
		    // remove this job from the linked list of blocked jobs
		    job.remove();
		    break;
		case Job.WAITING:
		    try {
			waiting.remove(job);
		    } catch (RuntimeException e) {
			Assert.isLegal(false, "Tried to remove a job that wasn't in the queue");
		    }
		    break;
		case Job.SLEEPING:
		    try {
			sleeping.remove(job);
		    } catch (RuntimeException e) {
			Assert.isLegal(false, "Tried to remove a job that wasn't in the queue");
		    }
		    break;
		case Job.RUNNING:
		case InternalJob.ABOUT_TO_RUN:
		    running.remove(job);
		    // add any blocked jobs back to the wait queue
		    InternalJob blocked = job.previous();
		    job.remove();
		    blockedJobs = blocked != null;
		    while (blocked != null) {
			InternalJob previous = blocked.previous();
			changeState(blocked, Job.WAITING);
			blocked = previous;
		    }
		    break;
		default:
		    Assert.isLegal(false, "Invalid job state: " + job + ", state: " + oldState);
		}
		job.internalSetState(newState);
		switch (newState) {
		case Job.NONE:
		    job.setStartTime(InternalJob.T_NONE);
		    job.setWaitQueueStamp(InternalJob.T_NONE);
		    job.setRunCanceled(false);
		case InternalJob.BLOCKED:
		    break;
		case Job.WAITING:
		    waiting.enqueue(job);
		    break;
		case Job.SLEEPING:
		    try {
			sleeping.enqueue(job);
		    } catch (RuntimeException e) {
			throw new RuntimeException("Error changing from state: " + oldState);
		    }
		    break;
		case Job.RUNNING:
		case InternalJob.ABOUT_TO_RUN:
		    // These flags must be reset in all cases, including
		    // resuming from yield
		    job.setStartTime(InternalJob.T_NONE);
		    job.setWaitQueueStamp(InternalJob.T_NONE);
		    running.add(job);
		    break;
		case InternalJob.YIELDING:
		    yielding.add(job);
		case InternalJob.ABOUT_TO_SCHEDULE:
		    break;
		default:
		    Assert.isLegal(false, "Invalid job state: " + job + ", state: " + newState);
		}
	    }
	}
	// notify queue outside sync block
	if (blockedJobs)
	    pool.jobQueued();
    }

    /**
     * Returns a new progress monitor for this job, belonging to the given
     * progress group. Returns null if it is not a valid time to set the job's
     * group.
     */
    public IProgressMonitor createMonitor(InternalJob job, IProgressMonitor group, int ticks) {
	synchronized (lock) {
	    // group must be set before the job is scheduled
	    // this includes the ABOUT_TO_SCHEDULE state, during which it is
	    // still
	    // valid to set the progress monitor
	    if (job.getState() != Job.NONE)
		return null;
	    IProgressMonitor monitor = null;
	    if (progressProvider != null)
		monitor = progressProvider.createMonitor((Job) job, group, ticks);
	    if (monitor == null)
		monitor = new NullProgressMonitor();
	    return monitor;
	}
    }

    /**
     * Returns a new progress monitor for this job. Never returns
     * null. @GuardedBy("lock")
     */
    private IProgressMonitor createMonitor(Job job) {
	IProgressMonitor monitor = null;
	if (progressProvider != null)
	    monitor = progressProvider.createMonitor(job);
	if (monitor == null)
	    monitor = new NullProgressMonitor();
	return monitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#createProgressGroup()
     */
    @Override
    public IProgressMonitor createProgressGroup() {
	if (progressProvider != null)
	    return progressProvider.createProgressGroup();
	return new NullProgressMonitor();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#currentJob()
     */
    @Override
    public Job currentJob() {
	Thread current = Thread.currentThread();
	if (current instanceof Worker)
	    return ((Worker) current).currentJob();
	synchronized (lock) {
	    for (Iterator<InternalJob> it = running.iterator(); it.hasNext();) {
		Job job = (Job) it.next();
		if (job.getThread() == current)
		    return job;
	    }
	}
	return null;
    }

    @Override
    public ISchedulingRule currentRule() {
	// check thread job first, because actual current job may have null rule
	Job currentJob = implicitJobs.getThreadJob(Thread.currentThread());
	if (currentJob != null)
	    return currentJob.getRule();
	currentJob = currentJob();
	if (currentJob != null)
	    return currentJob.getRule();
	return null;
    }

    /**
     * Returns the delay in milliseconds that a job with a given priority can
     * tolerate waiting.
     */
    private long delayFor(int priority) {
	// these values may need to be tweaked based on machine speed
	switch (priority) {
	case Job.INTERACTIVE:
	    return 0L;
	case Job.SHORT:
	    return 50L;
	case Job.LONG:
	    return 100L;
	case Job.BUILD:
	    return 500L;
	case Job.DECORATE:
	    return 1000L;
	default:
	    Assert.isTrue(false, "Job has invalid priority: " + priority);
	    return 0;
	}
    }

    /**
     * Performs the scheduling of a job. Does not perform any notifications.
     */
    private void doSchedule(InternalJob job, long delay) {
	synchronized (lock) {
	    // job may have been canceled already
	    int state = job.internalGetState();
	    if (state != InternalJob.ABOUT_TO_SCHEDULE && state != Job.SLEEPING)
		return;
	    // if it's a decoration job with no rule, don't run it right now if
	    // the system is busy
	    if (job.getPriority() == Job.DECORATE && job.getRule() == null) {
		long minDelay = running.size() * 100;
		delay = Math.max(delay, minDelay);
	    }
	    if (delay > 0) {
		job.setStartTime(System.currentTimeMillis() + delay);
		changeState(job, Job.SLEEPING);
	    } else {
		job.setStartTime(System.currentTimeMillis() + delayFor(job.getPriority()));
		job.setWaitQueueStamp(waitQueueCounter.increment());
		changeState(job, Job.WAITING);
	    }
	}
    }

    /**
     * Shuts down the job manager. Currently running jobs will be told to stop,
     * but worker threads may still continue processing. (note: This implemented
     * IJobManager.shutdown which was removed due to problems caused by
     * premature shutdown)
     */
    private void doShutdown() {
	Job[] toCancel = null;
	synchronized (lock) {
	    if (!active)
		return;
	    active = false;
	    // cancel all running jobs
	    toCancel = running.toArray(new Job[running.size()]);
	    // discard any jobs that have not yet started running
	    sleeping.clear();
	    waiting.clear();
	}

	// Give running jobs a chance to finish. Wait 0.1 seconds for up to 3
	// times.
	if (toCancel != null && toCancel.length > 0) {
	    for (int i = 0; i < toCancel.length; i++) {
		cancel(toCancel[i]); // cancel jobs outside sync block to avoid
				     // deadlock
	    }

	    for (int waitAttempts = 0; waitAttempts < 3; waitAttempts++) {
		Thread.yield();
		synchronized (lock) {
		    if (running.isEmpty())
			break;
		}
		if (DEBUG_SHUTDOWN) {
		    JobManager.debug("Shutdown - job wait cycle #" + (waitAttempts + 1));
		    Job[] stillRunning = null;
		    synchronized (lock) {
			stillRunning = running.toArray(new Job[running.size()]);
		    }
		    if (stillRunning != null) {
			for (int j = 0; j < stillRunning.length; j++) {
			    JobManager.debug("\tJob: " + printJobName(stillRunning[j]));
			}
		    }
		}
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    // ignore
		}
		Thread.yield();
	    }

	    synchronized (lock) { // retrieve list of the jobs that are still
				  // running
		toCancel = running.toArray(new Job[running.size()]);
	    }
	}
	internalWorker.cancel();
	if (toCancel != null) {
	    for (int i = 0; i < toCancel.length; i++) {
		String jobName = printJobName(toCancel[i]);
		// this doesn't need to be translated because it's just being
		// logged
		String msg = "Job found still running after platform shutdown.  Jobs should be canceled by the plugin that scheduled them during shutdown: "
			+ jobName;

		// TODO the RuntimeLog.log in its current implementation won't
		// produce a log
		// during this stage of shutdown. For now add a standard error
		// output.
		// One the logging story is improved, the System.err output
		// below can be removed:
		System.err.println(msg);
	    }
	}
	synchronized (lock) {
	    // discard reference to any jobs still running at this point
	    running.clear();
	}

	pool.shutdown();
    }

    /**
     * Indicates that a job was running, and has now finished. Note that this
     * method can be called under OutOfMemoryError conditions and thus must be
     * paranoid about allocating objects.
     */
    public void endJob(InternalJob job, IStatus result, boolean notify) {
	long rescheduleDelay = InternalJob.T_NONE;
	synchronized (lock) {
	    // if the job is finishing asynchronously, there is nothing more to
	    // do for now
	    if (result == Job.ASYNC_FINISH)
		return;
	    // if job is not known then it cannot be done
	    if (job.getState() == Job.NONE)
		return;
	    if (JobManager.DEBUG && notify)
		JobManager.debug("Ending job: " + job);
	    job.setResult(result);
	    job.setProgressMonitor(null);
	    job.setThread(null);
	    rescheduleDelay = job.getStartTime();
	    changeState(job, Job.NONE);
	}
	// notify listeners outside sync block
	final boolean reschedule = active && rescheduleDelay > InternalJob.T_NONE && job.shouldSchedule();
	if (notify)
	    jobListeners.done((Job) job, result, reschedule);
	// reschedule the job if requested and we are still active
	if (reschedule)
	    schedule(job, rescheduleDelay, reschedule);
	// log result if it is warning or error
	if ((result.getSeverity() & (IStatus.ERROR | IStatus.WARNING)) != 0) {
	    // TODO log here
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#endRule(org.eclipse.core.
     * runtime.jobs.ISchedulingRule)
     */
    @Override
    public void endRule(ISchedulingRule rule) {
	implicitJobs.end(rule, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#find(java.lang.String)
     */
    @Override
    public Job[] find(Object family) {
	List<InternalJob> members = select(family);
	return members.toArray(new Job[members.size()]);
    }

    /**
     * Returns a running or blocked job whose scheduling rule conflicts with the
     * scheduling rule of the given waiting job. Returns null if there are no
     * conflicting jobs. A job can only run if there are no running jobs and no
     * blocked jobs whose scheduling rule conflicts with its rule.
     */
    protected InternalJob findBlockingJob(InternalJob waitingJob) {
	if (waitingJob.getRule() == null)
	    return null;
	synchronized (lock) {
	    if (running.isEmpty())
		return null;
	    // check the running jobs
	    boolean hasBlockedJobs = false;
	    for (Iterator<InternalJob> it = running.iterator(); it.hasNext();) {
		InternalJob job = it.next();
		if (waitingJob.isConflicting(job))
		    return job;
		if (!hasBlockedJobs)
		    hasBlockedJobs = job.previous() != null;
	    }
	    // there are no blocked jobs, so we are done
	    if (!hasBlockedJobs)
		return null;
	    // check all jobs blocked by running jobs
	    for (Iterator<InternalJob> it = running.iterator(); it.hasNext();) {
		InternalJob job = it.next();
		while (true) {
		    job = job.previous();
		    if (job == null)
			break;
		    if (waitingJob.isConflicting(job))
			return job;
		}
	    }
	}
	return null;
    }

    /**
     * Returns a job from the given collection whose scheduling rule conflicts
     * with the scheduling rule of the given job. Returns null if there are no
     * conflicting jobs.
     */
    InternalJob findBlockedJob(InternalJob job, Iterator<InternalJob> jobs) {
	synchronized (lock) {
	    while (jobs.hasNext()) {
		InternalJob waitingJob = jobs.next();
		if (waitingJob.isConflicting(job))
		    return waitingJob;
	    }
	    return null;
	}
    }

    void dequeue(JobQueue queue, InternalJob job) {
	synchronized (lock) {
	    queue.remove(job);
	}
    }

    void enqueue(JobQueue queue, InternalJob job) {
	synchronized (lock) {
	    queue.enqueue(job);
	}
    }

    public LockManager getLockManager() {
	return lockManager;
    }

    /**
     * Returns a translated message indicating we are waiting for the given
     * number of jobs to complete.
     */
    private String getWaitMessage(int jobCount) {
	String message = jobCount == 1 ? JobMessages.jobs_waitFamSubOne : JobMessages.jobs_waitFamSub;
	return message;
    }

    private void initDebugOptions() {
	DEBUG = true;
	DEBUG_BEGIN_END = true;
	DEBUG_YIELDING = true;
	DEBUG_YIELDING_DETAILED = true;
	DEBUG_DEADLOCK = true;
	DEBUG_LOCKS = true;
	DEBUG_TIMING = true;
	DEBUG_SHUTDOWN = true;
    }

    /**
     * Returns whether the job manager is active (has not been shutdown).
     */
    public boolean isActive() {
	return active;
    }

    /**
     * Returns true if the given job is blocking the execution of a non-system
     * job.
     */
    public boolean isBlocking(InternalJob runningJob) {
	synchronized (lock) {
	    // if this job isn't running, it can't be blocking anyone
	    if (runningJob.getState() != Job.RUNNING)
		return false;
	    // if any job is queued behind this one, it is blocked by it
	    InternalJob previous = runningJob.previous();
	    while (previous != null) {
		// ignore jobs of lower priority (higher priority value means
		// lower priority)
		if (previous.getPriority() < runningJob.getPriority()) {
		    if (!previous.isSystem())
			return true;
		    // implicit jobs should interrupt unless they act on behalf
		    // of system jobs
		    if (previous instanceof ThreadJob && ((ThreadJob) previous).shouldInterrupt())
			return true;
		}
		previous = previous.previous();
	    }
	    // none found
	    return false;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#isIdle()
     */
    @Override
    public boolean isIdle() {
	synchronized (lock) {
	    return running.isEmpty() && waiting.isEmpty();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#isSuspended()
     */
    @Override
    public boolean isSuspended() {
	synchronized (lock) {
	    return suspended;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.jobs.Job#job(org.eclipse.core.runtime.jobs.Job)
     */
    public void join(InternalJob job) {
	final IJobChangeListener listener;
	final Semaphore barrier;
	synchronized (lock) {
	    int state = job.getState();
	    if (state == Job.NONE)
		return;
	    // don't join a waiting or sleeping job when suspended (deadlock
	    // risk)
	    if (suspended && state != Job.RUNNING)
		return;
	    // it's an error for a job to join itself
	    if (state == Job.RUNNING && job.getThread() == Thread.currentThread())
		throw new IllegalStateException("Job attempted to join itself");
	    // the semaphore will be released when the job is done
	    barrier = new Semaphore(null);
	    listener = new JobChangeAdapter() {
		@Override
		public void done(IJobChangeEvent event) {
		    barrier.release();
		}
	    };
	    job.addJobChangeListener(listener);
	    // compute set of all jobs that must run before this one
	    // add a listener that removes jobs from the blocking set when they
	    // finish
	}
	// wait until listener notifies this thread.
	try {
	    while (true) {
		// notify hook to service pending syncExecs before falling
		// asleep
		lockManager.aboutToWait(job.getThread());
		try {
		    if (barrier.acquire(Long.MAX_VALUE))
			break;
		} catch (InterruptedException e) {
		    // loop and keep trying
		}
	    }
	} finally {
	    lockManager.aboutToRelease();
	    job.removeJobChangeListener(listener);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobManager#join(String, IProgressMonitor)
     */
    @Override
    public void join(final Object family, IProgressMonitor monitor)
	    throws InterruptedException, OperationCanceledException {
	monitor = monitorFor(monitor);
	IJobChangeListener listener = null;
	final Set<InternalJob> jobs;
	int jobCount;
	Job blocking = null;
	synchronized (lock) {
	    // don't join a waiting or sleeping job when suspended (deadlock
	    // risk)
	    int states = suspended ? Job.RUNNING : Job.RUNNING | Job.WAITING | Job.SLEEPING;
	    jobs = Collections.synchronizedSet(new HashSet<InternalJob>(select(family, states)));
	    jobCount = jobs.size();
	    if (jobCount > 0) {
		// if there is only one blocking job, use it in the blockage
		// callback below
		if (jobCount == 1)
		    blocking = (Job) jobs.iterator().next();
		listener = new JobChangeAdapter() {
		    @Override
		    public void done(IJobChangeEvent event) {
			// don't remove from list if job is being rescheduled
			if (!((JobChangeEvent) event).reschedule)
			    jobs.remove(event.getJob());
		    }

		    // update the list of jobs if new ones are added during the
		    // join
		    @Override
		    public void scheduled(IJobChangeEvent event) {
			// don't add to list if job is being rescheduled
			if (((JobChangeEvent) event).reschedule)
			    return;
			Job job = event.getJob();
			if (job.belongsTo(family))
			    jobs.add(job);
		    }
		};
		addJobChangeListener(listener);
	    }
	}
	if (jobCount == 0) {
	    // use up the monitor outside synchronized block because monitors
	    // call untrusted code
	    monitor.beginTask(JobMessages.jobs_blocked0, 1);
	    monitor.done();
	    return;
	}
	// spin until all jobs are completed
	try {
	    monitor.beginTask(JobMessages.jobs_blocked0, jobCount);
	    monitor.subTask(getWaitMessage(jobCount));
	    reportBlocked(monitor, blocking);
	    int jobsLeft;
	    int reportedWorkDone = 0;
	    while ((jobsLeft = jobs.size()) > 0) {
		// don't let there be negative work done if new jobs have
		// been added since the join began
		int actualWorkDone = Math.max(0, jobCount - jobsLeft);
		if (reportedWorkDone < actualWorkDone) {
		    monitor.worked(actualWorkDone - reportedWorkDone);
		    reportedWorkDone = actualWorkDone;
		    monitor.subTask(getWaitMessage(jobsLeft));
		}
		if (Thread.interrupted())
		    throw new InterruptedException();
		if (monitor.isCanceled())
		    throw new OperationCanceledException();
		// notify hook to service pending syncExecs before falling
		// asleep
		lockManager.aboutToWait(null);
		Thread.sleep(100);
	    }
	} finally {
	    lockManager.aboutToRelease();
	    removeJobChangeListener(listener);
	    reportUnblocked(monitor);
	    monitor.done();
	}
    }

    /**
     * Returns a non-null progress monitor instance. If the monitor is null,
     * returns the default monitor supplied by the progress provider, or a
     * NullProgressMonitor if no default monitor is available.
     */
    private IProgressMonitor monitorFor(IProgressMonitor monitor) {
	if (monitor == null || (monitor instanceof NullProgressMonitor)) {
	    if (progressProvider != null) {
		try {
		    monitor = progressProvider.getDefaultMonitor();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}

	if (monitor == null)
	    return new NullProgressMonitor();
	return monitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobManager#newLock(java.lang.String)
     */
    @Override
    public ILock newLock() {
	return lockManager.newLock();
    }

    /**
     * Removes and returns the first waiting job in the queue. Returns null if
     * there are no items waiting in the queue. If an item is removed from the
     * queue, it is moved to the running jobs list.
     */
    private Job nextJob() {
	synchronized (lock) {
	    // do nothing if the job manager is suspended
	    if (suspended)
		return null;
	    // tickle the sleep queue to see if anyone wakes up
	    long now = System.currentTimeMillis();
	    InternalJob job = sleeping.peek();
	    while (job != null && job.getStartTime() < now) {
		job.setStartTime(now + delayFor(job.getPriority()));
		job.setWaitQueueStamp(waitQueueCounter.increment());
		changeState(job, Job.WAITING);
		job = sleeping.peek();
	    }
	    // process the wait queue until we find a job whose rules are
	    // satisfied.
	    while ((job = waiting.peek()) != null) {
		InternalJob blocker = findBlockingJob(job);
		if (blocker == null)
		    break;
		// queue this job after the job that's blocking it
		changeState(job, InternalJob.BLOCKED);
		// assert job does not already belong to some other data
		// structure
		Assert.isTrue(job.next() == null);
		Assert.isTrue(job.previous() == null);
		blocker.addLast(job);
	    }
	    // the job to run must be in the running list before we exit
	    // the sync block, otherwise two jobs with conflicting rules could
	    // start at once
	    if (job != null) {
		changeState(job, InternalJob.ABOUT_TO_RUN);
		if (JobManager.DEBUG)
		    JobManager.debug("Starting job: " + job);
	    }
	    return (Job) job;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.jobs.IJobManager#removeJobListener(org.eclipse.
     * core.runtime.jobs.IJobChangeListener)
     */
    @Override
    public void removeJobChangeListener(IJobChangeListener listener) {
	jobListeners.remove(listener);
    }

    /**
     * Report to the progress monitor that this thread is blocked, supplying an
     * information message, and if possible the job that is causing the
     * blockage. Important: An invocation of this method MUST be followed
     * eventually be an invocation of reportUnblocked.
     * 
     * @param monitor
     *            The monitor to report blocking to
     * @param blockingJob
     *            The job that is blocking this thread, or <code>null</code>
     * @see #reportUnblocked
     */
    final void reportBlocked(IProgressMonitor monitor, InternalJob blockingJob) {
	if (!(monitor instanceof IProgressMonitorWithBlocking))
	    return;
	IStatus reason;
	if (blockingJob == null || blockingJob instanceof ThreadJob || blockingJob.isSystem()) {
	    reason = new Status(IStatus.INFO, JobManager.PI_JOBS, 1, JobMessages.jobs_blocked0, null);
	} else {
	    String msg = blockingJob.getName();
	    reason = new JobStatus(IStatus.INFO, (Job) blockingJob, msg);
	}
	((IProgressMonitorWithBlocking) monitor).setBlocked(reason);
    }

    /**
     * Reports that this thread was blocked, but is no longer blocked and is
     * able to proceed.
     * 
     * @param monitor
     *            The monitor to report unblocking to.
     * @see #reportBlocked
     */
    final void reportUnblocked(IProgressMonitor monitor) {
	if (monitor instanceof IProgressMonitorWithBlocking)
	    ((IProgressMonitorWithBlocking) monitor).clearBlocked();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#resume()
     */
    @Override
    public final void resume() {
	synchronized (lock) {
	    suspended = false;
	    // poke the job pool
	    pool.jobQueued();
	}
    }

    /**
     * (non-Javadoc)
     * 
     * @deprecated this method should not be used
     * @see my.netty.estudiar.server.mthreads.helpers.core.runtime.jobs.IJobManager#resume(my.netty.estudiar.server.mthreads.helpers.core.runtime.jobs.ISchedulingRule)
     */
    @Override
    @Deprecated
    public final void resume(ISchedulingRule rule) {
	implicitJobs.resume(rule);
    }

    /**
     * Attempts to immediately start a given job. Returns null if the job was
     * successfully started, and the blocking job if it could not be started
     * immediately due to a currently running job with a conflicting rule.
     * Listeners will never be notified of jobs that are run in this way.
     */
    protected InternalJob runNow(ThreadJob job, boolean releaseWaiting) {
	if (releaseWaiting) {
	    synchronized (implicitJobs) {
		synchronized (lock) {
		    return doRunNow(job, releaseWaiting);
		}
	    }
	}
	synchronized (lock) {
	    return doRunNow(job, releaseWaiting);
	}
    }

    private InternalJob doRunNow(ThreadJob job, boolean releaseWaiting) {
	InternalJob blocking = findBlockingJob(job);
	// cannot start if there is a conflicting job
	if (blocking == null) {
	    changeState(job, Job.RUNNING);
	    ((InternalJob) job).setProgressMonitor(new NullProgressMonitor());
	    job.run(null);
	    if (releaseWaiting) {
		// atomically release waiting
		implicitJobs.removeWaiting(job);
	    }
	}
	return blocking;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.Job#schedule(long)
     */
    public void schedule(InternalJob job, long delay, boolean reschedule) {
	if (!active)
	    throw new IllegalStateException("Job manager has been shut down.");
	Assert.isNotNull(job, "Job is null");
	Assert.isLegal(delay >= 0, "Scheduling delay is negative");
	synchronized (lock) {
	    // if the job is already running, set it to be rescheduled when done
	    if (job.getState() == Job.RUNNING) {
		job.setStartTime(delay);
		return;
	    }
	    // can't schedule a job that is waiting or sleeping
	    if (job.internalGetState() != Job.NONE)
		return;
	    if (JobManager.DEBUG)
		JobManager.debug("Scheduling job: " + job);
	    // remember that we are about to schedule the job
	    // to prevent multiple schedule attempts from succeeding (bug 68452)
	    changeState(job, InternalJob.ABOUT_TO_SCHEDULE);
	}
	// notify listeners outside sync block
	jobListeners.scheduled((Job) job, delay, reschedule);
	// schedule the job
	doSchedule(job, delay);
	// call the pool outside sync block to avoid deadlock
	pool.jobQueued();
    }

    /**
     * Adds all family members in the list of jobs to the collection
     */
    private void select(List<InternalJob> members, Object family, InternalJob firstJob, int stateMask) {
	if (firstJob == null)
	    return;
	InternalJob job = firstJob;
	do {
	    // note that job state cannot be NONE at this point
	    if ((family == null || job.belongsTo(family)) && ((job.getState() & stateMask) != 0))
		members.add(job);
	    job = job.previous();
	} while (job != null && job != firstJob);
    }

    /**
     * Returns a list of all jobs known to the job manager that belong to the
     * given family.
     */
    private List<InternalJob> select(Object family) {
	return select(family, Job.WAITING | Job.SLEEPING | Job.RUNNING);
    }

    /**
     * Returns a list of all jobs known to the job manager that belong to the
     * given family and are in one of the provided states.
     */
    private List<InternalJob> select(Object family, int stateMask) {
	List<InternalJob> members = new ArrayList<InternalJob>();
	synchronized (lock) {
	    if ((stateMask & Job.RUNNING) != 0) {
		for (Iterator<InternalJob> it = running.iterator(); it.hasNext();) {
		    select(members, family, it.next(), stateMask);
		}
	    }
	    if ((stateMask & Job.WAITING) != 0) {
		select(members, family, waiting.peek(), stateMask);
		for (Iterator<InternalJob> it = yielding.iterator(); it.hasNext();) {
		    select(members, family, it.next(), stateMask);
		}
	    }
	    if ((stateMask & Job.SLEEPING) != 0)
		select(members, family, sleeping.peek(), stateMask);
	}
	return members;
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobManager#setLockListener(LockListener)
     */
    @Override
    public void setLockListener(LockListener listener) {
	lockManager.setLockListener(listener);
    }

    /**
     * Changes a job priority.
     */
    public void setPriority(InternalJob job, int newPriority) {
	synchronized (lock) {
	    int oldPriority = job.getPriority();
	    if (oldPriority == newPriority)
		return;
	    job.internalSetPriority(newPriority);
	    // if the job is waiting to run, re-shuffle the queue
	    if (job.getState() == Job.WAITING) {
		long oldStart = job.getStartTime();
		job.setStartTime(oldStart + (delayFor(newPriority) - delayFor(oldPriority)));
		waiting.resort(job);
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobManager#setProgressProvider(IProgressProvider)
     */
    @Override
    public void setProgressProvider(ProgressProvider provider) {
	progressProvider = provider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setRule
     */
    public void setRule(InternalJob job, ISchedulingRule rule) {
	synchronized (lock) {
	    // cannot change the rule of a job that is already running
	    Assert.isLegal(job.getState() == Job.NONE);
	    validateRule(rule);
	    job.internalSetRule(rule);
	}
    }

    /**
     * Puts a job to sleep. Returns true if the job was successfully put to
     * sleep.
     */
    protected boolean sleep(InternalJob job) {
	synchronized (lock) {
	    switch (job.getState()) {
	    case Job.RUNNING:
		// cannot be paused if it is already running (as opposed to
		// ABOUT_TO_RUN)
		if (job.internalGetState() == Job.RUNNING)
		    return false;
		// job hasn't started running yet (aboutToRun listener)
		break;
	    case Job.SLEEPING:
		// update the job wake time
		job.setStartTime(InternalJob.T_INFINITE);
		// change state again to re-shuffle the sleep queue
		changeState(job, Job.SLEEPING);
		return true;
	    case Job.NONE:
		return true;
	    case Job.WAITING:
		// put the job to sleep
		break;
	    }
	    job.setStartTime(InternalJob.T_INFINITE);
	    changeState(job, Job.SLEEPING);
	}
	jobListeners.sleeping((Job) job);
	return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobManager#sleep(String)
     */
    @Override
    public boolean sleep(Object family) {
	// don't synchronize because sleep calls listeners
	for (Iterator<InternalJob> it = select(family).iterator(); it.hasNext();) {
	    sleep(it.next());
	}
	return true;
    }

    /**
     * Returns the estimated time in milliseconds before the next job is
     * scheduled to wake up. The result may be negative. Returns
     * InternalJob.T_INFINITE if there are no sleeping or waiting jobs.
     */
    public long sleepHint() {
	synchronized (lock) {
	    // wait forever if job manager is suspended
	    if (suspended)
		return InternalJob.T_INFINITE;
	    if (!waiting.isEmpty())
		return 0L;
	    // return the anticipated time that the next sleeping job will wake
	    InternalJob next = sleeping.peek();
	    if (next == null)
		return InternalJob.T_INFINITE;
	    return next.getStartTime() - System.currentTimeMillis();
	}
    }

    /**
     * Implementation of {@link Job#yieldRule(IProgressMonitor)}
     */
    public Job yieldRule(InternalJob job, IProgressMonitor monitor) {
	Thread currentThread = Thread.currentThread();
	Assert.isLegal(job.getState() == Job.RUNNING,
		"Cannot yieldRule job that is " + printState(job.internalGetState()));
	Assert.isLegal(currentThread == job.getThread(), "Cannot yieldRule from outside job's thread");

	InternalJob unblocked;
	// If job is not a ThreadJob, and it has implicitly started rules,
	// likeThreadJob
	// is the corresponding ThreadJob. Similarly, if likeThreadJob is not
	// null, then
	// job is not a ThreadJob
	ThreadJob likeThreadJob;
	synchronized (implicitJobs) {
	    synchronized (lock) {
		// The nested implicit job, if any
		likeThreadJob = implicitJobs.getThreadJob(currentThread);

		unblocked = job.previous();

		// if unblocked is not null, it was a blocked job. It is
		// guaranteed
		// that it will be the next job run by the worker threads once
		// this
		// lock is released.
		if (unblocked == null) {

		    if (likeThreadJob != null) {

			// look for any explicit jobs we may be blocking
			unblocked = ((InternalJob) likeThreadJob).previous();

			if (unblocked == null) {

			    // look for any implicit (or yielding) jobs we may
			    // be blocking.
			    unblocked = findBlockedJob(likeThreadJob, waitingThreadJobs.iterator());
			}

		    } else {

			// look for any implicit (or yielding) jobs we may be
			// blocking.
			unblocked = findBlockedJob(job, waitingThreadJobs.iterator());
		    }
		}

		// optimization: do nothing if we don't unblock any job
		if (unblocked == null)
		    return null;

		// "release" our rule by exiting RUNNING state
		changeState(job, InternalJob.YIELDING);
		if (DEBUG_YIELDING)
		    JobManager.debug(job + " will yieldRule to " + unblocked);

		if (likeThreadJob != null && likeThreadJob != job) {
		    // if there is a corresponding thread job, it needs yield as
		    // well
		    changeState(likeThreadJob, InternalJob.YIELDING);
		    if (DEBUG_YIELDING)
			JobManager.debug(job + " will yieldRule to " + unblocked);
		}

		if (likeThreadJob != null) {
		    // only null-out threads out for non-ThreadJobs
		    job.setThread(null);
		    if (likeThreadJob.getRule() != null) {
			getLockManager().removeLockThread(currentThread, likeThreadJob.getRule());
		    }
		}

		if ((job.getRule() != null) && !(job instanceof ThreadJob))
		    getLockManager().removeLockThread(currentThread, job.getRule());

	    }
	}
	// To prevent this job from immediately re-grabbing the scheduling rule
	// wait until
	// the unblocked job changes state. This unblocked job is guaranteed to
	// be the
	// next job of the set of similar conflicting rules to attempt to run.
	if (DEBUG_YIELDING_DETAILED)
	    JobManager.debug(job + " is waiting for " + unblocked + " to transition from WAITING state");

	waitForUnblocked(unblocked);

	// restart this job, unless we've been restarted already
	// This is the same as ThreadJob begin, except that cancelation CAN NOT
	// be supported
	// throwing the OperationCanceledException will return execution to the
	// caller.
	IProgressMonitor mon = monitorFor(monitor);
	ProgressMonitorWrapper nonCanceling = new ProgressMonitorWrapper(mon) {
	    @Override
	    public boolean isCanceled() {
		// pass-through request
		getWrappedProgressMonitor().isCanceled();
		// ignore result
		return false;
	    }
	};

	if (DEBUG_YIELDING)
	    JobManager.debug(job + " waiting to resume");

	// this yielding job becomes an implicit job, unless it is one already
	if (likeThreadJob == null) {
	    // Create a Threadjob proxy. This is strictly an internal job, but
	    // its not
	    // preventing from "leaking" out to clients in the form of listener
	    // notifications, and via IJobManager API usage like find().
	    // Set a flag to differentiate it from regular ThreadJobs.
	    ThreadJob threadJob = new ThreadJob(job.getRule()) {
		@Override
		boolean isResumingAfterYield() {
		    return true;
		}
	    };
	    threadJob.setRealJob((Job) job);
	    ThreadJob.joinRun(threadJob, nonCanceling);
	    // the following state changes are atomic
	    synchronized (lock) {
		// Must end the temporary threadJob to remove from running list
		changeState(threadJob, Job.NONE);
		changeState(job, Job.RUNNING);
		job.setThread(currentThread);
	    }
	} else {
	    ThreadJob.joinRun(likeThreadJob, nonCanceling);
	    synchronized (lock) {
		changeState(job, Job.RUNNING);
		job.setThread(currentThread);
	    }
	}
	if (DEBUG_YIELDING) {
	    // extra assert: make sure no other conflicting jobs are running now
	    synchronized (lock) {
		for (Iterator<InternalJob> it = running.iterator(); it.hasNext();) {
		    InternalJob other = it.next();
		    if (other == job)
			continue;
		    Assert.isTrue(!other.isConflicting(job), other + " conflicts and ran simultaneously with " + job);
		}
	    }
	    JobManager.debug(job + " resumed");
	}
	if (unblocked instanceof ThreadJob && ((ThreadJob) unblocked).isResumingAfterYield()) {
	    // if the unblocked job is a proxy for a yielding job to start,
	    // return
	    // the original job. No need to expose the proxy ThreadJob.
	    return ((ThreadJob) unblocked).realJob;
	}
	return (Job) unblocked;
    }

    private void waitForUnblocked(InternalJob theJob) {
	// wait until theJob leaves WAITING state
	boolean interrupted = false;
	synchronized (theJob.jobStateLock) {
	    if (theJob instanceof ThreadJob) {
		// We can't acquire the implicitJob lock while holding
		// jobStateLock,
		// so use isWaiting instead.
		while (((ThreadJob) theJob).isWaiting) {
		    try {
			theJob.jobStateLock.wait();
		    } catch (InterruptedException e) {
			interrupted = true;
		    }
		}
	    } else {
		while (theJob.internalGetState() == Job.WAITING) {
		    try {
			theJob.jobStateLock.wait();
		    } catch (InterruptedException e) {
			interrupted = true;
		    }
		}
	    }
	}
	if (interrupted)
	    Thread.currentThread().interrupt();
    }

    /**
     * Returns the next job to be run, or null if no jobs are waiting to run.
     * The worker must call endJob when the job is finished running.
     */
    public Job startJob() {
	Job job = null;
	while (true) {
	    job = nextJob();
	    if (job == null)
		return null;
	    // must perform this outside sync block because it is third party
	    // code
	    boolean shouldRun = job.shouldRun();
	    // check for listener veto
	    if (shouldRun)
		jobListeners.aboutToRun(job);
	    // listeners may have canceled or put the job to sleep
	    boolean endJob = false;
	    synchronized (lock) {
		InternalJob internal = job;
		synchronized (internal.jobStateLock) {
		    if (internal.internalGetState() == InternalJob.ABOUT_TO_RUN) {
			if (shouldRun && !internal.isAboutToRunCanceled()) {
			    internal.setProgressMonitor(createMonitor(job));
			    // change from ABOUT_TO_RUN to RUNNING
			    internal.internalSetState(Job.RUNNING);
			    internal.jobStateLock.notifyAll();
			    break;
			}
			internal.setAboutToRunCanceled(false);
			endJob = true;
			// fall through and end the job below
		    }
		}
	    }
	    if (endJob) {
		// job has been vetoed or canceled, so mark it as done
		endJob(job, Status.CANCEL_STATUS, true);
		continue;
	    }
	}
	jobListeners.running(job);
	return job;

    }

    /*
     * non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#suspend()
     */
    @Override
    public final void suspend() {
	synchronized (lock) {
	    suspended = true;
	}
    }

    /**
     * (non-Javadoc)
     * 
     * @deprecated this method should not be used
     * @see my.netty.estudiar.server.mthreads.helpers.core.runtime.jobs.IJobManager#suspend(my.netty.estudiar.server.mthreads.helpers.core.runtime.jobs.ISchedulingRule,
     *      my.netty.estudiar.server.mthreads.progress.core.runtime.IProgressMonitor)
     */
    @Override
    @Deprecated
    public final void suspend(ISchedulingRule rule, IProgressMonitor monitor) {
	Assert.isNotNull(rule);
	implicitJobs.suspend(rule, monitorFor(monitor));
    }

    /*
     * non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.jobs.IJobManager#transferRule()
     */
    @Override
    public void transferRule(ISchedulingRule rule, Thread destinationThread) {
	implicitJobs.transfer(rule, destinationThread);
    }

    /**
     * Validates that the given scheduling rule obeys the constraints of
     * scheduling rules as described in the <code>ISchedulingRule</code> javadoc
     * specification.
     */
    private void validateRule(ISchedulingRule rule) {
	// null rule always valid
	if (rule == null)
	    return;
	// contains method must be reflexive
	Assert.isLegal(rule.contains(rule));
	// contains method must return false when given an unknown rule
	Assert.isLegal(!rule.contains(nullRule));
	// isConflicting method must be reflexive
	Assert.isLegal(rule.isConflicting(rule));
	// isConflicting method must return false when given an unknown rule
	Assert.isLegal(!rule.isConflicting(nullRule));
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#wakeUp(long)
     */
    public void wakeUp(InternalJob job, long delay) {
	Assert.isLegal(delay >= 0, "Scheduling delay is negative");
	synchronized (lock) {
	    // cannot wake up if it is not sleeping
	    if (job.getState() != Job.SLEEPING)
		return;
	    doSchedule(job, delay);
	}
	// call the pool outside sync block to avoid deadlock
	pool.jobQueued();

	// only notify of wake up if immediate
	if (delay == 0)
	    jobListeners.awake((Job) job);
    }

    /*
     * (non-Javadoc)
     * 
     * @see IJobFamily#wakeUp(String)
     */
    @Override
    public void wakeUp(Object family) {
	// don't synchronize because wakeUp calls listeners
	for (Iterator<InternalJob> it = select(family).iterator(); it.hasNext();) {
	    wakeUp(it.next(), 0L);
	}
    }

    void endMonitoring(ThreadJob threadJob) {
	synchronized (monitorStack) {
	    for (int i = monitorStack.size() - 1; i >= 0; i--) {
		if (monitorStack.get(i)[0] == threadJob) {
		    monitorStack.remove(i);
		    monitorStack.notifyAll();
		    break;
		}
	    }
	}
    }

    void beginMonitoring(ThreadJob threadJob, IProgressMonitor monitor) {
	synchronized (monitorStack) {
	    monitorStack.add(new Object[] { threadJob, monitor });
	    monitorStack.notifyAll();
	}
    }
}
