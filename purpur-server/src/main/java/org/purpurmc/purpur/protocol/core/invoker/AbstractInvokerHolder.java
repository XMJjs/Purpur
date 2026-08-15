package org.purpurmc.purpur.protocol.core.invoker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.purpurmc.purpur.PurpurConfig;
import org.purpurmc.purpur.PurpurLogger;
import org.purpurmc.purpur.protocol.core.PurpurProtocol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractInvokerHolder<T> {

    protected final PurpurProtocol owner;
    protected final Method invoker;
    protected final T handler;
    protected final Class<?> returnType;
    protected final Class<?>[] parameterTypes;

    protected AbstractInvokerHolder(PurpurProtocol owner, Method invoker, T handler, @Nullable Class<?> returnType, @NotNull Class<?>... parameterTypes) {
        this.owner = owner;
        this.invoker = invoker;
        this.handler = handler;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;

        validateMethodSignature();
    }

    protected void validateMethodSignature() {
        if (returnType != null && !returnType.isAssignableFrom(invoker.getReturnType())) {
            throw new IllegalArgumentException("Return type mismatch in " + owner.getClass().getName() + "#" + invoker.getName() +
                ": expected " + returnType.getName() + " but found " + invoker.getReturnType().getName());
        }

        Class<?>[] methodParamTypes = invoker.getParameterTypes();
        if (methodParamTypes.length != parameterTypes.length) {
            throw new IllegalArgumentException("Parameter count mismatch in " + owner.getClass().getName() + "#" + invoker.getName() +
                ": expected " + parameterTypes.length + " but found " + methodParamTypes.length);
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(methodParamTypes[i])) {
                throw new IllegalArgumentException("Parameter type mismatch in " + owner.getClass().getName() + "#" + invoker.getName() +
                    " at index " + i + ": expected " + parameterTypes[i].getName() + " but found " + methodParamTypes[i].getName());
            }
        }
    }

    public PurpurProtocol owner() {
        return owner;
    }

    public T handler() {
        return handler;
    }

    protected Object invoke0(boolean force, Object... args) {
        if (!force && !owner.isActive()) {
            return null;
        }
        try {
            return invoker.invoke(owner, args);
        } catch (Exception e) {
            throwOrLog(e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwOrLog(Exception e) throws T {
        Throwable t = e instanceof InvocationTargetException ex ? ex.getTargetException() : e;
        if (PurpurConfig.protocol.strictMode) {
            throw (T) t;
        }
        PurpurLogger.LOGGER.error("Exception on invoking protocol ", t);
    }
}
