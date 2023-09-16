package dev.intelligentcreations.pingme.mixin;

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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.server.translations.api.LocalizationTarget;
import xyz.nucleoid.server.translations.impl.ServerTranslations;

import java.util.Objects;

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
        // We'd better not judge if the server is single-player or not,
        // otherwise LAN world servers won't be able to enjoy the mod.

        // if (!this.server.isSingleplayer()) {
            String processedString = message.getContent().getString();

            while (processedString.contains("@")) {
                processedString = processedString.substring(processedString.indexOf("@"));
                ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(params.name().getString());

                if (player != null) {
					String name;

					if (processedString.contains(" ")) name = processedString.substring(processedString.indexOf("@") + 1, processedString.indexOf(" "));
					else name = processedString.substring(processedString.indexOf("@") + 1);

					doPing(player, name);
				}
                processedString = processedString.substring(1);
            }


        // }
    }

    @Unique
    private void doPing(ServerPlayerEntity playerEntity, String name) {
        Text pingMessage = Text.literal(
                playerEntity.getName().getString()
                        + ServerTranslations.INSTANCE.getLanguage((LocalizationTarget) playerEntity)
                                  .serverTranslations()
                                  .get("message.pingme.ping")
        );

        if (name.equals("everyone")) {
            this.server.getPlayerManager().getPlayerList().forEach(player -> {
                if (!Objects.equals(player.getName().getString(), playerEntity.getName().getString())) {
                    System.out.println(player.getName());
                    player.sendMessage(pingMessage, true);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            });
        }

        else {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(name);
            if (player != null) {
                player.sendMessage(pingMessage, true);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }
}
