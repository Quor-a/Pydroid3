package com.pydroid.interpreter.editor;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pydroid.interpreter.R;
import com.pydroid.interpreter.databinding.ActivityCodeEditorBinding;
import com.pydroid.interpreter.engine.PythonEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 代码编辑器界面
 */
public class CodeEditorActivity extends AppCompatActivity {
    private ActivityCodeEditorBinding binding;
    private PythonEngine pythonEngine;
    private String currentFilePath;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCodeEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("代码编辑器");
        }
        
        pythonEngine = PythonEngine.getInstance();
        
        // 设置回调
        pythonEngine.setCallback(new PythonEngine.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                runOnUiThread(() -> {
                    binding.outputText.append(output);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.outputText.append("错误: " + error + "\n");
                });
            }
            
            @Override
            public void onExecutionComplete(int exitCode) {
                runOnUiThread(() -> {
                    binding.runButton.setEnabled(true);
                    binding.statusIndicator.setText(exitCode == 0 ? "完成" : "错误");
                });
            }
        });
        
        // 按钮事件
        binding.runButton.setOnClickListener(v -> runCode());
        binding.saveButton.setOnClickListener(v -> saveFile());
        binding.clearOutputButton.setOnClickListener(v -> binding.outputText.setText(""));
        
        // 加载示例代码
        binding.codeEditor.setText(getSampleCode());
    }
    
    private void runCode() {
        String code = binding.codeEditor.getText().toString();
        if (code.isEmpty()) {
            Toast.makeText(this, "代码为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.runButton.setEnabled(false);
        binding.statusIndicator.setText("运行中...");
        binding.outputText.append("\n--- 执行开始 ---\n");
        
        pythonEngine.executeCode(code);
    }
    
    private void saveFile() {
        // 实际实现需要文件选择器
        String code = binding.codeEditor.getText().toString();
        String filename = "script_" + System.currentTimeMillis() + ".py";
        
        try {
            File file = new File(getExternalFilesDir(null), filename);
            FileWriter writer = new FileWriter(file);
            writer.write(code);
            writer.close();
            
            currentFilePath = file.getAbsolutePath();
            Toast.makeText(this, "已保存: " + filename, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getSampleCode() {
        return "# Pydroid3 示例代码\n" +
               "\n" +
               "# 基本输出\n" +
               "print(\"Hello, Python on Android!\")\n" +
               "\n" +
               "# 变量和运算\n" +
               "x = 10\n" +
               "y = 20\n" +
               "print(x + y)\n" +
               "\n" +
               "# 列表\n" +
               "numbers = [1, 2, 3, 4, 5]\n" +
               "print(numbers)\n" +
               "\n" +
               "# 循环\n" +
               "for i in range(5):\n" +
               "    print(i)\n" +
               "\n" +
               "# 函数定义\n" +
               "def greet(name):\n" +
               "    return f\"Hello, {name}!\"\n" +
               "\n" +
               "print(greet(\"Android\"))\n";
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_new) {
            binding.codeEditor.setText("");
            return true;
        } else if (id == R.id.action_open) {
            Toast.makeText(this, "打开文件（待实现）", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_save) {
            saveFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
