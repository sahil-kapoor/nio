package my.spring.server.threads.workers;

import my.spring.server.threads.Job;
import my.spring.server.threads.helpers.JobManager;
import my.spring.server.threads.progress.IProgressMonitor;

/**
 * Used to perform internal JobManager tasks. Currently, this is limited to
 * checking progress monitors while a thread is performing a blocking wait in
 * ThreadJob.
 */
public class InternalWorker extends Thread {
    private final JobManager manager;
    /**
     * @GuardedBy("manager.monitorStack")
     */
    private boolean canceled;

    public InternalWorker(JobManager manager) {
	super("Worker-JM"); //$NON-NLS-1$
	this.manager = manager;
    }

    /**
     * Will loop until there are progress monitors to check. While there are
     * monitors registered, it will check cancelation every 250ms, and if it is
     * canceled it will interrupt the ThreadJob that is performing a blocking
     * wait.
     */
    @Override
    public void run() {
	int timeout = 0;
	synchronized (manager.monitorStack) {
	    while (!canceled) {
		if (manager.monitorStack.isEmpty()) {
		    timeout = 0;
		} else {
		    timeout = 250;
		}
		for (int i = 0; i < manager.monitorStack.size(); i++) {
		    Object[] o = manager.monitorStack.get(i);
		    IProgressMonitor monitor = (IProgressMonitor) o[1];
		    if (monitor.isCanceled()) {
			Job job = (Job) o[0];
			Thread t = job.getThread();
			if (t != null) {
			    t.interrupt();
			}
		    }
		}
		try {
		    manager.monitorStack.wait(timeout);
		} catch (InterruptedException e) {
		    // loop
		}
	    }
	}
    }

    /**
     * Terminate this thread. Once terminated, it cannot be restarted.
     */
    public void cancel() {
	synchronized (manager.monitorStack) {
	    canceled = true;
	    manager.monitorStack.notifyAll();
	}
    }
}
