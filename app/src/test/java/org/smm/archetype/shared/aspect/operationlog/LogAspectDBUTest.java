package org.smm.archetype.shared.aspect.operationlog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogAspect DB 写入（异步持久化）")
class LogAspectDBUTest {

    @Mock
    private OperationLogWriter operationLogWriter;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private LogAspect logAspect;

    private void initLogAspect() {
        logAspect = new LogAspect(operationLogWriter);
    }

    private void mockJoinPoint(String methodName, Class<?>[] paramTypes, Object[] args, Object result) throws Throwable {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(
                SampleMethods.class.getDeclaredMethod(methodName, paramTypes));
        when(signature.getDeclaringType()).thenReturn(SampleMethods.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(result);
    }

    private void mockJoinPointException(String methodName, Class<?>[] paramTypes, Object[] args, Throwable throwable) throws Throwable {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(
                SampleMethods.class.getDeclaredMethod(methodName, paramTypes));
        when(signature.getDeclaringType()).thenReturn(SampleMethods.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(throwable);
    }

    @Test
    @DisplayName("方法成功执行后，OperationLogWriter.write() 被调用，status=SUCCESS")
    void should_write_success_log_on_method_success() throws Throwable {
        initLogAspect();
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {"input"}, "ok");

        logAspect.doAround(joinPoint);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.status()).isEqualTo("SUCCESS");
        assertThat(record.errorMessage()).isEmpty();
        assertThat(record.method()).contains("dummySuccessMethod");
    }

    @Test
    @DisplayName("方法失败后，OperationLogWriter.write() 被调用，status=ERROR")
    void should_write_error_log_on_method_failure() throws Throwable {
        initLogAspect();
        mockJoinPointException("dummyFailMethod", new Class[] {}, new Object[] {},
                new RuntimeException("test error"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> logAspect.doAround(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test error");

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.status()).isEqualTo("ERROR");
        assertThat(record.errorMessage()).contains("test error");
    }

    @Test
    @DisplayName("samplingRate=1.0 时每次都写入")
    void should_always_write_when_sampling_rate_1() throws Throwable {
        initLogAspect();
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {"input"}, "ok");

        // 调用 10 次，每次都应该写入
        for (int i = 0; i < 10; i++) {
            logAspect.doAround(joinPoint);
        }

        verify(operationLogWriter, times(10)).write(any(OperationLogRecord.class));
    }

    @Test
    @DisplayName("samplingRate=0.0 时从不写入")
    void should_never_write_when_sampling_rate_0() throws Throwable {
        initLogAspect();
        mockJoinPoint("zeroSamplingMethod", new Class[] {}, new Object[] {}, "ok");

        // 调用 10 次，由于 samplingRate=0.0，不应写入
        for (int i = 0; i < 10; i++) {
            logAspect.doAround(joinPoint);
        }

        verify(operationLogWriter, never()).write(any(OperationLogRecord.class));
    }

    @Test
    @DisplayName("写入的 record 包含正确的 description 和 operationType")
    void should_record_contain_correct_description_and_operation_type() throws Throwable {
        initLogAspect();
        mockJoinPoint("annotatedMethod", new Class[] {}, new Object[] {}, "ok");

        logAspect.doAround(joinPoint);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.description()).isEqualTo("测试描述");
        assertThat(record.module()).isEqualTo("测试模块");
        assertThat(record.operationType()).isEqualTo(OperationType.CREATE.code());
    }

