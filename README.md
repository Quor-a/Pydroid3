# Pydroid3 - Android Python 解释器

一个完整的 Android Python 解释器应用，使用 **Chaquoco** 插件嵌入 Python 3.11。

## 项目特点

✅ **零配置** - 使用 Chaquoco 插件，自动处理 Python 嵌入
✅ **完整功能** - 代码执行、REPL、代码编辑器
✅ **预编译 Python** - 无需手动交叉编译 CPython
✅ **标准库支持** - 完整的 Python 3.11 标准库

## 项目结构

```
Pydroid3/
├── app/
│   ├── src/main/
│   │   ├── java/com/pydroid/interpreter/
│   │   │   ├── engine/          # 核心引擎
│   │   │   │   ├── PythonEngine.java      # Python 解释器引擎
│   │   │   │   ├── PythonEnvironment.java # 环境管理
│   │   │   │   └── PipManager.java        # pip 包管理
│   │   │   ├── editor/          # 代码编辑器
│   │   │   │   └── CodeEditorActivity.java
│   │   │   └── ui/              # 用户界面
│   │   │       ├── MainActivity.java      # 主控制台
│   │   │       └── TerminalActivity.java  # 交互式 REPL
│   │   ├── python/              # Python 代码（会被打包到 APK）
│   │   │   ├── main.py          # 示例脚本
│   │   │   └── utils.py         # 工具函数
│   │   ├── res/                 # Android 资源
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle                 # 项目级配置（包含 Chaquoco 插件）
├── settings.gradle
└── README.md
```

## 快速开始

### 1. 用 Android Studio 打开项目

```bash
# 解压项目
tar -xzf Pydroid3_Chaquoco.tar.gz
cd Pydroid3

# 用 Android Studio 打开
```

### 2. 同步 Gradle

Android Studio 会自动：
- 下载 Chaquoco 插件
- 下载预编译的 Python 3.11
- 配置所有依赖

### 3. 运行应用

点击 Run 按钮，应用会自动构建并安装到设备/模拟器。

## 技术架构

```
┌─────────────────────────────────────┐
│      Android UI (Java)              │
│  MainActivity / EditorActivity      │
│  TerminalActivity                   │
├─────────────────────────────────────┤
│   PythonEngine (Java)               │
│   - Chaquoco API 调用               │
│   - 线程管理                         │
│   - 输出捕获                         │
├─────────────────────────────────────┤
│   Chaquoco Plugin                   │
│   - 自动管理 Python 运行时          │
│   - 预编译的 Python 3.11            │
│   - 标准库打包                       │
└─────────────────────────────────────┘
```

## 功能说明

### 主控制台 (MainActivity)
- 快速执行 Python 代码
- 查看执行输出
- 清空输出

### 代码编辑器 (CodeEditorActivity)
- 多行代码编辑
- 保存/加载文件
- 运行代码并查看输出

### 交互式终端 (TerminalActivity)
- Python REPL
- 多行输入支持
- 命令历史导航（上下键）
- 支持 help() 命令

### Python 引擎 (PythonEngine)
- 使用 Chaquoco 执行 Python 代码
- 自动捕获 stdout/stderr
- 线程安全的代码执行
- 支持单行和文件执行

## 添加 pip 包

在 `app/build.gradle` 中添加：

```gradle
python {
    version "3.11"
    abiFilters "arm64-v8a", "armeabi-v7a"
    
    // 添加 pip 包
    pip {
        install "numpy"
        install "requests"
        install "matplotlib"
    }
}
```

## 系统要求

- Android 7.0+ (API 24)
- Android Studio Hedgehog+ (2023.1.1+)
- 约 30MB 存储空间（含 Python 运行时）

## 与手动交叉编译的对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| **Chaquoco（本方案）** | 零配置、自动管理、稳定 | APK 体积较大 |
| 手动交叉编译 | 完全控制、可优化体积 | 需要 NDK、编译复杂 |

## 已知限制

1. **APK 体积** - 包含 Python 运行时，约 30-50MB
2. **pip 安装** - 需要在 build.gradle 中预声明
3. **C 扩展** - 需要 Chaquoco 支持的预编译版本

## 后续优化

1. **代码编辑器增强**
   - 语法高亮
   - 代码补全
   - 多文件管理

2. **功能扩展**
   - 添加更多 pip 包支持
   - matplotlib 图形显示
   - 文件浏览器

3. **UI 改进**
   - 深色/浅色主题
   - 自定义字体大小
   - 代码片段库

## 许可证

MIT License

## 致谢

- [Chaquoco](https://chaquo.com/chaquoco/) - Android Python 嵌入方案
- [CPython](https://www.python.org/) - Python 解释器

---

**提示**：首次构建时，Gradle 会下载 Python 运行时，可能需要几分钟。
