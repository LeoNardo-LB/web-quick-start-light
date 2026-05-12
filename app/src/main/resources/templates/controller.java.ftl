package ${modulePackage}.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${modulePackage}.${entityName}Facade;

/**
 * ${tableComment} Controller
 * <p>
 * 自动生成，禁止手动修改。
 */
@RestController
@RequestMapping("/api/${moduleName}")
class ${entityName}Controller {

    private final ${entityName}Facade facade;

    ${entityName}Controller(${entityName}Facade facade) {
        this.facade = facade;
    }

    // TODO: 添加 CRUD 端点
}
