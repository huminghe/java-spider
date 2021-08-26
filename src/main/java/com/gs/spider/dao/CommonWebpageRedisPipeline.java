package com.gs.spider.dao;

import com.google.gson.Gson;
import com.gs.spider.utils.StaticValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

@Component
public class CommonWebpageRedisPipeline implements Pipeline {
    private static Jedis jedis;
    private final boolean needRedis;
    private final String publishChannelName;
    private final Gson gson = new Gson();
    private Logger LOG = LogManager.getLogger(CommonWebpageRedisPipeline.class);

    public CommonWebpageRedisPipeline() {
        this.needRedis = StaticValue.needRedis;
        this.publishChannelName = StaticValue.webpageRedisPublishChannelName;
        if (this.needRedis) {
            LOG.info("正在初始化Redis客户端,Host:{},Port:{}", StaticValue.redisHost, StaticValue.redisPort);
            jedis = new Jedis(StaticValue.redisHost, StaticValue.redisPort);
            LOG.info("Jedis初始化成功,Clients List:{}", jedis.clientList());
        } else {
            LOG.warn("未初始化Redis客户端");
        }
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        if (!needRedis) return;
        long receivedClientsCount = jedis.publish(publishChannelName, gson.toJson(resultItems.getAll()));
    }
}
