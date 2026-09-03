package com.superai.workbench.ui.pay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.superai.workbench.R;
import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.VIPLevel;
import com.superai.workbench.data.payment.WXPayConfig;
import com.superai.workbench.data.payment.WxPayClient;
import com.superai.workbench.data.payment.WxPayLaunchParams;
import com.superai.workbench.data.payment.WxPayResult;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

/**
 * 支付中心 Fragment
 * 4档会员体系：免费版 / 月会员 / 年会员 / 终身会员
 * 接入微信支付 SDK（使用 WxPayClient 统一下单）
 */
public class PayFragment extends Fragment {

    private ConfigManager configManager;
    private IWXAPI wxApi;
    private WxPayClient wxPayClient;

    private TextView tvCurrentLevel;
    private TextView tvExpireInfo;
    private LinearLayout cardsContainer;
    private ProgressBar progressBar;

    private int pendingVipLevel = -1;

    // 广播接收器 - 接收微信支付结果
    private final BroadcastReceiver payResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            int errCode = intent.getIntExtra("errCode", -999);
            handlePayResult(result, errCode);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);
        configManager = ConfigManager.getInstance(requireContext());

        // 初始化微信支付
        wxApi = WXAPIFactory.createWXAPI(requireContext(), WXPayConfig.APP_ID, true);
        wxApi.registerApp(WXPayConfig.APP_ID);

        // 初始化支付客户端（替换为你的服务器地址）
        // 开发测试：http://192.168.1.100:8080
        // 生产环境：https://your-domain.com
        String serverUrl = WXPayConfig.SANDBOX
            ? "http://10.0.2.2:8080"  // Android 模拟器访问本机 localhost
            : "https://your-domain.com";
        wxPayClient = new WxPayClient(requireContext(), serverUrl);

        tvCurrentLevel = view.findViewById(R.id.tv_current_level);
        tvExpireInfo = view.findViewById(R.id.tv_expire_info);
        cardsContainer = view.findViewById(R.id.cards_container);
        progressBar = view.findViewById(R.id.progress_bar);

        updateCurrentStatus();
        buildVipCards();

        IntentFilter filter = new IntentFilter("com.superai.workbench.WX_PAY_RESULT");
        requireContext().registerReceiver(payResultReceiver, filter,
            requireContext().getPackageName() + ".permission.WX_PAY", null);

        return view;
    }

    private void updateCurrentStatus() {
        int level = configManager.getVipLevel();
        VIPLevel vip = VIPLevel.getAllLevels()[level];
        tvCurrentLevel.setText(VIPLevel.getLevelBadge(level) + " 当前等级：" + vip.getName());

        if (level == VIPLevel.FREE) {
            tvExpireInfo.setText("升级会员解锁更多模型与功能");
        } else if (level == VIPLevel.LIFETIME) {
            tvExpireInfo.setText("终身会员 - 永久有效");
        } else {
            int days = configManager.getVipRemainingDays();
            tvExpireInfo.setText("剩余 " + days + " 天");
        }
    }

    private void buildVipCards() {
        VIPLevel[] levels = VIPLevel.getAllLevels();
        int currentLevel = configManager.getVipLevel();

        for (int i = 0; i < levels.length; i++) {
            VIPLevel vip = levels[i];
            if (vip.getLevel() == VIPLevel.FREE) continue;

            CardView card = new CardView(requireContext());
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            cardContent.setPadding(pad, pad, pad, pad);

            // 标题
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(VIPLevel.getLevelBadge(vip.getLevel()) + " " + vip.getName());
            tvTitle.setTextSize(18f);
            tvTitle.setTextColor(0xFFE6EDF3);
            tvTitle.setPadding(0, 0, 0, pad / 2);
            cardContent.addView(tvTitle);

            // 价格
            TextView tvPrice = new TextView(requireContext());
            String priceText = vip.getPrice() > 0
                ? "￥" + String.format("%.1f", vip.getPrice()) + " / " + vip.getPeriod()
                : "免费";
            tvPrice.setText(priceText);
            tvPrice.setTextSize(22f);
            tvPrice.setTextColor(vip.getLevel() == VIPLevel.YEARLY ? 0xFFFFD700 : 0xFF58A6FF);
            tvPrice.setPadding(0, 0, 0, pad / 2);
            cardContent.addView(tvPrice);

            // 描述
            TextView tvDesc = new TextView(requireContext());
            tvDesc.setText(vip.getDesc());
            tvDesc.setTextSize(13f);
            tvDesc.setTextColor(0xFF8B949E);
            tvDesc.setPadding(0, 0, 0, pad / 2);
            cardContent.addView(tvDesc);

            // 权益
            for (String feature : vip.getFeatures()) {
                TextView tvFeature = new TextView(requireContext());
                tvFeature.setText("✓ " + feature);
                tvFeature.setTextSize(13f);
                tvFeature.setTextColor(0xFFE6EDF3);
                tvFeature.setPadding(pad / 4, 0, 0, pad / 6);
                cardContent.addView(tvFeature);
            }

            // 购买按钮
            Button btnBuy = new Button(requireContext());
            if (vip.getLevel() <= currentLevel) {
                btnBuy.setText("已开通");
                btnBuy.setEnabled(false);
            } else {
                btnBuy.setText("立即开通");
                final int targetLevel = vip.getLevel();
                btnBuy.setOnClickListener(v -> startPurchase(targetLevel));
            }
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = pad;
            btnBuy.setLayoutParams(btnParams);
            cardContent.addView(btnBuy);

            // 卡片样式
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            int cardMargin = (int) (8 * getResources().getDisplayMetrics().density);
            cardParams.bottomMargin = cardMargin;
            card.setLayoutParams(cardParams);
            card.setRadius(16 * getResources().getDisplayMetrics().density);
            card.setCardBackgroundColor(0xFF161B22);
            card.setContentPadding(pad, pad, pad, pad);
            card.addView(cardContent);

            cardsContainer.addView(card);
        }
    }

    /**
     * 开始购买流程
     */
    private void startPurchase(int targetLevel) {
        if (!wxApi.isWXAppInstalled()) {
            Toast.makeText(requireContext(), "请先安装微信", Toast.LENGTH_LONG).show();
            return;
        }

        pendingVipLevel = targetLevel;
        progressBar.setVisibility(View.VISIBLE);

        // 获取设备标识（用于服务器订单追踪）
        String deviceId = Settings.Secure.getString(
            requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // 调用 WxPayClient 统一下单
        wxPayClient.createOrder(targetLevel, deviceId, new WxPayClient.WxPayCallback() {
            @Override
            public void onOrderCreated(WxPayLaunchParams params) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // 使用 WxPayClient 构建 PayReq 并调起微信支付
                    PayReq req = WxPayClient.buildPayReq(params);
                    boolean sent = wxApi.sendReq(req);
                    if (!sent) {
                        Toast.makeText(requireContext(), "调起微信支付失败", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onPayResult(WxPayResult result) {
                // 支付结果由广播接收器 WXPayEntryActivity -> Broadcast 处理
            }

            @Override
            public void onError(WxPayResult error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    // 沙箱模式：模拟支付成功（开发测试用）
                    if (WXPayConfig.SANDBOX) {
                        simulatePaySuccess(targetLevel);
                    }
                });
            }
        });
    }

    /**
     * 处理支付结果（来自 WXPayEntryActivity 广播）
     */
    private void handlePayResult(String result, int errCode) {
        progressBar.setVisibility(View.GONE);

        switch (result) {
            case "PAY_SUCCESS":
                if (pendingVipLevel >= 0) {
                    configManager.purchaseVip(pendingVipLevel);
                    Toast.makeText(requireContext(),
                        "支付成功！您现在是 " + VIPLevel.getLevelName(pendingVipLevel),
                        Toast.LENGTH_LONG).show();
                    refreshUI();
                }
                break;
            case "PAY_CANCEL":
                Toast.makeText(requireContext(), "支付已取消", Toast.LENGTH_SHORT).show();
                break;
            case "PAY_FAIL":
                Toast.makeText(requireContext(), "支付失败: " + errCode, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(requireContext(), "支付异常: " + result, Toast.LENGTH_LONG).show();
                break;
        }

        pendingVipLevel = -1;
    }

    /**
     * 沙箱/开发测试：模拟支付成功
     */
    private void simulatePaySuccess(int targetLevel) {
        configManager.purchaseVip(targetLevel);
        Toast.makeText(requireContext(),
            "[测试模式] 模拟支付成功！您现在是 " + VIPLevel.getLevelName(targetLevel),
            Toast.LENGTH_LONG).show();
        refreshUI();
    }

    private void refreshUI() {
        cardsContainer.removeAllViews();
        updateCurrentStatus();
        buildVipCards();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cardsContainer != null) {
            refreshUI();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            requireContext().unregisterReceiver(payResultReceiver);
        } catch (Exception e) {
            // 忽略未注册异常
        }
    }
}
