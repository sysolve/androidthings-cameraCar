/*
 * @author Ray, ray@sysolve.com
 * Copyright 2018, Sysolve IoT Open Source
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sysolve.androidthings.utils;

import android.os.Build;

/**
 * Define pins for different Board Type
 */
public class BoardSpec {
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX6UL_PICO = "imx6ul_pico";
    private static final String DEVICE_IMX7D_PICO = "imx7d_pico";

    public static final int PIN_13 = 0;
    public static final int PIN_15 = 1;
    public static final int PIN_16 = 2;
    public static final int PIN_18 = 3;
    public static final int PIN_22 = 4;
    public static final int PIN_29 = 5;
    public static final int PIN_31 = 6;            //for google simplepio/blink application
    public static final int PIN_32 = 7;
    public static final int PIN_35 = 8;
    public static final int PIN_36 = 9;
    public static final int PIN_37 = 10;
    public static final int PIN_38 = 11;
    public static final int PIN_40 = 12;           //for google simplepio/button application

    public static BoardSpec getInstance() {
        return instance;
    }

    public static BoardSpec instance = getBoardSpec(Build.DEVICE);

    public static BoardSpec getBoardSpec(String device) {
        switch (device) {
            case DEVICE_RPI3:
                return createBoardSpecRPI3();
            case DEVICE_IMX6UL_PICO:
                return createBoardSpecIMX6UL_PICO();
            case DEVICE_IMX7D_PICO:
                return createBoardSpecIMX7D_PICO();
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    private String[] gpios;
    private String[] pwms;
    private String i2c = null;
    private String spi = null;
    private String uart = null;

    public String getGpioPin(int i) {
        if (gpios==null || i>=gpios.length || gpios[i]==null) {
            throw new IllegalArgumentException("GPIO pin not Supported, Device: " + Build.DEVICE);
        } else {
            return gpios[i];
        }
    }

    public String getPwm(int i) {
        if (pwms==null || i>=pwms.length || pwms[i]==null) {
            throw new IllegalArgumentException("PWM_" +i+ " not Supported, Device: " + Build.DEVICE);
        } else {
            return pwms[i];
        }
    }

    public static String getGoogleSampleButtonGpioPin() {
        return BoardSpec.getInstance().getGpioPin(PIN_40);
    }

    public static String getGoogleSampleLedGpioPin() {
        return BoardSpec.getInstance().getGpioPin(PIN_31);
    }

    public static String getPWMPort() {
        return BoardSpec.getInstance().getPwm(0);
    }

    public static String getGoogleSampleSpeakerPwmPin() {
        return BoardSpec.getInstance().getPwm(1);
    }

    public static String getI2cBus() {
        String s = BoardSpec.getInstance().i2c;
        if (s!=null)
            return s;
        else
            throw new IllegalArgumentException("I2C Bus not Supported, Device: " + Build.DEVICE);
    }

    public static String getUartName() {
        String s = BoardSpec.getInstance().uart;
        if (s!=null)
            return s;
        else
            throw new IllegalArgumentException("UART not Supported, Device: " + Build.DEVICE);
    }

    public static String getSpiBus() {
        String s = BoardSpec.getInstance().spi;
        if (s!=null)
            return s;
        else
            throw new IllegalArgumentException("SPI Bus not Supported, Device: " + Build.DEVICE);
    }


    private static BoardSpec createBoardSpecRPI3() {
        BoardSpec spec = new BoardSpec();
        spec.i2c = "I2C1";
        spec.spi = "SPI0.0";
        spec.uart = "UART0";
        spec.pwms = new String[] {
                "PWM0",
                "PWM1"
        };
        spec.gpios = new String[] {
                "BCM27",    //                PIN_13,
                "BCM22",    //                PIN_15,
                "BCM23",    //                PIN_16,
                "BCM24",    //                PIN_18,
                "BCM25",    //                PIN_22,
                "BCM5",    //                 PIN_29,
                "BCM6",    //                 PIN_31,
                "BCM12",    //                PIN_32,
                "BCM19",    //                PIN_35,
                "BCM16",    //                PIN_36,
                "BCM26",    //                PIN_37,
                "BCM20",    //                PIN_38,
                "BCM21",    //                PIN_40
        };

        return spec;
    }

    private static BoardSpec createBoardSpecIMX6UL_PICO() {
        BoardSpec spec = new BoardSpec();
        spec.i2c = "I2C2";
        spec.spi = "SPI3.0";
        spec.uart = "UART3";
        spec.pwms = new String[] {
                "PWM7",
                "PWM8"
        };
        spec.gpios = new String[] {
                "GPIO4_IO23",    //                PIN_13,
                null,             //                PIN_15, //not support
                "GPIO2_IO00",    //                PIN_16,
                "GPIO2_IO01",    //                PIN_18,
                null,             //                PIN_22, //not support
                "GPIO4_IO21",    //                PIN_29,
                "GPIO4_IO22",    //                PIN_31,
                null,             //                PIN_32, //not support
                "GPIO4_IO19",    //                PIN_35,
                "GPIO5_IO02",    //                PIN_36,
                "GPIO1_IO18",    //                PIN_37,
                "GPIO2_IO02",    //                PIN_38,
                "GPIO2_IO03"     //                PIN_40
        };
        return spec;
    }

    private static BoardSpec createBoardSpecIMX7D_PICO() {
        BoardSpec spec = new BoardSpec();
        spec.i2c = "I2C1";
        spec.spi = "SPI3.1";
        spec.uart = "UART6";
        spec.pwms = new String[] {
                "PWM1",
                "PWM2"
        };
        spec.gpios = new String[] {
                "GPIO2_IO03",    //                PIN_13,
                "GPIO1_IO10",    //                PIN_15,
                "GPIO6_IO13",    //                PIN_16,
                "GPIO6_IO12",    //                PIN_18,
                "GPIO5_IO00",    //                PIN_22,
                "GPIO2_IO01",    //                PIN_29,
                "GPIO2_IO02",    //                PIN_31,
                null,    //                PIN_32, //not support
                "GPIO2_IO00",    //                PIN_35,
                "GPIO2_IO07",    //                PIN_36,
                "GPIO2_IO05",    //                PIN_37,
                "GPIO6_IO15",    //                PIN_38,
                "GPIO6_IO14",    //                PIN_40
        };
        return spec;
    }

}
