package org.example.controller;

import org.example.model.Coupon;
import org.example.repository.CouponRepository;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.example.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCouponController.class)
class AdminCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponRepository couponRepository;
    @MockitoBean
    private CouponService couponService;
    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnListViewWhenListCouponsCalled() throws Exception {
        given(couponService.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/coupon-list"))
                .andExpect(model().attributeExists("coupons"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowCreateFormWhenCreateCalled() throws Exception {
        mockMvc.perform(get("/admin/coupons/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/coupon-form"))
                .andExpect(model().attributeExists("coupon"))
                .andExpect(model().attributeExists("discountTypes"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectToCouponsWhenSaveSuccessful() throws Exception {
        mockMvc.perform(post("/admin/coupons/save")
                        .param("code", "SALE20")
                        .param("type", "PERCENTAGE")
                        .param("discountValue", "20")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/coupons"))
                .andExpect(flash().attribute("successMessage", "Kupón byl úspěšně uložen."));

        verify(couponService).save(any(Coupon.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectToCouponsAfterDeletion() throws Exception {
        mockMvc.perform(get("/admin/coupons/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/coupons"))
                .andExpect(flash().attribute("successMessage", "Kupón byl smazán."));

        verify(couponService).delete(eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnErrorMessageWhenDeletionFails() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("Error")).when(couponService).delete(1L);

        mockMvc.perform(get("/admin/coupons/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/coupons"))
                .andExpect(flash().attribute("errorMessage", "Kupón nelze smazat."));
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowEditFormWhenEditCalled() throws Exception {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        given(couponService.findById(1L)).willReturn(coupon);
        given(productRepository.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/coupons/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/coupon-form"))
                .andExpect(model().attributeExists("coupon"))
                .andExpect(model().attributeExists("discountTypes"))
                .andExpect(model().attributeExists("products"));
    }
}