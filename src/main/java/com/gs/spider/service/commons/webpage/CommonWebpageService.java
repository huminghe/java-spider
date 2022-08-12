package com.gs.spider.service.commons.webpage;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.gs.spider.dao.CommonWebpageDAO;
import com.gs.spider.gather.commons.CommonSpider;
import com.gs.spider.model.commons.SpiderInfo;
import com.gs.spider.model.commons.Webpage;
import com.gs.spider.model.utils.ClozeResult;
import com.gs.spider.model.utils.ResultBundle;
import com.gs.spider.model.utils.ResultBundleBuilder;
import com.gs.spider.model.utils.ResultListBundle;
import com.gs.spider.utils.ClozeExtractor;
import com.gs.spider.utils.KeywordExtractor;
import com.gs.spider.utils.Loader;
import com.gs.spider.utils.NlpUtil;
import com.gs.spider.utils.RelationExtractionCorpusGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CommonWebpageService
 */
@Component
public class CommonWebpageService {
    private static final Gson gson = new Gson();
    private Logger LOG = LogManager.getLogger(CommonWebpageService.class);
    @Autowired
    private CommonWebpageDAO commonWebpageDAO;
    @Autowired
    private ResultBundleBuilder bundleBuilder;
    @Autowired
    private CommonSpider commonSpider;
    @Autowired
    private ClozeExtractor clozeExtractor;
    @Autowired
    private KeywordExtractor keywordExtractor;

    private static final Map<String, String> domainNameMap = Loader.loadMapping("/data/domain_info.txt");

    /**
     * 根据spiderUUID返回该spider抓取到的文章
     *
     * @param spiderUUID
     * @return
     */
    public ResultListBundle<Webpage> getWebpageListBySpiderUUID(String spiderUUID, int size, int page) {
        return bundleBuilder.listBundle(spiderUUID, () -> commonWebpageDAO.getWebpageBySpiderUUID(spiderUUID, size, page));
    }

    /**
     * 根据domain获取结果,按照抓取时间排序
     *
     * @param domain 网站域名
     * @param size   每页数量
     * @param page   页码
     * @return
     */
    public ResultListBundle<Webpage> getWebpageByDomain(String domain, int size, int page) {
        return bundleBuilder.listBundle(domain, () -> commonWebpageDAO.getWebpageByDomain(domain, size, page));
    }

    /**
     * 根据domain列表获取结果
     *
     * @param domain 网站域名列表
     * @param size   每页数量
     * @param page   页码
     * @return
     */
    public ResultListBundle<Webpage> getWebpageByDomains(Collection<String> domain, int size, int page) {
        return bundleBuilder.listBundle(domain.toString(), () -> commonWebpageDAO.getWebpageByDomains(domain, size, page));
    }

    /**
     * 根据关键词搜索网页
     *
     * @param query 关键词
     * @param size  每页数量
     * @param page  页码
     * @return
     */
    public ResultListBundle<Webpage> searchByQuery(String query, int size, int page) {
        return bundleBuilder.listBundle(query, () -> commonWebpageDAO.searchByQuery(query, size, page));
    }

    /**
     * 根据ES中的id获取网页
     *
     * @param id 网页id
     * @return
     */
    public ResultBundle<Webpage> getWebpageById(String id) {
        return bundleBuilder.bundle(id, () -> commonWebpageDAO.getWebpageById(id));
    }

    /**
     * 根据id删除网页
     *
     * @param id 网页id
     * @return 是否删除
     */
    public ResultBundle<Boolean> deleteById(String id) {
        return bundleBuilder.bundle(id, () -> commonWebpageDAO.deleteById(id));
    }

    /**
     * 获取所有网页,并按照抓取时间排序
     *
     * @param size 每页数量
     * @param page 页码
     * @return
     */
    public ResultListBundle<Webpage> listAll(int size, int page) {
        return bundleBuilder.listBundle(null, () -> commonWebpageDAO.listAll(size, page));
    }

    /**
     * 聚合所有网页的Domain信息
     *
     * @param size 大小
     * @return
     */
    public ResultBundle<Map<String, Long>> countDomain(int size) {
        return bundleBuilder.bundle(null, () -> commonWebpageDAO.countDomain(size));
    }

