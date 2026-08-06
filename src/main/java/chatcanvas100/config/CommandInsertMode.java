package chatcanvas100.config;

public enum CommandInsertMode {
	REPLACE_INPUT,
	INSERT_AT_CURSOR;

	public CommandInsertMode opposite() {
		return this == REPLACE_INPUT ? INSERT_AT_CURSOR : REPLACE_INPUT;
	}
}
