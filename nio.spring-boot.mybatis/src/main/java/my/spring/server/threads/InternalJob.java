package my.spring.server.threads;

import java.util.Map;

import my.spring.server.threads.helpers.ISchedulingRule;
import my.spring.server.threads.helpers.JobManager;
import my.spring.server.threads.jchange.IJobChangeListener;
import my.spring.server.threads.progress.IProgressMonitor;
import my.spring.server.threads.status.IStatus;
import my.spring.server.threads.things.ListenerList;
import my.spring.server.threads.things.MultiRule;
import my.spring.server.threads.things.ObjectMap;
import my.spring.server.threads.things.QualifiedName;
import my.spring.server.utils.Assert;

@SuppressWarnings("rawtypes")
public abstract class InternalJob extends ParentJob implements Comparable {
    /**
     * Job state code (value 16) indicating that a job has been removed from the
     * wait queue and is about to start running. From an API point of view, this
     * is the same as RUNNING.
     */
    public static final int ABOUT_TO_RUN = 0x10;

    /**
     * Job state code (value 32) indicating that a job has passed scheduling
     * precondition checks and is about to be added to the wait queue. From an
     * API point of view, this is the same as WAITING.
     */
    public static final int ABOUT_TO_SCHEDULE = 0x20;
    /**
     * Job state code (value 8) indicating that a job is blocked by another
     * currently running job. From an API point of view, this is the same as
     * WAITING.
     */
    public static final int BLOCKED = 0x08;
    /**
     * Job state code (value 64) indicating that a job is yielding. From an API
     * point of view, this is the same as WAITING.
     */
    public static final int YIELDING = 0x40;

    // flag mask bits
    private static final int M_STATE = 0xFF;
    private static final int M_SYSTEM = 0x0100;
    private static final int M_USER = 0x0200;

    /*
     * flag on a job indicating that it was about to run, but has been canceled
     */
    private static final int M_ABOUT_TO_RUN_CANCELED = 0x0400;

    /*
     * Flag on a job indicating that it was canceled when running. This flag is
     * used to ensure that #canceling is only ever called once on a job in case
     * of recursive cancelation attempts.
     */
    private static final int M_RUN_CANCELED = 0x0800;

    private static int nextJobNumber = 0;
    protected static final JobManager manager = JobManager.getInstance();

    /**
     * Start time constant indicating a job should be started at a time in the
     * infinite future, causing it to sleep forever.
     */
    public static final long T_INFINITE = Long.MAX_VALUE;
    /**
     * Start time constant indicating that the job has no start time.
     */
    public static final long T_NONE = -1;

    private volatile int flags = Job.NONE;
    private final int jobNumber = getNextJobNumber();
    private ListenerList listeners = null;
    private volatile IProgressMonitor monitor;
    private String name;
    /**
     * The job ahead of me in a queue or list. @GuardedBy("manager.lock")
     */
    private InternalJob next;
    /**
     * The job behind me in a queue or list. @GuardedBy("manager.lock")
     */
    private InternalJob previous;
    private int priority = Job.LONG;
    /**
     * Arbitrary properties (key,value) pairs, attached to a job instance by a
     * third party.
     */
    private ObjectMap properties;

    /**
     * Volatile because it is usually set via a Worker thread and is read via a
     * client thread.
     */
    private volatile IStatus result;
    /**
     * @GuardedBy("manager.lock")
     */
    private ISchedulingRule schedulingRule;
    /**
     * If the job is waiting, this represents the time the job should start by.
     * If this job is sleeping, this represents the time the job should wake up.
     * If this job is running, this represents the delay automatic rescheduling,
     * or -1 if the job should not be rescheduled. @GuardedBy("manager.lock")
     */
    private long startTime;

    /**
     * Stamp added when a job is added to the wait queue. Used to ensure jobs in
     * the wait queue maintain their insertion order even if they are removed
     * from the wait queue temporarily while blocked @GuardedBy("manager.lock")
     */
    private long waitQueueStamp = T_NONE;

    /*
     * The thread that is currently running this job
     */
    private volatile Thread thread = null;

    /**
     * This lock will be held while performing state changes on this job. It is
     * also used as a notifier used to wake up yielding jobs or waiting
     * ThreadJobs when 1) a conflicting job completes and releases a scheduling
     * rule, or 2) when a this job changes state.
     * 
     * See also the lock ordering protocol explanation in JobManager's
     * documentation.
     * 
     * @GuardedBy("itself")
     */
    public final Object jobStateLock = new Object();

    private static synchronized int getNextJobNumber() {
	return nextJobNumber++;
    }

