package com.plugflow.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件执行上下文，可在调用链中传递业务身份、请求参数、扩展属性等。
 * 业务方可继承此类增加业务字段。
 */
public class PluginContext {

    /** 扩展属性，用于在不改 Context 子类的前提下传递键值对 */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object v = attributes.get(key);
        return type.isInstance(v) ? (T) v : null;
    }

    public void setAttribute(String key, Object value) {
        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }
    }

    public Map<String, Object> getAttributes() {
        return Map.copyOf(attributes);
    }
}
