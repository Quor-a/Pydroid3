package com.pydroid.interpreter.mcp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP-ACI 桥接器 - 管理 MCP 服务器连接和工具映射
 * 
 * 将 MCP 工具自动映射为 ACI 能力，映射规则：mcp_{toolName}
 */
public class McpBridge {
    private static final String TAG = "McpBridge";
    
    private static McpBridge instance;
    
    // MCP 服务器配置
    private final Map<String, McpServerConfig> servers = new HashMap<>();
    
    // 缓存的 MCP 工具列表
    private final Map<String, List<McpClient.McpTool>> toolCache = new HashMap<>();
    
    // 活跃的 MCP 客户端
    private final Map<String, McpClient> clients = new HashMap<>();
    
    private McpBridge() {}
    
    public static synchronized McpBridge getInstance() {
        if (instance == null) {
            instance = new McpBridge();
        }
        return instance;
    }
    
    /**
     * 添加 MCP 服务器配置
     */
    public void addServer(String name, String url, int timeout) {
        servers.put(name, new McpServerConfig(name, url, timeout));
        Log.i(TAG, "添加 MCP 服务器: " + name + " -> " + url);
    }
    
    /**
     * 移除 MCP 服务器
     */
    public void removeServer(String name) {
        servers.remove(name);
        clients.remove(name);
        toolCache.remove(name);
        Log.i(TAG, "移除 MCP 服务器: " + name);
    }
    
    /**
     * 获取所有 MCP 服务器配置
     */
    public List<McpServerConfig> getServers() {
        return new ArrayList<>(servers.values());
    }
    
    /**
     * 刷新指定服务器的工具列表
     */
    public void refreshTools(String serverName) throws Exception {
        McpServerConfig config = servers.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("MCP 服务器不存在: " + serverName);
        }
        
        McpClient client = clients.get(serverName);
        if (client == null) {
            client = new McpClient(config.url, config.timeout);
            clients.put(serverName, client);
        }
        
        List<McpClient.McpTool> tools = client.listTools();
        toolCache.put(serverName, tools);
        
        Log.i(TAG, "刷新 MCP 工具: " + serverName + " -> " + tools.size() + " 个工具");
    }
    
    /**
     * 刷新所有服务器的工具列表
     */
    public void refreshAllTools() {
        for (String serverName : servers.keySet()) {
            try {
                refreshTools(serverName);
            } catch (Exception e) {
                Log.e(TAG, "刷新 MCP 工具失败: " + serverName, e);
            }
        }
    }
    
    /**
     * 获取所有可用的 MCP 工具（映射为 ACI 能力格式）
     */
    public JSONArray getMcpCapabilities() {
        JSONArray capabilities = new JSONArray();
        
        for (Map.Entry<String, List<McpClient.McpTool>> entry : toolCache.entrySet()) {
            String serverName = entry.getKey();
            for (McpClient.McpTool tool : entry.getValue()) {
                try {
                    JSONObject cap = new JSONObject();
                    cap.put("id", "mcp_" + tool.name);
                    cap.put("description", "[MCP:" + serverName + "] " + tool.description);
                    cap.put("server", serverName);
                    cap.put("tool", tool.name);
                    
                    // 转换参数
                    JSONArray params = new JSONArray();
                    for (McpClient.McpParameter param : tool.parameters) {
                        JSONObject paramObj = new JSONObject();
                        paramObj.put("name", param.name);
                        paramObj.put("type", param.type);
                        paramObj.put("required", param.required);
                        paramObj.put("description", param.description);
                        params.put(paramObj);
                    }
                    cap.put("params", params);
                    
                    capabilities.put(cap);
                } catch (Exception e) {
                    Log.e(TAG, "转换 MCP 工具失败: " + tool.name, e);
                }
            }
        }
        
        return capabilities;
    }
    
    /**
     * 调用 MCP 工具
     * 
     * @param capabilityId ACI 能力 ID（格式：mcp_{toolName}）
     * @param args 参数 JSON
     * @return 调用结果
     */
    public JSONObject callMcpTool(String capabilityId, JSONObject args) throws Exception {
        // 解析能力 ID
        if (!capabilityId.startsWith("mcp_")) {
            throw new IllegalArgumentException("无效的 MCP 能力 ID: " + capabilityId);
        }
        
        String toolName = capabilityId.substring(4);
        
        // 查找工具所在的服务器
        for (Map.Entry<String, List<McpClient.McpTool>> entry : toolCache.entrySet()) {
            String serverName = entry.getKey();
            for (McpClient.McpTool tool : entry.getValue()) {
                if (tool.name.equals(toolName)) {
                    McpClient client = clients.get(serverName);
                    if (client == null) {
                        throw new IllegalStateException("MCP 客户端未初始化: " + serverName);
                    }
                    
                    Log.i(TAG, "调用 MCP 工具: " + serverName + "/" + toolName);
                    return client.callTool(toolName, args);
                }
            }
        }
        
        throw new IllegalArgumentException("MCP 工具不存在: " + toolName);
    }
    
    /**
     * 获取桥接器状态
     */
    public JSONObject getStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("servers_count", servers.size());
            
            int totalTools = 0;
            for (List<McpClient.McpTool> tools : toolCache.values()) {
                totalTools += tools.size();
            }
            status.put("tools_count", totalTools);
            
            JSONArray serverList = new JSONArray();
            for (McpServerConfig config : servers.values()) {
                JSONObject serverObj = new JSONObject();
                serverObj.put("name", config.name);
                serverObj.put("url", config.url);
                serverObj.put("tools_count", toolCache.containsKey(config.name) ? 
                    toolCache.get(config.name).size() : 0);
                serverList.put(serverObj);
            }
            status.put("servers", serverList);
            
            return status;
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            try {
                error.put("error", e.getMessage());
            } catch (Exception ignored) {}
            return error;
        }
    }
    
    /**
     * MCP 服务器配置
     */
    public static class McpServerConfig {
        public final String name;
        public final String url;
        public final int timeout;
        
        public McpServerConfig(String name, String url, int timeout) {
            this.name = name;
            this.url = url;
            this.timeout = timeout;
        }
    }
}
