package com.gs.spider.gather.commons.puppeteer;

import com.ruiyun.jvppeteer.core.page.Mouse;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.core.browser.Browser;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

/**
 * @author huminghe
 * @date 2021/7/15
 * @email huminghe@zhihu.com
 */
@Component
public class PuppeAction extends AbstractChromiumAction {
    @Override
    protected Page doAction(Browser browser, Request request, Page page, Site site) throws Exception {
        Mouse mouse = page.mouse();
        mouse.down();
        mouse.up();
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);

        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.move(20.0, 500.0);
        mouse.move(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        mouse.wheel(20.0, 500.0);
        mouse.wheel(-20.0, 500.0);
        Thread.sleep(3000);
        return page;
    }
}
