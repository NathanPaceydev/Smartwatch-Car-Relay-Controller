package com.controlanything.NCDTCPRelay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

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
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;


public class ControlPanel extends Application {

	final String PREFS_NAME= "Settings";
	String ipA = null;
	int port;
	InetSocketAddress sAddress;
	static Socket s;
	int x = 1;
	String deviceSettings;

	//Global Variables
	Boolean shouldContinue = true;
	String Mac;
	boolean verizon = false;
	final static String verizonName = "Verizon Wireless";
	Boolean connected = false;
	static int timeout = 3000;
	ArrayList<Byte> bufferArray;


	SendCommand comHandler;

	Boolean fusion = false;
	int[] bankStatus = {0,0,0,0,0,0,0,0};

	//delay between sending command and reading data back.
	static int delay = 100;

	//Bluetooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;
	Messenger messenger;

	//WiNet device stuff
	boolean winet;
	int returnBytes = 1;
	int winetTimeout = 1000;
	int winetWaitTime = 0;
	String winetMac;
	String deviceType;
	boolean remote = false;

	//PWM device stuff
	boolean pwm;
	//Actuator device stuff
	boolean actuator;

	//Hands Video cycle variables
	boolean cycle = false;
	AsyncTask<String, Void, Void> cRelays;

	String cPanelIntentFilter = "CONTROLPANELBROADCAST";



	//These are variables jacob built into this list
	int bankRelayStatus = 0;	//this holds the value of inBuffer[0] which is the first byte returned from the board for the relaystatus command
	int[] individualRelayStatus = {0, 0, 0, 0, 0, 0, 0, 0, 0};	//this holds the array of indivdual relay status returned from the board
	byte[] inBuffer = new byte[16];	//this holds the values returned from the board

	public ControlPanel(){

	}

	public void setMessenger(Messenger msgr){
		if (messenger!=null){
			messenger = null;
		}
		messenger = msgr;
	}

	public void saveString(String ident, String sSave){

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);			//Create SharedPreferences Object
		SharedPreferences.Editor editor = settings.edit();							//Create Editor object for SharedPreferences Object
		editor.putString(ident, sSave);												//Store String into Preferences file settings.

