package com.superai.workbench.data.payment;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 微信支付签名工具（MD5）
 */
public class WXPaySign {

    /**
     * 生成 MD5 签名
     * @param params 参数 Map（不含 sign）
     * @param apiKey API V2 密钥
     */
    public static String generateSign(Map<String, String> params, String apiKey) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v != null && !v.isEmpty() && !"sign".equals(k)) {
                sorted.put(k, v);
            }
        }

        StringBuilder sb = new StringBuilder();
        Set<String> keys = sorted.keySet();
        for (String key : keys) {
            sb.append(key).append("=").append(sorted.get(key)).append("&");
        }
        sb.append("key=").append(apiKey);

        return md5(sb.toString()).toUpperCase();
    }

    /**
     * 验证微信支付回调签名
     */
    public static boolean verifySign(Map<String, String> params, String apiKey) {
        String sign = params.get("sign");
        if (sign == null) return false;
        String expected = generateSign(params, apiKey);
        return expected.equals(sign);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 生成随机字符串
     */
    public static String nonceStr() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
