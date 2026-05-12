package ${modulePackage}.internal;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment} Service
 * <p>
 * 自动生成，禁止手动修改。
 */
class ${entityName}Service {

    private final ${entityName}Repository repository;

    ${entityName}Service(${entityName}Repository repository) {
        this.repository = repository;
    }

    Optional<${entityName}> findById(String id) {
        return repository.findById(id);
    }

    List<${entityName}> findAll() {
        return repository.findAll();
    }

    ${entityName} save(${entityName} model) {
        return repository.save(model);
    }
}
