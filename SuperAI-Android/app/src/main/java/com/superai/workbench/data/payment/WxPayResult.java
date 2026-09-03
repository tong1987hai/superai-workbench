package com.superai.workbench.data.payment;

/**
 * 微信支付结果
 */
public class WxPayResult {

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_CANCEL = -1;
    public static final int CODE_ERROR = -2;
    public static final int CODE_NETWORK = -3;
    public static final int CODE_TIMEOUT = -4;

    private final int code;
    private final String message;
    private String outTradeNo;
    private int vipLevel = -1;

    public WxPayResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public int getVipLevel() { return vipLevel; }
    public void setVipLevel(int vipLevel) { this.vipLevel = vipLevel; }

    public boolean isSuccess() { return code == CODE_SUCCESS; }
    public boolean isCancelled() { return code == CODE_CANCEL; }
}
