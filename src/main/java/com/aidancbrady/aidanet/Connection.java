package com.aidancbrady.aidanet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import com.aidancbrady.aidanet.client.AidaNetClient;
import com.aidancbrady.aidanet.server.AidaNetServer;

public class Connection extends Thread {

    private AidaNet owner;
    private Socket socket;
    private UUID uuid;

    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Encryptor encryptor;
    private OutThread out;

    private boolean disconnected;

    private Connection(AidaNet owner, Socket socket) throws IOException {
        this.owner = owner;
        this.socket = socket;

        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());

        encryptor = new Encryptor();
        out = new OutThread();
    }

    public static Connection create(AidaNetServer owner, Socket socket, UUID uuid) throws IOException {
        Connection connection = new Connection(owner, socket);
        connection.uuid = uuid;
        return connection;
    }

    public static Connection create(AidaNetClient owner, Socket socket) throws IOException {
        return new Connection(owner, socket);
    }

    @Override
    public void run() {
        if(!handshake()) {
            close();
            return;
        }

        out.start();
        owner.handleConnect(this);

        while (!disconnected) {
            try {
                int id = inputStream.readInt();
                int msgSize = inputStream.readInt();
                byte[] bytes = new byte[msgSize];
                inputStream.readFully(bytes);
                ByteArrayInputStream buffer = new ByteArrayInputStream(encryptor.decrypt(bytes));
                owner.getMessageRegistry().handleMessage(id, new DataInputStream(buffer));
            } catch (EOFException e) {
                owner.info("Reached end of stream.");
                break;
            } catch (IOException e) {
                if (e.getMessage().contains("Socket closed")) {
                    owner.info("Socket closed, terminating.");
                } else {
                    e.printStackTrace();
                }
                // terminate if we encounter an IOException
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        owner.info("Terminating connection.");
        close();
    }

    private boolean handshake() {
        if (!encryptor.init(this) || !encryptor.parseHandshake(this)) {
            owner.warn("Failed to complete security protocol handshake. ");
            return false;
        }

        try {
            if (getOwner().isRemote()) {
                uuid = Util.readUUID(inputStream);
            } else {
                Util.writeUUID(outputStream, uuid);
                outputStream.flush();
            }
            return true;
        } catch (IOException e) {
            owner.warn("Failed to complete UUID transfer protocol.");
            e.printStackTrace();
            return false;
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public UUID getUUID() {
        return uuid;
    }

    public AidaNet getOwner() {
        return owner;
    }

    public DataOutputStream getOutputStream() {
        return outputStream;
    }

    public DataInputStream getInputStream() {
        return inputStream;
    }

    public void send(Message message) {
        out.queue(message);
    }

    public void close() {
        if (disconnected) {
            return;
        }

        if(out != null) {
            out.interrupt();
        }

        if(socket != null) {
            try {
                socket.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        owner.handleDisconnect(this);
        disconnected = true;
    }

    public class OutThread extends Thread {

        public final Queue<Message> messageQueue = new LinkedList<>();

        @Override
        public void run() {
            while(!disconnected) {
                try {
                    Message m = messageQueue.poll();
                    if(m != null) {
                        sendMessage(m);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            owner.info("Ending output stream");
        }

        private void sendMessage(Message m) throws Exception {
            int id = owner.getMessageRegistry().getMessageID(m);
            if (id == -1) {
                owner.warn("Attempted to send unregistered message: " + m.getClass().getName());
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            m.write(new DataOutputStream(buffer));
            owner.info("Sending message: " + m.getClass().getSimpleName() + ", size=" + buffer.size() + ", id=" + id);
            byte[] encrypted = encryptor.encrypt(buffer.toByteArray());
            outputStream.writeInt(id);
            outputStream.writeInt(encrypted.length);
            outputStream.write(encrypted);
            outputStream.flush();
        }

        public void queue(Message message) {
            messageQueue.add(message);
        }
    }
}
