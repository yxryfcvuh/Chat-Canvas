package chatcanvas100.ui;

import chatcanvas100.editor.EditorPointerTarget;

public interface NumericScrubber extends EditorPointerTarget {
	boolean valueRegionContains(double mouseX, double mouseY);

	boolean scroll(double amount);

	boolean restoreDefault();

	void resizeViewport(int width, int height);
}