    protected InternalJob(String name) {
	Assert.isNotNull(name);
	this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#addJobListener(IJobChangeListener)
     */
    public void addJobChangeListener(IJobChangeListener listener) {
	if (listeners == null)
	    listeners = new ListenerList(ListenerList.IDENTITY);
	listeners.add(listener);
    }

    /**
     * Adds an entry at the end of the list of which this item is the
     * head. @GuardedBy("manager.lock")
     */
    public final void addLast(InternalJob entry) {
	InternalJob last = this;
	// find the end of the queue
	while (last.previous != null)
	    last = last.previous;
	// add the new entry to the end of the queue
	last.previous = entry;
	entry.next = last;
	entry.previous = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#belongsTo(Object)
     */
    public boolean belongsTo(Object family) {
	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#cancel()
     */
    protected boolean cancel() {
	return manager.cancel(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#canceling()
     */
    public void canceling() {
	// default implementation does nothing
    }

    /*
     * (on-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(Object otherJob) {
	return ((InternalJob) otherJob).startTime >= startTime ? 1 : -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#done(IStatus)
     */
    protected void done(IStatus endResult) {
	manager.endJob(this, endResult, true);
    }

    /**
     * Returns the job listeners that are only listening to this job. Returns
     * <code>null</code> if this job has no listeners.
     */
    public final ListenerList getListeners() {
	return listeners;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getName()
     */
    public String getName() {
	return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getPriority()
     */
    public int getPriority() {
	return priority;
    }

    /**
     * Returns the job's progress monitor, or null if it is not running.
     */
    public final IProgressMonitor getProgressMonitor() {
	return monitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getProperty
     */
    protected Object getProperty(QualifiedName key) {
	// thread safety: (Concurrency001 - copy on write)
	Map temp = properties;
	if (temp == null)
	    return null;
	return temp.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getResult
     */
    protected IStatus getResult() {
	return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getRule
     */
    public ISchedulingRule getRule() {
	return schedulingRule;
    }

    /**
     * Returns the time that this job should be started, awakened, or
     * rescheduled, depending on the current state.
     * 
     * @return time in milliseconds
     */
    public final long getStartTime() {
	return startTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#getState()
     */
    public int getState() {
	int state = flags & M_STATE;
	switch (state) {
	// blocked and yielding state is equivalent to waiting state for
	// clients
	case YIELDING:
	case BLOCKED:
	    return Job.WAITING;
	case ABOUT_TO_RUN:
	    return Job.RUNNING;
	case ABOUT_TO_SCHEDULE:
	    return Job.WAITING;
	default:
	    return state;
	}
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.getThread
     */
    public Thread getThread() {
	return thread;
    }

    /**
     * Returns the raw job state, including internal states no exposed as API.
     */
    public final int internalGetState() {
	return flags & M_STATE;
    }

    /**
     * Must be called from JobManager#setPriority
     */
    public final void internalSetPriority(int newPriority) {
	this.priority = newPriority;
    }

    /**
     * Must be called from JobManager#setRule
     */
    public final void internalSetRule(ISchedulingRule rule) {
	this.schedulingRule = rule;
    }

    /**
     * Must be called from JobManager#changeState
     */
    public final void internalSetState(int i) {
	flags = (flags & ~M_STATE) | i;
    }

    /**
     * Returns whether this job was canceled when it was about to run
     */
    public final boolean isAboutToRunCanceled() {
	return (flags & M_ABOUT_TO_RUN_CANCELED) != 0;
    }

    /**
     * Returns whether this job was canceled when it was running.
     */
    public final boolean isRunCanceled() {
	return (flags & M_RUN_CANCELED) != 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#isBlocking()
     */
    protected boolean isBlocking() {
	return manager.isBlocking(this);
    }

    /**
     * Returns true if this job conflicts with the given job, and false
     * otherwise.
     */
    public final boolean isConflicting(InternalJob otherJob) {
	ISchedulingRule otherRule = otherJob.getRule();
	if (schedulingRule == null || otherRule == null)
	    return false;
	// if one of the rules is a compound rule, it must be asked the
	// question.
	if (schedulingRule.getClass() == MultiRule.class)
	    return schedulingRule.isConflicting(otherRule);
	return otherRule.isConflicting(schedulingRule);
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.isSystem()
     */
    public boolean isSystem() {
	return (flags & M_SYSTEM) != 0;
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.isUser()
     */
    protected boolean isUser() {
	return (flags & M_USER) != 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#join()
     */
    protected void join() throws InterruptedException {
	manager.join(this);
    }

    /**
     * Returns the next entry (ahead of this one) in the list, or null if there
     * is no next entry
     */
    public final InternalJob next() {
	return next;
    }

    /**
     * Returns the previous entry (behind this one) in the list, or null if
     * there is no previous entry
     */
    public final InternalJob previous() {
	return previous;
    }

    /**
     * Removes this entry from any list it belongs to. Returns the receiver.
     */
    public final InternalJob remove() {
	if (next != null)
	    next.setPrevious(previous);
	if (previous != null)
	    previous.setNext(next);
	next = previous = null;
	return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#removeJobListener(IJobChangeListener)
     */
    public void removeJobChangeListener(IJobChangeListener listener) {
	if (listeners != null)
	    listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#run(IProgressMonitor)
     */
    public abstract IStatus run(IProgressMonitor progressMonitor);

    /*
     * (non-Javadoc)
     * 
     * @see Job#schedule(long)
     */
    protected void schedule(long delay) {
	if (shouldSchedule())
	    manager.schedule(this, delay, false);
    }

    /**
     * Sets whether this job was canceled when it was about to run
     */
    public final void setAboutToRunCanceled(boolean value) {
	flags = value ? flags | M_ABOUT_TO_RUN_CANCELED : flags & ~M_ABOUT_TO_RUN_CANCELED;

    }

    /**
     * Sets whether this job was canceled when it was running
     */
    public final void setRunCanceled(boolean value) {
	flags = value ? flags | M_RUN_CANCELED : flags & ~M_RUN_CANCELED;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setName(String)
     */
    protected void setName(String name) {
	Assert.isNotNull(name);
	this.name = name;
    }

    /**
     * Sets the next entry in this linked list of jobs.
     * 
     * @param entry
     */
    public final void setNext(InternalJob entry) {
	this.next = entry;
    }

    /**
     * Sets the previous entry in this linked list of jobs.
     * 
     * @param entry
     */
    public final void setPrevious(InternalJob entry) {
	this.previous = entry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setPriority(int)
     */
    protected void setPriority(int newPriority) {
	switch (newPriority) {
	case Job.INTERACTIVE:
	case Job.SHORT:
	case Job.LONG:
	case Job.BUILD:
	case Job.DECORATE:
	    manager.setPriority(this, newPriority);
	    break;
	default:
	    throw new IllegalArgumentException(String.valueOf(newPriority));
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setProgressGroup(IProgressMonitor, int)
     */
    protected void setProgressGroup(IProgressMonitor group, int ticks) {
	Assert.isNotNull(group);
	IProgressMonitor pm = manager.createMonitor(this, group, ticks);
	if (pm != null)
	    setProgressMonitor(pm);
    }

    /**
     * Sets the progress monitor to use for the next execution of this job, or
     * for clearing the monitor when a job completes.
     * 
     * @param monitor
     *            a progress monitor
     */
    public final void setProgressMonitor(IProgressMonitor monitor) {
	this.monitor = monitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setProperty(QualifiedName,Object)
     */
    protected void setProperty(QualifiedName key, Object value) {
	// thread safety: (Concurrency001 - copy on write)
	if (value == null) {
	    if (properties == null)
		return;
	    ObjectMap temp = (ObjectMap) properties.clone();
	    temp.remove(key);
	    if (temp.isEmpty())
		properties = null;
	    else
		properties = temp;
	} else {
	    ObjectMap temp = properties;
	    if (temp == null)
		temp = new ObjectMap(5);
	    else
		temp = (ObjectMap) properties.clone();
	    temp.put(key, value);
	    properties = temp;
	}
    }

    /**
     * Sets or clears the result of an execution of this job.
     * 
     * @param result
     *            a result status, or
     *            <code>null</code> @GuardedBy("manager.lock")
     */
    public final void setResult(IStatus result) {
	this.result = result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#setRule(ISchedulingRule)
     * 
     * @GuardedBy("manager.lock")
     */
    protected void setRule(ISchedulingRule rule) {
	manager.setRule(this, rule);
    }

    /**
     * Sets a time to start, wake up, or schedule this job, depending on the
     * current state
     * 
     * @param time
     *            a time in milliseconds @GuardedBy("manager.lock")
     */
    public final void setStartTime(long time) {
	startTime = time;
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.setSystem
     */
    protected void setSystem(boolean value) {
	if (getState() != Job.NONE)
	    throw new IllegalStateException();
	flags = value ? flags | M_SYSTEM : flags & ~M_SYSTEM;
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.setThread
     */
    public void setThread(Thread thread) {
	this.thread = thread;
    }

    /*
     * (non-javadoc)
     * 
     * @see Job.setUser
     */
    protected void setUser(boolean value) {
	if (getState() != Job.NONE)
	    throw new IllegalStateException();
	flags = value ? flags | M_USER : flags & ~M_USER;
    }

    /*
     * (Non-javadoc)
     * 
     * @see Job#shouldSchedule
     */
    public boolean shouldSchedule() {
	return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#sleep()
     */
    protected boolean sleep() {
	return manager.sleep(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#yieldRule()
     */
    protected Job yieldRule(IProgressMonitor progressMonitor) {
	return manager.yieldRule(this, progressMonitor);
    }

    /*
     * (non-Javadoc) Prints a string-based representation of this job instance.
     * For debugging purposes only.
     */
    @Override
    public String toString() {
	return getName() + "(" //$NON-NLS-1$
		+ jobNumber + ")"; //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see Job#wakeUp(long)
     */
    protected void wakeUp(long delay) {
	manager.wakeUp(this, delay);
    }

    /**
     * @param waitQueueStamp
     *            The waitQueueStamp to set. @GuardedBy("manager.lock")
     */
    public void setWaitQueueStamp(long waitQueueStamp) {
	this.waitQueueStamp = waitQueueStamp;
    }

    /**
     * @return Returns the waitQueueStamp. @GuardedBy("manager.lock")
     */
    public long getWaitQueueStamp() {
	return waitQueueStamp;
    }
}
