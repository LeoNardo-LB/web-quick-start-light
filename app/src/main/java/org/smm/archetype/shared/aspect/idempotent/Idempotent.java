package org.smm.archetype.shared.aspect.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等防护注解。
 * <p>
 * 标注在方法上，通过 AOP 切面实现幂等校验：
 * <ul>
 *   <li>首次调用正常执行，结果被缓存</li>
 *   <li>幂等窗口内相同 Key 重复调用 → 抛 BizException</li>
 *   <li>幂等窗口过期后可正常调用</li>
 * </ul>
 * <p>
 * Key 生成策略：
 * <ul>
 *   <li>field 非空时：使用 SpEL 表达式解析（如 #request.orderId）</li>
 *   <li>field 为空时：使用 className.methodName(paramsHash)</li>
 * </ul>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等窗口超时时间
     */
    long timeout() default 3000;

    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * SpEL 表达式，用于提取幂等 Key 字段。
     * <p>
     * 支持的变量：#p0, #p1（按参数索引）、#参数名（需编译时保留参数名）
     * <p>
     * 为空时使用 className.methodName(paramsHash) 作为默认 Key
     */
    String field() default "";

    /**
     * 重复调用时抛出的异常消息
     */
    String message() default "请勿重复操作";

}
