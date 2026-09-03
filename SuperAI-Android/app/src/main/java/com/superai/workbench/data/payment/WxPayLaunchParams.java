package com.superai.workbench.data.payment;

/**
 * 微信支付调起参数
 * 从服务器下单接口获取，用于调起微信支付 SDK
 */
public class WxPayLaunchParams {

    private final String appId;
    private final String partnerId;
    private final String prepayId;
    private final String packageValue;
    private final String nonceStr;
    private final String timeStamp;
    private final String sign;
    private final String outTradeNo;
    private final int totalFee;

    public WxPayLaunchParams(String appId, String partnerId, String prepayId,
                             String packageValue, String nonceStr, String timeStamp,
                             String sign, String outTradeNo, int totalFee) {
        this.appId = appId;
        this.partnerId = partnerId;
        this.prepayId = prepayId;
        this.packageValue = packageValue;
        this.nonceStr = nonceStr;
        this.timeStamp = timeStamp;
        this.sign = sign;
        this.outTradeNo = outTradeNo;
        this.totalFee = totalFee;
    }

    // Getters
    public String getAppId() { return appId; }
    public String getPartnerId() { return partnerId; }
    public String getPrepayId() { return prepayId; }
    public String getPackageValue() { return packageValue; }
    public String getNonceStr() { return nonceStr; }
    public String getTimeStamp() { return timeStamp; }
    public String getSign() { return sign; }
    public String getOutTradeNo() { return outTradeNo; }
    public int getTotalFee() { return totalFee; }
}
