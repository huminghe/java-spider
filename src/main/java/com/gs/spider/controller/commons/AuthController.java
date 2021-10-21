package com.gs.spider.controller.commons;

import com.gs.spider.controller.BaseController;
import com.gs.spider.model.utils.AuthSign;
import com.gs.spider.utils.AuthUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * @author huminghe
 * @date 2021/10/20
 */
@Controller
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private final static Logger logger = LogManager.getLogger(AuthController.class);

    @RequestMapping(value = {"/", ""}, method = RequestMethod.POST)
    @ResponseBody
    public String checkAuth(@RequestBody AuthSign authSign) {
        logger.info("ts: " + authSign.getTs());
        logger.info("tsSign: " + authSign.getTsSign());
        return String.valueOf(AuthUtil.verifySign(authSign.getTs(), authSign.getTsSign()));

    }

}
