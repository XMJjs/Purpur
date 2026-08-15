package org.purpurmc.purpur.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * NBT save/load helpers for the protocol layer.
 * Ported from Leaves (org.leavesmc.leaves.util.TagFactory).
 * NOTE: verify the 1.21.11-era TagValueInput/TagValueOutput API against MC 26.1 when building.
 */
public class TagFactory {

    private static final RegistryAccess.Frozen registryAccess = MinecraftServer.getServer().registryAccess();

    public static TagValueOutput output() {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess);
    }

    public static TagValueOutput output(CompoundTag tag) {
        return TagValueOutput.createWrappingWithContext(ProblemReporter.DISCARDING, registryAccess, tag);
    }

    public static TagValueInput input() {
        return input(new CompoundTag());
    }

    public static TagValueInput input(CompoundTag tag) {
        return (TagValueInput) TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, tag);
    }
}
