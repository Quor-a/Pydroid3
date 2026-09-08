package com.pydroid.interpreter.aci;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ACI 唤醒接收器 - 用于唤醒处于 stopped 状态的应用
 * 当 ZorvAI 等控制端需要调用 Pydroid3 时，会先发送 ACTION_WAKE 广播拉起进程
 */
public class PydroidAciWakeReceiver extends BroadcastReceiver {
    private static final String TAG = "PydroidAciWakeReceiver";
    private static final String ACTION_WAKE = "ai.aci.core.ACTION_WAKE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_WAKE.equals(intent.getAction())) {
            Log.i(TAG, "收到 ACI 唤醒广播，拉起应用");
            try {
                // 启动主 Activity 使应用脱离 stopped 状态
                Intent launchIntent = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName());
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "唤醒应用失败", e);
            }
        }
    }
}
