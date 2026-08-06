package chatcanvas100.editor;

import chatcanvas100.config.ChatCanvasSettings;
import chatcanvas100.config.ChatBackgroundConfig;
import chatcanvas100.config.ChatTextConfig;
import chatcanvas100.config.CommandClipboardConfig;
import chatcanvas100.config.LayoutConfig;
import chatcanvas100.config.MentionConfig;
import chatcanvas100.config.PlayerColorConfig;
import chatcanvas100.config.CommandSystemConfig;
import chatcanvas100.config.PlayerChatLayoutMode;

public record EditorSnapshot(
		LayoutConfig layout,
		ChatTextConfig text,
		ChatBackgroundConfig background,
		PlayerColorConfig playerColors,
		MentionConfig mention,
		CommandClipboardConfig commandClipboard,
		PlayerChatLayoutMode playerChatLayoutMode,
		double splitMessageMaxWidthRatio,
		CommandSystemConfig commandSystem
) {
	public EditorSnapshot(LayoutConfig layout, ChatTextConfig text) {
		this(layout, text, ChatBackgroundConfig.DEFAULT, PlayerColorConfig.DEFAULT,
				MentionConfig.DEFAULT, CommandClipboardConfig.DEFAULT,
				PlayerChatLayoutMode.CLASSIC,
				ChatCanvasSettings.DEFAULT_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
				CommandSystemConfig.DEFAULT);
	}

	public EditorSnapshot(LayoutConfig layout, ChatTextConfig text, ChatBackgroundConfig background) {
		this(layout, text, background, PlayerColorConfig.DEFAULT, MentionConfig.DEFAULT,
				CommandClipboardConfig.DEFAULT, PlayerChatLayoutMode.CLASSIC,
				ChatCanvasSettings.DEFAULT_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
				CommandSystemConfig.DEFAULT);
	}

	public EditorSnapshot(LayoutConfig layout, ChatTextConfig text, ChatBackgroundConfig background,
						  PlayerColorConfig playerColors) {
		this(layout, text, background, playerColors, MentionConfig.DEFAULT,
				CommandClipboardConfig.DEFAULT, PlayerChatLayoutMode.CLASSIC,
				ChatCanvasSettings.DEFAULT_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
				CommandSystemConfig.DEFAULT);
	}

	public EditorSnapshot(LayoutConfig layout, ChatTextConfig text, ChatBackgroundConfig background,
						  PlayerColorConfig playerColors, MentionConfig mention) {
		this(layout, text, background, playerColors, mention, CommandClipboardConfig.DEFAULT,
				PlayerChatLayoutMode.CLASSIC,
				ChatCanvasSettings.DEFAULT_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
				CommandSystemConfig.DEFAULT);
	}

	public EditorSnapshot {
		layout = layout == null ? LayoutConfig.DEFAULT : layout.sanitized();
		text = text == null ? ChatTextConfig.DEFAULT : text.sanitized();
		background = background == null ? ChatBackgroundConfig.DEFAULT : background.sanitized();
		playerColors = playerColors == null ? PlayerColorConfig.DEFAULT : playerColors.sanitized();
		mention = mention == null ? MentionConfig.DEFAULT : mention.sanitized();
		commandClipboard = commandClipboard == null
				? CommandClipboardConfig.DEFAULT : commandClipboard.sanitized();
		playerChatLayoutMode = playerChatLayoutMode == null
				? PlayerChatLayoutMode.CLASSIC : playerChatLayoutMode;
		if (!Double.isFinite(splitMessageMaxWidthRatio)) {
			splitMessageMaxWidthRatio =
					ChatCanvasSettings.DEFAULT_SPLIT_MESSAGE_MAX_WIDTH_RATIO;
		}
		splitMessageMaxWidthRatio = Math.max(
				ChatCanvasSettings.MIN_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
				Math.min(ChatCanvasSettings.MAX_SPLIT_MESSAGE_MAX_WIDTH_RATIO,
						splitMessageMaxWidthRatio));
		commandSystem = commandSystem == null ? CommandSystemConfig.DEFAULT : commandSystem.sanitized();
	}

	public ChatCanvasSettings settings() {
		return new ChatCanvasSettings(
				layout, text, background, playerColors, mention, commandClipboard,
				java.util.List.of(), chatcanvas100.editor.EditorUiStyle.CHAT_CANVAS,
				true, true, playerChatLayoutMode, splitMessageMaxWidthRatio, commandSystem);
	}
}
