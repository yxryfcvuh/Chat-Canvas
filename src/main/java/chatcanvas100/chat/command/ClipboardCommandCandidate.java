package chatcanvas100.chat.command;

public record ClipboardCommandCandidate(
		String command,
		boolean hadLeadingSlash,
	String preview
) {
	public ClipboardCommandCandidate {
		command = CommandTextSanitizer.normalizeClipboardCommand(command);
		preview = preview == null ? command : preview;
	}
}
