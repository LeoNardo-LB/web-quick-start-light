package org.smm.archetype.controller.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.api.BaseResult;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.aspect.operationlog.BusinessLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口")
@Validated
public class TestController {

    @Operation(summary = "hello world")
    @GetMapping("/hello")
    public BaseResult<List<String>> hello() {
        return BaseResult.success(List.of("Hello", "World"));
    }

    @Operation(summary = "测试异常")
    @GetMapping("/exception")
    public BaseResult<Void> exception() {
        throw new BizException(CommonErrorCode.FAIL, "测试业务异常");
    }

    @Operation(summary = "参数校验测试")
    @GetMapping("/validate")
    public BaseResult<String> validate(@RequestParam @NotBlank String name) {
        return BaseResult.success("Hello, " + name);
    }

    @BusinessLog("测试业务日志")
    @Operation(summary = "测试BusinessLog注解")
    @GetMapping("/bizlog")
    public BaseResult<String> bizLog() {
        log.info("执行业务日志测试方法");
        return BaseResult.success("bizLog test completed");
    }
}
