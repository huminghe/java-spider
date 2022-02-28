package com.gs.spider.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class StaticValue {

    private static Logger LOG = LogManager.getLogger(StaticValue.class);

    public static String esHost;
    public static int esPort;
    public static String commonsIndex;
    public static int commonSpiderTaskManagerPort;
    public static long maxHttpDownloadLength;
    public static boolean commonsSpiderDebug;
    public static String esClusterName;
    /**
     * 删除任务延时,单位为小时
     */
    public static int taskDeleteDelay;
    /**
     * 删除任务时间间隔,单位为小时
     */
    public static int taskDeletePeriod;
    /**
     * 普通网页下载器队列最大长度限制
     */
    public static int limitOfCommonWebpageDownloadQueue;
    /**
     * 是否需要Redis
     */
    public static boolean needRedis;
    public static boolean needEs;
    public static int redisPort;
    public static String redisHost;
    public static String webpageRedisPublishChannelName;
    /**
     * 抓取页面比例,如果抓取页面超过最大抓取数量ratio倍的时候仍未达到最大抓取数量爬虫也退出
     */
    public static int commonsWebpageCrawlRatio;
    public static String ajaxDownloader;
    public static String fileSystemPrefix;
    public static String internalFileStorePrefix;
    public static String pdfFontPath;
    public static String wordVectorsPath;
    public static String ocrApi;
    public static String nerApi;

    static {
        LOG.debug("正在初始化StaticValue");
        try {
            String env = System.getenv("SPIDER_ENV");
            if (StringUtils.isBlank(env)) {
                env = "local";
            }
            String configPath = String.format("staticvalue-%s.json", env);
            String json = FileUtils.readFileToString(new File(StaticValue.class.getClassLoader()
                .getResource(configPath).getFile()));
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(json);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            esHost = jsonObject.get("esHost").getAsString();
            esPort = jsonObject.get("esPort").getAsInt();
            esClusterName = jsonObject.get("esClusterName").getAsString();
            commonsIndex = jsonObject.get("commonsIndex").getAsString();
            maxHttpDownloadLength = jsonObject.get("maxHttpDownloadLength").getAsLong();
            commonsSpiderDebug = jsonObject.get("commonsSpiderDebug").getAsBoolean();
            taskDeleteDelay = jsonObject.get("taskDeleteDelay").getAsInt();
            taskDeletePeriod = jsonObject.get("taskDeletePeriod").getAsInt();
            limitOfCommonWebpageDownloadQueue = jsonObject.get("limitOfCommonWebpageDownloadQueue").getAsInt();
            redisPort = jsonObject.get("redisPort").getAsInt();
            redisHost = jsonObject.get("redisHost").getAsString();
            needRedis = jsonObject.get("needRedis").getAsBoolean();
            needEs = jsonObject.get("needEs").getAsBoolean();
            webpageRedisPublishChannelName = jsonObject.get("webpageRedisPublishChannelName").getAsString();
            commonsWebpageCrawlRatio = jsonObject.get("commonsWebpageCrawlRatio").getAsInt();
            ajaxDownloader = jsonObject.get("ajaxDownloader").getAsString();
            fileSystemPrefix = jsonObject.get("fileSystemPrefix").getAsString();
            internalFileStorePrefix = jsonObject.get("internalFileStorePrefix").getAsString();
            pdfFontPath = jsonObject.get("pdfFontPath").getAsString();
            wordVectorsPath = jsonObject.get("wordVectorsPath").getAsString();
            ocrApi = jsonObject.get("ocrApi").getAsString();
            nerApi = jsonObject.get("nerApi").getAsString();
            LOG.debug("StaticValue初始化成功");
        } catch (IOException e) {
            LOG.fatal("初始化StaticValue失败," + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

}
