package app.akexorcist.ioiocamerarobot.ioio;

import android.os.Handler;
import android.os.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import app.akexorcist.ioiocamerarobot.constant.Command;

/**
 * Created by Akexorcist on 9/5/15 AD.
 */
public class ConnectionManager {

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

    public void onDataIncoming() {
        if (connectionListener != null)
            connectionListener.onDataIncoming();
    }

    public void onChangeQuality(Object obj) {
        String str  = (String) obj;
        if (connectionListener != null) {
            connectionListener.onChangeQuality(str);
        }
    }

    public void onControllerConnected(Socket socket) {
        try {
            out = socket.getOutputStream();
            dos = new DataOutputStream(out);
            if (connectionListener != null)
                connectionListener.onControllerConnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onControllerPasswordWrong(Socket socket) {
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

    public void onControllerDisconnected() {
        restart();
        if (connectionListener != null)
            connectionListener.onControllerDisconnected();
    }

    public void onControllerClosed() {
        restart();
        if (connectionListener != null)
            connectionListener.onControllerClosed();
    }

    public void onFlashCommand(String command) {
        if (commandListener != null)
            commandListener.onFlashCommand(command);
    }

    public void onRequestTakePicture() {
        if (commandListener != null)
            commandListener.onRequestTakePicture();
    }

    public void onRequestAutoFocus() {
        if (commandListener != null)
            commandListener.onRequestAutoFocus();
    }

    public void stopPreview() {
        if (commandListener != null) {
            commandListener.onStopPreview();
        }
    }

    public void onMoveForwardCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardCommand(speed);
    }

    public void onMoveForwardRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardRightCommand(speed);
    }

    public void onMoveForwardLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveForwardLeftCommand(speed);
    }

    public void onMoveBackwardCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardCommand(speed);
    }

    public void onMoveBackwardRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardRightCommand(speed);
    }

    public void onMoveBackwardLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveBackwardLeftCommand(speed);
    }

    public void onMoveRightCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveRightCommand(speed);
    }

    public void onMoveLeftCommand(int speed) {
        if (commandListener != null)
            commandListener.onMoveLeftCommand(speed);
    }

    public void onMoveStopCommand() {
        if (commandListener != null)
            commandListener.onMoveStopCommand();
    }

    public void stop() {
        if (ioio != null)
            ioio.killTask();
    }

    public void restart() {
        stop();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                start();
            }
        }, 1000);
    }

    public void sendImageData(byte[] data) {
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

    public void sendPreviewSizes(String str) {
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

    public void sendCommand(String str) {
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

    public interface ConnectionListener {

        void onControllerConnected();

        void onWrongPassword();

        void onControllerDisconnected();

        void onControllerClosed();

        void onDataIncoming();

        void onChangeQuality(String string);
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
    }
}
