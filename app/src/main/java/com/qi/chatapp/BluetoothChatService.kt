package com.example.bluetoothmessaging

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.os.Bundle
import android.os.Handler
import java.util.*

class BluetoothChatService(context: Context, handler: Handler){

    private var mAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int = 0
    private var mNewState: Int = 0

    private val  TAG: String = javaClass.simpleName

    private val MY_UUID_SECURE = UUID.fromString("29621b37-e817-485a-a258-52da5261421a")
    private val MY_UUID_INSECURE = UUID.fromString("d620cd2b-e0a4-435b-b02e-40324d57195b")

    private val NAME_SECURE = "BluetoothChatSecure"
    private val NAME_INSECURE = "BluetoothChatInsecure"

    companion object {
        val STATE_NONE = 0       // we're doing nothing
        val STATE_LISTEN = 1     // now listening for incoming connections
        val STATE_CONNECTING = 2 // now initiating an outgoing connection
        val STATE_CONNECTED = 3  // now connected to a remote device
    }

    init {

        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mNewState = mState
        mHandler = handler
    }

    @Synchronized fun getState(): Int {
        return mState
    }

    @Synchronized fun start() {
        Log.d(TAG, "start")


        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread?.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread?.start()
        }
        //updateUserInterfaceTitle()
    }


    @Synchronized fun connect(device: BluetoothDevice?, secure: Boolean) {

        Log.d(TAG, "connect to: $device")

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread?.cancel()
                mConnectThread = null
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.start()

        //updateUserInterfaceTitle()
    }


    @Synchronized fun connected(socket: BluetoothSocket?, device: BluetoothDevice?, socketType: String) {
        Log.d(TAG, "connected, Socket Type:$socketType")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }

        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        val msg = mHandler?.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device?.name)
        msg?.data = bundle
        mHandler?.sendMessage(msg)
        //updateUserInterfaceTitle()
    }


    @Synchronized fun stop() {
        Log.d(TAG, "stop")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }
        mState = STATE_NONE
        //updateUserInterfaceTitle()
    }


    fun write(out: ByteArray) {

        var r: ConnectedThread?  = null

        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        r?.write(out)
    }



    private fun connectionFailed() {
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE

        //updateUserInterfaceTitle()

        this@BluetoothChatService.start()
    }


    private fun connectionLost() {

        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE
       // updateUserInterfaceTitle()

        this@BluetoothChatService.start()
    }

    private inner class AcceptThread(secure: Boolean) : Thread() {

        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
                if (secure) {
                    tmp = mAdapter?.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE)
                } else {
                    tmp = mAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        override fun run() {

            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this)
            name = "AcceptThread$mSocketType"

            var socket: BluetoothSocket?

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket?.accept()
                }

                catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized(this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                connected(socket, socket.remoteDevice, mSocketType)
                            STATE_NONE, STATE_CONNECTED ->
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }

                            else -> {
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: $mSocketType")

        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }

        }
    }


    private inner class ConnectThread(private val mmDevice: BluetoothDevice?, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
                if (secure) {
                    tmp = mmDevice?.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE)
                } else {
                    tmp = mmDevice?.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }

            mmSocket = tmp
            mState = STATE_CONNECTING
        }

        override fun run() {

            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            mAdapter?.cancelDiscovery()

            try {
                mmSocket?.connect()

            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2)
                }

                connectionFailed()
                return
            }

            synchronized(this@BluetoothChatService) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }

        }
    }


    private inner class ConnectedThread(private val mmSocket: BluetoothSocket?, socketType: String) : Thread() {

        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket?.inputStream
                tmpOut = mmSocket?.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream?.read(buffer) ?: 0

                    // Send the obtained bytes to the UI Activity
                    mHandler?.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            ?.sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }

            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)

                // Share the sent message back to the UI Activity
                mHandler?.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        ?.sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }

        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }
}