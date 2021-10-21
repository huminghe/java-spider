package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2021/10/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {
    private String filePath;
    private String accountName;
}
