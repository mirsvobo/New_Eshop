package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.AuditLog;
import org.example.model.User;
import org.example.repository.AuditLogRepository;
import org.example.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(
            String module,
            String action,
            String details
    ) {
        String currentUserEmail =
                getCurrentUserEmail();

        String ipAddress =
                getClientIp();

        CompletableFuture.runAsync(() -> {
            try {
                User user = null;

                if (currentUserEmail != null) {
                    user = userRepository
                            .findByEmail(currentUserEmail)
                            .orElse(null);
                }

                AuditLog logEntry =
                        AuditLog.builder()
                                .user(user)
                                .module(module)
                                .action(action)
                                .details(details)
                                .ipAddress(ipAddress)
                                .build();

                auditLogRepository.save(logEntry);
            } catch (Exception exception) {
                log.error(
                        "Nepodařilo se uložit audit log pro modul {}: ",
                        module,
                        exception
                );
            }
        });
    }

    private String getCurrentUserEmail() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !authentication
                .getPrincipal()
                .equals("anonymousUser")) {

            return authentication.getName();
        }

        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder
                                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request =
                        attributes.getRequest();

                String xForwardedFor =
                        request.getHeader(
                                "X-Forwarded-For"
                        );

                if (xForwardedFor != null
                        && !xForwardedFor.isEmpty()) {

                    return xForwardedFor
                            .split(",")[0]
                            .trim();
                }

                return request.getRemoteAddr();
            }
        } catch (Exception exception) {
            log.warn(
                    "Nepodařilo se získat IP adresu pro audit log",
                    exception
            );
        }

        return "Neznámá IP";
    }

    public List<AuditLog> getFilteredLogs(
            Long userId,
            String module
    ) {
        boolean hasUser =
                userId != null;

        boolean hasModule =
                module != null
                        && !module.isBlank();

        if (hasUser && hasModule) {
            return auditLogRepository
                    .findByUserIdAndModuleOrderByTimestampDesc(
                            userId,
                            module
                    );
        }

        if (hasUser) {
            return auditLogRepository
                    .findByUserIdOrderByTimestampDesc(
                            userId
                    );
        }

        if (hasModule) {
            return auditLogRepository
                    .findByModuleOrderByTimestampDesc(
                            module
                    );
        }

        return auditLogRepository
                .findAllByOrderByTimestampDesc();
    }

    public List<String> getAllModules() {
        return List.of(
                "DOCHÁZKA",
                "SKLAD",
                "OBJEDNÁVKY",
                "PRODUKTY",
                "MONTÁŽE",
                "UŽIVATELÉ",
                "SYSTÉM"
        );
    }
}