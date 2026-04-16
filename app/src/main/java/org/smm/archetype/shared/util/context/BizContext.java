package org.smm.archetype.shared.util.context;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Scope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;

/**
 * 业务上下文传播工具。
 * <p>
 * 基于 Java 25 ScopedValue 实现，提供请求作用域内的业务上下文数据存取。
 * 通过枚举键定义上下文属性，支持扩展。
 * <p>
 * 标记为 {@code propagated=true} 的键在<strong>请求线程</strong>中通过 {@link Key#set} 修改时
 * 会自动同步到 OTel Baggage，实现跨服务传播。
 * <strong>子线程</strong>中的 {@code set()} 仅修改自己的 EnumMap 副本，不会写入 Baggage。
 * <p>
 * 注意：traceId 由 OTel Span 全权负责（{@code Span.current().getSpanContext().getTraceId()}），
 * 不属于业务上下文的范畴。
 */
@Slf4j
public final class BizContext {

    /**
     * 内部持有器：包装 EnumMap + 是否为副本标记。
     * <p>
     * 副本（子线程）的 {@code set()} 仅修改 EnumMap，不写入 OTel Baggage。
     */
    public static final class Holder {
        final EnumMap<Key, String> map;
        final boolean replica;

        Holder(EnumMap<Key, String> map, boolean replica) {
            this.map = map;
            this.replica = replica;
        }
    }

    private static final ScopedValue<Holder> SCOPED = ScopedValue.newInstance();

    /**
     * 业务上下文键。
     * <p>
     * 扩展方式：新增枚举值即可。
     * <ul>
     *   <li>{@code propagated=true} 的键在请求线程中自动写入 OTel Baggage</li>
     *   <li>{@code propagated=false} 的键仅在当前 JVM 内可见</li>
     * </ul>
     */
    public enum Key {

        /** 用户 ID — 跨服务传播 */
        USER_ID("userId", true);

        @Getter
        private final String  baggageKey;

        @Getter
        private final boolean propagated;

        Key(String baggageKey, boolean propagated) {
            this.baggageKey = baggageKey;
            this.propagated = propagated;
        }

        public String get() {
            if (!SCOPED.isBound()) return null;
            return SCOPED.get().map.get(this);
        }

        /**
         * 在当前作用域内更新值。
         * <p>
         * 请求线程（非副本）：propagated=true 的键自动同步 OTel Baggage。
         * 子线程（副本）：仅修改 EnumMap，不写入 Baggage。
         */
        public void set(String value) {
            if (!SCOPED.isBound()) return;
            Holder holder = SCOPED.get();
            holder.map.put(this, value);
            if (propagated && !holder.replica) {
                syncToBaggage(this, value);
            }
            // 如果在子线程中设置，则打印警告信息，说明子线程不会设置到 OTel Baggage 中
            if (holder.replica) {
                log.warn("Setting {} in replica context, will not be propagated to OTel Baggage", this);
            }
        }

    }

    private BizContext() {}

    // ================================================================
    // runWithContext
    // ================================================================

    public static void runWithContext(Runnable action, EnumMap<Key, String> context) {
        Baggage baggage = buildBaggage(context);
        Holder holder = new Holder(context, false);
        if (baggage != null) {
            try (Scope _ = baggage.makeCurrent()) {
                ScopedValue.where(SCOPED, holder).run(action);
            }
        } else {
            ScopedValue.where(SCOPED, holder).run(action);
        }
    }

    /**
     * 便捷方法：使用单个 Key-Value 绑定上下文。
     *
     * @param action 在上下文内执行的操作
     * @param key    上下文键
     * @param value  上下文值（可为 null）
     */
    public static void runWithContext(Runnable action, Key key, String value) {
        EnumMap<Key, String> context = new EnumMap<>(Key.class);
        if (value != null) context.put(key, value);
        runWithContext(action, context);
    }

    // ================================================================
    // 上下文导出
    // ================================================================

    public static EnumMap<Key, String> getContext() {
        if (!SCOPED.isBound()) return null;
        return SCOPED.get().map;
    }

    public static EnumMap<Key, String> copyContext() {
        if (!SCOPED.isBound()) return null;
        return new EnumMap<>(SCOPED.get().map);
    }

    /** 创建副本 Holder（供 TaskDecorator 内部使用）。 */
    public static Holder copyAsReplica() {
        if (!SCOPED.isBound()) return null;
        return new Holder(new EnumMap<>(SCOPED.get().map), true);
    }

    public static ScopedValue<Holder> getScoped() {
        return SCOPED;
    }

    // ================================================================
    // 便捷方法
    // ================================================================

    public static String getUserId() { return Key.USER_ID.get(); }

    // ================================================================
    // 内部
    // ================================================================

    private static Baggage buildBaggage(EnumMap<Key, String> context) {
        BaggageBuilder builder = null;
        for (Map.Entry<Key, String> entry : context.entrySet()) {
            if (entry.getKey().propagated && entry.getValue() != null) {
                if (builder == null) {
                    builder = Baggage.current().toBuilder();
                }
                builder.put(entry.getKey().baggageKey, entry.getValue());
            }
        }
        return builder != null ? builder.build() : null;
    }

    private static void syncToBaggage(Key key, String value) {
        Baggage updated = Baggage.current().toBuilder()
                .put(key.baggageKey, value != null ? value : "")
                .build();
        updated.makeCurrent();
    }
}
