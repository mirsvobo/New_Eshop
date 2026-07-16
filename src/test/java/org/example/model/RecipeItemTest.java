package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipeItemTest {

    @Test
    void recipeItem_Linking_WorksCorrectly() {
        Product finalProduct = Product.builder()
                .name("Stůl")
                .type(Product.ProductType.PRODUCT)
                .build();

        Product material = Product.builder()
                .name("Dřevo")
                .unit("m3")
                .type(Product.ProductType.MATERIAL)
                .build();

        RecipeItem item = RecipeItem.builder()
                .product(finalProduct)
                .material(material)
                .quantity(2)
                .build();

        assertEquals("Stůl", item.getProduct().getName());
        assertEquals("Dřevo", item.getMaterial().getName());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void recipeItem_Values_MatchBuilder() {
        RecipeItem item = new RecipeItem();
        item.setQuantity(10);

        assertEquals(10, item.getQuantity());
    }
}