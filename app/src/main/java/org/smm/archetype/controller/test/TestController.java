package org.smm.archetype.controller.test;

import cn.dev33.satoken.exception.NotLoginException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.base.BaseResult;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.exception.SysException;
import org.smm.archetype.shared.aspect.operationlog.BusinessLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(summary = "测试 ClientException")
    @GetMapping("/client-exception")
    public BaseResult<Void> clientException() {
        throw new ClientException(CommonErrorCode.RPC_EXCEPTION);
    }

    @Operation(summary = "测试 SysException")
    @GetMapping("/sys-exception")
    public BaseResult<Void> sysException() {
        throw new SysException(CommonErrorCode.SYS_ERROR);
    }

    @Operation(summary = "测试通用 Exception")
    @GetMapping("/generic-exception")
    public BaseResult<Void> genericException() {
        throw new RuntimeException("测试通用异常");
    }

    @Operation(summary = "测试 NotLoginException")
    @GetMapping("/not-login")
    public BaseResult<Void> notLogin() {
        throw new NotLoginException("测试未登录异常", "login", NotLoginException.NOT_TOKEN);
    }

    @Operation(summary = "测试 BindException — 表单绑定")
    @PostMapping("/bind-test")
    public BaseResult<String> bindTest(@Valid TestForm form) {
        return BaseResult.success("Hello, " + form.name);
    }

    public static class TestForm {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
