# 阶段 1 Spec：基础设施层

> 关联总纲：`docs/architecture/refactoring-plan.md` → 阶段 1
> 关联 Plan：`docs/superpowers/plans/2026-05-12-phase1-infrastructure.md`
> 创建日期：2026-05-12
> 状态：待实施

---

## 概览

阶段 1 创建新架构的共享类型基础，**不触碰现有业务代码**。所有现有功能继续正常工作。

| 操作类型 | 数量 | 说明 |
|---------|------|------|
| 新增文件 | 2 | PageQuery、PageResult |
| 迁移文件 | 6 | BaseResult、BasePageResult + 4 个操作日志接口 |
| 废弃标记 | 8 | BaseRequest、BasePageRequest + 旧位置 BaseResult/BasePageResult + 4 个旧位置 operationlog 文件 |
| 修改文件 | 1 | common/pom.xml |

---

## 一、新增文件

### 1.1 PageQuery.java

**位置**：`app/src/main/java/org/smm/archetype/shared/pagination/PageQuery.java`

```java
package org.smm.archetype.shared.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求值对象（1-based，前端友好）。
 * <p>
 * 各模块的 *PageQuery record 通过紧凑构造器复用此类的校验逻辑。
 *
 * @since 2026-05-12
 */
public record PageQuery(
        @Min(1) int pageNo,
        @Min(1) @Max(100) int pageSize
) {
    public PageQuery {
        if (pageNo <= 0) pageNo = 1;
        if (pageSize <= 0) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
    }
}
```

**验证**：
- 编译零报错
- `pageNo=0` → 自动修正为 `1`
- `pageSize=0` → 自动修正为 `10`
- `pageSize=200` → 自动修正为 `100`

---

### 1.2 PageResult.java

**位置**：`app/src/main/java/org/smm/archetype/shared/pagination/PageResult.java`

```java
package org.smm.archetype.shared.pagination;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果值对象（框架无关，泛型）。
 * <p>
 * 替代 MyBatis-Plus 的 {@code IPage<T>} 作为 Repository 接口的返回类型，
 * 实现业务层与 ORM 框架的解耦。仅在 RepositoryImpl 内部发生 IPage ↔ PageResult 转换。
 *
 * @param <T> 结果项类型
 * @since 2026-05-12
 */
public record PageResult<T>(
        List<T> list,
        long total,
        int pageNo,
        int pageSize,
        int totalPages
) {
    /**
     * 静态工厂：从分页数据创建 PageResult。
     *
     * @param list     数据列表
     * @param total    总记录数
     * @param pageNo   当前页（1-based）
     * @param pageSize 每页大小
     * @param <T>      结果项类型
     * @return PageResult 实例
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, got: " + pageSize);
        }
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(list, total, pageNo, pageSize, totalPages);
    }

    /**
     * 空结果快捷方法。
     */
    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize, 0);
    }
}
```

**验证**：
- 编译零报错
- `PageResult.of(list, 100, 1, 20)` → totalPages=5 ✅
- `PageResult.of(list, 0, 1, 20)` → totalPages=0 ✅

---

## 二、迁移文件（位置变更 + 内容调整）

### 2.1 BaseResult.java

**旧位置**：`app/src/main/java/org/smm/archetype/entity/base/BaseResult.java`
**新位置**：`app/src/main/java/org/smm/archetype/shared/result/BaseResult.java`

**变更**：仅修改 package 声明，内容不变。

```java
// 旧
package org.smm.archetype.entity.base;

// 新
package org.smm.archetype.shared.result;
```

**旧文件处理**：保留旧文件，顶部添加 `@Deprecated` 注解和迁移说明注释：

```java
/**
 * @deprecated 已迁移至 {@code org.smm.archetype.shared.result.BaseResult}，将在阶段 4 删除。
 */
@Deprecated
@Getter
@Setter
public class BaseResult<T> {
    // ... 内容不变
}
```

