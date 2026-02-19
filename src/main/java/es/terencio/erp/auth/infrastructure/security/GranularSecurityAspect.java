package es.terencio.erp.auth.infrastructure.security;

import java.lang.reflect.Method;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import es.terencio.erp.auth.domain.service.RuntimePermissionService;

/**
 * AOP aspect that enforces {@link RequiresPermission} on controller methods.
 *
 * <p>
 * Target ID resolution order:
 * <ol>
 * <li>Direct UUID/String method parameter matching {@code targetIdParam} by
 * name.</li>
 * <li>DTO field/accessor reflection: if the parameter name doesn't match
 * directly,
 * the aspect inspects all non-JDK arguments looking for a getter or record
 * accessor named {@code targetIdParam} (or {@code get<Param>}) that returns a
 * UUID.</li>
 * </ol>
 */
@Aspect
@Component
public class GranularSecurityAspect {

    private final RuntimePermissionService permissionService;

    public GranularSecurityAspect(RuntimePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        if (auth.getPrincipal() instanceof CustomUserDetails user) {
            UUID targetId = extractTargetId(joinPoint, requiresPermission.targetIdParam());

            if (targetId == null) {
                throw new AccessDeniedException("Target ID is null or missing for permission check");
            }

            boolean hasPermission = permissionService.hasPermission(
                    user.getId(),
                    requiresPermission.permission().getCode(),
                    targetId,
                    requiresPermission.scope());

            if (!hasPermission) {
                throw new AccessDeniedException(String.format(
                        "Access Denied: Missing permission '%s' for target '%s' with scope '%s'",
                        requiresPermission.permission(),
                        targetId,
                        requiresPermission.scope()));
            }
            return;
        }

        throw new AccessDeniedException("Authenticated principal is not a valid user context");
    }

    /**
     * Resolves the target UUID from method arguments.
     *
     * <p>
     * Strategy:
     * <ol>
     * <li>Look for a parameter whose name exactly matches {@code paramName} and is
     * a UUID or String.</li>
     * <li>Fall back to DTO reflection: iterate all non-JDK args, try to invoke
     * {@code <paramName>()} (record accessor) or {@code get<ParamName>()} (JavaBean
     * getter).</li>
     * </ol>
     */
    private UUID extractTargetId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 1. Direct parameter match by name
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof UUID uuid) {
                    return uuid;
                } else if (arg instanceof String s) {
                    return UUID.fromString(s);
                }
            }
        }

        // 2. DTO reflection: look inside non-JDK objects for a matching accessor
        for (Object arg : args) {
            if (arg == null)
                continue;
            String pkg = arg.getClass().getPackageName();
            // Skip JDK types, Spring types, and primitives
            if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("org.springframework.")) {
                continue;
            }

            UUID found = tryExtractFromObject(arg, paramName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Attempts to read a UUID field from {@code obj} using a record-style accessor
     * ({@code paramName()}) or a JavaBean getter ({@code get<ParamName>()}).
     */
    private UUID tryExtractFromObject(Object obj, String paramName) {
        Class<?> clazz = obj.getClass();

        // Record accessor: companyId() / storeId() etc.
        try {
            Method accessor = clazz.getMethod(paramName);
            Object value = accessor.invoke(obj);
            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (NoSuchMethodException ignored) {
            // fall through to getter
        } catch (Exception e) {
            // Reflection error — don't block the call but skip this arg
            return null;
        }

        // JavaBean getter: getCompanyId() etc.
        String getterName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
        try {
            Method getter = clazz.getMethod(getterName);
            Object value = getter.invoke(obj);
            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (NoSuchMethodException ignored) {
            // no such getter
        } catch (Exception e) {
            return null;
        }

        return null;
    }
}
