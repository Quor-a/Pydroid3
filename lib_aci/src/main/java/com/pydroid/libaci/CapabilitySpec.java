package com.pydroid.libaci;

import java.util.ArrayList;
import java.util.List;

/**
 * 能力规范定义
 * 
 * 描述一个 ACI 能力的元数据：
 * - ID：能力的唯一标识
 * - 描述：给 LLM 看的自然语言描述
 * - 参数：输入参数定义
 * - 结果：输出结果定义
 * - 标志：行为标志（后台执行、无需 UI、危险操作等）
 */
public class CapabilitySpec {
    
    private final String id;
    private final String description;
    private final List<Param> params;
    private final List<Param> results;
    private final List<String> flags;
    private final boolean dangerous;
    
    public CapabilitySpec(String id, String description, boolean dangerous) {
        this.id = id;
        this.description = description;
        this.dangerous = dangerous;
        this.params = new ArrayList<>();
        this.results = new ArrayList<>();
        this.flags = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<Param> getParams() {
        return params;
    }
    
    public List<Param> getResults() {
        return results;
    }
    
    public List<String> getFlags() {
        return flags;
    }
    
    public boolean isDangerous() {
        return dangerous;
    }
    
    /**
     * 添加参数定义
     */
    public CapabilitySpec addParam(String name, String type, boolean required, String description) {
        params.add(new Param(name, type, required, description));
        return this;
    }
    
    /**
     * 添加结果定义
     */
    public CapabilitySpec addResult(String name, String type, String description) {
        results.add(new Param(name, type, false, description));
        return this;
    }
    
    /**
     * 添加行为标志
     */
    public CapabilitySpec addFlag(String flag) {
        flags.add(flag);
        return this;
    }
    
    /**
     * 参数定义
     */
    public static class Param {
        private final String name;
        private final String type;
        private final boolean required;
        private final String description;
        
        public Param(String name, String type, boolean required, String description) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
