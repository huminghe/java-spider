package com.gs.spider.gather.commons.puppeteer;

import com.ruiyun.jvppeteer.core.Puppeteer;
import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.options.LaunchOptions;
import com.ruiyun.jvppeteer.options.LaunchOptionsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.proxy.Proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author zky
 * @Date 2020/12/29 16:21
 * puppeteer的请求配置设置(使用多浏览器，用于需要切换使用代理的情况)
 */
public class PuppeteerBrowserGenerator extends AbstractPuppeteerBrowserGenerator {
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    public Browser fetchBrowser(ChromiumOptions chromiumOptions, Proxy proxy) {
        Browser browser = null;

        //配置要忽略的执行默认参数
        List<String> ignoreDefaultArgsList = new ArrayList<>();
        ignoreDefaultArgsList.add("--enable-automation");

        //配置要执行的参数
        List<String> chromiumCMDList = null;
        if (null == chromiumOptions.getChromiumCMDList()) {
            chromiumCMDList = new ArrayList<>();
        } else {
            chromiumCMDList = chromiumOptions.getChromiumCMDList();
        }

        //优化性能相关参数()
        chromiumCMDList.add("--disable-gpu");
        chromiumCMDList.add("--disable-dev-shm-usage");
        chromiumCMDList.add("--disable-setuid-sandbox");
        chromiumCMDList.add("--no-first-run");
        chromiumCMDList.add("--no-sandbox");
        chromiumCMDList.add("--no-zygote");
        chromiumCMDList.add("--single-process");
        chromiumCMDList.add("--disable-web-security");

        //配置代理
        String currentProxy = "";
        String proxyParameterStr = "";

        //启动配置
        LaunchOptions launchOptions = new LaunchOptionsBuilder()
                .withArgs(chromiumCMDList)
                .withIgnoreDefaultArgs(ignoreDefaultArgsList)
                .withHeadless(chromiumOptions.getUseHeadless())
                .withDumpio(true)
                .build();

        //如果配置了远程浏览器地址，则不适用本地浏览器
        if (StringUtils.isNotBlank(chromiumOptions.getBrowserWSEndpoint())) {
            chromiumOptions.setUseLocalBrowser(false);
        }

        //创建browser的方式
        if (chromiumOptions.isUseLocalBrowser() == true) {
            try {
                browser = Puppeteer.launch(launchOptions);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            /**
             * 使用远程chromium ws://192.168.2.220:3000
             * 配置代理需要在远程浏览器的启动参数里配置，建议隧道代理使用这种方式
             */
            browser = Puppeteer.connect(launchOptions, chromiumOptions.getBrowserWSEndpoint(), null, null);
        }

        return browser;
    }

}
