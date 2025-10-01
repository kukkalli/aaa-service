package com.kukkalli.aaa.audit;

import java.lang.annotation.*;

/**
 * Mark service methods to be auto-audited after successful execution.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    /** Action name, e.g., "ROLE_CREATE", "PERMISSION_GRANT" */
    String action();

    /** Optional target type, e.g., "ROLE", "USER" (informational) */
    String targetType() default "";

    /** Optional string ID for the target (you can fill via SpEL later if needed) */
    String targetId() default "";
}
