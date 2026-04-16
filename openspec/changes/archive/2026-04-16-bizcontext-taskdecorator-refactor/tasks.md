## Chunk 0: 前置准备

- [x] 0.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

---

## Chunk 1: 核心重命名 + API 简化（BizContext）

> 目标：将 `ScopedThreadContext` 重命名为 `BizContext`，移除 `Key.TRACE_ID` 死字段，简化 API 签名。此 Chunk 完成后代码可以编译通过但测试暂时失败（RED）。
> 
> 涉及 spec：`bizcontext-propagation` — BizContext API 契约、Holder 副本机制、便捷方法、文件位置、ContextFillFilter 使用、MyMetaObjectHandler 使用

### 1.1 [RED] 编写 BizContext 新 API 的失败测试

**文件**：`app/src/test/java/org/smm/archetype/shared/util/context/BizContextUTest.java`（新建）
**前置**：删除旧测试 `ScopedThreadContextUTest.java`

**测试场景覆盖**：

| 测试方法 | 覆盖场景 | spec 对应 |
|----------|----------|-----------|
| `should_readUserId_inRunWithContext` | 请求线程中 `BizContext.getUserId()` 返回传入值 | bizcontext-propagation → "请求线程中读写 userId" |
| `should_returnNull_whenNotBound` | 未绑定时 `BizContext.getUserId()` 返回 null | bizcontext-propagation → "未绑定时读取返回 null" |
| `should_syncToBaggage_onRequestThread` | 请求线程 `Key.USER_ID.set()` 自动同步 Baggage | bizcontext-propagation → "Key.set 在请求线程中自动同步 Baggage" |
| `should_notSyncToBaggage_onReplicaThread` | 子线程 `Key.USER_ID.set()` 不同步 Baggage | bizcontext-propagation → "Key.set 在子线程中不同步 Baggage" |
| `should_runWithContext_acceptOnlyUserId` | `runWithContext(action, userId)` 不需要 traceId | bizcontext-propagation → "runWithContext 只接收 userId" |
| `should_copyAsReplica_deepCopy` | `copyAsReplica()` 创建深拷贝，replica=true | bizcontext-propagation → "copyAsReplica 创建深拷贝" |
| `should_copyAsReplica_returnNull_whenNotBound` | 未绑定 `copyAsReplica()` 返回 null | bizcontext-propagation → "未绑定时 copyAsReplica 返回 null" |
| `should_getUserId_returnValue` | `getUserId()` 在有上下文时返回值 | bizcontext-propagation → "getUserId 在有上下文时返回值" |
| `should_getUserId_returnNull_whenNoContext` | `getUserId()` 在无上下文时返回 null | bizcontext-propagation → "getUserId 在无上下文时返回 null" |
| `should_notContainTraceId_key` | `Key` 枚举不包含 `TRACE_ID` | bizcontext-propagation → "BizContext API 契约" |

**关键实现细节**：
- Baggage 同步测试需要 OTel Span 环境，使用 `BizContext.runWithContext()` 建立上下文后，通过 `Baggage.current().getEntryValue("userId")` 断言
- replica 测试需要在 `BizContext.runWithContext()` 内部创建新线程，在新线程中调用 `copyAsReplica()` 并验证 `replica=true`

### 1.2 [GREEN] 创建 BizContext.java（最小可用实现）

**文件**：`app/src/main/java/org/smm/archetype/shared/util/context/BizContext.java`（新建）
**操作**：从 `ScopedThreadContext.java` 复制并重命名，执行以下修改：

