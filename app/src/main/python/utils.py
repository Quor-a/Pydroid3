# Pydroid3 工具函数库

import sys
import os
import json
from datetime import datetime

def get_python_info():
    """获取 Python 环境信息"""
    return {
        "version": sys.version,
        "platform": sys.platform,
        "executable": sys.executable,
        "path": sys.path,
        "prefix": sys.prefix,
        "encoding": sys.getdefaultencoding()
    }

def get_system_info():
    """获取系统信息"""
    import platform
    return {
        "system": platform.system(),
        "release": platform.release(),
        "machine": platform.machine(),
        "processor": platform.processor(),
        "python_version": platform.python_version()
    }

def save_to_file(filename, content):
    """保存内容到文件"""
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(content)
        return True, f"成功保存到 {filename}"
    except Exception as e:
        return False, f"保存失败: {str(e)}"

def read_from_file(filename):
    """从文件读取内容"""
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return True, f.read()
    except Exception as e:
        return False, f"读取失败: {str(e)}"

def list_files(directory='.'):
    """列出目录中的文件"""
    try:
        files = os.listdir(directory)
        return True, files
    except Exception as e:
        return False, str(e)

def execute_code(code):
    """执行 Python 代码并捕获输出"""
    import io
    from contextlib import redirect_stdout, redirect_stderr
    
    stdout_capture = io.StringIO()
    stderr_capture = io.StringIO()
    
    try:
        with redirect_stdout(stdout_capture), redirect_stderr(stderr_capture):
            exec(code, globals())
        
        output = stdout_capture.getvalue()
        error = stderr_capture.getvalue()
        
        return {
            "success": True,
            "output": output,
            "error": error
        }
    except Exception as e:
        return {
            "success": False,
            "output": stdout_capture.getvalue(),
            "error": str(e)
        }

def timestamp():
    """获取当前时间戳"""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def pretty_print(obj, indent=2):
    """美化打印 JSON 对象"""
    print(json.dumps(obj, indent=indent, ensure_ascii=False))

# 测试函数
if __name__ == "__main__":
    print("Pydroid3 工具库")
    print("-" * 40)
    
    print("\nPython 信息:")
    pretty_print(get_python_info())
    
    print("\n系统信息:")
    pretty_print(get_system_info())
    
    print(f"\n当前时间: {timestamp()}")
