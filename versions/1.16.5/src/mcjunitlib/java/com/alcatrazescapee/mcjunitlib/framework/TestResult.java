package com.alcatrazescapee.mcjunitlib.framework;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class TestResult
{
    static Optional<TestResult> success()
    {
        return Optional.of(new TestResult(Collections.emptyList(), true));
    }

    static Optional<TestResult> fail(List<String> errors)
    {
        return Optional.of(new TestResult(errors, false));
    }

    private final List<String> errors;
    private final boolean success;

    TestResult(List<String> errors, boolean success)
    {
        this.errors = errors;
        this.success = success;
    }

    boolean isSuccess()
    {
        return success;
    }

    List<String> getErrors()
    {
        return errors;
    }
}