| 修改项 | 修改前 | 修改后 |
|--------|--------|--------|
| 类名 | `ScopedThreadContext` | `BizContext` |
| `Key` 枚举 | `USER_ID` + `TRACE_ID` | 仅 `USER_ID`（删除 `TRACE_ID`） |
| `runWithContext(action, userId, traceId)` | 接受两个参数 | 删除此方法 |
| `runWithContext(action, EnumMap)` | 接受 EnumMap | 保留 |
| 新增 `runWithContext(action, userId)` | 不存在 | 接受单个 userId 参数 |
| `getTraceId()` | 存在 | 删除 |
| `buildBaggage()` | 遍历 map 跳过 `propagated=false` | 遍历 map，所有键都是 `propagated=true`（简化） |
| JavaDoc | 引用 `ScopedThreadContext` | 引用 `BizContext` |

**`runWithContext(Runnable action, String userId)` 实现参考**：
```java
public static void runWithContext(Runnable action, String userId) {
    EnumMap<Key, String> context = new EnumMap<>(Key.class);
    if (userId != null) context.put(Key.USER_ID, userId);
    runWithContext(action, context);
}
```

**`buildBaggage()` 简化后**：
```java
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
```

### 1.3 [GREEN] 更新 ContextFillFilter 使用 BizContext

**文件**：`app/src/main/java/org/smm/archetype/controller/global/ContextFillFilter.java`

| 修改项 | 修改前 | 修改后 |
|--------|--------|--------|
| import | `ScopedThreadContext` | `BizContext` |
| `runWithContext` 调用 | `ScopedThreadContext.runWithContext(() -> {...}, userId, traceId)` | `BizContext.runWithContext(() -> {...}, userId)` |
| `traceId` 局部变量 | 用于传给 `runWithContext` | 删除（仍用于 `response.setHeader`，但不再传给 BizContext） |

**修改后的 `doFilterInternal` 核心逻辑**：
```java
String traceId = Span.current().getSpanContext().getTraceId();
response.setHeader(TRACE_ID_HEADER, traceId);
String userId = resolveUserId();
BizContext.runWithContext(() -> {
    try {
        filterChain.doFilter(request, response);
    } catch (IOException | ServletException e) {
        throw new RuntimeException(e);
    }
}, userId);
```

### 1.4 [GREEN] 更新 MyMetaObjectHandler 使用 BizContext

**文件**：`app/src/main/java/org/smm/archetype/shared/util/dal/MyMetaObjectHandler.java`

| 修改项 | 修改前 | 修改后 |
|--------|--------|--------|
| import | `ScopedThreadContext` | `BizContext` |
| `insertFill` | `ScopedThreadContext.getUserId()` | `BizContext.getUserId()` |
| `updateFill` | `ScopedThreadContext.getUserId()` | `BizContext.getUserId()` |

### 1.5 删除 ScopedThreadContext.java

**操作**：删除 `app/src/main/java/org/smm/archetype/shared/util/context/ScopedThreadContext.java`

**验证**：确认 `BizContext.java` 存在且 `ScopedThreadContext.java` 不存在

### 1.6 更新 ContextFillFilterUTest

**文件**：`app/src/test/java/org/smm/archetype/controller/global/ContextFillFilterUTest.java`

| 修改项 | 说明 |
|--------|------|
| import | `ScopedThreadContext` → `BizContext` |
| 断言 | 移除对 `ScopedThreadContext.Key.TRACE_ID` 的引用 |
| `runWithContext` 调用 | 使用 `BizContext.runWithContext(action, userId)` |

### 1.7 更新 MyMetaObjectHandlerUTest

**文件**：`app/src/test/java/org/smm/archetype/shared/util/dal/MyMetaObjectHandlerUTest.java`（如存在）

| 修改项 | 说明 |
|--------|------|
| import | `ScopedThreadContext` → `BizContext` |
| `runWithContext` 调用 | 使用 `BizContext.runWithContext(action, userId)` |

### 1.8 [REFACTOR] 全局替换剩余 import

**操作**：在 `app/src/main/java` 和 `app/src/test/java` 中搜索所有 `ScopedThreadContext` 引用，替换为 `BizContext`。

**已知需要替换的文件**（基于 grep 结果）：
- 无其他文件（ContextRunnable/ContextCallable 在 Chunk 2 删除）

