package nio.springserver.mthreads.status;

import nio.springserver.utils.Assert;

public class Status implements IStatus {

	/**
	 * Constant used to indicate an unknown plugin id.
	 */
	private static final String unknownId = "unknown";

	/**
	 * A standard OK status with an "ok" message.
	 *
	 * @since 3.0
	 */
	public static final IStatus OK_STATUS = new Status(OK, unknownId, OK, "ok",
			null);
	/**
	 * A standard CANCEL status with no message.
	 * 
	 * @since 3.0
	 */
	public static final IStatus CANCEL_STATUS = new Status(CANCEL, unknownId, 1,
			"", 
			null);
	/**
	 * The severity. One of
	 * <ul>
	 * <li><code>CANCEL</code></li>
	 * <li><code>ERROR</code></li>
	 * <li><code>WARNING</code></li>
	 * <li><code>INFO</code></li>
	 * <li>or <code>OK</code> (0)</li>
	 * </ul>
	 */
	private int severity = OK;

	/**
	 * Unique identifier of process.
	 */
	private String processName;

	/**
	 * Plug-in-specific status code.
	 */
	private int code;

	/**
	 * Message, localized to the current locale.
	 */
	private String message;

	/**
	 * Wrapped exception, or <code>null</code> if none.
	 */
	private Throwable exception = null;

	/**
	 * Constant to avoid generating garbage.
	 */
	private static final IStatus[] theEmptyStatusArray = new IStatus[0];

	/**
	 * Creates a new status object. The created status has no children.
	 *
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 * @param processName
	 *            the unique identifier of the relevant plug-in
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 */
	public Status(int severity, String processName, int code, String message,
			Throwable exception) {
		setSeverity(severity);
		setProcessName(processName);
		setCode(code);
		setMessage(message);
		setException(exception);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is
	 * <code>OK</code>. The created status has no children.
	 *
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 * @param pluginId
	 *            the unique identifier of the relevant plug-in
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * 
	 * @since org.eclipse.equinox.common 3.3
	 */
	public Status(int severity, String pluginId, String message,
			Throwable exception) {
		setSeverity(severity);
		setProcessName(pluginId);
		setMessage(message);
		setException(exception);
		setCode(OK);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is
	 * <code>OK</code> and exception is <code>null</code>. The created status
	 * has no children.
	 *
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 * @param pluginId
	 *            the unique identifier of the relevant plug-in
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * 
	 * @since org.eclipse.equinox.common 3.3
	 */
	public Status(int severity, String pluginId, String message) {
		setSeverity(severity);
		setProcessName(pluginId);
		setMessage(message);
		setCode(OK);
		setException(null);
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public IStatus[] getChildren() {
		return theEmptyStatusArray;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public int getCode() {
		return code;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public Throwable getException() {
		return exception;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public String getProcessName() {
		return processName;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public int getSeverity() {
		return severity;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public boolean isMultiStatus() {
		return false;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public boolean isOK() {
		return severity == OK;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
	@Override
	public boolean matches(int severityMask) {
		return (severity & severityMask) != 0;
	}

	/**
	 * Sets the status code.
	 *
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 */
	protected void setCode(int code) {
		this.code = code;
	}

	/**
	 * Sets the exception.
	 *
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 */
	protected void setException(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * Sets the message. If null is passed, message is set to an empty string.
	 *
	 * @param message
	 *            a human-readable message, localized to the current locale
	 */
	protected void setMessage(String message) {
		if (message == null)
			this.message = "";
		else
			this.message = message;
	}

	/**
	 * Sets the plug-in id.
	 *
	 * @param processName
	 *            the unique identifier of the relevant plug-in
	 */
	@Override
	public void setProcessName(String processName) {
		Assert.isLegal(processName != null && processName.length() > 0);
		this.processName = processName;
	}

	/**
	 * Sets the severity.
	 *
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 */
	protected void setSeverity(int severity) {
		Assert.isLegal(
				severity == OK || severity == ERROR || severity == WARNING
						|| severity == INFO || severity == CANCEL);
		this.severity = severity;
	}

	/**
	 * Returns a string representation of the status, suitable for debugging
	 * purposes only.
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Status ");
		if (severity == OK) {
			buf.append("OK");
		} else if (severity == ERROR) {
			buf.append("ERROR");
		} else if (severity == WARNING) {
			buf.append("WARNING");
		} else if (severity == INFO) {
			buf.append("INFO");
		} else if (severity == CANCEL) {
			buf.append("CANCEL");
		} else {
			buf.append("severity=");
			buf.append(severity);
		}
		buf.append(": ");
		buf.append(processName);
		buf.append(" code=");
		buf.append(code);
		buf.append(' ');
		buf.append(message);
		buf.append(' ');
		buf.append(exception);
		return buf.toString();
	}
}
