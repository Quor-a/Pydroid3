package ai.aidl.aci.core;

import org.json.JSONObject;

/**
 * ACI 结构化错误模型
 * 让 LLM 能自助纠错
 */
public class AidlAciErrorModel {
    // 语义码
    public static final int E_SERVICE_UNBOUND = 1503;
    public static final int E_TIMEOUT = 1504;
    public static final int E_BAD_REQUEST = 2400;
    public static final int E_HTTP_CLIENT = 2500;
    public static final int E_HTTP_TLS = 2520;
    public static final int E_INTERNAL = 2599;

    public final int code;
    public final String message;
    public final String suggestion;
    public final String layer;  // binder, http, protocol

    public AidlAciErrorModel(int code, String message, String suggestion, String layer) {
        this.code = code;
        this.message = message;
        this.suggestion = suggestion;
        this.layer = layer;
    }

    /**
     * 序列化为 JSON
     */
    public String toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("code", code);
            json.put("message", message);
            json.put("suggestion", suggestion);
            json.put("layer", layer);
            return json.toString();
        } catch (Throwable e) {
            return "{\"code\":" + code + ",\"message\":\"" + message + "\"}";
        }
    }

    /**
     * 从 JSON 解析
     */
    public static AidlAciErrorModel fromJSON(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return new AidlAciErrorModel(
                obj.optInt("code", 0),
                obj.optString("message", ""),
                obj.optString("suggestion", ""),
                obj.optString("layer", "")
            );
        } catch (Throwable e) {
            return new AidlAciErrorModel(0, json, "", "");
        }
    }

    /**
     * 从 Binder 响应生成错误建议
     */
    public static AidlAciErrorModel fromBinderResponse(AidlAciResponse response) {
        if (response == null || response.isSuccess()) {
            return null;
        }

        int code = response.getErrorCode();
        String message = response.getErrorMessage();
        String suggestion;
        String layer = "binder";

        switch (code) {
            case AidlAciError.SERVICE_UNAVAILABLE:
                suggestion = "目标应用未运行，请先启动应用或检查 WakeReceiver 是否正确配置";
                break;
            case AidlAciError.TIMEOUT:
                suggestion = "调用超时，可能是目标应用处理过慢或已卡死，建议增加超时时间或重启应用";
                break;
            case AidlAciError.PERMISSION_DENIED:
                suggestion = "权限被拒绝，检查调用方包名是否在白名单中，或 Token 是否有效";
                break;
            case AidlAciError.CAPABILITY_NOT_FOUND:
                suggestion = "能力不存在，检查能力 ID 是否正确，或目标应用版本是否支持该能力";
                break;
            case AidlAciError.BAD_REQUEST:
                suggestion = "请求参数错误，检查参数类型和必填项是否符合要求";
                break;
            default:
                suggestion = "内部错误，请查看目标应用的日志获取详细信息";
        }

        return new AidlAciErrorModel(code, message, suggestion, layer);
    }

    /**
     * 从 HTTP 错误生成建议
     */
    public static AidlAciErrorModel httpSuggestion(int httpCode, String message) {
        String suggestion;
        int aciCode;

        if (httpCode >= 400 && httpCode < 500) {
            aciCode = E_HTTP_CLIENT;
            suggestion = "HTTP 客户端错误，检查请求参数、URL 或认证信息";
        } else if (httpCode >= 500) {
            aciCode = E_HTTP_CLIENT;
            suggestion = "HTTP 服务器错误，目标服务可能暂时不可用";
        } else if (message != null && message.toLowerCase().contains("ssl") || 
                   message != null && message.toLowerCase().contains("certificate")) {
            aciCode = E_HTTP_TLS;
            suggestion = "HTTPS 证书校验失败，可能是证书过期、自签名证书或中间人攻击";
        } else {
            aciCode = E_HTTP_CLIENT;
            suggestion = "HTTP 请求失败，检查网络连接和目标服务状态";
        }

        return new AidlAciErrorModel(aciCode, message, suggestion, "http");
    }
}
