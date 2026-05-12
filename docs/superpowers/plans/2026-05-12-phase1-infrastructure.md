# 阶段 1 基础设施层 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建新架构的共享类型基础（PageQuery、PageResult）、迁移响应类型到 `shared/result/`、迁移操作日志接口到 `common/`、废弃旧类型，不触碰任何现有业务代码。

**Architecture:** 纯增量操作 — 新增文件、迁移文件（旧位置保留 @Deprecated）、修改 common/pom.xml。所有现有功能保持正常工作。

**Tech Stack:** Java 25, Maven, Jakarta Validation, OTel, Lombok

**关联 Spec:** `docs/superpowers/specs/2026-05-12-phase1-infrastructure-spec.md`

**Prerequisites:**
```bash
# 确保工作目录干净
git status
# 基于 main 创建特性分支
git checkout main && git pull
git checkout -b refactor/phase1-infrastructure
```

---

## File Structure

```
新增:
  app/src/main/java/org/smm/archetype/shared/pagination/PageQuery.java
  app/src/main/java/org/smm/archetype/shared/pagination/PageResult.java
  app/src/test/java/org/smm/archetype/shared/pagination/PageQueryUTest.java
  app/src/test/java/org/smm/archetype/shared/pagination/PageResultUTest.java
  app/src/test/java/org/smm/archetype/shared/result/BasePageResultUTest.java

迁移（新位置）:
  app/src/main/java/org/smm/archetype/shared/result/BaseResult.java       ← entity/base/
  app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java    ← entity/base/ (重写)
  common/src/main/java/org/smm/archetype/operationlog/OperationType.java   ← shared/aspect/operationlog/
  common/src/main/java/org/smm/archetype/operationlog/OperationLogRecord.java
  common/src/main/java/org/smm/archetype/operationlog/OperationLogWriter.java
  common/src/main/java/org/smm/archetype/operationlog/BusinessLog.java

废弃标记（旧位置添加 @Deprecated）:
  app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java
  app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java
  app/src/main/java/org/smm/archetype/entity/base/BaseResult.java
  app/src/main/java/org/smm/archetype/entity/base/BasePageResult.java
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationType.java
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogRecord.java
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogWriter.java
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/BusinessLog.java

修改:
  common/pom.xml
```

---

### Task 1: 创建 PageQuery record *(Spec §一.1)*

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/pagination/PageQuery.java`
- Create: `app/src/test/java/org/smm/archetype/shared/pagination/PageQueryUTest.java`

- [ ] **Step 1: 创建 PageQuery.java**

```java
package org.smm.archetype.shared.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求值对象（1-based，前端友好）。
 * <p>
 * 各模块的 *PageQuery record 通过紧凑构造器复用此类的校验逻辑。
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

- [ ] **Step 2: 编写 PageQueryUTest**

```java
package org.smm.archetype.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PageQuery 分页请求值对象")
class PageQueryUTest {

    @Test
    @DisplayName("正常值创建成功")
    void shouldCreateWithValidValues() {
        var page = new PageQuery(3, 20);
        assertEquals(3, page.pageNo());
        assertEquals(20, page.pageSize());
    }

    @Test
    @DisplayName("pageNo=0 自动修正为 1")
    void shouldFixZeroPageNo() {
        var page = new PageQuery(0, 20);
        assertEquals(1, page.pageNo());
    }

    @Test
    @DisplayName("pageNo 负数自动修正为 1")
    void shouldFixNegativePageNo() {
        var page = new PageQuery(-5, 20);
        assertEquals(1, page.pageNo());
    }

    @Test
    @DisplayName("pageSize=0 自动修正为 10")
    void shouldFixZeroPageSize() {
        var page = new PageQuery(1, 0);
        assertEquals(10, page.pageSize());
    }

    @Test
    @DisplayName("pageSize 负数自动修正为 10")
    void shouldFixNegativePageSize() {
        var page = new PageQuery(1, -5);
        assertEquals(10, page.pageSize());
    }

    @Test
    @DisplayName("pageSize 超限自动修正为 100")
    void shouldCappedPageSize() {
        var page = new PageQuery(1, 999);
        assertEquals(100, page.pageSize());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn test -Dtest="PageQueryUTest" -pl app
```
Expected: 6 tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/pagination/PageQuery.java \
        app/src/test/java/org/smm/archetype/shared/pagination/PageQueryUTest.java
