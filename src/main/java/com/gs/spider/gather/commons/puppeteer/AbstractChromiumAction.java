package com.gs.spider.gather.commons.puppeteer;

import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.protocol.network.CookieParam;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author zky
 * @Date 2020/12/29 14:35
 * chromium操作接口，实现该类可以灵活浏览器
 */
public abstract class AbstractChromiumAction {
    //浏览器操作
    protected abstract Page doAction(Browser browser, Request request, Page page, Site site) throws Exception;

    //浏览器执行请求
    public Page execute(Browser browser, Request request, Page page, Site site) throws Exception {

        //设置cookies
        //setCookie(page, site);

        //具体执行
        return doAction(browser, request, page, site);
    }

    ;

    //设置cookie
    private void setCookie(Page page, Site site) {
        //禁用cookies判断 todo
        //设置cookies
        List<CookieParam> cookieList = new ArrayList<>();
        for (Map.Entry<String, String> cookieEntry : site.getCookies().entrySet()) {
            CookieParam param = new CookieParam();
            param.setName(cookieEntry.getKey());
            param.setValue(cookieEntry.getValue());
            param.setDomain(site.getDomain());

        }
        for (Map.Entry<String, Map<String, String>> domainEntry : site.getAllCookies().entrySet()) {
            for (Map.Entry<String, String> cookieEntry : domainEntry.getValue().entrySet()) {
                CookieParam param = new CookieParam();
                param.setName(cookieEntry.getKey());
                param.setValue(cookieEntry.getValue());
                param.setDomain(site.getDomain());
            }
        }

        try {
            page.setCookie(cookieList);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
