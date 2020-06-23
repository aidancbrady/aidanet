package com.aidancbrady.aidanet.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.aidancbrady.aidanet.Message;
import com.aidancbrady.aidanet.client.AidaNetClient;
import com.aidancbrady.aidanet.server.AidaNetServer;

@DisplayName("Test a basic AidaNet implementation")
class NetworkTest {

    @Test
    @DisplayName("Test a simple client/server")
    void testSimpleClientServer() throws Exception {
        AidaNetServer server = new AidaNetServer(4000);
        AidaNetClient client = new AidaNetClient("localhost", 4000);

        client.getMessageRegistry().registerMessage(TestMessage.class, () -> new TestMessage());
        server.getMessageRegistry().syncFrom(client.getMessageRegistry());

        server.start();
        client.connect();
        client.sendToServer(new TestMessage(5, "client to server"));
        server.getConnections().forEach(c -> c.send(new TestMessage(8, "server to client")));
        Thread.sleep(5000);
        client.disconnect();
        server.stop();
    }

    public static class TestMessage implements Message {

        private int testInt;
        private String testString;

        public TestMessage() {}

        public TestMessage(int testInt, String testString) {
            this.testInt = testInt;
            this.testString = testString;
        }

        @Override
        public void read(DataInputStream buffer) throws IOException {
            testInt = buffer.readInt();
            testString = buffer.readUTF();
        }

        @Override
        public void write(DataOutputStream buffer) throws IOException {
            buffer.writeInt(testInt);
            buffer.writeUTF(testString);
        }

        @Override
        public void handle() {
            System.out.println("Received: " + testInt + " " + testString);
        }
    }
}
