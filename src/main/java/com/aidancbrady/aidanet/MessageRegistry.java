package com.aidancbrady.aidanet;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MessageRegistry {

    private List<MessageRegistryObject<?>> messages = new ArrayList<>();
    private Map<Class<?>, MessageRegistryObject<?>> messageMap = new HashMap<>();

    private AidaNet owner;

    public MessageRegistry(AidaNet owner) {
        this.owner = owner;
    }

    public void registerMessage(Class<? extends Message> messageClass, Supplier<? extends Message> creator) {
        MessageRegistryObject<?> obj = new MessageRegistryObject<>(messages.size(), creator);
        messages.add(obj);
        messageMap.put(messageClass, obj);
    }

    public void handleMessage(int messageId, DataInputStream inputStream) throws IOException {
        if (messageId > messages.size()) {
            owner.severe("Received message with invalid ID. Discarding.");
            return;
        }

        handle(messageId, inputStream, messages.get(messageId));
    }

    public void syncFrom(MessageRegistry other) {
        messages = other.messages;
        messageMap = other.messageMap;
    }

    private <T extends Message> void handle(int messageId, DataInputStream inputStream, MessageRegistryObject<T> obj) throws IOException {
        T message = obj.create();
        int size = inputStream.available();
        message.read(inputStream);
        message.handle();
        obj.listeners.forEach(l -> l.handle(message));
        owner.info("Handled message: " + message.getClass().getSimpleName() + ", size=" + size + ", id=" + obj.getID());
    }

    public int getMessageID(Message m) {
        MessageRegistryObject<?> obj = messageMap.get(m.getClass());
        return obj != null ? obj.getID() : -1;
    }

    @SuppressWarnings("unchecked")
    public <T extends Message> void addListener(Class<T> messageClass, MessageListener<T> listener) {
        MessageRegistryObject<T> obj = (MessageRegistryObject<T>) messageMap.get(messageClass);
        if (obj != null) {
            obj.listeners.add(listener);
        } else {
            owner.warn("Attempted to add a listener for an unregistered message class.");
        }
    }

    private static class MessageRegistryObject<T extends Message> {

        private List<MessageListener<T>> listeners = new ArrayList<>();

        private int id;
        private Supplier<T> creator;

        public MessageRegistryObject(int id, Supplier<T> creator) {
            this.id = id;
            this.creator = creator;
        }

        public int getID() {
            return id;
        }

        public T create() {
            return creator.get();
        }
    }

    public interface MessageListener<T extends Message> {

        void handle(T message);
    }
}