**验证**：
- 新位置编译通过
- 旧位置编译通过（`@Deprecated` 不影响编译）
- 所有现有测试的 import 更新为 `org.smm.archetype.shared.result.BaseResult`

---

### 2.2 BasePageResult.java（重写）

**旧位置**：`app/src/main/java/org/smm/archetype/entity/base/BasePageResult.java`
**新位置**：`app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java`

**核心变更**：公开 API 不再直接使用 `IPage`，废弃 `fromPage(IPage)`，新增 `from(PageResult)`。仅 `@Deprecated fromPage(IPage)` 方法签名保留 `IPage` import 以维持向后兼容，阶段 4 删除该方法后彻底移除。

```java
package org.smm.archetype.shared.result;

import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import lombok.Setter;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.pagination.PageResult;

import java.time.Instant;
import java.util.List;

/**
 * 分页结果包装（继承 BaseResult&lt;List&lt;T&gt;&gt;）。
 * <p>
 * 已去除 MyBatis-Plus {@code IPage} 依赖，仅通过 {@code from(PageResult)} 静态工厂构建。
 *
 * @param <T> 结果项类型
 * @since 2025/7/14
 */
@Getter
@Setter
public class BasePageResult<T> extends BaseResult<List<T>> {

    private long total;
    private int pageNo;
    private int pageSize;

    /**
     * 从 PageResult 构建 BasePageResult（框架无关）。
     *
     * @param pageResult PageResult 实例
     * @param <T>        结果项类型
     * @return BasePageResult 实例
     */
    public static <T> BasePageResult<T> from(PageResult<T> pageResult) {
        if (pageResult == null) {
            throw new IllegalArgumentException("pageResult must not be null");
        }
        BasePageResult<T> result = new BasePageResult<>();
        result.setData(pageResult.list());
        result.setTotal(pageResult.total());
        result.setPageNo(pageResult.pageNo());
        result.setPageSize(pageResult.pageSize());
        result.setCode(CommonErrorCode.SUCCESS.code());
        result.setMessage(CommonErrorCode.SUCCESS.message());
        result.setSuccess(true);
        result.setTime(Instant.now());
        result.setTraceId(Span.current().getSpanContext().getTraceId());
        return result;
    }

    /**
     * @deprecated 已废弃，请使用 {@link #from(PageResult)}。
     *             将在阶段 4 删除。当前保留以兼容阶段 1 未迁移的调用方。
     */
    @Deprecated
    public static <T> BasePageResult<T> fromPage(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return from(PageResult.of(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        ));
    }
}
```

**旧文件处理**：保留旧文件，顶部添加 `@Deprecated` 注解和迁移说明注释。

**边界行为说明**：
- `Span.current()` 在非 OTel 上下文（如单元测试）中返回 `InvalidSpan`，`getTraceId()` 返回全零字符串 `"00000000000000000000000000000000"`，不影响业务逻辑。
- `from(PageResult)` 要求 `pageResult` 非 null，传入 null 将抛出 `IllegalArgumentException`。

**验证**：
- 新位置编译通过
- `import com.baomidou.mybatisplus.core.metadata.IPage` 不在新文件中
- `from(PageResult<T>)` 静态工厂可用
- 旧 `fromPage(IPage)` 方法标记为 `@Deprecated` 但保留，向下兼容

---

### 2.3 操作日志接口层迁移（4 个文件）

**迁移规则**：仅修改 package 声明，内容完全不变。旧文件标记 `@Deprecated`。

| # | 文件 | 旧位置 | 新位置 |
|---|------|--------|--------|
| 1 | OperationType.java | `org.smm.archetype.shared.aspect.operationlog` | `org.smm.archetype.operationlog` |
| 2 | OperationLogRecord.java | `org.smm.archetype.shared.aspect.operationlog` | `org.smm.archetype.operationlog` |
| 3 | OperationLogWriter.java | `org.smm.archetype.shared.aspect.operationlog` | `org.smm.archetype.operationlog` |
| 4 | BusinessLog.java | `org.smm.archetype.shared.aspect.operationlog` | `org.smm.archetype.operationlog` |