**验证**：`grep -r "ScopedThreadContext" app/src/` 返回零结果

### 1.9 [验证] Chunk 1 编译测试

**命令**：`mvn test-compile -pl app -q`
**期望**：编译成功，无错误

---

## Chunk 2: TaskDecorator 融合 + 删除 ContextRunnable/ContextCallable

> 目标：将三合一传播逻辑融合到 TaskDecorator，删除 ContextRunnable/ContextCallable，修复 schedulerThreadPool bug。
> 
> 涉及 spec：`taskdecorator-unified` — 三合一 TaskDecorator、删除 ContextRunnable/ContextCallable、ScheduledExecutorService 上下文传播、ThreadPoolConfigure 统一、TaskDecorator 集成测试

### 2.1 [RED] 编写 TaskDecorator 三合一传播的失败测试

**文件**：`app/src/test/java/org/smm/archetype/config/TaskDecoratorContextPropagationUTest.java`（新建）

**测试场景覆盖**：

| 测试方法 | 覆盖场景 | spec 对应 |
|----------|----------|-----------|
| `should_propagateBizContext_toWorkerThread` | BizContext 在工作线程可读 | taskdecorator-unified → "TaskDecorator 传播 BizContext" |
| `should_propagateOtelContext_toWorkerThread` | OTel Span traceId 在工作线程可读 | taskdecorator-unified → "TaskDecorator 传播 OTel Context" |
| `should_propagateMdc_toWorkerThread` | MDC traceId 在工作线程可读 | taskdecorator-unified → "TaskDecorator 传播 MDC" |
| `should_notPolluteBaggage_fromChildThread` | 子线程 Key.set 不污染请求线程 Baggage | taskdecorator-unified → "TaskDecorator 在子线程中不污染 Baggage" |
| `should_restoreMdc_afterTaskExecution` | 工作线程 MDC 在任务执行后恢复 | taskdecorator-unified → "MDC 执行后恢复" |

**关键实现细节**：
- 使用 `ThreadPoolTaskExecutor` 手动设置 `CONTEXT_PROPAGATING_DECORATOR`，提交任务并验证
- BizContext 传播测试：在 `BizContext.runWithContext()` 内提交任务，在工作线程中调用 `BizContext.getUserId()`
- OTel 传播测试：在 OTel Span 内提交任务，在工作线程中调用 `Span.current().getSpanContext().getTraceId()`
- MDC 传播测试：先 `MDC.put("traceId", "test")`，提交任务，在工作线程中验证 `MDC.get("traceId")`
- Baggage 隔离测试：在 `BizContext.runWithContext()` 内提交任务，在工作线程中 `Key.USER_ID.set("child")`，验证主线程 Baggage 不变
- 所有异步测试使用 `CountDownLatch` + `AtomicReference` 收集结果，确保线程安全

### 2.2 [GREEN] 实现三合一 TaskDecorator

**文件**：`app/src/main/java/org/smm/archetype/config/ThreadPoolConfigure.java`

**修改内容**：

| 修改项 | 修改前 | 修改后 |
|--------|--------|--------|
| 常量名 | `OTEL_CONTEXT_DECORATOR` | `CONTEXT_PROPAGATING_DECORATOR` |
| 常量实现 | `runnable -> Context.current().wrap(runnable)` | 三合一实现（见下方） |
| import | 无 MDC/ScopedValue import | +`org.slf4j.MDC` +`java.lang.ScopedValue` +`BizContext` |

**三合一 TaskDecorator 实现参考**：
```java
private static final TaskDecorator CONTEXT_PROPAGATING_DECORATOR = runnable -> {
    BizContext.Holder replica = BizContext.copyAsReplica();
    ScopedValue<BizContext.Holder> scoped = BizContext.getScoped();
    io.opentelemetry.context.Context otelContext = io.opentelemetry.context.Context.current();
    java.util.Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    return () -> {
        java.util.Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            Runnable task = runnable;
            if (replica != null) {
                task = () -> ScopedValue.where(scoped, replica).run(runnable);
            }
            otelContext.wrap(task).run();
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    };
};
```

