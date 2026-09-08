package ai.aidl.aci.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ACI 事件总线
 * 用于进程内事件订阅和发布
 */
public class AidlAciEvents {
    public static final String EVENT_SERVICE_BOUND = "service_bound";
    public static final String EVENT_SERVICE_UNBOUND = "service_unbound";
    public static final String EVENT_CALL_FAILED = "call_failed";
    public static final String EVENT_DISCOVERED = "discovered";
    public static final String EVENT_PROTOCOL_NEGOTIATED = "protocol_negotiated";

    private static final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    public interface EventListener {
        void onEvent(String event, Object data);
    }

    public static void subscribe(EventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

    public static void emit(String event, Object data) {
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event, data);
            } catch (Throwable e) {
                // 忽略监听器的异常，避免影响其他监听器
            }
        }
    }

    public static void clear() {
        listeners.clear();
    }
}
