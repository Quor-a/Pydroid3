package ai.aidl.aci.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ACI 能力注册表
 * 用于存储和查询能力元数据
 */
public class AidlAciRegistry {
    private static final Map<String, CapabilityMeta> registry = new LinkedHashMap<>();

    public static class CapabilityMeta {
        public final String packageId;
        public final String capabilityId;
        public final String description;
        public final String[] tags;
        public final long registeredAt;

        public CapabilityMeta(String packageId, String capabilityId, String description, String[] tags) {
            this.packageId = packageId;
            this.capabilityId = capabilityId;
            this.description = description;
            this.tags = tags != null ? tags : new String[0];
            this.registeredAt = System.currentTimeMillis();
        }
    }

    /**
     * 注册能力
     */
    public static void register(String packageId, Capability capability) {
        if (packageId == null || capability == null || capability.getId() == null) {
            return;
        }
        
        String key = packageId + "::" + capability.getId();
        String[] tags = inferTags(capability.getId(), capability.getDescription());
        
        registry.put(key, new CapabilityMeta(
            packageId,
            capability.getId(),
            capability.getDescription(),
            tags
        ));
    }

    /**
     * 获取能力元数据
     */
    public static CapabilityMeta get(String packageId, String capabilityId) {
        String key = packageId + "::" + capabilityId;
        return registry.get(key);
    }

    /**
     * 按包名查询
     */
    public static CapabilityMeta[] byPackage(String packageId) {
        return registry.values().stream()
            .filter(meta -> meta.packageId.equals(packageId))
            .toArray(CapabilityMeta[]::new);
    }

    /**
     * 按标签查询（任意匹配）
     */
    public static CapabilityMeta[] queryByTagsAny(String... tags) {
        if (tags == null || tags.length == 0) {
            return new CapabilityMeta[0];
        }
        
        return registry.values().stream()
            .filter(meta -> {
                for (String tag : meta.tags) {
                    for (String queryTag : tags) {
                        if (tag.equals(queryTag)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toArray(CapabilityMeta[]::new);
    }

    /**
     * 按标签查询（全部匹配）
     */
    public static CapabilityMeta[] queryByTagsAll(String... tags) {
        if (tags == null || tags.length == 0) {
            return new CapabilityMeta[0];
        }
        
        return registry.values().stream()
            .filter(meta -> {
                for (String queryTag : tags) {
                    boolean found = false;
                    for (String tag : meta.tags) {
                        if (tag.equals(queryTag)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
                return true;
            })
            .toArray(CapabilityMeta[]::new);
    }

    /**
     * 获取所有注册的能力
     */
    public static CapabilityMeta[] all() {
        return registry.values().toArray(new CapabilityMeta[0]);
    }

    /**
     * 清空注册表
     */
    public static void clear() {
        registry.clear();
    }

    /**
     * 推断能力标签
     */
    private static String[] inferTags(String capabilityId, String description) {
        String text = (capabilityId + " " + (description != null ? description : "")).toLowerCase();
        
        java.util.List<String> tags = new java.util.ArrayList<>();
        
        if (text.contains("network") || text.contains("http") || text.contains("web") || 
            text.contains("request") || text.contains("fetch")) {
            tags.add("network");
        }
        if (text.contains("file") || text.contains("fs") || text.contains("storage") || 
            text.contains("read") || text.contains("write")) {
            tags.add("fs");
        }
        if (text.contains("message") || text.contains("sms") || text.contains("email") || 
            text.contains("chat")) {
            tags.add("messaging");
        }
        if (text.contains("calendar") || text.contains("event") || text.contains("schedule")) {
            tags.add("calendar");
        }
        if (text.contains("media") || text.contains("image") || text.contains("video") || 
            text.contains("audio") || text.contains("photo")) {
            tags.add("media");
        }
        if (text.contains("location") || text.contains("gps") || text.contains("map") || 
            text.contains("coordinate")) {
            tags.add("location");
        }
        if (text.contains("execute") || text.contains("run") || text.contains("script") || 
            text.contains("command")) {
            tags.add("execute");
        }
        if (text.contains("ui") || text.contains("view") || text.contains("display") || 
            text.contains("show")) {
            tags.add("ui");
        }
        if (text.contains("auth") || text.contains("login") || text.contains("token") || 
            text.contains("password")) {
            tags.add("auth");
        }
        
        if (tags.isEmpty()) {
            tags.add("misc");
        }
        
        return tags.toArray(new String[0]);
    }
}
