package com.aidancbrady.aidanet;

import java.io.PrintStream;

public abstract class AidaNet {

    private MessageRegistry messageRegistry;

    private boolean remote;
    private int port;

    public AidaNet(boolean remote, int port) {
        this.remote = remote;
        this.port = port;

        messageRegistry = new MessageRegistry(this);
    }

    public abstract void handleConnect(Connection connection);

    public abstract void handleDisconnect(Connection connection);

    public int getPort() {
        return port;
    }

    public MessageRegistry getMessageRegistry() {
        return messageRegistry;
    }

    public boolean isRemote() {
        return remote;
    }

    public void info(String s) {
        log(System.out, "INFO: " + s);
    }

    public void warn(String s) {
        log(System.err, "WARN: " + s);
    }

    public void severe(String s) {
        log(System.err, "SEVERE: " + s);
    }

    public void log(PrintStream stream, String s) {
        stream.println((remote ? "CLIENT " : "SERVER ") + s);
    }
}
