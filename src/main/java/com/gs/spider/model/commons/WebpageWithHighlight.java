package com.gs.spider.model.commons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author huminghe
 * @date 2021/7/6
 * @email huminghe@zhihu.com
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebpageWithHighlight extends Webpage {
    private Highlight highlights;
}
