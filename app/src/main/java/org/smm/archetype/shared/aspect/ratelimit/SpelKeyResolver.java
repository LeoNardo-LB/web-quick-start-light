package org.smm.archetype.shared.aspect.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 表达式解析器。
 * <p>
 * 将方法参数绑定到 SpEL 上下文中，支持通过参数名引用方法参数。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 方法签名：void doSomething(Long userId, String name)
 * // 表达式：#userId + ":" + #name
 * // 结果："42:alice"
 * }</pre>
 */
@Slf4j
public final class SpelKeyResolver {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private SpelKeyResolver() {
        // 工具类禁止实例化
    }

    /**
     * 解析 SpEL 表达式。
     * @param method        目标方法（用于获取参数名）
     * @param args          方法参数值
     * @param keyExpression SpEL 表达式
     * @return 解析后的字符串值，解析失败时返回原始表达式
     */
    public static String resolve(Method method, Object[] args, String keyExpression) {
        if (keyExpression == null || keyExpression.isBlank()) {
            return "";
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            bindParameters(context, method, args);

            Expression expression = PARSER.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "null";
        } catch (Exception e) {
            log.warn("[RateLimit] SpEL key resolution failed for '{}': {}", keyExpression, e.getMessage());
            return keyExpression;
        }
    }

    private static void bindParameters(StandardEvaluationContext context, Method method, Object[] args) {
        if (args == null) {
            return;
        }

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < Math.min(parameters.length, args.length); i++) {
            String name = parameters[i].isNamePresent()
                                  ? parameters[i].getName()
                                  : "arg" + i;
            context.setVariable(name, args[i]);
        }
    }

}
