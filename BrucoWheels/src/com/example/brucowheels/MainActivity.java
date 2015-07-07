package com.example.brucowheels;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;


public class MainActivity extends Activity implements OnItemClickListener, PeerListListener, ConnectionInfoListener {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(); // lista con i peers scoperti
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>(); // lista dei peers selezionati per la connessione
    private ArrayList<String> peersName = new ArrayList<String>(); // lista con i nomi dei peers scoperti
    private ListView list;
    private Button bSearch;
    private Button bConnect;
    private Button bDisconnect;
    private int nSelectedDevices = 0; // numero di peers selezionati

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// inizializzo il manager wifi, il canale e il filtro degli intent per il broadcast receiver
	    mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(this, getMainLooper(), null);
	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
	    
	    // pulsante per la ricerca di peers; serve anche per rendere visibile il dispositivo verso gli altri
	    bSearch = (Button) this.findViewById(R.id.searcher);
		bSearch.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				//list.setVisibility(ListView.INVISIBLE);
				//bConnect.setVisibility(View.INVISIBLE);
				//bDisconnect.setVisibility(View.INVISIBLE);
				nSelectedDevices = 0;
				peersConnect.clear();
				peers.clear();
				peersName.clear();
				searchDevices();	
				list.setVisibility(ListView.VISIBLE);
			}
		});
		// pulsante per connettersi ai peers selezionati nella lista
		bConnect = (Button) this.findViewById(R.id.connecter);
		bConnect.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				//bDisconnect.setVisibility(View.VISIBLE);
				connectDevices();		
				//bConnect.setVisibility(View.INVISIBLE);
				nSelectedDevices = 0;
				peersConnect.clear();
			}
		});
		// pulsante per disconnettersi
		bDisconnect = (Button) this.findViewById(R.id.disconnecter);
		bDisconnect.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				disconnectDevices();
				peersName.clear();
				list.setVisibility(View.INVISIBLE);
			}
		});
		// inizializzo la lista
		list = (ListView) this.findViewById(R.id.list);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		list.setOnItemClickListener(this);
		 
	}
	
	// registro il broadcast receiver
	@Override
	protected void onResume() {
	    super.onResume();
	    mReceiver = new WifiReceiver(mManager, mChannel, this);
	    registerReceiver(mReceiver, mIntentFilter);
	}
	
	// stacco il broadcast receiver
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	}
	
	// arresta la ricerca, la visibilità del dispositivo e uccide l'app
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    stopSearchDevices();
	    finish();
	}
	
	
	// aggiunge o toglie peers ai quali connettersi
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		CheckedTextView item = (CheckedTextView) v;
		if(item.isChecked()) {
			nSelectedDevices++;
			peersConnect.add(peers.get(position));
		}
		else {
			nSelectedDevices--;
			peersConnect.remove(peers.get(position));
		}
		if(nSelectedDevices == 1) {
			//bConnect.setVisibility(View.VISIBLE);
			//bDisconnect.setVisibility(View.VISIBLE);
		}
		else if(nSelectedDevices == 0) {
			//bConnect.setVisibility(View.INVISIBLE);
			//bDisconnect.setVisibility(View.INVISIBLE);
		}
	}
	
	// metodo richiamato da mManager.requestPeers() ogni volta che il receiver cattura l'intent onPeersChanged
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        // elimino i vecchi peers e aggiungo i nuovi
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if(peers.size() != 0) {
        	getDeviceName();
        	list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, peersName));
        }
    }
    
    // metodo richiamato da mManager.requestConnectionInfo() per distinguere tra owner e client
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        // InetAddress from WifiP2pInfo struct.
        InetAddress groupOwnerAddress = info.groupOwnerAddress;

        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        	new Receiver(this).execute();
        	Toast.makeText(MainActivity.this, "I'm the owner!!", Toast.LENGTH_SHORT).show();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        	Intent serviceIntent = new Intent(this, Sender.class);
            serviceIntent.setAction(Sender.ACTION_SEND);
            serviceIntent.putExtra(Sender.EXTRAS_GROUP_OWNER_ADDRESS,groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(Sender.EXTRAS_GROUP_OWNER_PORT, 8988);
            startService(serviceIntent);
        	Toast.makeText(MainActivity.this, "I'm a client...", Toast.LENGTH_SHORT).show();
        	Toast.makeText(MainActivity.this, "Server IP: " + groupOwnerAddress.getHostAddress(), Toast.LENGTH_SHORT).show();
        }
    }
	
    // metodo che richiama mManager.discoverPeers() per ricercare dispositivi e rendersi visibile
	private void searchDevices() {
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    	//Toast.makeText(MainActivity.this, "Inizio ricerca...", Toast.LENGTH_SHORT).show();
		    }

		    @Override
		    public void onFailure(int reasonCode) {
		    	//Toast.makeText(MainActivity.this, "Ricerca fallita!", Toast.LENGTH_SHORT).show();
		    }
		});
		
	}
	
	// blocca la ricerca di altri dispositivi
	private void stopSearchDevices() {
		mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    }

		    @Override
		    public void onFailure(int reasonCode) {
		    }
		});
	}
	
	private void connectDevices() {
		for(int i = 0; i < peersConnect.size(); i++) {
		    
	        // Picking the first device found on the network. 
	        WifiP2pDevice device = peersConnect.get(i);

	        WifiP2pConfig config = new WifiP2pConfig();
	        config.deviceAddress = device.deviceAddress;
	        config.wps.setup = WpsInfo.PBC;
	        config.groupOwnerIntent = 15; // dovrebbe impostare come group owner quello che preme connect ma nn funzia tanto

	        mManager.connect(mChannel, config, new ActionListener() {

	            @Override
	            public void onSuccess() {
	                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
	            	//Toast.makeText(MainActivity.this, "Connection requested...", Toast.LENGTH_SHORT).show();
	            }

	            @Override
	            public void onFailure(int reason) {
	                //Toast.makeText(MainActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
	            }
	        });
		}
	}
	
	public void disconnectDevices() {
	    if (mManager != null && mChannel != null) {
	        mManager.requestGroupInfo(mChannel, new GroupInfoListener() {
	            @Override
	            public void onGroupInfoAvailable(WifiP2pGroup group) {
	                if (group != null && mManager != null && mChannel != null /*&& group.isGroupOwner()*/) {
	                    mManager.removeGroup(mChannel, new ActionListener() {
	                    	@Override
	                        public void onSuccess() {
	                        }

	                        @Override
	                        public void onFailure(int reason) {
	                         
	                        }
	                    });
	                }
	            }
	        });
	    }
	}
	
	// creo una lista con i nomi dei dispositivi trovati per visualizzarla
	private void getDeviceName() {
		int i = 0;
		peersName.clear();
		while(i < peers.size()) {
			peersName.add(peers.get(i).deviceName);
			i++;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
