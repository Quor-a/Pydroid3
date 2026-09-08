package com.pydroid.interpreter.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pydroid.interpreter.R;
import com.pydroid.interpreter.databinding.ActivityMainBinding;
import com.pydroid.interpreter.engine.PythonEngine;

/**
 * 主界面 - Python 解释器控制台
 */
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private PythonEngine pythonEngine;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        
        // 初始化 Python 引擎
        pythonEngine = PythonEngine.getInstance();
        pythonEngine.initialize(this, new PythonEngine.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                runOnUiThread(() -> {
                    binding.outputText.append(output);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.outputText.append("Error: " + error + "\n");
                });
            }
            
            @Override
            public void onExecutionComplete(int exitCode) {
                runOnUiThread(() -> {
                    binding.runButton.setEnabled(true);
                    if (exitCode == 0) {
                        binding.statusIndicator.setText("就绪");
                    } else {
                        binding.statusIndicator.setText("错误");
                    }
                });
            }
        });
        
        // 设置按钮点击事件
        binding.runButton.setOnClickListener(v -> runCode());
        binding.clearButton.setOnClickListener(v -> clearOutput());
        
        // 显示欢迎信息
        binding.outputText.setText("Pydroid3 - Python 解释器\n");
        binding.outputText.append("版本: " + pythonEngine.getVersion() + "\n");
        binding.outputText.append("================================\n\n");
    }
    
    private void runCode() {
        String code = binding.codeInput.getText().toString();
        if (code.isEmpty()) {
            Toast.makeText(this, "请输入代码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.runButton.setEnabled(false);
        binding.statusIndicator.setText("运行中...");
        binding.outputText.append(">>> " + code + "\n");
        
        pythonEngine.executeCode(code);
    }
    
    private void clearOutput() {
        binding.outputText.setText("");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            clearOutput();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showAbout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于 Pydroid3")
            .setMessage("Android 原生 Python 解释器\n\n" +
                       "版本: 1.0\n" +
                       "Python: 3.11.0\n\n" +
                       "功能:\n" +
                       "• 执行 Python 代码\n" +
                       "• 交互式 REPL\n" +
                       "• 文件执行\n" +
                       "• 代码编辑器\n\n" +
                       "© 2024 Pydroid3")
            .setPositiveButton("确定", null)
            .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        pythonEngine.shutdown();
    }
}
