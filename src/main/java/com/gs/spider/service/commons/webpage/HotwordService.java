package com.gs.spider.service.commons.webpage;

import com.gs.spider.dao.CommonWebpageDAO;
import com.gs.spider.model.commons.Webpage;
import com.gs.spider.utils.KeyPhrasesExtractor;
import com.gs.spider.utils.KeywordExtractor;
import com.gs.spider.utils.RelationExtractionCorpusGenerator;
import com.hankcs.hanlp.HanLP;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2022/7/18
 */
@Component
public class HotwordService {

    private Logger logger = LogManager.getLogger(HotwordService.class);

    @Autowired
    private CommonWebpageDAO commonWebpageDAO;

    @Autowired
    private KeyPhrasesExtractor keyPhrasesExtractor;

    @Autowired
    private KeywordExtractor keywordExtractor;

    public List<Pair<String, Integer>> getHotwords(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> titles = getTitleByDomain(domain, size, page, startDate, endDate);
        List<List<String>> entities = RelationExtractionCorpusGenerator.batchFetchNerResults(titles);
        return entities.stream().flatMap(Collection::stream)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .sorted((a, b) -> Integer.compare(b.getValue1(), a.getValue1()))
            .collect(Collectors.toList());
    }

    public List<String> getHotwordsNew(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> titles = getTitleByDomain(domain, size, page, startDate, endDate);
        String titlesCombined = StringUtils.join(titles, " ");
        logger.info(titlesCombined);

        List<String> words = HanLP.extractPhrase(titlesCombined, 200);
        return words;
    }

    public List<String> getHotwordsByAllContent(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> contents = getAllContentByDomain(domain, size, page, startDate, endDate);
        String contentsCombined = StringUtils.join(contents, " ");

        List<String> words = HanLP.extractPhrase(contentsCombined, 200);
        return words;
    }

    public List<Pair<String, Integer>> getHotwordsV3(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> titles = getTitleByDomain(domain, size, page, startDate, endDate);
        List<List<String>> phrases = titles.stream().map(x -> keyPhrasesExtractor.fetchKeyPhrases(x)).collect(Collectors.toList());
        return phrases.stream().flatMap(Collection::stream)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .sorted((a, b) -> Integer.compare(b.getValue1(), a.getValue1()))
            .collect(Collectors.toList());
    }

    public List<Pair<String, Integer>> getHotwordsV4(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> contents = getAllContentByDomain(domain, size, page, startDate, endDate);
        List<List<String>> phrases = contents.stream().map(x -> keyPhrasesExtractor.fetchKeyPhrases(x)).collect(Collectors.toList());
        return phrases.stream().flatMap(Collection::stream)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .sorted((a, b) -> Integer.compare(b.getValue1(), a.getValue1()))
            .collect(Collectors.toList());
    }

    public List<String> getHotwordsV5(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> titles = getTitleByDomain(domain, size, page, startDate, endDate);
        String titlesCombined = StringUtils.join(titles, " ");

        List<String> words = keywordExtractor.extractKeywordsCustom("", titlesCombined, 200, 0.15);
        return words;
    }

    public List<String> getHotwordsV6(String domain, int size, int page, Date startDate, Date endDate) {
        List<String> titles = getTitleByDomain(domain, size, page, startDate, endDate);
        String titlesCombined = StringUtils.join(titles, " ");

        List<String> contents = getContentByDomain(domain, size, page, startDate, endDate);
        String contentsCombined = StringUtils.join(contents, " ");

        List<String> words = keywordExtractor.extractKeywordsCustom(titlesCombined, contentsCombined, 200, 0.15);
        return words;
    }

    public List<String> getHotwordsV7(String domain, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpageList = getWebpagesByDomain(domain, size, page, startDate, endDate);
        List<List<String>> keywords = webpageList.stream().map(x -> keywordExtractor.extractKeywordsCustom(x.getTitle(), x.getContentCleaned(), 6, 0.2))
            .collect(Collectors.toList());
        return keywords.stream().flatMap(Collection::stream)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(x -> x))
            .entrySet()
            .stream().map(x -> {
                String word = x.getKey();
                int cnt = x.getValue().size();
                return new Pair<>(word, cnt);
            })
            .sorted((a, b) -> Integer.compare(b.getValue1(), a.getValue1()))
            .map(x -> x.getValue0())
            .collect(Collectors.toList());

    }

    public List<String> getHotwordsByLevelV1(int level, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpages = getWebpagesByLevel(level, size, page, startDate, endDate);
        List<String> titles = webpages.stream().map(Webpage::getTitle).collect(Collectors.toList());
        String titlesCombined = StringUtils.join(titles, " ");
        List<String> words = HanLP.extractPhrase(titlesCombined, 200);
        return words;
    }

    public List<String> getHotwordsByLevelV2(int level, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpages = getWebpagesByLevel(level, size, page, startDate, endDate);
        List<String> contents = webpages.stream().map(x -> x.getTitle() + " " + x.getContentCleaned()).collect(Collectors.toList());
        String contentsCombined = StringUtils.join(contents, " ");
        List<String> words = HanLP.extractPhrase(contentsCombined, 200);
        return words;
    }

    private List<Webpage> getWebpagesByLevel(int level, int size, int page, Date startDate, Date endDate) {
        int idx = 1;
        boolean notFinished = true;
        List<Webpage> webpageList = new LinkedList<>();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByLevel(level, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            List<Webpage> contents = webpages.stream()
                .filter(x -> {
                    Date publishDate = x.getPublishTime();
                    return publishDate != null && x.getPublishTime().compareTo(startDate) > 0 && x.getPublishTime().compareTo(endDate) <= 0;
                })
                .collect(Collectors.toList());
            webpageList.addAll(contents);
        }
        return webpageList;
    }

    private List<String> getTitleByDomain(String domain, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpages = getWebpagesByDomain(domain, size, page, startDate, endDate);
        return webpages.stream().map(Webpage::getTitle).collect(Collectors.toList());
    }

    private List<String> getAllContentByDomain(String domain, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpages = getWebpagesByDomain(domain, size, page, startDate, endDate);
        return webpages.stream().map(x -> x.getTitle() + " " + x.getContentCleaned()).collect(Collectors.toList());
    }

    private List<String> getContentByDomain(String domain, int size, int page, Date startDate, Date endDate) {
        List<Webpage> webpages = getWebpagesByDomain(domain, size, page, startDate, endDate);
        return webpages.stream().map(Webpage::getContentCleaned).collect(Collectors.toList());
    }

    private List<Webpage> getWebpagesByDomain(String domain, int size, int page, Date startDate, Date endDate) {
        int idx = 1;
        boolean notFinished = true;
        List<Webpage> webpageList = new LinkedList<>();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            List<Webpage> contents = webpages.stream()
                .filter(x -> {
                    Date publishDate = x.getPublishTime();
                    return publishDate != null && x.getPublishTime().compareTo(startDate) > 0 && x.getPublishTime().compareTo(endDate) <= 0;
                })
                .collect(Collectors.toList());
            webpageList.addAll(contents);
        }
        return webpageList;
    }

}
