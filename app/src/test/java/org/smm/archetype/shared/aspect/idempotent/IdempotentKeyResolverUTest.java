package org.smm.archetype.shared.aspect.idempotent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotentKeyResolver Key 生成器")
class IdempotentKeyResolverUTest {

    private IdempotentKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new IdempotentKeyResolver();
    }

    @Test
    @DisplayName("field 非空时：使用 SpEL 表达式解析 Key")
    void should_resolve_spel_field() {
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#request.orderId", "msg");
        String[] paramNames = {"request"};
        Object[] args = {new TestRequest("ORD-001")};

        String key = resolver.resolve("TestService", "createOrder", paramNames, args, idempotent);

        assertThat(key).isEqualTo("TestService.createOrder(ORD-001)");
    }

    @Test
    @DisplayName("field 为空时：使用 className.methodName(paramsHash) 作为 Key")
    void should_use_default_hash_key_when_field_empty() {
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "", "msg");
        String[] paramNames = {"id", "name"};
        Object[] args = {42, "test"};

        String key = resolver.resolve("TestService", "process", paramNames, args, idempotent);

        assertThat(key).startsWith("TestService.process(");
        // 不同参数应产生不同的 hash
        Object[] otherArgs = {99, "other"};
        String otherKey = resolver.resolve("TestService", "process", paramNames, otherArgs, idempotent);
        assertThat(key).isNotEqualTo(otherKey);
    }

    @Test
    @DisplayName("支持 #p0 语法按索引引用参数")
    void should_support_p0_index_syntax() {
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#p0", "msg");
        String[] paramNames = {"orderId"};
        Object[] args = {"ORDER-123"};

        String key = resolver.resolve("TestService", "submit", paramNames, args, idempotent);

        assertThat(key).isEqualTo("TestService.submit(ORDER-123)");
    }

    @Test
    @DisplayName("相同参数应生成相同的 Key")
    void should_generate_same_key_for_same_params() {
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "", "msg");
        String[] paramNames = {"id"};
        Object[] args = {42};

        String key1 = resolver.resolve("Svc", "m", paramNames, args, idempotent);
        String key2 = resolver.resolve("Svc", "m", paramNames, args, idempotent);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("不同类名/方法名应生成不同的 Key")
    void should_generate_different_key_for_different_class_or_method() {
        Idempotent idempotent = mockIdempotent(3000, TimeUnit.MILLISECONDS, "#key", "msg");
        String[] paramNames = {"key"};
        Object[] args = {"sameValue"};

        String key1 = resolver.resolve("ServiceA", "method", paramNames, args, idempotent);
        String key2 = resolver.resolve("ServiceB", "method", paramNames, args, idempotent);

        assertThat(key1).isNotEqualTo(key2);
    }

    private Idempotent mockIdempotent(long timeout, TimeUnit timeUnit, String field, String message) {
        Idempotent idempotent = org.mockito.Mockito.mock(Idempotent.class);
        org.mockito.Mockito.when(idempotent.timeout()).thenReturn(timeout);
        org.mockito.Mockito.when(idempotent.timeUnit()).thenReturn(timeUnit);
        org.mockito.Mockito.when(idempotent.field()).thenReturn(field);
        org.mockito.Mockito.when(idempotent.message()).thenReturn(message);
        return idempotent;
    }

    record TestRequest(String orderId) {

    }

}