		editor.commit();															//Save
	}

	public void saveInt(String ident, int iSave){

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);			//Create SharedPreferences Object
		SharedPreferences.Editor editor = settings.edit();							//Create Editor object for SharedPreferences Object
		editor.putInt(ident, iSave);												//Store Int into Preferences file settings.

		editor.commit();															//Save

	}

	public String getStoredString(String ident){		

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);			//Create SharedPreferences Object
		String returnString = settings.getString(ident, "n/a");						//Read String from Shared Preferences Object

		return returnString;														//Return Requested String from Preferences File

	}

	public int getStoredInt(String ident){

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);			//Create SharedPreferences Object
		int returnInt = settings.getInt(ident, 0);									//Read String from Shared Preferences Object

		return returnInt;															//Return Requested Int from Preferences File

	}

	public void deleteString(String ident){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(ident);
		editor.commit();
	}

	public void deleteInt(String ident){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(ident);
		editor.commit();
	}

	public boolean exportSettings(File destination){
		boolean returnBoolean = false;
		ObjectOutputStream output = null;
		try {
			output = new ObjectOutputStream(new FileOutputStream(destination));
			SharedPreferences pref = 
					getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			output.writeObject(pref.getAll());
			returnBoolean = true;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				if (output != null) {
					output.flush();
					output.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return returnBoolean;
	}

	@SuppressWarnings({ "unchecked" })
	public boolean loadSharedPreferencesFromFile(File src) {
		boolean res = false;
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(src));
			Editor prefEdit = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
			prefEdit.clear();
			Map<String, ?> entries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					prefEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					prefEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					prefEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.commit();
			res = true;         
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

	public boolean testConnection(){
		Byte[] command = new Byte[5];
		command[0] = (byte)170;
		command[1] = (byte)2;
		command[2] = (byte)254;
		command[3] = (byte)33;
		command[4] = (byte)((command[0]+command[1]+command[2]+command[3])&255);

		System.out.println("about to trigger connection Bluetooth");
		try {
			//Send command to Async Task(blocks until data is returned or connection fails)
			byte[] recievedData = (new SendCommand().execute(command).get());

			if(recievedData != null){
				if(recievedData.length > 1){
					System.out.println("recieved: "+ Arrays.toString(recievedData));
					if(recievedData[0] == 85 || recievedData[0] == 86 || recievedData[2] == 85 || recievedData[2] == 86 ||recievedData[10]==85 ||recievedData[10]==86){
						connected = true;
						return true;
					}else{
						return false;
					}
				}else{
					if(recievedData[0] == 85 || recievedData[0] == 86){
						connected = true;
						return true;
					}else{
						return false;
					}
				}

			}else{
				System.out.println("connect returning false2");
				return false;
			}

		} catch (InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			return false;
		}
	}

	//Bluetooth Connect
	public boolean connect(String address){
		bluetooth = true;
		btDeviceAddress = address;

		Byte[] command = new Byte[5];
		command[0] = (byte)170;
		command[1] = (byte)2;
		command[2] = (byte)254;
		command[3] = (byte)33;
		command[4] = (byte)((command[0]+command[1]+command[2]+command[3])&255);

		System.out.println("about to trigger connection Bluetooth");
		try {
			//Send command to Async Task(blocks until data is returned or connection fails)
			byte[] recievedData = (new SendCommandBT().execute(command).get());

			if(recievedData != null){
				if(recievedData.length > 1){
					System.out.println("recieved: "+ Arrays.toString(recievedData));
					if(recievedData[0] == 85 || recievedData[0] == 86 || recievedData[2] == 85 || recievedData[2] == 86){
						connected = true;
						return true;
					}
				}else{
					if(recievedData[0] == 85 || recievedData[0] == 86){
						connected = true;
						return true;
					}else{
						return false;
					}
				}

			}else{
				System.out.println("connect returning false2 Bluetooth");
				disconnect();
				return false;
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;

	}

	//TCP Connect
	public boolean connect(String ip, int portN)
	{
		if(ip.contains(";")){
			ip = ip.split(";")[0];
		}
		System.out.println("connect winet: "+winet);
		bluetooth = false;

		if(!isWiFiConnected(getBaseContext())){
			verizon = getCarrierName().equalsIgnoreCase(verizonName);
			System.out.println("Connect to Verizon = "+verizon);
		}




		if(winet){
			returnBytes = 1;
			port = portN;
			System.out.println("port = "+port);

			SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
			String[] storedInfo = settings.getString(winetMac, "n/a").split(";");
			if(storedInfo.length>1){
				int storedPort = Integer.parseInt(storedInfo[2]);
				if(!(port == storedPort)){
					System.out.println("port = "+port);
					ipA = ip+":"+port;
					remote = true;
				}else{
					ipA = ip;
					remote = false;
				}
			}else{
				ipA = ip;
				remote = false;
			}


			System.out.println("cPanel.ipA = "+ipA);


			sAddress = new InetSocketAddress(ip, port);
			if(connected){
				return true;
			}

			Integer[] wiNetCommand = new Integer[2];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 33;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					connected = true;
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		if(connected){
			return true;
		}

		if(comHandler == null){
			comHandler = new SendCommand();
		}

		ipA = ip;
		port = portN;

		sAddress = new InetSocketAddress(ipA, port);

		System.out.println("Connecting to: " + ipA + " On Port: "+port);

		Byte[] command = new Byte[5];
		command[0] = (byte)170;
		command[1] = (byte)2;
		command[2] = (byte)254;
		command[3] = (byte)33;
		command[4] = (byte)((command[0]+command[1]+command[2]+command[3])&255);

		try {
			System.out.println("about to trigger connection");
			byte[] recievedData = (new SendCommand().execute(command).get());
			//			System.out.println("Got" +Arrays.toString(recievedData)+" back from Connect Method");
			if(recievedData != null){
				System.out.println(Arrays.toString(recievedData));
				if(recievedData.length > 1){
					if(recievedData[0] == 85 || recievedData[0] == 86 || recievedData[2] == 85 || recievedData[2] == 86){
						connected = true;
						return true;
					}else{
						if(recievedData.length > 4){
							//Reformat data, sometimes we get zeros on the first few bytes.
							ArrayList<Byte> reformatedData = new ArrayList<Byte>();
							for(int i = 0; i < recievedData.length; i++){
								if(recievedData[i] != 0){
									reformatedData.add(recievedData[i]);
								}
							}
							if(reformatedData.get(0) == 85 ||reformatedData.get(0) == 86 ||reformatedData.get(2) == 85 ||reformatedData.get(2) == 86){
								connected = true;
								return true;
							}else{
								return false;
							}
						}
					}
				}else{
					if(recievedData[0] == 85 || recievedData[0] == 86){
						connected = true;
						return true;
					}else{
						return false;
					}
				}

			}else{
				System.out.println("connect returning false2");
				return false;
			}
			return false;
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			System.out.println("connect returning false3");
			return false;
		} catch (ExecutionException e1) {
			e1.printStackTrace();
			System.out.println("connect returning false4");
			return false;
		}
	}

	//Calls Async Task to disconnect the socket, then prints line.
	public void disconnect(){

		if(bluetooth){
			if(btSocket != null){
				try {
					btSocket.close();
					btSocket = null;
					connected = false;
					System.out.println("Bluetooth Socket Closed");
				} catch (IOException e) {
					System.out.println("Exception closing Bluetooth Socket");
					e.printStackTrace();
				}
			}



		}
		if(winet){
			return;
		}

		else{
			try {
				if(s != null){
					s.close();
					connected = false;
					s = null;
					System.out.println("connected = false");
					System.out.println("Socket Closed");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}

	public String getCarrierName(){
		TelephonyManager manager = (TelephonyManager)getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
		String carrierName = manager.getNetworkOperatorName();
		System.out.println(carrierName);
		return carrierName;
	}

	public static boolean isWiFiConnected(Context context){
		ConnectivityManager connectivityManager = (ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
			networkInfo =
					connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		return networkInfo == null ? false : networkInfo.isConnected();
	}

	public void cycleRelays(String param){
		if(param.equalsIgnoreCase("start")){
			cycle = true;
			cRelays = new CycleRelays().execute("go");
		}else{
			cycle = false;
		}


	}

	public boolean checkFusion(){
		System.out.println("Checking Fusion");
		Byte[] command = new Byte[6];
		command[0] = (byte)170;
		command[1] = (byte)3;
		command[2] = (byte)254;
		command[3] = (byte)53;
		command[4] = (byte)244;
		command[5] = (byte)((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){
			try {
				byte[] deviceType = (new SendCommandBT().execute(command).get());
				if(deviceType != null){
					if((deviceType[2]&128)==128){
						fusion = true;
						return true;
					}else{
						fusion = false;
						return false;
					}

				}else{
					fusion = false;
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}

		if(winet){
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 53;
			wiNetCommand[2] = 244;

			try {
				int[] deviceType = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(deviceType != null){
					if((deviceType[0]&128)==128){
						fusion = true;
						return true;
					}else{
						fusion = false;
						return false;
					}
				}else{
					//No Data Back
					return false;
				}

			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{
			try {
				byte[] deviceType = (new SendCommand().execute(command).get());
				if(deviceType != null){
					if(deviceType.length >= 3){
						if((deviceType[2]&128)==128){
							fusion = true;
							return true;
						}else{
							fusion = false;
							return false;
						}
					}else{
						fusion = false;
						return false;
					}


				}else{
					fusion = false;
					return false;
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}



	}

	public boolean checkPWM(){
		System.out.println("Checking PWM");
		Byte[] command = new Byte[6];
		command[0] = (byte)170;
		command[1] = (byte)3;
		command[2] = (byte)254;
		command[3] = (byte)53;
		command[4] = (byte)244;
		command[5] = (byte)((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){
			try {
				byte[] deviceType = (new SendCommandBT().execute(command).get());
				if(deviceType != null){
					if((deviceType[2]&2)==2){
						pwm = true;
						return true;
					}else{
						pwm = false;
						return false;
					}

				}else{
					pwm = false;
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}

		if(winet){
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 53;
			wiNetCommand[2] = 244;

			try {
				int[] deviceType = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(deviceType != null){
					if((deviceType[0]&2)==2){
						pwm = true;
						return true;
					}else{
						pwm = false;
						return false;
					}
				}else{
					//No Data Back
					return false;
				}

			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{
			try {
				byte[] deviceType = (new SendCommand().execute(command).get());
				if(deviceType != null){
					if((deviceType[2]&2)==2){
						pwm = true;
						return true;
					}else{
						pwm = false;
						return false;
					}

				}else{
					pwm = false;
					return false;
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}

	}

	public boolean checkActuator(){
		System.out.println("Checking Linear Actuator");
		Byte[] command = new Byte[6];
		command[0] = (byte)170;
		command[1] = (byte)3;
		command[2] = (byte)254;
		command[3] = (byte)53;
		command[4] = (byte)247;
		command[5] = (byte)((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){
			try {
				byte[] deviceType = (new SendCommandBT().execute(command).get());
				if(deviceType != null){
					if((deviceType[2]&1)==1){
						actuator = true;
						return true;
					}else{
						actuator = false;
						return false;
					}

				}else{
					actuator = false;
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}

		if(winet){
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 53;
			wiNetCommand[2] = 247;

			try {
				int[] deviceType = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(deviceType != null){
					if((deviceType[0]&1)==1){
						actuator = true;
						return true;
					}else{
						actuator = false;
						return false;
					}
				}else{
					//No Data Back
					return false;
				}

			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{
			try {
				byte[] deviceType = (new SendCommand().execute(command).get());
				if(deviceType != null){
					if((deviceType[2]&1)==1){
						actuator = true;
						return true;
					}else{
						actuator = false;
						return false;
					}

				}else{
					actuator = false;
					return false;
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	//This method sends out a command passed to it by the activity
	public void sendCommand(ArrayList<byte[]> commands, ArrayList<Integer> delays){
		for(int i = 0; i < commands.size(); i++){
			Byte[] sendCommand = new Byte[commands.get(i).length];
			for(int n = 0; n < commands.get(i).length; n++){
				sendCommand[n] = commands.get(i)[n];
			}
			if(bluetooth){
				try {
					//Send command
					//					byte[] recievedData = (new SendCommandBT().execute(this.buildAPIPacket(sendCommand)).get());
					byte[] recievedData = (new SendCommandBT().execute(sendCommand).get());
					//Wait for a response or null from task
					if(recievedData == null){
						//Command failed so stop
						System.out.println("Error Sending Command");
						Intent intent = new Intent(this.cPanelIntentFilter);
						intent.setAction(this.cPanelIntentFilter);
						intent.putExtra("ERROR", "true");
						sendBroadcast(intent);
						break;
					}else{
						//Command was successful.
						System.out.println("Command returned: "+Arrays.toString(recievedData));
						if(i != commands.size() - 1){
							//If this is not the last command then wait before sending the next command
							Thread.sleep(delays.get(i));
						}

					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("Error Sending Command");
					Intent intent = new Intent();
					intent.setAction(this.cPanelIntentFilter);
					intent.putExtra("ERROR", "true");
					sendBroadcast(intent);
				} catch (ExecutionException e) {
					e.printStackTrace();
					System.out.println("Error Sending Command");
					Intent intent = new Intent();
					intent.setAction(this.cPanelIntentFilter);
					intent.putExtra("ERROR", "true");
					sendBroadcast(intent);
				}
			}else{
				if(winet){
					returnBytes =1;
					Integer[] sendCommandWiNet = new Integer[commands.get(i).length];

					for(int n = 0; n < commands.get(i).length; n++){
						sendCommandWiNet[n] = (int)commands.get(i)[n];
						if(sendCommandWiNet[n] < 0){
							sendCommandWiNet[n] = sendCommandWiNet[n]+256;
						}
					}

					try {
						int[] recievedData = (new SendCommandWiNet().execute(sendCommandWiNet).get());
						if(recievedData == null){
							//Command failed so stop
							System.out.println("Error Sending Command");
							System.out.println("Error Sending Command");
							Intent intent = new Intent();
							intent.setAction(this.cPanelIntentFilter);
							intent.putExtra("ERROR", "true");
							sendBroadcast(intent);
						}
						else{
							//Command was successful.
							System.out.println("Command returned: "+Arrays.toString(recievedData));
							if(i != commands.size() - 1){
								//If this is not the last command then wait before sending the next command
								Thread.sleep(delays.get(i));
							}
						}
					} catch (InterruptedException e) {
						System.out.println("SendCommandWiNet failed to Execute");
						System.out.println("Error Sending Command");
						Intent intent = new Intent();
						intent.setAction(this.cPanelIntentFilter);
						intent.putExtra("ERROR", "true");
						sendBroadcast(intent);
					} catch (ExecutionException e) {
						System.out.println("SendCommandWiNet failed to Execute");
						System.out.println("Error Sending Command");
						Intent intent = new Intent();
						intent.setAction(this.cPanelIntentFilter);
						intent.putExtra("ERROR", "true");
						sendBroadcast(intent);
					}
				}else{
					try {
						//Send command
						byte[] recievedData = (new SendCommand().execute(this.buildAPIPacket(sendCommand)).get());
						//Wait for a response or null from task
						if(recievedData == null){
							//Command failed so stop
							System.out.println("Error Sending Command");
							Intent intent = new Intent();
							intent.setAction(this.cPanelIntentFilter);
							intent.putExtra("ERROR", "true");
							sendBroadcast(intent);
							return;
						}else{
							//Command was successful.
							System.out.println("Command returned: "+Arrays.toString(recievedData));
							if(i != commands.size() - 1){
								//If this is not the last command then wait before sending the next command
								Thread.sleep(delays.get(i));
							}

						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						Intent intent = new Intent();
						intent.setAction(this.cPanelIntentFilter);
						intent.putExtra("ERROR", "true");
						sendBroadcast(intent);
					} catch (ExecutionException e) {
						e.printStackTrace();
						Intent intent = new Intent();
						intent.setAction(this.cPanelIntentFilter);
						intent.putExtra("ERROR", "true");
						sendBroadcast(intent);
					}
				}
			}

		}
	}

	private Byte[] buildAPIPacket(Byte[] command){
		Byte[] newCommand = new Byte[command.length+3]; 

		newCommand[0] = (byte)170;
		newCommand[1] = (byte)command.length;
		for(int i = 2; i < command.length+2; i++){
			newCommand[i] = command[i-2];
		}
		byte checkSum = (byte) (newCommand[0]+newCommand[1]);
		for(byte b : command){
			checkSum = (byte) (checkSum + b);
		}
		checkSum = (byte) ((byte)checkSum&255);

		newCommand[newCommand.length - 1] = checkSum;

		return newCommand;
	}

	//this method creates a byte array to send to the board to turn off a relay and if successful returns true
	public boolean TurnOnRelay(int relay, int bank){
		Byte[] command = new Byte[6];
		command[0] = (byte) 170;
		command[1] = (byte) 3;
		command[2] = (byte) 254;
		command[3] = (byte) (108 + relay);
		command[4] = (byte) bank;
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 108 + relay;
			wiNetCommand[2] = bank;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){

					//Standard board
					if(recievedData.length == 1){
						if(recievedData[0] == 85 || recievedData[0] == 86){
							return true;
						}else{
							return false;
						}
					}

					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}
	}

	//this method creates a byte array to send to the board to turn on a relay and if successful returns true
	public boolean TurnOffRelay(int relay, int bank){

		Byte[] command = new Byte[6];
		command[0] = (byte) 170;
		command[1] = (byte) 3;
		command[2] = (byte) 254;
		command[3] = (byte) (100 + relay);
		command[4] = (byte) bank;
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 100 + relay;
			wiNetCommand[2] = bank;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){

					//Standard board
					if(recievedData.length == 1){
						if(recievedData[0] == 85 || recievedData[0] == 86){
							return true;
						}else{
							return false;
						}
					}

					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}
		}
	}

	public int[] TurnOnRelayFusion(int relay, int bank){
		System.out.println("turnoffrelayfusion");
		int[] fail = {260, 260};

		Byte[] command = new Byte[6];

		command[0] = (byte) 170;
		command[1] = (byte) 3;
		command[2] = (byte) 254;
		command[3] = (byte) (relay + 108);
		command[4] = (byte) bank;
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] byteBankStatus = (new SendCommandBT().execute(command).get());
				if(byteBankStatus == null || byteBankStatus.length < 4){
					try {
						if(btSocket != null){
							btSocket.close();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
					return fail;
				}
				if(byteBankStatus.length > 2){
					Byte statusByte = byteBankStatus[3];
					int status = statusByte;
					if (status < 0){status = status + 256;}
					//				bankStatus[8] = status;
					if(status > 127)
					{
						bankStatus[7] = 128;
						status = status - 128;
					}
					else {
						bankStatus[7] = 0;
					}
					if(status > 63)
					{	
						bankStatus[6] = 64;
						status = status - 64;
					}
					else {
						bankStatus[6] = 0;
					}
					if(status > 31)
					{
						bankStatus[5] = 32;
						status = status - 32;
					}
					else {
						bankStatus[5] = 0;
					}
					if(status > 15)
					{
						bankStatus[4] = 16;
						status = status - 16;
					}
					else {
						bankStatus[4] = 0;
					}
					if(status > 7)
					{
						bankStatus[3] = 8;
						status = status - 8;
					}
					else {
						bankStatus[3] = 0;
					}
					if(status > 3)
					{	
						bankStatus[2] = 4;
						status = status - 4;
					}
					else {
						bankStatus[2] = 0;
					}
					if(status > 1)
					{
						bankStatus[1] = 2;
						status = status - 2;
					}
					else {
						bankStatus[1] = 0;
					}
					if(status > 0)
					{
						bankStatus[0] = 1;
					}
					else {
						bankStatus[0] = 0;
					}
				}
				System.out.println("Control Command Complete");
				return bankStatus;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return fail;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return fail;
			}

		}

		if(winet){
			returnBytes =2;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 108 + relay;
			wiNetCommand[2] = bank;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return fail;
				}
				int status = recievedData[1];

				if(status > 127)
				{
					bankStatus[7] = 128;
					status = status - 128;
				}
				else {
					bankStatus[7] = 0;
				}
				if(status > 63)
				{	
					bankStatus[6] = 64;
					status = status - 64;
				}
				else {
					bankStatus[6] = 0;
				}
				if(status > 31)
				{
					bankStatus[5] = 32;
					status = status - 32;
				}
				else {
					bankStatus[5] = 0;
				}
				if(status > 15)
				{
					bankStatus[4] = 16;
					status = status - 16;
				}
				else {
					bankStatus[4] = 0;
				}
				if(status > 7)
				{
					bankStatus[3] = 8;
					status = status - 8;
				}
				else {
					bankStatus[3] = 0;
				}
				if(status > 3)
				{	
					bankStatus[2] = 4;
					status = status - 4;
				}
				else {
					bankStatus[2] = 0;
				}
				if(status > 1)
				{
					bankStatus[1] = 2;
					status = status - 2;
				}
				else {
					bankStatus[1] = 0;
				}
				if(status > 0)
				{
					bankStatus[0] = 1;
				}
				else {
					bankStatus[0] = 0;
				}
				return bankStatus;


			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return fail;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return fail;
			}
		}

		else{

			try {
				byte[] byteBankStatus = (new SendCommand().execute(command).get());
				if(byteBankStatus == null){
					return fail;
				}
				if(byteBankStatus.length > 2){
					Byte statusByte = byteBankStatus[3];
					int status = statusByte;
					if (status < 0){status = status + 256;}
					//				bankStatus[8] = status;
					if(status > 127)
					{
						bankStatus[7] = 128;
						status = status - 128;
					}
					else {
						bankStatus[7] = 0;
					}
					if(status > 63)
					{	
						bankStatus[6] = 64;
						status = status - 64;
					}
					else {
						bankStatus[6] = 0;
					}
					if(status > 31)
					{
						bankStatus[5] = 32;
						status = status - 32;
					}
					else {
						bankStatus[5] = 0;
					}
					if(status > 15)
					{
						bankStatus[4] = 16;
						status = status - 16;
					}
					else {
						bankStatus[4] = 0;
					}
					if(status > 7)
					{
						bankStatus[3] = 8;
						status = status - 8;
					}
					else {
						bankStatus[3] = 0;
					}
					if(status > 3)
					{	
						bankStatus[2] = 4;
						status = status - 4;
					}
					else {
						bankStatus[2] = 0;
					}
					if(status > 1)
					{
						bankStatus[1] = 2;
						status = status - 2;
					}
					else {
						bankStatus[1] = 0;
					}
					if(status > 0)
					{
						bankStatus[0] = 1;
					}
					else {
						bankStatus[0] = 0;
					}
				}
				System.out.println("Control Command Complete");
				return bankStatus;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return fail;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return fail;
			}
		}
	}

	public int[] TurnOffRelayFusion(int relay, int bank){
		System.out.println("turnoffrelayfusion");
		int[] fail = {260, 260};

		Byte[] command = new Byte[6];

		command[0] = (byte) 170;
		command[1] = (byte) 3;
		command[2] = (byte) 254;
		command[3] = (byte) (relay + 100);
		command[4] = (byte) bank;
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] byteBankStatus = (new SendCommandBT().execute(command).get());
				if(byteBankStatus == null || byteBankStatus.length != 5){
					try {
						if(btSocket != null){
							btSocket.close();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
					return fail;
				}
				if(byteBankStatus.length > 2){
					Byte statusByte = byteBankStatus[3];
					int status = statusByte;
					if (status < 0){status = status + 256;}
					//				bankStatus[8] = status;
					if(status > 127)
					{
						bankStatus[7] = 128;
						status = status - 128;
					}
					else {
						bankStatus[7] = 0;
					}
					if(status > 63)
					{	
						bankStatus[6] = 64;
						status = status - 64;
					}
					else {
						bankStatus[6] = 0;
					}
					if(status > 31)
					{
						bankStatus[5] = 32;
						status = status - 32;
					}
					else {
						bankStatus[5] = 0;
					}
					if(status > 15)
					{
						bankStatus[4] = 16;
						status = status - 16;
					}
					else {
						bankStatus[4] = 0;
					}
					if(status > 7)
					{
						bankStatus[3] = 8;
						status = status - 8;
					}
					else {
						bankStatus[3] = 0;
					}
					if(status > 3)
					{	
						bankStatus[2] = 4;
						status = status - 4;
					}
					else {
						bankStatus[2] = 0;
					}
					if(status > 1)
					{
						bankStatus[1] = 2;
						status = status - 2;
					}
					else {
						bankStatus[1] = 0;
					}
					if(status > 0)
					{
						bankStatus[0] = 1;
					}
					else {
						bankStatus[0] = 0;
					}
				}

				return bankStatus;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return fail;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return fail;
			}

		}

		if(winet){
			returnBytes =2;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 100 + relay;
			wiNetCommand[2] = bank;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return fail;
				}
				int status = recievedData[1];

				if(status > 127)
				{
					bankStatus[7] = 128;
					status = status - 128;
				}
				else {
					bankStatus[7] = 0;
				}
				if(status > 63)
				{	
					bankStatus[6] = 64;
					status = status - 64;
				}
				else {
					bankStatus[6] = 0;
				}
				if(status > 31)
				{
					bankStatus[5] = 32;
					status = status - 32;
				}
				else {
					bankStatus[5] = 0;
				}
				if(status > 15)
				{
					bankStatus[4] = 16;
					status = status - 16;
				}
				else {
					bankStatus[4] = 0;
				}
				if(status > 7)
				{
					bankStatus[3] = 8;
					status = status - 8;
				}
				else {
					bankStatus[3] = 0;
				}
				if(status > 3)
				{	
					bankStatus[2] = 4;
					status = status - 4;
				}
				else {
					bankStatus[2] = 0;
				}
				if(status > 1)
				{
					bankStatus[1] = 2;
					status = status - 2;
				}
				else {
					bankStatus[1] = 0;
				}
				if(status > 0)
				{
					bankStatus[0] = 1;
				}
				else {
					bankStatus[0] = 0;
				}
				return bankStatus;


			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return fail;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return fail;
			}
		}

		else{

			try {
				byte[] byteBankStatus = (new SendCommand().execute(command).get());
				if(byteBankStatus == null){
					return fail;
				}
				if(byteBankStatus.length > 2){
					Byte statusByte = byteBankStatus[3];
					int status = statusByte;
					if (status < 0){status = status + 256;}
					//				bankStatus[8] = status;
					if(status > 127)
					{
						bankStatus[7] = 128;
						status = status - 128;
					}
					else {
						bankStatus[7] = 0;
					}
					if(status > 63)
					{	
						bankStatus[6] = 64;
						status = status - 64;
					}
					else {
						bankStatus[6] = 0;
					}
					if(status > 31)
					{
						bankStatus[5] = 32;
						status = status - 32;
					}
					else {
						bankStatus[5] = 0;
					}
					if(status > 15)
					{
						bankStatus[4] = 16;
						status = status - 16;
					}
					else {
						bankStatus[4] = 0;
					}
					if(status > 7)
					{
						bankStatus[3] = 8;
						status = status - 8;
					}
					else {
						bankStatus[3] = 0;
					}
					if(status > 3)
					{	
						bankStatus[2] = 4;
						status = status - 4;
					}
					else {
						bankStatus[2] = 0;
					}
					if(status > 1)
					{
						bankStatus[1] = 2;
						status = status - 2;
					}
					else {
						bankStatus[1] = 0;
					}
					if(status > 0)
					{
						bankStatus[0] = 1;
					}
					else {
						bankStatus[0] = 0;
					}
				}

				return bankStatus;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return fail;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return fail;
			}
		}
	}

	//jacob built
	public Byte queryBoardRelayBankStatus(int bank) {

		Byte[]command = new Byte[6];
		command[0] = (byte) 170;
		command[1] = (byte) 3;
		command[2] = (byte) 254;
		command[3] = (byte) 124;
		command[4] = (byte) bank;
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					System.out.println("Returned Data: "+Arrays.toString(recievedData));
					if(recievedData[1] == 1){
						return recievedData[2];
					}
					else{
						return recievedData[0];
					}
				}else{
					try {
						btSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return null;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return null;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return null;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 124;
			wiNetCommand[2] = bank;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return null;
				}
				byte statusByte = (byte)recievedData[0];
				return statusByte;

			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return null;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return null;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){

					//Standard board
					if(recievedData.length == 1){
						return recievedData[0];
					}

					if(recievedData[1] == 1){
						return recievedData[2];
					}
					else{
						return recievedData[0];
					}
				}else{
					return null;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return null;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return null;
			}
		}
	}

	public int[] getBankStatus(int bank) {
		Byte statusByte = queryBoardRelayBankStatus(bank);
		int status = 0;
		if(statusByte != null){
			status = statusByte;
			//this line makes the negative return values that android sometimes returns and makes it positive
			if (status < 0){status = status + 256;}
			individualRelayStatus[8] = status;
			if(status > 127)
			{
				individualRelayStatus[7] = 128;
				status = status - 128;
			}
			else {
				individualRelayStatus[7] = 0;
			}
			if(status > 63)
			{	
				individualRelayStatus[6] = 64;
				status = status - 64;
			}
			else {
				individualRelayStatus[6] = 0;
			}
			if(status > 31)
			{
				individualRelayStatus[5] = 32;
				status = status - 32;
			}
			else {
				individualRelayStatus[5] = 0;
			}
			if(status > 15)
			{
				individualRelayStatus[4] = 16;
				status = status - 16;
			}
			else {
				individualRelayStatus[4] = 0;
			}
			if(status > 7)
			{
				individualRelayStatus[3] = 8;
				status = status - 8;
			}
			else {
				individualRelayStatus[3] = 0;
			}
			if(status > 3)
			{	
				individualRelayStatus[2] = 4;
				status = status - 4;
			}
			else {
				individualRelayStatus[2] = 0;
			}
			if(status > 1)
			{
				individualRelayStatus[1] = 2;
				status = status - 2;
			}
			else {
				individualRelayStatus[1] = 0;
			}
			if(status > 0)
			{
				individualRelayStatus[0] = 1;
			}
			else {
				individualRelayStatus[0] = 0;
			}
			return individualRelayStatus;
		}else{
			return null;
		}



	}

	public void readAllInputs10Bit(){

		Byte[] sendBytes = new Byte[5];

		sendBytes[0] = (byte)170;
		sendBytes[1] = (byte)2;
		sendBytes[2] = (byte)254;
		sendBytes[3] = (byte)167;
		//Calculate checksum for api packet
		sendBytes[4] = (byte)((sendBytes[0]+sendBytes[1]+sendBytes[2]+sendBytes[3])&255);

		if(bluetooth){
			System.out.println("Bluetooth");
			try {
				byte[] returnData = (new SendCommandBT().execute(sendBytes).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						messenger.send(msg);
						return;
					} catch (RemoteException e) {
						e.printStackTrace();
						return;
					}
				}else{
					int [] dataToReturn = new int[returnData.length];
					if(returnData.length>0){
						for(int i = 0; i < dataToReturn.length; i ++){
							dataToReturn[i] = returnData[i];
							if(dataToReturn[i] < 0){
								dataToReturn[i] = dataToReturn[i] + 256;
							}
						}
						System.out.println("Recieved: "+Arrays.toString(dataToReturn));
						Message msg = new Message();
						msg.obj = dataToReturn;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
							return;
						} catch (RemoteException e) {
							e.printStackTrace();
							return;
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
					return;
				} catch (RemoteException e1) {
					e.printStackTrace();
					return;
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
					return;
				} catch (RemoteException e1) {
					e.printStackTrace();
					return;
				}
			}
		}

		if(winet){
			returnBytes = 16;
			Integer[] wiNetCommand = new Integer[2];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 167;

			try {
				int[] returnData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						messenger.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}else{
					if(returnData.length>0){
						System.out.println("Recieved: "+Arrays.toString(returnData));
						Message msg = new Message();
						msg.obj = returnData;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}


			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		else{
			try {
				byte[] returnData = (new SendCommand().execute(sendBytes).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						System.out.println(messenger);
						messenger.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}else{
					int [] dataToReturn = new int[returnData.length];
					if(returnData.length>0){
						for(int i = 0; i < dataToReturn.length; i ++){
							dataToReturn[i] = returnData[i];
							if(dataToReturn[i] < 0){
								dataToReturn[i] = dataToReturn[i] + 256;
							}
						}
						Message msg = new Message();
						msg.obj = dataToReturn;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
				} catch (RemoteException e1) {
					e.printStackTrace();
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
				} catch (RemoteException e1) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean setChannelBrightness(int channel, int value){

		Byte[] command = new Byte[7];
		command[0] = (byte) 170;
		command[1] = (byte) 4;
		command[2] = (byte) 253;
		command[3] = (byte) channel;
		command[4] = (byte) value;
		command[5] = (byte) 1;
		command[6] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4]+command[5])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[4];
			wiNetCommand[0] = 253;
			wiNetCommand[1] = channel;
			wiNetCommand[2] = value;
			wiNetCommand[3] = 1;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{
			System.out.println("not winet");

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}
		}
	}

	public int[] getChannelBrightness(int numberOfChannels){
		int[] returnStatus = new int[numberOfChannels];

		Byte[] command = new Byte[8];

		command[0] = (byte) 170;
		command[1] = (byte) 5;
		command[2] = (byte) 253;	//PWM command header
		command[3] = (byte) 1;		//Request Buffer Status
		command[4] = (byte) 0;		
		command[5] = (byte) 1;		//Buffer Number
		command[6] = (byte) numberOfChannels;
		command[7] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4]+command[5]+command[6])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData.length == numberOfChannels+3){
						for(int i = 0; i<numberOfChannels;i++){
							returnStatus[i] = recievedData[i+2];
							if(returnStatus[i] < 0){
								returnStatus[i] = returnStatus[i]+256;
							}
						}
						return returnStatus;
					}else{
						return null;
					}
				}else{
					return null;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return null;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return null;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[5];
			wiNetCommand[0] = 253;
			wiNetCommand[1] = 1;
			wiNetCommand[2] = 0;
			wiNetCommand[3] = 1;
			wiNetCommand[4] = numberOfChannels;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return null;
				}
				if(recievedData.length == numberOfChannels){
					return recievedData;
				}else{
					return null;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return null;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return null;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData.length == numberOfChannels+3){
						for(int i = 0; i<numberOfChannels;i++){
							returnStatus[i] = recievedData[i+2];
							if(returnStatus[i] < 0){
								returnStatus[i] = returnStatus[i]+256;
							}
						}
						return returnStatus;
					}else{
						return null;
					}
				}else{
					return null;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return null;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return null;
			}
		}


	}

	public boolean moveActuatorOut(int channel, int speed){
		Byte[] command = new Byte[7];
		command[0] = (byte) 170;	//API Header
		command[1] = (byte) 4;		//Length
		command[2] = (byte) 219;	//Actuator Command Header
		command[3] = (byte) channel;		//Motor 1
		command[4] = (byte) 1;		//Motor Retract in	
		command[5] = (byte) speed;
		command[6] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4]+command[5])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[4];
			wiNetCommand[0] = 219;
			wiNetCommand[1] = channel;
			wiNetCommand[2] = 1;
			wiNetCommand[3] = speed;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}
	}

	public boolean moveActuatorIn(int channel, int speed){
		Byte[] command = new Byte[7];
		command[0] = (byte) 170;	//API Header
		command[1] = (byte) 4;		//Length
		command[2] = (byte) 219;	//Actuator Command Header
		command[3] = (byte) channel;		//Motor 1
		command[4] = (byte) 2;		//Motor Retract in	
		command[5] = (byte) speed;
		command[6] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4]+command[5])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[4];
			wiNetCommand[0] = 219;
			wiNetCommand[1] = channel;
			wiNetCommand[2] = 2;
			wiNetCommand[3] = speed;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}
	}

	public boolean stopActuator(int channel){
		System.out.println("Stop Actuator Called");
		Byte[] command = new Byte[6];
		command[0] = (byte) 170;	//API Header
		command[1] = (byte) 3;		//Length
		command[2] = (byte) 219;	//Actuator Command Header
		command[3] = (byte) channel;		//Motor 1
		command[4] = (byte) 0;		//Motor Break	
		command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[3];
			wiNetCommand[0] = 219;
			wiNetCommand[1] = channel;
			wiNetCommand[2] = 0;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}
	}

	public boolean setActuatorPosition(int channel, int MSB, int LSB, int speed){
		Byte[] command = new Byte[9];
		command[0] = (byte) 170;	//API Header
		command[1] = (byte) 6;		//Length
		command[2] = (byte) 219;	//Actuator Command Header
		command[3] = (byte) channel;		//Motor 1
		command[4] = (byte) 3;		//Set position	
		command[5] = (byte) MSB;	//MSB 10 bit position
		command[6] = (byte) LSB;	//LSB 10 bit position
		command[7] = (byte) speed;
		command[8] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4]+command[5]+command[6]+command[7])&255);

		if(bluetooth){

			try {
				byte[] recievedData = (new SendCommandBT().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}

		if(winet){
			returnBytes =1;
			Integer[] wiNetCommand = new Integer[6];
			wiNetCommand[0] = 219;
			wiNetCommand[1] = channel;
			wiNetCommand[2] = 3;
			wiNetCommand[3] = MSB;
			wiNetCommand[4] = LSB;
			wiNetCommand[5] = speed;

			try {
				int[] recievedData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(recievedData == null){
					return false;
				}
				if(recievedData[0] == 85 || recievedData[0] == 86){
					return true;
				}else{
					return false;
				}
			} catch (InterruptedException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			} catch (ExecutionException e) {
				System.out.println("SendCommandWiNet failed to Execute");
				return false;
			}
		}

		else{

			try {
				byte[] recievedData = (new SendCommand().execute(command).get());
				if(recievedData != null){
					if(recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86){
						return true;
					}else{
						System.out.println(Arrays.toString(recievedData));
						System.out.println("Response from controller invalid");
						return false;
					}
				}else{
					return false;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}

		}
	}

	public void getActuatorPosition(){
		Byte[] sendBytes = new Byte[6];
		sendBytes[0] = (byte) 170;	//API Header
		sendBytes[1] = (byte) 3;		//Length
		sendBytes[2] = (byte) 219;	//Actuator Command Header
		sendBytes[3] = (byte) 0;		//Motor 1
		sendBytes[4] = (byte) 4;		//Set position	
		sendBytes[5] = (byte) ((sendBytes[0]+sendBytes[1]+sendBytes[2]+sendBytes[3]+sendBytes[4])&255);

		if(bluetooth){
			System.out.println("Bluetooth");
			try {
				byte[] returnData = (new SendCommandBT().execute(sendBytes).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						messenger.send(msg);
						return;
					} catch (RemoteException e) {
						e.printStackTrace();
						return;
					}
				}else{
					int [] dataToReturn = new int[returnData.length];
					if(returnData.length>0){
						for(int i = 0; i < dataToReturn.length; i ++){
							dataToReturn[i] = returnData[i];
							if(dataToReturn[i] < 0){
								dataToReturn[i] = dataToReturn[i] + 256;
							}
						}
						System.out.println("Recieved: "+Arrays.toString(dataToReturn));
						Message msg = new Message();
						msg.obj = dataToReturn;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
							return;
						} catch (RemoteException e) {
							e.printStackTrace();
							return;
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
					return;
				} catch (RemoteException e1) {
					e.printStackTrace();
					return;
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
					return;
				} catch (RemoteException e1) {
					e.printStackTrace();
					return;
				}
			}
		}

		if(winet){
			returnBytes = 16;
			Integer[] wiNetCommand = new Integer[2];
			wiNetCommand[0] = 254;
			wiNetCommand[1] = 167;

			try {
				int[] returnData = (new SendCommandWiNet().execute(wiNetCommand).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						messenger.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}else{
					if(returnData.length>0){
						System.out.println("Recieved: "+Arrays.toString(returnData));
						Message msg = new Message();
						msg.obj = returnData;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}


			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		else{
			try {
				byte[] returnData = (new SendCommand().execute(sendBytes).get());
				if(returnData == null){
					Message msg = new Message();
					msg.arg1 =(Activity.RESULT_CANCELED);
					try {
						System.out.println(messenger);
						messenger.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}else{
					int [] dataToReturn = new int[returnData.length];
					if(returnData.length>0){
						for(int i = 0; i < dataToReturn.length; i ++){
							dataToReturn[i] = returnData[i];
							if(dataToReturn[i] < 0){
								dataToReturn[i] = dataToReturn[i] + 256;
							}
						}
						Message msg = new Message();
						msg.obj = dataToReturn;
						msg.arg1 =(Activity.RESULT_OK);
						try {
							messenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
				} catch (RemoteException e1) {
					e.printStackTrace();
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 =(Activity.RESULT_CANCELED);
				try {
					messenger.send(msg);
				} catch (RemoteException e1) {
					e.printStackTrace();
				}
			}
		}
	}

	public class SendCommand extends AsyncTask<Byte, Integer, byte[]>{

		@Override
		protected byte[] doInBackground(Byte... command) {

			//Generate byte command to be sent
			byte[]sendCommand = new byte[command.length];
			for(int i = 0; i < command.length; i++){
				sendCommand[i] = (byte)command[i];
			}

			//Check to see if the socket exists and if it is connected
			if(s != null){
				try {
					if (s.isConnected())
					{
						//Socket is connected do nothing
						System.out.println("Socket was connected");
					}
					else{
						//Socket is not connected so connect it.
						s = null;
						s = new Socket();
						s.setSoTimeout(timeout);
						System.out.println("Connect 1");
						s.connect(sAddress, 3000);
						System.out.println("Connected");
						connected = true;
					}
				}
				catch (RuntimeException e) {
					e.printStackTrace();
					return null;
				} catch (SocketException e) {
					e.printStackTrace();
					return null;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}else{
				try {
					//Socket does not exist so create it and connect it
					s = new Socket();
					s.setSoTimeout(timeout);
					System.out.println("Connect 2");
					s.connect(sAddress, 3000);
					System.out.println("Connected");
					connected = true;
				} catch (SocketException e) {
					System.out.println("Socket Exception on connect 2");
					s = null;
					try {
						s = new Socket();
						s.setSoTimeout(timeout);
						System.out.println("Connect 2 second try");
						s.connect(sAddress, 3000);
						System.out.println("Connected");
						connected = true;
					} catch (SocketException e1) {
						System.out.println("Socket Exception on connect 2 after second try SocketException");
						e1.printStackTrace();
						return null;
					} catch (IOException e1) {
						System.out.println("Socket Exception on connect 2 after second try IOException");
						e1.printStackTrace();
						return null;
					}

				}
				catch (IOException e1) {
					e1.printStackTrace();
					return null;
				}


			}


			//Send command to controller and wait for response.
			try {
				System.out.println("Sending 1 " + Arrays.toString(sendCommand));
				s.getOutputStream().write(sendCommand, 0, sendCommand.length);

				int sendTimeout = 0;

				while(sendTimeout < 30){
					if(s.getInputStream().available() != 0){
						Thread.sleep(delay);
						break;
					}else{
						Thread.sleep(100);
						sendTimeout = sendTimeout+1;
					}
				}
				byte[] recieveBuffer = new byte[s.getInputStream().available()];

				s.getInputStream().read(recieveBuffer);

				//Retry
				if(recieveBuffer.length == 0){
					System.out.println("Nothing returned, retrying");
					if(s.isConnected()){
						s.close();
					}

					s = null;
					s = new Socket();
					s.setSoTimeout(timeout);
					System.out.println("Connect 3");
					s.connect(sAddress, 3000);
					System.out.println("Socket Connected after retry");
					Thread.sleep(delay);
					s.getOutputStream().write(sendCommand, 0, sendCommand.length);
					Thread.sleep(delay);

					sendTimeout = 0;

					while(sendTimeout < 30){
						if(s.getInputStream().available() != 0){
							Thread.sleep(delay);
							break;
						}else{
							Thread.sleep(100);
							sendTimeout = sendTimeout+1;
						}
					}
					if(s.getInputStream().available() != 0){
						byte[] receiveAfterRetryBuffer = new byte[s.getInputStream().available()];
						s.getInputStream().read(receiveAfterRetryBuffer);
						recieveBuffer = receiveAfterRetryBuffer;
						System.out.println("Recieved after retry "+Arrays.toString(receiveAfterRetryBuffer));
					}else{
						return null;
					}


				}
				System.out.println("Recieved 1 "+Arrays.toString(recieveBuffer));
//				if(verizon){
//					if(s.isConnected()){
//						System.out.println("Closing socket because we are on Verizon");
//						s.close();
//					}
//
//					s = null;
//				}
				return recieveBuffer;

			} catch (IOException e) {
				System.out.println("Hit retry in IOException exception");
				try {
					s.close();
					s = null;
					s = new Socket();
					s.setSoTimeout(timeout);
					System.out.println("Connect 4");
					s.connect(sAddress, 3000);
					System.out.println("Connected");
					System.out.println("Sending 2 " + Arrays.toString(sendCommand));
					s.getOutputStream().write(sendCommand, 0, sendCommand.length);
					Thread.sleep(delay);

					while(timeout < 30){
						if(s.getInputStream().available() != 0){
							break;
						}else{
							Thread.sleep(100);
							timeout = timeout+1;
						}
					}
					if(s.getInputStream().available() == 0){
						return null;
					}
					byte[] recieveBuffer = new byte[s.getInputStream().available()];

					s.getInputStream().read(recieveBuffer);
					System.out.println("Recieved 2 "+Arrays.toString(recieveBuffer));
					//					s.close();
					//					s = null;
					//					connected = false;
					return recieveBuffer;
				} catch (SocketException e1) {
					System.out.println("Hit retry in SocketException e1");
					try {
						s.close();
						connected = false;
						s = null;
						s = new Socket();
						s.setSoTimeout(timeout);
						System.out.println("Connect 5");
						s.connect(sAddress, 3000);
						System.out.println("Sending 3 " + Arrays.toString(sendCommand));
						s.getOutputStream().write(sendCommand, 0, sendCommand.length);
						Thread.sleep(delay);

						while(timeout < 30){
							if(s.getInputStream().available() != 0){
								break;
							}else{
								Thread.sleep(100);
								timeout = timeout+1;
							}
						}
						if(s.getInputStream().available() == 0){
							return null;
						}
						byte[] recieveBuffer = new byte[s.getInputStream().available()];

						s.getInputStream().read(recieveBuffer);
						System.out.println("Recieved 3 "+Arrays.toString(recieveBuffer));
						//						s.close();
						//						s = null;
						//						connected = false;
						return recieveBuffer;

					} catch (IOException e2) {
						System.out.println("IOException e2");
						e2.printStackTrace();
					} catch (InterruptedException e2) {
						e2.printStackTrace();
						return null;
					}

					e1.printStackTrace();
					return null;
				} catch (IOException e1) {
					e1.printStackTrace();
					return null;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					return null;
				}
			}catch(RuntimeException e1){
				e1.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}



	}

	public class CycleRelays extends AsyncTask<String, Void, Void>{

		@Override
		protected Void doInBackground(String... params) {
			while(cycle){
				//Turn on all 72 relays
				for(int i = 0; i < 72; i++){
					int bank = 1;
					int relay = i;
					if(i > 7){
						bank = 2;
						relay = i - 8;
					}
					if(i > 15){
						bank = 3;
						relay = i - 16;
					}
					if(i > 23){
						bank = 4;
						relay = i - 24;
					}
					if(i > 31){
						bank = 5;
						relay = i - 32;
					}
					if(i > 39){
						bank = 6;
						relay = i - 40;
					}
					if(i > 47){
						bank = 7;
						relay = i - 48;
					}
					if(i > 55){
						bank = 8;
						relay = i - 56;
					}
					if(i > 63){
						bank = 9;
						relay = i - 64;
					}

					Byte[] command = new Byte[6];
					command[0] = (byte) 170;
					command[1] = (byte) 3;
					command[2] = (byte) 254;
					command[3] = (byte) (108 + relay);
					command[4] = (byte) bank;
					command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

					byte[] recievedData;
					try {
						recievedData = (new SendCommand().execute(command).get());
						if(recievedData != null){
							//							Thread.sleep(50);
						}else{
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}
				//Turn off all 72 relays
				for(int i = 0; i < 72; i++){
					int bank = 1;
					int relay = i;
					if(i > 7){
						bank = 2;
						relay = i - 8;
					}
					if(i > 15){
						bank = 3;
						relay = i - 16;
					}
					if(i > 23){
						bank = 4;
						relay = i - 24;
					}
					if(i > 31){
						bank = 5;
						relay = i - 32;
					}
					if(i > 39){
						bank = 6;
						relay = i - 40;
					}
					if(i > 47){
						bank = 7;
						relay = i - 48;
					}
					if(i > 55){
						bank = 8;
						relay = i - 56;
					}
					if(i > 63){
						bank = 9;
						relay = i - 64;
					}

					Byte[] command = new Byte[6];
					command[0] = (byte) 170;
					command[1] = (byte) 3;
					command[2] = (byte) 254;
					command[3] = (byte) (100 + relay);
					command[4] = (byte) bank;
					command[5] = (byte) ((command[0]+command[1]+command[2]+command[3]+command[4])&255);

					byte[] recievedData;
					try {
						recievedData = (new SendCommand().execute(command).get());
						if(recievedData != null){
							//							Thread.sleep(200);
						}else{
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}
			}
			return null;
		}

	}

	public class SendCommandBT extends AsyncTask<Byte, Integer, byte[]>{

		@Override
		protected byte[] doInBackground(Byte... command) {

			//Socket not yet created so create it
			if(btSocket == null){
				//No Address so stop
				if(btDeviceAddress == null){
					System.out.println("No btDeviceAddress");
					return null;
				}
				btAdapter = BluetoothAdapter.getDefaultAdapter();
				btDevice = btAdapter.getRemoteDevice(btDeviceAddress);
				try {
					btSocket = btDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					System.out.println("btSocket object created");
				} catch (IOException e) {
					//Could not create socket so stop
					System.out.println("Exception creating socket");
					e.printStackTrace();
					btSocket = null;
					return null;
				}
			}
			//Socket is not yet connected so connect it
			if(!btSocket.isConnected()){
				try{
					//					btAdapter.cancelDiscovery();
					btSocket.connect();
					System.out.println("Connected");
					//Sleep required after connection due to trash data bluetooth module puts out to chip after connection.
					Thread.sleep(500);
				}catch(IOException e){
					//Could not connect so stop
					System.out.println("could not connect bt");
					e.printStackTrace();
					try {
						btSocket.close();
						btSocket = null;
					} catch (IOException e1) {
						e1.printStackTrace();
						btSocket = null;
					}
					btSocket = null;
					return null;
				} catch (InterruptedException e) {
					System.out.println("Exception on Sleep");
					e.printStackTrace();
					try {
						btSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					btSocket = null;
					return null;
				}
			}

			//send the command
			//Generate byte command to be sent
			byte[]sendCommand = new byte[command.length];

			for(int i = 0; i < command.length; i++){
				sendCommand[i] = (byte)command[i];
			}

			//send the command
			try {
				System.out.println("Sending: "+Arrays.toString(sendCommand));
				btSocket.getOutputStream().write(sendCommand);

				//wait for a response
				long startTime = System.currentTimeMillis();
				long timeout = 3000;
				while(System.currentTimeMillis() < (startTime + timeout)){
					if(btSocket.getInputStream().available() != 0){
						break;
					}
					System.out.println("Bytes in inputStream: "+btSocket.getInputStream().available());
					System.out.println("timeout: "+ (System.currentTimeMillis() > (startTime + timeout)));
					Thread.sleep(100);
				}

				//Check that we got a response
				if(btSocket.getInputStream().available() == 0){
					//No Data returned so return null
					System.out.println("No Data Returned here");
					btSocket.close();
					connected = false;
					btSocket = null;
					return null;

				}
				Thread.sleep(100);
				//Success
				byte[] returnData = new byte[btSocket.getInputStream().available()];
				btSocket.getInputStream().read(returnData);
				System.out.println("Received: "+Arrays.toString(returnData)+"After "+(System.currentTimeMillis()-startTime)+" ms");
				return returnData;

			} catch (IOException e) {
				System.out.println("Exception reading or writing to Bluetooth Socket");
				e.printStackTrace();
				try {
					btSocket.close();
					btSocket = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				connected = false;
				btSocket = null;
				return null;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}

	}

	public class SendCommandWiNet extends AsyncTask<Integer, int[], int[]>{

		@Override
		protected int[] doInBackground(Integer... command) {

			//create variables for HTTP request
			String firstURLHalf = "http://";
			String secondURLHalf = "/cgi-bin/runcommand.sh?524:cmd=";
			String timeout = "t"+String.valueOf(winetTimeout);
			String expectedReturnBytes = "r"+String.valueOf(returnBytes);
			String waitTime = "w"+String.valueOf(winetWaitTime);
			String commandString = null;

			int[] returnData = null;

			//convert command to string and put it in our commandString object
			for(int i = 0; i < command.length; i++){
				if (commandString == null){
					commandString = String.valueOf(command[i]);
				}else{
					commandString = commandString+","+String.valueOf(command[i]);
				}
			}

			//Create Params for HTTP Client Object
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);

			//Create new HTTP Client Object
			HttpClient httpclient = new DefaultHttpClient(httpParameters);

			//Create Object to hold response from controller and a string object to put that response in
			HttpResponse response;
			String[] responseString;

			//Create HTTP request object
			HttpGet httpget = new HttpGet(firstURLHalf+ipA+secondURLHalf+commandString+expectedReturnBytes+timeout+waitTime+"id"+winetMac);
			System.out.println("Request = "+firstURLHalf+ipA+secondURLHalf+commandString+expectedReturnBytes+timeout+waitTime+"id"+winetMac);

			//Execute HTTP request and wait for response
			try {
				response = httpclient.execute(httpget);
				StatusLine statusLine = response.getStatusLine();
				if(statusLine.getStatusCode() == HttpStatus.SC_OK)
				{
					//got ok so check the data
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);

					out.close();
					//put the resonse from the server in a string so we can see it in plain English
					String rString = out.toString();
					System.out.println("rString: "+rString);
					responseString = rString.split("\n");

					//Check to make sure we got more than just an ok
					if(responseString.length > 1){
						//Check if board was fusion so it returns bank number + status
						if(responseString[1].contains(",")){
							int length = responseString[1].split(",").length+1;
							String[] tempArray = responseString[1].split(",");
							String[] lineSplit = new String[length];
							lineSplit[0] = "OK";
							for(int i = 0; i < length - 1; i++){
								lineSplit[i+1] = tempArray[i];
							}
							responseString = lineSplit;
						}
						for(int i = 0; i< responseString.length; i++){
							responseString[i]=responseString[i].replaceAll("\\s+","");
						}
						returnData = new int[responseString.length -1];
						//Start at 1 because position 0 will just have an OK in it
						for(int i = 0; i < returnData.length; i++){
							returnData[i] = Integer.parseInt(responseString[i+1]);
						}
						System.out.println("sendCommandWiNet returning: "+Arrays.toString(returnData));
						return returnData;
					}else{
						//no data back
						return null;
					}

				}else{
					//Status was not ok for some reason
					return null;
				}
			} catch (ClientProtocolException e) {
				System.out.println("Client Protocol problem with HTTP request");
				return null;
			} catch (IOException e) {
				System.out.println("HTTP failed, probably timed out");
				e.printStackTrace();
				connected = false;
				return null;
			}
		}

	}
}