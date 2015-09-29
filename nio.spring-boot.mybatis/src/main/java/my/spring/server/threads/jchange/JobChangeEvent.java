package my.spring.server.threads.jchange;

import my.spring.server.threads.Job;
import my.spring.server.threads.status.IStatus;

public class JobChangeEvent implements IJobChangeEvent {
    /**
     * The job on which this event occurred.
     */
    public Job job = null;
    /**
     * The result returned by the job's run method, or <code>null</code> if not
     * applicable.
     */
    public IStatus result = null;
    /**
     * The amount of time to wait after scheduling the job before it should be
     * run, or <code>-1</code> if not applicable for this type of event.
     */
    public long delay = -1;
    /**
     * Whether this job is being immediately rescheduled.
     */
    public boolean reschedule = false;

    /*
     * (non-Javadoc) Method declared on IJobChangeEvent
     */
    @Override
    public long getDelay() {
	return delay;
    }

    /*
     * (non-Javadoc) Method declared on IJobChangeEvent
     */
    @Override
    public Job getJob() {
	return job;
    }

    /*
     * (non-Javadoc) Method declared on IJobChangeEvent
     */
    @Override
    public IStatus getResult() {
	return result;
    }
}
