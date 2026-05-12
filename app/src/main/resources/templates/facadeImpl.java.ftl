package ${modulePackage}.internal;

import org.springframework.stereotype.Service;
import ${modulePackage}.${entityName}Facade;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment} Facade 实现
 * <p>
 * 自动生成，禁止手动修改。
 */
@Service
class ${entityName}FacadeImpl implements ${entityName}Facade {

    private final ${entityName}Service service;

    ${entityName}FacadeImpl(${entityName}Service service) {
        this.service = service;
    }

    @Override
    public Optional<${entityName}VO> findById(String id) {
        return service.findById(id).map(this::toVO);
    }

    @Override
    public List<${entityName}VO> findAll() {
        return service.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public BasePageResult<${entityName}VO> findByPage(${entityName}PageQuery query) {
        // TODO: 实现分页查询
        return BasePageResult.from(PageResult.empty(query.pageNo(), query.pageSize()));
    }

    private ${entityName}VO toVO(${entityName} model) {
        // TODO: 实现 Model → VO 转换
        ${entityName}VO vo = new ${entityName}VO();
        vo.setId(model.getId());
        return vo;
    }
}
