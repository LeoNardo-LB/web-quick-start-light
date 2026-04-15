package org.smm.archetype.component.dto;

/**
 * 服务提供商枚举。
 * <p>
 * 用于 Email、SMS 等客户端模块标识不同的服务提供商。
 */
public enum ServiceProvider {
    /** 阿里云 */
    ALIYUN,
    /** 腾讯云 */
    TENCENT,
    /** 本地（无操作） */
    LOCAL,
    /** 自定义 */
    CUSTOM
}
