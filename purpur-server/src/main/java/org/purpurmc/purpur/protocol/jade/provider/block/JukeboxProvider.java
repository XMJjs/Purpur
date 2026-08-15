package org.purpurmc.purpur.protocol.jade.provider.block;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.purpurmc.purpur.protocol.jade.JadeProtocol;
import org.purpurmc.purpur.protocol.jade.accessor.BlockAccessor;
import org.purpurmc.purpur.protocol.jade.provider.StreamServerDataProvider;

public enum JukeboxProvider implements StreamServerDataProvider<BlockAccessor, ItemStack> {
    INSTANCE;

    private static final Identifier MC_JUKEBOX = JadeProtocol.mc_id("jukebox");

    @Override
    public @NotNull ItemStack streamData(BlockAccessor accessor) {
        return ((JukeboxBlockEntity) accessor.getBlockEntity()).getTheItem();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemStack> streamCodec() {
        return ItemStack.OPTIONAL_STREAM_CODEC;
    }

    @Override
    public Identifier getUid() {
        return MC_JUKEBOX;
    }
}
