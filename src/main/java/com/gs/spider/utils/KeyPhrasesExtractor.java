package com.gs.spider.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author huminghe
 * @date 2022/7/25
 */
@Component
public class KeyPhrasesExtractor {

    private static Logger LOG = LoggerFactory.getLogger(KeyPhrasesExtractor.class);

    public List<String> fetchKeyPhrases(String content) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        List<String> wordList = new LinkedList<>();
        try {
            // 创建httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost(StaticValue.phreseApi);
            // 配置请求参数实例
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(350000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(350000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
            // 为httpPost实例设置配置
            httpPost.setConfig(requestConfig);

            httpPost.addHeader("Content-Type", "application/json;charset=utf-8");

            JSONObject params = new JSONObject();
            params.put("content", content);
            StringEntity s = new StringEntity(params.toJSONString(), "utf-8");
            httpPost.setEntity(s);

            LOG.info("http post: " + httpPost);
            LOG.info("string: " + params.toString());
            LOG.info("json string: " + params.toJSONString());

            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            String entityString = EntityUtils.toString(entity);
            LOG.info("entity response: " + entityString);
            JSONObject jsonObject = JSON.parseObject(entityString);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            LOG.info("result: " + jsonArray);
            if (jsonArray != null) {
                int size = jsonArray.size();
                for (int i = 0; i < size; i++) {
                    String word = jsonArray.getString(i);
                    wordList.add(word);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return wordList;
    }
}
