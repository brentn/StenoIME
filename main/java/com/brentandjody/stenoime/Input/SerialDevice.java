package com.brentandjody.stenoime.Input;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;


import java.io.IOException;

/**
 * Created by brentn on 26/11/13.
 * Implements serial communication protocol over USB
 */
public abstract class SerialDevice {

    public static final String TAG = "StenoIME";
    public static enum STATE {DISCONNECTED, INITIALIZING, CONNECTED, ERROR}
    public static final int USB_TIMEOUT=5000; //milliseconds
    public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
    private static final int MODEM_STATUS_HEADER_LENGTH = 2;  // transmitted with every read


    private OnStateChangeListener onStateChangeListener;
    private STATE mState;

    protected final UsbDevice mDevice;
    protected final UsbDeviceConnection mConnection;
    protected final Object mReadBufferLock = new Object();
    protected final Object mWriteBufferLock = new Object();
    protected byte[] mReadBuffer;
    protected byte[] mWriteBuffer;

    public SerialDevice(UsbDevice device, UsbDeviceConnection connection) {
        mDevice=device;
        mConnection=connection;
        mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
        mState = STATE.DISCONNECTED;
    }

    public final UsbDevice getDevice() {
        return mDevice;
    }

    public interface OnStateChangeListener {
        public void onStateChange(String state);
    }

    public void reset() throws IOException {
        int result = (mConnection.controlTransfer(UsbConstants.USB_TYPE_VENDOR, 0, 0, 0, null, 0, USB_TIMEOUT));
        //int result = (mConnection.controlTransfer(0x00000080, 0x03, 0x4138, 0, null, 0, 0)); // read at 9600 baud
        if (result !=0 ) {
            Log.e(TAG, "serial reset failed ("+result+")");
            throw new IOException("Reset Failed: "+result);
        }
    }

    public boolean connect() throws IOException {
        mState=STATE.DISCONNECTED;
        try {
            for (int i=0; i<mDevice.getInterfaceCount(); i++) {
                if (mConnection.claimInterface(mDevice.getInterface(i), true)) {
                    Log.d(TAG, "serial device connected to interface: " + i);
                    mState = STATE.CONNECTED;
                } else {
                    throw new IOException("Error connecting to interface: " + i);
                }
                reset();
            }
        } finally {
            if (mState != STATE.CONNECTED)
                disconnect();
        }
        return (mState==STATE.CONNECTED);
    }

    public void disconnect() throws IOException {
        if (mConnection!=null) mConnection.close();
        mState=STATE.DISCONNECTED;
    }

    public int read(final byte[] data, final int timeout) throws IOException {
        //TODO: score the real interface and endpoint
        final UsbEndpoint endpoint = mDevice.getInterface(0).getEndpoint(0);
        final int totalBytesRead;
        synchronized (mReadBufferLock) {
            final int readSize = Math.min(data.length, mReadBuffer.length);
            totalBytesRead = mConnection.bulkTransfer(endpoint, mReadBuffer, readSize, timeout);
            if (totalBytesRead < MODEM_STATUS_HEADER_LENGTH) {
                throw new IOException("Expected at least " + MODEM_STATUS_HEADER_LENGTH + " bytes");
            }
            return filterStatusBytes(mReadBuffer, data, totalBytesRead, endpoint.getMaxPacketSize());
        }
    }

    public int write(final byte[] data, final int timeout) throws IOException {
        //TODO: score the real interface and endpoint
        final UsbEndpoint endpoint = mDevice.getInterface(0).getEndpoint(1);
        int offset = 0;
        while (offset < data.length) {
            final int write_length;
            final int result;
            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;
                write_length = Math.min(data.length-offset, mWriteBuffer.length);
                if (offset == 0) {
                    writeBuffer = data;
                } else {
                    System.arraycopy(data, offset, mWriteBuffer, 0, write_length);
                    writeBuffer = mWriteBuffer;
                }
                result = mConnection.bulkTransfer(endpoint, writeBuffer, write_length, timeout);
            }
            if (result <=0) {
                throw new IOException("Error writing " + write_length + " bytes at offset " + offset + " length="+data.length);
            }
            Log.d(TAG, "Wrote " + result + " bytes.  Attempted=" + write_length);
            offset += result;
        }
        return offset;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        onStateChangeListener = listener;
    }

    public String getState() {
        return mState.name();
    }

    private int filterStatusBytes(byte[] in, byte[] out, int totalBytes, int maxPacketSize) {
        final int number_of_packets = totalBytes/maxPacketSize + 1;
        for (int i=0; i<number_of_packets; i++) {
            final int count = (i == (number_of_packets - 1))?
                    (totalBytes % maxPacketSize) - MODEM_STATUS_HEADER_LENGTH:
                    maxPacketSize - MODEM_STATUS_HEADER_LENGTH;
            if (count > 0) {
                System.arraycopy(in, i * maxPacketSize + MODEM_STATUS_HEADER_LENGTH,
                        out, i * (maxPacketSize - MODEM_STATUS_HEADER_LENGTH), count);
            }
        }
        return totalBytes-(number_of_packets * 2);
    }
}
