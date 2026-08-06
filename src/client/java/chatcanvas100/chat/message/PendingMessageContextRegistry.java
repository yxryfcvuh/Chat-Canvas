package chatcanvas100.chat.message;

import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PendingMessageContextRegistry {
	private static final int CAPACITY = 256;
	private static final long PLAIN_TEXT_TTL_MS = 5_000L;
	private final LinkedHashMap<MessageSignatureData, PendingMessage> signatures =
			new LinkedHashMap<>();
	private final IdentityHashMap<Text, Deque<PendingMessage>> identities =
			new IdentityHashMap<>();
	private final Deque<Text> order = new ArrayDeque<>();
	// Index ALL pending messages by plain text, not just unsigned ones.
	// This is the last-resort fallback when both signature and Text-identity
	// lookups fail — common on servers where the Fabric API event and
	// vanilla ChatHud.addMessage() receive distinct Text objects.
	private final LinkedHashMap<String, Deque<PendingMessage>> byText =
			new LinkedHashMap<>();

	public synchronized PendingMessage register(
			Text message, MessageSignatureData signature, MessageContext context) {
		if (message == null) throw new IllegalArgumentException("message");
		UUID id = signature == null
				? UUID.randomUUID()
				: UUID.nameUUIDFromBytes(signature.data());
		PendingMessage pending = new PendingMessage(
				id, context, message.getString(), System.currentTimeMillis());
		if (signature != null) {
			signatures.put(signature, pending);
			trimSignatures();
		}
		identities.computeIfAbsent(message, ignored -> new ArrayDeque<>()).addLast(pending);
		order.addLast(message);
		byText.computeIfAbsent(pending.plainText(), ignored -> new ArrayDeque<>())
				.addLast(pending);
		while (order.size() > CAPACITY) removeOldest();
		return pending;
	}

	public synchronized PendingMessage consume(Text message, MessageSignatureData signature) {
		// 1) Match by signature (most reliable)
		PendingMessage pending = signature == null ? null : signatures.remove(signature);
		Deque<PendingMessage> queue = identities.get(message);
		if (pending != null && queue != null) {
			queue.removeFirstOccurrence(pending);
		} else if (pending == null && queue != null) {
			// 2) Match by Text identity
			pending = queue.pollFirst();
		}
		// 3) Match by plain text (last-resort for BOTH signed and unsigned)
		if (pending == null) {
			pending = consumeByPlainText(message.getString(), System.currentTimeMillis());
		}
		if (queue != null && queue.isEmpty()) identities.remove(message);
		if (pending != null) {
			removeOneOrderReference(message);
			Deque<PendingMessage> textQueue = byText.get(pending.plainText());
			if (textQueue != null) {
				textQueue.removeFirstOccurrence(pending);
				if (textQueue.isEmpty()) byText.remove(pending.plainText());
			}
		}
		return pending;
	}

	public synchronized void clear() {
		signatures.clear();
		identities.clear();
		order.clear();
		byText.clear();
	}

	private void trimSignatures() {
		Iterator<Map.Entry<MessageSignatureData, PendingMessage>> iterator =
				signatures.entrySet().iterator();
		while (signatures.size() > CAPACITY && iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	private void removeOldest() {
		Text oldest = order.pollFirst();
		if (oldest == null) return;
		Deque<PendingMessage> queue = identities.get(oldest);
		if (queue == null) return;
		PendingMessage removed = queue.pollFirst();
		if (queue.isEmpty()) identities.remove(oldest);
		if (removed != null) {
			signatures.values().removeIf(value -> value == removed);
			Deque<PendingMessage> textQueue = byText.get(removed.plainText());
			if (textQueue != null) {
				textQueue.removeFirstOccurrence(removed);
				if (textQueue.isEmpty()) byText.remove(removed.plainText());
			}
		}
	}

	private void removeOneOrderReference(Text message) {
		Iterator<Text> iterator = order.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == message) {
				iterator.remove();
				return;
			}
		}
	}

	/**
	 * Matches a pending message by its plain-text content.  Works for ALL
	 * messages — signed and unsigned alike.  This is the fallback when
	 * both signature lookup and {@code Text}-identity lookup fail, which
	 * happens frequently on multiplayer servers where Fabric API and the
	 * vanilla {@code ChatHud.addMessage()} produce distinct {@code Text}
	 * instances for the same chat line.
	 */
	private PendingMessage consumeByPlainText(String plainText, long nowMs) {
		Deque<PendingMessage> queue = byText.get(plainText);
		if (queue == null) return null;
		// Expire stale entries
		Iterator<PendingMessage> iterator = queue.iterator();
		while (iterator.hasNext()) {
			PendingMessage candidate = iterator.next();
			if (nowMs - candidate.registeredAtMs() > PLAIN_TEXT_TTL_MS) {
				iterator.remove();
			}
		}
		if (queue.isEmpty()) {
			byText.remove(plainText);
			return null;
		}
		return queue.pollFirst();
	}

	public record PendingMessage(
			UUID messageId,
			MessageContext context,
			String plainText,
			long registeredAtMs
	) {
	}
}
