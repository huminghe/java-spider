package com.gs.spider.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import com.itextpdf.kernel.pdf.PdfObject;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author huminghe
 * @date 2021/8/25
 */
public class PdfUtil {

    public static final String FONT_FS = "sysfFS";
    public static final String FONT_FZFS = "sysfFZFS";

    public static boolean needOCR(String sourceFile) {
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourceFile));
            int numberOfPages = pdfDoc.getNumberOfPages();
            System.out.println(numberOfPages);

            for (int i = 1; i <= numberOfPages; i++) {
                PdfDictionary pageDict = pdfDoc.getPage(i).getPdfObject();

                PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
                PdfDictionary fonts = resources.getAsDictionary(PdfName.Font);

                Set<Map.Entry<PdfName, PdfObject>> fontsSet = fonts.entrySet();
                for (Map.Entry<PdfName, PdfObject> m : fontsSet) {
                    PdfDictionary dic = (PdfDictionary) m.getValue();
                    PdfName fontName = dic.getAsName(PdfName.BaseFont);
                    if (fontName != null && (fontName.toString().contains(FONT_FS) || fontName.toString().contains(FONT_FZFS))) {
                        return true;
                    }
                }
            }
            pdfDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String imageToBase64(String imgFile) {
        InputStream in = null;
        byte[] data = null;
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    public static String doOCRFromFile(String imageFile) {
        String result = "";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        try {
            String str = imageToBase64(imageFile);
            // 创建httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost(StaticValue.ocrApi);
            // 配置请求参数实例
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(350000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(350000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
            // 为httpPost实例设置配置
            httpPost.setConfig(requestConfig);
            // 设置请求头
            // httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            // 封装post请求参数
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("ImgString", str));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            String entityString = EntityUtils.toString(entity);
            JSONObject jsonObject = JSON.parseObject(entityString);
            result = jsonObject.getString("text");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String fetchContentByOCR(String sourceFile) {
        StringBuilder sb = new StringBuilder();
        PDDocument pdDocument = null;
        try {
            pdDocument = PDDocument.load(new File(sourceFile));
            PDFRenderer renderer = new PDFRenderer(pdDocument);
            /* dpi越大转换后越清晰，相对转换速度越慢 */
            int pages = pdDocument.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                String imgPath = new File(sourceFile).getPath() + RandomStringUtils.randomAlphabetic(10) + ".png";
                File dstFile = new File(imgPath);
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                ImageIO.write(image, "png", dstFile);
                String picInfo = doOCRFromFile(imgPath);
                sb.append(picInfo);
                dstFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pdDocument != null) {
                    pdDocument.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static String fetchContentByTika(String sourceFile) {
        InputStream inStream = null;
        String result = "";
        try {
            inStream = new FileInputStream(sourceFile);
            AutoDetectParser autoDetectParser = new AutoDetectParser();
            BodyContentHandler bodyContentHandler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            autoDetectParser.parse(inStream, bodyContentHandler, metadata);
            result = bodyContentHandler.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

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
