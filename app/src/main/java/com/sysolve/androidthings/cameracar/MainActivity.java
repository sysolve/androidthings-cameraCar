package com.sysolve.androidthings.cameracar;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.sysolve.androidthings.utils.BoardSpec;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    Gpio mButtonGpio = null;

    private Pwm pwmLeft;
    private Pwm pwmRight;

    Gpio left0;
    Gpio left1;
    Gpio right0;
    Gpio right1;
    Gpio stop;

    private static double PWM_FREQUENCY_HZ = 100000;

    private static final int SPEED_NORMAL = 100;
    private static final int SPEED_TURNING_INSIDE = 70;
    private static final int SPEED_TURNING_OUTSIDE = 250;

    int buttonPressedTimes = 0;

    private HandlerThread mInputThread;
    private Handler mInputHandler;

    Gpio echoDeviceEn;

    private Runnable mTransferUartRunnable = new Runnable() {
        @Override
        public void run() {
            transferUartData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PeripheralManagerService service = new PeripheralManagerService();
        try {
            // Create a background looper thread for I/O
            mInputThread = new HandlerThread("InputThread");
            mInputThread.start();
            mInputHandler = new Handler(mInputThread.getLooper());

            // Attempt to access the UART device
            try {
                openUart(service, BoardSpec.getUartName(), 9600);
                // Read any initially buffered data
                mInputHandler.post(mTransferUartRunnable);
            } catch (IOException e) {
                Log.e(TAG, "Unable to open UART device", e);
            }

            pwmLeft = service.openPwm(BoardSpec.getInstance().getPwm(0));
            pwmRight = service.openPwm(BoardSpec.getInstance().getPwm(1));

            echoDeviceEn = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_18));
            echoDeviceEn.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            left0 = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_29));
            left1 = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_31));
            left0.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            left1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            right0 = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_35));
            right1 = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_37));
            right0.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            right1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            stop = service.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_16));
            stop.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            pwmLeft.setPwmDutyCycle(50);       //percent, 0-100
            pwmLeft.setPwmFrequencyHz(PWM_FREQUENCY_HZ);
            pwmLeft.setEnabled(true);

            pwmRight.setPwmDutyCycle(50);
            pwmRight.setPwmFrequencyHz(PWM_FREQUENCY_HZ);
            pwmRight.setEnabled(true);

            //define a button for counter
            mButtonGpio = service.openGpio(BoardSpec.getGoogleSampleButtonGpioPin());
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    buttonPressedTimes++;

                    Log.i(TAG, "GPIO changed, button pressed "+ buttonPressedTimes);
                    emStop();

                    // Return true to continue listening to events
                    return true;
                }
            });

            new HttpServer(this);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }


    }

    public void leftForward() {
        try {
            left0.setValue(false);
            left1.setValue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void leftBack() {
        try {
            left0.setValue(true);
            left1.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void rightForward() {
        try {
            right0.setValue(false);
            right1.setValue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void rightBack() {
        try {
            right0.setValue(true);
            right1.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void go() {
        try {
            stop.setValue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void emStop() {
        try {
            stop.setValue(false);
            lrdSpeed = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean mKeyPressed;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_A) { //29
            if (!mKeyPressed) {
                mKeyPressed = true;
                mResetHandler.postDelayed(mDisconnectRunnable, DISCONNECT_DELAY);
                mResetHandler.postDelayed(mResetRunnable, RESET_DELAY);
            }
            return true;
        }*/
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_A) { //29
            mKeyPressed = false;
            // No effect if these have already run
            mResetHandler.removeCallbacks(mDisconnectRunnable);
            mResetHandler.removeCallbacks(mResetRunnable);
            return true;
        }*/
        return handleKeyCode(keyCode) || super.onKeyUp(keyCode, event);
    }

    // For testing commands to the motors via ADB.
    private boolean handleKeyCode(int keyCode) {
        //if (mCarController != null) {
        Log.i(TAG,"Key Press "+keyCode);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: //19
                    goForward();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: //20
                    goBack();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: //21
                    turnLeft();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: //22
                    turnRight();
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER: //23
                    emStop();
                    return true;
            }
        //}
        return false;
    }

    public int lrdSpeed = 0;

    public void turnLeft() {
        //leftBack();
        //rightForward();
        lrdSpeed -= 2;
        try {
            pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
            pwmRight.setPwmDutyCycle(speed - lrdSpeed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        go();
    }

    public void turnRight() {
        lrdSpeed += 2;
        //leftForward();
        //rightBack();
        try {
            pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
            pwmRight.setPwmDutyCycle(speed - lrdSpeed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        go();
    }


    public void goBack() {
        leftBack();
        rightBack();
        go();
    }

    public void goForward() {
        leftForward();
        rightForward();
        go();
    }

    double speed = 50;

    public void speedDecrease() {
        if (speed>10) {
            speed = speed - 5;
            try {
                pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
                pwmRight.setPwmDutyCycle(speed - lrdSpeed);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void speedAdd() {
        if (speed<100) {
            speed = speed + 5;
            try {
                pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
                pwmRight.setPwmDutyCycle(speed - lrdSpeed);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private UartDevice echoDevice;

    private void openUart(PeripheralManagerService service, String name, int baudRate) throws IOException {
        echoDevice = service.openUartDevice(name);
        // Configure the UART
        echoDevice.setBaudrate(baudRate);
        echoDevice.setDataSize(8);
        echoDevice.setParity(UartDevice.PARITY_NONE);
        echoDevice.setStopBits(1);

        echoDevice.registerUartDeviceCallback(mCallback, mInputHandler);
    }

    /**
     * Close the UART device connection, if it exists
     */
    private void closeUart() throws IOException {
        if (echoDevice != null) {
            echoDevice.unregisterUartDeviceCallback(mCallback);
            try {
                echoDevice.close();
            } finally {
                echoDevice = null;
            }
        }
    }

    /**
     * Callback invoked when UART receives new incoming data.
     */
    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            // Queue up a data transfer
            transferUartData();
            //Continue listening for more interrupts
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    public static int CHUNK_SIZE = 128;

    public boolean echo_prefix;
    public int echoHigh;
    public int echoLow;
    public int echoCRC;

    private static byte[] prefix = "IoT".getBytes();
    /**
     * Loop over the contents of the UART RX buffer, transferring each
     * one back to the TX buffer to create a loopback service.
     *
     * Potentially long-running operation. Call from a worker thread.
     */
    private void transferUartData() {
        if (echoDevice != null) {
            // Loop until there is no more data in the RX buffer.
            try {
                byte[] buffer = new byte[CHUNK_SIZE];
                int read;

                while ((read = echoDevice.read(buffer, buffer.length)) > 0) {
                    //mLoopbackDevice.write(prefix, prefix.length);
                    //mLoopbackDevice.write(buffer, read);
                    for (int i=0;i<read;) {
                        if (buffer[i]==(byte)0xFF) {
                            if (i+3<read) {
                                if ((( (0|buffer[i]) + (0|buffer[i+1]) + (0|buffer[i+2]) ) & 0xFF) == (0| buffer[i+3])) {
                                    int distance = (0|buffer[i + 1]) * 256 + (0|buffer[i+2]);
                                    if (distance>0 && distance<100)
                                        emStop();
                                    Log.i(TAG, "distance="+distance+"mm");
                                } else {
                                    Log.i(TAG, "distance CRC Error");
                                }
                                i+=4;
                            } else {
                                break;
                            }
                        } else {
                            i++;
                        }
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            left0.close();
            left1.close();
            right0.close();
            right1.close();
            echoDeviceEn.close();
            stop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            pwmLeft.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        try {
            pwmRight.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (mButtonGpio!=null) try {
            mButtonGpio.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        // Terminate the worker thread
        if (mInputThread != null) {
            mInputThread.quitSafely();
        }

        // Attempt to close the UART device
        try {
            closeUart();
        } catch (IOException e) {
            Log.e(TAG, "Error closing UART device:", e);
        }
    }

}
