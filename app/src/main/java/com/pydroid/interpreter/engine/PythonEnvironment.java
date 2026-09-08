package com.pydroid.interpreter.engine;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Python 环境管理器
 * 负责从 assets 提取 Python 标准库和运行时文件
 */
public class PythonEnvironment {
    private static final String TAG = "PythonEnv";
    
    private final Context context;
    private final File pythonHome;
    private final File libDir;
    private final File sitePackagesDir;
    
    public PythonEnvironment(Context context) {
        this.context = context;
        this.pythonHome = new File(context.getFilesDir(), "python");
        this.libDir = new File(pythonHome, "lib");
        this.sitePackagesDir = new File(libDir, "python3.11/site-packages");
    }
    
    /**
     * 初始化 Python 环境
     * 从 assets 提取必要的运行时文件
     */
    public void setup() throws IOException {
        // 创建目录结构
        pythonHome.mkdirs();
        libDir.mkdirs();
        sitePackagesDir.mkdirs();
        
        // 提取标准库（如果尚未提取）
        File marker = new File(pythonHome, ".extracted");
        if (!marker.exists()) {
            extractAssets("python/", pythonHome);
            marker.createNewFile();
            Log.i(TAG, "Python environment extracted to: " + pythonHome);
        }
        
        // 设置环境变量
        setupEnvironment();
    }
    
    private void setupEnvironment() {
        // 设置 PYTHONHOME
        System.setProperty("python.home", pythonHome.getAbsolutePath());
        System.setProperty("python.path", 
            libDir.getAbsolutePath() + File.pathSeparator +
            sitePackagesDir.getAbsolutePath()
        );
    }
    
    /**
     * 从 assets 提取文件
     */
    private void extractAssets(String assetPath, File destDir) throws IOException {
        String[] files = context.getAssets().list(assetPath);
        if (files == null) return;
        
        for (String fileName : files) {
            String fullPath = assetPath + fileName;
            File destFile = new File(destDir, fileName);
            
            // 尝试作为目录列出
            String[] subFiles = context.getAssets().list(fullPath);
            if (subFiles != null && subFiles.length > 0) {
                destFile.mkdirs();
                extractAssets(fullPath + "/", destFile);
            } else {
                // 作为文件提取
                copyAssetToFile(fullPath, destFile);
            }
        }
    }
    
    private void copyAssetToFile(String assetPath, File destFile) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }
    
    /**
     * 安装 pip 包
     */
    public File getSitePackagesDir() {
        return sitePackagesDir;
    }
    
    public File getPythonHome() {
        return pythonHome;
    }
    
    /**
     * 检查环境是否就绪
     */
    public boolean isReady() {
        return pythonHome.exists() && libDir.exists();
    }
}
