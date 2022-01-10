package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2021/12/22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClozeResult {
    private String content;
    private String cloze;
    private int index;
}
