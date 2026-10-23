package dfb.fitz.proxy;

import java.lang.reflect.Method;

/**
 * Advice executed before a targeted method invocation.
 */
@FunctionalInterface
public interface BeforeAdvice {

    /**
     * Callback executed before the target method is invoked.
     *
     * @param target the target instance
     * @param method the method being invoked
     * @param args   the method arguments (never {@code null}; empty array if no arguments)
     * @throws Throwable if any error occurs
     */
    void before(Object target, Method method, Object[] args) throws Throwable;
}
