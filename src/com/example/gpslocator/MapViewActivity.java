package com.example.gpslocator;

import com.google.android.maps.MapActivity;

import android.os.Bundle;

public class MapViewActivity extends MapActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapviewlayout);
		
		
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
}