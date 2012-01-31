package net.casainho;

import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.os.Handler;

public class SmartLamp extends Activity
implements SeekBar.OnSeekBarChangeListener {

	private static final String TAG = "SmartLamp";
	
    // Message types sent from the BluetoothCommService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
	public static final int RESULT_FIND_DEVICE = 0; 
	
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the Comm services
    private BluetoothCommService mCommService = null;
    
    public boolean mDeviceIsProcessing = false;
    
    SeekBar seekBarR;
    SeekBar seekBarG;
    SeekBar seekBarB;
    
    ProgressDialog mDialog;
    
    public Vector<Byte> mDataVector = new Vector<Byte>();
    public String dataString = "";
    
    static long lastTime = System.currentTimeMillis();
    
    private int mLampState = 1;
    private int mBTInitState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up the window layout
        setContentView(R.layout.main);
        //TODO: Comment all code, and test with with new layout
        //setContentView(R.layout.colorpicker);
        mDialog = ProgressDialog.show(this, "", 
        		this.getString(R.string.title_find_devices), true);
        
        // Setup seekBardR and install the OnSeekBarChangeListener
        seekBarR = (SeekBar) findViewById(R.id.seekbarr);
        
        // Setup seekBardG and install the OnSeekBarChangeListener
        seekBarG = (SeekBar)findViewById(R.id.seekbarg);
        
        // Setup seekBardB and install the OnSeekBarChangeListener
        seekBarB = (SeekBar)findViewById(R.id.seekbarb);        
          
        seekBarR.setOnSeekBarChangeListener(this);
        seekBarG.setOnSeekBarChangeListener(this);
        seekBarB.setOnSeekBarChangeListener(this);
        
        // Bind the action for the save button.
        findViewById(R.id.buttonOnOff).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When the button is clicked
                if (mLampState == 0) {
                    sendMessage("d1\n");
                    mLampState = 1;
                } else {
                    sendMessage("d0\n");
                    mLampState = 0;            	
                }
            }
        });


        // Bind the action for the save button.
        /*findViewById(R.id.btModuleNameChange).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When the button is clicked
                sendMessage("z\n");
            }
        }); */
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Start activity find and return the device bluetooth address
        Intent intent = new Intent(this, FindDeviceActivity.class);
        startActivityForResult(intent, RESULT_FIND_DEVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth Comm services
        if (mCommService != null) mCommService.stop();
        
        // Disable bluetooth if it was disabled before
        if (mBTInitState == 0) {
            mBluetoothAdapter.disable();
        }
        
        Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
    	
        // Check that we're actually connected before trying anything
        if (mCommService.getState() != BluetoothCommService.STATE_CONNECTED) {
            //Toast.makeText(this, "not connected to device", Toast.LENGTH_SHORT).show();
            return;
        }

    	byte[] messageByteArray = null; 	
    	
        // Check that there's actually something to send
        if (messageByteArray != null) {
            // Message will be sent and device will be busy, until device signal the contrary
            mDeviceIsProcessing = true;
        	
            mCommService.write(messageByteArray);
        }
    }


    // The Handler that gets information back from the BluetoothCommService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothCommService.STATE_CONNECTED:
                	mDialog.dismiss();
                    break;
                case BluetoothCommService.STATE_CONNECTING:
                    
                    break;
                case BluetoothCommService.STATE_NONE:
mDialog.dismiss();
//					finish();
                    break;
                }
                break;
            case MESSAGE_WRITE:

                break;
            case MESSAGE_READ:
            	byte[] data = (byte[]) msg.obj;
            	int len = msg.arg1;
            	
            	mDeviceIsProcessing = false;
            	
            	// store all received bytes on vector
            	for (int i = 0; i < len; ) {
            		mDataVector.add(data[i]);
            		i++;
            	}
            	
            	mCommService.newMessage = false;
            	
            	byte[] buf = new byte[32];
            	// loop and store on buf[], bytes from mDataVector, until found a \n
            	for (int i = 0; (i < 32) && (i < mDataVector.size()); ) {
            		buf[i] = mDataVector.get(i);
            		
            		// looking for \n, the end of string/command
            		if (buf[i] == '\n') {
            			// remove elements from vector 
            			for (int j = 0; j < (i+1); ) {
            				mDataVector.removeElementAt(0);
            			
            				j++;
            			}

            			dataString = new String(buf, 0, (i+1));
            			i = -1;
                    	
            			/* 
                         * Processing message sent by device
                         */
            			
                        // device is not processing any more
                        if (dataString.equals("ok\n")) {
                        	mDeviceIsProcessing = false;
                        }
            		}
            		
            		i++;
            	}
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to: "
                               + mConnectedDeviceName, Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case RESULT_FIND_DEVICE:
            // When FindDeviceActivity returns with an address device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Read the device MAC address from Intent
                String address = data.getExtras()
                                     .getString(FindDeviceActivity.EXTRA_DEVICE_ADDRESS); 

                // Read BT initial state
                mBTInitState = data.getExtras()
                .getInt(FindDeviceActivity.EXTRA_BT_INITIAL_STATE);
                
                // Get the BluetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                
                // Initialize the BluetoothCommService to perform bluetooth connections
                mCommService = new BluetoothCommService(this, mHandler);

                // Attempt to connect to the device
                mCommService.connect(device);
            }
            // For some reason FindDeviceActivity didn't return a bluetooth device address
            else {
            	Toast.makeText(this, "not bluetooth device", Toast.LENGTH_SHORT).show();
            //	finish();
            }
            break;
        }
    }
    

    //@Override
    public void onProgressChanged(SeekBar seekBar, int progress,
      boolean fromUser) {
    	if (mDeviceIsProcessing == false || ((lastTime + System.currentTimeMillis()) > 100) )
    	{
    		lastTime = System.currentTimeMillis();
    		
    		if (progress == 0)
    			progress = 1;
    		
			if (seekBar.equals(seekBarR))
			{
			     sendMessage(String.valueOf("r" + progress));	
			}
			
			else if (seekBar.equals(seekBarG))
			{
			     sendMessage(String.valueOf("g" + progress));	
			}
			
			else if (seekBar.equals(seekBarB))
			{
			     sendMessage(String.valueOf("b" + progress));	
			}
    	}
    }

    //@Override
    public void onStartTrackingTouch(SeekBar seekBar) {
     // Auto-generated method stub
    }

    //@Override
    public void onStopTrackingTouch(SeekBar seekBar) {
     // Auto-generated method stub
    }
}