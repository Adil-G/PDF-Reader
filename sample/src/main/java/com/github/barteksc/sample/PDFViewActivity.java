/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.sample;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

import static java.security.AccessController.getContext;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.options)
public class PDFViewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = PDFViewActivity.class.getSimpleName();

    private final static int REQUEST_CODE = 42;
    public static final int PERMISSION_CODE = 42042;

    public static final String SAMPLE_FILE = "sample.pdf";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";

    @ViewById
    PDFView pdfView;

    @NonConfigurationInstance
    Uri uri;

    @NonConfigurationInstance
    Integer pageNumber = 0;

    TextToSpeech t1;

    String pdfFileName;
    GetPageText getPageText;
    @OptionsItem(R.id.pickFile)
    void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    PERMISSION_CODE
            );

            return;
        }

        launchPicker();
    }
    public void onPause(){
        if(t1 !=null){
            t1.stop();
        }
        super.onPause();
    }
    void launchPicker() {


        //Context.startActivity(intent);
        try {
            /*ShareCompat.IntentBuilder intent = ShareCompat.IntentBuilder.from(PDFViewActivity.this);
            intent .setType("application/pdf")
                    .setStream(uri)
                    .setChooserTitle("Choose bar")
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    */
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            try {
                startActivityForResult(intent, REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                //alert user that file manager not working
                Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
            }
            /*Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("application/pdf");
            startActivityForResult(i, REQUEST_CODE);*/
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    @AfterViews
    void afterViews() {
        if (uri != null) {
            displayFromUri(uri);
        } else {
            displayFromAsset(SAMPLE_FILE);
        }
        setTitle(pdfFileName);
    }

    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromAsset(SAMPLE_FILE)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    private void displayFromUri(Uri uri) {

        pdfFileName = getFileName(uri);

        pdfView.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
        String path = uri.toString(); // "file:///mnt/sdcard/FileName.mp3"
        File file = new File(Environment.getExternalStorageDirectory()+"/hello.pdf");
        System.out.println("f389wwjf9: "+file.getAbsolutePath());
        ContentResolver s = getContentResolver();
        try {
            copyInputStreamToFile(s.openInputStream(uri),file);
            System.out.println("f389wwjf9 FIle copied: "+file.getAbsolutePath());
            isLoading(true);
            if(getPageText==null)
            {
                getPageText = new GetPageText(file, pdfFileName,getApplicationContext(),progress);

            }
            else if(getPageText.getName() != pdfFileName)
            {
                getPageText = new GetPageText(file, pdfFileName,getApplicationContext(),progress);

            }
            System.out.println("f389wwjf9 FINISHED GET TEXT"+file.getAbsolutePath());
            checkPageAndReadFree(true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    public String getPath(Uri uri)
    {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index =             cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s=cursor.getString(column_index);
        cursor.close();
        return s;
    }
    public String getFromGallery(Uri selectedImage)
    {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        return picturePath;
    }
    public String getAbsolutePath(Uri uri) {
        if(Build.VERSION.SDK_INT >= 19){
            String id = uri.toString().split(":")[1];
            final String[] imageColumns = {MediaStore.Images.Media.DATA };
            final String imageOrderBy = null;
            Uri tempUri = uri;
            Cursor imageCursor = getContentResolver().query(tempUri, imageColumns,
                    MediaStore.Images.Media._ID + "="+id, null, imageOrderBy);
            if (imageCursor.moveToFirst()) {
                return imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }else{
                return null;
            }
        }else{
            String[] projection = { MediaStore.MediaColumns.DATA };

            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else
                return null;
        }

    }
    public static String getFileNameByUri(Context context, Uri uri)
    {
        String fileName="unknown";//default fileName
        Uri filePathUri = uri;
        if (uri.getScheme().toString().compareTo("content")==0)
        {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst())
            {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                filePathUri = Uri.parse(cursor.getString(column_index));
                fileName = filePathUri.getLastPathSegment().toString();
            }
        }
        else if (uri.getScheme().compareTo("file")==0)
        {
            fileName = filePathUri.getLastPathSegment().toString();
        }
        else
        {
            fileName = fileName+"_"+filePathUri.getLastPathSegment();
        }
        return fileName;
    }
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    @OnActivityResult(REQUEST_CODE)
    public void onResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            uri = intent.getData();
            displayFromUri(uri);
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));



    }
   // @Click(R.id.pause)
    public void pausePage()
    {

        if(t1!=null)
        {
            isSpeaking= false;
            t1.stop();
        }
    }
    @Click(R.id.stop)
    public void stopPage()
    {
        if(stopButton==null)
        {
            stopButton = ((Button)findViewById(R.id.button));
        }
        stopButton.startAnimation(new AlphaAnimation(1F, 0.8F));
        if(playButton==null)
        {
            playButton = ((Button)findViewById(R.id.button));
        }
        playButton.setBackgroundResource((R.drawable.play));
        if(t1!=null)
        {
            isSpeaking= false;
            t1.stop();
            speakProgress = 0;
        }
    }
    public void speakPageContinue()
    {
        isSpeaking=true;
       // isLoading(false);
        speak(false);
    }
    ProgressDialog progress;
    Thread thread;
    public void checkPageAndReadFree(final boolean isStarting)
    {

        isLoading(true);
        if(thread==null) {
            thread = new Thread() {
                @Override
                public void run() {
                    speakProgress = Math.max(0, (speakProgress - 1));
                    isSpeaking = false;
                    if (!isStarting&&curPageReading == pageNumber) {
                        System.out.println("9332902: CONTINUING PAGE");
                        speakPageContinue();
                    } else {
                        speakProgress = 0;
                        speakPage();
                    }
                }
            };
        }
        else
        {
            thread.interrupt();
            thread = new Thread() {
                @Override
                public void run() {
                    speakProgress = Math.max(0, (speakProgress - 1));
                    isSpeaking = false;
                    if (!isStarting&&curPageReading == pageNumber) {
                        System.out.println("9332902: CONTINUING PAGE");
                        speakPageContinue();
                    } else {
                        speakProgress = 0;
                        speakPage();
                    }
                }
            };
        }
        thread.start();


    }
    Button playButton, stopButton;
    @Click(R.id.button)
    public void playOrPause()
    {
        if(playButton==null)
        {
            playButton = ((Button)findViewById(R.id.button));
        }
        playButton.startAnimation(new AlphaAnimation(1F, 0.8F));

        if(isSpeaking)
        {
            playButton.setBackgroundResource((R.drawable.play));
            pausePage();

        }
        else
        {
            playButton.setBackgroundResource((R.drawable.pause));
            checkPageAndRead();
        }
    }

    public void checkPageAndRead()
    {
        isLoading(true);
        if(thread==null) {
            thread = new Thread() {
                @Override
                public void run() {
                    speakProgress = Math.max(0, (speakProgress - 1));
                    if (curPageReading == pageNumber) {
                        System.out.println("9332902: CONTINUING PAGE");
                        speakPageContinue();
                    } else {
                        speakProgress = 0;
                        speakPage();
                    }
                }
            };
        }
        else
        {
            thread.interrupt();
            thread = new Thread() {
                @Override
                public void run() {
                    speakProgress = Math.max(0, (speakProgress - 1));
                    if (curPageReading == pageNumber) {
                        System.out.println("9332902: CONTINUING PAGE");
                        speakPageContinue();
                    } else {
                        speakProgress = 0;
                        speakPage();
                    }
                }
            };
        }
        thread.start();


    }

    String[] sentences;
    int curPageReading = -10;
    public static final int OCR=1;
    public static final int TEXT=0;
    public void speakPage()
    {
        //stopPage();
        curPageReading = pageNumber;
// To dismiss the dialog

        if(getPageText!=null)
        {
            String pageText=  getPageText.getText(pageNumber, PDFViewActivity.TEXT,mTessOCR);
            System.out.println("9332902: " + pageText);
            if(t1==null)
            {

                t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        System.out.println("9332902: TTS: "+status);
                        if(status != TextToSpeech.ERROR) {

                        }
                    }
                });
            }
            isSpeaking= true;
            this.sentences = pageText.split("[\\.\\?!]+|\\n{2,}");
            speakProgress = 0;
            seekBar1.setProgress(speakProgress);
            //isLoading(false);
            speak(true);

        }
        //isLoading(false);
    }
    public void onDestroy() {

        super.onDestroy();
        stopPage();

    }

    public void isLoading(boolean isLoading)
    {
        System.out.println("9332902: "+isLoading+" LOADIER");
        if(progress == null)
        {
            progress = new ProgressDialog(this);
            progress.setTitle("Loading");
            progress.setMessage("Wait while loading...");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        }
        if(progress!=null)
        {
            if(isLoading) {
                progress.show();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else
                progress.dismiss();
        }

    }

    boolean isSpeaking = true;
    static int speakProgress = 0;
    public void speak(final boolean isStarting)
    {
        isLoading(false);
        final String toSpeak = sentences[Math.min(Math.max(speakProgress, 0), sentences.length)];
       // Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
        if(isSpeaking) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //isLoading(false);
                    seekBar1.setMax(sentences.length);
                    seekBar1.setProgress(speakProgress);
                    int progress = (int)(((float)seekBar1.getProgress()/(float)seekBar1.getMax())*100);
                    ((TextView)findViewById(R.id.textView)).setText("("+progress+"%): "+toSpeak);
                }//public void run() {
            });
        }
        else
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // isLoading(false);
                    ((TextView)findViewById(R.id.textView)).setText("Stopped.");
                }//public void run() {
            });
            return;
        }

        String utteranceId = "23";
        if(isSpeaking) {

            //t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            t1.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    Log.d("MainActivity", "9332902");
                    if(isSpeaking) {
                        if(!isStarting)
                        nextSentence();
                        //isLoading(false);

                        speak(false);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });
        }
        else {
            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            t1.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    if(isSpeaking) {
                        if(!isStarting)
                        nextSentence();
                        //isLoading(false);
                        speak(false);
                    }
                }
            });
        }
    }

    public void nextSentence()
    {
        speakProgress++;
    }
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    SeekBar seekBar1;
    private MyTessOCR mTessOCR;
    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");

        seekBar1=(SeekBar)findViewById(R.id.seekBar1);
        seekBar1.setOnSeekBarChangeListener(this);
        //mTessOCR = new MyTessOCR(getApplicationContext());
        Locale loc = new Locale("en");
        Locale[] locales =  loc.getAvailableLocales();
        Spinner dropdown = (Spinner)findViewById(R.id.spinner1);
        ArrayAdapter<Locale> adapter = new ArrayAdapter<Locale>(this, android.R.layout.simple_spinner_dropdown_item, locales);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
            if(t1 !=null)
            {
                t1.setLanguage((Locale) arg0.getItemAtPosition(arg2));
            }
        }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }});
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    /**
     * Listener for response to user permission request
     *
     * @param requestCode  Check that permission request code matches
     * @param permissions  Permissions that requested
     * @param grantResults Whether permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int progress = (int)(((float)seekBar.getProgress()/(float)seekBar.getMax())*100);
        speakProgress = i;
        pausePage();
        speakPageContinue();


    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
