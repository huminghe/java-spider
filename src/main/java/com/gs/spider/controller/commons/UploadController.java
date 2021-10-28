package com.gs.spider.controller.commons;

import com.gs.spider.controller.BaseController;
import com.gs.spider.utils.StaticValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author huminghe
 * @date 2021/10/22
 */
@Controller
@RequestMapping("/upload")
public class UploadController extends BaseController {

    private final static Logger logger = LogManager.getLogger(AuthController.class);

    @RequestMapping(value = {"/file"}, method = RequestMethod.POST)
    @ResponseBody
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            File file1 = new File(StaticValue.internalFileStorePrefix, file.getOriginalFilename());
            logger.info("upload file name: " + file.getOriginalFilename());
            if (!file1.exists()) {
                file1.createNewFile();
            }
            file.transferTo(file1);
            return "success";
        } catch (Exception e) {
            logger.error("upload file error, filename = " + file.getOriginalFilename(), e);
            return "failed";
        }
    }

    @RequestMapping(value = {"/file"}, method = RequestMethod.OPTIONS)
    public void check(HttpServletRequest request, HttpServletResponse response) {
        logger.info("cors check");
    }

}
