package com.terminal_devilal.decision.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DecisionState {
    private final Map<String, Object> outputs = new HashMap<>();

    public DecisionState(Map<String, Object> initialOutputs) {
        if (initialOutputs != null) outputs.putAll(initialOutputs);
    }

    public void set(String target, Object value) { outputs.put(target, value); }

    public void increase(String target, BigDecimal value) {
        outputs.put(target, number(target).add(value));
    }

    public void decrease(String target, BigDecimal value) {
        outputs.put(target, number(target).subtract(value));
    }

    public Object get(String target) { return outputs.get(target); }
    public Map<String, Object> getOutputs() { return Collections.unmodifiableMap(outputs); }

    private BigDecimal number(String target) {
        Object value = outputs.get(target);
        if (!(value instanceof Number)) throw new IllegalArgumentException("Output is not numeric: " + target);
        return new BigDecimal(value.toString());
    }
}
