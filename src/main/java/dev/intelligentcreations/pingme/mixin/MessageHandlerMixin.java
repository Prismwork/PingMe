package dev.intelligentcreations.pingme.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
	@Unique
	private static final HashMap<Pattern, Integer> PATTERN_COLOR_MAP = new HashMap<>(ImmutableMap.of(
			// @everyone
			Pattern.compile("@everyone(?![A-Za-z0-9_.-])"), 0xFFCB25,
			// @...
			Pattern.compile("@(?!everyone)[A-Za-z0-9_.-]+"), 0xFF52D0
	));

	@ModifyArg(
			method = "processChatMessageInternal",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"
			)
	)
	private Text colorPings(Text message) {
		return matchAndColor(message.getString());
	}

	@Unique
	private MutableText matchAndColor(String string) {
		String lasting = string;
		MutableText text = Text.empty();

		while (!lasting.isEmpty()) {
			String finalLasting = lasting;
			ArrayList<Matcher> matchers = PATTERN_COLOR_MAP.keySet().stream().map(
					pattern -> pattern.matcher(finalLasting)
			).collect(Collectors.toCollection(ArrayList::new));

			Optional<Matcher> currentMatcher = matchers.stream().filter(Matcher::find).findFirst();
			if (currentMatcher.isPresent()) {
				int start = currentMatcher.get().start(), end = currentMatcher.get().end();
				int color = PATTERN_COLOR_MAP.get(currentMatcher.get().pattern());

				String betweenMatches = lasting.substring(0, start);
				String currentMatch = lasting.substring(start, end);

				text.append(Text.literal(betweenMatches));
				text.append(Text.literal(currentMatch).styled(withStyle(currentMatch, color)));

				lasting = lasting.substring(end);
			}
			else {
				break;
			}
		}

		return text.append(Text.literal(lasting));
	}

	@Unique
	private UnaryOperator<Style> withStyle(String ping, int color) {
		// Remove @ symbol
		String name = ping.substring(1);

		UnaryOperator<Style> baseStyle =
				style -> style.withColor(color)
								 .withItalic(true)
								 .withClickEvent(new ClickEvent(
										 ClickEvent.Action.SUGGEST_COMMAND,
										 ping + " "
								 ));

		// 'everyone'
		if (name.equals("everyone")) {
			return style -> baseStyle.apply(style)
									.withHoverEvent(new HoverEvent(
											HoverEvent.Action.SHOW_TEXT,
											Text.translatable("action.pingme.ping_everyone")
													.styled(s -> s.withColor(color))
									));
		}

		// '...'
		else {
			AtomicReference<UnaryOperator<Style>> result = new AtomicReference<>(style -> style);

			if (MinecraftClient.getInstance().getServer() != null) {
				MinecraftClient.getInstance().getServer().getPlayerManager().getPlayerList()
						.stream().filter(player -> Objects.equals(player.getName().getString(), name)).findFirst().ifPresent(player -> result.set(
								style -> baseStyle.apply(style)
												 .withHoverEvent(new HoverEvent(
														 HoverEvent.Action.SHOW_TEXT,
														 MutableText.of(Text.empty().getContent())
																 .append(
																		 MutableText.of(player.getDisplayName().getContent())
																				 .styled(s -> s.withColor(color))
																 ).append("\n")
																 .append(Text.literal(player.getUuidAsString()))
												 ))
						));
			}

			return result.get();
		}
	}
}
