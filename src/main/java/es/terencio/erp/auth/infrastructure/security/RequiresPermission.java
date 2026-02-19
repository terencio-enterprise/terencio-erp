package es.terencio.erp.auth.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;

/**
 * Enforces fine-grained, scope-aware access control on controller methods.
 *
 * <p>
 * The aspect resolves the target UUID from method parameters (by name) or via
 * reflection inside DTO objects, then delegates to
 * {@link es.terencio.erp.auth.domain.service.RuntimePermissionService}.
 *
 * @see GranularSecurityAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /** The permission required. */
    Permission permission();

    /** The scope against which the target UUID is evaluated. */
    AccessScope scope();

    /**
     * Name of the method parameter (or DTO field) containing the target UUID.
     * Defaults to {@code "targetId"}.
     */
    String targetIdParam() default "targetId";
}
