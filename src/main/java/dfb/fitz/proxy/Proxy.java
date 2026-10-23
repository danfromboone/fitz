package dfb.fitz.proxy;

import dfb.fitz.LazyValue;
import dfb.fitz.Validate;

import java.lang.reflect.InvocationHandler;
import java.util.function.Supplier;

/**
 * Utility class providing static factory methods for creating dynamic proxies.
 */
public final class Proxy {

    private Proxy() {
    }

    /**
     * Creates a typed dynamic proxy implementing {@code interfaceType} that delegates
     * invocations to the specified {@link InvocationHandler}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy (must not be {@code null} and must be an interface)
     * @param handler       the invocation handler to dispatch method calls to (must not be {@code null})
     * @return a proxy implementing {@code T}
     * @throws NullPointerException     if {@code interfaceType} or {@code handler} is {@code null}
     * @throws IllegalArgumentException if {@code interfaceType} is not an interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceType, InvocationHandler handler) {
        Validate.nonNull(interfaceType, "interfaceType");
        Validate.nonNull(handler, "handler");

        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
        }

        ClassLoader classLoader = resolveClassLoader(interfaceType);
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{interfaceType},
                handler
        );
    }

    /**
     * Creates a typed dynamic proxy implementing {@code interfaceType} using the specified
     * {@link ClassLoader} and {@link InvocationHandler}.
     *
     * @param <T>           the interface type
     * @param classLoader   the class loader to define the proxy class (if {@code null}, resolved from {@code interfaceType})
     * @param interfaceType the interface to proxy (must not be {@code null} and must be an interface)
     * @param handler       the invocation handler (must not be {@code null})
     * @return a proxy implementing {@code T}
     * @throws NullPointerException     if {@code interfaceType} or {@code handler} is {@code null}
     * @throws IllegalArgumentException if {@code interfaceType} is not an interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(ClassLoader classLoader, Class<T> interfaceType, InvocationHandler handler) {
        Validate.nonNull(interfaceType, "interfaceType");
        Validate.nonNull(handler, "handler");

        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
        }

        ClassLoader cl = classLoader != null ? classLoader : resolveClassLoader(interfaceType);
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                cl,
                new Class<?>[]{interfaceType},
                handler
        );
    }

    /**
     * Creates a dynamic proxy implementing multiple interfaces.
     *
     * @param classLoader the class loader (if {@code null}, resolved from the first interface)
     * @param interfaces  the interfaces to implement (must not be {@code null} or empty)
     * @param handler     the invocation handler (must not be {@code null})
     * @return the proxy instance
     * @throws NullPointerException     if {@code interfaces} or {@code handler} is {@code null}
     * @throws IllegalArgumentException if {@code interfaces} is empty or contains non-interfaces
     */
    public static Object create(ClassLoader classLoader, Class<?>[] interfaces, InvocationHandler handler) {
        Validate.nonNull(interfaces, "interfaces");
        Validate.nonNull(handler, "handler");

        if (interfaces.length == 0) {
            throw new IllegalArgumentException("interfaces array must not be empty");
        }
        for (Class<?> intf : interfaces) {
            Validate.nonNull(intf, "interface");
            if (!intf.isInterface()) {
                throw new IllegalArgumentException(intf.getName() + " is not an interface");
            }
        }

        ClassLoader cl = classLoader != null ? classLoader : resolveClassLoader(interfaces[0]);
        return java.lang.reflect.Proxy.newProxyInstance(
                cl,
                interfaces.clone(),
                handler
        );
    }

    /**
     * Creates a dynamic proxy implementing {@code interfaceType} that delegates
     * all method calls to the instance returned by {@code supplier.get()}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy (must not be {@code null} and must be an interface)
     * @param supplier      the supplier providing the target instance (must not be {@code null})
     * @return a proxy implementing {@code T}
     */
    public static <T> T supplier(Class<T> interfaceType, Supplier<? extends T> supplier) {
        return SupplierProxy.create(interfaceType, supplier);
    }

    /**
     * Creates a dynamic proxy implementing {@code interfaceType} that lazily initializes
     * the target instance via the given supplier on first invocation, and caches it.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy (must not be {@code null} and must be an interface)
     * @param supplier      the supplier providing the target instance on first call (must not be {@code null})
     * @return a lazy proxy implementing {@code T}
     */
    public static <T> T lazy(Class<T> interfaceType, Supplier<? extends T> supplier) {
        Validate.nonNull(supplier, "supplier");
        return SupplierProxy.create(interfaceType, LazyValue.of(supplier));
    }

