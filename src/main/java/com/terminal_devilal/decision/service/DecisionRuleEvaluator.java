package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.model.DecisionState;
import com.terminal_devilal.decision.model.RuleDefinition;
import com.terminal_devilal.decision.model.SubjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class DecisionRuleEvaluator {
    private static final Logger log = LoggerFactory.getLogger(DecisionRuleEvaluator.class);

    public void evaluate(List<RuleDefinition> rules, SubjectContext context, DecisionState state) {
        evaluateAndReturnFiredRules(rules, context, state);
    }

    public List<String> evaluateAndReturnFiredRules(List<RuleDefinition> rules, SubjectContext context, DecisionState state) {
        List<String> fired = new java.util.ArrayList<>();
        log.info("Evaluating {} rules for subject {} {} at {}", rules.size(), context.getSubjectType(), context.getSubjectId(), context.getAsOfDate());
        rules.stream().sorted(Comparator.comparingInt(RuleDefinition::priority).reversed())
                .forEach(rule -> {
                    boolean allConditionsMatched = rule.conditions().stream().allMatch(condition -> matches(condition, context));
                    log.info("Rule {} priority={} matched={} conditions={}", rule.name(), rule.priority(), allConditionsMatched, rule.conditions());
                    if (allConditionsMatched) {
                        fired.add(rule.name());
                        log.info("Rule {} fired for subject {} {}. Applying {} action(s)", rule.name(), context.getSubjectType(), context.getSubjectId(), rule.actions().size());
                        rule.actions().forEach(action -> apply(action, context, state));
                    }
                });
        log.info("Completed rule evaluation for subject {} {}. Fired rules: {}", context.getSubjectType(), context.getSubjectId(), fired);
        return fired;
    }

    private boolean matches(RuleDefinition.Condition condition, SubjectContext context) {
        Object actual = context.getAttribute(condition.indicator());
        log.debug("Checking condition indicator={} actual={} operator={} expected={}", condition.indicator(), actual, condition.operator(), condition.value());
        if (condition.operator() == RuleDefinition.Operator.BETWEEN) {
            if (!(actual instanceof Number) || !(condition.value() instanceof List<?> range) || range.size() != 2) {
                log.debug("Condition {} failed because actual or range is invalid", condition.indicator());
                return false;
            }
            BigDecimal actualValue = new BigDecimal(actual.toString());
            boolean result = actualValue.compareTo(new BigDecimal(range.get(0).toString())) >= 0
                    && actualValue.compareTo(new BigDecimal(range.get(1).toString())) <= 0;
            log.debug("Condition {} BETWEEN result={} actualValue={}", condition.indicator(), result, actualValue);
            return result;
        }
        if (!(actual instanceof Number) || !(condition.value() instanceof Number)) {
            boolean result = actualEquals(actual, condition.value(), condition.operator());
            log.debug("Condition {} non-numeric compare result={} actual={} expected={}", condition.indicator(), result, actual, condition.value());
            return result;
        }
        BigDecimal left = new BigDecimal(actual.toString());
        BigDecimal right = new BigDecimal(condition.value().toString());
        boolean result = switch (condition.operator()) {
            case GREATER_THAN -> left.compareTo(right) > 0;
            case GREATER_THAN_OR_EQUAL -> left.compareTo(right) >= 0;
            case LESS_THAN -> left.compareTo(right) < 0;
            case LESS_THAN_OR_EQUAL -> left.compareTo(right) <= 0;
            case EQUAL -> left.compareTo(right) == 0;
                case BETWEEN -> false;
        };
        log.debug("Condition {} numeric compare result={} left={} right={}", condition.indicator(), result, left, right);
        return result;
    }

    private boolean actualEquals(Object actual, Object expected, RuleDefinition.Operator operator) {
        return operator == RuleDefinition.Operator.EQUAL && actual != null && actual.equals(expected);
    }

    private void apply(RuleDefinition.Action action, SubjectContext context, DecisionState state) {
        if (action.expression() instanceof RuleDefinition.Literal literal) {
            if (action.type() == RuleDefinition.ActionType.SET || action.type() == RuleDefinition.ActionType.TAG) {
                log.info("Applying literal action target={} type={} value={}", action.target(), action.type(), literal.value());
                state.set(action.target(), literal.value());
            }
            return;
        }
        BigDecimal value = action.expression() == null ? action.constantValue() : evaluate(action.expression(), context);
        log.info("Applying action target={} type={} computedValue={} constantValue={}", action.target(), action.type(), value, action.constantValue());
        switch (action.type()) {
            case SET, TAG -> state.set(action.target(), value == null ? null : value.stripTrailingZeros());
            case INCREASE -> state.increase(action.target(), requireValue(value));
            case DECREASE -> state.decrease(action.target(), requireValue(value));
            case SELECT -> state.set(action.target(), Boolean.TRUE);
            case EXCLUDE -> state.set(action.target(), Boolean.FALSE);
        }
        log.info("Updated output {} = {}", action.target(), state.get(action.target()));
    }

    private BigDecimal evaluate(RuleDefinition.Expression expression, SubjectContext context) {
        if (expression instanceof RuleDefinition.Constant constant) return constant.value();
        if (expression instanceof RuleDefinition.Attribute attribute) return number(context.getAttribute(attribute.code()));
        if (expression instanceof RuleDefinition.Literal literal) throw new IllegalArgumentException("Literal expressions are only valid for SET or TAG actions");
        if (expression instanceof RuleDefinition.BinaryExpression binary) {
            BigDecimal left = evaluate(binary.left(), context), right = evaluate(binary.right(), context);
            return switch (binary.operator()) {
                case ADD -> left.add(right); case SUBTRACT -> left.subtract(right);
                case MULTIPLY -> left.multiply(right); case DIVIDE -> left.divide(right, 12, RoundingMode.HALF_UP);
                case MODULO -> left.remainder(right);
            };
        }
        RuleDefinition.FunctionExpression function = (RuleDefinition.FunctionExpression) expression;
        List<BigDecimal> values = function.arguments().stream().map(argument -> evaluate(argument, context)).toList();
        return switch (function.operator()) {
            case ABS -> values.get(0).abs(); case MIN -> values.stream().min(BigDecimal::compareTo).orElseThrow();
            case MAX -> values.stream().max(BigDecimal::compareTo).orElseThrow();
            case CLAMP -> values.get(0).max(values.get(1)).min(values.get(2));
        };
    }

    private BigDecimal number(Object value) {
        if (!(value instanceof Number)) throw new IllegalArgumentException("Expression attribute is not numeric");
        return new BigDecimal(value.toString());
    }

    private BigDecimal requireValue(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Numeric action requires a value");
        return value;
    }
}
