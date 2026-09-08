# exec_helper.py - Python 代码执行辅助模块
# 解决从 Java 调用 exec() 时 "frame does not exist" 的问题

def run_code(code):
    """执行 Python 代码并返回输出"""
    import sys
    from io import StringIO
    
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    sys.stdout = StringIO()
    sys.stderr = StringIO()
    
    try:
        exec(code, globals())
        output = sys.stdout.getvalue()
        error = sys.stderr.getvalue()
    except Exception as e:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        output = ""
        error = str(e)
    else:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
    
    result = output
    if error:
        result += "\nError: " + error
    return result

def eval_code(code):
    """执行单行表达式并返回结果"""
    try:
        result = eval(code, globals())
        return str(result) if result is not None else ""
    except Exception as e:
        return f"Error: {e}"
