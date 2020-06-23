package com.aidancbrady.aidanet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class Util {

    public static void clear(DataInputStream inputStream) throws IOException {
        while (inputStream.available() > 0) {
            inputStream.read();
        }
    }

    public static void writeUUID(DataOutputStream buffer, UUID uuid) throws IOException {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(DataInputStream buffer) throws IOException {
        return new UUID(buffer.readLong(), buffer.readLong());
    }
}
