package nio.springserver.utils;

import java.io.PrintStream;
import java.io.PrintWriter;

import nio.springserver.mthreads.status.IStatus;

public class PrintStackUtil {

	static public void printChildren(IStatus status, PrintStream output) {
		IStatus[] children = status.getChildren();
		if (children == null
		    || children.length == 0)
		    return;
		for (int i = 0; i < children.length; i++) {
			output.println(
			    "Contains: " //$NON-NLS-1$
			        + children[i].getMessage());
			Throwable exception = children[i].getException();
			if (exception != null)
			    exception.printStackTrace(
			        output);
			printChildren(
			    children[i],
			    output);
		}
	}

	static public void printChildren(IStatus status, PrintWriter output) {
		IStatus[] children = status.getChildren();
		if (children == null
		    || children.length == 0)
		    return;
		for (int i = 0; i < children.length; i++) {
			output.println(
			    "Contains: " //$NON-NLS-1$
			        + children[i].getMessage());
			output.flush(); // call to synchronize output
			Throwable exception = children[i].getException();
			if (exception != null)
			    exception.printStackTrace(
			        output);
			printChildren(
			    children[i],
			    output);
		}
	}

}