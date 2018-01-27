package com.sysolve.androidthings.cameracar;

import android.util.Log;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by 13311 on 2018-01-27.
 */

public class HttpServer extends NanoHTTPD {

    MainActivity main;

        public HttpServer(MainActivity main) throws IOException {
            super(8080);
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
            this.main = main;
        }

        @Override
        public Response serve(IHTTPSession session) {
            String msg = "<html><meta content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0;\" name=\"viewport\" /><style>button { width: 160px; height: 80px; margin: 5px; }</style><body>";
            Map<String, String> parms = session.getParms();
            String cmd = parms.get("cmd");
            if (cmd == null) {
                msg += "<div><button onClick=\"send('f')\">Forward</button>";
                msg += "<button onClick=\"send('s')\">Stop</button></div>";
                msg += "<div><button onClick=\"send('l')\">Left</button>";
                msg += "<button onClick=\"send('r')\">Right</button></div>";
                msg += "<div><button onClick=\"send('b')\">Back</button></div>";
                msg += "<div><button onClick=\"send('a')\">Faster</button>";
                msg += "<button onClick=\"send('d')\">Slower</button></div>";

                msg += "<script src=\"https://cdn.bootcss.com/jquery/3.3.1/jquery.min.js\"></script>";
                msg += "<script>function send(d) { $.get('/?cmd='+d); }</script>";
            } else {
                Log.i("HTTP", "cmd="+cmd);
                cmd = cmd.trim().toLowerCase();
                if ("s".equals(cmd)) {
                    //停止
                    main.emStop();
                } else if ("f".equals(cmd)) {
                    //前进
                    main.goForward();
                } else if ("b".equals(cmd)) {
                    //后退
                    main.goBack();
                } else if ("l".equals(cmd)) {
                    //左转
                    main.turnLeft();
                } else if ("r".equals(cmd)) {
                    //右转
                    main.turnRight();
                } else if ("a".equals(cmd)) {
                    //加速
                    main.speedAdd();
                } else if ("d".equals(cmd)) {
                    //减速
                    main.speedDecrease();
                }
            }

            return newFixedLengthResponse(msg + "</body></html>\n");
        }
}
