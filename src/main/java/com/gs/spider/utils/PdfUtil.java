package com.gs.spider.utils;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfAnnotationAppearance;
import com.itextpdf.kernel.pdf.annot.PdfFixedPrint;
import com.itextpdf.kernel.pdf.annot.PdfWatermarkAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

import java.util.Date;

/**
 * @author huminghe
 * @date 2021/8/25
 */
public class PdfUtil {

    public static void removeWatermarkPDF(String sourceFile, String destinationPath) {
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourceFile), new PdfWriter(destinationPath));
            int numberOfPages = pdfDoc.getNumberOfPages();
            System.out.println(numberOfPages);

            for (int i = 1; i <= numberOfPages; i++) {
                PdfDictionary pageDict = pdfDoc.getPage(i).getPdfObject();

                PdfArray contents = pageDict.getAsArray(PdfName.Contents);
                PdfStream psw = contents.getAsStream(2);

                psw.setData("".getBytes());
                psw.clear();

                PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
                PdfDictionary fonts = resources.getAsDictionary(PdfName.Font);

                fonts.entrySet().forEach(x -> {
                    if (x.getKey().toString().startsWith("/Xi")) {
                        PdfDictionary p = (PdfDictionary) x.getValue();
                        p.clear();
                    }
                });
            }
            pdfDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void watermarkPDF(String sourceFile, String destinationPath) {
        float watermarkTrimmingRectangleWidth;
        float watermarkTrimmingRectangleHeight;

        float formWidth;
        float formHeight;
        float formXOffset = 0;
        float formYOffset = 0;

        float xTranslation = 50;
        float yTranslation = 25;

        double rotationInRads = Math.PI / 3;
        try {

            String pdfFontPath = StaticValue.pdfFontPath;

            PdfFont font = PdfFontFactory.createFont(pdfFontPath, PdfEncodings.IDENTITY_H);
            float fontSize = 40;

            PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourceFile), new PdfWriter(destinationPath));
            int numberOfPages = pdfDoc.getNumberOfPages();
            PdfPage page = null;

            for (int i = 1; i <= numberOfPages; i++) {
                page = pdfDoc.getPage(i);
                Rectangle ps = page.getPageSize();
                watermarkTrimmingRectangleWidth = ps.getWidth() - 50;
                watermarkTrimmingRectangleHeight = ps.getHeight() - 50;
                formWidth = watermarkTrimmingRectangleWidth;
                formHeight = watermarkTrimmingRectangleHeight;

                //Center the annotation
                float bottomLeftX = ps.getWidth() / 2 - watermarkTrimmingRectangleWidth / 2;
                float bottomLeftY = ps.getHeight() / 2 - watermarkTrimmingRectangleHeight / 2;
                Rectangle watermarkTrimmingRectangle = new Rectangle(bottomLeftX, bottomLeftY, watermarkTrimmingRectangleWidth, watermarkTrimmingRectangleHeight);

                PdfWatermarkAnnotation watermark = new PdfWatermarkAnnotation(watermarkTrimmingRectangle);

                //Apply linear algebra rotation math
                //Create identity matrix
                AffineTransform transform = new AffineTransform();//No-args constructor creates the identity transform
                //Apply translation
                transform.translate(xTranslation, yTranslation);
                //Apply rotation
                transform.rotate(rotationInRads);

                PdfFixedPrint fixedPrint = new PdfFixedPrint();
                watermark.setFixedPrint(fixedPrint);
                //Create appearance
                Rectangle formRectangle = new Rectangle(formXOffset, formYOffset, formWidth, formHeight);

                //Observation: font XObject will be resized to fit inside the watermark rectangle
                PdfFormXObject form = new PdfFormXObject(formRectangle);
                PdfExtGState gs1 = new PdfExtGState().setFillOpacity(0.5f);
                PdfCanvas canvas = new PdfCanvas(form, pdfDoc);

                Date date = new Date();
                String dateString = date.toString();

                float[] transformValues = new float[6];
                transform.getMatrix(transformValues);
                canvas.saveState()
                    .beginText().setColor(ColorConstants.GRAY, true).setExtGState(gs1)
                    .setTextMatrix(transformValues[0], transformValues[1], transformValues[2], transformValues[3], transformValues[4], transformValues[5])
                    .setFontAndSize(font, fontSize)
                    .showText("知识萃取平台----" + dateString)
                    .endText()
                    .restoreState();

                canvas.release();

                watermark.setAppearance(PdfName.N, new PdfAnnotationAppearance(form.getPdfObject()));
                watermark.setFlags(PdfAnnotation.PRINT);

                page.addAnnotation(watermark);

            }
            page.flush();
            pdfDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
