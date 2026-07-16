package org.example.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.model.Product;
import org.example.model.RecipeItem;
import org.example.model.StockMovement;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.RecipeItemRepository;
import org.example.repository.StockMovementRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "SKLAD";

    public List<StockMovement> getFilteredMovements(Long productId, Long userId, String direction,
                                                    Double minQty, Double maxQty, String dateRange) {
        return stockMovementRepository.findAll((Specification<StockMovement>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) predicates.add(cb.equal(root.get("product").get("id"), productId));
            if (userId != null) predicates.add(cb.equal(root.get("performedBy").get("id"), userId));

            if (direction != null && !direction.isEmpty()) {
                if ("positive".equals(direction)) {
                    predicates.add(root.get("type").in(
                            StockMovement.MovementType.RECEIPT,
                            StockMovement.MovementType.ADJUSTMENT_PLUS,
                            StockMovement.MovementType.PRODUCTION_IN
                    ));
                } else if ("negative".equals(direction)) {
                    predicates.add(root.get("type").in(
                            StockMovement.MovementType.SALE,
                            StockMovement.MovementType.ISSUE,
                            StockMovement.MovementType.ADJUSTMENT_MINUS,
                            StockMovement.MovementType.PRODUCTION_OUT
                    ));
                }
            }

            if (minQty != null) predicates.add(cb.ge(root.get("quantity"), minQty));
            if (maxQty != null) predicates.add(cb.le(root.get("quantity"), maxQty));

            if (dateRange != null && !dateRange.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateRange) {
                    case "today" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.toLocalDate().atStartOfDay()));
                    case "week" -> predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.minusWeeks(1)));
                    case "month" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.withDayOfMonth(1).toLocalDate().atStartOfDay()));
                    case "year" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.withDayOfYear(1).toLocalDate().atStartOfDay()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    @Transactional
    public void addMovement(StockMovement movement) {
        recordMovement(
                movement.getProduct().getId(),
                movement.getQuantity(),
                movement.getType(),
                movement.getNote(),
                movement.getPerformedBy()
        );
    }

    /**
     * Dedikovaná metoda pro zadání produktu do výroby.
     */
    @Transactional
    public void produceProduct(Long productId, double quantity, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nenalezen."));

        if (product.getType() != Product.ProductType.PRODUCT) {
            throw new IllegalArgumentException("Do výroby lze zadat pouze produkty typu PRODUCT.");
        }

        recordMovement(productId, quantity, StockMovement.MovementType.PRODUCTION_IN, "Výroba", user);
    }

    @Transactional
    public void recordMovement(Long productId, double quantity, StockMovement.MovementType type, String note, User user) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Množství musí být větší než nula.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nenalezen."));

        if (isReduction(type)) {
            // ZMĚNA: Pouze pokud typ není SALE, tak striktně kontrolujeme sklad.
            // Prodej z e-shopu (SALE) může jít do mínusu, čímž vzniká požadavek na výrobu.
            if (type != StockMovement.MovementType.SALE && product.getStockQuantity() < quantity) {
                throw new IllegalStateException("Na skladě není dostatek položky: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - quantity);
        } else {
            product.setStockQuantity(product.getStockQuantity() + quantity);
        }

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .quantity(quantity)
                .type(type)
                .note(note)
                .performedBy(user)
                .timestamp(LocalDateTime.now())
                .build();

        movement = stockMovementRepository.save(movement);

        auditService.log(MODULE_NAME, "Pohyb zásob (" + type.name() + ")",
                "Proveden pohyb u produktu '" + product.getName() + "' (ID: " + productId + ") o " + quantity + " ks. Důvod: " + note);

        if (type == StockMovement.MovementType.PRODUCTION_IN) {
            processRecipeDeduction(product, quantity, movement, user);
            auditService.log(MODULE_NAME, "Automatický odpočet",
                    "Automaticky odepsány suroviny pro výrobu " + quantity + " ks produktu '" + product.getName() + "'.");
        }
    }

    private void processRecipeDeduction(Product finishedProduct, double producedQuantity, StockMovement parentMovement, User user) {
        List<RecipeItem> recipeItems = recipeItemRepository.findByProduct(finishedProduct);

        if (recipeItems.isEmpty()) {
            return;
        }

        List<Product> materialsToUpdate = new ArrayList<>();
        List<StockMovement> automaticMovements = new ArrayList<>();

        for (RecipeItem item : recipeItems) {
            Product material = item.getMaterial();
            double amountToDeduct = item.getQuantity() * producedQuantity;

            // U surovin potřebných do výroby striktní kontrola stále platí
            if (material.getStockQuantity() < amountToDeduct) {
                throw new IllegalStateException(String.format(
                        "Nedostatek suroviny '%s' pro výrobu %s ks produktu '%s'. Chybí: %s",
                        material.getName(), producedQuantity, finishedProduct.getName(), (amountToDeduct - material.getStockQuantity())
                ));
            }

            material.setStockQuantity(material.getStockQuantity() - amountToDeduct);
            materialsToUpdate.add(material);

            StockMovement materialMovement = StockMovement.builder()
                    .product(material)
                    .quantity(amountToDeduct)
                    .type(StockMovement.MovementType.PRODUCTION_OUT)
                    .note("Automatický výdej pro výrobu: " + finishedProduct.getName() + " (Vygenerováno pohybem ID: " + parentMovement.getId() + ")")
                    .performedBy(user)
                    .timestamp(LocalDateTime.now())
                    .build();

            automaticMovements.add(materialMovement);
        }

        productRepository.saveAll(materialsToUpdate);
        stockMovementRepository.saveAll(automaticMovements);
    }

    private boolean isReduction(StockMovement.MovementType type) {
        return type == StockMovement.MovementType.SALE ||
                type == StockMovement.MovementType.ISSUE ||
                type == StockMovement.MovementType.ADJUSTMENT_MINUS ||
                type == StockMovement.MovementType.PRODUCTION_OUT;
    }
}