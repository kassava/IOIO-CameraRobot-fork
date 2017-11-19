package app.akexorcist.ioiocamerarobot.ioio;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import app.akexorcist.ioiocamerarobot.constant.Command;

/**
 * Created by Akexorcist on 9/5/15 AD.
 */
public class ConnectionManager {

    private static final String LOG_TAG = ConnectionManager.class.getSimpleName();

    private ConnectionListener connectionListener;
    private ControllerCommandListener commandListener;
    private SendCommandListener sendListener;
    private OutputStream out;
    private DataOutputStream dos;
    private IOIOService ioio;
    private String ipAddress;

    public ConnectionManager(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setConnectionListener(ConnectionListener listener) {
        connectionListener = listener;
    }

    public void setCommandListener(ControllerCommandListener listener) {
        commandListener = listener;
    }

    public void setSendCommandListener(SendCommandListener sendListener) {
        this.sendListener = sendListener;
    }

    public void start() {
        ioio = new IOIOService(mHandler, ipAddress);
        ioio.execute();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            onDataIncoming();
            switch (msg.what) {
                case Command.MOVE_COMMAND:
                    onMoveCommand((String) msg.obj);
                    break;
                case Command.MESSAGE_PASS:
                    onControllerConnected((Socket) msg.obj);
                    break;
                case Command.MESSAGE_WRONG:
                    onControllerPasswordWrong((Socket) msg.obj);
                    break;
                case Command.MESSAGE_DISCONNECTED:
                    onControllerDisconnected();
                    break;
                case Command.MESSAGE_CLOSE:
                    onControllerClosed();
                    break;
                case Command.MESSAGE_FLASH:
                    onFlashCommand(msg.obj.toString());
                    break;
                case Command.MESSAGE_SNAP:
                    onRequestTakePicture();
                    break;
                case Command.MESSAGE_FOCUS:
                    onRequestAutoFocus();
                    break;
                case Command.MESSAGE_UP:
                    onMoveForwardCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_UPRIGHT:
                    onMoveForwardRightCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_UPLEFT:
                    onMoveForwardLeftCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_DOWN:
                    onMoveBackwardCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_DOWNRIGHT:
                    onMoveBackwardRightCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_DOWNLEFT:
                    onMoveBackwardLeftCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_RIGHT:
                    onMoveRightCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_LEFT:
                    onMoveLeftCommand((Integer) msg.obj);
                    break;
                case Command.MESSAGE_STOP:
                    onMoveStopCommand();
                    break;
                case Command.MESSAGE_QUALITY:
                    onChangeQuality(msg.obj);
                    break;
                case Command.MESSAGE_STOP_PREVIEW:
                    stopPreview();
                    break;
            }
        }
    };

    private void onMoveCommand(String command) {
        if (connectionListener != null) {
            connectionListener.onMoveCommandIncoming(command);
        }
    }

    private void onDataIncoming() {
        if (connectionListener != null)
            connectionListener.onDataIncoming();
    }

    private void onChangeQuality(Object obj) {
        String str  = (String) obj;
        if (connectionListener != null) {
            connectionListener.onChangeQuality(str);
        }
    }

    private void onControllerConnected(Socket socket) {
        try {
            out = socket.getOutputStream();
            dos = new DataOutputStream(out);
            if (connectionListener != null)
                connectionListener.onControllerConnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onControllerPasswordWrong(Socket socket) {
        try {
            out = socket.getOutputStream();
            dos = new DataOutputStream(out);
            restart();
            if (connectionListener != null)
                connectionListener.onWrongPassword();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onControllerDisconnected() {
        restart();
        if (connectionListener != null)
            connectionListener.onControllerDisconnected();
    }

    void onControllerClosed() {
        restart();
        if (connectionListener != null)
            connectionListener.onControllerClosed();
    }

    private void onFlashCommand(String command) {
        if (commandListener != null)
            commandListener.onFlashCommand(command);
    }

    private void onRequestTakePicture() {
        if (commandListener != null)
            commandListener.onRequestTakePicture();
    }

    private void onRequestAutoFocus() {
        if (commandListener != null)
            commandListener.onRequestAutoFocus();
    }

    private void stopPreview() {
        if (commandListener != null) {
            commandListener.onStopPreview();
        }
    }

    private void onMoveForwardCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardCommand(speed);
    }

    private void onMoveForwardRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardRightCommand(speed);
    }

    private void onMoveForwardLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardLeftCommand(speed);
    }

    private void onMoveBackwardCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardCommand(speed);
    }

