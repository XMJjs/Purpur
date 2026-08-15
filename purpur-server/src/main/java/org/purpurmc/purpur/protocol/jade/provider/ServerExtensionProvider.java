package org.purpurmc.purpur.protocol.jade.provider;

import org.purpurmc.purpur.protocol.jade.accessor.Accessor;
import org.purpurmc.purpur.protocol.jade.util.ViewGroup;

import java.util.List;

public interface ServerExtensionProvider<T> extends JadeProvider {
    List<ViewGroup<T>> getGroups(Accessor<?> request);
}