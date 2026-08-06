package chatcanvas100;
import chatcanvas100.voice.VoiceInputManager;
import chatcanvas100.voice.VoskEncodingBootstrap;
import chatcanvas100.voice.VoiceEncodingDiagnostics;

import com.sun.jna.Native;
import chatcanvas100.chat.layout.ChatLineWidthCache;
import chatcanvas100.chat.layout.ChatLayoutRuntime;
import chatcanvas100.chat.identity.PlayerChatCapture;
import chatcanvas100.chat.input.ChatCanvasInputScreenBridge;
import chatcanvas100.chat.interaction.PlayerNameDoubleClickHandler;
import chatcanvas100.chat.notification.MentionNotificationController;
import chatcanvas100.chat.render.DualChatHudRenderer;
import chatcanvas100.chat.command.CommandToolRuntime;
import chatcanvas100.chat.history.ChatLogConfigStorage;
import chatcanvas100.chat.history.LocalChatLogService;
import chatcanvas100.chat.emoji.EmojiFontSupport;
import chatcanvas100.chat.emoji.EmojiRuntime;
import chatcanvas100.chat.text.GlyphAdvanceCache;
import chatcanvas100.config.ChatCanvasConfig;
import chatcanvas100.compat.ChatCanvasCompat;
import chatcanvas100.editor.ChatCanvasEditorScreen;
import chatcanvas100.editor.EditorScreenFactory;
import java.nio.charset.Charset;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ChatCanvasClient implements ClientModInitializer {
	private static KeyBinding openEditor;

	@Override
	public void onInitializeClient() {
		VoskEncodingBootstrap.initialize();
		if (VoiceEncodingDiagnostics.enabled()) {
			ChatCanvas.LOGGER.info(
					"Voice encoding bootstrap initialized: defaultCharset={}, "
							+ "file.encoding={}, native.encoding={}, jna.encoding={}, "
							+ "jna.defaultStringEncoding={}",
					Charset.defaultCharset(),
					System.getProperty("file.encoding"),
					System.getProperty("native.encoding"),
					System.getProperty("jna.encoding"),
					Native.getDefaultStringEncoding());
		}
		ChatCanvasConfig.initialize();
		EmojiRuntime.initialize();
		ChatCanvasCompat.initialize();
		MentionNotificationController.instance().register();
		PlayerChatCapture.register();
		{
			ChatLogConfigStorage logConfigStorage = new ChatLogConfigStorage();
			LocalChatLogService.instance().updateConfig(logConfigStorage.load());
		}
		ClientLifecycleEvents.CLIENT_STOPPING.register(
				client -> {
					CommandToolRuntime.manager().flush();
					EmojiRuntime.flush();
					VoiceInputManager.instance().shutdown();
					LocalChatLogService.instance().close();
				});
		openEditor = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.chat_canvas.open_editor",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				net.minecraft.client.option.KeyBinding.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ChatLayoutRuntime.tick(client);
			CommandToolRuntime.manager().tick(System.currentTimeMillis());
			EmojiRuntime.tick(client);
			if (client.currentScreen instanceof ChatCanvasInputScreenBridge bridge) {
				bridge.chat_canvas$voiceTick();
			} else if (VoiceInputManager.instance().isBusy()) {
				VoiceInputManager.instance().cancel();
			}
			if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen chatScreen) {
				PlayerNameDoubleClickHandler.instance().tick(chatScreen);
			} else {
				PlayerNameDoubleClickHandler.instance().reset();
			}
			while (openEditor.wasPressed()) {
				if (!(client.currentScreen instanceof ChatCanvasEditorScreen)) {
					client.setScreen(EditorScreenFactory.create(client.currentScreen));
				}
			}
		});
	}

	public static net.minecraft.client.option.KeyBinding voiceInputKey() {
		return openEditor;
	}
}
