package chatcanvas100.chat.layout;

public interface PlayerChatLayoutStrategy {
	int wrapWidth(int contentWidth, int headWidth, double splitRatio, boolean selfMessage);

	int textX(int contentLeft, int contentRight, int lineWidth,
			  int headWidth, boolean selfMessage);

	int headX(int contentLeft, int textX, int headWidth, boolean selfMessage);

	boolean reserveHead(boolean selfMessage);
}