git commit -m "feat(phase1): add PageQuery record with validation"
```

---

### Task 2: 创建 PageResult record *(Spec §一.2)*

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/pagination/PageResult.java`
- Create: `app/src/test/java/org/smm/archetype/shared/pagination/PageResultUTest.java`

- [ ] **Step 1: 创建 PageResult.java**

```java
package org.smm.archetype.shared.pagination;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果值对象（框架无关，泛型）。
 * <p>
 * 替代 MyBatis-Plus {@code IPage<T>} 作为 Repository 接口的返回类型。
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

- [ ] **Step 2: 编写 PageResultUTest**

```java
package org.smm.archetype.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResult 分页结果值对象")
class PageResultUTest {

    @Test
    @DisplayName("正常分页结果 totalPages 计算正确")
    void shouldCalculateTotalPages() {
        var list = List.of("a", "b", "c", "d", "e");
        var result = PageResult.of(list, 100, 1, 20);
        assertEquals(5, result.totalPages());
        assertEquals(100, result.total());
        assertEquals(5, result.list().size());
    }

    @Test
    @DisplayName("总数 = 0 时 totalPages = 0")
    void shouldHandleZeroTotal() {
        var result = PageResult.of(List.of(), 0, 1, 20);
        assertEquals(0, result.totalPages());
        assertEquals(0, result.total());
        assertTrue(result.list().isEmpty());
    }

    @Test
    @DisplayName("总数不能整除时 totalPages 向上取整")
    void shouldCeilTotalPages() {
        var result = PageResult.of(List.of("a", "b", "c"), 105, 1, 20);
        assertEquals(6, result.totalPages());
    }

    @Test
    @DisplayName("空结果快捷方法创建零 total")
    void shouldCreateEmptyResult() {
        var result = PageResult.<String>empty(1, 20);
        assertEquals(0, result.total());
        assertEquals(0, result.totalPages());
        assertTrue(result.list().isEmpty());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn test -Dtest="PageResultUTest" -pl app
```
Expected: 4 tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/pagination/PageResult.java \
        app/src/test/java/org/smm/archetype/shared/pagination/PageResultUTest.java
git commit -m "feat(phase1): add PageResult record with static factory"
```

---

### Task 3: 迁移 BaseResult 到 shared/result/ *(Spec §二.1)*

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/result/BaseResult.java`
- Modify: `app/src/main/java/org/smm/archetype/entity/base/BaseResult.java`

- [ ] **Step 1: 复制 BaseResult.java 到 shared/result/，仅修改 package 声明**

```bash
mkdir -p app/src/main/java/org/smm/archetype/shared/result/
cp app/src/main/java/org/smm/archetype/entity/base/BaseResult.java \
   app/src/main/java/org/smm/archetype/shared/result/BaseResult.java
```

然后将新文件第 1 行 `package org.smm.archetype.entity.base;` 改为 `package org.smm.archetype.shared.result;`。

> **注意**：完整复制文件（保留所有字段 javadoc、类级 javadoc），仅改 package 声明，不修改其他任何内容。

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl app -q
```
Expected: BUILD SUCCESS （新位置编译通过）

- [ ] **Step 3: 旧位置 BaseResult.java 顶部添加 @Deprecated**

打开 `app/src/main/java/org/smm/archetype/entity/base/BaseResult.java`，在现有类级 javadoc 块之前插入以下内容：

```java
/**
 * @deprecated 已迁移至 {@code org.smm.archetype.shared.result.BaseResult}，将在阶段 4 删除。
 */
@Deprecated
```

> **精确编辑**：在 `public class BaseResult<T>` 上方的现有 javadoc 块（以 `/**` 开头）之前，插入上面的 javadoc+@Deprecated 块。不要删除原有 javadoc，两个 javadoc 块会由编译器合并。

- [ ] **Step 4: 编译验证（两个 BaseResult 共存）**

```bash
mvn compile -pl app -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 搜索旧 import 引用，更新为新位置**

```bash
grep -rl "import org.smm.archetype.entity.base.BaseResult" app/src/main/java app/src/test/java
```

对每个匹配文件，将 `import org.smm.archetype.entity.base.BaseResult;` 替换为 `import org.smm.archetype.shared.result.BaseResult;`

- [ ] **Step 6: 编译 + 全部测试**

```bash
mvn test -pl app
```
Expected: 所有现有测试 PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/result/BaseResult.java \
        app/src/main/java/org/smm/archetype/entity/base/BaseResult.java
# 如果 grep 替换了 import，也添加修改后的文件
git add app/src/main/java/org/smm/archetype/ app/src/test/java/org/smm/archetype/
git commit -m "refactor(phase1): migrate BaseResult to shared/result/, deprecate old location"
```

> **回滚**：若此 Task 失败，`git reset --soft HEAD~1` 撤回 commit，然后 `git checkout -- .` 恢复工作区。

---

### Task 4: 重写并迁移 BasePageResult *(Spec §二.2)*

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java`
- Create: `app/src/test/java/org/smm/archetype/shared/result/BasePageResultUTest.java`
- Modify: `app/src/main/java/org/smm/archetype/entity/base/BasePageResult.java`

- [ ] **Step 1: 编写 BasePageResultUTest（先写测试）**

```java
package org.smm.archetype.shared.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.pagination.PageResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BasePageResult 分页响应包装")
class BasePageResultUTest {

    @Test
    @DisplayName("from(PageResult) 构建正确的分页响应")
    void shouldBuildFromPageResult() {
        var pageResult = PageResult.of(
                List.of("a", "b", "c"), 100, 2, 20);
        var result = BasePageResult.from(pageResult);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData().size());
        assertEquals(100, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(20, result.getPageSize());
    }

    @Test
    @DisplayName("from(PageResult) 空结果构建")
    void shouldBuildEmptyFromPageResult() {
        var pageResult = PageResult.of(List.of(), 0, 1, 10);
        var result = BasePageResult.from(pageResult);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotal());
    }
}
```

- [ ] **Step 2: 运行测试，预期失败**

```bash
mvn test -Dtest="BasePageResultUTest" -pl app
```
Expected: FAIL（`BasePageResult` 在 shared/result/ 尚不存在）

- [ ] **Step 3: 在 shared/result/ 创建重写后的 BasePageResult.java**

```java
package org.smm.archetype.shared.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
 * 已去除 MyBatis-Plus {@code IPage} 依赖在公开 API 中的直接使用，
 * 仅通过 {@code from(PageResult)} 静态工厂构建。
 *
 * @param <T> 结果项类型
 */
@Getter
@Setter
public class BasePageResult<T> extends BaseResult<List<T>> {

