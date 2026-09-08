package com.pydroid.libaci;

import android.os.Bundle;

/**
 * ACI 能力处理器接口
 * 
 * 业务模块实现此接口来处理特定的 ACI 能力
 * 每个能力对应一个 Handler，简化业务逻辑开发
 */
public interface AciHandler {
    
    /**
     * 获取能力规范
     * 定义能力的元数据（ID、描述、参数、结果等）
     */
    CapabilitySpec getSpec();
    
    /**
     * 处理 ACI 请求
     * 
     * @param params 请求参数
     * @return 响应结果
     */
    Bundle handle(Bundle params);
}
