package org.purpurmc.purpur.protocol.jade.provider;

import net.minecraft.resources.Identifier;

public interface JadeProvider {

    Identifier getUid();

    default int getDefaultPriority() {
        return 0;
    }
}
