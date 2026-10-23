package dfb.fitz.proxy;

/**
 * Advice executed around a targeted method invocation, controlling execution flow and return value.
 */
@FunctionalInterface
public interface AroundAdvice {

    /**
     * Intercepts a method invocation.
     *
     * @param invocation the method invocation handle
     * @return the result of the method invocation
     * @throws Throwable if any error occurs
     */
    Object around(MethodInvocation invocation) throws Throwable;
}
