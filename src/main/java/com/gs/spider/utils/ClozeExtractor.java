package com.gs.spider.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gs.spider.model.utils.ClozeResult;
import com.gs.spider.model.utils.NerResult;
import com.gs.spider.model.utils.Sentence;
import com.hankcs.hanlp.seg.common.Term;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2021/12/14
 */
@Component
public class ClozeExtractor {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private KeywordExtractor keywordExtractor;
    private final AhoCorasickMatcher<Boolean> keyQuestionMatcher;
    private final AhoCorasickMatcher<Boolean> keyQuestionFilter;

    private static final Pattern BOOK_PATTERN = Pattern.compile("《[^》\n\r]{4,}》");

    private static final Pattern NUM_PATTERN = Pattern.compile("([0-9]+[0-9一二三四五六七八九十零〇两亿百万千年月日半 .%/～届中]*)|([一二三四五六七八九十]+[一二三四五六七八九十零〇亿百万千]+[0-9一二三四五六七八九十零〇两亿百万千年月日半 .%/～届中大]*)");

    {
        Set<String> keyQuestionWords = Loader.load("/data/key_question_words.txt");
        keyQuestionMatcher = new AhoCorasickMatcher<>(keyQuestionWords.stream().collect(Collectors.toMap(x -> x, x -> true)));
        Set<String> keyQuestionFilterWords = Loader.load("/data/key_question_filter.txt");
        keyQuestionFilter = new AhoCorasickMatcher<>(keyQuestionFilterWords.stream().collect(Collectors.toMap(x -> x, x -> true)));
    }

