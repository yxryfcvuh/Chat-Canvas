package chatcanvas100.chat.style;

import chatcanvas100.config.MentionConfig;
import net.minecraft.text.Style;

import java.util.function.UnaryOperator;

public final class MentionStyleProvider {
	public UnaryOperator<Style> overlay(MentionConfig value) {
		MentionConfig config = value == null ? MentionConfig.DEFAULT : value.sanitized();
		return style -> {
			Style result = style.withColor(config.highlightColor());
			return config.highlightBold() ? result.withBold(true) : result;
		};
	}
}
