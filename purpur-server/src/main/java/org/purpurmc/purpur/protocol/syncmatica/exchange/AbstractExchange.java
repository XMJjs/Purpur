package org.purpurmc.purpur.protocol.syncmatica.exchange;

import net.minecraft.network.FriendlyByteBuf;
import org.purpurmc.purpur.protocol.syncmatica.CommunicationManager;
import org.purpurmc.purpur.protocol.syncmatica.SyncmaticaProtocol;

import java.util.UUID;

public abstract class AbstractExchange implements Exchange {

    private final ExchangeTarget partner;
    private boolean success = false;
    private boolean finished = false;

    protected AbstractExchange(final ExchangeTarget partner) {
        this.partner = partner;
    }

    protected static boolean checkUUID(final FriendlyByteBuf sourceBuf, final UUID targetId) {
        final int r = sourceBuf.readerIndex();
        final UUID sourceId = sourceBuf.readUUID();
        sourceBuf.readerIndex(r);
        return sourceId.equals(targetId);
    }

    @Override
    public ExchangeTarget getPartner() {
        return partner;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isSuccessful() {
        return success;
    }

    @Override
    public void close(final boolean notifyPartner) {
        finished = true;
        success = false;
        onClose();
        if (notifyPartner) {
            sendCancelPacket();
        }
    }

    public CommunicationManager getManager() {
        return SyncmaticaProtocol.getCommunicationManager();
    }

    protected void sendCancelPacket() {
    }

    protected void onClose() {
    }

    protected void succeed() {
        finished = true;
        success = true;
        onClose();
    }
}
