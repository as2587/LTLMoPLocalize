package com.ASL.ltlmoplocalize;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.util.Log;

public class Client implements Runnable{
	//private final static String SERVER_ADDRESS = "192.168.1.6"; //IP of Server 
	//private final static int SERVER_PORT = 5005;
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
			Log.d("my", "Sent: " + message);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SERVER_PORT);
			
			socket.send(packet);
		} catch (Exception e){
			
			}
	}
	
	
}
