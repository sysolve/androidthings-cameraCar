package com.sysolve.androidthings.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by 13311 on 2018-04-21.
 */

public class Unzip {

    public static void unzip(InputStream zip, String targetDir) {
        int BUFFER = 4096; //这里缓冲区我们使用4KB，
        String strEntry; //保存每个zip的条目名称

        try {
            BufferedOutputStream dest = null; //缓冲输出流
            InputStream fis = zip;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry; //每个zip条目的实例

            while ((entry = zis.getNextEntry()) != null) {

                try {
                    Log.i("Unzip: ","="+ entry);
                    int count;
                    byte data[] = new byte[BUFFER];
                    strEntry = entry.getName();

                    File entryFile = new File(targetDir + strEntry);
                    if (entry.isDirectory()) {
                        if (!entryFile.exists()) {
                            entryFile.mkdirs();
                        }
                    } else {
                        FileOutputStream fos = new FileOutputStream(entryFile);
                        dest = new BufferedOutputStream(fos, BUFFER);
                        while ((count = zis.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.flush();
                        dest.close();
                    }
                } catch (Exception ex) {
                    Log.e("Unzip", ex.getMessage(), ex);
                }
            }
            zis.close();
        } catch (Exception e) {
            Log.e("Unzip", e.getMessage(), e);
        }
    }
}