    /**
     * 聚合所有网页的Domain信息
     *
     * @return
     */
    public ResultBundle<Map<String, Long>> countWordByDomain(String domain) {
        return bundleBuilder.bundle(null, () -> commonWebpageDAO.countWordByDomain(domain));
    }

    /**
     * 根据网站的文章ID获取相似网站的文章
     *
     * @param id   文章ID
     * @param size 页面容量
     * @param page 页码
     * @return
     */
    public ResultListBundle<Webpage> moreLikeThis(String id, int size, int page) {
        return bundleBuilder.listBundle(id, () -> commonWebpageDAO.moreLikeThis(id, size, page));
    }

    /**
     * 统计指定网站每天抓取数量
     *
     * @param domain 网站域名
     * @return
     */
    public ResultBundle<Map<Date, Long>> countDomainByGatherTime(String domain) {
        return bundleBuilder.bundle(domain, () -> commonWebpageDAO.countDomainByGatherTime(domain));
    }

    /**
     * 根据网站domain删除数据
     *
     * @param domain 网站域名
     * @return 删除任务ID
     */
    public ResultBundle<String> deleteByDomain(String domain) {
        return bundleBuilder.bundle(domain, () -> commonSpider.deleteByDomain(domain));
    }

    public ResultBundle<Boolean> updateAll() {
        return bundleBuilder.bundle(null, () -> commonWebpageDAO.updateAll());
    }

    /**
     * 开始滚动数据
     *
     * @return 滚动id
     */
    public ResultBundle<Pair<String, List<Webpage>>> startScroll() {
        return bundleBuilder.bundle(null, () -> commonWebpageDAO.startScroll());
    }

    /**
     * 根据scrollId获取全部数据
     *
     * @param scrollId scrollId
     * @return 网页列表
     */
    public ResultListBundle<Webpage> scrollAllWebpage(String scrollId) {
        return bundleBuilder.listBundle(scrollId, () -> commonWebpageDAO.scrollAllWebpage(scrollId));
    }

    /**
     * 根据spiderinfoID更新数据
     *
     * @param spiderInfoIdUpdateBy 待更新网站模板编号
     * @param callbackUrls         回调地址
     * @param spiderInfoJson       新的网页抽取模板JSON
     * @return 是否全部数据删除成功
     */
    public ResultBundle<String> updateBySpiderInfoID(String spiderInfoIdUpdateBy, String spiderInfoJson, List<String> callbackUrls) {
        SpiderInfo spiderInfo = gson.fromJson(spiderInfoJson, SpiderInfo.class);
        return bundleBuilder.bundle(spiderInfoIdUpdateBy, () -> commonSpider.updateBySpiderinfoID(spiderInfoIdUpdateBy, spiderInfo, callbackUrls));
    }

    public ResultBundle<String> updateWebpage(String id, String title, String contentCleaned, String url, String domain, String domainName,
                                              List<String> keywords, int level, String publishDate) {
        Webpage webpage = new Webpage();
        Date publishTime = new Date();
        if (!publishDate.isEmpty()) {
            DateFormat format = new SimpleDateFormat("yyyyMMdd");
            try {
                publishTime = format.parse(publishDate);
            } catch (Exception e) {
                LOG.info("parse publish date error, ", e);
            }
        }
        if (!id.isEmpty()) {
            webpage = commonWebpageDAO.getWebpageById(id);
        } else {
            webpage.setGathertime(publishTime);
            webpage.setContent(contentCleaned);
            String newId = Hashing.md5().hashString(url.replaceAll("https://", "http://"), Charset.forName("utf-8")).toString();
            webpage.setId(newId);
        }
        webpage.setPublishTime(publishTime);
        webpage.setTitle(title);
        webpage.setContentCleaned(contentCleaned);
        webpage.setUrl(url);
        webpage.setDomain(domain);
        if (domainName.isEmpty()) {
            domainName = domainNameMap.getOrDefault(domain, "其他");
        }
        webpage.setDomainName(domainName);
        List<String> algoKeywords = keywordExtractor.extractKeywords(title, contentCleaned);
        for (String word: algoKeywords) {
            if (word != null && !keywords.contains(word)) {
                keywords.add(word);
            }
        }
        webpage.setKeywords(keywords);
        List<String> summary = keywordExtractor.extractSummary(contentCleaned);
        webpage.setSummary(summary);
        webpage.setLevel(level);
        final Webpage webpageNew = webpage;
        return bundleBuilder.bundle(id, () -> commonWebpageDAO.upsert(webpageNew));
    }

