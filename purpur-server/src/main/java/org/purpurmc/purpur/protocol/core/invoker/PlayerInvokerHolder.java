package org.purpurmc.purpur.protocol.core.invoker;

import net.minecraft.server.level.ServerPlayer;
import org.purpurmc.purpur.protocol.core.PurpurProtocol;

import java.lang.reflect.Method;

public class PlayerInvokerHolder<T> extends AbstractInvokerHolder<T> {
    public PlayerInvokerHolder(PurpurProtocol owner, Method invoker, T handler) {
        super(owner, invoker, handler, null, ServerPlayer.class);
    }

    public void invoke(ServerPlayer player) {
        invoke0(false, player);
    }
}
