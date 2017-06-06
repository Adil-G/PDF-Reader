package com.github.barteksc.sample;

/**
 * Created by garad on 2017-06-03.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


import android.util.Log;

public class Downloader {

    private static String PATH = "/data/data/com.github.barteksc/tessdata/";  //put the downloaded file here
    public static void downloadByLanguage(final String lang, String path)
    {
        PATH = path;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    new Downloader().asdf(lang);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();

    }
    public void asdf(String lang)
    {
        String[] items = new String[]{".traineddata",".cube.bigrams",".cube.fold",".cube.lm",".cube.nn",".cube.params",".cube.size",".cube.word-freq"
        ,".tesseract_cube.nn",".traineddata"};
        for(String item :items)
            new Downloader().DownloadFromUrl("https://github.com/tesseract-ocr/tessdata/blob/3.04.00/"+lang+".traineddata?raw=true",lang+item);
    }
    public void DownloadFromUrl(String imageURL, String fileName) {  //this is the downloader method
        try {
            URL url = new URL(imageURL); //you can write here any link
                    File file = new File(PATH+fileName);

            long startTime = System.currentTimeMillis();
            Log.d("Downloader", "download begining");
            Log.d("Downloader", "download url:" + url);
            Log.d("Downloader", "downloaded file name:" + fileName);
                        /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();

                        /*
                         * Define InputStreams to read from the URLConnection.
                         */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

                        /*
                         * Read bytes to the Buffer until there is nothing more to read(-1).
                         */
            FileOutputStream fos = new FileOutputStream(file);

            int current = 0;
            while ((current = bis.read()) != -1) {
                fos.write(current);
            }

            fos.close();
            Log.d("Downloader", "download ready in"
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec");

        } catch (IOException e) {
            Log.d("Downloader", "Error: " + e);
        }

    }
}