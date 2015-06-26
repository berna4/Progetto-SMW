package com.example.brucowheels;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    private ArrayList<String> peersName = new ArrayList<String>();
    private ListView list;
    private Button bSearch;
    private Button bConnect;
    private Button bDisconnect;
    private int nSelectedDevices = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	    mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(this, getMainLooper(), null);
	    
	    
	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    //mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
	    
	    bSearch = (Button) this.findViewById(R.id.searcher);
		bSearch.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				list.setVisibility(ListView.INVISIBLE);
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
		
		bDisconnect = (Button) this.findViewById(R.id.disconnecter);
		bDisconnect.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				disconnectDevices();
				
				//bDisconnect.setVisibility(View.INVISIBLE);
			}
		});
		
		//searchDevices();
		
		list = (ListView) this.findViewById(R.id.list);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		 
		 
	}
	
	/* register the broadcast receiver with the intent values to be matched */
	@Override
	protected void onResume() {
	    super.onResume();
	    mReceiver = new WifiReceiver(mManager, mChannel, this);
	    registerReceiver(mReceiver, mIntentFilter);
	}
	
	/* unregister the broadcast receiver */
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    stopSearchDevices();
	    finish();
	}
	
	
	
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
	
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        // Out with the old, in with the new.
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if(peers.size() == 0)
        	peersName.add("No devices!" + ++nSelectedDevices);
        else
        	getDeviceName();
        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, peersName));
        list.setOnItemClickListener(this);
        // If an AdapterView is backed by this data, notify it
        // of the change.  For instance, if you have a ListView of available
        // peers, trigger an update.
        //((ListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
        	return;
        }
    }
    
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        // InetAddress from WifiP2pInfo struct.
        InetAddress groupOwnerAddress = info.groupOwnerAddress;

        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        	Toast.makeText(MainActivity.this, "I'm the owner!!", Toast.LENGTH_SHORT).show();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        	Toast.makeText(MainActivity.this, "I'm a client...", Toast.LENGTH_SHORT).show();
        }
    }
	
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
	
	private void stopSearchDevices() {
		mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    	Toast.makeText(MainActivity.this, "Search stopped", Toast.LENGTH_SHORT).show();
		    }

		    @Override
		    public void onFailure(int reasonCode) {
		    	Toast.makeText(MainActivity.this, "Fail stop search!", Toast.LENGTH_SHORT).show();
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
	                    		nSelectedDevices = 0;
	            				peersConnect.clear();
	            				peers.clear();
	            				peersName.clear();
	            				searchDevices();
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
