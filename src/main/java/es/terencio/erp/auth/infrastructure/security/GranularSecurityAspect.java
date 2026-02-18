package es.terencio.erp.auth.infrastructure.security;

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

            // If targetId is null, strict check fails? Or maybe it's not applicable?
            // For now, assume targetId is mandatory for scoped checks.
            if (targetId == null) {
                throw new AccessDeniedException("Target ID is null or missing for permission check");
            }

            boolean hasPermission = permissionService.hasPermission(
                    user.getId(),
                    requiresPermission.permission(),
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

    private UUID extractTargetId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof UUID) {
                    return (UUID) arg;
                } else if (arg instanceof String) {
                    return UUID.fromString((String) arg);
                }
            }
        }
        return null; // Or throw exception if param not found
    }
}
