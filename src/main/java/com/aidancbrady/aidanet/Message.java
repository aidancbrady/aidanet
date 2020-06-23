package com.aidancbrady.aidanet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Message {

    public void read(DataInputStream buffer) throws IOException;

    public void write(DataOutputStream buffer) throws IOException;

    public void handle();
}
