package org.tap4j.plugin.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tap4j.model.Plan;

/**
 * A TAP test suite. Equivalent to a TAP Test Set. May contain only Case Results..
 * @since 2.1
 */
public final class SuiteResult {

    private final String file;
    private float duration;

    private Plan plan = null;

    private final List<CaseResult> cases = new ArrayList<CaseResult>();

    public SuiteResult(File tapFile) {
        file = tapFile.getAbsolutePath();
        duration = 0.0f;
    }

    public void addCase(CaseResult caseResult) {
        cases.add(caseResult);
    }

    public float getDuration() {
        return this.duration;
    }

    public List<CaseResult> getCases() {
        return Collections.unmodifiableList(cases);
    }

    public Plan getPlan() {
        return plan;
    }

}
