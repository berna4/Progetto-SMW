package com.example.brucowheels;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListDevices extends Activity{
	
	private ArrayList<String> peers = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_layout);
		ListView list = (ListView) this.findViewById(R.id.list);
		Intent intent = getIntent();
		
		peers = intent.getStringArrayListExtra("deviceList");
		
		
		list.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, peers));
		
	}
	
	private void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		
	}
}
