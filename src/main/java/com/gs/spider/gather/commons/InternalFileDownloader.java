package com.gs.spider.gather.commons;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.selector.PlainText;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/8/23
 */
public class InternalFileDownloader extends AbstractDownloader {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<SpiderListener> spiderListenerList;

    public InternalFileDownloader() {
    }

    public InternalFileDownloader(List<SpiderListener> spiderListenerList) {
        this.spiderListenerList = spiderListenerList;
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

    @Override
    public Page download(Request request, Task task) {
        if (task != null && task.getSite() != null) {

            String url = request.getUrl();
            InputStream inStream = null;
            logger.info("loading file from: " + url);
            String result = "";
            Page page = new Page();
            try {
                if (new File(url).isDirectory()) {
                    page = handleResponse(request, "");
                } else {
                    inStream = new FileInputStream(url);
                    AutoDetectParser autoDetectParser = new AutoDetectParser();
                    BodyContentHandler bodyContentHandler = new BodyContentHandler();
                    Metadata metadata = new Metadata();
                    autoDetectParser.parse(inStream, bodyContentHandler, metadata);
                    result = bodyContentHandler.toString();
                    String[] resultSplited = result.split("\n\n");
                    List<String> resultList = Arrays.stream(resultSplited).filter(x -> !x.contains("国家电网有限公司高级培训中心 信息技术部"))
                        .filter(x -> {
                            Pattern p = Pattern.compile("— [0-9]* —");
                            return !p.matcher(x).find();
                        })
                        .collect(Collectors.toList());

                    StringBuilder sb = new StringBuilder();
                    for (String line : resultList) {
                        if (line.length() > 0 && line.length() < 25) {
                            sb.append(line);
                            sb.append("\n");
                        } else {
                            sb.append(line);
                        }
                    }

                    String resultOut = sb.toString();
                    page = handleResponse(request, resultOut);
                }
                this.onSuccess(request);
                this.logger.info("downloading page success {}", request.getUrl());
            } catch (Exception ex) {
                this.onError(request);
                this.logger.warn("download page error {},{}", request.getUrl(), ex);
            } finally {
                try {
                    if (inStream != null) {
                        inStream.close();
                    }
                } catch (Exception e) {
                    logger.error("failed to close file: " + url, e);
                }
            }
            return page;
        } else {
            throw new NullPointerException("task or site can not be null");
        }
    }

    protected Page handleResponse(Request request, String content) throws IOException, InterruptedException {
        Page page = new Page();
        page.setRawText(content);

        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(200);
        return page;
    }

    @Override
    public void setThread(int threadNum) {

    }

}
