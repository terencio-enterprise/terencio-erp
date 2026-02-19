package es.terencio.erp.auth.infrastructure.config.security.aop;

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
import es.terencio.erp.auth.application.service.PermissionService;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;

@Aspect
@Component
public class GranularSecurityAspect {

    private final PermissionService permissionService;

    public GranularSecurityAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("User not authenticated");

        if (auth.getPrincipal() instanceof CustomUserDetails user) {
            UUID targetId = extractTargetId(joinPoint, requiresPermission.targetIdParam());
            if (targetId == null) throw new AccessDeniedException("Target ID is null or missing for permission check");

            boolean hasPermission = permissionService.hasPermission(user.getId(), requiresPermission.permission().getCode(), targetId, requiresPermission.scope());
            if (!hasPermission) {
                throw new AccessDeniedException(String.format("Access Denied: Missing permission '%s' for target '%s' with scope '%s'", requiresPermission.permission(), targetId, requiresPermission.scope()));
            }
            return;
        }
        throw new AccessDeniedException("Authenticated principal is not a valid user context");
    }

    private UUID extractTargetId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                if (args[i] instanceof UUID uuid) return uuid;
                if (args[i] instanceof String s) return UUID.fromString(s);
            }
        }

        for (Object arg : args) {
            if (arg == null) continue;
            String pkg = arg.getClass().getPackageName();
            if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("org.springframework.")) continue;
            UUID found = tryExtractFromObject(arg, paramName);
            if (found != null) return found;
        }
        return null;
    }

    private UUID tryExtractFromObject(Object obj, String paramName) {
        Class<?> clazz = obj.getClass();
        try {
            Method accessor = clazz.getMethod(paramName);
            Object value = accessor.invoke(obj);
            if (value instanceof UUID uuid) return uuid;
        } catch (Exception ignored) {}

        String getterName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
        try {
            Method getter = clazz.getMethod(getterName);
            Object value = getter.invoke(obj);
            if (value instanceof UUID uuid) return uuid;
        } catch (Exception ignored) {}

        return null;
    }
}
