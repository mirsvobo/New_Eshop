package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.OrderStatus;
import org.example.repository.OrderStatusRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderStatusRepository statusRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "SYSTÉM";

    @Cacheable(value = "orderStatuses", key = "#root.methodName")
    public List<OrderStatus> getAllOrdered() {
        return statusRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Cacheable(value = "orderStatuses")
    public OrderStatus findById(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stav objednávky nenalezen."));
    }

    @CacheEvict(value = "orderStatuses", allEntries = true)
    @Transactional
    public void save(OrderStatus orderStatus) {
        boolean isNew = (orderStatus.getId() == null);
        statusRepository.save(orderStatus);

        String action = isNew ? "Nový stav objednávky" : "Úprava stavu objednávky";
        auditService.log(MODULE_NAME, action, "Zpracován stav objednávky: " + orderStatus.getName() + ".");
    }

    @CacheEvict(value = "orderStatuses", allEntries = true)
    @Transactional
    public void toggleActive(Long id) {
        OrderStatus status = findById(id);
        status.setActive(!status.isActive());
        statusRepository.save(status);

        String newState = status.isActive() ? "Aktivován" : "Deaktivován";
        auditService.log(MODULE_NAME, "Aktivace/Deaktivace stavu",
                "Stav objednávky '" + status.getName() + "' byl " + newState.toLowerCase() + ".");
    }
}