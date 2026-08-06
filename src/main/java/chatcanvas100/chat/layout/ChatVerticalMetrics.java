package chatcanvas100.chat.layout;

public record ChatVerticalMetrics(
		double glyphHeight,
		double backgroundTopOffset,
		double backgroundHeight,
		double lineAdvance
) {
	public double backgroundTop(double textY) {
		return textY - backgroundTopOffset;
	}

	public double backgroundBottom(double textY) {
		return backgroundTop(textY) + backgroundHeight;
	}
}
