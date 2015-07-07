package com.example.brucowheels;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.AsyncTask;

public class Receiver extends AsyncTask<Void, Void, String> {
	
	private MainActivity mActivity;

    /**
     * @param context
     * @param statusText
     */
    public Receiver(MainActivity mActivity) {
    	this.mActivity = mActivity;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(8988);
            Socket client = serverSocket.accept(); // resta in ascolto per ricevere
            
            ObjectInputStream oIn = new ObjectInputStream(client.getInputStream());
            String oMessage = null;
			try {
				oMessage = (String) oIn.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
            serverSocket.close();
            System.out.println(oMessage);
            return "ciao miccio";
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
           // result dovrebbe essere la stringa restituita da doInBackground sopra...
        	System.out.println("Ho ricevuto la string: " + result);
        }

    }

    @Override
    protected void onPreExecute() {
        
    }

}
