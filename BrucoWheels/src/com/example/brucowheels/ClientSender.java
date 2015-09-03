package com.example.brucowheels;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.IntentService;
import android.content.Intent;

public class ClientSender extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND = "SEND"; // azione
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host"; // IP dell'owner
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port"; // porta di invio
    public static final String AUDIO_FILE = "audio_data"; // file audio da inviare
    

    public ClientSender(String name) {
        super(name);
    }

    public ClientSender() {
        super("Sender");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	System.out.println("Io client sto cercando di inviare il file audio");
        if (intent.getAction().equals(ACTION_SEND)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            String audioPath = intent.getExtras().getString(AUDIO_FILE);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            
            try { // connessione dei socket e invio dei dati
            	FileInputStream fileIn = new FileInputStream(audioPath);
            	ObjectInputStream in = new ObjectInputStream(fileIn);
     			File audio = (File) in.readObject();
     			in.close();
     			fileIn.close();
            	
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                OutputStream stream = socket.getOutputStream();
                ObjectOutputStream oStream = new ObjectOutputStream(stream);
                System.out.println("Io client sto cercando di inviare hi owner...");
                oStream.writeObject(audio);
               
            } catch (IOException e) {
            	e.printStackTrace();
            } catch (ClassNotFoundException e) {
				e.printStackTrace();
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
