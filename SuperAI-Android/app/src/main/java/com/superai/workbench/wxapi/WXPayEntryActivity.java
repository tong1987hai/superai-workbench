package com.superai.workbench.ui.pay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.superai.workbench.data.payment.WXPayConfig;

/**
 * 微信支付回调 Activity
 * 微信 SDK 在支付完成后会自动跳转到此 Activity
 * 
 * 注意：必须放在项目包名下（wxapi 目录）
 * 例如 com.superai.workbench.wxapi.WXPayEntryActivity
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, WXPayConfig.APP_ID);
        api.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // 微信发起的请求，通常不需要处理
    }

    @Override
    public void onResp(BaseResp resp) {
        // 支付结果回调
        int errCode = resp.errCode;
        String result;
        switch (errCode) {
            case 0:
                result = "PAY_SUCCESS";
                break;
            case -1:
                result = "PAY_FAIL";
                break;
            case -2:
                result = "PAY_CANCEL";
                break;
            default:
                result = "PAY_ERROR_" + errCode;
                break;
        }
        
        // 发送广播通知 PayFragment 处理支付结果
        Intent broadcastIntent = new Intent("com.superai.workbench.WX_PAY_RESULT");
        broadcastIntent.putExtra("result", result);
        broadcastIntent.putExtra("errCode", errCode);
        sendBroadcast(broadcastIntent);

        finish();
    }
}
