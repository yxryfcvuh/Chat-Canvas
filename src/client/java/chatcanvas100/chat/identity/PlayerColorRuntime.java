package chatcanvas100.chat.identity;

import chatcanvas100.config.ChatCanvasConfig;

public final class PlayerColorRuntime {
	private static final PlayerNameColorProvider PROVIDER = new PlayerNameColorProvider();

	private PlayerColorRuntime() {
	}

	public static PlayerNameColorProvider provider() {
		PROVIDER.updateConfig(ChatCanvasConfig.instance().playerColors());
		return PROVIDER;
	}
}
