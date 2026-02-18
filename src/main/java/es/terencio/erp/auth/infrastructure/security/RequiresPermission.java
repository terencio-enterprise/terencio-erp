package es.terencio.erp.auth.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import es.terencio.erp.auth.domain.model.AccessScope;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String permission();

    AccessScope scope();

    String targetIdParam() default "targetId";
}
