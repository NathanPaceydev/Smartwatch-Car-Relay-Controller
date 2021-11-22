package com.controlanything.NCDTCPRelay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Enumeration;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class WebiDiscoveryService extends IntentService{

	private int result = Activity.RESULT_CANCELED;
	Bundle extras;
	static Messenger messenger;
	public static Message msg;
	String stringToReturn;
	DatagramSocket UDPSocket;
	Boolean shouldContinue = true;
	String Mac;
	String ip;
	int port = 2101;
	String dBm = "Ethernet";

	public WebiDiscoveryService() {
		super("WebiDiscoveryService");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onHandleIntent(Intent intent) {
		System.out.println("WEB-i Discovery Service started");
		shouldContinue = true;

		//Get messenger object from calling activity and setup message object
		extras = intent.getExtras();
		if (extras != null){
			messenger = (Messenger) extras.get("MESSENGER");
			msg = Message.obtain();
		}
		getIP();
		
	}
	
	
	
	public void getIP()
	{
		try {
			UDPSocket = new DatagramSocket(13000);
			byte[] buf = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			while(shouldContinue){
				try {
					UDPSocket.receive(packet);
					InetAddress senderAddress = packet.getAddress();
					String Sender = senderAddress.getHostAddress();
					ip = Sender;
					System.out.println("Device found in WEB-iDiscovery Service, IP = "+ip);
//					port = packet.getPort();
					
					UDPSocket.close();
					
					
					String info = new String(packet.getData());
					System.out.println(info);
					
					String[] infoSplit = info.split(",");
					
					String first = String.valueOf(infoSplit[1].charAt(0));
					String second = String.valueOf(infoSplit[1].charAt(1));
					
					String M = (first+second+":"+infoSplit[1].charAt(2)+infoSplit[1].charAt(3)+":"+infoSplit[1].charAt(4)+infoSplit[1].charAt(5)+":"+infoSplit[1].charAt(6)+infoSplit[1].charAt(7)+":"+infoSplit[1].charAt(8)+infoSplit[1].charAt(9)+":"+infoSplit[1].charAt(10)+infoSplit[1].charAt(11));

					if(info.contains("WebiB")){
						dBm = "WebiB";
					}
					
					if(info.contains("WiNet")){
						dBm = "WiNet";
					}
					
					if(info.contains("Webi")){
						dBm = "WEB-i";
					}
					
					String stringToSend= (M+"~"+infoSplit[0])+"~"+port+"~"+dBm;
					System.out.println("Discovery returning: "+stringToSend);
					sendMacBack(stringToSend);
					shouldContinue = false;
					

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
//	                System.out.println("Host Name = "+inetAddress.getHostName().toString() +"host Address = " + inetAddress.getHostAddress().toString());
	                
	                if (inetAddress.isSiteLocalAddress()) {
	                	if(!inetAddress.isLoopbackAddress()){
	                	}
//	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        System.out.println("Socket Exception ex");
	    }
	    return null;
	}
	
	public static void sendMacBack(String Mac)
	{
		msg.arg1 = Activity.RESULT_OK;
		msg.obj = Mac;
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


}
