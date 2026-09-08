# Pydroid3 示例脚本
# 这个文件会在应用启动时被打包到 APK 中

import sys
import os

def hello():
    """基础示例"""
    print("Hello from Python on Android!")
    print(f"Python version: {sys.version}")
    print(f"Platform: {sys.platform}")
    print(f"Python path: {sys.path}")
    return True

def math_demo():
    """数学运算示例"""
    import math
    print(f"\n数学示例:")
    print(f"π = {math.pi}")
    print(f"e = {math.e}")
    print(f"√2 = {math.sqrt(2)}")
    print(f"sin(30°) = {math.sin(math.radians(30))}")
    return True

def list_demo():
    """列表操作示例"""
    print(f"\n列表示例:")
    numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    print(f"数字列表: {numbers}")
    print(f"偶数: {[n for n in numbers if n % 2 == 0]}")
    print(f"平方: {[n**2 for n in numbers]}")
    print(f"总和: {sum(numbers)}")
    return True

def dict_demo():
    """字典操作示例"""
    print(f"\n字典示例:")
    person = {
        "name": "Pydroid3",
        "version": "1.0",
        "platform": "Android",
        "python": "3.11"
    }
    for key, value in person.items():
        print(f"{key}: {value}")
    return True

def class_demo():
    """类定义示例"""
    print(f"\n类示例:")
    
    class Calculator:
        def __init__(self):
            self.result = 0
        
        def add(self, x):
            self.result += x
            return self
        
        def subtract(self, x):
            self.result -= x
            return self
        
        def get_result(self):
            return self.result
    
    calc = Calculator()
    calc.add(10).add(5).subtract(3)
    print(f"计算结果: {calc.get_result()}")
    return True

if __name__ == "__main__":
    print("=" * 50)
    print("Pydroid3 - Android Python 解释器")
    print("=" * 50)
    
    hello()
    math_demo()
    list_demo()
    dict_demo()
    class_demo()
    
    print("\n" + "=" * 50)
    print("所有示例运行成功!")
    print("=" * 50)
