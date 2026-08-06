package chatcanvas100.chat.render;

import chatcanvas100.chat.layout.ChatBackgroundBounds;
import chatcanvas100.chat.layout.ChatBackgroundMetrics;
import chatcanvas100.config.ChatBackgroundConfig;

public final class ChatBackgroundRenderer {
	public void drawMessageBackground(ChatRenderContext context,
									  ChatBackgroundBounds bounds,
									  double vanillaOpacity) {
		ChatBackgroundConfig background = context.backgroundConfig().sanitized();
		int color = ChatBackgroundMetrics.composeBackgroundColor(
				background.messageColor(),
				background.messageOpacity(),
				vanillaOpacity
		);
		ChatBackgroundDraw.fill(context.drawContext(), bounds, color);
	}

	public void drawInputBackground(ChatRenderContext context, int y, int height) {
		ChatBackgroundConfig background = context.backgroundConfig().sanitized();
		int color = ChatBackgroundMetrics.composeBackgroundColor(
				background.inputColor(),
				background.inputOpacity(),
				context.inputProgress()
		);
		if (color >>> 24 != 0) {
			context.drawContext().fill(context.x(), y, context.right(), y + height, color);
		}
		if (background.inputBorderEnabled()) {
			int borderColor = ChatBackgroundMetrics.composeBackgroundColor(
					background.inputBorderColor(),
					background.inputBorderOpacity(),
					context.inputProgress()
			);
			ChatBackgroundDraw.drawRectBorder(
					context.drawContext(), context.x(), y, context.right(), y + height, borderColor);
		}
	}
}
