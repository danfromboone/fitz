package dfb.fitz.proxy;

import dfb.fitz.Validate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Factory and builder for creating dynamic proxies that execute {@link BeforeAdvice},
 * {@link AfterAdvice}, and {@link AroundAdvice} around targeted method invocations.
 */
public final class InterceptingProxy {

    private InterceptingProxy() {
    }

    /**
     * Creates a new fluent builder for configuring an intercepting proxy.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface class to implement
     * @param target        the underlying target instance to delegate to
     * @return a new builder
     */
    public static <T> Builder<T> builder(Class<T> interfaceType, T target) {
        return new Builder<>(interfaceType, target);
    }

    /**
     * Creates a proxy executing the given {@link BeforeAdvice} before every method invocation.
     */
    public static <T> T before(Class<T> interfaceType, T target, BeforeAdvice advice) {
        return builder(interfaceType, target).before(advice).build();
    }

    /**
     * Creates a proxy executing the given {@link BeforeAdvice} before invocations of the named method.
     */
    public static <T> T before(Class<T> interfaceType, T target, String methodName, BeforeAdvice advice) {
        return builder(interfaceType, target).before(methodName, advice).build();
    }

    /**
     * Creates a proxy executing the given {@link AfterAdvice} after every method returns successfully.
     */
    public static <T> T after(Class<T> interfaceType, T target, AfterAdvice advice) {
        return builder(interfaceType, target).after(advice).build();
    }

    /**
     * Creates a proxy executing the given {@link AfterAdvice} after the named method returns successfully.
     */
    public static <T> T after(Class<T> interfaceType, T target, String methodName, AfterAdvice advice) {
        return builder(interfaceType, target).after(methodName, advice).build();
    }

    /**
     * Creates a proxy executing the given {@link AroundAdvice} around every method invocation.
     */
    public static <T> T around(Class<T> interfaceType, T target, AroundAdvice advice) {
        return builder(interfaceType, target).around(advice).build();
    }

    /**
     * Creates a proxy executing the given {@link AroundAdvice} around invocations of the named method.
     */
    public static <T> T around(Class<T> interfaceType, T target, String methodName, AroundAdvice advice) {
        return builder(interfaceType, target).around(methodName, advice).build();
    }

    /**
     * Fluent builder for configuring method interception rules.
     *
     * @param <T> the interface type
     */
    public static final class Builder<T> {
        private final Class<T> interfaceType;
        private final T target;
        private final List<Advisor> advisors = new ArrayList<>();

        public Builder(Class<T> interfaceType, T target) {
            this.interfaceType = Validate.nonNull(interfaceType, "interfaceType");
            this.target = Validate.nonNull(target, "target");

            if (!interfaceType.isInterface()) {
                throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
            }
        }

        // Before advice

        public Builder<T> before(BeforeAdvice advice) {
            return before(m -> true, advice);
        }

        public Builder<T> before(String methodName, BeforeAdvice advice) {
            Validate.nonNull(methodName, "methodName");
            return before(m -> m.getName().equals(methodName), advice);
        }

        public Builder<T> before(Method method, BeforeAdvice advice) {
            Validate.nonNull(method, "method");
            return before(m -> m.equals(method), advice);
        }

        public Builder<T> before(Predicate<Method> predicate, BeforeAdvice advice) {
            Validate.nonNull(predicate, "predicate");
            Validate.nonNull(advice, "advice");
            advisors.add(new Advisor(predicate, advice, null, null));
            return this;
        }

        // After advice

        public Builder<T> after(AfterAdvice advice) {
            return after(m -> true, advice);
        }

        public Builder<T> after(String methodName, AfterAdvice advice) {
            Validate.nonNull(methodName, "methodName");
            return after(m -> m.getName().equals(methodName), advice);
        }

        public Builder<T> after(Method method, AfterAdvice advice) {
            Validate.nonNull(method, "method");
            return after(m -> m.equals(method), advice);
        }

        public Builder<T> after(Predicate<Method> predicate, AfterAdvice advice) {
            Validate.nonNull(predicate, "predicate");
            Validate.nonNull(advice, "advice");
            advisors.add(new Advisor(predicate, null, advice, null));
            return this;
        }

        // Around advice

        public Builder<T> around(AroundAdvice advice) {
            return around(m -> true, advice);
        }

