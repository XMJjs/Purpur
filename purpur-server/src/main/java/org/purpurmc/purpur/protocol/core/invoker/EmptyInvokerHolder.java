package org.purpurmc.purpur.protocol.core.invoker;

import org.purpurmc.purpur.protocol.core.PurpurProtocol;

import java.lang.reflect.Method;

public class EmptyInvokerHolder<T> extends AbstractInvokerHolder<T> {
    public EmptyInvokerHolder(PurpurProtocol owner, Method invoker, T handler) {
        super(owner, invoker, handler, null);
    }

    public void invoke() {
        invoke0(false);
    }
}
