package com.gs.spider.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gs.spider.model.utils.ClozeResult;
import com.gs.spider.model.utils.NerResult;
import org.apache.commons.lang3.RandomUtils;
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2022/1/11
 */
public class RelationExtractionCorpusGenerator {

    private static Logger LOG = LoggerFactory.getLogger(RelationExtractionCorpusGenerator.class);

    public static List<ClozeResult> getNerResults(String content) {
        List<ClozeResult> resultList = new LinkedList<>();
        List<String> sentences = NlpUtil.toSentence(content)
            .stream()
            .map(String::trim)
            .filter(s -> s.length() > 32 && s.length() < 75).collect(Collectors.toList());
        int size = sentences.size();
        if (size > 0) {
            List<List<String>> nerResults = batchFetchNerResults(sentences);
            for (int i = 0; i < size; i++) {
                String sen = sentences.get(i);
                List<String> words = nerResults.get(i);
                int wordsNum = words.size();
                if (wordsNum > 0) {
                    int idx = RandomUtils.nextInt(0, wordsNum);
                    String ne = words.get(idx);
                    if (ne.contains("“") && !ne.contains("”")) {
                        ne = ne.replaceAll("“", "");
                    } else if (ne.contains("”") && !ne.contains("“")) {
                        ne = ne.replaceAll("”", "");
                    }
                    ClozeResult result = new ClozeResult(sen, ne, 0);
                    resultList.add(result);
                }
            }
        }
        return resultList;
    }

    public static List<List<String>> batchFetchNerResults(List<String> content) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        List<List<String>> batchResults = new LinkedList<>();

        try {
            // 创建httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost("http://0.0.0.0:7765/v1/ner_all");
            // 配置请求参数实例
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000000)// 设置连接请求超时时间
                .setSocketTimeout(6000000)// 设置读取数据连接超时时间
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
                    List<String> words = new LinkedList<>();
                    JSONArray subArray = jsonArray.getJSONArray(i);
                    int subSize = subArray.size();
                    for (int j = 0; j < subSize; j++) {
                        NerResult r = subArray.getObject(j, NerResult.class);
                        if (r != null) {
                            if (r.getCategory() == 1) {
                                words.add(r.getWord());
                            } else if (r.getCategory() >= 2 && RandomUtils.nextDouble(0, 1) >= 0.7) {
                                words.add(r.getWord());
                            }
                        }
                    }
                    batchResults.add(words);
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
        return batchResults;
    }
}
