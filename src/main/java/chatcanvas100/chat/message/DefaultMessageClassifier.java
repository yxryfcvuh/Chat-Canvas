package chatcanvas100.chat.message;

import chatcanvas100.ChatCanvas;
import chatcanvas100.chat.identity.ChatMessageMetadata;
import chatcanvas100.chat.identity.PlayerChatIdentity;
import chatcanvas100.chat.identity.PluginChatFallbackResolver;
import chatcanvas100.config.PlayerColorConfig;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.Locale;

public final class DefaultMessageClassifier implements MessageClassifier {
	@Override
	public ClassifiedMessage classify(Text message, MessageContext context) {
		if (message == null) throw new IllegalArgumentException("message");
		MessageContext safe = context == null
				? MessageContext.direct(java.util.List.of(), null, "")
				: context;
		if (safe.ingress() == MessageIngress.CHAT) {
			ClassifiedMessage r = player(message, safe.sender(), safe);
			log("CHAT_ingress", r, message);
			return r;
		}
		if (safe.ingress() == MessageIngress.COMMAND_INPUT) {
			ClassifiedMessage r = system(message, ChatCanvasMessageSource.COMMAND_INPUT);
			log("COMMAND_INPUT_ingress", r, message);
			return r;
		}
		if (safe.ingress() == MessageIngress.CHAT_CANVAS_ERROR) {
			ClassifiedMessage r = system(message, ChatCanvasMessageSource.CHAT_CANVAS_ERROR);
			log("ERROR_ingress", r, message);
			return r;
		}
		if (safe.overlay()) {
			ClassifiedMessage r = system(message, ChatCanvasMessageSource.UNKNOWN);
			log("overlay", r, message);
			return r;
		}

		ClassifiedMessage translatedPlayerChat = translatedPlayerChat(message, safe);
		if (translatedPlayerChat != null) {
			log("translatedPlayerChat", translatedPlayerChat, message);
			return translatedPlayerChat;
		}

		ChatCanvasMessageSource vanilla = vanillaSource(message);
		if (vanilla != null) {
			ClassifiedMessage r = system(message, vanilla);
			log("vanillaSrc=" + vanilla.name(), r, message);
			return r;
		}

		var plugin = PluginChatFallbackResolver.resolve(message, safe.onlinePlayers());
		if (plugin.isPresent()) {
			ClassifiedMessage r = player(message, plugin.get().sender(), safe);
			log("pluginFallback", r, message);
			return r;
		}
		ClassifiedMessage r = system(message, safe.ingress() == MessageIngress.GAME
				? ChatCanvasMessageSource.SYSTEM
				: ChatCanvasMessageSource.UNKNOWN);
		log("FALLBACK", r, message);
		return r;
	}

	private static void log(String path, ClassifiedMessage c, Text message) {
		ChatCanvas.LOGGER.info("[ChatCanvas] classify: path={} -> channel={} src={} txt='{}'",
				path, c.channel(), c.source(),
				message.getString().replace("\n", "\\n"));
	}

	private static ClassifiedMessage player(
			Text message, PlayerChatIdentity sender, MessageContext context) {
		boolean self = isSelf(sender, context);
		return new ClassifiedMessage(
				ChatCanvasChannel.PLAYER_CHAT,
				self ? ChatCanvasMessageSource.SELF_PLAYER : ChatCanvasMessageSource.PLAYER,
				sender == null ? null : sender.uuid(),
				sender == null ? null : Text.literal(sender.playerName()),
				message,
				self
		);
	}

	private static ClassifiedMessage system(Text message, ChatCanvasMessageSource source) {
		return new ClassifiedMessage(
				ChatCanvasChannel.COMMAND_SYSTEM, source,
				null, null, message, false);
	}

	private static boolean isSelf(PlayerChatIdentity sender, MessageContext context) {
		if (sender == null) return false;
		if (sender.uuid() != null && context.localPlayerUuid() != null) {
			if (!sender.uuid().equals(context.localPlayerUuid())) return false;
			if (sender.reliable()) return true;
			return uniquelyMatchesLocalPlayer(sender, context);
		}
		if (!sender.reliable()) return false;
		return uniquelyMatchesLocalPlayer(sender, context);
	}

	private static boolean uniquelyMatchesLocalPlayer(
			PlayerChatIdentity sender, MessageContext context) {
		String senderName = PlayerColorConfig.normalizeName(sender.playerName());
		String localName = PlayerColorConfig.normalizeName(context.localPlayerName());
		if (senderName.isEmpty() || !senderName.equals(localName)) return false;
		long matches = context.onlinePlayers().stream()
				.filter(player -> PlayerColorConfig.normalizeName(player.playerName())
						.equals(localName))
				.count();
		return matches == 1;
	}

