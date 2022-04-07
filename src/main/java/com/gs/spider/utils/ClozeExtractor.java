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

    public List<ClozeResult> extractSentenceKeyQuestion(List<String> sentences, int num) {
        List<ClozeResult> keyQuestionResults = new LinkedList<>();
        List<String> subSentenceList = sentences.stream().flatMap(sentence -> NlpUtil.toSentences(sentence, 126).stream()).collect(Collectors.toList());

        List<List<String>> nerResults = batchFetchNerResults(subSentenceList);
        int startIdx = 0;
        int endIdx = 0;
        for (String sentence : sentences) {
            List<String> subSentence = NlpUtil.toSentences(sentence, 126);
            endIdx = endIdx + subSentence.size();
            List<String> nerList = new LinkedList<>();
            for (int i = startIdx; i < endIdx; i++) {
                nerList.addAll(nerResults.get(i));
            }
            startIdx = endIdx;

            List<String> options = generatePiecesRule1(sentence);
            options.addAll(generatePiecesRule2(sentence));
            int maxLen = nerList.isEmpty() ? 10 : nerList.stream().max(Comparator.comparingInt(String::length)).get().length();
            List<Term> segResultList = KeywordExtractor.hanlpSegment.seg(sentence);
            List<Integer> offsetList = segResultList.stream().map(t -> t.offset).collect(Collectors.toList());
            List<Integer> endList = segResultList.stream().map(t -> t.offset + t.length()).collect(Collectors.toList());
            List<String> namedEntities = nerList.stream()
                .filter(w -> w.length() >= maxLen || w.length() >= 4)
                .filter(w -> entityIndependent(sentence, w))
                .filter(w -> {
                    int idx = StringUtils.indexOf(sentence, w);
                    int end = idx + w.length();
                    return offsetList.contains(idx) && endList.contains(end);
                })
                .collect(Collectors.toList());
            for (String word : namedEntities) {
                if (!wordDuplicateWithList(options, word)) {
                    options.add(word);
                }
            }
            String numOption = generatePiecesRule3(sentence);
            boolean notContainNum = options.stream().noneMatch(x -> x.contains(numOption));
            int numOptionStart = StringUtils.indexOf(sentence, numOption);
            int numOptionEnd = numOptionStart + numOption.length();
            if (notContainNum && offsetList.contains(numOptionStart) && endList.contains(numOptionEnd)) {
                options.add(numOption);
            }
            List<ClozeResult> results = options.stream().filter(StringUtils::isNotBlank)
                .map(option -> generateClozeResult(option, sentence))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            keyQuestionResults.addAll(results);
        }

        return keyQuestionResults.stream().limit(num).collect(Collectors.toList());
    }

    public List<ClozeResult> extractKeyQuestionResult(String content, int num) {
        List<String> topList = extractKeyQuestionCandidates(content, num);
        return extractSentenceKeyQuestion(topList, num);
    }

    public List<String> extractKeyQuestionCandidates(String content, int num) {
        List<String> candidateSentences = generateKeyQuestionSentencesV2(content);
        List<Sentence> keyQuestionSentences = keywordExtractor.generateSummarySentences(candidateSentences);
        return keyQuestionSentences.stream()
            .limit(num * 3L)
            .sorted(Comparator.comparingInt(Sentence::getIdx))
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
            HttpPost httpPost = new HttpPost(StaticValue.nerApi);
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

    private List<String> generateKeyQuestionSentencesV2(String content) {
        List<String> sentences = NlpUtil.toSentence(content);
        List<String> candidateSentences = sentences
            .stream()
            .filter(x -> x.length() > 10)
            .collect(Collectors.toList());
        return candidateSentences;
    }

    private ClozeResult generateClozeResult(String ne, String sentence) {
        if (ne.contains("“") && !ne.contains("”")) {
            ne = ne.replaceAll("“", "");
        } else if (ne.contains("”") && !ne.contains("“")) {
            ne = ne.replaceAll("”", "");
        }
        if (ne.contains("《") && !ne.contains("》")) {
            ne = ne + "》";
        }
        if (ne.length() >= sentence.length() - 1) {
            return null;
        }
        int idx = StringUtils.indexOf(sentence, ne);
        int idxRepeat = StringUtils.indexOf(sentence, ne, idx + 1);
        return new ClozeResult(sentence, ne, idx);
    }

    private boolean entityIndependent(String content, String entity) {
        int idx = StringUtils.indexOf(content, entity);
        int length = content.length();
        int entityLength = entity.length();
        String left = "";
        String right = "";
        if (0 < idx) {
            left = content.substring(idx - 1, idx);
        }
        if (idx + entityLength < length) {
            right = content.substring(idx + entityLength, idx + entityLength + 1);
        }
        return !left.contains("、") && !right.contains("、");
    }

    private boolean wordDuplicateWithList(List<String> wordList, String word) {
        if (wordList.isEmpty()) {
            return false;
        } else {
            return wordList.stream().map(w -> w.contains(word) || word.contains(w))
                .reduce((a, b) -> a || b).orElse(false);
        }
    }

    private boolean verifyPhrase(String word1, String word2) {
        /*
        if (word1.length() == word2.length()) {
            for (int i = 0; i < word1.length(); i++) {
                if (word1.substring(i, i + 1).equals(word2.substring(i, i + 1))) {
                    return true;
                }
            }
        }
        return false;
        */
        return word1.length() == word2.length() && word1.length() >= 2;
    }

    private String getTailWord(String word, String tail) {
        int len = word.length();
        String wordLastChar = word.substring(len - 1, len);
        List<Integer> tailEndList = KeywordExtractor.hanlpSegment.seg(tail)
            .stream()
            .map(t -> t.offset + t.length()).collect(Collectors.toList());
        if (tail.length() >= len && tailEndList.contains(len)) {
            String tailWord = tail.substring(0, len);
            String afterTailWordPart = tail.substring(len);
            int afterLength = Math.min(afterTailWordPart.length(), 2);
            String afterWord = afterTailWordPart.substring(0, afterLength);
            if (verifyPhrase(tailWord, word) && !afterWord.contains(wordLastChar)) {
                return tailWord;
            }
        }
        return "";
    }

    private String getHeadWord(String word, String head) {
        int len = word.length();
        int subLength = Math.min(len, 3);
        String wordMiddleSeq = word.substring(1, subLength);
        List<Integer> headOffsetList = KeywordExtractor.hanlpSegment.seg(head)
            .stream()
            .map(t -> t.offset).collect(Collectors.toList());
        if (head.length() >= len && headOffsetList.contains(head.length() - len)) {
            String headWord = head.substring(head.length() - len);
            String headFirstChar = headWord.substring(0, 1);
            if (verifyPhrase(headWord, word) && !wordMiddleSeq.contains(headFirstChar)) {
                return headWord;
            }
        }
        return "";
    }

    private List<String> generatePiecesRule1(String sentence) {
        String[] outerArr = sentence.split("[，《》；]");
        List<String> optionList = Arrays.stream(outerArr).map(sen -> {
            String[] arr = sen.split("、");
            List<String> resultWordList = new LinkedList<>();
            if (arr.length > 3) {
                List<String> options = Arrays.stream(arr).limit(arr.length - 1).skip(1).collect(Collectors.toList());
                boolean sameLength = true;
                String wordSelected = arr[1];
                int len = wordSelected.length();
                if (len == 0) {
                    return "";
                }
                for (String op : options) {
                    if (len != op.length()) {
                        sameLength = false;
                    }
                }
                if (sameLength) {
                    String head = arr[0];
                    String tail = arr[arr.length - 1];
                    String headWord = getHeadWord(wordSelected, head);
                    if (!headWord.isEmpty()) {
                        resultWordList.add(headWord);
                    }
                    resultWordList.addAll(options);
                    String tailWord = getTailWord(wordSelected, tail);
                    if (!tailWord.isEmpty()) {
                        resultWordList.add(tailWord);
                    }
                }
            } else if (arr.length > 2) {
                String wordSelected = arr[1];
                if (wordSelected.length() == 0) {
                    return "";
                }
                String head = arr[0];
                String tail = arr[arr.length - 1];
                String headWord = getHeadWord(wordSelected, head);
                if (!headWord.isEmpty()) {
                    resultWordList.add(headWord);
                }
                resultWordList.add(wordSelected);
                String tailWord = getTailWord(wordSelected, tail);
                if (!tailWord.isEmpty()) {
                    resultWordList.add(tailWord);
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
            if (!r.startsWith("一") && !r.startsWith("0") && !r.startsWith("1") && r.length() < sentence.length() - 1) {
                return r;
            }
        }
        return "";
    }

}
