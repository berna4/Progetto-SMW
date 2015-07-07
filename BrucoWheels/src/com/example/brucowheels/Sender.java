package com.example.brucowheels;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.IntentService;
import android.content.Intent;

public class Sender extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND = "SEND"; // azione
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host"; // IP dell'owner
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port"; // porta di invio

    public Sender(String name) {
        super(name);
    }

    public Sender() {
        super("Sender");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_SEND)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            try { // connessione dei socket e invio dei dati
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                OutputStream stream = socket.getOutputStream();
                ObjectOutputStream oStream = new ObjectOutputStream(stream);
                oStream.writeObject(new String("Hi dude!"));
               
            } catch (IOException e) {
            
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}
