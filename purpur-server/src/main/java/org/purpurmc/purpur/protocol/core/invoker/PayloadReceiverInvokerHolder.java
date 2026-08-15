package org.purpurmc.purpur.protocol.core.invoker;

import org.purpurmc.purpur.protocol.core.IdentifierSelector;
import org.purpurmc.purpur.protocol.core.PurpurCustomPayload;
import org.purpurmc.purpur.protocol.core.PurpurProtocol;
import org.purpurmc.purpur.protocol.core.ProtocolHandler;

import java.lang.reflect.Method;

public class PayloadReceiverInvokerHolder extends AbstractInvokerHolder<ProtocolHandler.PayloadReceiver> {
    public PayloadReceiverInvokerHolder(PurpurProtocol owner, Method invoker, ProtocolHandler.PayloadReceiver handler) {
        super(owner, invoker, handler, null, handler.stage().identifier(), handler.payload());
    }

    public void invoke(IdentifierSelector selector, PurpurCustomPayload payload) {
        invoke0(false, selector.select(handler.stage()), payload);
    }
}
