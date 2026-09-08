package com.pydroid.interpreter.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pydroid.interpreter.databinding.ActivityTerminalBinding;
import com.pydroid.interpreter.engine.PythonEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 交互式终端 - Python REPL
 */
public class TerminalActivity extends AppCompatActivity {
    private ActivityTerminalBinding binding;
    private PythonEngine pythonEngine;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private StringBuilder currentBlock = new StringBuilder();
    private boolean inMultiLine = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTerminalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Python REPL");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        pythonEngine = PythonEngine.getInstance();
        
        // 显示欢迎信息
        binding.terminalOutput.setText(
            "Python 3.11.0 on Android\n" +
            "Type \"help\" for help, \"exit()\" to quit.\n" +
            ">>> "
        );
        
        // 处理输入
        binding.terminalInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = binding.terminalInput.getText().toString();
                processInput(input);
                binding.terminalInput.setText("");
                return true;
            }
            return false;
        });
        
        // 上下键历史导航
        binding.terminalInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    navigateHistory(-1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    navigateHistory(1);
                    return true;
                }
            }
            return false;
        });
    }
    
    private void processInput(String input) {
        // 特殊命令
        if (input.equals("exit()") || input.equals("quit()")) {
            finish();
            return;
        }
        
        if (input.equals("help()")) {
            appendOutput("Pydroid3 Python REPL\n" +
                        "输入 Python 代码并回车执行\n" +
                        "多行语句：以冒号结尾的行自动进入多行模式\n" +
                        "上/下键：浏览历史\n" +
                        "exit()：退出\n\n>>> ");
            return;
        }
        
        // 添加到历史
        history.add(input);
        historyIndex = history.size();
        
        // 处理多行输入
        if (inMultiLine) {
            if (input.isEmpty()) {
                // 空行结束多行块
                inMultiLine = false;
                String code = currentBlock.toString();
                currentBlock.setLength(0);
                executeCode(code);
            } else {
                currentBlock.append(input).append("\n");
                appendOutput("... " + input + "\n");
            }
            return;
        }
        
        // 检查是否开始多行块
        if (input.endsWith(":") || input.endsWith(": ")) {
            inMultiLine = true;
            currentBlock.append(input).append("\n");
            appendOutput(input + "\n... ");
            return;
        }
        
        // 单行执行
        appendOutput(input + "\n");
        executeCode(input);
    }
    
    private void executeCode(String code) {
        pythonEngine.interactiveExecute(code, new PythonEngine.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                runOnUiThread(() -> {
                    appendOutput(output + "\n>>> ");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendOutput("Error: " + error + "\n>>> ");
                });
            }
            
            @Override
            public void onExecutionComplete(int exitCode) {
                // 已在 onOutput/onError 中处理
            }
        });
    }
    
    private void appendOutput(String text) {
        binding.terminalOutput.append(text);
        // 自动滚动到底部
        binding.terminalScroll.post(() -> 
            binding.terminalScroll.fullScroll(TextView.FOCUS_DOWN)
        );
    }
    
    private void navigateHistory(int direction) {
        int newIndex = historyIndex + direction;
        if (newIndex >= 0 && newIndex < history.size()) {
            historyIndex = newIndex;
            binding.terminalInput.setText(history.get(historyIndex));
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
