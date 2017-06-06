package com.github.barteksc.sample;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

/**
 * Created by garad on 2017-06-02.
 */

public class GetPageText {
    private PDDocument document;
    private File file;
    private String name;
    private ProgressDialog progress;
    private PDFRenderer pdfRenderer;
    public PDFRenderer getPDFRenderer()
    {
        return this.pdfRenderer;
    }
    public PDDocument getDocument()
    {
        return document;
    }
    public File getFile()
    {
        return file;
    }
    public String getName()
    {
        return name;
    }
    public GetPageText(File file, String name, Context context, ProgressDialog progress)
    {
        this.progress = progress;
        PDFBoxResourceLoader.init(context);
        this.name = name;
        this.file = file;
        try {
            this.document =  PDDocument.load(file);
            this.pdfRenderer =  new PDFRenderer(this.document);
            this.progress.dismiss();
        } catch (IOException e) {
            e.printStackTrace();
            this.progress.dismiss();
        }


    }
    public String getOCRResult(int pageCounter, MyTessOCR mTessOCR)
    {
        String currentPage = "";
        Bitmap bim = null;
        try {
            bim = pdfRenderer.renderImage(2);//Math.max(0, pageCounter-1)
            boolean isLandscape = bim.getWidth() > bim.getHeight();
            if(!isLandscape)
            {
                String temp = mTessOCR.getOCRResult(bim);
                currentPage += temp;
                System.out.println("9332902: current page: "+temp);

            }
            else
            {
                Bitmap[] imgs = new Bitmap[2];
                imgs[0] = Bitmap.createBitmap(bim, 0, 0,  bim.getWidth() / 2, bim.getHeight());
                imgs[1] = Bitmap.createBitmap(bim,  bim.getWidth() / 2, 0,  bim.getWidth() / 2, bim.getHeight());

                for(Bitmap image : imgs)
                {
                    String temp = mTessOCR.getOCRResult(image);
                    currentPage += temp;
                }
            }
            return currentPage;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

    }

    public static void main(String[] args) throws IOException {
       // GetPageText  getPageText = new GetPageText(new File("C:\\Users\\garad\\Documents\\paperbait\\oneclass\\The Trouble with Growth SOW15_Victor  Jackson.pdf"), "hello", null, );

    }
    public String getText(int page, int isOCR,MyTessOCR mTessOCR) {
        if(isOCR == PDFViewActivity.OCR)
        {
            return getOCRResult(page,mTessOCR);
        }
        else if(isOCR == PDFViewActivity.TEXT) {
            try {
                PDFTextStripper reader = new PDFTextStripper();
                reader.setStartPage(page + 1);
                reader.setEndPage(page + 1);
                String pageText = reader.getText(document);
                System.out.println("f4903f390: " + pageText);
                return pageText;
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        else
            return "";
    }

}