    public List<ClozeResult> extractKeyQuestionResult(String content, int num) {
        List<ClozeResult> keyQuestionResults = new LinkedList<>();

        List<String> keyQuestionCandidates = extractKeyQuestionCandidates(content);

        List<String> topList = keyQuestionCandidates.stream().limit(num).collect(Collectors.toList());
        List<String> subSentenceList = topList.stream().flatMap(sentence -> NlpUtil.toSentences(sentence, 62).stream()).collect(Collectors.toList());

        List<List<String>> nerResults = batchFetchNerResults(subSentenceList);
        int startIdx = 0;
        int endIdx = 0;
        for (String sentence : topList) {
            List<String> subSentence = NlpUtil.toSentences(sentence, 62);
            endIdx = endIdx + subSentence.size();
            List<String> nerList = new LinkedList<>();
            for (int i = startIdx; i < endIdx; i++) {
                nerList.addAll(nerResults.get(i));
            }
            startIdx = endIdx;

            Set<String> options = new HashSet<>(generatePiecesRule1(sentence));
            options.addAll(generatePiecesRule2(sentence));
            if (!nerList.isEmpty()) {
                String namedEntity = nerList.stream().max(Comparator.comparingInt(String::length)).get();
                options.add(namedEntity);
            }
            List<String> namedEntities = nerList.stream()
                .filter(w -> w.length() >= 6)
                .collect(Collectors.toList());
            options.addAll(namedEntities);
            String numOption = generatePiecesRule3(sentence);
            boolean notContainNum = options.stream().noneMatch(x -> x.contains(numOption));
            if (notContainNum) {
                options.add(generatePiecesRule3(sentence));
            }
            List<ClozeResult> results = options.stream().filter(StringUtils::isNotBlank)
                .map(option -> {
                    int idx = StringUtils.indexOf(sentence, option);
                    int idxRepeat = StringUtils.indexOf(sentence, option, idx + 1);
                    if (idxRepeat >= 0) {
                        return null;
                    } else {
                        return new ClozeResult(sentence, option, idx);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            keyQuestionResults.addAll(results);
        }

        return keyQuestionResults.stream().limit(num).collect(Collectors.toList());
    }

    public List<String> extractKeyQuestionCandidates(String content) {
        List<String> candidateSentences = generateKeyQuestionSentences(content);
        List<Sentence> keyQuestionSentences = keywordExtractor.generateSummarySentences(candidateSentences);
        return keyQuestionSentences.stream()
            .map(Sentence::getSentence)
            .map(NlpUtil::removeNumPrefix)
            .collect(Collectors.toList());
    }

    private List<List<String>> batchFetchNerResults(List<String> content) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        List<List<String>> batchResults = new LinkedList<>();

        try {
            // 创建httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost("http://0.0.0.0:7765/v1/ner_all");
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
                    List<String> words = new LinkedList<>();
                    JSONArray subArray = jsonArray.getJSONArray(i);
                    int subSize = subArray.size();
                    for (int j = 0; j < subSize; j++) {
                        NerResult r = subArray.getObject(j, NerResult.class);
                        if (r != null && r.getCategory() == 1) {
                            words.add(r.getWord());
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

    private List<String> fetchNerResults(String content) {
        String result = "";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        List<String> wordList = new LinkedList<>();
        try {
            // 创建httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost("http://0.0.0.0:7765/v1/named_entity_recognition");
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
            result = jsonObject.getString("result");
            LOG.info("result: " + result);
            if (result == null) {
                result = "";
            }
            wordList = Arrays.asList(result.split("\t").clone());
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

    private List<String> generateKeyQuestionSentences(String content) {
        List<String> sentences = NlpUtil.toSentence(content);
        List<String> candidateSentences = sentences
            .stream()
            .filter(x -> x.length() > 15)
            .filter(x -> {
                boolean matchKeyQuestionWords = !keyQuestionMatcher.matching(x, false).isEmpty();
                boolean filterFlag = keyQuestionFilter.matching(x, false).isEmpty();
                return matchKeyQuestionWords && filterFlag;
            })
            .collect(Collectors.toList());
        return candidateSentences;
    }

    private boolean verifyPhrase(String word1, String word2) {
        if (word1.length() == word2.length()) {
            for (int i = 0; i < word1.length(); i++) {
                if (word1.substring(i, i + 1).equals(word2.substring(i, i + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> generatePiecesRule1(String sentence) {
        String[] outerArr = sentence.split("，");
        List<String> optionList = Arrays.stream(outerArr).map(sen -> {
            String[] arr = sen.split("、");
            List<String> resultWordList = new LinkedList<>();
            if (arr.length > 3) {
                List<String> options = Arrays.stream(arr).limit(arr.length - 1).skip(1).collect(Collectors.toList());
                boolean sameLength = true;
                int len = arr[1].length();
                for (String op : options) {
                    if (len != op.length()) {
                        sameLength = false;
                    }
                }
                if (sameLength) {
                    String head = arr[0];
                    String tail = arr[arr.length - 1];
                    List<Integer> headOffsetList = KeywordExtractor.hanlpSegment.seg(head)
                        .stream()
                        .map(t -> t.offset).collect(Collectors.toList());
                    if (head.length() >= len && headOffsetList.contains(head.length() - len)) {
                        String headWord = head.substring(head.length() - len);
                        if (verifyPhrase(headWord, arr[1])) {
                            resultWordList.add(headWord);
                        }
                    }
                    resultWordList.addAll(options);
                    List<Integer> tailEndList = KeywordExtractor.hanlpSegment.seg(tail)
                        .stream()
                        .map(t -> t.offset + t.length()).collect(Collectors.toList());
                    if (tail.length() >= len && tailEndList.contains(len)) {
                        String tailWord = tail.substring(0, len);
                        if (verifyPhrase(tailWord, arr[1])) {
                            resultWordList.add(tailWord);
                        }
                    }
                }
            } else if (arr.length > 2) {
                int len = arr[1].length();
                String head = arr[0];
                String tail = arr[arr.length - 1];
                List<Integer> headOffsetList = KeywordExtractor.hanlpSegment.seg(head)
                    .stream()
                    .map(t -> t.offset).collect(Collectors.toList());
                if (head.length() >= len && headOffsetList.contains(head.length() - len)) {
                    String headWord = head.substring(head.length() - len);
                    if (verifyPhrase(headWord, arr[1])) {
                        resultWordList.add(headWord);
                    }
                }
                resultWordList.add(arr[1]);
                List<Integer> tailEndList = KeywordExtractor.hanlpSegment.seg(tail)
                    .stream()
                    .map(t -> t.offset + t.length()).collect(Collectors.toList());
                if (tail.length() >= len && tailEndList.contains(len)) {
                    String tailWord = tail.substring(0, len);
                    if (verifyPhrase(tailWord, arr[1])) {
                        resultWordList.add(tailWord);
                    }
                }
            }
            if (resultWordList.size() > 2) {
                return StringUtils.join(resultWordList, "、");
            } else return "";
        })
            .collect(Collectors.toList());

        return optionList.stream().filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private List<String> generatePiecesRule2(String sentence) {
        List<String> results = new LinkedList<>();
        Matcher m = BOOK_PATTERN.matcher(sentence);
        int start = 0;
        while (m.find(start)) {
            results.add(m.group());
            start = m.end();
        }
        return results;
    }

    private String generatePiecesRule3(String sentence) {
        Matcher m = NUM_PATTERN.matcher(sentence);
        int start = 0;
        while (m.find(start)) {
            String r = m.group();
            start = m.end();
            if (!r.startsWith("一") && !r.startsWith("0") && !r.startsWith("1")) {
                return r;
            }
        }
        return "";
    }

}
