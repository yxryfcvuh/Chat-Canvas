package chatcanvas100.voice;

public record VoiceRecognitionResult(String text, boolean empty, long durationMillis) {
	public VoiceRecognitionResult {
		text = text == null ? "" : text;
		empty = text.isBlank();
		durationMillis = Math.max(0L, durationMillis);
	}
}
