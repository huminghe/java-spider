package com.gs.spider.gather.commons.puppeteer;

import java.util.List;

/**
 * @Author zky
 * @Date 2020/12/29 16:22
 * chromium
 * 可选参数
 */
public class ChromiumOptions {

    /**
     * chromium启动命令行
     */
    List<String> ChromiumCMDList;
    /**
     * 使用本地浏览器
     */
    private boolean useLocalBrowser = true;
    /**
     * 浏览器远程地址
     */
    private String browserWSEndpoint;
    /**
     * 是否使用无头模式，默认使用
     */
    private boolean useHeadless = true;

    public List<String> getChromiumCMDList() {
        return ChromiumCMDList;
    }

    public void setChromiumCMDList(List<String> chromiumCMDList) {
        ChromiumCMDList = chromiumCMDList;
    }

    public boolean isUseLocalBrowser() {
        return useLocalBrowser;
    }

    public void setUseLocalBrowser(boolean useLocalBrowser) {
        this.useLocalBrowser = useLocalBrowser;
    }

    public String getBrowserWSEndpoint() {
        return browserWSEndpoint;
    }

    public void setBrowserWSEndpoint(String browserWSEndpoint) {
        this.browserWSEndpoint = browserWSEndpoint;
    }

    public boolean getUseHeadless() {
        return useHeadless;
    }

    public void setUseHeadless(boolean useHeadless) {
        this.useHeadless = useHeadless;
    }
}
