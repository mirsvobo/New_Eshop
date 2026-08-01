package org.example.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.ProductImageLayer;
import org.example.repository.ProductImageLayerRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageLayerService {
    private static final int MAX_OPTION_NAME_LENGTH = 100;
    private static final int DEFAULT_OPTION_SORT_ORDER = -1000;
    private final ProductImageLayerRepository layerRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final WebpImageValidator webpImageValidator;
    @Transactional(readOnly = true)
    public List<ProductImageLayer> getLayersForProduct(Long productId) {
        requireProduct(productId);
        return layerRepository.findAllByProductIdOrderByTypeAscSortOrderAscOptionNameAsc(productId);
    }
    @Transactional(readOnly = true)
    public List<ProductImageLayer> getActiveLayersForProduct(Long productId) {
        requireProduct(productId);
        return layerRepository.findAllByProductIdAndActiveTrueOrderByTypeAscSortOrderAscOptionNameAsc(productId);
    }
    @Transactional(readOnly = true)
    public List<ProductImageLayer> getActiveLayersByType(Long productId, LayerType type) {
        requireProduct(productId);
        requireType(type);
        return layerRepository.findAllByProductIdAndTypeAndActiveTrueOrderBySortOrderAscOptionNameAsc(
                productId,
                type
        );
    }
    @Transactional
    public void ensureDefaultVariants(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Produkt musí být před vytvořením výchozích variant uložen.");
        }
        if (product.getType() != Product.ProductType.PRODUCT) {
            return;
        }
        ensureDefaultVariant(product, LayerType.LAZURE);
        ensureDefaultVariant(product, LayerType.ROOF_COLOR);
    }
    @Transactional
    public ProductImageLayer createLayer(
            Long productId,
            LayerType type,
            String optionName,
            Integer sortOrder,
            boolean active,
            MultipartFile imageFile
    ) {
        Product product = requireProduct(productId);
        LayerType validatedType = requireType(type);
        String normalizedOptionName = normalizeOptionName(optionName);
        assertNotDefaultOption(validatedType, normalizedOptionName);
        assertUniqueOptionName(productId, validatedType, normalizedOptionName, null);
        webpImageValidator.validateProductLayer(imageFile);
        String storedFileName = fileStorageService.storeProductLayer(imageFile);
        boolean rollbackCleanupRegistered = registerDeleteOnRollback(storedFileName);
        try {
            ProductImageLayer layer = ProductImageLayer.builder()
                    .product(product)
                    .type(validatedType)
                    .optionName(normalizedOptionName)
                    .imageUrl(storedFileName)
                    .sortOrder(normalizeSortOrder(sortOrder))
                    .active(active)
                    .build();
            return layerRepository.saveAndFlush(layer);
        } catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered) {
                deleteCompensatingFile(storedFileName, exception);
            }
            throw exception;
        }
    }
    @Transactional
    public ProductImageLayer updateLayer(
            Long productId,
            Long layerId,
            LayerType type,
            String optionName,
            Integer sortOrder,
            boolean active,
            MultipartFile replacementImageFile
    ) {
        ProductImageLayer layer = requireOwnedLayer(productId, layerId);
        assertMutableLayer(layer);
        LayerType validatedType = requireType(type);
        String normalizedOptionName = normalizeOptionName(optionName);
        assertNotDefaultOption(validatedType, normalizedOptionName);
        assertUniqueOptionName(productId, validatedType, normalizedOptionName, layerId);
        String oldFileName = layer.getImageUrl();
        String newFileName = null;
        boolean rollbackCleanupRegistered = false;
        if (replacementImageFile != null && !replacementImageFile.isEmpty()) {
            webpImageValidator.validateProductLayer(replacementImageFile);
            newFileName = fileStorageService.storeProductLayer(replacementImageFile);
            rollbackCleanupRegistered = registerDeleteOnRollback(newFileName);
            layer.setImageUrl(newFileName);
        }
        if (!StringUtils.hasText(layer.getImageUrl())) {
            throw new IllegalArgumentException("Pro obrazovou variantu nahrajte WebP soubor.");
        }
        layer.setType(validatedType);
        layer.setOptionName(normalizedOptionName);
        layer.setSortOrder(normalizeSortOrder(sortOrder));
        layer.setActive(active);
        try {
            ProductImageLayer savedLayer = layerRepository.saveAndFlush(layer);
            if (newFileName != null && !newFileName.equals(oldFileName)) {
                registerDeleteAfterCommit(oldFileName);
            }
            return savedLayer;
        } catch (RuntimeException exception) {
            if (newFileName != null && !rollbackCleanupRegistered) {
                deleteCompensatingFile(newFileName, exception);
            }
            throw exception;
        }
    }
    @Transactional
    public ProductImageLayer setLayerActive(Long productId, Long layerId, boolean active) {
        ProductImageLayer layer = requireOwnedLayer(productId, layerId);
        if (layer.isDefaultOption() && !active) {
            throw new IllegalArgumentException("Výchozí variantu nelze deaktivovat.");
        }
        layer.setActive(active);
        return layerRepository.saveAndFlush(layer);
    }
    @Transactional
    public void deleteLayer(Long productId, Long layerId) {
        ProductImageLayer layer = requireOwnedLayer(productId, layerId);
        assertMutableLayer(layer);
        String storedFileName = layer.getImageUrl();
        layerRepository.delete(layer);
        layerRepository.flush();
        registerDeleteAfterCommit(storedFileName);
    }
    @Transactional(readOnly = true)
    public String validateAndResolveSelection(
            Long productId,
            LayerType type,
            String requestedOptionName
    ) {
        requireProduct(productId);
        LayerType validatedType = requireType(type);
        boolean hasActiveOptions = layerRepository.existsByProductIdAndTypeAndActiveTrue(
                productId,
                validatedType
        );
        if (!StringUtils.hasText(requestedOptionName)) {
            if (hasActiveOptions) {
                throw new IllegalArgumentException(missingSelectionMessage(validatedType));
            }
            return null;
        }
        String normalizedOptionName = requestedOptionName.trim();
        return layerRepository
                .findFirstByProductIdAndTypeAndActiveTrueAndOptionNameIgnoreCase(
                        productId,
                        validatedType,
                        normalizedOptionName
                )
                .map(ProductImageLayer::getOptionName)
                .orElseThrow(() -> new IllegalArgumentException(invalidSelectionMessage(validatedType)));
    }
    private Product requireProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("ID produktu nesmí být prázdné.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nebyl nalezen."));
    }
    private ProductImageLayer requireOwnedLayer(Long productId, Long layerId) {
        requireProduct(productId);
        if (layerId == null) {
            throw new IllegalArgumentException("ID obrazové vrstvy nesmí být prázdné.");
        }
        return layerRepository.findByIdAndProductId(layerId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Obrazová vrstva nebyla nalezena nebo nepatří zadanému produktu."
                ));
    }
    private LayerType requireType(LayerType type) {
        if (type == null) {
            throw new IllegalArgumentException("Vyberte typ obrazové vrstvy.");
        }
        return type;
    }
    private String normalizeOptionName(String optionName) {
        if (!StringUtils.hasText(optionName)) {
            throw new IllegalArgumentException("Název varianty nesmí být prázdný.");
        }
        String normalizedOptionName = optionName.trim();
        if (normalizedOptionName.length() > MAX_OPTION_NAME_LENGTH) {
            throw new IllegalArgumentException("Název varianty může mít nejvýše 100 znaků.");
        }
        return normalizedOptionName;
    }
    private int normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : Math.max(0, sortOrder);
    }
    private void ensureDefaultVariant(Product product, LayerType type) {
        String defaultOptionName = type.getDefaultOptionName();
        ProductImageLayer layer = layerRepository
                .findFirstByProductIdAndTypeAndOptionNameIgnoreCase(
                        product.getId(),
                        type,
                        defaultOptionName
                )
                .orElseGet(() -> ProductImageLayer.builder()
                        .product(product)
                        .type(type)
                        .optionName(defaultOptionName)
                        .build());
        String obsoleteFileName = layer.getImageUrl();
        layer.setType(type);
        layer.setOptionName(defaultOptionName);
        layer.setImageUrl(null);
        layer.setSortOrder(DEFAULT_OPTION_SORT_ORDER);
        layer.setActive(true);
        layerRepository.saveAndFlush(layer);
        if (StringUtils.hasText(obsoleteFileName)) {
            registerDeleteAfterCommit(obsoleteFileName);
        }
    }
    private void assertNotDefaultOption(LayerType type, String optionName) {
        if (type.isDefaultOption(optionName)) {
            throw new IllegalArgumentException(
                    "Výchozí varianta " + type.getDefaultOptionName()
                            + " vzniká automaticky a nepoužívá WebP soubor."
            );
        }
    }
    private void assertMutableLayer(ProductImageLayer layer) {
        if (layer.isDefaultOption()) {
            throw new IllegalArgumentException(
                    "Výchozí variantu nelze upravit ani odstranit."
            );
        }
    }
    private void assertUniqueOptionName(
            Long productId,
            LayerType type,
            String optionName,
            Long excludedLayerId
    ) {
        boolean duplicate = excludedLayerId == null
                ? layerRepository.existsByProductIdAndTypeAndOptionNameIgnoreCase(
                productId,
                type,
                optionName
        )
                : layerRepository.existsByProductIdAndTypeAndOptionNameIgnoreCaseAndIdNot(
                productId,
                type,
                optionName,
                excludedLayerId
        );
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Varianta s tímto názvem již pro vybraný produkt a typ existuje."
            );
        }
    }
    private String missingSelectionMessage(LayerType type) {
        return type == LayerType.LAZURE
                ? "Vyberte dostupnou lazuru produktu."
                : "Vyberte dostupnou barvu střechy produktu.";
    }
    private String invalidSelectionMessage(LayerType type) {
        return type == LayerType.LAZURE
                ? "Vybraná lazura není pro tento produkt dostupná."
                : "Vybraná barva střechy není pro tento produkt dostupná.";
    }
    private boolean registerDeleteOnRollback(String storedFileName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteFileSafely(storedFileName, "vrácení databázové transakce");
                }
            }
        });
        return true;
    }
    private void registerDeleteAfterCommit(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFileSafely(storedFileName, "dokončení operace bez aktivní synchronizace transakce");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFileSafely(storedFileName, "potvrzení databázové transakce");
            }
        });
    }
    private void deleteCompensatingFile(String storedFileName, RuntimeException originalException) {
        try {
            fileStorageService.deleteFile(storedFileName);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.error(
                    "Databázová operace selhala a nově uložený soubor vrstvy {} se nepodařilo odstranit.",
                    storedFileName,
                    cleanupException
            );
        }
    }
    private void deleteFileSafely(String storedFileName, String operationContext) {
        try {
            fileStorageService.deleteFile(storedFileName);
        } catch (RuntimeException exception) {
            log.error(
                    "Soubor obrazové vrstvy {} se nepodařilo odstranit po operaci: {}. Databázový stav zůstal platný, soubor vyžaduje ruční úklid.",
                    storedFileName,
                    operationContext,
                    exception
            );
        }
    }
}
