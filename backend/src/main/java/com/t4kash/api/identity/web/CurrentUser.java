package com.t4kash.api.identity.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves the authenticated caller from the Authorization header.
 * When {@link #role()} is set, the request is rejected with 403 unless the
 * caller has that role - replaces the old "requireRole" boilerplate that was
 * copy-pasted into every controller method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
    String role() default "";
}
