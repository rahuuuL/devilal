package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.model.DecisionState;
import com.terminal_devilal.decision.model.RuleDefinition;
import com.terminal_devilal.decision.model.SubjectContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionRuleEvaluatorTest {
    private final DecisionRuleEvaluator evaluator = new DecisionRuleEvaluator();

    @Test
    void appliesRiskRulesUsingIndicatorExpression() {
        RuleDefinition rule = new RuleDefinition(
                "RSI risk", 10,
                List.of(new RuleDefinition.Condition("RSI_PERCENTILE", RuleDefinition.Operator.GREATER_THAN_OR_EQUAL, 0)),
                List.of(new RuleDefinition.Action("RISK_SCORE", RuleDefinition.ActionType.DECREASE, null,
                        new RuleDefinition.BinaryExpression(RuleDefinition.BinaryOperator.SUBTRACT,
                                new RuleDefinition.Constant(BigDecimal.valueOf(100)),
                                new RuleDefinition.Attribute("RSI_PERCENTILE")))));

        DecisionState state = new DecisionState(Map.of("RISK_SCORE", BigDecimal.valueOf(100)));
        evaluator.evaluate(List.of(rule), context(Map.of("RSI_PERCENTILE", BigDecimal.valueOf(90))), state);

        assertEquals(0, ((BigDecimal) state.get("RISK_SCORE")).compareTo(BigDecimal.valueOf(90)));
    }

    @Test
    void supportsGenericFilterOutputAndBetween() {
        RuleDefinition rule = new RuleDefinition(
                "High volume", 10,
                List.of(new RuleDefinition.Condition("RVOL", RuleDefinition.Operator.BETWEEN, List.of(3, 5))),
                List.of(new RuleDefinition.Action("MATCHED", RuleDefinition.ActionType.SET, null, new RuleDefinition.Constant(BigDecimal.ONE))));

        DecisionState state = new DecisionState(Map.of("MATCHED", Boolean.FALSE));
        evaluator.evaluate(List.of(rule), context(Map.of("RVOL", BigDecimal.valueOf(4.1))), state);

        assertEquals(BigDecimal.ONE, state.get("MATCHED"));
    }

    private SubjectContext context(Map<String, Object> attributes) {
        return new SubjectContext("TICKER", "RELIANCE", LocalDate.of(2026, 9, 6), attributes);
    }
}
