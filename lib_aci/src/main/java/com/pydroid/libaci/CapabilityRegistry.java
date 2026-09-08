package com.pydroid.libaci;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 能力注册中心
 * 
 * 管理所有 ACI 能力的注册和查询
 * 使用 LinkedHashMap 保持注册顺序
 */
public class CapabilityRegistry {
    
    private static final Map<String, AciHandler> handlers = new LinkedHashMap<>();
    
    /**
     * 注册能力处理器
     * 
     * @param handler 能力处理器
     */
    public static void register(AciHandler handler) {
        if (handler == null || handler.getSpec() == null) {
            return;
        }
        
        String id = handler.getSpec().getId();
        if (id == null || id.isEmpty()) {
            return;
        }
        
        // 幂等注册：同 ID 覆盖
        handlers.put(id, handler);
    }
    
    /**
     * 获取能力处理器
     * 
     * @param id 能力 ID
     * @return 能力处理器，如果不存在返回 null
     */
    public static AciHandler get(String id) {
        return handlers.get(id);
    }
    
    /**
     * 获取所有已注册的能力处理器
     */
    public static Map<String, AciHandler> all() {
        return new LinkedHashMap<>(handlers);
    }
    
    /**
     * 获取所有能力 ID
     */
    public static String[] ids() {
        return handlers.keySet().toArray(new String[0]);
    }
    
    /**
     * 清空所有注册的能力
     */
    public static void clear() {
        handlers.clear();
    }
    
    /**
     * 检查能力是否已注册
     */
    public static boolean contains(String id) {
        return handlers.containsKey(id);
    }
}
