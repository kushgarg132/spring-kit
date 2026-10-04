package io.github.kushgarg132.kit.error;

import java.util.Map;

public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleViolationException(String message, Map<String, String> details) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, details);
    }
}
