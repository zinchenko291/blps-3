package me.zinch.itmo.mts.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import me.zinch.itmo.mts.domain.entity.Product;

@Service
public class ProductCatalogInitializer implements ApplicationRunner {

    private static final String PRODUCTS_RESOURCE = "catalog-products.json";

    private final EntityManager entityManager;
    private final TransactionTemplate jtaTransactionTemplate;
    private final ObjectMapper objectMapper;

    public ProductCatalogInitializer(
            EntityManager entityManager,
            TransactionTemplate jtaTransactionTemplate,
            ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.jtaTransactionTemplate = jtaTransactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        jtaTransactionTemplate.executeWithoutResult(status -> {
            loadProducts().forEach(product -> {
                Product existing = entityManager.find(Product.class, product.id());
                if (existing == null) {
                    entityManager.persist(product.toEntity());
                } else if (existing.getServiceCode() == null) {
                    existing.setServiceCode(product.code());
                }
            });
        });
    }

    private List<CatalogProduct> loadProducts() {
        try (var input = new ClassPathResource(PRODUCTS_RESOURCE).getInputStream()) {
            return List.of(objectMapper.readValue(input, CatalogProduct[].class));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load product catalog from " + PRODUCTS_RESOURCE, exception);
        }
    }

    private record CatalogProduct(UUID id, String code, String name, String description, BigDecimal price) {
        Product toEntity() {
            return Product.builder()
                    .id(id)
                    .serviceCode(code)
                    .name(name)
                    .description(description)
                    .price(price)
                    .build();
        }
    }
}