**新位置完整路径**：
- `common/src/main/java/org/smm/archetype/operationlog/OperationType.java`
- `common/src/main/java/org/smm/archetype/operationlog/OperationLogRecord.java`
- `common/src/main/java/org/smm/archetype/operationlog/OperationLogWriter.java`
- `common/src/main/java/org/smm/archetype/operationlog/BusinessLog.java`

**旧文件处理**：每个旧文件添加 `@Deprecated` 注解 + 迁移说明注释。enum 和 record 无法添加类注解，在 javadoc 中标注。

```java
// OperationType.java (旧位置)
/**
 * @deprecated 已迁移至 {@code org.smm.archetype.operationlog.OperationType}，将在阶段 4 删除。
 * @see org.smm.archetype.operationlog.OperationType
 */
@Deprecated
public enum OperationType { ... }
```

**验证**：
- common 模块新文件编译通过（零 Spring 依赖，零新依赖引入）
- app 模块旧文件编译通过（`@Deprecated` 不影响编译）
- common 模块 `pom.xml` 移除 `spring-boot-starter` 后仍可编译

---

## 三、废弃标记（仅标记，不删除）

### 3.1 BaseRequest.java

**位置**：`app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java`

**操作**：添加 `@Deprecated` 注解 + 说明注释。内容不变。

```java
/**
 * @deprecated traceId 由 OTel Span 自动管理，requestId 由 Filter 生成。
 *             请求 record 只需包含业务字段，不再需要此基类。将在阶段 4 删除。
 */
@Deprecated
@Getter
@Setter
public class BaseRequest {
    // ... 内容不变
}
```

### 3.2 BasePageRequest.java

**位置**：`app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java`

**操作**：添加 `@Deprecated` 注解 + 说明注释。内容不变。

```java
/**
 * @deprecated 被 {@code org.smm.archetype.shared.pagination.PageQuery} record 替代。
 *             record 无法继承 class，设计为独立 record + 紧凑构造器模式。将在阶段 4 删除。
 * @see org.smm.archetype.shared.pagination.PageQuery
 */
@Deprecated
@Getter
@Setter
public class BasePageRequest extends BaseRequest {
    // ... 内容不变
}
```

**验证**：
- 编译通过（`@Deprecated` 不影响编译）
- `javac -Xlint:deprecation` 编译时，使用 BaseRequest/BasePageRequest 的代码会输出 deprecation 警告
- 无现有业务代码引用 `BaseRequest`/`BasePageRequest`（已验证：没有 Request record 继承它们）

> **注意**：Phase 4 已直接删除这些废弃文件（跳过了 @Deprecated 过渡期），因为全项目无代码依赖它们。

---

## 四、修改文件

### 4.1 common/pom.xml

**变更**：移除 `spring-boot-starter` 依赖（异常类零 Spring API 使用），仅保留 `lombok`。

```xml
<!-- 移除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

```xml
<!-- 保留 -->
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**验证**：
- `mvn clean compile -pl common` 通过
- `mvn test -pl common` 通过（现有异常体系测试）
- ArchUnit M-01 规则通过（exception 包零 Spring 依赖）

---

## 五、测试同步更新

### 5.1 需更新 import 的测试文件

以下测试文件引用了迁移的类，需要在后续阶段更新 import（阶段 1 暂不更新，旧文件保留 @Deprecated 可编译）：

| 测试文件 | 迁移类型 | 旧 import | 新 import（阶段 2+ 执行） |
|---------|---------|-----------|-----------|
| `BaseResult` 的测试（如有） | BaseResult | `org.smm.archetype.entity.base.BaseResult` | `org.smm.archetype.shared.result.BaseResult` |
| `BasePageResult` 的测试（如有） | BasePageResult | `org.smm.archetype.entity.base.*` | `org.smm.archetype.shared.result.*` |

### 5.2 新增测试

