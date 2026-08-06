package chatcanvas100.chat.history;

import chatcanvas100.chat.message.ChatCanvasMessage;

public interface ChatLogService extends AutoCloseable {
    void record(ChatCanvasMessage message);
    void switchContext(ChatLogContext context);
    void flush();
    @Override
    void close();
}
