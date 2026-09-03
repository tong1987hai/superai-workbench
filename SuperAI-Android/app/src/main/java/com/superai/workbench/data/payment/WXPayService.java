package com.superai.workbench.data.payment;

import com.google.gson.Gson;
import com.superai.workbench.data.model.VIPLevel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 微信支付统一下单服务
 * 负责调用微信统一下单API，生成预支付订单，返回调起支付所需的参数
 * 
 * 流程：
 * 1. 客户端生成订单信息 -> 调商户服务器接口生成统一下单参数
 * 2. 商户服务器调用微信统一下单 API 获取 prepay_id
 * 3. 商户服务器返回包含 prepay_id 的调起参数给客户端
 * 4. 客户端调起微信支付 SDK
 */
public class WXPayService {

    private static final String TAG = "WXPayService";
    private static final MediaType XML_TYPE = MediaType.parse("application/xml; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public WXPayService() {
        client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 向商户服务器请求统一下单
     * 实际项目中，统一下单应在商户服务器完成（安全要求，密钥不可放在客户端）
     * 此方法仅作演示，生产环境必须走服务器代理
     * 
     * @param order 支付订单
     * @param vipLevel VIP等级（0/1/2/3 对应 FREE/MONTHLY/YEARLY/LIFETIME）
     * @param callback 回调
     */
    public void requestUnifiedOrder(PayOrder order, int vipLevel, UnifiedOrderCallback callback) {
        // ===== 生产环境：调商户服务器接口 =====
        // 请替换为你的服务器下单接口
        String serverUrl = "https://your-domain.com/api/pay/create-order";
        
        Map<String, Object> params = new HashMap<>();
        params.put("vip_level", vipLevel);
        params.put("body", order.getBody());
        params.put("total_fee", order.getTotalFee());
        params.put("out_trade_no", order.getOutTradeNo());
        params.put("trade_type", "APP");
        
        String jsonBody = gson.toJson(params);
        RequestBody body = RequestBody.create(jsonBody.getBytes(), MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
            .url(serverUrl)
            .post(body)
            .header("Content-Type", "application/json")
            .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                callback.onError(new Exception("服务器请求失败: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("服务器返回错误: " + response.code()));
                    return;
                }
                
                try {
                    String respJson = response.body().string();
                    Map respMap = gson.fromJson(respJson, Map.class);
                    
                    // 提取调起参数
                    if (respMap.containsKey("appid") && respMap.containsKey("prepayid")) {
                        Map<String, String> payParams = new HashMap<>();
                        payParams.put("appid", String.valueOf(respMap.get("appid")));
                        payParams.put("partnerid", String.valueOf(respMap.get("partnerid")));
                        payParams.put("prepayid", String.valueOf(respMap.get("prepayid")));
                        payParams.put("package", "Sign=WXPay");
                        payParams.put("noncestr", String.valueOf(respMap.get("noncestr")));
                        payParams.put("timestamp", String.valueOf(respMap.get("timestamp")));
                        payParams.put("sign", String.valueOf(respMap.get("sign")));
                        callback.onSuccess(payParams);
                    } else if (respMap.containsKey("error")) {
                        callback.onError(new Exception(String.valueOf(respMap.get("error"))));
                    } else {
                        callback.onError(new Exception("服务器返回格式异常"));
                    }
                } catch (Exception e) {
                    callback.onError(new Exception("解析服务器响应失败: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * ===== 开发测试：直连微信统一下单 =====
     * ⚠️ 仅用于开发测试，生产环境严禁在客户端直接调微信统一下单 API！
     * 密钥暴露风险极高，必须在商户服务器完成签名和下单。
     * 
     * @param order 订单信息
     * @param callback 回调
     */
    public void requestUnifiedOrderDev(PayOrder order, UnifiedOrderCallback callback) {
        Map<String, String> params = new TreeMap<>();
        params.put("appid", WXPayConfig.APP_ID);
        params.put("mch_id", WXPayConfig.MCH_ID);
        params.put("nonce_str", order.getNonceStr());
        params.put("body", order.getBody());
        params.put("out_trade_no", order.getOutTradeNo());
        params.put("total_fee", String.valueOf(order.getTotalFee()));
        params.put("spbill_create_ip", "127.0.0.1");
        params.put("notify_url", WXPayConfig.NOTIFY_URL);
        params.put("trade_type", "APP");
        
        String sign = WXPaySign.generateSign(params, WXPayConfig.API_KEY);
        params.put("sign", sign);
        
        String xmlBody = mapToXml(params);
        
        RequestBody body = RequestBody.create(xmlBody.getBytes(), XML_TYPE);
        Request request = new Request.Builder()
            .url(WXPayConfig.UNIFIED_ORDER_URL)
            .post(body)
            .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                callback.onError(e);
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("统一下单请求失败: " + response.code()));
                    return;
                }
                
                try {
                    String respXml = response.body().string();
                    Map<String, String> resp = xmlToMap(respXml);
                    
                    String returnCode = resp.get("return_code");
                    String resultCode = resp.get("result_code");
                    
                    if ("SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode)) {
                        String prepayId = resp.get("prepay_id");
                        // 构建客户端调起参数
                        Map<String, String> payParams = new HashMap<>();
                        payParams.put("appid", WXPayConfig.APP_ID);
                        payParams.put("partnerid", WXPayConfig.MCH_ID);
                        payParams.put("prepayid", prepayId);
                        payParams.put("package", "Sign=WXPay");
                        payParams.put("noncestr", WXPaySign.nonceStr());
                        payParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                        payParams.put("sign", WXPaySign.generateSign(payParams, WXPayConfig.API_KEY));
                        
                        callback.onSuccess(payParams);
                    } else {
                        String errMsg = resp.getOrDefault("err_code_des", resp.getOrDefault("return_msg", "未知错误"));
                        callback.onError(new Exception("统一下单失败: " + errMsg));
                    }
                } catch (Exception e) {
                    callback.onError(new Exception("解析响应失败: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Map 转 XML
     */
    private String mapToXml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("<").append(entry.getKey()).append("><![CDATA[");
            sb.append(entry.getValue());
            sb.append("]]></").append(entry.getKey()).append(">");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    /**
     * XML 转 Map
     */
    private Map<String, String> xmlToMap(String xml) throws Exception {
        Map<String, String> map = new HashMap<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        
        String currentKey = null;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                currentKey = parser.getName();
            } else if (eventType == XmlPullParser.TEXT) {
                if (currentKey != null && !"xml".equals(currentKey)) {
                    map.put(currentKey, parser.getText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                currentKey = null;
            }
            eventType = parser.next();
        }
        return map;
    }

    /**
     * 根据 VIP 等级和价格创建订单描述和金额
     */
    public static PayOrder createVipOrder(int vipLevel) {
        String body;
        int totalFee; // 单位：分
        String outTradeNo = "VIP" + vipLevel + "_" + System.currentTimeMillis();
        
        switch (vipLevel) {
            case VIPLevel.MONTHLY:
                body = "SuperAI月会员";
                totalFee = 2990; // 29.90元
                break;
            case VIPLevel.YEARLY:
                body = "SuperAI年会员";
                totalFee = 26900; // 269.00元
                break;
            case VIPLevel.LIFETIME:
                body = "SuperAI终身会员";
                totalFee = 99900; // 999.00元
                break;
            default:
                body = "SuperAI会员";
                totalFee = 0;
                break;
        }
        
        return new PayOrder(body, outTradeNo, totalFee);
    }

    public interface UnifiedOrderCallback {
        void onSuccess(Map<String, String> payParams);
        void onError(Exception e);
    }
}
