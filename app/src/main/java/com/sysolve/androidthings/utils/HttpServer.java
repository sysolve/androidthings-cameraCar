package com.sysolve.androidthings.utils;

import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import com.sysolve.androidthings.cameracar.MainActivity;

import java.io.File;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;

/**
 * Created by 13311 on 2018-01-27.
 */

public class HttpServer extends SimpleWebServer {

    public static String wwwRoot = Environment.getExternalStorageDirectory() + "/cameracar-www/";

    MainActivity main;

    public HttpServer(MainActivity main) {
        super(null, 8080, new File(wwwRoot), false, "*");

        try {
            this.main = main;
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
            Log.i("HTTPD", "\nRunning! Point your browsers to http://localhost:8080/ \n");
        } catch (Exception e) {
            Log.e("HTTPD", e.getMessage(), e);
        }
    }

    @Override
    protected NanoHTTPD.Response addCORSHeaders(Map<String, String> queryHeaders, NanoHTTPD.Response
            resp, String cors) {
        //resp = super.addCORSHeaders(queryHeaders, resp, cors);

        resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.addHeader("Pragma", "no-cache");
        resp.addHeader("Expires", "0");

        return resp;
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        //Log.i("HTTPD", session.getUri());
        if (session.getUri().startsWith("/action/")) {
            String msg = "";
            Map<String, String> parms = session.getParms();
            String cmd = parms.get("cmd");
            if (cmd == null) {
                msg = "<html><meta content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0;\" name=\"viewport\" /><style>button { width: 160px; height: 80px; margin: 5px; }</style><body>";

                msg += "<div><button onClick=\"send('f')\">Forward</button>";
                msg += "<button onClick=\"send('s')\">Stop</button></div>";
                msg += "<div><button onClick=\"send('l')\">Left</button>";
                msg += "<button onClick=\"send('r')\">Right</button></div>";
                msg += "<div><button onClick=\"send('b')\">Back</button></div>";
                msg += "<div><button onClick=\"send('a')\">Faster</button>";
                msg += "<button onClick=\"send('d')\">Slower</button></div>";

                msg += "<div><button onClick=\"send('lt')\">Left+</button>";
                msg += "<button onClick=\"send('rt')\">Right+</button></div>";
                msg += "<div><button onClick=\"send('nt')\">no</button></div>";

                msg += "<div><button onClick=\"send('cblue')\">Blue</button>";
                msg += "<button onClick=\"send('cgreen')\">Green</button></div>";
                msg += "<div><button onClick=\"send('cno')\">Black</button>";
                msg += "<button onClick=\"send('cmore')\">Colorful</button></div>";

                msg += "<div><button onClick=\"send('tp')\">Photo</button>";
                msg += "<button onClick=\"reloadImage()\">Refresh</button></div>";

                msg += "<img id=\"pic\" class=\"img-responsive\" src=\"\" alt=\"\">";

                msg += "<script src=\"/bower_components/jquery.min.js\"></script>";
                msg += "<script>function send(d) { $.get('/action/?cmd='+d); }";
                msg += "function reloadImage() { $(\"#pic\").attr(\"src\", \"pic.jpg\"); }";
                msg += "</script>";
                msg += "</body></html>\n";
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
                } else if ("lt".equals(cmd)) {
                    //向左偏移
                    main.leftT();
                } else if ("rt".equals(cmd)) {
                    //向右偏移
                    main.rightT();
                } else if ("nt".equals(cmd)) {
                    //无偏移
                    main.noT();
                } else if ("cred".equals(cmd)) {
                    //显示红色
                    main.color(Color.RED);
                } else if ("cyellow".equals(cmd)) {
                    //显示黄色
                    main.color(Color.YELLOW);
                } else if ("cgreen".equals(cmd)) {
                    //显示绿色
                    main.color(Color.GREEN);
                } else if ("cblue".equals(cmd)) {
                    //显示蓝色
                    main.color(Color.BLUE);
                } else if ("cmore".equals(cmd)) {
                    //显示炫彩
                    main.moreColor();
                } else if ("cno".equals(cmd)) {
                    //不显示颜色
                    main.color(Color.BLACK);
                } else if ("son".equals(cmd)) {
                    //启用超声波
                    main.superSound(true);
                } else if ("soff".equals(cmd)) {
                    //停用超声波
                    main.superSound(false);
                } else if ("tp".equals(cmd)) {
                    //拍照
                    main.takePicture();
                } else if ("tpv".equals(cmd)) {
                    //拍照
                    //main.takePicture();
                    msg = "{\"tpv\": "+ main.picVersion +"}";
                }
            }

            return newFixedLengthResponse(msg);
        } else {
            return super.serve(session);
        }
    }
}
