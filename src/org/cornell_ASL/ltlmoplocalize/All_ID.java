package org.cornell_ASL.ltlmoplocalize;
/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;


import android.net.wifi.WifiManager;
import android.util.Log;


//Class used to hold all information relavent to MARKER and ROBOT tags. This class holds all marker/tag informations
//and contains methods used in processing/identifying markers frame-to-frame. This class also has a method to send marker information over UDP

public class All_ID {
	public List<Marker> allMarkers; //Array of all MARKER tags (should be length 4 when defined)
	public List<Marker> allRobots; // Array of all ROBOT tags (should be length 2 when defined)
	
	private double m_threshold, r_threshold; //Amount the center of each marker is allowed to move between each frame to be classified as the same marker
	private int defined; //Used to 
	public int[] pass_m = new int[4]; //If allMarkers is defined, this array holds the indeces of markers begining with id=1, etc.
	public int[] pass_r = new int[2]; //If allRobots is defined,    "       "           "			"			"			"
	int rep1_idx, UDP_PORT;
	String UDP_IP;

	private WifiManager wifi_manager; 

	public  All_ID (int thresh_m, int thresh_r, WifiManager in_wifi, String ip_address, int port_num){
		m_threshold = thresh_m;
		r_threshold = thresh_r;
		allMarkers = new ArrayList<Marker>();
		allRobots = new ArrayList<Marker>();
		wifi_manager = in_wifi;
		UDP_IP = ip_address;
		UDP_PORT = port_num;
	}

//Scan's marker tags to find if Point "in" is near another marker
//If found, return the marker's idx in allMarkers(0, 1, 2, 3, etc.)
//If not found, returns -1
	public int closeToMarker(Point in){
		int res = -1;
		for (int i=0; i <allMarkers.size(); i++){ 
			if (dist(in, allMarkers.get(i).getPoint())<m_threshold){
				res = i;
			}
		}
		return res;
	}
	
//Nearly identical to closeToMarker but scans robot tags, not marker tags. 
	public int closeToRobot(Point in){
		int res = -1;
		for (int i=0; i <allRobots.size(); i++){ 
			if (dist(in, allRobots.get(i).getPoint())<r_threshold){
				res = i;
			}
		}
		return res;
	}

//Returns distance between two points. 
	public double dist(Point p1, Point p2){
		double dist_val;
		dist_val = Math.sqrt(Math.pow((p2.y-p1.y),2)+Math.pow((p2.x-p1.x),2));
		return dist_val;
	}

//Finds marker which corresponds to the input markerPt. Increments ID of that marker	
	public void incrementID(Point markerPt){
		int temp = closeToMarker(markerPt);
		if (temp>=0){
			allMarkers.get(temp).increment();
			//Log.d("EDebug", "incremented id "+ allMarkers.get(temp).getID() + " , anchor: " +allMarkers.get(temp).getAnchor().x +", " + allMarkers.get(temp).getAnchor().y);
		}
		else{
			allMarkers.add(new Marker(markerPt, 1));
		}
	}

//Finds robot tag which corresponds to the input centerPt. Increments ID of that robot tag	
	public void incrementRb(Point centerPt){
		int temp = closeToRobot(centerPt);
		if (temp>=0){
			allRobots.get(temp).incrementR();
		}
		else{
			allRobots.add(new Marker(centerPt, 1));
		}

	}
		
//Checks if all markers are defined - there is exactly one Marker 1, one Marker 2, one Marker 3, and one Marker 4
	public int allMarkDefined(){
		defined = 0;
		for(int i=0; i<4;i++ ){
			int iD_reps=0;
			rep1_idx=-1;
			for(int k=0; k<allMarkers.size();k++){//For each marker, 
				if(allMarkers.get(k).getID()==(i+1)){
					iD_reps = iD_reps+1;
					rep1_idx = k;
				}
			}
			if((iD_reps==1)){
				pass_m[i]=rep1_idx;
			}
			else{
				pass_m[i]=-1;//else, marker 'i' is improperly defined
			}
		}
		if ((pass_m[0]>=0)&&(pass_m[1]>=0)&&(pass_m[2]>=0)&&(pass_m[3]>=0)){
			defined =1;
		}
		return defined;
	}
	
//Checks if all robots are defined - there is exactly one Robot 1, one robot 2
	public int allRobotDefined(){
		defined = 0;
		for(int i=0; i<2;i++ ){
			int iD_reps=0;//Number of times id "i" gets repeated
			rep1_idx=-1;
			for(int k=0; k<allRobots.size();k++){ 
				if(allRobots.get(k).getID()==(i+1)){
					iD_reps = iD_reps+1;
					rep1_idx = k;
				}
			}
			if((iD_reps==1)){
				Log.d("allid", "Id " + i + " at idx " + rep1_idx);
				pass_r[i]=rep1_idx;
			}
			else{
				pass_r[i]=-1;//else, marker 'i' is not properly defined
			}
		}
		if ((pass_r[0]>=0)&&(pass_r[1]>=0)){
			defined =1;
		}
		return defined;
	}
	
//Packs marker information and robot information into one string
	public String toString(){
		String sendstr="";
		for (int i =0; i<4; i++){
			sendstr+= allMarkers.get(pass_m[i]).toString()+"_";
		}
		for (int i = 0; i<2; i++){
			sendstr+= allRobots.get(pass_r[i]).toString()+"_";
			
		}

		return sendstr;
	}
	
	public void resetPass(int i){
		if (i==1){
			for(int j= 0; j<4; j++){
				pass_m[j] = -1;
			}
		}
		if (i==2){
			for(int j= 0; j<2; j++){
				pass_r[j] = -1;
			}
		}
	}
	
//Called every frame to send UDP packet; sends "nan_nan_nan" if alMarkers and allRobots are not defined. Else, sends toString()
	public void sendInfo(int mode){
		//Log.d("MyActivity", "Send Messages Now");
		boolean wifiEnabled = wifi_manager.isWifiEnabled();
		if (wifiEnabled){
			//Log.d("MyActivity", "Wifi has been enabled");
			if(mode==1){
				//Log.d("send", "send points");
				Thread t1 = new Thread(new Client(toString(), UDP_IP, UDP_PORT));
				t1.start();
			}
			else if (mode==2){
				//Log.d("send", "send Nan");
				Thread t1 = new Thread(new Client("nan_nan_nan", UDP_IP, UDP_PORT));
				t1.start();
			}
		}
		else{
			//Log.d("MyActivity", "Wifi is disabled....");
		}
	}

		
}
		
	
