package com.gs.spider.gather.commons;

import com.gs.spider.gather.commons.puppeteer.PuppeAction;
import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.core.page.Response;
import com.ruiyun.jvppeteer.options.Viewport;
import com.ruiyun.jvppeteer.protocol.webAuthn.Credentials;
import com.gs.spider.gather.commons.puppeteer.AbstractChromiumAction;
import com.gs.spider.gather.commons.puppeteer.ChromiumOptions;
import com.gs.spider.gather.commons.puppeteer.PuppeteerBrowserGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.*;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.proxy.Proxy;
// import us.codecraft.webmagic.proxy.ProxyProvider;
import us.codecraft.webmagic.selector.PlainText;

import java.io.IOException;
import java.util.*;

/**
 * @Author zky
 * @Date 2020/12/25 13:19
 * puppeteer下载器（Chromium ）
 */
@Component
public class PuppeteerDownloader extends AbstractDownloader {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private PuppeteerBrowserGenerator puppeteerBrowserGenerator = new PuppeteerBrowserGenerator();
    // private ProxyProvider proxyProvider;
    private List<SpiderListener> spiderListenerList;
    private ChromiumOptions chromiumOptions;
    private AbstractChromiumAction chromiumAction;

    @Autowired
    private ContentLengthLimitHttpClientDownloader contentLengthLimitHttpClientDownloader;

    public PuppeteerDownloader() {
        List<SpiderListener> spiderListenerList = new ArrayList<>(1);

        //下载器，配置代理
        ChromiumOptions options = new ChromiumOptions();
        options.setUseHeadless(true);
        AbstractChromiumAction action = new PuppeAction();
        this.spiderListenerList = spiderListenerList;
        this.chromiumOptions = options;
        this.chromiumAction = action;
    }

    @Override
    protected void onSuccess(Request request) {
        if (CollectionUtils.isNotEmpty(this.spiderListenerList)) {
            Iterator var2 = this.spiderListenerList.iterator();

            while (var2.hasNext()) {
                SpiderListener spiderListener = (SpiderListener) var2.next();
                spiderListener.onSuccess(request);
            }
        }
    }

    @Override
    protected void onError(Request request) {
        if (CollectionUtils.isNotEmpty(this.spiderListenerList)) {
            Iterator var2 = this.spiderListenerList.iterator();

            while (var2.hasNext()) {
                SpiderListener spiderListener = (SpiderListener) var2.next();
                spiderListener.onError(request);
            }
        }
    }

    private Browser fetchBrowser(ChromiumOptions chromiumOptions, Proxy proxy) {
        return puppeteerBrowserGenerator.fetchBrowser(chromiumOptions, proxy);
    }

    @Override
    public Page download(Request request, Task task) {
        if (task != null && task.getSite() != null) {
            logger.info("downloading page {}", request.toString());
            Response response = null;
            // Proxy proxy = this.proxyProvider != null ? this.proxyProvider.getProxy(task) : null;
            Page page = new Page();
            Browser browser = null;
            com.ruiyun.jvppeteer.core.page.Page chromiumPage = null;
            Page var9;
            String currentProxy = "";
            Proxy proxy = null;
            try {
                //获取浏览器
                browser = fetchBrowser(chromiumOptions, proxy);
                chromiumPage = browser.newPage();
                //设置窗口大小
                Viewport viewport = new Viewport();
                viewport.setWidth(1920);
                viewport.setHeight(1080);
                chromiumPage.setViewport(viewport);
                chromiumPage.setDefaultTimeout(task.getSite().getTimeOut() / 2);
                chromiumPage.coverage().startJSCoverage();
                chromiumPage.coverage().startCSSCoverage();
                response = chromiumPage.goTo(request.getUrl());
                chromiumPage.coverage().stopJSCoverage();
                chromiumPage.coverage().stopCSSCoverage();
                //浏览器操作
                chromiumPage = chromiumAction.execute(browser, request, chromiumPage, task.getSite());
                //处理页面内容转换成page对象
                page = this.handleResponse(request, chromiumPage.content(), response);
                this.onSuccess(request);
                //chromiumPage.close();
                this.logger.info("downloading page success {},{}", request.getUrl(), "代理为：" + currentProxy);
                Page var8 = page;
                return var8;
            } catch (Exception e) {
                this.logger.warn("download page error {},{},{}", request.getUrl(), e, "代理为：" + currentProxy);
                request.putExtra("exceptionMessage", e.toString().split(":")[0]);
                this.onError(request);
                Page result = contentLengthLimitHttpClientDownloader.download(request, task);
                var9 = result;
            } finally {
                if (null != chromiumPage) {
                    try {
                        chromiumPage.close();
                        this.logger.info("close page success");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (null != browser) {
                    browser.close();
                    this.logger.info("close browser success");
                }
            }
            return var9;
        } else {
            throw new NullPointerException("task or site can not be null");
        }
    }

    protected Page handleResponse(Request request, String content, Response response) throws IOException, InterruptedException {
//        byte[] bytes = null;
//        if (StringUtils.isNotBlank(content)) {
//            bytes = content.getBytes();
//        } else {
//            bytes = response.buffer();
//        }
//        String contentType = "";
//        if(response.headers().get("Content-Type") != null){
//            contentType = response.headers().get("Content-Type");
//        } else if ( response.headers().get("content-type") != null){
//            contentType = response.headers().get("content-type");
//        }
//        Page page = new Page();
//        page.setBytes(bytes);
//        if (!request.isBinaryContent()) {
//            if (charset == null) {
//                charset = this.getHtmlCharset(contentType, bytes);
//            }
//
//            page.setCharset(charset);
//            page.setRawText(new String(bytes, charset));
//            page.setRawText(content);
//        }
        Page page = new Page();
        page.setRawText(content);

        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(response.status());
        return page;
    }

    public static Map<String, List<String>> convertHeaders(Map<String, String> headers) {
        Map<String, List<String>> results = new HashMap<>();
        List<String> list;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            list = new ArrayList<>();
            list.add(entry.getValue());
            results.put(entry.getKey(), list);
        }
        return results;
    }

    @Override
    public void setThread(int threadNum) {

    }

}
