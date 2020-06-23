package com.aidancbrady.aidanet.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.aidancbrady.aidanet.AidaNet;
import com.aidancbrady.aidanet.Connection;

public class AidaNetServer extends AidaNet {

    private boolean running = false;

    private ServerSocket serverSocket;

    private Map<UUID, Connection> connections = new LinkedHashMap<>();
    private List<ConnectionListener> listeners = new ArrayList<>();

    public AidaNetServer(int port) {
        super(false, port);
    }

    public void start() throws IOException {
        if (!running) {
            running = true;
            serverSocket = new ServerSocket(getPort());
            new ConnectionHandler().start();
            info("Initiated server.");
        }
    }

    public void stop() throws IOException {
        if (running) {
            // clone collection to prevent CME
            new HashSet<>(connections.values()).forEach(c -> c.close());
            serverSocket.close();
            info("Shut down server.");
        }

        running = false;
    }

    public Collection<Connection> getConnections() {
        return connections.values();
    }

    public void addListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void handleDisconnect(Connection connection) {
        connections.remove(connection.getUUID());
        listeners.forEach(l -> l.onDisconnect(connection));
    }

    @Override
    public void handleConnect(Connection connection) {
        listeners.forEach(l -> l.onConnect(connection));
    }

    private class ConnectionHandler extends Thread {

        @Override
        public void run() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();

                    if (running) {
                        info("Initiating connection with " + socket.getInetAddress());
                        Connection connection = Connection.create(AidaNetServer.this, socket, UUID.randomUUID());
                        connections.put(connection.getUUID(), connection);
                        connection.start();
                    }
                } catch (IOException e) {
                    if (!e.getMessage().contains("Socket closed")) {
                        warn("Error while accepting connection.");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public interface ConnectionListener {

        void onConnect(Connection connection);

        void onDisconnect(Connection connection);
    }
}
