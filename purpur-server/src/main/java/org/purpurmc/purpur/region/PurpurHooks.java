package org.purpurmc.purpur.region;

import ca.spottedleaf.moonrise.paper.PaperHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Replaces {@code ca.spottedleaf.moonrise.paper.PaperHooks} in the
 * {@code META-INF/services/ca.spottedleaf.moonrise.common.PlatformHooks} service registration
 * so Purpur can intercept chunk watch events (used by the Servux structures protocol).
 */
public final class PurpurHooks extends PaperHooks {

    @Override
    public void onChunkWatch(ServerLevel world, LevelChunk chunk, ServerPlayer player) {
        super.onChunkWatch(world, chunk, player);
        org.purpurmc.purpur.protocol.servux.ServuxStructuresProtocol.onStartedWatchingChunk(player, chunk); // Purpur - servux
    }
}
