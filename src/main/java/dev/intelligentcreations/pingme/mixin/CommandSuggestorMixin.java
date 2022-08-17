package dev.intelligentcreations.pingme.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.intelligentcreations.pingme.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Mixin(CommandSuggestor.class)
public abstract class CommandSuggestorMixin {
    private static final Pattern COLON_PATTERN = Pattern.compile("(@)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");

    @Shadow @Final TextFieldWidget textField;

    @Shadow @Final private boolean slashOptional;

    @Shadow @Final private MinecraftClient client;

    @Shadow @Nullable private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(
            method = "refresh",
            at = @At("TAIL"),
            cancellable = true
    )
    public void onRefresh(CallbackInfo ci) {
        if (!this.client.isInSingleplayer()) {
            String message = this.textField.getText();
            StringReader reader = new StringReader(message);
            boolean hasSlash = reader.canRead() && reader.peek() == '/';
            if (hasSlash) reader.skip();
            boolean isCommand = this.slashOptional || hasSlash;
            int cursor = this.textField.getCursor();
            if (!isCommand) {
                String textUptoCursor = message.substring(0, cursor);
                int start = Math.max(TextUtil.getLastPattern(textUptoCursor, COLON_PATTERN) - 1, 0);
                int whitespace = TextUtil.getLastPattern(textUptoCursor, WHITESPACE_PATTERN);
                if (start < textUptoCursor.length() && start >= whitespace) {
                    if (textUptoCursor.charAt(start) == '@') {
                        List<String> playerNames = new ArrayList<>();
                        ClientPlayNetworkHandler networkHandler = this.client.getNetworkHandler();
                        if (networkHandler != null) {
                            networkHandler.getPlayerList().forEach(entry ->
                                    playerNames.add("@" + entry.getProfile().getName())
                            );
                            this.pendingSuggestions = CommandSource.suggestMatching(playerNames, new SuggestionsBuilder(textUptoCursor, start));
                            this.pendingSuggestions.thenRun(() -> {
                                if (this.pendingSuggestions.isDone()) return;
                                this.showSuggestions(false);
                            });
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }
}
