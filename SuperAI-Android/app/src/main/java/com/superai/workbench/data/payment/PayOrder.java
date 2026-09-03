package com.superai.workbench.data.payment;

/**
 * 支付订单信息（统一下单请求参数）
 */
public class PayOrder {
    private String body;
    private String outTradeNo;
    private int totalFee; // 单位：分
    private String nonceStr;
    private String tradeType = "APP";
    private long timestamp;

    public PayOrder(String body, String outTradeNo, int totalFee) {
        this.body = body;
        this.outTradeNo = outTradeNo;
        this.totalFee = totalFee;
        this.nonceStr = WXPaySign.nonceStr();
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    // Getters
    public String getBody() { return body; }
    public String getOutTradeNo() { return outTradeNo; }
    public int getTotalFee() { return totalFee; }
    public String getNonceStr() { return nonceStr; }
    public String getTradeType() { return tradeType; }
    public long getTimestamp() { return timestamp; }
}
