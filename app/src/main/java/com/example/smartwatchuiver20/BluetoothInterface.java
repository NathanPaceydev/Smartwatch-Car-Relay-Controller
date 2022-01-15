package com.example.smartwatchuiver20;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Message;
import android.os.RemoteException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class BluetoothInterface extends Application {

    static BluetoothAdapter btAdapter;
    static BluetoothDevice btDevice;
    static BluetoothSocket btSocket;
    static String btAddress;

    public void btConnect() throws IOException, InterruptedException {
        btAddress = SetupActivity.btDevice.getAddress();
        if (btAddress == null) {}
        else {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            btDevice = btAdapter.getRemoteDevice(btAddress);
            btSocket = btDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            ConnectedThread reader = new ConnectedThread(btSocket);
            //reader.run();
            }
        }

    public int[] TurnOnRelayFusion(int relay, int bank) {
        System.out.println("turnoffrelayfusion");
        int[] fail = {260, 260};

        Byte[] command = new Byte[6];

        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) (relay + 108);
        command[4] = (byte) bank;
        command[5] = (byte) ((command[0] + command[1] + command[2] + command[3] + command[4]) & 255);

        try {
            byte[] byteBankStatus = (new SendCommandBT().execute(command).get());
            if (byteBankStatus == null || byteBankStatus.length < 4) {
                if (btSocket != null) {
                    btSocket.close();
                    System.out.println("Turn on Bluetooth Socket Closed");
                }

                return fail;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return fail;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fail;
    }
    public int[] TurnOffRelayFusion(int relay, int bank) {
        System.out.println("turnoffrelayfusion");
        int[] fail = {260, 260};

        Byte[] command = new Byte[6];

        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) (relay + 100);
        command[4] = (byte) bank;
        command[5] = (byte) ((command[0] + command[1] + command[2] + command[3] + command[4]) & 255);

        try {
            byte[] byteBankStatus = (new SendCommandBT().execute(command).get());
            if (byteBankStatus == null || byteBankStatus.length < 4) {
                if (btSocket != null) {
                    System.out.println("Turn on Bluetooth Socket Closed");
                    btSocket.close();
                }

                return fail;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return fail;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fail;
    }

    public void readAllInputs10Bit(){

        Byte[] sendBytes = new Byte[5];

        sendBytes[0] = (byte)170;
        sendBytes[1] = (byte)2;
        sendBytes[2] = (byte)254;
        sendBytes[3] = (byte)167;
        //Calculate checksum for api packet
        sendBytes[4] = (byte)((sendBytes[0]+sendBytes[1]+sendBytes[2]+sendBytes[3])&255);

            System.out.println("Bluetooth");
            try {
                byte[] returnData = (new SendCommandBT().execute(sendBytes).get());
                if(returnData == null){
                    Message msg = new Message();
                    msg.arg1 =(Activity.RESULT_CANCELED);

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
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();

            } catch (ExecutionException e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.arg1 =(Activity.RESULT_CANCELED);
            }

    }

    @SuppressLint("StaticFieldLeak")
    public static class SendCommandBT extends AsyncTask<Byte, Integer, byte[]> {
        @Override
        protected byte[] doInBackground(Byte... command) {

            //Socket not yet created so create it
            if (btSocket == null) {
                //No Address so stop
                if (btAddress == null) {
                    System.out.println("No btDeviceAddress");
                    return null;
                }
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                btDevice = btAdapter.getRemoteDevice(btAddress);
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
            if (!btSocket.isConnected()) {
                try {
                    //					btAdapter.cancelDiscovery();
                    btSocket.connect();
                    System.out.println("Connected");
                    //Sleep required after connection due to trash data bluetooth module puts out to chip after connection.
                    Thread.sleep(500);
                } catch (IOException e) {
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
            byte[] sendCommand = new byte[command.length];

            for (int i = 0; i < command.length; i++) {
                sendCommand[i] = (byte) command[i];
            }

            //send the command
            try {
                System.out.println("Sending: " + Arrays.toString(sendCommand));
                btSocket.getOutputStream().write(sendCommand);

                //wait for a response
                long startTime = System.currentTimeMillis();
                long timeout = 3000;
                while (System.currentTimeMillis() < (startTime + timeout)) {
                    if (btSocket.getInputStream().available() != 0) {
                        break;
                    }
                    System.out.println("Bytes in inputStream: " + btSocket.getInputStream().available());
                    System.out.println("timeout: " + (System.currentTimeMillis() > (startTime + timeout)));
                    Thread.sleep(100);
                }

                //Check that we got a response
                if (btSocket.getInputStream().available() == 0) {
                    //No Data returned so return null
                    System.out.println("No Data Returned here");
                    btSocket.close();
                    btSocket = null;
                    return null;

                }
                Thread.sleep(100);
                //Success
                byte[] returnData = new byte[btSocket.getInputStream().available()];
                btSocket.getInputStream().read(returnData);
                System.out.println("Received: " + Arrays.toString(returnData) + "After " + (System.currentTimeMillis() - startTime) + " ms");
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
                btSocket = null;
                return null;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                System.out.println("FAILED");
                e.printStackTrace();
                return null;
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;

            try{
                tmpIn = socket.getInputStream();
            } catch (IOException e){
            }

            mmInStream = tmpIn;

        }

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;

            while(true){
                try{
                    numBytes = mmInStream.read(mmBuffer);
                    System.out.println(numBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


    }
}
