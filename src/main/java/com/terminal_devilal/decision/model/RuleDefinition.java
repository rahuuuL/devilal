package com.terminal_devilal.decision.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RuleDefinition(
        String name,
        int priority,
        List<Condition> conditions,
        List<Action> actions) {

    public record Condition(String indicator, Operator operator, Object value, Map<String, Object> parameters) {
        public Condition(String indicator, Operator operator, Object value) {
            this(indicator, operator, value, Map.of());
        }
    }

    public record Action(String target, ActionType type, BigDecimal constantValue, Expression expression) {}

    public sealed interface Expression permits Constant, Attribute, Literal, BinaryExpression, FunctionExpression {}
    public record Constant(BigDecimal value) implements Expression {}
    public record Attribute(String code) implements Expression {}
    public record Literal(Object value) implements Expression {}
    public record BinaryExpression(BinaryOperator operator, Expression left, Expression right) implements Expression {}
    public record FunctionExpression(FunctionOperator operator, List<Expression> arguments) implements Expression {}

    public enum Operator { GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, EQUAL, BETWEEN }
    public enum ActionType { SET, INCREASE, DECREASE, TAG, SELECT, EXCLUDE }
    public enum BinaryOperator { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO }
    public enum FunctionOperator { ABS, MIN, MAX, CLAMP }
}
