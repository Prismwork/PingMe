package dev.intelligentcreations.pingme.inject;

import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.UUID;

public interface ServerPlayerEntityHelper {
    default void internalSendMessage(Text message, MessageType type, UUID sender) {
    }

    default void internalSendMessage(Text message, boolean actionBar) {
        this.internalSendMessage(message, actionBar ? MessageType.GAME_INFO : MessageType.CHAT, Util.NIL_UUID);
    }
}
