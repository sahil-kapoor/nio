package my.spring.server.utils;

import my.spring.server.exceptions.AssertionFailedException;

public final class Assert {
    /* This class is not intended to be instantiated. */
    private Assert() {
	// not allowed
    }

    /**
     * Asserts that an argument is legal. If the given boolean is not
     * <code>true</code>, an <code>IllegalArgumentException</code> is thrown.
     *
     * @param expression
     *            the outcome of the check
     * @return <code>true</code> if the check passes (does not return if the
     *         check fails)
     * @exception IllegalArgumentException
     *                if the legality test failed
     */
    public static boolean isLegal(boolean expression) {
	return isLegal(expression, "");
    }

    /**
     * Asserts that an argument is legal. If the given boolean is not
     * <code>true</code>, an <code>IllegalArgumentException</code> is thrown.
     * The given message is included in that exception, to aid debugging.
     *
     * @param expression
     *            the outcome of the check
     * @param message
     *            the message to include in the exception
     * @return <code>true</code> if the check passes (does not return if the
     *         check fails)
     * @exception IllegalArgumentException
     *                if the legality test failed
     */
    public static boolean isLegal(boolean expression, String message) {
	if (!expression)
	    throw new IllegalArgumentException(message);
	return expression;
    }

    /**
     * Asserts that the given object is not <code>null</code>. If this is not
     * the case, some kind of unchecked exception is thrown.
     * 
     * @param object
     *            the value to test
     */
    public static void isNotNull(Object object) {
	isNotNull(object, "");
    }

    /**
     * Asserts that the given object is not <code>null</code>. If this is not
     * the case, some kind of unchecked exception is thrown. The given message
     * is included in that exception, to aid debugging.
     *
     * @param object
     *            the value to test
     * @param message
     *            the message to include in the exception
     */
    public static void isNotNull(Object object, String message) {
	if (object == null)
	    throw new AssertionFailedException("null argument:" + message);
    }

    /**
     * Asserts that the given boolean is <code>true</code>. If this is not the
     * case, some kind of unchecked exception is thrown.
     *
     * @param expression
     *            the outcome of the check
     * @return <code>true</code> if the check passes (does not return if the
     *         check fails)
     */
    public static boolean isTrue(boolean expression) {
	return isTrue(expression, "");
    }

    /**
     * Asserts that the given boolean is <code>true</code>. If this is not the
     * case, some kind of unchecked exception is thrown. The given message is
     * included in that exception, to aid debugging.
     *
     * @param expression
     *            the outcome of the check
     * @param message
     *            the message to include in the exception
     * @return <code>true</code> if the check passes (does not return if the
     *         check fails)
     */
    public static boolean isTrue(boolean expression, String message) {
	if (!expression)
	    throw new AssertionFailedException("assertion failed: " + message);
	return expression;
    }
}