    private long total;
    private int pageNo;
    private int pageSize;

    /**
     * 从 PageResult 构建 BasePageResult（框架无关，推荐使用）。
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
     *             此方法仅在 RepositoryImpl 内部过渡使用，将在阶段 4 删除。
     */
    @Deprecated
    public static <T> BasePageResult<T> fromPage(IPage<T> page) {
        return from(PageResult.of(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        ));
    }
}
```

- [ ] **Step 4: 运行 BasePageResultUTest**

```bash
mvn test -Dtest="BasePageResultUTest" -pl app
```
Expected: 2 tests PASS

- [ ] **Step 5: 验证新文件 MyBatis-Plus import 仅限于 @Deprecated 方法**

```bash
grep "import com.baomidou" app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java
```
Expected: 1 match（`import com.baomidou.mybatisplus.core.metadata.IPage` — 仅用于 `@Deprecated fromPage(IPage)` 方法签名）

- [ ] **Step 6: 旧位置 BasePageResult 添加 @Deprecated**

打开 `app/src/main/java/org/smm/archetype/entity/base/BasePageResult.java`，在现有类级 javadoc 块之前插入：

```java
/**
 * @deprecated 已迁移至 {@code org.smm.archetype.shared.result.BasePageResult}，将在阶段 4 删除。
 */
@Deprecated
```

> **精确编辑**：在 `public class BasePageResult<T>` 上方的现有 javadoc 块（以 `/**` 开头）之前插入。不要删除原有 javadoc，两个 javadoc 块由编译器合并。

- [ ] **Step 7: 更新所有旧 import 引用**

```bash
grep -rl "import org.smm.archetype.entity.base.BasePageResult" app/src/main/java app/src/test/java
```

对每个匹配文件，将 `import org.smm.archetype.entity.base.BasePageResult;` 替换为 `import org.smm.archetype.shared.result.BasePageResult;`

- [ ] **Step 8: 编译 + 全部测试**

```bash
mvn test -pl app
```
Expected: 所有测试 PASS（包括新增的 BasePageResultUTest）

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java \
        app/src/test/java/org/smm/archetype/shared/result/BasePageResultUTest.java \
        app/src/main/java/org/smm/archetype/entity/base/BasePageResult.java
# 如果 grep 替换了 import，也添加修改后的文件
git add app/src/main/java/org/smm/archetype/ app/src/test/java/org/smm/archetype/
git commit -m "refactor(phase1): rewrite BasePageResult, remove IPage from public API, migrate to shared/result/"
```

> **回滚**：若此 Task 失败，`git reset --soft HEAD~1` 撤回 commit，然后 `git checkout -- .` 恢复工作区。

---

### Task 5: 迁移操作日志接口层到 common

> **注意**：操作日志目录实际有 5 个文件（含 LogAspect.java），本 Task 仅迁移 4 个接口层文件。LogAspect.java 是 Spring AOP 实现（依赖 @Aspect/@Around/@Pointcut），属于 app 模块横切关注点，不随接口层迁移至 common。

**Files:**
- Create: `common/src/main/java/org/smm/archetype/operationlog/OperationType.java`
- Create: `common/src/main/java/org/smm/archetype/operationlog/OperationLogRecord.java`
- Create: `common/src/main/java/org/smm/archetype/operationlog/OperationLogWriter.java`
- Create: `common/src/main/java/org/smm/archetype/operationlog/BusinessLog.java`
- Modify: 4 个旧位置文件（添加 @Deprecated）

- [ ] **Step 1: 创建 common 下的 4 个新文件**

复制内容，仅修改 package 声明：

**OperationType.java:**
```java
package org.smm.archetype.operationlog;

/**
 * 操作类型枚举，用于标注业务日志的操作分类。
 */
public enum OperationType {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String description;

    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
```

**OperationLogRecord.java:**
```java
package org.smm.archetype.operationlog;

/**
 * 操作日志记录，用于在 LogAspect 和 OperationLogWriter 之间传递日志数据。
 */
public record OperationLogRecord(
        String traceId,
        String userId,
        String module,
        String operationType,
        String description,
        String method,
        String params,
        String result,
        long executionTime,
        String ip,
        String status,
        String errorMessage
) {}
```

**OperationLogWriter.java:**
```java
package org.smm.archetype.operationlog;

/**
 * 操作日志写入器接口。
 * <p>
 * 由 app 模块实现，LogAspect 通过此接口将操作日志写入数据库。
 */
public interface OperationLogWriter {

