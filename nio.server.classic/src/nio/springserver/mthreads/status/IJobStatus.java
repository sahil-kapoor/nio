package nio.springserver.mthreads.status;

import nio.springserver.mthreads.Job;

/**
 * Represents status relating to the execution of jobs.
 * 
 * @see my.netty.estudiar.server.mthreads.status.core.runtime.IStatus
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJobStatus extends IStatus {
	/**
	 * Returns the job associated with this status.
	 * 
	 * @return the job associated with this status
	 */
	public Job getJob();
}
