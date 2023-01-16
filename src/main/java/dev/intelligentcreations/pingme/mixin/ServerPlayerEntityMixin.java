package dev.intelligentcreations.pingme.mixin;

import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.ServerTranslations;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow @Final public MinecraftServer server;

    @Inject(
            method = "sendChatMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/message/SentMessage;send(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/network/message/MessageType$Parameters;)V"
            )
    )
    public void onSendMessage(SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params, CallbackInfo ci) {
        if (!this.server.isSingleplayer()) {
            String processString = message.getContent().getString();
            while (processString.contains("@")) {
                processString = processString.substring(processString.indexOf("@"));
                ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(params.name().getString());
                if (player != null) {
                    if (processString.contains(" ")) {
                        String name = processString.substring(processString.indexOf("@") + 1, processString.indexOf(" "));
                        doPing(player, name);
                    } else {
                        String name = processString.substring(processString.indexOf("@") + 1);
                        doPing(player, name);
                    }
                }
                processString = processString.substring(1);
            }
        }
    }

    private void doPing(ServerPlayerEntity playerEntity, String name) {
        Text pingMessage = Text.literal(playerEntity.getName().getString() + ServerTranslations.INSTANCE.getLanguage((LocalizationTarget) playerEntity).remote().get("message.pingme.ping"));
        if (name.equals("everyone")) {
            this.server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.getName() != playerEntity.getName()) {
                    player.sendMessage(pingMessage, true);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            });
        } else {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(name);
            if (player != null) {
                player.sendMessage(pingMessage, true);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }
}
