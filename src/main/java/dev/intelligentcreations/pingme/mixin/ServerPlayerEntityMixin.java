package dev.intelligentcreations.pingme.mixin;

import dev.intelligentcreations.pingme.inject.ServerPlayerEntityHelper;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityHelper {
    @Shadow @Final public MinecraftServer server;

    @Shadow protected abstract boolean acceptsMessage(MessageType type);

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Inject(
            method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V"
            )
    )
    public void onSendMessage(Text message, MessageType type, UUID sender, CallbackInfo ci) {
        if (!this.server.isSingleplayer()) {
            String processString = message.getString();
            while (processString.contains("@")) {
                processString = processString.substring(processString.indexOf("@"));
                Text playerName = this.server.getPlayerManager().getPlayer(sender) != null ?
                        this.server.getPlayerManager().getPlayer(sender).getName()
                        : new LiteralText(sender.toString());
                if (processString.contains(" ")) {
                    String name = processString.substring(processString.indexOf("@") + 1, processString.indexOf(" "));
                    doPing(playerName, name);
                } else {
                    String name = processString.substring(processString.indexOf("@") + 1);
                    doPing(playerName, name);
                }
                processString = processString.substring(1);
            }
        }
    }

    private void doPing(Text playerName, String name) {
        Text pingMessage = Language.getInstance().hasTranslation("message.pingme.ping") ?
                new TranslatableText("message.pingme.ping", playerName)
                : new LiteralText(playerName.getString() + " just pinged you in the chat!");
        if (name.equals("everyone")) {
            this.server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.getName() != playerName) {
                    player.internalSendMessage(pingMessage, true);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            });
        } else {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(name);
            if (player != null) {
                player.internalSendMessage(pingMessage, true);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public void internalSendMessage(Text message, MessageType type, UUID sender) {
        if (this.acceptsMessage(type)) {
            this.networkHandler.sendPacket(new GameMessageS2CPacket(message, type, sender), (future) -> {
                if (!future.isSuccess() && (type == MessageType.GAME_INFO || type == MessageType.SYSTEM) && this.acceptsMessage(MessageType.SYSTEM)) {
                    String string = message.asTruncatedString(256);
                    Text text2 = (new LiteralText(string)).formatted(Formatting.YELLOW);
                    this.networkHandler.sendPacket(new GameMessageS2CPacket((new TranslatableText("multiplayer.message_not_delivered", new Object[]{text2})).formatted(Formatting.RED), MessageType.SYSTEM, sender));
                }
            });
        }
    }

    @Override
    public void internalSendMessage(Text message, boolean actionBar) {
        this.internalSendMessage(message, actionBar ? MessageType.GAME_INFO : MessageType.CHAT, Util.NIL_UUID);
    }
}
