package org.tap4j.plugin.model;

import java.util.Map;

public final class CaseResult {

    private final SuiteResult parent;
    private final Boolean todoIsFailure;
    private float duration;
    private int number;
    private String name;
    private String comment;
    private Map<String, Object> diagnostics;
    private String errorStackTrace;

    public CaseResult(SuiteResult parent, int number, String name, String errorStackTrace, Boolean todoIsFailure) {
        this.parent = parent;
        this.number = number;
        this.name = name;
        this.errorStackTrace = errorStackTrace;
        this.todoIsFailure = todoIsFailure;
    }

    public Map<String, Object> getDiagnostics() {
        return diagnostics;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isSkipped() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isFailure() {
        // TODO Auto-generated method stub
        // FIXME: remember to use todoIsFailure
        return false;
    }

    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

}
