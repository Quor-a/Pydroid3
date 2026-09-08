package com.pydroid.libaci;

import android.os.Bundle;
import android.util.Log;

import ai.aidl.aci.core.AidlAciError;
import ai.aidl.aci.core.AidlAciRequest;
import ai.aidl.aci.core.AidlAciResponse;

/**
 * ACI 能力路由器
 * 
 * 负责将 ACI 请求派发到对应的 Handler
 * 统一处理异常和错误响应
 */
public class AciRouter {
    
    private static final String TAG = "AciRouter";
    
    /**
     * 派发 ACI 请求到对应的 Handler
     * 
     * @param request ACI 请求
     * @return ACI 响应
     */
    public static AidlAciResponse dispatch(AidlAciRequest request) {
        if (request == null) {
            return AidlAciResponse.error(AidlAciError.BAD_REQUEST, "Request is null");
        }
        
        String capabilityId = request.getCapability();
        if (capabilityId == null || capabilityId.isEmpty()) {
            return AidlAciResponse.error(AidlAciError.BAD_REQUEST, "Capability ID is empty");
        }
        
        // 查找对应的 Handler
        AciHandler handler = CapabilityRegistry.get(capabilityId);
        if (handler == null) {
            return AidlAciResponse.error(
                AidlAciError.CAPABILITY_NOT_FOUND,
                "Capability not found: " + capabilityId
            );
        }
        
        try {
            // 调用 Handler 处理请求
            Bundle params = request.getParams();
            Bundle result = handler.handle(params);
            
            // 构建成功响应
            AidlAciResponse response = AidlAciResponse.success();
            if (result != null) {
                response.setResult(result);
            }
            return response;
            
        } catch (IllegalArgumentException e) {
            // 参数错误
            Log.w(TAG, "Bad request for capability: " + capabilityId, e);
            return AidlAciResponse.error(AidlAciError.BAD_REQUEST, e.getMessage());
            
        } catch (Exception e) {
            // 内部错误
            Log.e(TAG, "Error handling capability: " + capabilityId, e);
            return AidlAciResponse.error(
                AidlAciError.INTERNAL_ERROR,
                "Handler error: " + e.getMessage()
            );
        }
    }
}
