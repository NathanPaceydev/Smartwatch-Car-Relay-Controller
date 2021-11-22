package com.controlanything.NCDTCPRelay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class DiscoveryUtility extends IntentService
{
	
	private int result = Activity.RESULT_CANCELED;
	Bundle extras;
	static Messenger messenger;
	public static Message msg;
	String stringToReturn;
	DatagramSocket UDPSocket;
	Boolean shouldContinue = true;
	String Mac;
	int dbm;
	int port;

	public DiscoveryUtility() {
		super("DiscoveryUtility");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onHandleIntent(Intent intent) {
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
			UDPSocket = new DatagramSocket(55555);
			UDPSocket.setBroadcast(true);
			byte[] buf = new byte[120];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			while(shouldContinue){
				try {
					UDPSocket.receive(packet);
					//Get board's IP address and port number
					InetAddress senderAddress = packet.getAddress();
					port = packet.getPort();
					String Sender = senderAddress.getHostAddress();
					dbm = buf[7];
					
					UDPSocket.close();
					shouldContinue = false;
					
					pingIP(Sender);
					

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void pingIP(String address)
	{
		try {
			InetAddress ia = InetAddress.getByName(address);
			try {
				if (ia.isReachable(3000))
				{
					getMacFromArpCache(address);
				}else{
					System.out.println("could not ping address "+address);
					msg.arg1 = Activity.RESULT_CANCELED;
					try {
						messenger.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void getMacFromArpCache(String ip) {
	    if (ip == null)
	    {
	    	System.out.println("IP sent to getMacFromArpCache is null");
	    }

	    BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader("/proc/net/arp"));
	        String line;
	        while ((line = br.readLine()) != null) {
	            String[] splitted = line.split(" +");
	            if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
	                // Basic sanity check
	            	
	                String mac = splitted[3];
	                if (mac.matches("..:..:..:..:..:..")) {
	                	
	                	String macPlusIP = (splitted[3]+ "~" + splitted[0]+"~"+port+"~"+dbm);
	                	sendMacBack(macPlusIP);
	                } else {
	          

	                    System.out.println("Something does not add up in getMacFromArpCache method");
	                }
	            }else{

	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            br.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    
	}
	
	public static void sendMacBack(String Mac)
	{
		msg.arg1 = Activity.RESULT_OK;
		msg.obj = Mac;
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
