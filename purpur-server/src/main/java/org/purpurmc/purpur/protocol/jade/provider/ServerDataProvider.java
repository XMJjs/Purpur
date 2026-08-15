package org.purpurmc.purpur.protocol.jade.provider;

import net.minecraft.nbt.CompoundTag;
import org.purpurmc.purpur.protocol.jade.accessor.Accessor;

public interface ServerDataProvider<T extends Accessor<?>> extends JadeProvider {
    void appendServerData(CompoundTag data, T accessor);
}
