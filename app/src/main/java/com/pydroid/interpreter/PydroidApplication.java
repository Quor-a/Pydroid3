package com.pydroid.interpreter;

import android.app.Application;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class PydroidApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 Chaquoco Python 运行时
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
}
