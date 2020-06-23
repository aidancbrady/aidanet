package com.aidancbrady.aidanet.client;

import java.io.IOException;
import java.net.Socket;
import com.aidancbrady.aidanet.AidaNet;
import com.aidancbrady.aidanet.Connection;
import com.aidancbrady.aidanet.Message;

public class AidaNetClient extends AidaNet {

    private String ip;

    private Connection activeConnection;

    public AidaNetClient(String ip, int port) {
        super(true, port);
        this.ip = ip;
    }

    public String getIP() {
        return ip;
    }

    public void connect() throws IOException, InterruptedException {
        if (!isConnected()) {
            Socket socket = new Socket(ip, getPort());
            activeConnection = Connection.create(this, socket);
            activeConnection.start();
            // wait for handshake to complete
            synchronized (this) {
                wait();
            }
        }
    }

    public void disconnect() {
        if (isConnected()) {
            activeConnection.close();
        }
    }

    public void sendToServer(Message message) {
        if (isConnected()) {
            activeConnection.send(message);
        }
    }

    public boolean isConnected() {
        return activeConnection != null;
    }

    @Override
    public void handleDisconnect(Connection connection) {
        activeConnection = null;
    }

    @Override
    public void handleConnect(Connection connection) {
        synchronized (this) {
            notify();
        }
    }
}
