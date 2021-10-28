package com.gs.spider.controller.commons;

import com.gs.spider.controller.BaseController;
import com.gs.spider.model.utils.FileInfo;
import com.gs.spider.utils.PdfUtil;
import com.gs.spider.utils.StaticValue;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author huminghe
 * @date 2021/8/23
 */
@Controller
@RequestMapping("/file")
public class FileController extends BaseController {

    private final static Logger logger = LogManager.getLogger(FileController.class);

    @RequestMapping(value = {"/", ""}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public void getAttach(@RequestBody FileInfo fileInfo, HttpServletResponse response) {
        FileInputStream bis = null;
        OutputStream os = null;
        try {
            String path = fileInfo.getFilePath();
            String accountName = fileInfo.getAccountName();
            logger.info("filePath: " + path);
            logger.info("accountName: " + accountName);
            path = new File(StaticValue.internalFileStorePrefix, path).getPath();

            String tmpPath = path + RandomStringUtils.randomAlphabetic(10);
            try {
                PdfUtil.watermarkPDF(path, tmpPath, accountName);
            } catch (Exception ex) {
                logger.info("watermark failed, file: " + path);
                Files.copy(new File(path).toPath(), new File(tmpPath).toPath());
            }
            response.setContentType(getContentType(path));
            bis = new FileInputStream(tmpPath);
            os = response.getOutputStream();
            int count = 0;
            byte[] buffer = new byte[1024 * 1024];
            while ((count = bis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
            new File(tmpPath).delete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith("pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith("ppt") || fileName.endsWith("pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (fileName.endsWith("doc") || fileName.endsWith("docx")) {
            return "application/msword";
        } else if (fileName.endsWith("xls") || fileName.endsWith("xlsx")) {
            return "application/vnd.ms-excel";
        } else {
            return "text/plain";
        }
    }

}
