package com.controlanything.NCDTCPRelay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class FindDevice extends IntentService
{
	Bundle extras;
	static Messenger messenger;
	public static Message msg;
	DatagramSocket UDPSocket;
	String sentMac;
	boolean continueListening;
	long startTime;
	AsyncTask<Void, Void, Void> reciever;
	String deviceSettings;
	ControlPanel cPanel;
	boolean webi = false;
	boolean wifiModule = false;
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	ArrayList<String> discoveredDevices;

	public FindDevice() {
		super("FindDevice");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		extras = intent.getExtras();
		if (extras != null)
		{
			messenger = (Messenger) extras.get("MESSENGER");
			if(msg != null){
				msg = null;
			}
			msg = Message.obtain();
			System.out.println(extras.getString("LOCATION"));
			
			
			if (extras.getString("LOCATION").equals("remote")){
				System.out.println("connecting to signalswitch");
				sentMac = extras.getString("MAC");
				System.out.println(sentMac);
				
				signalSwitchTest(sentMac);
			}else{
				sentMac = extras.getString("MAC");
				System.out.println("Scanning Local Network for Device: "+sentMac);
				if(sentMac.startsWith("00:06:66")){
					//Scan for WiFi
					wifiModule = true;
					waitForIP(55555);
				}else{
					//Scan for Ethernet
					wifiModule = false;
					waitForIP(13000);
				}
				
				
			}
			
		}
	}
	
	public void signalSwitchTest(String mac)
	{
		System.out.println("Connecting to Signalswitch");
		
		deviceSettings = cPanel.getStoredString(sentMac);
		System.out.println("deviceSettings in FindDevice: "+deviceSettings);
		String[] settingsSplit = deviceSettings.split(";");
		if(settingsSplit.length > 12){
			if(settingsSplit[12].equalsIgnoreCase("true")){
				webi = true;
				System.out.println("Device is WEBi");
			}else{
				System.out.println("no setting for webi");
			}
		}else{
			System.out.println("no setting for WEBi");
		}

		String[] macSplit = mac.split(":");
		
		String signalSwitchFormatedMac = null;
		
		for(int i = 0; i < macSplit.length; i++){
			if( signalSwitchFormatedMac == null){
				signalSwitchFormatedMac = macSplit[i];
			}else{
				signalSwitchFormatedMac = signalSwitchFormatedMac + macSplit[i];
			}
		}
			System.out.println(signalSwitchFormatedMac);

			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
			HttpConnectionParams.setSoTimeout(httpParameters, 3000);
			
			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			
			HttpResponse response;
			String responseString = null;

			try {
				System.out.println("sending request to signalswitch");
				HttpGet httpget = new HttpGet("http://link.signalswitch.com/getip.aspx?mac=" + signalSwitchFormatedMac);
				response = httpclient.execute(httpget);
				StatusLine statusLine = response.getStatusLine();
				System.out.println(statusLine.getStatusCode());
				if(statusLine.getStatusCode() == HttpStatus.SC_OK)
				{
					
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					
					out.close();
					responseString = out.toString();
					
					if(responseString.contains("not found"))
					{
						System.out.println("signalSwitch said: "+responseString);
						sendIPBack("device not available");	
						return;
						
					}else{
//						System.out.println(responseString);
						String[] responseStringSplit = responseString.split("\n");
						String[] ipSplit = responseStringSplit[0].split(":");
						if (ipSplit.length == 2){
							System.out.println(ipSplit[0]);
							
							
							System.out.println("Find Device says device is WEBI? "+webi);
							if(webi){
								ipSplit[1] = settingsSplit[2];
							}
							
							sendIPBack(ipSplit[0]+";"+ipSplit[1]);
							return;
						}
					}
					
					

				}else{
					System.out.println(response.getStatusLine().toString());
					sendIPBack("browser error" + "/n" + response.getStatusLine().toString());
					response.getEntity().getContent().close();
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				sendIPBack("device not available");
			} catch (IOException e) {
				e.printStackTrace();
				sendIPBack("device not available");
			}
	}
		
	public void waitForIP(int port){
		discoveredDevices = new ArrayList<String>();
		continueListening = true;
		startTime = System.currentTimeMillis();
				
		try {
			//Create DatagramSocket object and set it to broadcast
			UDPSocket = new DatagramSocket(port, InetAddress.getByName("255.255.255.255"));
			
			UDPSocket.setBroadcast(true);
			
			//Create byte array to store information received from device broadcast.
			byte[] buf = new byte[512];
			//Create DatagramPacket for holding information sent by the broadcast device.
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			//Set timeout of socket so we dont end up waiting indefinitely.
			UDPSocket.setSoTimeout(12000);
			
			System.out.println("Starting Scan");
			
			while(System.currentTimeMillis() < startTime + 12000  && continueListening == true){
				UDPSocket.receive(packet);
				System.out.println("Packet Received");
				
				if(wifiModule){
					System.out.println("Length of received packet: "+packet.getLength());
					byte[] macBytes = new byte[6];
					for(int i = 0; i < macBytes.length; i++){
						macBytes[i] = packet.getData()[i+110];
					}
					System.out.println(bytesToMAC(macBytes));
					if(sentMac.equalsIgnoreCase(bytesToMAC(macBytes))){
						upDateStoredIP(packet.getAddress().getHostAddress());
						sendIPBack(packet.getAddress().getHostAddress());
						return;
					}
				}else{
					String[] packetStringSplit = new String(buf, 0, packet.getLength()).split(",");
					char[] c = packetStringSplit[1].toCharArray();
					StringBuilder sBuilder = new StringBuilder();
					for(int i = 0; i < c.length; i++){
						sBuilder.append(c[i]);
						if(i == 1 || i == 3 || i == 5 || i == 7 || i == 9){
				    		sBuilder.append(":");
				    	}
					}
					String discoveredMac = sBuilder.toString();
					if(discoveredMac.equalsIgnoreCase(sentMac)){
						upDateStoredIP(packet.getAddress().getHostAddress());
						sendIPBack(packet.getAddress().getHostAddress());
						return;
					}
				}
				
			}
			System.out.println("DONE SCANNING");
			sendIPBack("device not found local");
			
		} catch (SocketException e) {
			sendIPBack("device not found local");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			sendIPBack("device not found local");
		} catch (IOException e) {
			sendIPBack("device not found local");
		}
		
		
		
		
		
	}
	
	private static String bytesToMAC(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    StringBuilder sBuilder = new StringBuilder();
	    for(int i = 0; i < hexChars.length; i++){
	    	sBuilder.append(hexChars[i]);
	    	if(i == 1 || i == 3 || i == 5 || i == 7 || i == 9){
	    		sBuilder.append(":");
	    	}
	    }
	    return sBuilder.toString();
	    
	}
	
	private void upDateStoredIP(String ip){
		String[] storedSettings = cPanel.getStoredString(sentMac).split(";");
		System.out.println(Arrays.toString(storedSettings));
		storedSettings[1] = ip;
		StringBuilder sBuilder = new StringBuilder();
		for(int i = 0; i < storedSettings.length; i++){
			sBuilder.append(storedSettings[i]);
			if(!(i == (storedSettings.length -1))){
				sBuilder.append(";");
			}
		}
		cPanel.saveString(sentMac, sBuilder.toString());
	}
		
	private void sendIPBack(String ip)
	{
		msg.arg1 = Activity.RESULT_OK;
		msg.obj = ip;
		try {
			continueListening = false;
			System.out.println("Sending IP Back");
			messenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		stopSelf();
	}

}