    /**
     * 获取query的关联信息
     *
     * @param query 查询queryString
     * @param size  结果集数量
     * @return 相关信息
     */
    public ResultBundle<Pair<Map<String, List<? extends Terms.Bucket>>, List<Webpage>>> relatedInfo(String query, int size) {
        return bundleBuilder.bundle(query, () -> commonWebpageDAO.relatedInfo(query, size));
    }

    /**
     * 根据爬虫id导出 标题-正文 对
     *
     * @param uuid         爬虫id
     * @param outputStream 文件输出流
     */
    public void exportTitleContentPairBySpiderUUID(String uuid, OutputStream outputStream) {
        commonWebpageDAO.exportTitleContentPairBySpiderUUID(uuid, outputStream);
    }

    /**
     * 根据爬虫id导出 webpage的JSON对象
     *
     * @param uuid         爬虫id
     * @param includeRaw   是否包含网页快照
     * @param outputStream 文件输出流
     */
    public void exportWebpageJSONBySpiderUUID(String uuid, Boolean includeRaw, OutputStream outputStream) {
        commonWebpageDAO.exportWebpageJSONBySpiderUUID(uuid, includeRaw, outputStream);
    }

    /**
     * 根据域名导出 webpage的JSON对象
     *
     * @param domain       域名
     * @param includeRaw   是否包含网页快照
     * @param outputStream 文件输出流
     */
    public void exportWebpageJSONByDomain(String domain, Boolean includeRaw, OutputStream outputStream) {
        commonWebpageDAO.exportWebpageJSONByDomain(domain, includeRaw, outputStream);
    }

    /**
     * 根据关键词和域名分页查找
     *
     * @param query
     * @param domain
     * @param size
     * @param page
     * @return
     */
    public ResultBundle<Pair<List<Webpage>, Long>> getWebPageByKeywordAndDomain(String query, String domain, int size, int page) {
        return bundleBuilder.bundle(query, () -> commonWebpageDAO.getWebpageByKeywordAndDomain(query, domain, size, page));
    }

    public ResultBundle<Pair<List<Webpage>, Long>> getWebPageByKeywordDomainAndId(String query, String id, String domain, int size, int page) {
        return bundleBuilder.bundle(query, () -> commonWebpageDAO.getWebpageByKeywordDomainAndId(query, id, domain, size, page));
    }