        public Builder<T> around(String methodName, AroundAdvice advice) {
            Validate.nonNull(methodName, "methodName");
            return around(m -> m.getName().equals(methodName), advice);
        }

        public Builder<T> around(Method method, AroundAdvice advice) {
            Validate.nonNull(method, "method");
            return around(m -> m.equals(method), advice);
        }

        public Builder<T> around(Predicate<Method> predicate, AroundAdvice advice) {
            Validate.nonNull(predicate, "predicate");
            Validate.nonNull(advice, "advice");
            advisors.add(new Advisor(predicate, null, null, advice));
            return this;
        }

        /**
         * Builds the intercepting proxy instance implementing {@code T}.
         *
         * @return the proxy instance
         */
        public T build() {
            return Proxy.create(interfaceType, new InterceptingInvocationHandler(target, advisors));
        }
    }

    private static final class Advisor {
        final Predicate<Method> matcher;
        final BeforeAdvice beforeAdvice;
        final AfterAdvice afterAdvice;
        final AroundAdvice aroundAdvice;

        Advisor(Predicate<Method> matcher, BeforeAdvice beforeAdvice, AfterAdvice afterAdvice, AroundAdvice aroundAdvice) {
            this.matcher = matcher;
            this.beforeAdvice = beforeAdvice;
            this.afterAdvice = afterAdvice;
            this.aroundAdvice = aroundAdvice;
        }

        boolean matches(Method method) {
            return matcher.test(method);
        }
    }

    private static final class InterceptingInvocationHandler implements InvocationHandler {
        private final Object target;
        private final List<Advisor> advisors;

        InterceptingInvocationHandler(Object target, List<Advisor> advisors) {
            this.target = target;
            this.advisors = List.copyOf(advisors);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] rawArgs) throws Throwable {
            Object[] args = rawArgs != null ? rawArgs : new Object[0];

            if (method.getDeclaringClass() == Object.class) {
                String name = method.getName();
                if ("equals".equals(name) && method.getParameterCount() == 1) {
                    if (args[0] == proxy) {
                        return true;
                    }
                }
            }

            List<BeforeAdvice> matchingBefore = new ArrayList<>();
            List<AfterAdvice> matchingAfter = new ArrayList<>();
            List<AroundAdvice> matchingAround = new ArrayList<>();

            for (Advisor advisor : advisors) {
                if (advisor.matches(method)) {
                    if (advisor.beforeAdvice != null) matchingBefore.add(advisor.beforeAdvice);
                    if (advisor.afterAdvice != null) matchingAfter.add(advisor.afterAdvice);
                    if (advisor.aroundAdvice != null) matchingAround.add(advisor.aroundAdvice);
                }
            }

            // Execute before advices
            for (BeforeAdvice before : matchingBefore) {
                before.before(target, method, args);
            }

            // Execute around chain or proceed to target method
            ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(
                    proxy, target, method, args, matchingAround, matchingAfter
            );
            return invocation.proceed();
        }
    }

    private static final class ReflectiveMethodInvocation implements MethodInvocation {
        private final Object proxy;
        private final Object target;
        private final Method method;
        private Object[] arguments;
        private final List<AroundAdvice> aroundAdvices;
        private final List<AfterAdvice> afterAdvices;
        private int currentAroundIndex = 0;

        ReflectiveMethodInvocation(Object proxy, Object target, Method method, Object[] arguments,
                                   List<AroundAdvice> aroundAdvices, List<AfterAdvice> afterAdvices) {
            this.proxy = proxy;
            this.target = target;
            this.method = method;
            this.arguments = arguments != null ? arguments : new Object[0];
            this.aroundAdvices = aroundAdvices;
            this.afterAdvices = afterAdvices;
        }

        @Override
        public Object getProxy() {
            return proxy;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public void setArguments(Object[] arguments) {
            this.arguments = arguments != null ? arguments : new Object[0];
        }

        @Override
        public Object proceed() throws Throwable {
            if (currentAroundIndex < aroundAdvices.size()) {
                AroundAdvice current = aroundAdvices.get(currentAroundIndex++);
                return current.around(this);
            }

            Object result;
            try {
                result = method.invoke(target, arguments);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

            for (AfterAdvice after : afterAdvices) {
                after.after(target, method, arguments, result);
            }

            return result;
        }
    }
}
