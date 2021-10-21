package com.gs.spider.model.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huminghe
 * @date 2021/10/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthSign {
    private long ts;
    private String tsSign;
    private String accountName;
}
