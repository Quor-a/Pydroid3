package com.pydroid.libaci;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ACI 权限守卫
 * 
 * 负责调用方鉴权：
 * 1. 白名单检查：只允许指定的控制端包名调用
 * 2. 危险能力检查：危险能力需要额外的权限验证
 */
public class PermissionGuard {
    
    private static final String TAG = "PermissionGuard";
    
    // 允许的控制端包名白名单
    private static final Set<String> CONTROLLER_PKGS = new HashSet<>(Arrays.asList(
        "com.ai.assistance.quro",  // ZorvAI 主程序
        "com.ai.assistance.quro.browser"  // ZorvBrowser 浏览器
    ));
    
    /**
     * 检查调用方是否有权限调用指定能力
     * 
     * @param callerPkg 调用方包名
     * @param capabilityId 能力 ID
     * @return true 表示允许，false 表示拒绝
     */
    public static boolean checkPermission(String callerPkg, String capabilityId) {
        // 1. 白名单检查
        if (!CONTROLLER_PKGS.contains(callerPkg)) {
            Log.w(TAG, "Caller not in whitelist: " + callerPkg);
            return false;
        }
        
        // 2. 危险能力检查
        AciHandler handler = CapabilityRegistry.get(capabilityId);
        if (handler != null && handler.getSpec().isDangerous()) {
            // 危险能力需要额外验证（这里可以扩展更多逻辑）
            Log.i(TAG, "Dangerous capability called: " + capabilityId + " by " + callerPkg);
            // 目前只记录日志，实际使用时可以添加更多验证
        }
        
        return true;
    }
    
    /**
     * 添加允许的控制端包名
     * 
     * @param pkg 包名
     */
    public static void addAllowedPackage(String pkg) {
        if (pkg != null && !pkg.isEmpty()) {
            CONTROLLER_PKGS.add(pkg);
            Log.i(TAG, "Added allowed package: " + pkg);
        }
    }
    
    /**
     * 移除允许的控制端包名
     * 
     * @param pkg 包名
     */
    public static void removeAllowedPackage(String pkg) {
        if (pkg != null && !pkg.isEmpty()) {
            CONTROLLER_PKGS.remove(pkg);
            Log.i(TAG, "Removed allowed package: " + pkg);
        }
    }
    
    /**
     * 获取所有允许的控制端包名
     */
    public static Set<String> getAllowedPackages() {
        return new HashSet<>(CONTROLLER_PKGS);
    }
}
