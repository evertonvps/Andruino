package com.blogspot.receitastenologicas;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.blogspot.receitastenologicas.bluetooth.BluetoothChatService;
import com.blogspot.receitastenologicas.bluetooth.DeviceListActivity;

public class Andruino extends Activity implements SensorEventListener {
	private final String TAG = "ANDRUINO";
	ToggleButton btEsq;
	ToggleButton btAce;
	ToggleButton btDir;
	Vibrator vibrator;
	/***/
	private Float maxLeft;
	private Float maxRight;
	private Float maxAccelerator;
	// Debugging
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Layout Views

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	private ProgressBar progressBarLeft;
	private ProgressBar progressBarRight;
	private ProgressBar progressBarAccelerator;
	private TextView txtViewRight;
	private TextView txtViewLeft;
	private TextView txtViewAccelerator;
	private TextView txtViewTitle;
	private SensorManager mgr;
	private Sensor accel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		setContentView(R.layout.andruino);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		btEsq = (ToggleButton) findViewById(R.id.btLeft);
		btEsq.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					Toast.makeText(buttonView.getContext(),
							"Vire o Controle para a Esquerda",
							Toast.LENGTH_SHORT).show();
				} else {

					if (maxLeft == null || maxLeft < Math.abs(y)) {
						maxLeft = Math.abs(y);
						vibrator.vibrate(100);
					}
				}
			}
		});
		btAce = (ToggleButton) findViewById(R.id.btAcelerador);
		btAce.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					Toast.makeText(buttonView.getContext(),
							"Incline o Controle para Frente",
							Toast.LENGTH_SHORT).show();
				} else {

					if (maxAccelerator == null || maxAccelerator < Math.abs(z)) {
						maxAccelerator = Math.abs(z);
						vibrator.vibrate(100);
					}
				}
			}
		});
		// /btAce.setOnClickListener(this);
		btDir = (ToggleButton) findViewById(R.id.btRight);
		btDir.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					Toast.makeText(buttonView.getContext(),
							"Vire o Controle para a Direita",
							Toast.LENGTH_SHORT).show();
				} else {

					if (maxRight == null || maxRight < Math.abs(y)) {
						maxRight = Math.abs(y);
						vibrator.vibrate(100);
					}
				}
			}
		});

		txtViewRight = (TextView) findViewById(R.id.txtViewRight);
		txtViewLeft = (TextView) findViewById(R.id.txtViewLeft);
		txtViewAccelerator = (TextView) findViewById(R.id.txtViewAccelerator);
		txtViewTitle = (TextView) findViewById(R.id.title);

		progressBarLeft = (ProgressBar) findViewById(R.id.progressBarLeft);
		progressBarRight = (ProgressBar) findViewById(R.id.progressBarRight);
		progressBarAccelerator = (ProgressBar) findViewById(R.id.progressBarAccelerator);

		mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		mgr.unregisterListener(this, accel);
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			txtViewTitle.setText(R.string.not_connected);
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
		}
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					break;
				case BluetoothChatService.STATE_CONNECTING:
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				// StringTokenizer tokens = new StringTokenizer(readMessage,
				// ":");
				// int left = Integer.parseInt(tokens.nextToken());
				// int right = Integer.parseInt(tokens.nextToken());
				// int walk = Integer.parseInt(tokens.nextToken());
				// Log.i("MSG", "L:" + left + " R:" + right + " W:" + walk);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/** Accuracy configuration */
	private long now = 0;
	private long lastUpdate = 0;

	private float x = 0;
	private float y = 0;
	private float z = 0;

	public void onSensorChanged(SensorEvent event) {
		now = TimeUnit.MILLISECONDS.convert(event.timestamp,
				TimeUnit.NANOSECONDS);

		if (lastUpdate == 0 || (now - lastUpdate) > 300) {
			x = event.values[0];
			y = event.values[1];// Controle de inclinação de direção
			z = event.values[2];// Controle de inclinação de acelerar/parar/ré

			if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
				Log.d("TIME: " + lastUpdate, now + " - " + (now - lastUpdate));
				Log.d("XYZ:", "X: " + x + " | Y: " + y + " | Z: " + z);
			}
			lastUpdate = now;
			doUpdate(null);
		}

	}

	public void doUpdate(View view) {
		// if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
		// {

		Float percentAccelerator = (float) 0;
		Float percentRight = (float) 0;
		Float percentLeft = (float) 0;

		if (isCalibrated()) {
			if (y < 0) {// Valor negativo - Inclinando para esquerda
				percentLeft = y > maxLeft ? 100 : Math.abs((y / maxLeft) * 100);
				txtViewRight.setText(" <<--\n " + percentLeft.intValue() + "%");
				txtViewLeft.setText(" -->>\n 0%");
				progressBarLeft.setProgress(percentLeft.intValue());
				progressBarRight.setProgress(0);
			} else {
				percentRight = y > maxRight ? 100 : Math
						.abs((y / maxRight) * 100);
				txtViewLeft.setText(" -->>\n " + percentRight.intValue() + "%");
				txtViewRight.setText(" <<--\n 0%");
				progressBarRight.setProgress(percentRight.intValue());
				progressBarLeft.setProgress(0);
			}

			// Tratando a aceleração / parada / Ré
			percentAccelerator = z > maxAccelerator ? 100 : Math
					.abs((z / maxAccelerator) * 100);
			if (((Float) z).intValue() > 3) {// Acelera

				txtViewAccelerator.setText("  A  \n "
						+ percentAccelerator.intValue() + "%");
				progressBarAccelerator.setProgress(percentAccelerator
						.intValue());
			} else if (((Float) z).intValue() > 0) {// Parar
				txtViewAccelerator.setText("  N  \n 0%");
				progressBarAccelerator.setProgress(0);
			} else {// Ré
				txtViewAccelerator.setText("  R  \n "
						+ percentAccelerator.intValue() + "%");
				progressBarAccelerator.setProgress(percentAccelerator
						.intValue());

			}

		}
		if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) 
		sendMessage(percentLeft.intValue()+"L"+percentAccelerator.intValue()+"A"+percentRight.intValue()+"R");
		// progressBarLeft.invalidate();
		// txtViewRight.invalidate();
		// progressBarRight.invalidate();
		// txtViewLeft.invalidate();
		// txtViewAccelerator.invalidate();
		// progressBarAccelerator.invalidate();

		// sendMessage("50:60:90");
		// Log.d("TESTE",total.intValue()+":"+totalDireita.intValue()+":"+totalAcelerapercentLeftntValue());
		/*
		 * if (totalAceleracao.intValue() > 80) { sendEsquerda = 0; if
		 * (sendFrente == null || totalAceleracao.intValue() > sendFrente) {
		 * sendFrente = totalAceleracao.intValue();
		 * 
		 * sendMessage("f"); return; } if (percentLeft.intValue() > 80) {//
		 * ESQUERDA if (sendEsquerda == null || percentLeft.intValue() >
		 * sendEsquerda) { sendEsquerda = percentLeft.intValue();
		 * sendMessage("e"); } else {
		 * 
		 * sendFrente = 0; } } else if (totalDireita.intValue() > 80) {//
		 * DIREITA if (sendDireita == null || totalDireita.intValue() >
		 * sendDireita) { sendDireita = totalDireita.intValue();
		 * sendMessage("d"); } else {
		 * 
		 * sendFrente = 0; } } else { sendFrente = 0;
		 * 
		 * }
		 * 
		 * } else if (totalAceleracao.intValue() < 80 &&
		 * totalAceleracao.intValue() > 40) { sendMessage("p"); sendFrente = 0;
		 * 
		 * } else if (totalAceleracao.intValue() < 20) { sendMessage("r");
		 * sendFrente = 0; }
		 */

		// }
	}

	private Boolean isCalibrated() {
		if (maxLeft == null) {
			txtViewTitle
					.setText("Aguardando a Calibragem LEFT!\n Incline o controle para a Esquerda!!");

		} else if (maxRight == null) {
			txtViewTitle
					.setText("Aguardando a Calibragem RIGHT!\n Incline o controle para a Direita!!");
		} else if (maxAccelerator == null) {
			txtViewTitle
					.setText("Aguardando a Calibragem ACCELERATOR!\n Incline o controle para a Frente!!");
		} else {
			txtViewTitle
					.setText(mChatService.getState() == BluetoothChatService.STATE_CONNECTED ? "Dispositivos Conectados :)"
							: "Controle Calibrado! Emparelhe os Dispositivos!!!");
			return true;
		}

		return false;
	}

}