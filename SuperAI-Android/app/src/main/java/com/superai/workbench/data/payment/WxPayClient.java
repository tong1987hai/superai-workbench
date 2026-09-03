package com.superai.workbench.data.payment;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.superai.workbench.data.model.VIPLevel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Android 客户端微信支付统一下单 & 调起工具
 * 
 * 使用方式：
 * 1. 初始化：WxPayClient client = new WxPayClient(context, "https://your-domain.com");
 * 2. 下单：client.createOrder(vipLevel, deviceId, new WxPayCallback() {...});
 * 3. 在回调中调起微信支付 SDK
 * 
 * 流程：
 * 客户端 -> 商户服务器(/api/pay/create-order) -> 微信统一下单API
 *                |
 *                v
 *          返回 prepay_id + 调起参数
 *                |
 *                v
 *         客户端调起微信支付SDK -> 支付完成 -> 服务器接收回调通知
 */
public class WxPayClient {

    private static final String TAG = "WxPayClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final Handler mainHandler;

    /**
     * @param context 上下文
     * @param serverBaseUrl 服务器地址，如 "https://your-domain.com" 或 "http://192.168.1.100:8080"
     */
    public WxPayClient(Context context, String serverBaseUrl) {
        this.baseUrl = serverBaseUrl.endsWith("/") 
            ? serverBaseUrl.substring(0, serverBaseUrl.length() - 1) 
            : serverBaseUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 创建支付订单
     * 
     * @param vipLevel VIP等级：1=月会员, 2=年会员, 3=终身会员
     * @param deviceId 设备标识（用于订单追踪，如 UUID）
     * @param callback 结果回调（在主线程）
     */
    public void createOrder(int vipLevel, String deviceId, final WxPayCallback callback) {
        // 构建请求体
        CreateOrderRequest requestBody = new CreateOrderRequest(vipLevel, deviceId);
        String json = gson.toJson(requestBody);
        
        RequestBody body = RequestBody.create(json.getBytes(), JSON);
        Request request = new Request.Builder()
            .url(baseUrl + "/api/pay/create-order")
            .post(body)
            .header("Content-Type", "application/json")
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(
                    new WxPayResult(WxPayResult.CODE_ERROR, "网络请求失败: " + e.getMessage())
                ));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError(
                        new WxPayResult(WxPayResult.CODE_ERROR, "服务器错误: " + response.code())
                    ));
                    return;
                }

                String responseBody = response.body().string();
                parseResponse(responseBody, callback);
            }
        });
    }

    /**
     * 解析服务器响应，提取调起参数
     */
    private void parseResponse(String json, WxPayCallback callback) {
        try {
            ApiResponse<UnifiedOrderResponse> apiResponse = 
                gson.fromJson(json, new com.google.gson.reflect.TypeToken<
                    ApiResponse<UnifiedOrderResponse>>(){}.getType());
            
            if (apiResponse == null || apiResponse.getCode() != 200) {
                String msg = apiResponse != null ? apiResponse.getMessage() : "响应解析失败";
                mainHandler.post(() -> callback.onError(
                    new WxPayResult(WxPayResult.CODE_ERROR, msg)
                ));
                return;
            }

            UnifiedOrderResponse data = apiResponse.getData();
            if (data == null || !data.isSuccess()) {
                String msg = data != null ? data.getMessage() : "下单失败";
                mainHandler.post(() -> callback.onError(
                    new WxPayResult(WxPayResult.CODE_ERROR, msg)
                ));
                return;
            }

            // 构建调起参数
            WxPayLaunchParams params = new WxPayLaunchParams(
                data.getAppid(),
                data.getPartnerid(),
                data.getPrepayid(),
                data.getPackageValue(),
                data.getNoncestr(),
                data.getTimestamp(),
                data.getSign(),
                data.getOutTradeNo(),
                data.getTotalFee()
            );

            mainHandler.post(() -> callback.onOrderCreated(params));

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError(
                new WxPayResult(WxPayResult.CODE_ERROR, "解析响应异常: " + e.getMessage())
            ));
        }
    }

    /**
     * 调起微信支付 SDK
     * 
     * 示例调用（在 Activity/Fragment 中）：
     * <pre>
     * IWXAPI wxApi = WXAPIFactory.createWXAPI(context, WXPayConfig.APP_ID);
     * wxApi.registerApp(WXPayConfig.APP_ID);
     * 
     * client.createOrder(1, deviceId, new WxPayCallback() {
     *     public void onOrderCreated(WxPayLaunchParams p) {
     *         PayReq req = WxPayClient.buildPayReq(p);
     *         wxApi.sendReq(req);
     *     }
     *     public void onPayResult(WxPayResult r) { ... }
     *     public void onError(WxPayResult e) { ... }
     * });
     * </pre>
     */
    public static com.tencent.mm.opensdk.modelpay.PayReq buildPayReq(WxPayLaunchParams params) {
        com.tencent.mm.opensdk.modelpay.PayReq req = new com.tencent.mm.opensdk.modelpay.PayReq();
        req.appId = params.getAppId();
        req.partnerId = params.getPartnerId();
        req.prepayId = params.getPrepayId();
        req.packageValue = params.getPackageValue();
        req.nonceStr = params.getNonceStr();
        req.timeStamp = params.getTimeStamp();
        req.sign = params.getSign();
        return req;
    }

    // ========== 内部数据类 ==========

    /**
     * 下单请求体
     */
    static class CreateOrderRequest {
        private Integer vipLevel;
        private String deviceId;
        
        CreateOrderRequest(Integer vipLevel, String deviceId) {
            this.vipLevel = vipLevel;
            this.deviceId = deviceId;
        }
    }

    /**
     * 服务器统一响应格式
     */
    static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;
        
        int getCode() { return code; }
        String getMessage() { return message; }
        T getData() { return data; }
    }

    /**
     * 统一下单响应数据
     */
    static class UnifiedOrderResponse {
        private boolean success;
        private String message;
        private String appid;
        private String partnerid;
        private String prepayid;
        private String packageValue;
        private String noncestr;
        private String timestamp;
        private String sign;
        private String outTradeNo;
        private Integer totalFee;
        
        boolean isSuccess() { return success; }
        String getMessage() { return message; }
        String getAppid() { return appid; }
        String getPartnerid() { return partnerid; }
        String getPrepayid() { return prepayid; }
        String getPackageValue() { return packageValue; }
        String getNoncestr() { return noncestr; }
        String getTimestamp() { return timestamp; }
        String getSign() { return sign; }
        String getOutTradeNo() { return outTradeNo; }
        Integer getTotalFee() { return totalFee; }
    }

    // ========== 回调接口 ==========

    public interface WxPayCallback {
        /**
         * 订单创建成功，返回调起参数
         * 此时应调起微信支付 SDK
         */
        void onOrderCreated(WxPayLaunchParams params);
        
        /**
         * 支付流程最终结果（成功/取消/失败）
         */
        void onPayResult(WxPayResult result);
        
        /**
         * 下单或网络请求失败
         */
        void onError(WxPayResult error);
    }
}
