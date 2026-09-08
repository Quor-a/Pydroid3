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
                // 使用 Chaquoco 初始化 Python
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
                // 使用 Chaquoco 执行代码
                // 创建输出捕获
                String wrappedCode = 
                    "import sys\n" +
                    "from io import StringIO\n" +
                    "_old_stdout = sys.stdout\n" +
                    "_old_stderr = sys.stderr\n" +
                    "sys.stdout = StringIO()\n" +
                    "sys.stderr = StringIO()\n" +
                    "try:\n" +
                    "    " + code.replace("\n", "\n    ") + "\n" +
                    "    _output = sys.stdout.getvalue()\n" +
                    "    _error = sys.stderr.getvalue()\n" +
                    "except Exception as e:\n" +
                    "    _output = ''\n" +
                    "    _error = str(e)\n" +
                    "finally:\n" +
                    "    sys.stdout = _old_stdout\n" +
                    "    sys.stderr = _old_stderr\n" +
                    "_result = _output + _error\n";
                
                PyObject module = python.getModule("__main__");
                module.callAttr("exec", wrappedCode);
                
                String output = "Code executed";
                outputHistory.add(output);
                
                if (callback != null) {
                    callback.onOutput(output);
                    callback.onExecutionComplete(0);
                }
            } catch (Exception e) {
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
                // 读取文件
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                
                // 执行代码
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
                // 执行单行代码
                PyObject result = python.getModule("__main__").callAttr("eval", code);
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
