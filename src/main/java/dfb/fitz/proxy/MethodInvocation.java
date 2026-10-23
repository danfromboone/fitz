package dfb.fitz.proxy;

import java.lang.reflect.Method;

/**
 * Encapsulates a method invocation intercepted by an {@link AroundAdvice}.
 */
public interface MethodInvocation {

    /**
     * Returns the proxy instance on which the method was called.
     *
     * @return the proxy instance
     */
    Object getProxy();

    /**
     * Returns the underlying target object being proxied.
     *
     * @return the target object
     */
    Object getTarget();

    /**
     * Returns the {@link Method} being invoked.
     *
     * @return the method
     */
    Method getMethod();

    /**
     * Returns the arguments passed to the method invocation.
     *
     * @return the argument array (never {@code null})
     */
    Object[] getArguments();

    /**
     * Sets or modifies the arguments to be used when proceeding to the next advice or target method.
     *
     * @param arguments the new arguments (if {@code null}, normalized to empty array)
     */
    void setArguments(Object[] arguments);

    /**
     * Proceeds to the next interceptor in the chain or to the target method.
     *
     * @return the return value of the invocation
     * @throws Throwable if the target method or downstream advice throws an exception
     */
    Object proceed() throws Throwable;

    /**
     * Updates the arguments and proceeds to the next interceptor or target method.
     *
     * @param arguments the new arguments
     * @return the return value of the invocation
     * @throws Throwable if an exception is thrown
     */
    default Object proceed(Object[] arguments) throws Throwable {
        setArguments(arguments);
        return proceed();
    }
}
