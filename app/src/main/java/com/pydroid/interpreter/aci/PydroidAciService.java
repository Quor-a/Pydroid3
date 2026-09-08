package com.pydroid.interpreter.aci;

import android.os.Bundle;
import android.util.Log;

import ai.aidl.aci.core.AciIntentBridge;
import ai.aidl.aci.core.AidlAciAudit;
import ai.aidl.aci.core.AidlAciError;
import ai.aidl.aci.core.AidlAciEvents;
import ai.aidl.aci.core.AidlAciProtocol;
import ai.aidl.aci.core.AidlAciRegistry;
import ai.aidl.aci.core.AidlAciRequest;
import ai.aidl.aci.core.AidlAciResponse;
import ai.aidl.aci.core.BaseAidlAciService;
import ai.aidl.aci.core.Capability;

import com.pydroid.interpreter.engine.PythonEngine;
import com.pydroid.interpreter.mcp.McpBridge;

import org.json.JSONArray;
import org.json.JSONObject;

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
    
    private AidlAciAudit audit;

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            audit = new AidlAciAudit(this);
            Log.i(TAG, "Pydroid3 ACI 服务已启动");
            
            // 发布服务绑定事件
            AidlAciEvents.emit(AidlAciEvents.EVENT_SERVICE_BOUND, getPackageName());
        } catch (Throwable e) {
            Log.e(TAG, "ACI 服务启动失败", e);
        }
    }

    @Override
    protected void onCreateCapabilities(List<Capability> capabilities) {
        Log.i(TAG, "注册 Pydroid3 ACI 能力...");

        // 能力 1: 执行 Python 代码
        Capability executePython = Capability.create("execute_python", "执行 Python 代码并返回输出结果")
            .addParam("code", "string", true, "要执行的 Python 代码")
            .addResult("output", "string", "执行输出结果")
            .addResult("error", "string", "错误信息（如果有）")
            .addResult("exit_code", "int", "退出码，0 表示成功")
            .addFlag(Capability.FLAG_BACKGROUND)
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(executePython);
        AidlAciRegistry.register(getPackageName(), executePython);

        // 能力 2: 执行 Python 文件
        Capability executeFile = Capability.create("execute_file", "执行指定路径的 Python 文件")
            .addParam("file_path", "string", true, "Python 文件的绝对路径")
            .addResult("output", "string", "执行输出结果")
            .addResult("error", "string", "错误信息（如果有）")
            .addResult("exit_code", "int", "退出码")
            .addFlag(Capability.FLAG_BACKGROUND)
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(executeFile);
        AidlAciRegistry.register(getPackageName(), executeFile);

        // 能力 3: 获取 Python 版本
        Capability getVersion = Capability.create("get_python_version", "获取当前 Python 解释器版本信息")
            .addResult("version", "string", "Python 版本字符串")
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(getVersion);
        AidlAciRegistry.register(getPackageName(), getVersion);

        // 能力 4: 获取解释器状态
        Capability getStatus = Capability.create("get_engine_status", "获取 Python 引擎状态信息")
            .addResult("initialized", "boolean", "是否已初始化")
            .addResult("running", "boolean", "是否正在执行代码")
            .addResult("version", "string", "Python 版本")
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(getStatus);
        AidlAciRegistry.register(getPackageName(), getStatus);

        // 能力 5: 交互式执行（REPL 模式）
        Capability evalPython = Capability.create("eval_python", "交互式执行 Python 表达式并返回结果")
            .addParam("expression", "string", true, "要执行的 Python 表达式")
            .addResult("result", "string", "表达式计算结果")
            .addResult("error", "string", "错误信息（如果有）")
            .addFlag(Capability.FLAG_BACKGROUND)
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(evalPython);
        AidlAciRegistry.register(getPackageName(), evalPython);

        // 能力 6: 协议协商（ACI 标准能力）
        Capability protocol = Capability.create("aci_protocol", "返回 ACI 协议版本信息")
            .addResult("protocol_version", "string", "当前协议版本")
            .addResult("semver", "string", "语义化版本号")
            .addResult("supported", "string", "支持的协议列表（JSON 数组）")
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(protocol);
        AidlAciRegistry.register(getPackageName(), protocol);

        // 能力 7: Intent 代理（ACI 标准能力）
        capabilities.add(AciIntentBridge.capability());

        // 能力 8: MCP-ACI 桥接 - 列出 MCP 工具
        Capability mcpList = Capability.create("mcp_aci_list", "列出所有可通过 ACI 调用的 MCP 工具")
            .addResult("tools", "string", "MCP 工具列表（JSON 数组）")
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(mcpList);
        AidlAciRegistry.register(getPackageName(), mcpList);

        // 能力 9: MCP-ACI 桥接 - 调用 MCP 工具
        Capability mcpCall = Capability.create("mcp_aci_call", "通过 ACI 调用 MCP 工具")
            .addParam("capability", "string", true, "MCP 能力 ID（格式：mcp_{toolName}）")
            .addParam("args", "string", false, "工具参数（JSON 对象）")
            .addResult("result", "string", "调用结果（JSON 对象）")
            .addResult("error", "string", "错误信息（如果有）")
            .addFlag(Capability.FLAG_BACKGROUND)
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(mcpCall);
        AidlAciRegistry.register(getPackageName(), mcpCall);

        // 能力 10: MCP-ACI 桥接 - 管理桥接器
        Capability mcpBridge = Capability.create("mcp_aci_bridge", "管理 MCP-ACI 桥接器")
            .addParam("action", "string", true, "操作：add_server/remove_server/refresh/status")
            .addParam("name", "string", false, "服务器名称（add/remove 时必需）")
            .addParam("url", "string", false, "服务器 URL（add 时必需）")
            .addParam("timeout", "int", false, "超时时间（毫秒，默认 5000）")
            .addResult("success", "boolean", "操作是否成功")
            .addResult("status", "string", "桥接器状态（JSON 对象）")
            .addResult("error", "string", "错误信息（如果有）")
            .addFlag(Capability.FLAG_BACKGROUND)
            .addFlag(Capability.FLAG_NO_UI);
        capabilities.add(mcpBridge);
        AidlAciRegistry.register(getPackageName(), mcpBridge);

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
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int errorCode = 0;

        try {
            AidlAciResponse response;
            
            switch (capability) {
                case "execute_python":
                    response = handleExecutePython(params);
                    break;
                case "execute_file":
                    response = handleExecuteFile(params);
                    break;
                case "get_python_version":
                    response = handleGetVersion();
                    break;
                case "get_engine_status":
                    response = handleGetStatus();
                    break;
                case "eval_python":
                    response = handleEvalPython(params);
                    break;
                case "aci_protocol":
                    response = handleProtocol();
                    break;
                case "intent":
                    response = AciIntentBridge.handle(this, params);
                    break;
                case "mcp_aci_list":
                    response = handleMcpList();
                    break;
                case "mcp_aci_call":
                    response = handleMcpCall(params);
                    break;
                case "mcp_aci_bridge":
                    response = handleMcpBridge(params);
                    break;
                default:
                    response = AidlAciResponse.error(
                        AidlAciError.CAPABILITY_NOT_FOUND,
                        "未知能力: " + capability
                    );
            }
            
            success = response.isSuccess();
            errorCode = response.getErrorCode();
            
            return response;
        } catch (Exception e) {
            Log.e(TAG, "处理 ACI 调用失败: " + capability, e);
            
            // 发布调用失败事件
            AidlAciEvents.emit(AidlAciEvents.EVENT_CALL_FAILED, capability);
            
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "执行失败: " + e.getMessage()
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录审计日志
            if (audit != null) {
                audit.log(getPackageName(), capability, success, errorCode, duration);
            }
        }
    }
    
    /**
     * 处理协议协商请求
     */
    private AidlAciResponse handleProtocol() {
        AidlAciResponse response = AidlAciResponse.success();
        response.putResult("protocol_version", AidlAciProtocol.PROTOCOL_VERSION);
        response.putResult("semver", AidlAciProtocol.PROTOCOL_SEMVER);
        
        try {
            org.json.JSONArray supported = new org.json.JSONArray();
            for (String p : AidlAciProtocol.SUPPORTED) {
                supported.put(p);
            }
            response.putResult("supported", supported.toString());
        } catch (Throwable e) {
            response.putResult("supported", "[]");
        }
        
        return response;
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
     * 处理 MCP-ACI 桥接 - 列出所有 MCP 工具
     */
    private AidlAciResponse handleMcpList() {
        try {
            JSONArray tools = McpBridge.getInstance().getMcpCapabilities();
            
            AidlAciResponse response = AidlAciResponse.success();
            response.putResult("tools", tools.toString());
            
            return response;
        } catch (Exception e) {
            Log.e(TAG, "列出 MCP 工具失败", e);
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "列出 MCP 工具失败: " + e.getMessage()
            );
        }
    }

    /**
     * 处理 MCP-ACI 桥接 - 调用 MCP 工具
     */
    private AidlAciResponse handleMcpCall(Bundle params) {
        if (params == null || !params.containsKey("capability")) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                "缺少必需参数: capability"
            );
        }

        String capabilityId = params.getString("capability");
        String argsJson = params.getString("args");
        
        try {
            JSONObject args = new JSONObject();
            if (argsJson != null && !argsJson.isEmpty()) {
                args = new JSONObject(argsJson);
            }
            
            JSONObject result = McpBridge.getInstance().callMcpTool(capabilityId, args);
            
            AidlAciResponse response = AidlAciResponse.success();
            response.putResult("result", result.toString());
            
            return response;
        } catch (IllegalArgumentException e) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                e.getMessage()
            );
        } catch (Exception e) {
            Log.e(TAG, "调用 MCP 工具失败: " + capabilityId, e);
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "调用 MCP 工具失败: " + e.getMessage()
            );
        }
    }

    /**
     * 处理 MCP-ACI 桥接 - 管理桥接器
     */
    private AidlAciResponse handleMcpBridge(Bundle params) {
        if (params == null || !params.containsKey("action")) {
            return AidlAciResponse.error(
                AidlAciError.BAD_REQUEST,
                "缺少必需参数: action"
            );
        }

        String action = params.getString("action");
        
        try {
            AidlAciResponse response = AidlAciResponse.success();
            
            switch (action) {
                case "add_server": {
                    String name = params.getString("name");
                    String url = params.getString("url");
                    int timeout = params.containsKey("timeout") ? 
                        params.getInt("timeout") : 5000;
                    
                    if (name == null || url == null) {
                        return AidlAciResponse.error(
                            AidlAciError.BAD_REQUEST,
                            "add_server 需要 name 和 url 参数"
                        );
                    }
                    
                    McpBridge.getInstance().addServer(name, url, timeout);
                    response.putResult("success", true);
                    break;
                }
                
                case "remove_server": {
                    String name = params.getString("name");
                    if (name == null) {
                        return AidlAciResponse.error(
                            AidlAciError.BAD_REQUEST,
                            "remove_server 需要 name 参数"
                        );
                    }
                    
                    McpBridge.getInstance().removeServer(name);
                    response.putResult("success", true);
                    break;
                }
                
                case "refresh": {
                    String name = params.getString("name");
                    if (name != null) {
                        McpBridge.getInstance().refreshTools(name);
                    } else {
                        McpBridge.getInstance().refreshAllTools();
                    }
                    response.putResult("success", true);
                    break;
                }
                
                case "status": {
                    JSONObject status = McpBridge.getInstance().getStatus();
                    response.putResult("status", status.toString());
                    response.putResult("success", true);
                    break;
                }
                
                default:
                    return AidlAciResponse.error(
                        AidlAciError.BAD_REQUEST,
                        "未知操作: " + action
                    );
            }
            
            return response;
        } catch (Exception e) {
            Log.e(TAG, "MCP 桥接操作失败: " + action, e);
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "操作失败: " + e.getMessage()
            );
        }
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
