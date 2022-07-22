package com.gs.spider.service.commons.webpage;

import com.gs.spider.dao.CommonWebpageDAO;
import com.gs.spider.model.commons.Webpage;
import com.gs.spider.utils.RelationExtractionCorpusGenerator;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private CommonWebpageDAO commonWebpageDAO;

    public List<String> getTitleByDomain(String domain, int size, int page, Date startDate, Date endDate) {
        int idx = 1;
        boolean notFinished = true;
        List<String> titleList = new LinkedList<>();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            List<String> titles = webpages.stream()
                .filter(x -> {
                    Date publishDate = x.getPublishTime();
                    return publishDate != null && x.getPublishTime().compareTo(startDate) > 0 && x.getPublishTime().compareTo(endDate) <= 0;
                })
                .map(Webpage::getTitle)
                .collect(Collectors.toList());
            titleList.addAll(titles);
        }
        return titleList;
    }

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
}
