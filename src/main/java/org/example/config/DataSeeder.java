package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final TaxRateRepository taxRateRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Zahajuji deterministické seedování dat pro systém FajnDřevo s.r.o...");

        clearDatabase();

        Map<String, OrderStatus> statuses = seedOrderStatuses();
        Map<String, TaxRate> taxes = seedTaxRates();
        Map<String, User> employees = seedEmployees();
        Map<String, User> customers = seedCustomers();

        Map<String, Product> materials = seedMaterials(taxes.get("Základní"));
        Map<String, Product> products = seedFinishedProducts(taxes.get("Základní"));

        seedRecipes(products, materials);
        seedInitialStockMovements(products, materials, employees.get("ceo"));

        seedAttendanceHistory(employees);
        seedOrders(customers, products, statuses);
        seedFinalAudit(employees.get("ceo"));

        log.info("Seedování dokončeno. Databáze obsahuje pevně definovanou sadu testovacích dat.");
    }

    private void clearDatabase() {
        log.debug("Čištění databáze...");
        auditLogRepository.deleteAll();
        stockMovementRepository.deleteAll();
        attendanceRepository.deleteAll();
        recipeItemRepository.deleteAll();
        orderRepository.deleteAll();
        orderStatusRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        taxRateRepository.deleteAll();
    }

    private Map<String, OrderStatus> seedOrderStatuses() {
        Map<String, OrderStatus> map = new HashMap<>();
        map.put("NEW", orderStatusRepository.save(OrderStatus.builder().name("Nová").colorClass("bg-blue-100 text-blue-800").displayOrder(1).active(true).build()));
        map.put("PAY", orderStatusRepository.save(OrderStatus.builder().name("Čeká na platbu").colorClass("bg-amber-100 text-amber-800").displayOrder(2).active(true).build()));
        map.put("PROC", orderStatusRepository.save(OrderStatus.builder().name("Zpracovává se").colorClass("bg-indigo-100 text-indigo-800").displayOrder(3).active(true).build()));
        map.put("PROD", orderStatusRepository.save(OrderStatus.builder().name("Výroba").colorClass("bg-cyan-100 text-cyan-800").displayOrder(4).active(true).build()));
        map.put("PACK", orderStatusRepository.save(OrderStatus.builder().name("Balení").colorClass("bg-orange-100 text-orange-800").displayOrder(5).active(true).build()));
        map.put("SHIP", orderStatusRepository.save(OrderStatus.builder().name("Odesláno").colorClass("bg-purple-100 text-purple-800").displayOrder(6).active(true).build()));
        map.put("DONE", orderStatusRepository.save(OrderStatus.builder().name("Doručeno").colorClass("bg-green-100 text-green-800").displayOrder(7).active(true).build()));
        map.put("CANCEL", orderStatusRepository.save(OrderStatus.builder().name("Zrušeno").colorClass("bg-red-100 text-red-800").displayOrder(8).active(true).build()));
        return map;
    }

    private Map<String, TaxRate> seedTaxRates() {
        Map<String, TaxRate> map = new HashMap<>();
        map.put("Základní", taxRateRepository.save(TaxRate.builder().name("Základní sazba").rate(new BigDecimal("21.0")).defaultRate(true).build()));
        map.put("Snížená", taxRateRepository.save(TaxRate.builder().name("Snížená sazba").rate(new BigDecimal("12.0")).build()));
        map.put("Nula", taxRateRepository.save(TaxRate.builder().name("Osvobozeno").rate(BigDecimal.ZERO).build()));
        return map;
    }

    private Map<String, User> seedEmployees() {
        String pass = passwordEncoder.encode("heslo123");
        Map<String, User> map = new HashMap<>();
        map.put("ceo", userRepository.save(User.builder().email("svoboda@fajndrevo.cz").password(pass).firstName("Miroslav").lastName("Svoboda").role(User.Role.ROLE_ADMIN).pin("1111").active(true).build()));
        map.put("t1", userRepository.save(User.builder().email("novak@fajndrevo.cz").password(pass).firstName("Jan").lastName("Novák").role(User.Role.ROLE_EMPLOYEE).pin("2222").active(true).build()));
        map.put("t2", userRepository.save(User.builder().email("dvorak@fajndrevo.cz").password(pass).firstName("Petr").lastName("Dvořák").role(User.Role.ROLE_EMPLOYEE).pin("3333").active(true).build()));
        map.put("logist", userRepository.save(User.builder().email("truhlar@fajndrevo.cz").password(pass).firstName("Josef").lastName("Truhlář").role(User.Role.ROLE_EMPLOYEE).pin("4444").active(true).build()));
        map.put("admin", userRepository.save(User.builder().email("novotna@fajndrevo.cz").password(pass).firstName("Hana").lastName("Novotná").role(User.Role.ROLE_EMPLOYEE).pin("5555").active(true).build()));
        return map;
    }

    private Map<String, User> seedCustomers() {
        String pass = passwordEncoder.encode("heslo123");
        Map<String, User> map = new HashMap<>();

        map.put("c1", userRepository.save(User.builder().email("info@architekti-brno.cz").password(pass).firstName("Ing. Marek").lastName("Kopecký").companyName("Architekti Brno s.r.o.").ico("12345678").dic("CZ12345678").role(User.Role.ROLE_CUSTOMER).active(true).build()));
        map.put("c2", userRepository.save(User.builder().email("hotel-slunce@gastromail.cz").password(pass).firstName("Provozní").lastName("Hotel Slunce").companyName("Hotel Slunce a.s.").ico("87654321").dic("CZ87654321").role(User.Role.ROLE_CUSTOMER).active(true).build()));
        map.put("c3", userRepository.save(User.builder().email("jan.novak@stavby-novak.cz").password(pass).firstName("Jan").lastName("Novák").companyName("Stavby Novák").role(User.Role.ROLE_CUSTOMER).active(true).build()));
        map.put("c4", userRepository.save(User.builder().email("petr.vesely@email.cz").password(pass).firstName("Petr").lastName("Veselý").role(User.Role.ROLE_CUSTOMER).active(true).build()));
        map.put("c5", userRepository.save(User.builder().email("lucie.kratka@seznam.cz").password(pass).firstName("Lucie").lastName("Krátká").role(User.Role.ROLE_CUSTOMER).active(true).build()));
        map.put("c6", userRepository.save(User.builder().email("martin.svoboda@gmail.com").password(pass).firstName("Martin").lastName("Svoboda").role(User.Role.ROLE_CUSTOMER).active(true).build()));

        return map;
    }

    private Map<String, Product> seedMaterials(TaxRate vat) {
        Map<String, Product> map = new HashMap<>();
        // Jednotky upraveny na kusy/balení, aby celočíselné vazby v kusovnících dávaly smysl
        map.put("dub", productRepository.save(createItem("Dubová fošna 50x200x2000mm", 1450, "ks", Product.ProductType.MATERIAL, vat, 50)));
        map.put("buk", productRepository.save(createItem("Bukový hranol 80x80x1000mm", 450, "ks", Product.ProductType.MATERIAL, vat, 80)));
        map.put("olej", productRepository.save(createItem("Tvrdý voskový olej Osmo 0.5l", 750, "ks", Product.ProductType.MATERIAL, vat, 25)));
        map.put("lepidlo", productRepository.save(createItem("Lepidlo Titebond III 250ml", 180, "ks", Product.ProductType.MATERIAL, vat, 30)));
        map.put("vruty", productRepository.save(createItem("Vruty do dřeva 4x40 (balení 100ks)", 120, "bal", Product.ProductType.MATERIAL, vat, 15)));
        map.put("podnoz", productRepository.save(createItem("Kovová podnož 'X' černá", 4200, "pár", Product.ProductType.MATERIAL, vat, 10)));
        return map;
    }

    private Map<String, Product> seedFinishedProducts(TaxRate vat) {
        Map<String, Product> map = new HashMap<>();
        map.put("stul", productRepository.save(createItem("Jídelní stůl 'FajnDub' - Masiv", 34500, "ks", Product.ProductType.PRODUCT, vat, 3)));
        map.put("stolek", productRepository.save(createItem("Konferenční stolek 'Industriál'", 14200, "ks", Product.ProductType.PRODUCT, vat, 5)));
        map.put("prkenko", productRepository.save(createItem("Dubové krájecí prkénko PRO", 1650, "ks", Product.ProductType.PRODUCT, vat, 15)));
        map.put("police", productRepository.save(createItem("Nástěnná police 'Minimalist'", 2400, "ks", Product.ProductType.PRODUCT, vat, 10)));
        map.put("podtacky", productRepository.save(createItem("Sada dřevěných podtácků (6ks)", 550, "sada", Product.ProductType.PRODUCT, vat, 20)));
        return map;
    }

    private Product createItem(String name, double price, String unit, Product.ProductType type, TaxRate vat, int qty) {
        return Product.builder().name(name).price(BigDecimal.valueOf(price)).unit(unit).type(type).taxRate(vat).stockQuantity(qty).active(true).build();
    }

    private void seedRecipes(Map<String, Product> p, Map<String, Product> m) {
        // Kusovník: Jídelní stůl
        saveRecipe(p.get("stul"), m.get("dub"), 4);
        saveRecipe(p.get("stul"), m.get("podnoz"), 1);
        saveRecipe(p.get("stul"), m.get("olej"), 2);
        saveRecipe(p.get("stul"), m.get("lepidlo"), 1);
        saveRecipe(p.get("stul"), m.get("vruty"), 1);

        // Kusovník: Konferenční stolek
        saveRecipe(p.get("stolek"), m.get("buk"), 3);
        saveRecipe(p.get("stolek"), m.get("podnoz"), 1);
        saveRecipe(p.get("stolek"), m.get("olej"), 1);
        saveRecipe(p.get("stolek"), m.get("lepidlo"), 1);
        saveRecipe(p.get("stolek"), m.get("vruty"), 1);

        // Kusovník: Prkénko
        saveRecipe(p.get("prkenko"), m.get("dub"), 1);
        saveRecipe(p.get("prkenko"), m.get("olej"), 1);

        // Kusovník: Police
        saveRecipe(p.get("police"), m.get("buk"), 1);
        saveRecipe(p.get("police"), m.get("olej"), 1);
        saveRecipe(p.get("police"), m.get("vruty"), 1);

        // Kusovník: Podtácky
        saveRecipe(p.get("podtacky"), m.get("dub"), 1);
        saveRecipe(p.get("podtacky"), m.get("olej"), 1);
    }

    private void saveRecipe(Product product, Product material, int quantity) {
        recipeItemRepository.save(RecipeItem.builder()
                .product(product)
                .material(material)
                .quantity(quantity)
                .build());
    }

    private void seedInitialStockMovements(Map<String, Product> products, Map<String, Product> materials, User admin) {
        products.values().forEach(p -> stockMovementRepository.save(StockMovement.builder().product(p).performedBy(admin).quantity(p.getStockQuantity()).type(StockMovement.MovementType.ADJUSTMENT_PLUS).note("Počáteční stav - hotové výrobky").build()));
        materials.values().forEach(m -> stockMovementRepository.save(StockMovement.builder().product(m).performedBy(admin).quantity(m.getStockQuantity()).type(StockMovement.MovementType.RECEIPT).note("Příjem materiálu - dodavatel DřevoSklad a.s.").build()));
    }

    private void seedAttendanceHistory(Map<String, User> employees) {
        LocalDate today = LocalDate.now();
        List<User> workers = List.of(employees.get("t1"), employees.get("t2"), employees.get("logist"));


        int daysAdded = 0;
        int daysToSubtract = 1;

        while (daysAdded < 3) {
            LocalDate date = today.minusDays(daysToSubtract);
            if (date.getDayOfWeek().getValue() <= 5) {
                for (User emp : workers) {
                    LocalDateTime in = date.atTime(LocalTime.of(7, 30));
                    LocalDateTime out = date.atTime(LocalTime.of(16, 0));

                    attendanceRepository.save(AttendanceRecord.builder().employee(emp).timestamp(in).type(AttendanceRecord.AttendanceType.CLOCK_IN).build());
                    attendanceRepository.save(AttendanceRecord.builder().employee(emp).timestamp(in.plusHours(4)).type(AttendanceRecord.AttendanceType.BREAK_START).build());
                    attendanceRepository.save(AttendanceRecord.builder().employee(emp).timestamp(in.plusHours(4).plusMinutes(30)).type(AttendanceRecord.AttendanceType.BREAK_END).build());
                    attendanceRepository.save(AttendanceRecord.builder().employee(emp).timestamp(out).type(AttendanceRecord.AttendanceType.CLOCK_OUT).build());
                }
                daysAdded++;
            }
            daysToSubtract++;
        }
    }

    private void seedOrders(Map<String, User> customers, Map<String, Product> p, Map<String, OrderStatus> s) {
        LocalDateTime now = LocalDateTime.now();

        createOrder("ORD-2024-00001", customers.get("c1"), s.get("DONE"), now.minusDays(14),
                Map.of(p.get("stul"), 2, p.get("podtacky"), 4));

        createOrder("ORD-2024-00002", customers.get("c2"), s.get("SHIP"), now.minusDays(5),
                Map.of(p.get("stolek"), 5));


        createOrder("ORD-2024-00003", customers.get("c4"), s.get("PROD"), now.minusDays(2),
                Map.of(p.get("stul"), 1, p.get("prkenko"), 1));

        createOrder("ORD-2024-00004", customers.get("c5"), s.get("PAY"), now.minusDays(1),
                Map.of(p.get("police"), 3));

        createOrder("ORD-2024-00005", customers.get("c6"), s.get("NEW"), now.minusHours(4),
                Map.of(p.get("podtacky"), 2, p.get("prkenko"), 2));

        createOrder("ORD-2024-00006", customers.get("c3"), s.get("CANCEL"), now.minusDays(10),
                Map.of(p.get("stul"), 1));
    }

    private void createOrder(String orderNumber, User customer, OrderStatus status, LocalDateTime createdAt, Map<Product, Integer> items) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .deliveryAddress(customer.getCompanyName() != null ? customer.getCompanyName() + ", Panská 15, Brno" : "Doma, Květná 12, Praha")
                .billingAddress(customer.getCompanyName() != null ? customer.getCompanyName() + ", Panská 15, Brno" : "Doma, Květná 12, Praha")
                .status(status)
                .shippingCost(new BigDecimal("150.00"))
                .totalAmount(BigDecimal.ZERO)
                .createdAt(createdAt)
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        BigDecimal total = order.getShippingCost();

        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            BigDecimal price = p.getPriceWithTax();

            OrderItem item = OrderItem.builder().order(order).product(p).quantity(qty).unitPrice(price).build();
            order.getItems().add(item);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
        }

        order.setTotalAmount(total);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .note("Pevně definovaná testovací objednávka.")
                .createdAt(createdAt)
                .build();

        order.getStatusHistory().add(history);
        orderRepository.save(order);
    }

    private void seedFinalAudit(User admin) {
        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .module("SYSTÉM")
                .action("INITIAL_SEED")
                .details("Založena deterministická datová sada - pevně definovaní zákazníci, kusovníky a 6 objednávek v různých stavech.")
                .timestamp(LocalDateTime.now())
                .ipAddress("127.0.0.1")
                .build());
    }
}