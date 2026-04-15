package org.smm.archetype.controller.operationlog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.base.BasePageResult;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.facade.operationlog.OperationLogFacade;
import org.smm.archetype.facade.operationlog.OperationLogVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志")
@Validated
public class OperationLogController {

    private final OperationLogFacade operationLogFacade;

    @GetMapping
    @Operation(summary = "分页查询操作日志")
    public BasePageResult<OperationLogVO> findByPage(
            @Valid @ModelAttribute OperationLogPageQuery query) {
        return operationLogFacade.findByPage(query);
    }
}