    public String getNerCorpusNew(String domain, int size, int page, int numPerDoc) {
        int idx = 1;
        boolean notFinished = true;
        StringBuffer sb = new StringBuffer();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            webpages.stream().map(webpage -> {
                String content = webpage.getContentCleaned();
                List<String> sentences = NlpUtil.toSentences(content, 64);
                Collections.shuffle(sentences);
                return sentences;
            })
                .forEach(sentences -> sentences.stream().filter(s -> s.length() > 15)
                    .limit(numPerDoc).forEach(s -> {
                        sb.append(s);
                        sb.append("\n");
                    }));
        }
        return sb.toString();
    }

    public String getCorpusNew(String domain, int size, int page) {
        int idx = 1;
        boolean notFinished = true;
        StringBuffer sb = new StringBuffer();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            webpages.stream().map(Webpage::getContentCleaned)
                .forEach(para -> {
                    sb.append(para);
                    sb.append("\n");
                });
        }
        return sb.toString();
    }

    public String getCorpusByLevel(int level, int size, int page) {
        int idx = 1;
        boolean notFinished = true;
        StringBuffer sb = new StringBuffer();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByLevel(level, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            webpages.stream().map(Webpage::getContentCleaned)
                .forEach(para -> {
                    sb.append(para);
                    sb.append("\n");
                });
        }
        return sb.toString();
    }

    public String getTitleByLevel(int level, int size, int page) {
        int idx = 1;
        boolean notFinished = true;
        StringBuffer sb = new StringBuffer();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByLevel(level, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            webpages.stream().map(Webpage::getTitle)
                .forEach(para -> {
                    sb.append(para);
                    sb.append("\n");
                });
        }
        return sb.toString();
    }

    public String compareNlp(String domain, int size, int page) {

        List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, page);
        List<String> info = webpages.stream().map(webpage -> {
            String content = webpage.getContentCleaned();
            String title = webpage.getTitle();
            String url = webpage.getUrl();
            List<String> lgKeywords = commonSpider.getKeywordsExtractor().extractKeywords(title, content);
            List<String> lgSummary = commonSpider.getKeywordsExtractor().extractSummary(content);
            List<String> hanlpKeywords = commonSpider.getNamedEntitiesExtractor().extractKeywords(title, content);
            List<String> hanlpSummary = commonSpider.getNamedEntitiesExtractor().extractSummary(content);
            StringBuilder sb = new StringBuilder();
            sb.append("标题\n");
            sb.append(title);
            sb.append("\n");
            sb.append("内容链接\n");
            sb.append(url);
            sb.append("\n");
            sb.append("当前采用的算法\n");
            sb.append(StringUtils.join(lgKeywords, ", "));
            sb.append("\n");
            sb.append(StringUtils.join(lgSummary, " "));
            sb.append("\n");
            sb.append("Hanlp 算法\n");
            sb.append(StringUtils.join(hanlpKeywords, ", "));
            sb.append("\n");
            sb.append(StringUtils.join(hanlpSummary, " "));
            sb.append("\n");
            return sb.toString();
        })
            .collect(Collectors.toList());

        return StringUtils.join(info, "\n");
    }

    public List<ClozeResult> getSentenceKeyQuestion(List<String> sentences, int num) {
        return clozeExtractor.extractSentenceKeyQuestion(sentences, num);
    }

    public List<ClozeResult> getKeyQuestion(String content, int num) {
        return clozeExtractor.extractKeyQuestionResult(content, num);
    }

    public List<String> getSummary(String content, int num) {
        return keywordExtractor.extractSummary(content, num);
    }

    public String getRelationExtractionCorpus(String domain, int size, int page, int numPerDoc) {
        int idx = 1;
        boolean notFinished = true;
        StringBuffer sb = new StringBuffer();
        while (idx <= page && notFinished) {
            List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
            idx++;
            notFinished = webpages.size() >= size;
            webpages.stream().map(webpage -> {
                String content = webpage.getContentCleaned();
                List<ClozeResult> nerResults = RelationExtractionCorpusGenerator.getNerResults(content);
                Collections.shuffle(nerResults);
                return nerResults;
            }).forEach(ner -> {
                ner.stream().limit(numPerDoc).forEach(n -> {
                    sb.append(n.getContent());
                    sb.append("\t");
                    sb.append(n.getCloze());
                    sb.append("\n");
                });
            });
        }
        return sb.toString();
    }

    public String writeRelationExtractionCorpus(String domain, int size, int page, int numPerDoc) {
        int idx = 1;
        boolean notFinished = true;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/data/huminghe/tmp2/relation_extraction/" + domain + ".txt"), StandardCharsets.UTF_8));
            BufferedWriter finalWriter = writer;
            while (idx <= page && notFinished) {
                List<Webpage> webpages = commonWebpageDAO.getWebpageByDomain(domain, size, idx);
                idx++;
                notFinished = webpages.size() >= size;
                webpages.stream().map(webpage -> {
                    String content = webpage.getContentCleaned();
                    List<ClozeResult> nerResults = RelationExtractionCorpusGenerator.getNerResults(content);
                    Collections.shuffle(nerResults);
                    return nerResults;
                })
                    .forEach(ner -> ner.stream()
                        .limit(numPerDoc).forEach(n -> {
                            try {
                                finalWriter.write(n.getContent());
                                finalWriter.write("\t");
                                finalWriter.write(n.getCloze());
                                finalWriter.newLine();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        return "success";
    }

}
