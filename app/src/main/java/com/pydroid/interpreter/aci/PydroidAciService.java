package com.pydroid.interpreter.aci;

import android.os.Bundle;
import android.util.Log;

import ai.aidl.aci.core.AidlAciError;
import ai.aidl.aci.core.AidlAciRequest;
import ai.aidl.aci.core.AidlAciResponse;
import ai.aidl.aci.core.BaseAidlAciService;
import ai.aidl.aci.core.Capability;

import com.pydroid.interpreter.engine.PythonEngine;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pydroid3 ACI 受控端服务
 * 向 ZorvAI 等控制端暴露 Python 执行能力
 */
public class PydroidAciService extends BaseAidlAciService {
    private static final String TAG = "PydroidAciService";
    
    // ZorvAI 主程序包名（控制端）
    private static final String ZORVAI_PACKAGE = "com.ai.assistance.quro";

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            Log.i(TAG, "Pydroid3 ACI 服务已启动");
        } catch (Throwable e) {
            Log.e(TAG, "ACI 服务启动失败", e);
        }
    }

    @Override
    protected void onCreateCapabilities(List<Capability> capabilities) {
        Log.i(TAG, "注册 Pydroid3 ACI 能力...");

        // 能力 1: 执行 Python 代码
        capabilities.add(
            Capability.create("execute_python", "执行 Python 代码并返回输出结果")
                .addParam("code", "string", true, "要执行的 Python 代码")
                .addResult("output", "string", "执行输出结果")
                .addResult("error", "string", "错误信息（如果有）")
                .addResult("exit_code", "int", "退出码，0 表示成功")
                .addFlag(Capability.FLAG_BACKGROUND)
                .addFlag(Capability.FLAG_NO_UI)
        );

        // 能力 2: 执行 Python 文件
        capabilities.add(
            Capability.create("execute_file", "执行指定路径的 Python 文件")
                .addParam("file_path", "string", true, "Python 文件的绝对路径")
                .addResult("output", "string", "执行输出结果")
                .addResult("error", "string", "错误信息（如果有）")
                .addResult("exit_code", "int", "退出码")
                .addFlag(Capability.FLAG_BACKGROUND)
                .addFlag(Capability.FLAG_NO_UI)
        );

        // 能力 3: 获取 Python 版本
        capabilities.add(
            Capability.create("get_python_version", "获取当前 Python 解释器版本信息")
                .addResult("version", "string", "Python 版本字符串")
                .addFlag(Capability.FLAG_NO_UI)
        );

        // 能力 4: 获取解释器状态
        capabilities.add(
            Capability.create("get_engine_status", "获取 Python 引擎状态信息")
                .addResult("initialized", "boolean", "是否已初始化")
                .addResult("running", "boolean", "是否正在执行代码")
                .addResult("version", "string", "Python 版本")
                .addFlag(Capability.FLAG_NO_UI)
        );

        // 能力 5: 交互式执行（REPL 模式）
        capabilities.add(
            Capability.create("eval_python", "交互式执行 Python 表达式并返回结果")
                .addParam("expression", "string", true, "要执行的 Python 表达式")
                .addResult("result", "string", "表达式计算结果")
                .addResult("error", "string", "错误信息（如果有）")
                .addFlag(Capability.FLAG_BACKGROUND)
                .addFlag(Capability.FLAG_NO_UI)
        );

        Log.i(TAG, "已注册 " + capabilities.size() + " 个 ACI 能力");
    }

    @Override
    protected boolean onCheckPermission(AidlAciRequest request, String callerPkg) {
        // 仅允许 ZorvAI 主程序和自身调用
        boolean allowed = ZORVAI_PACKAGE.equals(callerPkg) || 
                         getPackageName().equals(callerPkg);
        
        if (!allowed) {
            Log.w(TAG, "拒绝未授权的调用: " + callerPkg);
        }
        
        return allowed;
    }

    @Override
    protected AidlAciResponse onCall(AidlAciRequest request) {
        String capability = request.getCapability();
        Bundle params = request.getParams();
        
        Log.i(TAG, "收到 ACI 调用: " + capability);

        try {
            switch (capability) {
                case "execute_python":
                    return handleExecutePython(params);
                case "execute_file":
                    return handleExecuteFile(params);
                case "get_python_version":
                    return handleGetVersion();
                case "get_engine_status":
                    return handleGetStatus();
                case "eval_python":
                    return handleEvalPython(params);
                default:
                    return AidlAciResponse.error(
                        AidlAciError.CAPABILITY_NOT_FOUND,
                        "未知能力: " + capability
                    );
            }
        } catch (Exception e) {
            Log.e(TAG, "处理 ACI 调用失败: " + capability, e);
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "执行失败: " + e.getMessage()
            );
        }
    }

    /**
     * 处理执行 Python 代码请求
     */
    private AidlAciResponse handleExecutePython(Bundle params) {
        if (params == null || !params.containsKey("code")) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                "缺少必需参数: code"
            );
        }

        String code = params.getString("code");
        PythonEngine engine = PythonEngine.getInstance();

        if (!engine.isInitialized()) {
            return AidlAciResponse.error(
                AidlAciError.SERVICE_UNAVAILABLE,
                "Python 引擎未初始化"
            );
        }

        // 同步执行代码
        ExecutionResult result = executeCodeSynchronously(code);

        AidlAciResponse response = AidlAciResponse.success();
        response.putResult("output", result.output);
        response.putResult("error", result.error);
        response.putResult("exit_code", result.exitCode);
        
        return response;
    }

    /**
     * 处理执行 Python 文件请求
     */
    private AidlAciResponse handleExecuteFile(Bundle params) {
        if (params == null || !params.containsKey("file_path")) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                "缺少必需参数: file_path"
            );
        }

        String filePath = params.getString("file_path");
        PythonEngine engine = PythonEngine.getInstance();

        if (!engine.isInitialized()) {
            return AidlAciResponse.error(
                AidlAciError.SERVICE_UNAVAILABLE,
                "Python 引擎未初始化"
            );
        }

        // 读取文件内容并执行
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return AidlAciResponse.error(
                    AidlAciError.BAD_REQUEST,
                    "文件不存在: " + filePath
                );
            }

            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            ExecutionResult result = executeCodeSynchronously(content.toString());

            AidlAciResponse response = AidlAciResponse.success();
            response.putResult("output", result.output);
            response.putResult("error", result.error);
            response.putResult("exit_code", result.exitCode);
            
            return response;

        } catch (java.io.IOException e) {
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "读取文件失败: " + e.getMessage()
            );
        }
    }

    /**
     * 处理获取 Python 版本请求
     */
    private AidlAciResponse handleGetVersion() {
        PythonEngine engine = PythonEngine.getInstance();
        String version = engine.getVersion();

        AidlAciResponse response = AidlAciResponse.success();
        response.putResult("version", version);
        
        return response;
    }

    /**
     * 处理获取引擎状态请求
     */
    private AidlAciResponse handleGetStatus() {
        PythonEngine engine = PythonEngine.getInstance();

        AidlAciResponse response = AidlAciResponse.success();
        response.putResult("initialized", engine.isInitialized());
        response.putResult("running", engine.isRunning());
        response.putResult("version", engine.getVersion());
        
        return response;
    }

    /**
     * 处理交互式执行请求
     */
    private AidlAciResponse handleEvalPython(Bundle params) {
        if (params == null || !params.containsKey("expression")) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                "缺少必需参数: expression"
            );
        }

        String expression = params.getString("expression");
        PythonEngine engine = PythonEngine.getInstance();

        if (!engine.isInitialized()) {
            return AidlAciResponse.error(
                AidlAciError.SERVICE_UNAVAILABLE,
                "Python 引擎未初始化"
            );
        }

        // 使用 CountDownLatch 同步等待结果
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        engine.interactiveExecute(expression, new PythonEngine.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                resultRef.set(output);
            }

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }

            @Override
            public void onExecutionComplete(int exitCode) {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                return AidlAciResponse.error(
                    AidlAciError.TIMEOUT,
                    "执行超时"
                );
            }

            AidlAciResponse response = AidlAciResponse.success();
            response.putResult("result", resultRef.get() != null ? resultRef.get() : "");
            response.putResult("error", errorRef.get() != null ? errorRef.get() : "");
            
            return response;

        } catch (InterruptedException e) {
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "执行被中断"
            );
        }
    }

    /**
     * 同步执行 Python 代码（阻塞等待结果）
     */
    private ExecutionResult executeCodeSynchronously(String code) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutionResult result = new ExecutionResult();

        PythonEngine engine = PythonEngine.getInstance();
        engine.setCallback(new PythonEngine.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                result.output = output != null ? output : "";
            }

            @Override
            public void onError(String error) {
                result.error = error != null ? error : "";
            }

            @Override
            public void onExecutionComplete(int exitCode) {
                result.exitCode = exitCode;
                latch.countDown();
            }
        });

        engine.executeCode(code);

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                result.error = "执行超时（30秒）";
                result.exitCode = -1;
            }
        } catch (InterruptedException e) {
            result.error = "执行被中断";
            result.exitCode = -1;
        }

        return result;
    }

    /**
     * 执行结果封装类
     */
    private static class ExecutionResult {
        String output = "";
        String error = "";
        int exitCode = 0;
    }
}
