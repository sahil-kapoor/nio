package my.spring.server.threads.jchange;

public interface IJobChangeListener {
	/**
	 * Notification that a job is about to be run. Listeners are allowed to
	 * sleep, cancel, or change the priority of the job before it is started
	 * (and as a result may prevent the run from actually occurring).
	 * 
	 * @param event
	 *            the event details
	 */
	public void aboutToRun(IJobChangeEvent event);

	/**
	 * Notification that a job was previously sleeping and has now been
	 * rescheduled to run.
	 * 
	 * @param event
	 *            the event details
	 */
	public void awake(IJobChangeEvent event);

	/**
	 * Notification that a job has completed execution, either due to
	 * cancelation, successful completion, or failure. The event status object
	 * indicates how the job finished, and the reason for failure, if
	 * applicable.
	 * 
	 * @param event
	 *            the event details
	 */
	public void done(IJobChangeEvent event);

	/**
	 * Notification that a job has started running.
	 * 
	 * @param event
	 *            the event details
	 */
	public void running(IJobChangeEvent event);

	/**
	 * Notification that a job is being added to the queue of scheduled jobs.
	 * The event details includes the scheduling delay before the job should
	 * start running.
	 * 
	 * @param event
	 *            the event details, including the job instance and the
	 *            scheduling delay
	 */
	public void scheduled(IJobChangeEvent event);

	/**
	 * Notification that a job was waiting to run and has now been put in the
	 * sleeping state.
	 * 
	 * @param event
	 *            the event details
	 */
	public void sleeping(IJobChangeEvent event);
}
