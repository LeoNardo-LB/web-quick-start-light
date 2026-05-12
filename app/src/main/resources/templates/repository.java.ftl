package ${modulePackage}.internal;

import java.util.List;
import java.util.Optional;
import org.smm.archetype.shared.pagination.PageResult;

/**
 * ${tableComment}仓储接口
 * <p>
 * 自动生成，禁止手动修改。
 */
public interface ${entityName}Repository {

    Optional<${entityName}> findById(String id);

    List<${entityName}> findAll();

    ${entityName} save(${entityName} model);

    PageResult<${entityName}> findByPage(${entityName}PageQuery query);
}
