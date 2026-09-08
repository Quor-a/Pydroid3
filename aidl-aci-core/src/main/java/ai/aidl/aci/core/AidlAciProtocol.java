package ai.aidl.aci.core;

/**
 * ACI 协议协商器
 * 用于控制端和受控端协商支持的协议版本
 */
public class AidlAciProtocol {
    public static final String PROTOCOL_VERSION = "aci-protocol-v1";
    public static final String PROTOCOL_SEMVER = "1.0.0";
    public static final String[] SUPPORTED = {"aci-protocol-v1"};

    /**
     * 协商协议版本
     * @param peerSupported 对端支持的协议列表
     * @return 双方都支持的最高版本，如果没有共同版本返回 null
     */
    public static String negotiate(String[] peerSupported) {
        if (peerSupported == null || peerSupported.length == 0) {
            return null;
        }
        
        // 从后往前找第一个匹配的（假设版本从低到高排列）
        for (int i = SUPPORTED.length - 1; i >= 0; i--) {
            for (String peer : peerSupported) {
                if (SUPPORTED[i].equals(peer)) {
                    return SUPPORTED[i];
                }
            }
        }
        return null;
    }

    /**
     * 检查是否支持指定协议
     */
    public static boolean isSupported(String protocol) {
        if (protocol == null) return false;
        for (String p : SUPPORTED) {
            if (p.equals(protocol)) return true;
        }
        return false;
    }
}