**所有三个 ThreadPoolTaskExecutor Bean（`ioThreadPool`、`cpuThreadPool`、`daemonThreadPool`）**：
- 将 `executor.setTaskDecorator(OTEL_CONTEXT_DECORATOR)` 改为 `executor.setTaskDecorator(CONTEXT_PROPAGATING_DECORATOR)`

### 2.3 [GREEN] 实现 ContextPropagatingScheduledExecutorService

**文件**：`app/src/main/java/org/smm/archetype/config/ThreadPoolConfigure.java`（内部类或私有静态类）

**实现参考**：
```java
/**
 * ScheduledExecutorService 装饰器：在每次任务提交时传播三种上下文。
 * 覆盖 execute/submit/schedule/scheduleAtFixedRate/scheduleWithFixedDelay。
 */
private static class ContextPropagatingScheduledExecutorService 
        implements ScheduledExecutorService {
    
    private final ScheduledExecutorService delegate;

    ContextPropagatingScheduledExecutorService(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    // 所有任务提交方法：使用 CONTEXT_PROPAGATING_DECORATOR 装饰
    @Override public void execute(Runnable command) {
        delegate.execute(CONTEXT_PROPAGATING_DECORATOR.decorate(command));
    }
    @Override public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapCallable(task));
    }
    @Override public Future<?> submit(Runnable task) {
        return delegate.submit(CONTEXT_PROPAGATING_DECORATOR.decorate(task));
    }
    @Override public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(CONTEXT_PROPAGATING_DECORATOR.decorate(task), result);
    }
    @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(CONTEXT_PROPAGATING_DECORATOR.decorate(command), delay, unit);
    }
    @Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate.schedule(wrapCallable(callable), delay, unit);
    }
    @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(CONTEXT_PROPAGATING_DECORATOR.decorate(command), initialDelay, period, unit);
    }
    @Override public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(CONTEXT_PROPAGATING_DECORATOR.decorate(command), initialDelay, delay, unit);
    }

    // Callable 包装：捕获上下文后包装为 Runnable 再执行
    private <T> Callable<T> wrapCallable(Callable<T> callable) {
        BizContext.Holder replica = BizContext.copyAsReplica();
        ScopedValue<BizContext.Holder> scoped = BizContext.getScoped();
        io.opentelemetry.context.Context otelContext = io.opentelemetry.context.Context.current();
        java.util.Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            java.util.Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            if (mdcContext != null) MDC.setContextMap(mdcContext);
            try {
                Callable<T> task = callable;
                if (replica != null) {
                    final T[] result = (T[]) new Object[1];
                    final Exception[] ex = new Exception[1];
                    ScopedValue.where(scoped, replica).run(() -> {
                        try { result[0] = callable.call(); }
                        catch (Exception e) { ex[0] = e; }
                    });
                    if (ex[0] != null) throw ex[0];
                    return result[0];
                }
                return otelContext.wrap(callable).call();
            } finally {
                if (previousMdc != null) MDC.setContextMap(previousMdc);
                else MDC.clear();
            }
        };
    }

    // 生命周期方法：直接委托，不传播上下文
    @Override public void shutdown() { delegate.shutdown(); }
    @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
    @Override public boolean isShutdown() { return delegate.isShutdown(); }
    @Override public boolean isTerminated() { return delegate.isTerminated(); }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return delegate.awaitTermination(timeout, unit); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException { return delegate.invokeAll(tasks); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException { return delegate.invokeAll(tasks, timeout, unit); }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException { return delegate.invokeAny(tasks); }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException { return delegate.invokeAny(tasks, timeout, unit); }
}
```