    void write(OperationLogRecord record);
}
```

**BusinessLog.java:**
```java
package org.smm.archetype.operationlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLog {

    String value() default "";

    String module() default "";

    OperationType operation() default OperationType.QUERY;

    double samplingRate() default 1.0;
}
```

- [ ] **Step 2: 编译 common 模块**

```bash
mvn compile -pl common -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 旧位置 4 个文件添加 @Deprecated**

针对 enum（`OperationType`）和 record/interface/annotation，在 javadoc 中标注 + 添加 `@Deprecated`：

```java
// OperationType.java 旧位置 — 在 javadoc 第一行之前插入
/**
 * @deprecated 已迁移至 {@code org.smm.archetype.operationlog.OperationType}，将在阶段 4 删除。
 */
@Deprecated
public enum OperationType { ... }
```

其余 3 个文件同理。

> 精确编辑：在每个旧文件 javadoc 块后、类型声明前插入 `@Deprecated` 注解。

- [ ] **Step 4: 编译 app 模块**

```bash
mvn compile -pl app -q
```
Expected: BUILD SUCCESS（旧位置 @Deprecated 不影响编译）

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/org/smm/archetype/operationlog/ \
        app/src/main/java/org/smm/archetype/shared/aspect/operationlog/
git commit -m "refactor(phase1): migrate operation log interface layer to common module"
```

> **回滚**：若此 Task 失败，`git reset --soft HEAD~1` 撤回 commit，然后 `git checkout -- .` 恢复工作区。

---

### Task 6: 废弃标记 BaseRequest 和 BasePageRequest *(Spec §三)*

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java`
- Modify: `app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java`

- [ ] **Step 1: BaseRequest.java 添加 @Deprecated**

打开 `app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java`：

```java
package org.smm.archetype.entity.base;

import lombok.Getter;
import lombok.Setter;

/**
 * @deprecated traceId 由 OTel Span 自动管理，requestId 由 Filter 生成。
 *             请求 record 只需包含业务字段，不再需要此基类。将在阶段 4 删除。
 */
@Deprecated
@Getter
@Setter
public class BaseRequest {

    private String requestId;
    private String traceId;
}
```

- [ ] **Step 2: BasePageRequest.java 添加 @Deprecated**

打开 `app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java`：

```java
package org.smm.archetype.entity.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * @deprecated 被 {@code org.smm.archetype.shared.pagination.PageQuery} record 替代。
 *             record 无法继承 class，设计为独立 record + 紧凑构造器模式。将在阶段 4 删除。
 * @see org.smm.archetype.shared.pagination.PageQuery
 */
@Deprecated
@Getter
@Setter
public class BasePageRequest extends BaseRequest {

    @Min(1)
    private int pageNo = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 20;
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl app -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java \
        app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java
git commit -m "refactor(phase1): deprecate BaseRequest and BasePageRequest"
```

---

### Task 7: 清理 common/pom.xml *(Spec §四.1)*

**Files:**
- Modify: `common/pom.xml`

- [ ] **Step 1: 移除 spring-boot-starter**

编辑 `common/pom.xml`，找到并删除 `spring-boot-starter` 依赖块：

```xml
<!-- 删除此块 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 2: 编译 + 测试 common 模块**

```bash
mvn clean test -pl common
```
Expected: BUILD SUCCESS，所有测试 PASS

- [ ] **Step 3: 验证零 Spring 依赖（编译 scope）**

```bash
mvn dependency:tree -pl common | grep "spring-boot-starter" | grep -v test | grep -v "spring-boot-starter-test"
```
Expected: 零输出（编译 scope 无 spring-boot-starter）

- [ ] **Step 4: Commit**

```bash
git add common/pom.xml
git commit -m "refactor(phase1): remove spring-boot-starter from common, zero Spring dependency"
```

---

### Task 8: 全量验证 *(Spec §六)*

- [ ] **Step 1: 全项目编译**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 全项目测试**

```bash
mvn test
```
Expected: BUILD SUCCESS，所有测试 PASS

- [ ] **Step 3: ArchUnit 规则验证**

```bash
mvn test -Dtest="*ComplianceUTest" -pl app
```
Expected: 所有 ArchUnit 规则 PASS（含 M-01 common 零 Spring 依赖）

- [ ] **Step 4: 专项验证**

```bash
# BasePageResult 新位置不含 MyBatis-Plus 类型泄漏
grep -c "import com.baomidou" app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java
```
Expected: `1`（仅 @Deprecated fromPage 方法签名需要）

```bash
# common 模块 operationlog 包存在
ls common/src/main/java/org/smm/archetype/operationlog/
```
Expected: `BusinessLog.java  OperationLogRecord.java  OperationLogWriter.java  OperationType.java`

```bash
# 废弃类编译通过
mvn compile -q
```
Expected: 零编译错误（@Deprecated 仅警告）

- [ ] **Step 5: Commit**

```bash
git commit -m "verify(phase1): all tests pass, ArchUnit rules pass, ready for phase 2" --allow-empty
```

---

## Verification Summary

| # | 验证项 | 命令 | 预期 |
|---|--------|------|------|
| 1 | PageQueryUTest | `mvn test -Dtest="PageQueryUTest" -pl app` | 6 PASS |
| 2 | PageResultUTest | `mvn test -Dtest="PageResultUTest" -pl app` | 4 PASS |
| 3 | BasePageResultUTest | `mvn test -Dtest="BasePageResultUTest" -pl app` | 2 PASS |
| 4 | common 独立测试 | `mvn test -pl common` | PASS |
| 5 | 全项目测试 | `mvn test` | ALL PASS |
| 6 | ArchUnit 规则 | `mvn test -Dtest="*ComplianceUTest" -pl app` | ALL PASS |
| 7 | BasePageResult 不含 MyBatis-Plus import | grep | 仅 1 处 @Deprecated 方法 |
| 8 | common 零 Spring 编译依赖 | `mvn dependency:tree -pl common` | 无 spring-boot-starter |
