package org.cornellASL.ltlmoplocalize;
/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.util.Log;

//Class that sends UDP Message
public class Client implements Runnable{

	private String message;
	private String SERVER_ADDRESS;
	private int SERVER_PORT;
	String TAG = "Client";
	
	public Client (String msg, String address, int port){
		message = msg;
		SERVER_ADDRESS = address;
		SERVER_PORT = port;
	}
	
	@Override
	public void run(){
		try{
			InetAddress serverAddr;
			serverAddr = InetAddress.getByName(SERVER_ADDRESS);
			DatagramSocket socket = new DatagramSocket();
		
			//Preparing the packet
			byte[] buf = (message).getBytes();
			Log.d(TAG, "Sent: " + message);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SERVER_PORT);
			
			socket.send(packet);
		} catch (Exception e){
			Log.d(TAG, "Package not sent");
			}
	}
	
	
}
