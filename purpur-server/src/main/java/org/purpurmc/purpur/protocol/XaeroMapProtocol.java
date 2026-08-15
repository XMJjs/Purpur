package org.purpurmc.purpur.protocol;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.purpurmc.purpur.PurpurConfig;
import org.purpurmc.purpur.protocol.core.PurpurProtocol;
import org.purpurmc.purpur.protocol.core.ProtocolUtils;

@PurpurProtocol.Register(namespace = "xaerominimap_or_xaeroworldmap_i_dont_care")
public class XaeroMapProtocol implements PurpurProtocol {

    public static final String PROTOCOL_ID_MINI = "xaerominimap";
    public static final String PROTOCOL_ID_WORLD = "xaeroworldmap";

    private static final Identifier MINIMAP_KEY = idMini("main");
    private static final Identifier WORLDMAP_KEY = idWorld("main");

    @Contract("_ -> new")
    public static Identifier idMini(String path) {
        return Identifier.fromNamespaceAndPath(PROTOCOL_ID_MINI, path);
    }

    @Contract("_ -> new")
    public static Identifier idWorld(String path) {
        return Identifier.fromNamespaceAndPath(PROTOCOL_ID_WORLD, path);
    }

    public static void onSendWorldInfo(@NotNull ServerPlayer player) {
        if (PurpurConfig.protocol.xaeroMapProtocol) {
            ProtocolUtils.sendBytebufPacket(player, MINIMAP_KEY, buf -> {
                buf.writeByte(0);
                buf.writeInt(PurpurConfig.protocol.xaeroMapServerID);
            });
            ProtocolUtils.sendBytebufPacket(player, WORLDMAP_KEY, buf -> {
                buf.writeByte(0);
                buf.writeInt(PurpurConfig.protocol.xaeroMapServerID);
            });
        }
    }

    @Override
    public boolean isActive() {
        return PurpurConfig.protocol.xaeroMapProtocol;
    }
}
