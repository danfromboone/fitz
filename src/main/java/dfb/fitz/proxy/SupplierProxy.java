package dfb.fitz.proxy;

import dfb.fitz.Validate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Factory for creating JDK dynamic proxies that delegate all method calls to a {@link Supplier}.
 */
public final class SupplierProxy {

    private SupplierProxy() {
    }

    /**
     * Creates a dynamic proxy implementing {@code interfaceType} that delegates
     * all method calls to the instance returned by {@code supplier.get()}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface class to implement (must not be {@code null} and must be an interface)
     * @param supplier      the supplier providing the delegate target (must not be {@code null})
     * @return a proxy implementing {@code T}
     * @throws NullPointerException     if {@code interfaceType} or {@code supplier} is {@code null}
     * @throws IllegalArgumentException if {@code interfaceType} is not an interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceType, Supplier<? extends T> supplier) {
        Validate.nonNull(interfaceType, "interfaceType");
        Validate.nonNull(supplier, "supplier");

        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
        }

        ClassLoader classLoader = interfaceType.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        return (T) Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{interfaceType},
                new SupplierInvocationHandler<>(supplier)
        );
    }

    /**
     * Creates a dynamic proxy implementing {@code interfaceType} that delegates
     * all method calls to the instance returned by {@code supplier.get()}.
     *
     * @param <T>           the interface type
     * @param interfaceType the interface class to implement
     * @param supplier      the supplier providing the delegate target
     * @return a proxy implementing {@code T}
     */
    public static <T> T of(Class<T> interfaceType, Supplier<? extends T> supplier) {
        return create(interfaceType, supplier);
    }

    private static final class SupplierInvocationHandler<T> implements InvocationHandler {
        private final Supplier<? extends T> supplier;

        SupplierInvocationHandler(Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                String name = method.getName();
                if ("equals".equals(name) && method.getParameterCount() == 1) {
                    if (args[0] == proxy) {
                        return true;
                    }
                }
            }

            T target = supplier.get();
            if (target == null) {
                throw new NullPointerException(
                        "Target supplier returned null for method invocation: " + method.getName()
                );
            }

            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
