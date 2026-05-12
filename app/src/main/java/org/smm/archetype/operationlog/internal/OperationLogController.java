package org.smm.archetype.operationlog.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/operation-logs")
@Validated
class OperationLogController {

    private final OperationLogFacade operationLogFacade;

    @GetMapping
    public BasePageResult<OperationLogVO> findByPage(
            @Valid @ModelAttribute OperationLogPageQuery query) {
        return operationLogFacade.findByPage(query);
    }
}
