package chatcanvas100.chat.input;

public final class CommandInputState {
	private String command = "";
	private int cursor;
	private int selectionEnd;
	private int historyIndex;

	public ChatInputSnapshot snapshot() {
		return new ChatInputSnapshot(command, cursor, selectionEnd);
	}

	void restore(ChatInputSnapshot snapshot) {
		ChatInputSnapshot safe = snapshot == null
				? ChatInputSnapshot.EMPTY : snapshot;
		command = safe.text();
		cursor = Math.min(safe.cursor(), command.length());
		selectionEnd = Math.min(safe.selectionEnd(), command.length());
	}

	public String command() {
		return command;
	}

	public int cursor() {
		return cursor;
	}

	public int selectionEnd() {
		return selectionEnd;
	}

	public int historyIndex() {
		return historyIndex;
	}

	void historyIndex(int historyIndex) {
		this.historyIndex = Math.max(0, historyIndex);
	}
}
