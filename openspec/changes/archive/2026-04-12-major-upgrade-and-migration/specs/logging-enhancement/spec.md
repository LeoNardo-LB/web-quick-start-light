## REMOVED Requirements

### Requirement: 渐进式日志增强
**Reason**: 日志系统将完全替换为 speccoding 版本，不再采用渐进增强方式。原有的 BizLog/BizLogAspect/AuditLogService 等将被 BusinessLog/LogAspect/MethodExecutionLog 替代。
**Migration**: 所有引用 @BizLog 的代码改为 @BusinessLog，BizLogAspect 的功能由 LogAspect 替代。

## ADDED Requirements

### Requirement: 日志系统完全替换
现有日志系统 SHALL 完全删除并用 speccoding 版本替换（详见 logging-system-replacement spec）。logging-enhancement spec 中描述的渐进增强路径不再适用。

#### Scenario: 日志系统为 speccoding 版本
- **WHEN** 检查日志相关代码
- **THEN** 使用 speccoding 移植的 BusinessLog/LogAspect/LogConfigure/SamplingTurboFilter 等
