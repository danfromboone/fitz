package dfb.fitz.proxy;

import java.lang.reflect.Method;

/**
 * Advice executed after a targeted method invocation successfully returns.
 */
@FunctionalInterface
public interface AfterAdvice {

    /**
     * Callback executed after the target method successfully returns.
     *
     * @param target the target instance
     * @param method the method that was invoked
     * @param args   the method arguments (never {@code null}; empty array if no arguments)
     * @param result the value returned by the target method (or {@code null} if {@code void})
     * @throws Throwable if any error occurs
     */
    void after(Object target, Method method, Object[] args, Object result) throws Throwable;
}
