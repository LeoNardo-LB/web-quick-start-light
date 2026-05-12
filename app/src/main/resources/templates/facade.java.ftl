package ${modulePackage};

import java.util.List;
import java.util.Optional;
import org.smm.archetype.shared.result.BasePageResult;
import ${modulePackage}.internal.${entityName}VO;
import ${modulePackage}.internal.${entityName}PageQuery;

/**
 * ${tableComment} Facade 接口（模块公开 API）
 * <p>
 * 自动生成，禁止手动修改。
 */
public interface ${entityName}Facade {

    /**
     * 根据 ID 获取${tableComment}
     */
    Optional<${entityName}VO> findById(String id);

    /**
     * 获取所有${tableComment}
     */
    List<${entityName}VO> findAll();

    /**
     * 分页查询${tableComment}
     */
    BasePageResult<${entityName}VO> findByPage(${entityName}PageQuery query);
}