    private void onMoveBackwardRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardRightCommand(speed);
    }

    private void onMoveBackwardLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardLeftCommand(speed);
    }

    private void onMoveRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveRightCommand(speed);
    }

    private void onMoveLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveLeftCommand(speed);
    }

    private void onMoveStopCommand() {
        if (commandListener != null)
            commandListener.onMoveStopCommand();
    }

    void stop() {
        if (ioio != null)
            ioio.killTask();
    }

    void restart() {
        stop();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                start();
            }
        }, 1000);
    }

    void sendImageData(byte[] data) {
        try {
            dos.writeInt(data.length);
            dos.write(data);
            out.flush();
            if (sendListener != null)
                sendListener.onSendCommandSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            if (sendListener != null)
                sendListener.onSendCommandFailure();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    void sendPreviewSizes(String str) {
        try {
            dos.writeInt(str.length());
            dos.write(str.getBytes());
            out.flush();
            if (sendListener != null)
                sendListener.onSendPreviewSizesSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            if (sendListener != null)
                sendListener.onSendCommandFailure();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    void sendCommand(String str) {
        try {
            dos.writeInt(str.length());
            dos.write(str.getBytes());
            out.flush();
            if (sendListener != null)
                sendListener.onSendCommandSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            if (sendListener != null)
                sendListener.onSendCommandFailure();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendLocation(String str) {
        try {
            Log.d(LOG_TAG, "telemetry: " + str );
            dos.writeInt(Command.LOCATION.length());
            dos.write(Command.LOCATION.getBytes());
            dos.writeInt(str.length());
            dos.write(str.getBytes());
            out.flush();
            if (sendListener != null)
                sendListener.onSendTelemetry();
        } catch (IOException e) {
            e.printStackTrace();
            if (sendListener != null)
                sendListener.onSendTelemetryFailure();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendOrientation(String str) {
        try {
            Log.d(LOG_TAG, "telemetry: " + str );
            dos.writeInt(Command.ORIENTATION.length());
            dos.write(Command.ORIENTATION.getBytes());
            dos.writeInt(str.length());
            dos.write(str.getBytes());
            out.flush();
            if (sendListener != null)
                sendListener.onSendTelemetry();
        } catch (IOException e) {
            e.printStackTrace();
            if (sendListener != null)
                sendListener.onSendTelemetryFailure();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public interface ConnectionListener {

        void onControllerConnected();

        void onWrongPassword();

        void onControllerDisconnected();

        void onControllerClosed();

        void onDataIncoming();

        void onChangeQuality(String string);

        void onMoveCommandIncoming(String command);
    }

    public interface ControllerCommandListener {

        void onFlashCommand(String command);

        void onRequestTakePicture();

        void onRequestAutoFocus();

        void onStopPreview();

        void onMoveForwardCommand(int speed);

        void onMoveForwardRightCommand(int speed);

        void onMoveForwardLeftCommand(int speed);

        void onMoveBackwardCommand(int speed);

        void onMoveBackwardRightCommand(int speed);

        void onMoveBackwardLeftCommand(int speed);

        void onMoveLeftCommand(int speed);

        void onMoveRightCommand(int speed);

        void onMoveStopCommand();
    }

    public interface SendCommandListener {

        void onSendCommandSuccess();

        void onSendCommandFailure();

        void onSendPreviewSizesSuccess();

        void onSendTelemetry();

        void onSendTelemetryFailure();
    }
}
