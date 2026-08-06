package chatcanvas100.voice;

import chatcanvas100.ChatCanvas;
import chatcanvas100.chat.message.ChatCanvasMessageIngress;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class VoiceErrorHandler {
	private final Map<String, Long> lastShown = new HashMap<>();

	public synchronized void report(String key, Throwable error) {
		long now = System.currentTimeMillis();
		if (now - lastShown.getOrDefault(key, 0L) < 5_000L) {
			if (error != null) ChatCanvas.LOGGER.debug("Repeated voice error: {}", key, error);
			return;
		}
		lastShown.put(key, now);
		ChatCanvasMessageIngress.instance().reportError(Text.translatable(key), error);
	}
}
