package com.example.brucowheels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.widget.Toast;

public class WifiReceiver extends BroadcastReceiver {
	
	private WifiP2pManager mManager;
    private Channel mChannel;
    private MainActivity mActivity;

    // inizializzo il broadcast receiver
    public WifiReceiver(WifiP2pManager manager, Channel channel, MainActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    // metodo che cattura gli intent filtrati
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) { // verifica se il wifi è attivo oppure no
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            	//mActivity.setIsWifiP2pEnabled(true);
            } else {
                //mActivity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) { // verifica se il dispositivo sta ricercando
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
        	if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
        		System.out.println("Sto cercando");
            } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
            	// ho smesso di cercare
            	System.out.println("Ho smessi di cercando");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) { // la lista dei dispositivi è cambiata
            if (mManager != null) {
            	System.out.println("lista cambiata");
                mManager.requestPeers(mChannel, mActivity);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) { // lo stato della connessione è cambiato
        	// parte all'avvio dell'app, quando accetto o rifiuto una connessione, 
        	// quando spengo e riaccendo lo schermo e quando mi disconnetto
        	if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) { // il dispositivo si è connesso
                mManager.requestConnectionInfo(mChannel, mActivity); // richiedo informazioni sulla connessione
                System.out.println("Connesso");
            }
            else {
            	System.out.println("Connessione cambiata, NON connesso.");
            }
            
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) { // i dettagli del dispositivo sono cambiati
        }
    }
}
