package com.ASL.ltlmoplocalize;
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
	private int counter = 0;
	
	public Client (String msg, String address, int port){
		message = msg;
		SERVER_ADDRESS = address;
		SERVER_PORT = port;
	}
	
	@Override
	public void run(){
		try{
			counter +=1;
			InetAddress serverAddr;
			serverAddr = InetAddress.getByName(SERVER_ADDRESS);
			DatagramSocket socket = new DatagramSocket();
		
			//Preparing the packet
			byte[] buf = (message).getBytes();
			Log.d("udp", "Sent: " + message);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SERVER_PORT);
			
			socket.send(packet);
		} catch (Exception e){
			
			}
	}
	
	
}
