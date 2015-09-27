package nio.springserver.mthreads.status;

import nio.springserver.mthreads.Job;
import nio.springserver.mthreads.helpers.JobManager;

/**
 * Standard implementation of the IJobStatus interface.
 */
public class JobStatus extends Status implements IJobStatus {
	private Job job;

	/**
	 * Creates a new job status with no interesting error code or exception.
	 * 
	 * @param severity
	 * @param job
	 * @param message
	 */
	public JobStatus(int severity, Job job, String message) {
		super(severity, JobManager.PI_JOBS, 1, message, null);
		this.job = job;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.IJobStatus#getJob()
	 */
	public Job getJob() {
		return job;
	}
}
