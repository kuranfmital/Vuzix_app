package com.example.myapplication

import android.os.Bundle
import android.os.SystemClock
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.os.Handler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.nio.charset.Charset
import java.util.UUID;

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.R.attr.start
//import com.sun.xml.internal.ws.streaming.XMLStreamWriterUtil.getOutputStream
import androidx.core.app.ActivityCompat.startActivityForResult
import android.content.Intent




class MainActivity : AppCompatActivity() {
    protected lateinit var mBluetoothAdapter: BluetoothAdapter;
    protected lateinit var mmSocket: BluetoothSocket;
    protected lateinit var mmDevice: BluetoothDevice;
    protected lateinit var mmOutputStream: OutputStream;
    protected lateinit var mmInputStream: InputStream;
    @Volatile
    var stopWorker: Boolean = false
    protected lateinit var workerThread: Thread;
    protected lateinit var readBuffer: ByteArray;
    protected lateinit var readBuffer1: ByteArray;
    var readBufferPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            val originalText = textDisplay.text.toString()
            val newText = " "
            textDisplay.text = newText
            Snackbar.make(view, "Text updated", Snackbar.LENGTH_LONG)
                .show()

        }
        try
        {
            findBT();
            openBT();
        }
        catch (ie: IOException) {
            System.out.println("Error establishing connection")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun findBT() {
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (mBluetoothAdapter == null) {
        var message = "No bluetooth adapter available"
        textDisplay.setText(message)
    }

    if (!mBluetoothAdapter.isEnabled()) {
        val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetooth, 0)
    }

    val pairedDevices = mBluetoothAdapter.getBondedDevices()
    if (pairedDevices.size > 0) {
        System.out.println("Multiple devices found")
        for (device in pairedDevices) {
            if (device.getName() == "Galaxy S6") {
                System.out.println("Device: " + device.name)
                textDisplay.setText("Connected to " + device.name)
                mmDevice = device
                break
            }
        }
    }
    textDisplay.setText("Bluetooth Device Found")
}

    @Throws(IOException::class)
    fun openBT() {
        //val uuid = mmDevice.uuids[6].uuid //3 is contacts, 6 is messages
        //System.out.println("UUID: " + uuid)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid)
        try {
            mmSocket.connect()
            System.out.println("Connected")
        } catch (ie: IOException) {
            System.out.println("Could not establish connection")
        }

        //mmOutputStream = mmSocket.outputStream
        mmInputStream = mmSocket.inputStream

        beginListenForData()

        textDisplay.setText("Bluetooth Opened")
    }

fun beginListenForData() {
    val handler = Handler()
    val delimiter: Byte = 36 //This is the ASCII code for a newline character

    stopWorker = false
    readBufferPosition = 0
    readBuffer = ByteArray(1024)
    readBuffer1 = ByteArray(1024)
    workerThread = Thread(Runnable {
        System.out.println("About to start receiving data")
        while (!Thread.currentThread().isInterrupted && !stopWorker)
            try {
                val bytesAvailable = mmInputStream.available()
                if (bytesAvailable > 0) {
                    System.out.println("bytesAvailable is > 0")
                    val packetBytes = ByteArray(bytesAvailable)
                    System.out.println("packetBytes: " + packetBytes)
                    mmInputStream.read(packetBytes)
                    System.out.println("Reading: " + mmInputStream.read(packetBytes) )
                    for (i in 0 until bytesAvailable) {
                        //System.out.println("I = " + i + " and bytesAvailable = " + bytesAvailable)
                        val b = packetBytes[i]
                        //System.out.println("b = " + b + " and delimeter = " + delimiter)
                        if (b == delimiter) {
                            val encodedBytes = ByteArray(readBufferPosition)
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.size)
                            val char: Charset = Charset.forName("US-ASCII")
                            val data = String(encodedBytes, char)
                            val displayedText = textDisplay.text.toString()
                            val newText: String = displayedText + data
                            System.out.println("Receiving Data: " + data)
                            readBufferPosition = 0
                            handler.post(Runnable { textDisplay.setText(data) })
                        } else {
                            readBuffer[readBufferPosition++] = b
                        }
                    }
                } else {
                    SystemClock.sleep(100);
                }
            } catch (ex: IOException) {
                stopWorker = true
            }
    })

    workerThread.start()
}

    @Throws(IOException::class)
    fun sendData() {
        var msg = textDisplay.getText().toString()
        msg += "\n"
        mmOutputStream.write(msg.toByteArray())
        textDisplay.setText("Data Sent")
    }

    @Throws(IOException::class)
    fun closeBT() {
        stopWorker = true
        mmOutputStream.close()
        mmInputStream.close()
        mmSocket.close()
        textDisplay.setText("Bluetooth Closed")
    }

}
