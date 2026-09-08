package ai.aidl.aci.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ACI 审计日志
 * 记录每次 ACI 调用的详细信息
 */
public class AidlAciAudit {
    private static final String PREFS_NAME = "aci_audit";
    private static final String KEY_LOG = "audit_log";
    private static final int MAX_ENTRIES = 500;

    private final SharedPreferences prefs;

    public AidlAciAudit(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 记录一次 ACI 调用
     */
    public void log(String targetPackage, String capability, boolean success, 
                   int errorCode, long durationMs) {
        try {
            JSONArray log = getLog();
            
            JSONObject entry = new JSONObject();
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date()));
            entry.put("target", targetPackage);
            entry.put("capability", capability);
            entry.put("success", success);
            entry.put("error_code", errorCode);
            entry.put("duration_ms", durationMs);
            
            log.put(entry);
            
            // 保持日志大小
            while (log.length() > MAX_ENTRIES) {
                log.remove(0);
            }
            
            prefs.edit().putString(KEY_LOG, log.toString()).apply();
        } catch (Throwable e) {
            // 审计日志失败不应影响主流程
        }
    }

    /**
     * 获取审计日志
     */
    public JSONArray getLog() {
        try {
            String json = prefs.getString(KEY_LOG, "[]");
            return new JSONArray(json);
        } catch (Throwable e) {
            return new JSONArray();
        }
    }

    /**
     * 清空审计日志
     */
    public void clear() {
        prefs.edit().remove(KEY_LOG).apply();
    }

    /**
     * 获取日志条目数量
     */
    public int size() {
        return getLog().length();
    }
}