**修改 `schedulerThreadPool` Bean**：
```java
@Bean("schedulerThreadPool")
public ScheduledExecutorService schedulerThreadPool(ThreadPoolProperties props) {
    ScheduledExecutorService raw = Executors.newScheduledThreadPool(
            props.getSchedulerPoolSize(),
            r -> {
                Thread t = new Thread(r, "scheduler-pool-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
    return new ContextPropagatingScheduledExecutorService(raw);
}
```

### 2.4 [RED] 编写 ScheduledExecutorService 传播失败测试

**文件**：`app/src/test/java/org/smm/archetype/config/ScheduledExecutorContextPropagationUTest.java`（新建）

| 测试方法 | 覆盖场景 | spec 对应 |
|----------|----------|-----------|
| `should_propagateContext_viaSchedule` | schedule 传播上下文 | taskdecorator-unified → "schedule 传播上下文" |
| `should_propagateContext_viaScheduleAtFixedRate` | scheduleAtFixedRate 传播上下文 | taskdecorator-unified → "scheduleAtFixedRate 传播上下文" |

### 2.5 删除 ContextRunnable.java 和 ContextCallable.java

**删除文件**：
- `app/src/main/java/org/smm/archetype/shared/util/context/ContextRunnable.java`
- `app/src/main/java/org/smm/archetype/shared/util/context/ContextCallable.java`

### 2.6 删除对应的测试文件

**删除文件**：
- `app/src/test/java/org/smm/archetype/shared/util/context/ContextRunnableUTest.java`
- `app/src/test/java/org/smm/archetype/shared/util/context/ContextCallableUTest.java`

### 2.7 全局替换剩余引用

**操作**：在 `app/src/` 中搜索所有 `ContextRunnable`、`ContextCallable`、`OTEL_CONTEXT_DECORATOR` 引用，全部替换。

**验证**：
- `grep -r "ContextRunnable" app/src/` 返回零结果
- `grep -r "ContextCallable" app/src/` 返回零结果
- `grep -r "OTEL_CONTEXT_DECORATOR" app/src/` 返回零结果

### 2.8 [验证] Chunk 2 单元测试

**命令**：`mvn test -pl app -Dtest="*UTest" -q`
**期望**：所有单元测试通过（含新的 BizContextUTest、TaskDecoratorContextPropagationUTest、ScheduledExecutorContextPropagationUTest）

---

## Chunk 3: 全量集成验证

> 目标：确保全部测试通过，包括集成测试。验证端到端的三合一上下文传播。

### 3.1 [RED] 创建 TaskDecorator 集成测试

**文件**：`app/src/test/java/org/smm/archetype/cases/integrationtest/TaskDecoratorContextPropagationITest.java`（新建）
**继承**：`IntegrationTestBase`（`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("test")`）

| 测试方法 | 覆盖场景 | spec 对应 |
|----------|----------|-----------|
| `should_propagateAllContexts_toAsyncTask` | HTTP 请求触发异步任务，验证三种上下文 | taskdecorator-unified → "端到端验证异步上下文" |

**实现细节**：
- 注入 `@Qualifier("ioThreadPool") Executor ioThreadPool`
- 在测试 Controller 中添加一个端点（或利用现有端点），在 Controller 内部提交异步任务
- 或直接在测试中模拟：先通过 `BizContext.runWithContext()` 绑定上下文，再通过 `ioThreadPool` 提交任务，在任务中验证三种上下文

**注意**：如果现有 TestController 不支持异步操作，需要添加一个测试端点 `/api/test/async`，在端点内通过 `ioThreadPool` 提交任务并返回结果。

### 3.2 [验证] 全量测试

**命令**：`mvn clean test -pl app`
**期望**：所有测试通过（488+ 个测试 + 新增测试，零失败零错误）

### 3.3 [验证] 全量验证（含覆盖率）

**命令**：`mvn clean verify -pl app`
**期望**：BUILD SUCCESS

---

## [x] 进行 artifact 文档、讨论结果的一致性检查