| 新增测试 | 测试内容 |
|---------|---------|
| `shared/pagination/PageQueryUTest.java` | 校验边界值处理（0/负数/超大值） |
| `shared/pagination/PageResultUTest.java` | 验证静态工厂 `of()` 计算 totalPages |
| `shared/result/BasePageResultUTest.java` | 验证 `from(PageResult)` 转换正确性 |

---

## 六、验证清单

### 6.1 编译验证

```bash
mvn clean compile          # 全项目编译通过，零报错
mvn clean compile -pl common  # common 模块独立编译通过
```

### 6.2 测试验证

```bash
mvn test                   # 所有现有测试通过
mvn test -pl common        # common 模块测试通过
```

### 6.3 架构规则验证

```bash
mvn test -Dtest="*ComplianceUTest" -pl app   # ArchUnit 15+ 条规则全部通过
```

### 6.4 专项验证

| 验证项 | 验证方式 | 预期结果 |
|--------|---------|---------|
| BasePageResult IPage import 仅限 @Deprecated 方法 | `grep "import com.baomidou" app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java` | 1 match（仅 @Deprecated fromPage 方法签名） |
| common 零 Spring 依赖 | `mvn dependency:tree -pl common \| grep spring-boot-starter` | 仅 test scope 有 spring-boot-starter-test |
| 操作日志 4 文件在 common | `ls common/src/main/java/org/smm/archetype/operationlog/` | 4 个文件 |
| 废弃类编译通过 | `mvn compile` | 零 @Deprecated 导致的编译错误 |
| PageQuery 校验 | 单元测试 | pageNo=0→1, pageSize=200→100 |

---

## 七、文件清单汇总

| # | 操作 | 文件 | 类型 |
|---|------|------|------|
| 1 | 新增 | `app/.../shared/pagination/PageQuery.java` | record |
| 2 | 新增 | `app/.../shared/pagination/PageResult.java` | record |
| 3 | 迁移 | `app/.../entity/base/BaseResult.java` → `app/.../shared/result/BaseResult.java` | class |
| 4 | 迁移+重写 | `app/.../entity/base/BasePageResult.java` → `app/.../shared/result/BasePageResult.java` | class |
| 5 | 迁移 | `app/.../shared/aspect/operationlog/OperationType.java` → `common/.../operationlog/OperationType.java` | enum |
| 6 | 迁移 | `app/.../shared/aspect/operationlog/OperationLogRecord.java` → `common/.../operationlog/OperationLogRecord.java` | record |
| 7 | 迁移 | `app/.../shared/aspect/operationlog/OperationLogWriter.java` → `common/.../operationlog/OperationLogWriter.java` | interface |
| 8 | 迁移 | `app/.../shared/aspect/operationlog/BusinessLog.java` → `common/.../operationlog/BusinessLog.java` | annotation |
| 9 | 废弃标记 | `app/.../entity/base/BaseRequest.java` | 添加 @Deprecated |
| 10 | 废弃标记 | `app/.../entity/base/BasePageRequest.java` | 添加 @Deprecated |
| 11 | 废弃标记 | `app/.../entity/base/BaseResult.java`（旧位置） | 添加 @Deprecated |
| 12 | 废弃标记 | `app/.../entity/base/BasePageResult.java`（旧位置） | 添加 @Deprecated |
| 13 | 废弃标记 | 4 个旧位置的 operationlog 文件 | 添加 @Deprecated |
| 14 | 修改 | `common/pom.xml` | 移除 spring-boot-starter |
| 15 | 新增 | `shared/pagination/PageQueryUTest.java` | 测试 |
| 16 | 新增 | `shared/pagination/PageResultUTest.java` | 测试 |
| 17 | 新增 | `shared/result/BasePageResultUTest.java` | 测试 |

---

## 八、依赖关系（不变）

阶段 1 不引入新的 Maven 依赖。所有新增类型使用已有依赖（Jakarta Validation、OTel、Lombok）。
