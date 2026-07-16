package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.repository.OrderStatusRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    @Test
    @WithMockUser(username = "attacker@email.cz", roles = "CUSTOMER")
    void viewOrder_OtherCustomerOrder_ShouldRedirect() throws Exception {
        User attacker = User.builder()
                .email("attacker@email.cz")
                .password("pass")
                .role(User.Role.ROLE_CUSTOMER)
                .firstName("Zlý")
                .lastName("Útočník")
                .active(true)
                .build();
        userRepository.save(attacker);

        User victim = User.builder()
                .email("victim@email.cz")
                .password("password")
                .role(User.Role.ROLE_CUSTOMER)
                .firstName("Oběť")
                .lastName("Testovací")
                .active(true)
                .build();
        userRepository.save(victim);

        OrderStatus testStatus = orderStatusRepository.save(OrderStatus.builder()
                .name("Stav pro test " + System.currentTimeMillis())
                .colorClass("bg-blue-100")
                .displayOrder(99)
                .active(true)
                .build());

        Order victimsOrder = Order.builder()
                .orderNumber("VICTIM-001")
                .customer(victim)
                .deliveryAddress("Testovací ulice")
                .shippingCost(new BigDecimal("150.00"))
                .totalAmount(new BigDecimal("1150.00"))
                .status(testStatus)
                .build();
        orderRepository.save(victimsOrder);

        mockMvc.perform(get("/muj-ucet/objednavka/" + victimsOrder.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/muj-ucet"));
    }
}