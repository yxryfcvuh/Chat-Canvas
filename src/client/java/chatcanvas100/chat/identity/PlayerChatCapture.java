package chatcanvas100.chat.identity;

import com.mojang.authlib.GameProfile;
import chatcanvas100.chat.interaction.PlayerNameDoubleClickHandler;
import chatcanvas100.chat.input.ChatCanvasInputController;
import chatcanvas100.chat.command.CommandToolRuntime;
import chatcanvas100.chat.message.ChatCanvasMessageIngress;
import chatcanvas100.chat.render.DualChatHudRenderer;
import chatcanvas100.chat.message.MessageIngress;
import chatcanvas100.chat.notification.MentionNotificationController;
import chatcanvas100.voice.VoiceInputManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;

import java.util.Optional;

public final class PlayerChatCapture {
	private static boolean registered;

	private PlayerChatCapture() {
	}

	public static synchronized void register() {
		if (registered) return;
		registered = true;
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
			Optional<ChatMessageMetadata> metadata = standardMetadata(
					message, signedMessage, sender, params.name());
			metadata.ifPresent(value -> ChatMessageMetadataRegistry.instance()
					.registerIncoming(message, signatureOf(signedMessage), value));
			ChatCanvasMessageIngress.instance().registerIncoming(
					message,
					signatureOf(signedMessage),
					MessageIngress.CHAT,
					metadata.map(ChatMessageMetadata::sender).orElse(null),
					params.name(),
					false);
		});
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;
			Optional<ChatMessageMetadata> metadata =
					PluginChatFallbackResolver.resolve(message, PlayerRosterTracker.onlinePlayers());
			metadata.ifPresent(value -> ChatMessageMetadataRegistry.instance()
					.registerIncoming(message, null, value));
			ChatCanvasMessageIngress.instance().registerIncoming(
					message, null, MessageIngress.GAME,
					metadata.map(ChatMessageMetadata::sender).orElse(null),
					null, false);
		});
		ClientSendMessageEvents.CHAT.register(
				message -> ChatCanvasInputController.instance()
						.recordSentPlayerChat(message));
		ClientSendMessageEvents.COMMAND.register(command -> {
			ChatCanvasInputController.instance().recordExecutedCommand(command);
			ChatCanvasMessageIngress.instance().acceptCommand(command);
			CommandToolRuntime.recordExecuted(command);
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			CommandToolRuntime.beginSession(client);
			ChatCanvasInputController.instance().clearSession();
			ChatCanvasMessageIngress.instance().clearWorld();
			DualChatHudRenderer.instance().resetWorld();
			MentionNotificationController.instance().clearSession();
			PlayerRosterTracker.refresh(handler);
			chatcanvas100.chat.history.LocalChatLogService.instance()
					.switchContext(chatcanvas100.chat.history.ChatLogContexts.current(client));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			VoiceInputManager.instance().cancel();
			CommandToolRuntime.endSession();
			ChatCanvasInputController.instance().clearSession();
			PlayerRosterTracker.clear();
			ChatMessageMetadataRegistry.instance().clearAll();
			PlayerNameHitboxRegistry.clear();
			PlayerNameDoubleClickHandler.instance().reset();
			ChatCanvasMessageIngress.instance().clearWorld();
			DualChatHudRenderer.instance().resetWorld();
			MentionNotificationController.instance().clearSession();
			chatcanvas100.chat.history.LocalChatLogService.instance().switchContext(null);
		});
	}

	private static Optional<ChatMessageMetadata> standardMetadata(
			Text message, SignedMessage signedMessage, GameProfile sender, Text senderName) {
		if (sender != null) {
			return PlayerIdentityResolver.resolveStandard(
					message, senderName, sender.id(), sender.name());
		}
		String displayName = senderName == null ? "" : senderName.getString();
		return PlayerRosterTracker.onlinePlayers().stream()
				.filter(player -> displayName.equalsIgnoreCase(player.playerName())
						|| PlayerIdentityResolver.boundedIndexOf(
								displayName, player.playerName(), 0) >= 0)
				.findFirst()
				.flatMap(player -> PlayerIdentityResolver.resolveStandard(
						message, senderName, player.uuid(), player.playerName()));
	}

	private static MessageSignatureData signatureOf(SignedMessage message) {
		return message == null ? null : message.signature();
	}
}
