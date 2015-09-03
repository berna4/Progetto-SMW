package com.example.brucowheels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.AsyncTask;
import android.os.Environment;

public class OwnerReceiver extends AsyncTask<Void, Void, String> {
	
	private MainActivity mActivity;

    /**
     * @param context
     * @param statusText
     */
    public OwnerReceiver(MainActivity mActivity) {
    	this.mActivity = mActivity;
    }

    @Override
    protected String doInBackground(Void... params) {
    	System.out.println("Creato thread dell'owner receiver");
        try {
            ServerSocket serverSocket = new ServerSocket(8988);
            Socket client = serverSocket.accept(); // resta in ascolto per ricevere
            mActivity.connectClient(client.getInetAddress()); // l'owner ricava l'ip del client
            ObjectInputStream oIn = new ObjectInputStream(client.getInputStream());
            try {
     			FileOutputStream audio = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.3gp");
     			ObjectOutputStream out = new ObjectOutputStream(audio);
     			out.writeObject((File) oIn.readObject());
     			out.close();
     			audio.close();
    		}
    		catch (IOException e) {
     			e.printStackTrace();
     		} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
            serverSocket.close();
            System.out.println(" My IP: " + client.getInetAddress().getHostAddress());
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
        	
        	new OwnerReceiver(mActivity).execute();
        }

    }

    @Override
    protected void onPreExecute() {
        
    }

}
