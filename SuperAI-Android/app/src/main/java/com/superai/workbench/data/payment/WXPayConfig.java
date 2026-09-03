package com.superai.workbench.data.payment;

/**
 * 微信支付配置
 * 用户需在商户平台获取并填入真实值
 */
public class WXPayConfig {

    // ========== 必填配置（替换为真实值）==========
    /** 微信开放平台 AppID */
    public static final String APP_ID = "wx1234567890abcdef";
    /** 微信支付商户号 */
    public static final String MCH_ID = "1234567890";
    /** API V2 密钥（32位，用于签名验证） */
    public static final String API_KEY = "your_32_char_api_key_here";
    /** 统一下单 API 地址 */
    public static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    // ========== 可选配置 ==========
    /** 支付回调通知地址（由商户服务器接收） */
    public static final String NOTIFY_URL = "https://your-domain.com/api/wxpay/notify";
    /** 是否沙箱环境 */
    public static final boolean SANDBOX = false;

    private WXPayConfig() {}
}
