package org.purpurmc.purpur.protocol.jade.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.purpurmc.purpur.protocol.core.PurpurCustomPayload;
import org.purpurmc.purpur.protocol.jade.JadeProtocol;

public record ReceiveDataPayload(CompoundTag tag) implements PurpurCustomPayload {

    @ID
    private static final Identifier PACKET_RECEIVE_DATA = JadeProtocol.id("receive_data");

    @Codec
    private static final StreamCodec<FriendlyByteBuf, ReceiveDataPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG, ReceiveDataPayload::tag, ReceiveDataPayload::new
    );
}
