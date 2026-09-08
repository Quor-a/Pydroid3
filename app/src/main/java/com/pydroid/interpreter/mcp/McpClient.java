package com.pydroid.interpreter.mcp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP 客户端 - 连接和调用 MCP 服务器
 * 
 * MCP (Model Context Protocol) 是一个标准化的工具调用协议
 * 支持 tools/list 和 tools/call 两个核心方法
 */
public class McpClient {
    private static final String TAG = "McpClient";
    
    private final String serverUrl;
    private final int timeout;
    
    public McpClient(String serverUrl, int timeoutMs) {
        this.serverUrl = serverUrl;
        this.timeout = timeoutMs;
    }
    
    /**
     * 获取 MCP 服务器支持的工具列表
     */
    public List<McpTool> listTools() throws Exception {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/list");
        request.put("id", 1);
        
        JSONObject response = sendRequest(request);
        List<McpTool> tools = new ArrayList<>();
        
        if (response.has("result")) {
            JSONObject result = response.getJSONObject("result");
            if (result.has("tools")) {
                JSONArray toolsArray = result.getJSONArray("tools");
                for (int i = 0; i < toolsArray.length(); i++) {
                    JSONObject toolObj = toolsArray.getJSONObject(i);
                    McpTool tool = parseTool(toolObj);
                    tools.add(tool);
                }
            }
        }
        
        return tools;
    }
    
    /**
     * 调用 MCP 工具
     */
    public JSONObject callTool(String toolName, JSONObject arguments) throws Exception {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("params", new JSONObject()
            .put("name", toolName)
            .put("arguments", arguments));
        request.put("id", System.currentTimeMillis());
        
        JSONObject response = sendRequest(request);
        
        if (response.has("error")) {
            JSONObject error = response.getJSONObject("error");
            throw new RuntimeException("MCP 工具调用失败: " + error.optString("message", "未知错误"));
        }
        
        return response.optJSONObject("result");
    }
    
    /**
     * 发送 JSON-RPC 请求
     */
    private JSONObject sendRequest(JSONObject request) throws Exception {
        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);
            
            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP 错误: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return new JSONObject(response.toString());
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * 解析 MCP 工具定义
     */
    private McpTool parseTool(JSONObject toolObj) {
        String name = toolObj.optString("name");
        String description = toolObj.optString("description");
        
        JSONObject inputSchema = toolObj.optJSONObject("inputSchema");
        List<McpParameter> parameters = new ArrayList<>();
        
        if (inputSchema != null && inputSchema.has("properties")) {
            JSONObject properties = inputSchema.optJSONObject("properties");
            JSONArray required = inputSchema.optJSONArray("required");
            
            if (properties != null) {
                JSONArray keys = properties.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String key = keys.optString(i);
                        JSONObject prop = properties.optJSONObject(key);
                        if (key == null || prop == null) continue;
                        String type = prop.optString("type", "string");
                        String desc = prop.optString("description", "");
                        boolean isRequired = required != null && containsString(required, key);
                        
                        parameters.add(new McpParameter(key, type, desc, isRequired));
                    }
                }
            }
        }
        
        return new McpTool(name, description, parameters);
    }
    
    private boolean containsString(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * MCP 工具定义
     */
    public static class McpTool {
        public final String name;
        public final String description;
        public final List<McpParameter> parameters;
        
        public McpTool(String name, String description, List<McpParameter> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("description", description);
                
                JSONArray paramsArray = new JSONArray();
                for (McpParameter param : parameters) {
                    paramsArray.put(param.toJson());
                }
                json.put("parameters", paramsArray);
                
                return json;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }
    
    /**
     * MCP 工具参数定义
     */
    public static class McpParameter {
        public final String name;
        public final String type;
        public final String description;
        public final boolean required;
        
        public McpParameter(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("type", type);
                json.put("description", description);
                json.put("required", required);
                return json;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }
}
