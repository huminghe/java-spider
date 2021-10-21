package com.gs.spider.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;

/**
 * @author huminghe
 * @date 2021/10/20
 */
public class AuthUtil {

    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxiM4n1HFQ5NzQ3zzbvPmQ/1NFs4Q4rq1lT2WR4p2G6GzgoSdCQcP3o/xmtuxDs4Azc+65CFEMSOMfZqotqpbovIqLhUYh/7wsuwcsd/ezjqd/uq4G6mbXBiq9qqMPT8N2m6GVrF8JhypvMpE0anWaLm2oiIg7Ne2sfmdmzKOI2QIDAQAB";

    public static boolean verifySign(Long ts, String tsSign) {
        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA, null, Base64.decode(PUBLIC_KEY.getBytes()));
        try {
            return sign.verify(ts.toString().getBytes(), Base64.decode(tsSign.getBytes()));
        } catch (Exception e) {
            return false;
        }
    }
}
