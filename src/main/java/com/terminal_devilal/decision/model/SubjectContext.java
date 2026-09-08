package com.terminal_devilal.decision.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SubjectContext {
    private final String subjectType;
    private final String subjectId;
    private final LocalDate asOfDate;
    private final Map<String, Object> attributes;

    public SubjectContext(String subjectType, String subjectId, LocalDate asOfDate, Map<String, Object> attributes) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.asOfDate = asOfDate;
        this.attributes = new HashMap<>(attributes == null ? Collections.emptyMap() : attributes);
    }

    public Object getAttribute(String code) {
        return attributes.get(code);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }
    public LocalDate getAsOfDate() { return asOfDate; }
}
