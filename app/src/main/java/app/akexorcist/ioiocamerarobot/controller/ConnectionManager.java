package app.akexorcist.ioiocamerarobot.controller;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.App;
import app.akexorcist.ioiocamerarobot.constant.Command;
import app.akexorcist.ioiocamerarobot.model.Location;
import app.akexorcist.ioiocamerarobot.model.OrientationValue;
import app.akexorcist.ioiocamerarobot.utils.AverageBitrate;

/**
 * Created by Akexorcist on 9/5/15 AD.
 */
public class ConnectionManager {
    @Inject
    Gson gson;

    private static final int PORT = 10083;
    private static final int TIMEOUT = 5000;
    private static final String LOG_TAG = ConnectionManager.class.getSimpleName();
    private Activity activity;
    private ConnectionListener connectionListener;
    private IOIOResponseListener responseListener;

    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;

    private Socket socket;
    private boolean isTaskRunning = false;

    private String ipAddress;
    private String password;

    private AverageBitrate averageBitrate;

    public ConnectionManager(Activity activity, String ipAddress, String password) {
        App.getAppComponent().inject(this);

        this.activity = activity;
        this.ipAddress = ipAddress;
        this.password = password;
        this.password = Command.TOKEN;

        averageBitrate = new AverageBitrate();
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void setResponseListener(IOIOResponseListener listener) {
        this.responseListener = listener;
    }

    public long getBitrate() {
        return averageBitrate.average();
    }

    public void start() {
        if (!isTaskRunning) {
            new Thread(readThread).start();
            isTaskRunning = true;
        }
    }

    private Runnable readThread = new Runnable() {
        public void run() {
            try {
                socket = new Socket();
                socket.connect((new InetSocketAddress(InetAddress.getByName(ipAddress), PORT)),
                        TIMEOUT);

                outputStream = socket.getOutputStream();
                dataOutputStream = new DataOutputStream(outputStream);

                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                int size = dataInputStream.readInt();
                byte[] buf = new byte[size];
                dataInputStream.readFully(buf);
                final String sourceIpListStr = new String(buf);
                if (sourceIpListStr.startsWith(Command.IP_LIST)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionListener.onSourcesIpList(sourceIpListStr
                                    .substring(Command.IP_LIST.length()));
                        }
                    });
                }

                if (sourceIpListStr.endsWith("[]")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionListener.onConnectionDown();
                        }
                    });
                    return;
                }
                size = dataInputStream.readInt();
                buf = new byte[size];
                dataInputStream.readFully(buf);
                String acceptStr = new String(buf);
                if (acceptStr.equalsIgnoreCase(Command.ACCEPT_CONNECTION)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionListener.onIOIOConnected();
                        }
                    });
                }

                size = dataInputStream.readInt();
                buf = new byte[size];
                dataInputStream.readFully(buf);
                String qualityStr = new String(buf);
                if (qualityStr.startsWith(Command.QUALITY_LIST)) {
                    responseListener.onPreviewSizesResponse(qualityStr
                            .substring(Command.QUALITY_LIST.length()));
                }

                size = dataInputStream.readInt();
                buf = new byte[size];
                dataInputStream.readFully(buf);
                String sizes = new String(buf);

                while (isTaskRunning) {
                    try {
                        size = dataInputStream.readInt();
                        final byte[] buffer = new byte[size];
                        dataInputStream.readFully(buffer);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String message = new String(buffer);
                                if (buffer.length > 0 && buffer.length < 20) {
                                    if (message.equalsIgnoreCase(Command.SNAP)) {
                                        if (responseListener != null)
                                            responseListener.onPictureTaken();
                                    } else if (message.equalsIgnoreCase(Command.WRONG_PASSWORD)) {
                                        if (connectionListener != null)
                                            connectionListener.onWrongPassword();
                                    } else if (message.equalsIgnoreCase(Command.ACCEPT_CONNECTION)) {
                                        if (connectionListener != null)
                                            connectionListener.onIOIOConnected();
                                    } else if (message.equalsIgnoreCase(Command.FLASH_UNAVAILABLE)) {
                                        if (responseListener != null)
                                            responseListener.onFlashUnavailable();
                                    }
                                } else if (buffer.length > 20) {
                                    if (new String(buffer).startsWith(Command.ORIENTATION)) {
//                                        Log.d(LOG_TAG, "Orientation: " + message
//                                                .substring(Command.ORIENTATION.length()));
                                        OrientationValue orientationValue = gson.fromJson(message
                                                .substring(Command.ORIENTATION.length()),
                                                OrientationValue.class);
                                        responseListener.onOrientationIncoming(orientationValue);
                                    }
                                    else if (new String(buffer).startsWith(Command.LOCATION)) {
                                        Log.d(LOG_TAG, "Location: " + message
                                                .substring(Command.LOCATION.length()));
                                               Location location =  gson
                                                       .fromJson(message
                                                               .substring(Command.LOCATION.length()),
                                                               Location.class);
                                               responseListener.onLocationIncoming(location);
                                        Log.d(LOG_TAG, "Location: " + location);
                                    }
                                    else if (responseListener != null) {
                                        averageBitrate.push(buffer.length);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                                        responseListener.onCameraImageIncoming(bitmap);
                                    }
                                }
                            }
                        });
                    } catch (EOFException e) {
//                        e.printStackTrace();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (connectionListener != null)
                                    connectionListener.onConnectionDown();
                            }
                        });
                        isTaskRunning = false;
                    } catch (NumberFormatException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (NumberFormatException | UnknownHostException e) {
//                e.printStackTrace();
            } catch (IOException e) {
//                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectionListener != null)
                            connectionListener.onConnectionFailed();
                    }
                });
            }
        }
    };

    public interface ConnectionListener {

        void onConnectionDown();

        void onConnectionFailed();

        void onWrongPassword();

        void onIOIOConnected();

        void onSourcesIpList(String ipListStr);
    }

    public interface IOIOResponseListener {

        void onPictureTaken();

        void onFlashUnavailable();

        void onCameraImageIncoming(Bitmap bitmap);

        void onLocationIncoming(Location location);

        void onOrientationIncoming(OrientationValue value);

        void onPreviewSizesResponse(String previewSizesStr);
    }

    public void stop() {
        isTaskRunning = false;
        try {
            socket.close();
            outputStream.close();
            dataOutputStream.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String str) {
        try {
            Log.d(LOG_TAG, "command: " + str);
            dataOutputStream.writeInt(str.length());
            dataOutputStream.write(str.getBytes());
            outputStream.flush();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendMovement(final String str) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(LOG_TAG, "send movement: " + str);
                    byte[] data = str.getBytes();
                    DatagramSocket datagramSocket = new DatagramSocket();
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, PORT);
                    datagramSocket.send(datagramPacket);
                    datagramSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
