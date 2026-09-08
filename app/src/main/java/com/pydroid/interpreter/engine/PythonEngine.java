package com.pydroid.interpreter.engine;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Python 解释器引擎 - 使用 Chaquoco
 */
public class PythonEngine {
    private static final String TAG = "PythonEngine";
    
    private static PythonEngine instance;
    private boolean initialized = false;
    private Python python;
    private HandlerThread workerThread;
    private Handler workerHandler;
    private ExecutionCallback callback;
    private final List<String> outputHistory = new ArrayList<>();
    private boolean isRunning = false;
    
    public interface ExecutionCallback {
        void onOutput(String output);
        void onError(String error);
        void onExecutionComplete(int exitCode);
    }
    
    private PythonEngine() {
        workerThread = new HandlerThread("PythonWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
    }
    
    public static synchronized PythonEngine getInstance() {
        if (instance == null) {
            instance = new PythonEngine();
        }
        return instance;
    }
    
    /**
     * 初始化 Python 解释器
     */
    public void initialize(Context context, ExecutionCallback cb) {
        this.callback = cb;
        
        if (initialized) {
            if (cb != null) cb.onExecutionComplete(0);
            return;
        }
        
        workerHandler.post(() -> {
            try {
                // Python 已在 PydroidApplication 中通过 Python.start() 初始化
                python = Python.getInstance();
                initialized = true;
                Log.i(TAG, "Python initialized via Chaquoco");
                
                if (cb != null) cb.onExecutionComplete(0);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Python", e);
                if (cb != null) cb.onError(e.getMessage());
            }
        });
    }
    
    /**
     * 执行 Python 代码
     */
    public void executeCode(String code) {
        if (!initialized) {
            if (callback != null) callback.onError("Python not initialized");
            return;
        }
        
        isRunning = true;
        workerHandler.post(() -> {
            try {
                // 使用 exec_helper 模块执行代码（解决 frame does not exist 问题）
                PyObject helper = python.getModule("exec_helper");
                PyObject result = helper.callAttr("run_code", code);
                
                String output = result != null ? result.toString() : "执行完成";
                outputHistory.add(output);
                
                if (callback != null) {
                    callback.onOutput(output);
                    callback.onExecutionComplete(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Execution error", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                    callback.onExecutionComplete(1);
                }
            } finally {
                isRunning = false;
            }
        });
    }
    
    /**
     * 执行 Python 文件
     */
    public void executeFile(String filePath) {
        if (!initialized) {
            if (callback != null) callback.onError("Python not initialized");
            return;
        }
        
        isRunning = true;
        workerHandler.post(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                
                executeCode(sb.toString());
                
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError("Cannot read file: " + e.getMessage());
                    callback.onExecutionComplete(1);
                }
            } finally {
                isRunning = false;
            }
        });
    }
    
    /**
     * 交互式执行（REPL）
     */
    public void interactiveExecute(String code, ExecutionCallback cb) {
        if (!initialized) {
            if (cb != null) cb.onError("Python not initialized");
            return;
        }
        
        workerHandler.post(() -> {
            try {
                PyObject helper = python.getModule("exec_helper");
                PyObject result = helper.callAttr("eval_code", code);
                String output = result != null ? result.toString() : "";
                
                if (cb != null) {
                    cb.onOutput(output);
                    cb.onExecutionComplete(0);
                }
            } catch (Exception e) {
                if (cb != null) {
                    cb.onError(e.getMessage());
                    cb.onExecutionComplete(1);
                }
            }
        });
    }
    
    /**
     * 获取 Python 版本
     */
    public String getVersion() {
        if (!initialized) return "Python (not initialized)";
        try {
            PyObject sys = python.getModule("sys");
            return "Python " + sys.get("version").toString();
        } catch (Exception e) {
            return "Python 3.11 (Chaquoco)";
        }
    }
    
    /**
     * 关闭解释器
     */
    public void shutdown() {
        workerHandler.post(() -> {
            initialized = false;
            workerThread.quitSafely();
        });
    }
    
    public boolean isInitialized() { return initialized; }
    public boolean isRunning() { return isRunning; }
    public List<String> getOutputHistory() { return new ArrayList<>(outputHistory); }
    public void setCallback(ExecutionCallback cb) { this.callback = cb; }
}
