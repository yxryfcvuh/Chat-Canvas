package chatcanvas100.chat.command;

import java.util.List;

public record ClipboardCommandParseResult(
		List<ClipboardCommandCandidate> candidates,
		boolean multipleLines,
		boolean truncated
) {
	public ClipboardCommandParseResult {
		candidates = candidates == null ? List.of() : List.copyOf(candidates);
	}
}
