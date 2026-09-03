package com.superai.workbench.data.payment;

/**
 * 支付结果
 */
public class PayResult {
    public static final int SUCCESS = 0;
    public static final int CANCEL = -1;
    public static final int ERROR = -2;
    public static final int NETWORK = -3;
    public static final int TIMEOUT = -4;

    private int code;
    private String msg;
    private String vipLevel; // 购买的VIP等级标识
    private boolean paid;

    public PayResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.paid = (code == SUCCESS);
    }

    // Getters / Setters
    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public String getVipLevel() { return vipLevel; }
    public void setVipLevel(String vipLevel) { this.vipLevel = vipLevel; }
    public boolean isPaid() { return paid; }
}
