package com.example.brucowheels;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;


public class MainActivity extends Activity implements OnItemClickListener, ConnectionInfoListener {

	private final int SERVER_PORT = 8988;
	private final HashMap<String, String> buddies = new HashMap<String, String>();
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private WifiP2pDnsSdServiceRequest serviceRequest;
	private WifiP2pDnsSdServiceInfo serviceInfo; // info sul service locale
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(); // lista con i peers scoperti
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>(); // lista dei peers selezionati per la connessione
    private List<String> peersName = new ArrayList<String>(); // lista con i nomi dei peers scoperti
    private List<InetAddress> clientIp = new ArrayList<InetAddress>(); // lista con gli IP dei clienti
    private ListView list;
    private Button bSearch, bConnect, bDisconnect, bSend;
    private int nSelectedDevices = 0; // numero di peers selezionati
    private boolean connected = false;
    private boolean owner = false;
    private String ownerIP;
    private boolean mRecording = false;
    private boolean mPlaying = false;

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
				nSelectedDevices = 0;
				peersConnect.clear();
				peers.clear();
				peersName.clear();
				list.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_checked, peersName));
				discoverService();
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
		// pulsante per disconnettersi
		bSend = (Button) this.findViewById(R.id.sender);
		bSend.setOnClickListener(new OnClickListener() {
			public void onClick (View v) {
				sendData();
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
	    startRegistration();
	}
	
	// stacco il broadcast receiver
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(mReceiver);
	    
	    if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
	}
	
	// arresta la ricerca e la visibilità del dispositivo
		@Override
		protected void onStop() {
		    super.onStop();
		    unregisterLocalService();
		    stopSearchServices();
		}
	
	// arresta la ricerca, la visibilità del dispositivo e uccide l'app
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    disconnectDevices();
	    stopSearchServices();
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
	}
    
    // metodo richiamato da mManager.requestConnectionInfo() per distinguere tra owner e client
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        // InetAddress from WifiP2pInfo struct.
        InetAddress groupOwnerAddress = info.groupOwnerAddress;
        connected = true;
        ownerIP = groupOwnerAddress.getHostAddress();
        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
        	Toast.makeText(MainActivity.this, "I'm the owner!!", Toast.LENGTH_SHORT).show();
        	owner = true;
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        	/*Intent serviceIntent = new Intent(MainActivity.this, OwnerSender.class);
            serviceIntent.setAction(OwnerSender.ACTION_SEND);
            serviceIntent.putExtra(OwnerSender.EXTRAS_CLIENT_ADDRESS,"192.168.49.10");
            serviceIntent.putExtra(OwnerSender.EXTRAS_CLIENT_PORT, 8988);
            startService(serviceIntent);*/
        	new OwnerReceiver(this).execute(); // owner riceve dai client sulla porta 8988
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        	/*Intent serviceIntent = new Intent(MainActivity.this, ClientSender.class);
            serviceIntent.setAction(ClientSender.ACTION_SEND);
            serviceIntent.putExtra(ClientSender.EXTRAS_GROUP_OWNER_ADDRESS,ownerIP);
            serviceIntent.putExtra(ClientSender.EXTRAS_GROUP_OWNER_PORT, 8988);
            startService(serviceIntent);*/
            //new ClientReceiver(this).execute(); // i client ricevono dall'owner sula porta 8989
        	Toast.makeText(MainActivity.this, "I'm a client...", Toast.LENGTH_SHORT).show();
        	Toast.makeText(MainActivity.this, "Server IP: " + groupOwnerAddress.getHostAddress(), Toast.LENGTH_SHORT).show();
        }
    }
    
    public void sendData() {
    	if(connected) {
	    	if(owner) {
	    		if(!mPlaying)
	    			mPlaying = true;
	    		else 
	    			mPlaying = false;
	    		onPlay(mPlaying);
	    	}
	    	else { // i client inviano all'owner sulla porta 8988
	    		if(!mRecording) {
	    			mRecording = true;
	    			onRecord(mRecording);
	    		}
	    		else {
	    			mRecording = false;
	    			onRecord(mRecording);
		    		Intent serviceIntent = new Intent(this, ClientSender.class);
		            serviceIntent.setAction(ClientSender.ACTION_SEND);
		            serviceIntent.putExtra(ClientSender.EXTRAS_GROUP_OWNER_ADDRESS, ownerIP);
		            serviceIntent.putExtra(ClientSender.EXTRAS_GROUP_OWNER_PORT, 8988);
		            serviceIntent.putExtra(ClientSender.AUDIO_FILE, mFileName);
		            startService(serviceIntent);
	    		}
	    	}
    	}
    	else
    		Toast.makeText(MainActivity.this, "Nessuna connessione!", Toast.LENGTH_SHORT).show();
    		
    }
	
    public void connectClient(InetAddress ip) {
    	if(!clientIp.contains(ip)) {
    		clientIp.add(ip);
            //Toast.makeText(MainActivity.this, "Client IP: " + ip.getHostAddress(), Toast.LENGTH_SHORT).show();
    	}
    }
	
	// blocca la ricerca di altri servizi
	private void stopSearchServices() {
		mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    }

		    @Override
		    public void onFailure(int reasonCode) {
		    }
		});
		mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    }

		    @Override
		    public void onFailure(int reasonCode) {
		    }
		});
		peersName.clear();
	}
		
	// cancella il servizio locale
	private void unregisterLocalService() {
		mManager.removeLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
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
	private void addPeer(WifiP2pDevice device) {
		if(!peers.contains(device)) {
			peers.add(device);
			peersName.add(device.deviceName);
			list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, peersName));
		}
		else
			System.out.println(device.deviceName + " già presente");
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
	
	private void startRegistration() {
        //  crea un record con alcune info da passare
        Map<String, String> record = new HashMap<String, String>();
        //record.put("listenport", String.valueOf(SERVER_PORT));

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("brucoWheels", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }
	
	private void discoverService() {
	    DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
	        @Override
	        /* Callback includes:
	         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
	         * record: TXT record dta as a map of key/value pairs.
	         * device: The device running the advertised service.
	         */

	        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
	        	// fai qualcosa con i dati del record
                System.out.println(fullDomain);
            }
	    };
        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {

            	System.out.println(instanceName);
            	System.out.println(registrationType);
                // Add to the custom adapter defined specifically for showing wifi devices.
                if(instanceName.equals("brucoWheels"))
                	addPeer(resourceType);
        
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
        
        mManager.discoverServices(mChannel, new ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                
            }
        });
	        
	}
	
	
	private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private MediaPlayer   mPlayer = null;

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    public MainActivity() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
    }

}
