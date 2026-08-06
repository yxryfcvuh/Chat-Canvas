package chatcanvas100.chat.message;

import net.minecraft.text.Text;

public interface MessageClassifier {
	ClassifiedMessage classify(Text message, MessageContext context);
}
