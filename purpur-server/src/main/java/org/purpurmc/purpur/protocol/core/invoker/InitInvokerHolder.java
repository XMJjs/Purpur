package org.purpurmc.purpur.protocol.core.invoker;

import org.purpurmc.purpur.protocol.core.PurpurProtocol;
import org.purpurmc.purpur.protocol.core.ProtocolHandler;

import java.lang.reflect.Method;

public class InitInvokerHolder extends AbstractInvokerHolder<ProtocolHandler.Init> {
    public InitInvokerHolder(PurpurProtocol owner, Method invoker, ProtocolHandler.Init handler) {
        super(owner, invoker, handler, null);
    }

    public void invoke() {
        invoke0(true);
    }
}
