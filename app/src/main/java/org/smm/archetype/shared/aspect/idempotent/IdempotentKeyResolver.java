package org.smm.archetype.shared.aspect.idempotent;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.Objects;

/**
 * 幂等 Key 解析器。
 * <p>
 * 负责从 {@link Idempotent} 注解和方法参数中解析幂等 Key：
 * <ul>
 *   <li>field 非空时：使用 SpEL 表达式解析（如 #request.orderId、#p0）</li>
 *   <li>field 为空时：使用 paramsHashCode 作为默认值</li>
 * </ul>
 */
public class IdempotentKeyResolver {

    private final SpelExpressionParser parser;

    public IdempotentKeyResolver() {
        this.parser = new SpelExpressionParser();
    }

    /**
     * 解析幂等 Key。
     * @param className  类名
     * @param methodName 方法名
     * @param paramNames 参数名数组（可能为 null）
     * @param args       参数值数组
     * @param idempotent @Idempotent 注解
     * @return 幂等 Key，格式为 className.methodName(fieldValue)
     */
    public String resolve(String className, String methodName, String[] paramNames, Object[] args, Idempotent idempotent) {
        String field = idempotent.field();
        String fieldValue;

        if (field != null && !field.isEmpty()) {
            fieldValue = resolveSpelField(field, paramNames, args);
        } else {
            fieldValue = String.valueOf(Arrays.hashCode(args));
        }

        return className + "." + methodName + "(" + fieldValue + ")";
    }

    /**
     * 使用 SpEL 表达式解析字段值。
     * <p>
     * 支持的变量：参数名（#request）、参数索引（#p0/#a0）
     */
    String resolveSpelField(String expression, String[] paramNames, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
        }

        Expression expr = parser.parseExpression(expression);
        Object value = expr.getValue(context);
        return Objects.toString(value, "");
    }

}
