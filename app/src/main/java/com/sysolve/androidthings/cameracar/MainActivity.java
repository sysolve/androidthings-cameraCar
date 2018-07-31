package com.sysolve.androidthings.cameracar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.sysolve.androidthings.contrib.driver.ws2812b.Ws2812b;
import com.sysolve.androidthings.utils.BoardSpec;
import com.sysolve.androidthings.utils.HttpServer;
import com.sysolve.androidthings.utils.Unzip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

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


    Ws2812b mWs2812b;

    private int mFrame = 0;
    private Handler mHandler;
    private HandlerThread mPioThread;

    private static final int FRAME_DELAY_MS = 30; // 24fps

    int[] mLedColors = new int[] {
            Color.BLUE, Color.BLUE, Color.BLUE,
            Color.BLUE, Color.BLUE, Color.BLUE,
            Color.BLUE, Color.BLUE, Color.BLUE,
            Color.BLUE, //Color.RED, Color.RED,
            //Color.RED, Color.RED, Color.RED,
            //Color.RED, Color.RED, Color.RED,
            //Color.RED, Color.RED, Color.RED,
            //Color.RED, Color.GREEN, Color.BLUE,
            //Color.RED, Color.GREEN, Color.BLUE,
            //Color.RED, Color.GREEN, Color.BLUE
    };

    boolean showMoreColor = false;

    private Runnable mAnimateRunnable = new Runnable() {
        final float[] hsv = {1f, 1f, 1f};

        @Override
        public void run() {
            if (showMoreColor) {
                try {
                    for (int i = 0; i < mLedColors.length; i++) { // Assigns gradient colors.
                        int n = (i + mFrame) % (mLedColors.length * 3);
                        hsv[0] = n * 360.f / (mLedColors.length * 3);
                        mLedColors[i] = Color.HSVToColor(0, hsv);
                    }
                    mWs2812b.write(mLedColors);
                    mFrame = (mFrame + 1) % (mLedColors.length * 3);
                } catch (IOException e) {
                    Log.e(TAG, "Error while writing to LED strip", e);
                }
                mHandler.postDelayed(mAnimateRunnable, FRAME_DELAY_MS);
            } else {
                try {
                    for (int i = 0; i < mLedColors.length; i++) { // Assigns gradient colors.
                        mLedColors[i] = color;
                    }
                    mWs2812b.write(mLedColors);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mHandler.postDelayed(mAnimateRunnable, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        try {
            mWs2812b = new Ws2812b(BoardSpec.getSpiBus());
        } catch (IOException e) {
            // couldn't configure the device...
        }

        try {
            mWs2812b.write(mLedColors);
        } catch (IOException e) {
            // error setting LEDs
        }

        initCamera();

        /*
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int c = mLedColors[0];
        for (int i=0;i<mLedColors.length-1;++i) {
            mLedColors[i] = mLedColors[i+1];
        }
        */

        mPioThread = new HandlerThread("pioThread");
        mPioThread.start();

        mHandler = new Handler(mPioThread.getLooper());

        mHandler.postDelayed(mAnimateRunnable, 3000);


        try {
            File f = new File(HttpServer.wwwRoot);
            File index = new File(HttpServer.wwwRoot+"index.html");
            if (!f.exists() || !index.exists()) {
                f.mkdirs();
                Unzip.unzip(getResources().getAssets().open("www.zip"), HttpServer.wwwRoot);
                Log.i(TAG, "Unzip www");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Start");

        PeripheralManager manager = PeripheralManager.getInstance();
        Log.i(TAG, "PeripheralManager");

        try {
            Log.i(TAG, "Reg GPIOs");
            //Create a background looper thread for I/O
            mInputThread = new HandlerThread("InputThread");
            mInputThread.start();
            mInputHandler = new Handler(mInputThread.getLooper());

            // Attempt to access the UART device
            try {
                openUart(manager, BoardSpec.getUartName(), 9600);
                // Read any initially buffered data
                mInputHandler.post(mTransferUartRunnable);
            } catch (IOException e) {
                Log.e(TAG, "Unable to open UART device", e);
            }

            Log.i(TAG, "Reg GPIOs");

            pwmLeft = manager.openPwm(BoardSpec.getInstance().getPwm(0));
            pwmRight = manager.openPwm(BoardSpec.getInstance().getPwm(1));

            echoDeviceEn = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_18));
            echoDeviceEn.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            left0 = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_29));
            left1 = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_31));
            left0.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            left1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            right0 = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_35));
            right1 = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_37));
            right0.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            right1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            stop = manager.openGpio(BoardSpec.getInstance().getGpioPin(BoardSpec.PIN_16));
            stop.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            pwmLeft.setPwmDutyCycle(50);       //percent, 0-100
            pwmLeft.setPwmFrequencyHz(PWM_FREQUENCY_HZ);
            pwmLeft.setEnabled(true);

            pwmRight.setPwmDutyCycle(50);
            pwmRight.setPwmFrequencyHz(PWM_FREQUENCY_HZ);
            pwmRight.setEnabled(true);

            //define a button for counter
            mButtonGpio = manager.openGpio(BoardSpec.getGoogleSampleButtonGpioPin());
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    buttonPressedTimes++;

                    if (status_back || status_forward || status==3) {
                        Log.i(TAG, "GPIO changed, button pressed " + buttonPressedTimes);
                        emStop();
                        status = 0;
                    } else if (status==0) {
                        color(Color.BLACK);
                        status = 1;
                    } else if (status==1) {
                        color(Color.BLUE);
                        status = 2;
                    } else if (status==1) {
                        color(Color.GREEN);
                        status = 2;
                    } else if (status==2) {
                        moreColor();
                        status = 3;
                        goForward();
                    }

                    // Return true to continue listening to events
                    return true;
                }
            });

            Log.i(TAG, "Start HTTP Server");
            new HttpServer(this);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private DoorbellCamera mCamera;

    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "PhotoCamera OnImageAvailableListener");

                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    public int picVersion = 0;

    /**
     * Handle image processing in Firebase and Cloud Vision.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        Log.d(TAG, "PhotoCamera onPictureTaken");
        if (imageBytes != null) {
            String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            Log.d(TAG, "imageBase64:"+imageStr);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                File file=new File(HttpServer.wwwRoot + "pic.jpg");//将要保存图片的路径
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                picVersion++;
            }
        }
    }

    private HandlerThread mCameraThread;

    public void initCamera() {
        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "PhotoCamera No permission");

            return;
        }

        //imageView = (ImageView)findViewById(R.id.imageView);

        DoorbellCamera.dumpFormatInfo(this);
        Log.d(TAG, "PhotoCamera inited");

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        /*imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("takePicture", "click image to take picture");
                mCamera.takePicture();
            }
        });*/
    }

    public int status = 0;

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
        status_forward = false;
        status_back = false;
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

    public void leftT() {
        //leftBack();
        //rightForward();
        lrdSpeed -= 1;
        try {
            pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
            pwmRight.setPwmDutyCycle(speed - lrdSpeed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        go();
    }

    public void noT() {
        lrdSpeed = 0;
        //leftForward();
        //rightBack();
        try {
            pwmLeft.setPwmDutyCycle(speed);
            pwmRight.setPwmDutyCycle(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        go();
    }

    public void rightT() {
        lrdSpeed += 1;
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

    public void turnLeft() {
        //leftForward();
        //rightBack();
        try {
            pwmLeft.setPwmDutyCycle(speed*0.8);
            pwmRight.setPwmDutyCycle(speed*1.2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        go();
    }

    public void turnRight() {
        //leftForward();
        //rightBack();
        try {
            pwmLeft.setPwmDutyCycle(speed*1.2);
            pwmRight.setPwmDutyCycle(speed*0.8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        go();
    }

    boolean status_back;
    boolean status_forward;

    public void goBack() {
        status_forward = false;
        status_back = true;
        leftBack();
        rightBack();
        go();
    }

    public void goForward() {
        try {
            pwmLeft.setPwmDutyCycle(speed + lrdSpeed);
            pwmRight.setPwmDutyCycle(speed - lrdSpeed);
        } catch (IOException e) {
            e.printStackTrace();
        }

        status_back = false;
        status_forward = true;
        leftForward();
        rightForward();
        go();
    }

    double speed = 30;

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

    private void openUart(PeripheralManager service, String name, int baudRate) throws IOException {
        echoDevice = service.openUartDevice(name);
        // Configure the UART
        echoDevice.setBaudrate(baudRate);
        echoDevice.setDataSize(8);
        echoDevice.setParity(UartDevice.PARITY_NONE);
        echoDevice.setStopBits(1);

        Log.i(TAG, "Open UART device");

        echoDevice.registerUartDeviceCallback(mInputHandler, mCallback);
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

    //private static byte[] prefix = "IoT".getBytes();
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
                                    if (status_forward &&  distance>0 && distance<100)
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

        // Close the LED strip when finished:
        try {
            mWs2812b.close();
        } catch (IOException e) {
            // error closing LED strip
        }

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

        mCamera.shutDown();

        mCameraThread.quitSafely();
    }

    int color;

    public void color(int color) {
        showMoreColor = false;
       this.color = color;
    }

    public void moreColor() {
        showMoreColor = true;
    }

    public void superSound(boolean on) {
        try {
            if (on) {
                echoDeviceEn.setValue(false);
            } else {
                echoDeviceEn.setValue(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Handler handler = new Handler();

    public void takePicture() {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i(TAG, "PhotoCamera take Picture");
                        mCamera.takePicture();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