    /**
     * Creates a dynamic proxy implementing {@code interfaceType} that delegates to the given target instance.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy (must not be {@code null} and must be an interface)
     * @param target        the target instance to delegate to (must not be {@code null})
     * @return a proxy implementing {@code T}
     */
    public static <T> T delegate(Class<T> interfaceType, T target) {
        Validate.nonNull(target, "target");
        return SupplierProxy.create(interfaceType, () -> target);
    }

    /**
     * Alias for {@link #create(Class, InvocationHandler)}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy
     * @param handler       the invocation handler
     * @return a proxy implementing {@code T}
     */
    public static <T> T of(Class<T> interfaceType, InvocationHandler handler) {
        return create(interfaceType, handler);
    }

    /**
     * Alias for {@link #supplier(Class, Supplier)}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy
     * @param supplier      the target supplier
     * @return a proxy implementing {@code T}
     */
    public static <T> T of(Class<T> interfaceType, Supplier<? extends T> supplier) {
        return supplier(interfaceType, supplier);
    }

    /**
     * Creates a builder for configuring an intercepting proxy with before, after, or around advice.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface to proxy
     * @param target        the target instance
     * @return a new builder
     */
    public static <T> InterceptingProxy.Builder<T> intercept(Class<T> interfaceType, T target) {
        return InterceptingProxy.builder(interfaceType, target);
    }

    /**
     * Creates a proxy executing the given {@link BeforeAdvice} before every method invocation.
     */
    public static <T> T before(Class<T> interfaceType, T target, BeforeAdvice advice) {
        return InterceptingProxy.before(interfaceType, target, advice);
    }

    /**
     * Creates a proxy executing the given {@link BeforeAdvice} before the named method.
     */
    public static <T> T before(Class<T> interfaceType, T target, String methodName, BeforeAdvice advice) {
        return InterceptingProxy.before(interfaceType, target, methodName, advice);
    }

    /**
     * Creates a proxy executing the given {@link AfterAdvice} after every method returns successfully.
     */
    public static <T> T after(Class<T> interfaceType, T target, AfterAdvice advice) {
        return InterceptingProxy.after(interfaceType, target, advice);
    }

    /**
     * Creates a proxy executing the given {@link AfterAdvice} after the named method returns successfully.
     */
    public static <T> T after(Class<T> interfaceType, T target, String methodName, AfterAdvice advice) {
        return InterceptingProxy.after(interfaceType, target, methodName, advice);
    }

    /**
     * Creates a proxy executing the given {@link AroundAdvice} around every method invocation.
     */
    public static <T> T around(Class<T> interfaceType, T target, AroundAdvice advice) {
        return InterceptingProxy.around(interfaceType, target, advice);
    }

    /**
     * Creates a proxy executing the given {@link AroundAdvice} around the named method.
     */
    public static <T> T around(Class<T> interfaceType, T target, String methodName, AroundAdvice advice) {
        return InterceptingProxy.around(interfaceType, target, methodName, advice);
    }

    /**
     * Returns true if the specified object is a dynamic proxy instance.
     *
     * @param object the object to check
     * @return {@code true} if {@code object} is a proxy instance; {@code false} otherwise
     */
    public static boolean isProxy(Object object) {
        return object != null && java.lang.reflect.Proxy.isProxyClass(object.getClass());
    }

    /**
     * Returns true if the specified class is a dynamic proxy class.
     *
     * @param clazz the class to check
     * @return {@code true} if {@code clazz} is a proxy class; {@code false} otherwise
     */
    public static boolean isProxyClass(Class<?> clazz) {
        Validate.nonNull(clazz, "clazz");
        return java.lang.reflect.Proxy.isProxyClass(clazz);
    }

    /**
     * Returns the {@link InvocationHandler} for the given proxy instance.
     *
     * @param proxy the proxy instance
     * @return the invocation handler
     */
    public static InvocationHandler getInvocationHandler(Object proxy) {
        Validate.nonNull(proxy, "proxy");
        return java.lang.reflect.Proxy.getInvocationHandler(proxy);
    }

    static ClassLoader resolveClassLoader(Class<?> type) {
        ClassLoader classLoader = type != null ? type.getClassLoader() : null;
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }
}
