package com.ASL.ltlmoplocalize;
/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import org.opencv.core.Point;

import android.util.Log;

//Primary Data structure to hold information pertaining to a Marker. 
public class Marker {
	private Point pt, anchor;
	private int id;
	private int live;
	
	public Marker(Point in_pt, int in_id) {
		pt = in_pt;
		id = in_id;
		live = 1;
		anchor = new Point(0,0);
	}

	public int getID() {
		return id;
	}

	public void setId(int inID) {
		id = inID;
	}

	public void setPoint(Point inpt) {
		pt = inpt;
	}
	
	public Point getAnchor(){
		return anchor;
	}
	
	public void setAnchor(Point in_anchor){
		anchor = in_anchor;
	}

	public int getLive(){
		return live;
	}
	public void resetLive(){
		live=0;
	}
	
	public void setLive(){
		live = 1;
	}
	public Point getPoint() {
		return pt;
	}

	public void increment(){
		if(id<4){
			id = id+1;
		}
		else{
			id = 0;
		}
	}
	
	public void incrementR(){
		if(id<2){
			id = id+1;
		}
		else{
			id = 0;
		}
	}
	
	public String toString() {
		return (pt.x + ":" + pt.y + "_"+ anchor.x + ":" + anchor.y);
	}

}
