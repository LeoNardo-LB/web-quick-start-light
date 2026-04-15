package org.smm.archetype.shared.aspect.idempotent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.cache.CacheComponent;
import org.smm.archetype.exception.BizException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IdempotentAspect 切面")
class IdempotentAspectUTest {

    private IdempotentAspect aspect;
    private CacheComponent     cacheComponent;

    @BeforeEach
    void setUp() {
        cacheComponent = mock(CacheComponent.class);
        aspect = new IdempotentAspect(cacheComponent);
    }

    // --- 辅助方法 ---

    private ProceedingJoinPoint mockJoinPoint(String className, String methodName,
                                              String[] paramNames, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(className);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    private Idempotent mockIdempotent(long timeout, TimeUnit timeUnit, String field, String message) {
        Idempotent idempotent = mock(Idempotent.class);
        when(idempotent.timeout()).thenReturn(timeout);
        when(idempotent.timeUnit()).thenReturn(timeUnit);
        when(idempotent.field()).thenReturn(field);
        when(idempotent.message()).thenReturn(message);
        return idempotent;
    }

    // --- 测试用例 ---

    @Test
    @DisplayName("首次调用正常执行，结果被缓存")
    void should_execute_normally_on_first_call() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"order123"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint, idempotent);

        assertThat(result).isEqualTo("success");
        verify(cacheComponent).put(eq("TestService.process(order123)"), eq("1"), any());
    }

    @Test
    @DisplayName("幂等窗口内相同 Key 重复调用 → 抛 BizException")
    void should_throw_biz_exception_on_duplicate_call() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"order123"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        when(joinPoint.proceed()).thenReturn("success");

        // 首次调用：hasKey 返回 false
        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(false);
        aspect.around(joinPoint, idempotent);

        // 重复调用：hasKey 返回 true → 抛 BizException
        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(true);
        assertThatThrownBy(() -> aspect.around(joinPoint, idempotent))
                .isInstanceOf(BizException.class)
                .hasMessage("请勿重复操作");
    }

    @Test
    @DisplayName("幂等窗口过期后可正常调用（hasKey 返回 false）")
    void should_allow_call_after_window_expired() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"order123"}
        );
        Idempotent idempotent = mockIdempotent(100, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        when(joinPoint.proceed()).thenReturn("success");

        // 首次调用
        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(false);
        aspect.around(joinPoint, idempotent);

        // 过期后 hasKey 返回 false → 允许再次调用
        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(false);
        Object result = aspect.around(joinPoint, idempotent);
        assertThat(result).isEqualTo("success");

        // put 应被调用两次（首次 + 过期后再调用）
        verify(cacheComponent, times(2)).put(eq("TestService.process(order123)"), eq("1"), any());
    }

    @Test
    @DisplayName("field SpEL 表达式：#request.orderId → 从第一个参数的 orderId 字段提取 Key")
    void should_resolve_spel_field_expression() throws Throwable {
        OrderRequest request = new OrderRequest("ORD-001");

        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "createOrder",
                new String[] {"request"}, new Object[] {request}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#request.orderId", "订单已提交");

        when(cacheComponent.hasKey("TestService.createOrder(ORD-001)")).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("created");

        Object result = aspect.around(joinPoint, idempotent);

        assertThat(result).isEqualTo("created");
        verify(cacheComponent).put(eq("TestService.createOrder(ORD-001)"), eq("1"), any());
    }

    @Test
    @DisplayName("field 为空时使用 className.methodName(paramsHash) 作为 Key")
    void should_use_default_key_when_field_empty() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"id", "name"}, new Object[] {42, "test"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "", "请勿重复操作");

        when(cacheComponent.hasKey(anyString())).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("done");

        Object result = aspect.around(joinPoint, idempotent);

        assertThat(result).isEqualTo("done");
        // 验证 put 被调用（参数匹配 className.methodName(hashCode) 格式）
        verify(cacheComponent).put(anyString(), eq("1"), any());
    }

    @Test
    @DisplayName("不同参数值的调用互不影响")
    void should_not_affect_different_params() throws Throwable {
        ProceedingJoinPoint joinPoint1 = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"orderA"}
        );
        ProceedingJoinPoint joinPoint2 = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"orderB"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        when(joinPoint1.proceed()).thenReturn("resultA");
        when(joinPoint2.proceed()).thenReturn("resultB");

        // orderA 首次调用
        when(cacheComponent.hasKey("TestService.process(orderA)")).thenReturn(false);
        Object result1 = aspect.around(joinPoint1, idempotent);
        assertThat(result1).isEqualTo("resultA");

        // orderB 首次调用（不同 Key，应正常执行）
        when(cacheComponent.hasKey("TestService.process(orderB)")).thenReturn(false);
        Object result2 = aspect.around(joinPoint2, idempotent);
        assertThat(result2).isEqualTo("resultB");

        // orderA 重复调用应抛异常
        when(cacheComponent.hasKey("TestService.process(orderA)")).thenReturn(true);
        assertThatThrownBy(() -> aspect.around(joinPoint1, idempotent))
                .isInstanceOf(BizException.class);

        // orderB 重复调用应抛异常
        when(cacheComponent.hasKey("TestService.process(orderB)")).thenReturn(true);
        assertThatThrownBy(() -> aspect.around(joinPoint2, idempotent))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("执行失败时移除缓存标记允许重试")
    void should_delete_key_on_exception() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"order123"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(false);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("mock failure"));

        assertThatThrownBy(() -> aspect.around(joinPoint, idempotent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("mock failure");

        // 验证 put 被调用，delete 也被调用（回滚标记）
        verify(cacheComponent).put(eq("TestService.process(order123)"), eq("1"), any());
        verify(cacheComponent).delete("TestService.process(order123)");
    }

    @Test
    @DisplayName("幂等拦截时不调用 proceed，不调用 put")
    void should_not_proceed_on_duplicate() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "TestService", "process",
                new String[] {"key"}, new Object[] {"order123"}
        );
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "请勿重复操作");

        // hasKey 返回 true → 直接抛异常，不执行 proceed，不调用 put
        when(cacheComponent.hasKey("TestService.process(order123)")).thenReturn(true);

        assertThatThrownBy(() -> aspect.around(joinPoint, idempotent))
                .isInstanceOf(BizException.class);

        verify(joinPoint, never()).proceed();
        verify(cacheComponent, never()).put(anyString(), any(), any());
    }

    // --- 测试用 Request 对象 ---

    record OrderRequest(String orderId) {

    }

}
