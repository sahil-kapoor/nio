package my.spring.server.threads.things;

import java.util.Date;

/**
 * Job plugin message catalog
 */
public class JobMessages {
    // Job Manager and Locks
    public static String jobs_blocked0;
    public static String jobs_blocked1;
    public static String jobs_internalError;
    public static String jobs_waitFamSub;
    public static String jobs_waitFamSubOne;
    // metadata
    public static String meta_pluginProblems;

    static {
	// load message values from bundle file
	reloadMessages();
    }

    public static void reloadMessages() {
    }

    /**
     * Print a debug message to the console. Pre-pend the message with the
     * current date and the name of the current thread.
     */
    public static void message(String message) {
	StringBuffer buffer = new StringBuffer();
	buffer.append(new Date(System.currentTimeMillis()));
	buffer.append(" - ["); //$NON-NLS-1$
	buffer.append(Thread.currentThread().getName());
	buffer.append("] "); //$NON-NLS-1$
	buffer.append(message);
	System.out.println(buffer.toString());
    }
}
