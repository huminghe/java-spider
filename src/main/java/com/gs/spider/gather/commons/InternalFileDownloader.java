package com.gs.spider.gather.commons;

import com.gs.spider.utils.PdfUtil;
import com.gs.spider.utils.StaticValue;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.selector.PlainText;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/8/23
 */
@Component
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
            String path = new File(StaticValue.fileSystemPrefix, url).getPath();
            logger.info("loading file from: " + url);
            String result = "";
            Page page = new Page();
            try {
                if (new File(path).isDirectory()) {
                    page = handleResponse(request, "");
                } else {
                    File storeFile = new File(StaticValue.internalFileStorePrefix, url);
                    storeFile.getParentFile().mkdirs();
                    String storePath = storeFile.getPath();
                    PdfUtil.removeWatermarkPDF(path, storePath);
                    boolean needOCR = PdfUtil.needOCR(storePath);
                    if (needOCR) {
                        result = PdfUtil.fetchContentByOCR(storePath);
                    } else {
                        result = PdfUtil.fetchContentByTika(storePath);
                    }
                    String[] resultSplited = result.split("\n\n");
                    List<String> resultList = Arrays.stream(resultSplited)
                        .filter(x -> {
                            Pattern p = Pattern.compile("— [0-9]* —");
                            return !p.matcher(x).find();
                        })
                        .map(line -> line.replaceAll(" ", ""))
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
