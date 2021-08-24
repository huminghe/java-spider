package com.gs.spider.controller.commons;

import com.gs.spider.controller.BaseController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author huminghe
 * @date 2021/8/23
 */
@Controller
@RequestMapping("/file")
public class FileController extends BaseController {

    private final static Logger logger = LogManager.getLogger(FileController.class);

    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public void getAttach(HttpServletRequest request, HttpServletResponse response) {
        FileInputStream bis = null;
        OutputStream os = null;
        try {
            String path = request.getParameter("filePath");
            response.setContentType("application/pdf");
            bis = new FileInputStream(path);
            os = response.getOutputStream();
            int count = 0;
            byte[] buffer = new byte[1024 * 1024];
            while ((count = bis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
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

}
