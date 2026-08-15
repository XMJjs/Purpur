package org.purpurmc.purpur.protocol.jade.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.purpurmc.purpur.protocol.core.PurpurCustomPayload;
import org.purpurmc.purpur.protocol.jade.JadeProtocol;

public record ClientHandshakePayload(String protocolVersion) implements PurpurCustomPayload {

    @ID
    private static final Identifier PACKET_CLIENT_HANDSHAKE = JadeProtocol.id("client_handshake");

    @Codec
    private static final StreamCodec<RegistryFriendlyByteBuf, ClientHandshakePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ClientHandshakePayload::protocolVersion, ClientHandshakePayload::new
    );
}