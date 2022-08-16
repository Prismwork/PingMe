package dev.intelligentcreations.pingme.mixin;

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow @Final public MinecraftServer server;

    @Inject(
            method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V"
            )
    )
    public void onSendMessage(Text message, MessageType type, UUID sender, CallbackInfo ci) {
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
            processString = processString.substring(processString.indexOf("@", processString.indexOf("@") + 1));
        }
    }

    private void doPing(Text playerName, String name) {
        if (name.equals("everyone")) {
            this.server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.getName() != playerName) {
                    player.sendMessage(new TranslatableText("message.pingme.ping", playerName), true);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            });
        } else {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(name);
            if (player != null) {
                player.sendMessage(new TranslatableText("message.pingme.ping", playerName), true);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }
}
