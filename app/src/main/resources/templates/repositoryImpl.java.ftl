package ${modulePackage}.internal;

import org.springframework.stereotype.Repository;
import org.smm.archetype.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment}仓储实现
 * <p>
 * 自动生成，禁止手动修改。
 */
@Repository
class ${entityName}RepositoryImpl implements ${entityName}Repository {

    private final ${entityName}Mapper mapper;
    private final ${entityName}Converter converter;

    ${entityName}RepositoryImpl(${entityName}Mapper mapper, ${entityName}Converter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public Optional<${entityName}> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toModel);
    }

    @Override
    public List<${entityName}> findAll() {
        return mapper.selectList(null).stream()
                .map(converter::toModel)
                .toList();
    }

    @Override
    public ${entityName} save(${entityName} model) {
        ${entityName}DO dataObject = converter.toDO(model);
        if (model.getId() == null) {
            mapper.insert(dataObject);
        } else {
            mapper.updateById(dataObject);
        }
        return converter.toModel(dataObject);
    }

    @Override
    public PageResult<${entityName}> findByPage(${entityName}PageQuery query) {
        // TODO: 实现分页查询
        return PageResult.empty(query.pageNo(), query.pageSize());
    }
}
