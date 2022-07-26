package com.gs.spider.controller.commons.webpage;

import com.gs.spider.service.commons.webpage.HotwordService;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huminghe
 * @date 2022/7/18
 */
@RequestMapping("/commons/hotword")
@Controller
public class HotwordController {

    private Logger logger = LogManager.getLogger(HotwordController.class);

    @Autowired
    private HotwordService hotwordService;


    @RequestMapping(value = "getTitleByDomain", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getTitleByDomain(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                         @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                         @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> titles = hotwordService.getTitleByDomain(domain, 100, page, startDate, endDate);
        return titles;
    }

    @RequestMapping(value = "getHotwordsRaw", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Pair<String, Integer>> getHotwordsRaw(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<Pair<String, Integer>> hotwords = hotwordService.getHotwords(domain, 100, page, startDate, endDate);
        return hotwords;
    }

    @RequestMapping(value = "getHotwordsNew", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsNew(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                       @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                       @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> hotwords = hotwordService.getHotwordsNew(domain, 100, page, startDate, endDate);
        return hotwords;
    }

    @RequestMapping(value = "getHotwordsAllContent", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsAllContent(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                              @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                              @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> hotwords = hotwordService.getHotwordsByAllContent(domain, 100, page, startDate, endDate);
        return hotwords;
    }

    @RequestMapping(value = "getHotwordsV3", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsV3(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<Pair<String, Integer>> hotwords = hotwordService.getHotwordsV3(domain, 100, page, startDate, endDate);
        return hotwords.stream().map(x -> x.getValue0()).collect(Collectors.toList());
    }

    @RequestMapping(value = "getHotwordsV4", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsV4(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<Pair<String, Integer>> hotwords = hotwordService.getHotwordsV4(domain, 100, page, startDate, endDate);
        return hotwords.stream().map(x -> x.getValue0()).collect(Collectors.toList());
    }

    @RequestMapping(value = "getHotwordsV5", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsV5(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> hotwords = hotwordService.getHotwordsV5(domain, 100, page, startDate, endDate);
        return hotwords;
    }

    @RequestMapping(value = "getHotwordsV6", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsV6(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> hotwords = hotwordService.getHotwordsV6(domain, 100, page, startDate, endDate);
        return hotwords;
    }

    @RequestMapping(value = "getHotwordsV7", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<String> getHotwordsV7(String domain, @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "start", required = false, defaultValue = "100") int start,
                                      @RequestParam(value = "end", required = false, defaultValue = "50") int end) {
        Date startDate = DateUtils.addDays(new Date(), -start);
        Date endDate = DateUtils.addDays(new Date(), -end);
        List<String> hotwords = hotwordService.getHotwordsV7(domain, 100, page, startDate, endDate);
        return hotwords;
    }
}
