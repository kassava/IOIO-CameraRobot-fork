package app.akexorcist.ioiocamerarobot.ioio;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import app.akexorcist.ioiocamerarobot.constant.Command;

public class IOIOService extends AsyncTask<Void, Void, Void> {

    private static final String LOG_TAG = IOIOService.class.getSimpleName();
    private static final int PORT = 10082;
    private static final int TIMEOUT = 3000;

    private boolean isTaskRunning = true;
    private Socket socket;
    private String ipAddress;

    private DataInputStream dataInputStream;
    private InputStream inputStream;
    private Handler handler;

    public IOIOService(Handler handler, String ipAddress) {
        this.handler = handler;
        this.ipAddress = ipAddress;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected Void doInBackground(Void... params) {
        Runnable run = new Runnable() {
            public void run() {

                try {
                    byte[] message = new byte[10];
                    DatagramPacket datagramPacket = new DatagramPacket(message, message.length);
                    DatagramSocket datagramSocket = new DatagramSocket(null);
                    datagramSocket.setReuseAddress(true);
                    datagramSocket.setBroadcast(true);
                    datagramSocket.bind(new InetSocketAddress(PORT));

                    while (isTaskRunning) {
                        try {
                            datagramSocket.setSoTimeout(TIMEOUT);
                            datagramSocket.receive(datagramPacket);
                            String text = new String(message, 0, datagramPacket.getLength());
                            Log.d(LOG_TAG, "udp command: " + text);
                            RXBus.get().sendEvent(text);
                        } catch (SocketTimeoutException e) {
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    datagramSocket.close();
                    Log.e(LOG_TAG, "Kill Task");
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(run).start();

        try {
            while (socket == null && isTaskRunning) {
                try {
                    socket = new Socket();
                    socket.connect((new InetSocketAddress(InetAddress.getByName(ipAddress), PORT)), TIMEOUT);
                } catch (Exception e) {
                    handler.obtainMessage(Command.MESSAGE_STOP).sendToTarget();
                    isTaskRunning = false;
                }
            }

            if (isTaskRunning) {
                inputStream = socket.getInputStream();
                dataInputStream = new DataInputStream(inputStream);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        while (isTaskRunning) {
            try {
                int size = dataInputStream.readInt();
                byte[] buffer = new byte[size];
                dataInputStream.readFully(buffer);
                String data = new String(buffer);
                String subStr = data.substring(0, Command.QUALITY.length());

                if (data.equalsIgnoreCase(Command.SNAP)) {
                    handler.obtainMessage(Command.MESSAGE_SNAP).sendToTarget();
                } else if (data.equalsIgnoreCase(Command.LED_ON) || data.equalsIgnoreCase(Command.LED_OFF)) {
                    handler.obtainMessage(Command.MESSAGE_FLASH, data).sendToTarget();
                } else if (data.equalsIgnoreCase(Command.FOCUS)) {
                    handler.obtainMessage(Command.MESSAGE_FOCUS).sendToTarget();
                } else if (subStr.equalsIgnoreCase(Command.QUALITY)) {
                    handler.obtainMessage(Command.MESSAGE_QUALITY, data).sendToTarget();
                } else if (data.equalsIgnoreCase(Command.TOKEN)) {
                    handler.obtainMessage(Command.MESSAGE_PASS, socket).sendToTarget();
                } else if (data.equalsIgnoreCase(Command.STOP_PREVIEW)) {
                    handler.obtainMessage(Command.MESSAGE_STOP_PREVIEW).sendToTarget();
                }
            } catch (EOFException e) {
                e.printStackTrace();
                handler.obtainMessage(Command.MESSAGE_CLOSE).sendToTarget();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!socket.isConnected()) {
//                isTaskRunning = false;
                handler.obtainMessage(Command.MESSAGE_DISCONNECTED).sendToTarget();
            }
        }
        try {
            socket.close();
            inputStream.close();
            dataInputStream.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        Log.e(LOG_TAG, "Service was killed");
        return null;
    }

    void killTask() {
        isTaskRunning = false;

        try {
            socket.close();
            inputStream.close();
            dataInputStream.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