    @Test
    @DisplayName("异常 getMessage() 为 null 时使用类名作为 errorMessage")
    void should_use_class_name_when_exception_message_is_null() throws Throwable {
        initLogAspect();
        mockJoinPointException("nullMessageMethod", new Class[] {}, new Object[] {},
                new RuntimeException((String) null));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> logAspect.doAround(joinPoint))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.status()).isEqualTo("ERROR");
        assertThat(record.errorMessage()).isEqualTo("RuntimeException");
    }

    @Test
    @DisplayName("OperationLogWriter.write() 抛异常时不传播，正常返回")
    void should_not_propagate_when_writer_throws() throws Throwable {
        initLogAspect();
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {"input"}, "ok");
        doThrow(new RuntimeException("writer error")).when(operationLogWriter).write(any());

        // doAround 不应抛出异常
        Object result = logAspect.doAround(joinPoint);
        assertThat(result).isEqualTo("ok");

        verify(operationLogWriter, times(1)).write(any(OperationLogRecord.class));
    }

    @Test
    @DisplayName("无参构造（无 OperationLogWriter）时不抛 NPE")
    void should_not_throw_npe_with_no_arg_constructor() throws Throwable {
        logAspect = new LogAspect(); // 无 OperationLogWriter
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {"input"}, "ok");

        Object result = logAspect.doAround(joinPoint);

        assertThat(result).isEqualTo("ok");
        // 无 writer，不应调用 write
        verifyNoInteractions(operationLogWriter);
    }

    @Test
    @DisplayName("null 参数数组时 toSafeJson 不崩溃")
    void should_not_crash_with_null_args() throws Throwable {
        initLogAspect();
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, null, "ok");

        Object result = logAspect.doAround(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(operationLogWriter, times(1)).write(any(OperationLogRecord.class));
    }

    @Test
    @DisplayName("超大字符串参数应被截断并追加 ...(truncated)")
    void should_truncate_large_string_args() throws Throwable {
        initLogAspect();
        String largeString = "x".repeat(4096);
        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {largeString}, "ok");

        logAspect.doAround(joinPoint);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.params()).endsWith("...(truncated)");
        assertThat(record.params().length()).isLessThanOrEqualTo(2048);
    }

    @Test
    @DisplayName("循环引用对象序列化失败时应降级为 ClassName@hashCode")
    void should_fallback_to_classname_hashcode_for_non_serializable() throws Throwable {
        initLogAspect();
        // 创建循环引用的 HashMap，fastjson2 默认检测到循环引用会抛异常
        java.util.Map<String, Object> circular = new java.util.HashMap<>();
        circular.put("self", circular);

        mockJoinPoint("dummySuccessMethod", new Class[] {String.class}, new Object[] {circular}, "ok");

        logAspect.doAround(joinPoint);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogWriter, times(1)).write(captor.capture());
        OperationLogRecord record = captor.getValue();
        // fastjson2 序列化循环引用时抛异常，降级为 ClassName@hashCode 格式
        assertThat(record.params()).contains("@");
    }

    @Test
    @DisplayName("OperationLogWriter.write() 在错误路径也捕获异常")
    void should_not_propagate_when_writer_throws_on_error_path() throws Throwable {
        initLogAspect();
        mockJoinPointException("dummyFailMethod", new Class[] {}, new Object[] {},
                new RuntimeException("biz error"));
        doThrow(new RuntimeException("writer error")).when(operationLogWriter).write(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> logAspect.doAround(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("biz error");

        // writer 被调用但不传播 writer 的异常
        verify(operationLogWriter, times(1)).write(any(OperationLogRecord.class));
    }

    /**
     * 辅助类：持有 @BusinessLog 标注的方法，供 MethodSignature mock 反射获取。
     */
    @SuppressWarnings("unused")
    static class SampleMethods {

        @BusinessLog("旧版")
        String dummySuccessMethod(String input) {
            return "ok";
        }

        @BusinessLog("旧版")
        void dummyFailMethod() {}

        @BusinessLog(value = "零采样", samplingRate = 0.0)
        String zeroSamplingMethod() {
            return "ok";
        }

        @BusinessLog(value = "测试描述", module = "测试模块", operation = OperationType.CREATE)
        String annotatedMethod() {
            return "ok";
        }

        @BusinessLog("test")
        void nullMessageMethod() {}

    }

}
