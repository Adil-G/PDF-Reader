package com.github.barteksc.sample;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by garad on 2017-06-03.
 */

public class MyTessOCR {
    private String datapath;
    private TessBaseAPI mTess;
    Context context;
    public MyTessOCR(Context context) {

        // TODO Auto-generated constructor stub        this.context = context;
        datapath = Environment.getExternalStorageDirectory().toString();
        File dir = new File(datapath + "/tessdata/");
        File file = new File(datapath + "/tessdata/" + "eng.traineddata");
        if (!file.exists()) {
            Log.d("mylog", "in file doesn't exist");
            dir.mkdirs();
            copyFile(context);
        }

        mTess = new TessBaseAPI();
        String language = "eng";
        mTess.init(datapath, language); //Auto only        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY);
    }

    public void stopRecognition() {
        mTess.stop();
    }
    /*

    public static String getText(File imageFile) throws TesseractException {


        System.out.println("File exists: "+imageFile.exists());
        Tesseract instance = Tesseract.getInstance(); // JNA Interface Mapping
        try
        {
            String result = instance.doOCR(imageFile);
            System.out.println("FEWOINFSDON#(J#@(*FJ#(*FW: "+result);
            return result;
        }
        catch (TesseractException e)
        {
            return "Failed";
        }

       // return "";
    }
     */
    public String getOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        mTess.setPageSegMode(TessBaseAPI.OEM_TESSERACT_ONLY);
        String result = mTess.getUTF8Text();
        System.out.println("892uf989wjf TESS RESULT: "+result);
        return result;
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

    private void copyFile(Context context) {
        String thisPath = datapath + "/tessdata/";
        Downloader.downloadByLanguage("eng",thisPath);
        /*AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open("eng.traineddata");
            OutputStream out = new FileOutputStream(datapath + "/tessdata/" + "eng.traineddata");
            byte[] buffer = new byte[1024];
            int read = in .read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in .read(buffer);
            }
        } catch (Exception e) {
            Log.d("mylog", "couldn't copy with the following error : " + e.toString());
        }*/
    }
}