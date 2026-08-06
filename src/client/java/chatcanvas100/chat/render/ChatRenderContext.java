package chatcanvas100.chat.render;

import net.minecraft.client.gui.DrawContext;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import chatcanvas100.config.ChatTextConfig;
import chatcanvas100.config.ChatBackgroundConfig;
import chatcanvas100.config.PlayerColorConfig;
import chatcanvas100.config.MentionConfig;
import chatcanvas100.config.PlayerChatLayoutMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public record ChatRenderContext(
		DrawContext drawContext,
		TextRenderer textRenderer,
		int x,
		int y,
		int width,
		int height,
		float messageOpacity,
		float inputProgress,
		Text inputPlaceholder,
		ChatTextConfig textConfig,
		ChatBackgroundConfig backgroundConfig,
		PlayerColorConfig playerColorConfig,
		MentionConfig mentionConfig,
		String localPlayerName,
		PlayerChatLayoutMode playerChatLayoutMode,
		double splitMessageMaxWidthRatio,
		double vanillaBackgroundOpacity
) {
	public int right() {
		return x + width;
	}

	public int bottom() {
		return y + height;
	}
}