	private static ChatCanvasMessageSource vanillaSource(Text message) {
		if (!(message.getContent() instanceof TranslatableTextContent translated)) return null;
		String key = translated.getKey().toLowerCase(Locale.ROOT);
		if (key.startsWith("death.")) return ChatCanvasMessageSource.DEATH_MESSAGE;
		if (key.equals("multiplayer.player.joined")
				|| key.equals("multiplayer.player.joined.renamed")) {
			return ChatCanvasMessageSource.PLAYER_JOIN;
		}
		if (key.equals("multiplayer.player.left")) return ChatCanvasMessageSource.PLAYER_LEAVE;
		if (isCommandError(key)) return ChatCanvasMessageSource.COMMAND_ERROR;
		if (key.startsWith("commands.") || key.startsWith("command.")) {
			return ChatCanvasMessageSource.COMMAND_RESULT;
		}
		if (key.startsWith("chat.") || key.startsWith("multiplayer.")) {
			return ChatCanvasMessageSource.SYSTEM;
		}
		// Do NOT return SERVER_NOTICE for unrecognised keys — that would
		// short-circuit PluginChatFallbackResolver, which can still extract
		// a player name from the plain-text content.
		return null;
	}

	private static ClassifiedMessage translatedPlayerChat(Text message, MessageContext context) {
		if (!(message.getContent() instanceof TranslatableTextContent translated)) {
			ChatCanvas.LOGGER.info("[ChatCanvas] classify: translatedPlayerChat -> SKIP (not translatable) txt='{}'",
					message.getString().replace("\n", "\\n"));
			return null;
		}
		String key = translated.getKey();
		if (!isChatTypeKey(key)) {
			ChatCanvas.LOGGER.info("[ChatCanvas] classify: translatedPlayerChat -> SKIP (key={}) txt='{}'",
					key, message.getString().replace("\n", "\\n"));
			return null;
		}
		Object[] args = translated.getArgs();
		if (args.length == 0) {
			ChatCanvas.LOGGER.info("[ChatCanvas] classify: translatedPlayerChat -> SKIP (no args)");
			return null;
		}
		String candidate = args[0] instanceof Text text
				? text.getString() : String.valueOf(args[0]);
		PlayerChatIdentity matched = null;
		for (PlayerChatIdentity player : context.onlinePlayers()) {
			if (!PlayerColorConfig.normalizeName(player.playerName()).equals(
					PlayerColorConfig.normalizeName(candidate))) continue;
			if (matched != null) {
				ChatCanvas.LOGGER.info("[ChatCanvas] classify: translatedPlayerChat -> SKIP (ambiguous: {})",
						candidate);
				return null;
			}
			matched = player;
		}
		if (matched == null) {
			ChatCanvas.LOGGER.info("[ChatCanvas] classify: translatedPlayerChat -> SKIP (no match for '{}' among {} players)",
					candidate, context.onlinePlayers().size());
		}
		return matched == null ? null : player(message, matched, context);
	}

	/**
	 * Recognises translatable keys that carry player chat content.
	 * In 1.21+ the chat type system may produce keys such as
	 * {@code chat.type.text}, {@code chat.type.announcement}, or
	 * server-defined variants with a namespace prefix.
	 * <p>
	 * Some servers (especially those with chat plugins) use
	 * simple format strings like {@code %s} or {@code %1$s}
	 * as the translatable key, with the entire rendered chat
	 * line packed into the first argument.
	 */
	private static boolean isChatTypeKey(String key) {
		if (key.equals("chat.type.text")) return true;
		if (key.equals("chat.type.announcement")) return true;
		if (key.equals("chat.type.team")) return true;
		// Simple format-string keys used by plugin-based servers
		if (key.equals("%s") || key.equals("%1$s")) return true;
		// Also accept namespaced variants such as "minecraft:chat.type.text"
		int colon = key.lastIndexOf(':');
		String bareKey = colon >= 0 ? key.substring(colon + 1) : key;
		return bareKey.equals("chat.type.text")
				|| bareKey.equals("chat.type.announcement")
				|| bareKey.equals("chat.type.team");
	}

	private static boolean isCommandError(String key) {
		return key.contains("unknown")
				|| key.contains("invalid")
				|| key.contains("failed")
				|| key.contains("exception")
				|| key.startsWith("argument.")
				|| key.startsWith("brigadier.");
	}
}
