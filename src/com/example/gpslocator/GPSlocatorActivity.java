package com.example.gpslocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class GPSlocatorActivity extends Activity implements OnClickListener {
	
	ToggleButton toggleGPSon;
	ToggleButton toggleWifiEnable;
	
	TextView textViewDisplay;
	TextView textAccuracyDisplay;
	TextView textFilenameSuffix;
	TextView textTotalInDatabase;
	TextView textTotalFoundDuringInstance;
	TextView textTotalScanResults;
	
	EditText textFilename;
	
	CheckBox checkOpenNetworksOnly;
	
	Button saveLocation;
	Button showPoints;
	Button mergeFiles;
	Button filterByRectangle;
	Button buttonTest;
	
	//private MapView mapView;
	
	protected LocationManager locationManager;
	
	//public static List<Location> savedLocations;
	
	myLocationListener theLocationListener;
	public static double curLon;
	public static double curLat;
	
	public UserInputHandler theUserInputHandler;
	
	WifiManager wifi;
	static WifiLock wifiLock;
	public static BroadcastReceiver wifireceiver;
	
	private SessionInfo sessionStats;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        toggleGPSon = (ToggleButton) findViewById(R.id.toggleGPS);
        toggleGPSon.setOnClickListener(this);
        
        toggleWifiEnable = (ToggleButton) findViewById(R.id.toggleWifi);
        toggleWifiEnable.setOnClickListener(this);
        
        filterByRectangle = (Button) findViewById(R.id.buttonFilterByCoordinate);
        filterByRectangle.setOnClickListener(this);
        
        saveLocation = (Button) findViewById(R.id.buttonSave);
        saveLocation.setOnClickListener(this);
        saveLocation.setEnabled(false);
        showPoints = (Button) findViewById(R.id.buttonShowOnMaps);
        showPoints.setOnClickListener(this);
        showPoints.setEnabled(false);
        mergeFiles = (Button) findViewById(R.id.buttonMergeFiles);
        mergeFiles.setOnClickListener(this);
        buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonTest.setOnClickListener(this);
        buttonTest.setEnabled(false);
        
        checkOpenNetworksOnly = (CheckBox) findViewById(R.id.checkOpenOnly);
        checkOpenNetworksOnly.setOnClickListener(this);
        
        textViewDisplay = (TextView) findViewById(R.id.textLocation);
        
        textAccuracyDisplay = (TextView) findViewById(R.id.textAccuracy);
        
        textFilename = (EditText) findViewById(R.id.editTextFilename);
        
        textFilenameSuffix = (TextView) findViewById(R.id.textViewFilenameSuffix);
        
        textTotalInDatabase = (TextView) findViewById(R.id.textTotal);
        
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        //savedLocations = new ArrayList<Location>();
        
	     // Setup WiFi
	     wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	     
	     curLon = 0.0;
	     curLat = 0.0;
	     
	     theUserInputHandler = new UserInputHandler();
	     
	     textTotalFoundDuringInstance = (TextView) findViewById(R.id.textTotalPerScan);
	     
	     textTotalScanResults = (TextView) findViewById(R.id.textTotalScanResults);
	     
	     sessionStats = new SessionInfo();
		
    }
    
    
    public void disableNetworks() {
    	List<WifiConfiguration> configNetworkList = wifi.getConfiguredNetworks();
    	for (WifiConfiguration theNetwork : configNetworkList) {
    		wifi.disableNetwork(theNetwork.networkId);
    		
    	}
    }
    
    public void enableNetworks() {
    	List<WifiConfiguration> configNetworkList = wifi.getConfiguredNetworks();
    	for (WifiConfiguration theNetwork : configNetworkList) {
    		wifi.enableNetwork(theNetwork.networkId, false);
    	}
    }
    
    public class WiFiScanReceiver extends BroadcastReceiver {
    	//previousNetworkList = new List<NetworkList>;
    	
    	//GPSlocatorActivity gpsLocatorActivity;
    	
    	public WiFiScanReceiver() {
    		super();
    		//this.gpsLocatorActivity = gpsLocatorActivity;
    	}
    	
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			
			/*
			wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "scanning");
		    wifiLock.acquire();
		    */
			
			if (toggleWifiEnable.isChecked()) {
				
				//wifi.disconnect();
			    //wifi.createWifiLock(wifi.WIFI_MODE_SCAN_ONLY, "scanning");
				
				List<ScanResult> results = wifi.getScanResults();				
				
				List<NetworkLocation> savedNetworkLocations = new ArrayList<NetworkLocation>();
				savedNetworkLocations = readNetworkFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString());
				
				if (checkOpenNetworksOnly.isChecked()) {
					//savedNetworkLocations = readNetworkFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString());
					
					List<ScanResult> tempScanResults = new ArrayList<ScanResult>();
					for (ScanResult temp : results) {
						if (temp.capabilities.equals(""))
							tempScanResults.add(temp);
					}
					results = tempScanResults;
					
				} else {
					//savedNetworkLocations = readNetworkFile(textFilename.getText().toString());
				}
				
				String sNetworkList = "";
				
				String sNetworksFound = "";
				
				/*
				//dump ScanResult data into NetworkLocation type
				List<NetworkLocation> fromScanResults = new ArrayList<NetworkLocation>();
				fromScanResults = scanResultToNetworkLocationList(results);
				*/
				
				//merge the list, comparing new and old data
				savedNetworkLocations = mergeNewAndOldLists(savedNetworkLocations, results);
				
				sessionStats.newScanResultData(results);
				
				
				
				/*
				for(ScanResult temp : results) {
					//find item within the saved list
					boolean itemFound = false;
					
					for (NetworkLocation tempFind : savedNetworkLocations) {	
						if (tempFind.BSSID.equals(temp.BSSID)) {
							itemFound = true;
							
							//Update some data specific to this BSSID
							tempFind.SSID = temp.SSID;
							tempFind.capabilities = temp.capabilities;
									
							//Found previously saved result, check if signal is greater OR no GPS fix was located, then update GPS location and highest found level
							if ((tempFind.level < temp.level)  || (tempFind.lat == 0.0) || (tempFind.lon == 0.0)) {
								tempFind.level = temp.level;
								tempFind.lat = curLat;
								tempFind.lon = curLon;
							}
							
							break;
						}
					}
					
					if (!itemFound) {
						//add to savedNetworkLocations
						NetworkLocation newNetworkLocation = new NetworkLocation();
						newNetworkLocation.lat = curLat;
						newNetworkLocation.lon = curLon;
						newNetworkLocation.level = temp.level;
						newNetworkLocation.BSSID = temp.BSSID;
						newNetworkLocation.SSID = temp.SSID;
						newNetworkLocation.capabilities = temp.capabilities;
						
						savedNetworkLocations.add(newNetworkLocation);
					}
					
					sNetworksFound = temp.BSSID + "\t" + temp.SSID + "\t" + temp.level + "\n" + sNetworksFound;	//created only for display purposes via Toast()
				}
				
				*/
				
				for (ScanResult temp : results) {
					sNetworksFound = temp.SSID + " \t" + temp.level + "\n" + sNetworksFound;
				}
				
				//Display networks found every time the BroadcastReceiver receives a ScanResult<>
				Toast.makeText(getApplicationContext(), "Total found: " + results.size() + " (" + curLat + "," + curLon + ")\n" + sNetworksFound, Toast.LENGTH_LONG).show();
				
				
				
				
				/*
				if (checkOpenNetworksOnly.isChecked())
					writeFile(textFilename.getText().toString() + "_open_only.txt", sNetworkList);
				else
					writeFile(textFilename.getText().toString(), sNetworkList);
				*/
				
				//textTotalInDatabase.setText(savedNetworkLocations.size() + "");
				
				String fileName = textFilename.getText().toString();
			    //String totalInDatabase = Integer.toString(readNetworkFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString()).size());
			    textTotalInDatabase.setText(savedNetworkLocations.size() + " (" + fileName + ")");
				
				//Generate string of all locations that will be saved to the sdcard
				sNetworkList = formatForStorage(savedNetworkLocations);
				
				writeFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString(), sNetworkList);
				
			}
		}
    }
    
    public List<NetworkLocation> scanResultToNetworkLocationList(List<ScanResult> scanResults) {
    	List<NetworkLocation> networkLocationList = new ArrayList<NetworkLocation>();
    	NetworkLocation temp;
    	
		for (ScanResult tempScanResult : scanResults) {
			temp = new NetworkLocation();
			
			temp.BSSID = tempScanResult.BSSID;
			temp.capabilities = tempScanResult.capabilities;
			temp.lat = curLat;
			temp.lon = curLon;
			temp.level = tempScanResult.level;
			temp.SSID = tempScanResult.SSID;
			
			networkLocationList.add(temp);
		}
		
		return networkLocationList;
    }
    
    /*
    public List<NetworkLocation> mergeTwoLists(List<NetworkLocation> newData, List<NetworkLocation> savedData, boolean liveData) {    	
    	List<NetworkLocation> mergedLists = new ArrayList<NetworkLocation>();
    	
    	for(NetworkLocation temp : newData) {
			//find item within the saved list
			boolean itemFound = false;
			
			for (NetworkLocation tempFind : savedData) {	
				if (tempFind.BSSID.equals(temp.BSSID)) {
					itemFound = true;
					
					//Update some data specific to this BSSID
					tempFind.SSID = temp.SSID;
					tempFind.capabilities = temp.capabilities;
							
					//Found previously saved result, check if signal is greater OR no GPS fix was located, then update GPS location and highest found level
					if ((tempFind.level < temp.level)  || (tempFind.lat == 0.0) || (tempFind.lon == 0.0)) {
						tempFind.level = temp.level;
						tempFind.lat = curLat;
						tempFind.lon = curLon;
					} else {
						if (!liveData) {
							tempFind.lat = temp.lat;
							tempFind.lon = temp.lon;
						}
					}
					
					mergedLists.add(tempFind);
					
					break;
				}
			}
			
			if (!itemFound) {
				//add to savedNetworkLocations
				NetworkLocation newNetworkLocation = new NetworkLocation();
				if (liveData) {
					newNetworkLocation.lat = curLat;
					newNetworkLocation.lon = curLon;
				} else {
					newNetworkLocation.lat = temp.lat;
					newNetworkLocation.lon = temp.lon;
				}
				newNetworkLocation.level = temp.level;
				newNetworkLocation.BSSID = temp.BSSID;
				newNetworkLocation.SSID = temp.SSID;
				newNetworkLocation.capabilities = temp.capabilities;
				
				mergedLists.add(newNetworkLocation);
			}
			
			//sNetworksFound = temp.BSSID + "\t" + temp.SSID + "\t" + temp.level + "\n" + sNetworksFound;	//created only for display purposes via Toast()
		}
    	
    	return mergedLists;
    }
    
    */
    
    public List<NetworkLocation> mergeNewAndOldLists(List<NetworkLocation> prevData, List<ScanResult> newData) {
    	/*
    	 * This function is to be called when new ScanResult<> data is available,
    	 * and to be merged into the old NetworkLocation<> data, given the following algorithm
    	 */
    	
    	boolean matchFound;
    	NetworkLocation toBeAdded;
    	
    	for (ScanResult theNewData : newData) {
    		
    		matchFound = false;	//Needed assumption, until a match is found
    		
    		for (NetworkLocation thePrevData : prevData) {
    			if (thePrevData.BSSID.equals(theNewData.BSSID)) {	//Match found, update some stuffs, then check level
    				matchFound = true;
    				
    				thePrevData.capabilities = theNewData.capabilities;
    				thePrevData.SSID = theNewData.SSID;
    				thePrevData.DateTime = strGetCurrentDateTime();
    				
    				if (thePrevData.level < theNewData.level) {		//theNewData has better signal
    					
    					if ((curLon != 0.0) || (curLat != 0.0)) {
	    					thePrevData.level = theNewData.level;
	    					thePrevData.lat = curLat;
	    					thePrevData.lon = curLon;
    					}
    				}
    				
    				break;	//Exit for loop, no need to continue searching for matches
    			}
    		}
    		
    		if (!matchFound) {
    			toBeAdded = new NetworkLocation();
    			toBeAdded.BSSID = theNewData.BSSID;
    			toBeAdded.capabilities = theNewData.capabilities;
    			toBeAdded.lat = curLat;
    			toBeAdded.lon = curLon;
    			toBeAdded.level = theNewData.level;
    			toBeAdded.SSID = theNewData.SSID;
    			toBeAdded.DateTime = strGetCurrentDateTime();
    			
    			prevData.add(toBeAdded);
    		}
    		
    	}
    	
    	return prevData;
    	
    }
    
    public List<NetworkLocation> mergeNetworkLists(List<NetworkLocation> listA, List<NetworkLocation> listB) {
    	
    	List<NetworkLocation> mergedList = new ArrayList<NetworkLocation>();
    	NetworkLocation newNetworkLocation;
    	
    	boolean matchFound;
    	
    	for (NetworkLocation theListA : listA) {	//Find and save matches (by the best value from matches), and unique objects from listA
    		
    		matchFound = false;
    		
    		for (NetworkLocation theListB : listB) {
    			if (theListA.BSSID.equals(theListB.BSSID)) {
    				matchFound = true;
    				
    				newNetworkLocation = new NetworkLocation();
    				
    				if (theListA.level >= theListB.level) {	//theListA object has superior signal strength
    					newNetworkLocation = theListA;
    				} else {
    					newNetworkLocation = theListB;
    				}
    				
    				mergedList.add(newNetworkLocation);
    				
    				break;
    			}
    		}
    		
    		if (!matchFound) {
    			mergedList.add(theListA);
    		}
    	}
    	
    	for (NetworkLocation theListB : listB) {	//Do nothing with matches, but add unique objects from listB
    		
    		matchFound = false;
    		
    		for (NetworkLocation theListA : listA) {
    			if (theListA.BSSID.equals(theListB.BSSID)) {
    				matchFound = true;
    				
    				break;
    			}
    		}
    		
    		if (!matchFound) {
    			mergedList.add(theListB);
    		}
    	}
    	
    	return mergedList;
    }
    
    public String formatForStorage(List<NetworkLocation> theList) {
	    String sNetworkList = "";
    	for (NetworkLocation buff : theList) {
    		if (buff.capabilities.equals(""))
	    		buff.capabilities = "OPEN_NETWORK";
	    	
	    	sNetworkList = buff.BSSID + "\t"
					+ buff.SSID + "\t" 
					+ Integer.toString(buff.level) + "\t" 
					+ buff.lat + "\t" 
					+ buff.lon + "\t"
					+ buff.capabilities + "\t"
					+ buff.DateTime
					+ "\n" + sNetworkList;
    	}    		
	    	
    	return sNetworkList;
    }
    
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.toggleGPS) {
			if (toggleGPSon.isChecked()) {
				theLocationListener = new myLocationListener();
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, theLocationListener);
			} else {
				locationManager.removeUpdates(theLocationListener);
				textViewDisplay.setText("null");
				textAccuracyDisplay.setText("0.0");
			}
		}
		
		if (v.getId() == R.id.buttonSave) {
			//savedLocations.add(lastKnownLocation);
		}
		
		if (v.getId() == R.id.buttonShowOnMaps) {
			startActivityForResult(new Intent(v.getContext(), MapViewActivity.class), 0);
		}
		
		if (v.getId() == R.id.toggleWifi) {
			if (toggleWifiEnable.isChecked()) {
				// Register Broadcast Receiver
			    if (wifireceiver == null)
					wifireceiver = new WiFiScanReceiver();
			    
	
			    registerReceiver(wifireceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			     
			    /*
				wifi.disconnect();
			    wifi.createWifiLock(wifi.WIFI_MODE_SCAN_ONLY, "scanning");
			    wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "scanning");
			    wifiLock.acquire();
			     
			    */
			    
			    sessionStats.clearStats();
			    
			    //Show total in database statistic as soon as wifi button is pressed by doing this:
			    String fileName = textFilename.getText().toString();
			    //String totalInDatabase = Integer.toString(readNetworkFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString()).size());
			    textTotalInDatabase.setText(readNetworkFile(fileName + textFilenameSuffix.getText().toString()).size() + " (" + fileName + ")");
			    
			    
			    /*
			    wifi.disconnect();
			    disableNetworks();
			    wifi.startScan();
			    */
			    
				
				
				
				Toast.makeText(this, "wifi clicked", Toast.LENGTH_SHORT).show();
			} else {
				//wifi.createWifiLock(wifi.WIFI_MODE_FULL, "back to normal");
				//wifiLock.release();
				//enableNetworks();
				unregisterReceiver(wifireceiver);
				
				//Toast.makeText(this, "debug", Toast.LENGTH_SHORT).show();
			}
		}
		
		if (v.getId() == R.id.checkOpenOnly) {
			if (checkOpenNetworksOnly.isChecked())
				textFilenameSuffix.setText("_open_only.txt");
			else
				textFilenameSuffix.setText(".txt");
		}
		
		if (v.getId() == R.id.buttonMergeFiles) {
			
			Message msg = new Message();
			msg.what = 3;
			theUserInputHandler.sendMessage(msg);
			
			
		}
		
		if (v.getId() == R.id.buttonFilterByCoordinate) {
			Message msg = new Message();
			msg.what = 2;
			theUserInputHandler.sendMessage(msg);
		}
		
		if (v.getId() == R.id.buttonTest) {
			/*
			Date theDate;
			String strDateTime;
			
			theDate = new Date();
			strDateTime = theDate.getDate() + "/" + (theDate.getMonth()+1) + "/" + (theDate.getYear()+1901) + " " + theDate.getHours() + ":" + theDate.getMinutes() + ":" + theDate.getSeconds();
			
			Toast.makeText(getApplicationContext(), strDateTime, Toast.LENGTH_LONG).show();
			*/
			
			List<NetworkLocation> listToEdit = new ArrayList<NetworkLocation>();
			
			listToEdit = readNetworkFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString());
			
			for (NetworkLocation temp : listToEdit) {
				temp.DateTime = "N/A";
			}
			
			String strDatabase = formatForStorage(listToEdit);
			
			writeFile(textFilename.getText().toString() + textFilenameSuffix.getText().toString(), strDatabase);
			
		}
		
	}
	
	public String strGetCurrentDateTime() {
		Date theDate;
		String strDateTime;
		
		theDate = new Date();
		strDateTime = theDate.getDate() + "/" + (theDate.getMonth()+1) + "/" + (theDate.getYear()+1900) + " " + theDate.getHours() + ":" + theDate.getMinutes() + ":" + theDate.getSeconds();
		
		return strDateTime;
	}
	
	
	public void getUserTextInput(String title, String message, final int handleID, int theInputType) {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(title);
		alert.setMessage(message);
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setInputType(theInputType);
		
		input.setFocusable(true);
		input.setFocusableInTouchMode(true);
		input.requestFocus();
		
		//input.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM_);
		
		
		alert.setView(input);
		
		
		
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Message msg = new Message();
				msg.what = handleID;
				msg.obj = (String) input.getText().toString();
				theUserInputHandler.sendMessage(msg);
			  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Message msg = new Message();
				msg.what = -1;
				theUserInputHandler.sendMessage(msg);
			}
		});
		
		alert.show();
		
		
		
	}
	
	public class UserInputHandler extends Handler {
		String listA = "";
		String listB = "";
		
		double latFirst = 0.0;
		double lonFirst = 0.0;
		double latSecond = 0.0;
		double lonSecond = 0.0;
		String listNewFile = "";
		String listDatabase = "";
		
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case 1:	//Merge databases sequence
				if (!((String) msg.obj).equals((String) "")) {
					if (listA.equals((String) "")) {
						listA = (String) msg.obj;
						
						getUserTextInput("Enter list B", "Enter the filename of list B", 1, InputType.TYPE_CLASS_TEXT);
					} else {
						if (listB.equals((String) "")) {
							listB = (String) msg.obj;
							
							getUserTextInput("Enter new list", "Enter the filename of list to be saved as", 1, InputType.TYPE_CLASS_TEXT);
						} else {
							String newFile = (String) msg.obj;

							List<NetworkLocation> listALoaded = new ArrayList<NetworkLocation>();
							listALoaded = readNetworkFile(listA + ".txt");
							
							List<NetworkLocation> listBLoaded = new ArrayList<NetworkLocation>();
							listBLoaded = readNetworkFile(listB + ".txt");
							
							List<NetworkLocation> mergedList = new ArrayList<NetworkLocation>();
							mergedList = mergeNetworkLists(listALoaded, listBLoaded);
							
							String sNetworkList = formatForStorage(mergedList);
							
							writeFile(newFile + ".txt", sNetworkList);
							Toast.makeText(getApplicationContext(), listA + " contained " + listALoaded.size() + "\n"
									+ listB + " contained " + listBLoaded.size() + "\n"
									+ "Saved to " + newFile + " merged to contain " + mergedList.size(), Toast.LENGTH_LONG).show();
							
							//Done, reset data
							Message msgReset = new Message();
							msgReset.what = -1;
							this.sendMessage(msgReset);
						}
						
						
						
					}
				}
				
				break;
				
			case -1:	//User pressed cancel OR need to reset data, reset local values.
				listA = "";
				listB = "";
				
				latFirst = 0.0;
				lonFirst = 0.0;
				latSecond = 0.0;
				lonSecond = 0.0;
				listNewFile = "";
				listDatabase = "";
				
				break;
				
			case 2:
				//Start sequence for filtering by GPS coordinates
				
				getUserTextInput("Top left coordinate", "Enter decimal latitude", 4, InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_NUMBER_FLAG_SIGNED);
				
				break;
				
			case 4:	//GPS coordinate filter sequence
				
				if (latFirst == 0.0) {
					latFirst = Double.parseDouble((String) msg.obj); 
					getUserTextInput("Top left coordinate", "Enter decimal longitude", 4, InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_NUMBER_FLAG_SIGNED);
				} else {
					
					
					if (lonFirst == 0.0) {
						lonFirst = Double.parseDouble((String) msg.obj);
						//Toast.makeText(getApplicationContext(), lonFirst + "", Toast.LENGTH_SHORT).show();
						
						getUserTextInput("Bottom right coordinate", "Enter decimal latitude", 4, InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_NUMBER_FLAG_SIGNED);
					} else {
						
						
						if (latSecond == 0.0) {
							latSecond = Double.parseDouble((String) msg.obj); 
							getUserTextInput("Bottom right coordinate", "Enter decimal longitude", 4, InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_NUMBER_FLAG_SIGNED);
						} else {
							
							
							if (lonSecond == 0.0) {
								lonSecond = Double.parseDouble((String) msg.obj); 
								getUserTextInput("Input list", "Enter filename of list to load", 4, InputType.TYPE_CLASS_TEXT);
							} else {
								//We have the cooridnates!
								
								
								if (listDatabase.equals((String) "")) {
									listDatabase = (String) msg.obj;
									getUserTextInput("Output list", "Enter filename to output to", 4, InputType.TYPE_CLASS_TEXT);
								} else {
								
								
									listNewFile = (String) msg.obj;
									//We have coordinates and new file
									List<NetworkLocation> theList = readNetworkFile(listNewFile + ".txt");
									List<NetworkLocation> theOutput = new ArrayList<NetworkLocation>();
									
									theList = readNetworkFile(listDatabase + ".txt");
									Toast.makeText(getApplicationContext(), theList.size() + "\n" + latFirst + "\n" + lonFirst + "\n" + latSecond + "\n" + lonSecond, Toast.LENGTH_LONG).show();
									
									for (NetworkLocation curElement : theList) {
										if ((curElement.lat <= latFirst) && (curElement.lat >= latSecond)) {	//Matches latitudes, proceed to longitudes
											if ((curElement.lon <= lonSecond) && (curElement.lon >= lonFirst)) {
												theOutput.add(curElement);
											}
										}
									}
									
									Toast.makeText(getApplicationContext(), "Found " + theOutput.size() + " matches.", Toast.LENGTH_LONG).show();
									
									writeFile(listNewFile + ".txt", formatForStorage(theOutput));
									
									//Done, reset data
									Message msgReset = new Message();
									msgReset.what = -1;
									this.sendMessage(msgReset);
								}
							}
						}
					}
				}
				
				break;
				
			case 3: //Start sequence for getting file names to merge two files into one			
				//Toast.makeText(getApplicationContext(), "button pressed", Toast.LENGTH_SHORT).show();
				
				getUserTextInput("Enter list A", "Enter filename for list A", 1, InputType.TYPE_CLASS_TEXT);
				
				break;
				
			default:
				
			
			}
		}
	}
	
	private class myLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (location.getLatitude() != 0.0)
				curLat = location.getLatitude();
			
			if (location.getLongitude() != 0.0)
				curLon = location.getLongitude();
			
			textAccuracyDisplay.setText(Float.toString(location.getAccuracy()));
			
			textViewDisplay.setText(location.getLatitude() + ", " + location.getLongitude());
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			textViewDisplay.setText("GPS disabled");
			
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			textViewDisplay.setText("enabled, waiting for fix");
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private List<NetworkLocation> readNetworkFile(String sFilename) {
		File sRootPath = Environment.getExternalStorageDirectory();
		File theFile;
		theFile = new File(sRootPath.getAbsolutePath(), sFilename);
		
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(theFile));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String sLine = "";
		String[] sParsedArgs;
		
		List<NetworkLocation> networks = new ArrayList<NetworkLocation>();

		NetworkLocation temp;
		
		if (theFile.exists()) {
			try {
				while ((sLine = bufferedReader.readLine()) != null) {
					
					//sLine = dis.readLine();
					sParsedArgs = sLine.split("\t");
					
					temp = new NetworkLocation();
					
					temp.BSSID = sParsedArgs[0];
					temp.SSID = sParsedArgs[1];
					temp.level = Integer.parseInt(sParsedArgs[2]);
					if (sParsedArgs[3] == null) 
						sParsedArgs[3] = "0.0";
					if (sParsedArgs[4] == null) 
						sParsedArgs[4] = "0.0";
					temp.lat = Double.valueOf(sParsedArgs[3]);
					temp.lon = Double.valueOf(sParsedArgs[4]);
					
					try {
						temp.capabilities = sParsedArgs[5];
						} catch (Exception e) {
							temp.capabilities = "";
						}
					
					try {
						temp.DateTime = sParsedArgs[6];
					} catch (Exception a) {
						temp.DateTime = "N/A";
					}
					
					networks.add(temp);
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
	            //Close the BufferedReader
	            try {
	                if (bufferedReader != null)
	                    bufferedReader.close();
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
		}
			
		return networks;
			
	}
	
   protected class MyLocationOverlay extends com.google.android.maps.Overlay {
	   
   }

	
	private void writeFile(String sLocation, String sBody) {
		File sRootPath = Environment.getExternalStorageDirectory();
		File theFile;
		FileOutputStream out = null;
		
		try {
			theFile = new File(sRootPath.getAbsolutePath(), sLocation);
			out = new FileOutputStream(theFile);
			out.write(sBody.getBytes());
			out.flush();
			out.close();
		}
			catch (Exception e)
			{
				Toast.makeText(this, "IO error occured: " + e, Toast.LENGTH_LONG).show();
			}
		
	}
	
	class SessionInfo {
		
		List<String> prevFound;
		int totalScans;
		
		public SessionInfo() {
			prevFound = new ArrayList<String>();
		}
		
		public void newScanResultData(List<ScanResult> newData) {
			for (ScanResult tempNewData : newData) {
				boolean foundMatch = false;
				for (String tempPrevData : prevFound) {
					if (tempPrevData.equals(tempNewData.BSSID)) {
						foundMatch = true;
						break;
					}
					
				}
				
				if (!foundMatch) {
					prevFound.add(tempNewData.BSSID);
				}
			}
			++totalScans;
			
			updateDisplay();
				
		}
		
		private void updateDisplay() {
			textTotalFoundDuringInstance.setText(Integer.toString(prevFound.size()));
			textTotalScanResults.setText(Integer.toString(totalScans));
		}
		
		public void clearStats() {
			prevFound.clear();
			totalScans = 0;
			
			updateDisplay();
		}
	}
}



class NetworkLocation {
	public String SSID;
	public String BSSID;
	public int level;
	
	public double lat;
	public double lon;
	
	public String DateTime;
	
	public String capabilities;
	
	public NetworkLocation() {
		SSID = "";
		BSSID = "";
		level = 0;
		lat = 0.0;
		lon = 0.0;
		capabilities = "";
		DateTime = "N/A";
	}

}