package com.gs.spider.controller.commons.webpage;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.gs.spider.dao.CommonWebpageDAO;
import com.gs.spider.model.commons.Webpage;
import com.gs.spider.model.utils.ClozeResult;
import com.gs.spider.model.utils.ContentInfo;
import com.gs.spider.model.utils.ResultBundle;
import com.gs.spider.model.utils.ResultListBundle;
import com.gs.spider.model.utils.SentencesInfo;
import com.gs.spider.service.commons.webpage.CommonWebpageService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * CommonWebpageController
 */
@RequestMapping("/commons/webpage")
@Controller
public class CommonWebpageController {

    private Logger logger = LogManager.getLogger(CommonWebpageController.class);

    private static final Gson gson = new Gson();

    @Autowired
    private CommonWebpageService webpageService;

    @Autowired
    private CommonWebpageDAO webpageDAO;

    /**
     * 根据spiderUUID获取结果,翻页方式获取
     *
     * @param spiderUUID 任务ID
     * @param size       每页显示多少结果
     * @param page       页码,从1开始
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "getWebpageListBySpiderUUID", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> getWebpageListBySpiderUUID(String spiderUUID, @RequestParam(value = "size", required = false, defaultValue = "10") int size, @RequestParam(value = "page", required = false, defaultValue = "1") int page) throws IOException {
        return webpageService.getWebpageListBySpiderUUID(spiderUUID, size, page);
    }

    /**
     * 根据domain获取结果,按照抓取时间排序
     *
     * @param domain 网站域名
     * @param page   页码
     * @return
     */
    @RequestMapping(value = "getWebpageByDomain", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> getWebpageByDomain(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return webpageService.getWebpageByDomain(domain, 10, page);
    }

    /**
     * 根据ES中的id获取网页
     *
     * @param id 网页id
     * @return
     */
    @RequestMapping(value = "getWebpageById", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Webpage> getWebpageById(String id) {
        return webpageService.getWebpageById(id);
    }

    /**
     * 根据id删除网页
     *
     * @param id 网页id
     * @return 是否删除
     */
    @RequestMapping(value = "deleteById", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Boolean> deleteById(String id) {
        return webpageService.deleteById(id);
    }

    /**
     * 根据关键词搜索网页
     *
     * @param query 关键词
     * @param size  每页数量
     * @param page  页码
     * @return
     */
    @RequestMapping(value = "searchByQuery", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> searchByQuery(String query, @RequestParam(value = "size", required = false, defaultValue = "10") int size, @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return webpageService.searchByQuery(query, size, page);
    }

    /**
     * 根据网站的文章ID获取相似网站的文章
     *
     * @param id   文章ID
     * @param size 页面容量
     * @param page 页码
     * @return
     */
    @RequestMapping(value = "moreLikeThis", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> moreLikeThis(String id, @RequestParam(value = "size", required = false, defaultValue = "10") int size, @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return webpageService.moreLikeThis(id, size, page);
    }

    /**
     * 聚合所有网页的Domain信息
     *
     * @param size 大小
     * @return
     */
    @RequestMapping(value = "countDomain", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Map<String, Long>> countDomain(@RequestParam(value = "size", required = false, defaultValue = "50") int size) {
        return webpageService.countDomain(size);
    }

    /**
     * 统计指定网站每天抓取数量
     *
     * @param domain 网站域名
     * @return
     */
    @RequestMapping(value = "countDomainByGatherTime", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Map<Date, Long>> countDomainByGatherTime(String domain) {
        return webpageService.countDomainByGatherTime(domain);
    }

    /**
     * 根据网站domain删除数据
     *
     * @param domain 网站域名
     * @return 删除任务ID
     */
    @RequestMapping(value = "deleteByDomain", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<String> deleteByDomain(String domain) {
        return webpageService.deleteByDomain(domain);
    }

    /**
     * 开始滚动数据
     *
     * @return 滚动id
     */
    @RequestMapping(value = "startScroll", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Pair<String, List<Webpage>>> startScroll() {
        return webpageService.startScroll();
    }

    /**
     * 根据scrollId获取全部数据
     *
     * @param scrollId scrollId
     * @return 网页列表
     */
    @RequestMapping(value = "scrollAllWebpage", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> scrollAllWebpage(String scrollId) {
        return webpageService.scrollAllWebpage(scrollId);
    }

    /**
     * 获取网页列表,并按照抓取时间排序,仅允许获取前1000条
     *
     * @param size 每页数量
     * @param page 页码
     * @return
     */
    @RequestMapping(value = "listAll", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultListBundle<Webpage> listAll(int size, int page) {
        Preconditions.checkArgument(size * page < 1000, "最多获取前1000条数据");
        return webpageService.listAll(size, page);
    }

    /**
     * 根据spiderinfoID更新数据
     *
     * @param spiderInfoIdUpdateBy 待更新网站模板编号
     * @param callbackUrl          回调地址
     * @param spiderInfoJson       新的网页抽取模板
     * @return 是否全部数据删除成功
     */
    @RequestMapping(value = "updateBySpiderinfoID", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResultBundle<String> updateBySpiderinfoID(String spiderInfoIdUpdateBy, String spiderInfoJson, String callbackUrl) {
        List<String> callbackUrls = Lists.newArrayList(callbackUrl);
        return webpageService.updateBySpiderInfoID(spiderInfoIdUpdateBy, spiderInfoJson, callbackUrls);
    }

    @RequestMapping(value = "updateWebpageInfo", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResultBundle<String> updateWebpageInfo(String id, String title, String contentCleaned, String url, String domain, String domainName,
                                                  String keywords, int level, String publishTime) {
        String[] words = keywords.split(" ");
        List<String> keywordList = new LinkedList<>(Arrays.asList(words));
        return webpageService.updateWebpage(id, title, contentCleaned, url, domain, domainName, keywordList, level, publishTime);
    }

    /**
     * 根据爬虫id导出 webpage的JSON对象
     *
     * @param uuid       爬虫id
     * @param includeRaw 是否包含网页快照
     */
    @RequestMapping(value = "exportWebpageJSONBySpiderUUID", method = RequestMethod.GET, produces = "application/octet-stream")
    public void exportWebpageJSONBySpiderUUID(String uuid,
                                              @RequestParam(value = "includeRaw", required = false, defaultValue = "false") Boolean includeRaw,
                                              HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment;fileName=" + new String(uuid.getBytes("UTF-8"), "iso-8859-1") + ".segtxt");
        OutputStream outputStream = response.getOutputStream();
        webpageService.exportWebpageJSONBySpiderUUID(uuid, includeRaw, outputStream);
        outputStream.close();
    }

    /**
     * 根据domain导出 webpage的JSON对象
     *
     * @param domain     domain
     * @param includeRaw 是否包含网页快照
     */
    @RequestMapping(value = "exportWebpageJSONByDomain", method = RequestMethod.GET, produces = "application/octet-stream")
    public void exportWebpageJSONByDomain(String domain,
                                          @RequestParam(value = "includeRaw", required = false, defaultValue = "false") Boolean includeRaw,
                                          HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment;fileName=" + new String(domain.getBytes("UTF-8"), "iso-8859-1") + ".segtxt");
        OutputStream outputStream = response.getOutputStream();
        webpageService.exportWebpageJSONByDomain(domain, includeRaw, outputStream);
        outputStream.close();
    }

    /**
     * 根据爬虫id导出 webpage的JSON对象
     *
     * @param uuid 爬虫id
     */
    @RequestMapping(value = "exportTitleContentPairBySpiderUUID", method = RequestMethod.GET, produces = "application/octet-stream")
    public void exportTitleContentPairBySpiderUUID(String uuid,
                                                   HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment;fileName=" + new String((uuid).getBytes("UTF-8"), "iso-8859-1") + ".segtxt");
        OutputStream outputStream = response.getOutputStream();
        webpageService.exportTitleContentPairBySpiderUUID(uuid, outputStream);
        outputStream.close();
    }

    @RequestMapping(value = "getNerCorpus", method = RequestMethod.GET)
    public void getNerCorpus(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                             @RequestParam(value = "numPerDoc", required = false, defaultValue = "5") int numPerDoc,
                             HttpServletResponse response) {
        String info = webpageService.getNerCorpusNew(domain, 10, page, numPerDoc);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=nerCorpus.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "getCorpus", method = RequestMethod.GET)
    @ResponseBody
    public void getCorpus(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                          HttpServletResponse response) {
        String info = webpageService.getCorpusNew(domain, 10, page);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=rawCorpus.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "getCorpusByLevel", method = RequestMethod.GET)
    @ResponseBody
    public void getCorpusByLevel(int level, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                 HttpServletResponse response) {
        String info = webpageService.getCorpusByLevel(level, 10, page);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=rawCorpus.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "getTitleByLevel", method = RequestMethod.GET)
    @ResponseBody
    public void getTitleByLevel(int level, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                HttpServletResponse response) {
        String info = webpageService.getTitleByLevel(level, 10, page);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=rawCorpus.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "compareNlp", method = RequestMethod.GET)
    public void compareNlp(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                           @RequestParam(value = "size", required = false, defaultValue = "5") int size,
                           HttpServletResponse response) {
        String info = webpageService.compareNlp(domain, size, page);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=nlpCompare.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "getSentenceKeyQuestion", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public List<ClozeResult> getSentenceKeyQuestion(@RequestBody SentencesInfo contentInfo) {
        List<String> sentences = contentInfo.getSentences();
        int num = contentInfo.getNum();
        List<ClozeResult> result = webpageService.getSentenceKeyQuestion(sentences, num);
        return result;
    }

    @RequestMapping(value = "getKeyQuestion", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public List<ClozeResult> getKeyQuestion(@RequestBody ContentInfo contentInfo) {
        String content = contentInfo.getContent();
        int num = contentInfo.getNum();
        List<ClozeResult> result = webpageService.getKeyQuestion(content, num);
        return result;
    }

    @RequestMapping(value = "getSummary", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public String getSummary(@RequestBody ContentInfo contentInfo) {
        String content = contentInfo.getContent();
        int num = contentInfo.getNum();
        List<String> summarySentences = webpageService.getSummary(content, num);
        return StringUtils.join(summarySentences, " ");
    }

    @RequestMapping(value = "getRelationExtractionCorpus", method = RequestMethod.GET)
    public void getRelationExtractionCorpus(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                            @RequestParam(value = "numPerDoc", required = false, defaultValue = "5") int numPerDoc,
                                            HttpServletResponse response) {
        String info = webpageService.getRelationExtractionCorpus(domain, 10, page, numPerDoc);
        BufferedOutputStream buff = null;
        OutputStream out = null;
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment;filename=relationExtractionCorpus.txt");
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(info.getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "writeRelationExtraction", method = RequestMethod.GET)
    @ResponseBody
    public String writeRelationExtraction(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                          @RequestParam(value = "numPerDoc", required = false, defaultValue = "5") int numPerDoc) {
        return webpageService.writeRelationExtractionCorpus(domain, 10, page, numPerDoc);
    }

    @RequestMapping(value = "updateAll", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResultBundle<Boolean> updateAll() {
        return webpageService.updateAll();
    }

}
