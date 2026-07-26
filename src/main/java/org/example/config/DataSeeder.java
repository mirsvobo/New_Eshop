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
    private final InstallationPostRepository installationPostRepository;
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
        seedInstallationPosts();
        seedFinalAudit(employees.get("ceo"));

        log.info("Seedování dokončeno. Databáze obsahuje pevně definovanou sadu testovacích dat.");
    }

    private void clearDatabase() {
        log.debug("Čistím databázi...");
        auditLogRepository.deleteAll();
        installationPostRepository.deleteAll();
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
        map.put("ocel", productRepository.save(createItem("Ocelový jekl 40x40x2mm (1m)", 150, "m", Product.ProductType.MATERIAL, vat, 500)));
        map.put("prkna", productRepository.save(createItem("Dřevěné smrkové prkno 20x100mm (1m)", 45, "m", Product.ProductType.MATERIAL, vat, 1000)));
        map.put("strecha", productRepository.save(createItem("Plechová krytina - trapéz", 450, "m2", Product.ProductType.MATERIAL, vat, 100)));
        map.put("lazura", productRepository.save(createItem("Ochranná lazura Remmers 2.5l", 1250, "ks", Product.ProductType.MATERIAL, vat, 40)));
        map.put("vruty", productRepository.save(createItem("Konstrukční vruty s úpravou (bal. 500ks)", 350, "bal", Product.ProductType.MATERIAL, vat, 50)));
        map.put("patky", productRepository.save(createItem("Výškově nastavitelná patka žárový zinek", 180, "ks", Product.ProductType.MATERIAL, vat, 200)));
        return map;
    }

    private Map<String, Product> seedFinishedProducts(TaxRate vat) {
        Map<String, Product> map = new HashMap<>();

        Product kompakt = createItem("Dřevník Kompakt", 16898, "ks", Product.ProductType.PRODUCT, vat, 5);
        kompakt.setAvailableLazures("Pinie, Dub, Ořech, Palisandr, Bezbarvý lak");
        kompakt.setAvailableRoofColors("Černá, Antracit, Červenohnědá");
        kompakt.setWidth(100.0);
        kompakt.setDepth(73.0);
        kompakt.setHeight(220.0);
        kompakt.setVolume(1.6);
        kompakt.setAdditionalDimensions("Kompaktní dřevník z kovové konstrukce, který se hodí i na ty nejmenší zahrady.");
        map.put("kompakt", productRepository.save(kompakt));

        Product klasik = createItem("Dřevník Klasik", 23171, "ks", Product.ProductType.PRODUCT, vat, 3);
        klasik.setAvailableLazures("Pinie, Dub, Ořech, Palisandr, Bezbarvý lak");
        klasik.setAvailableRoofColors("Černá, Antracit, Červenohnědá");
        klasik.setWidth(160.0);
        klasik.setDepth(73.0);
        klasik.setHeight(220.0);
        klasik.setVolume(2.5);
        klasik.setAdditionalDimensions("Robustní řešení pro střední zahrady. Spojuje kompaktnost Dřevníku Klasik s větší kapacitou.");
        map.put("klasik", productRepository.save(klasik));

        Product elko = createItem("Dřevník L", 25696, "ks", Product.ProductType.PRODUCT, vat, 2);
        elko.setAvailableLazures("Pinie, Dub, Ořech, Palisandr, Bezbarvý lak");
        elko.setAvailableRoofColors("Černá, Antracit, Červenohnědá");
        elko.setWidth(160.0);
        elko.setDepth(109.0);
        elko.setHeight(220.0);
        elko.setVolume(2.9);
        elko.setAdditionalDimensions("Odolné řešení pro uskladnění až 2,90 m³ dříví. Dřevník L je vyráběn z kvalitní oceli.");
        map.put("elko", productRepository.save(elko));

        Product xxl = createItem("Dřevník XXL", 33224, "ks", Product.ProductType.PRODUCT, vat, 2);
        xxl.setAvailableLazures("Pinie, Dub, Ořech, Palisandr, Bezbarvý lak");
        xxl.setAvailableRoofColors("Černá, Antracit, Červenohnědá");
        xxl.setWidth(260.0);
        xxl.setDepth(109.0);
        xxl.setHeight(220.0);
        xxl.setVolume(6.2);
        xxl.setAdditionalDimensions("Prémiové řešení ochrany velkého množství dříví pro větší zahrady. Před vnějšími vlivy chrání stříška.");
        map.put("xxl", productRepository.save(xxl));

        return map;
    }

    private Product createItem(String name, double price, String unit, Product.ProductType type, TaxRate vat, int qty) {
        return Product.builder()
                .name(name)
                .price(BigDecimal.valueOf(price))
                .unit(unit)
                .type(type)
                .taxRate(vat)
                .stockQuantity(qty)
                .active(true)
                .build();
    }

    private void seedRecipes(Map<String, Product> p, Map<String, Product> m) {
        // Dřevník Kompakt
        saveRecipe(p.get("kompakt"), m.get("ocel"), 18);
        saveRecipe(p.get("kompakt"), m.get("prkna"), 25);
        saveRecipe(p.get("kompakt"), m.get("strecha"), 1);
        saveRecipe(p.get("kompakt"), m.get("lazura"), 1);
        saveRecipe(p.get("kompakt"), m.get("vruty"), 1);
        saveRecipe(p.get("kompakt"), m.get("patky"), 4);

        // Dřevník Klasik
        saveRecipe(p.get("klasik"), m.get("ocel"), 22);
        saveRecipe(p.get("klasik"), m.get("prkna"), 35);
        saveRecipe(p.get("klasik"), m.get("strecha"), 2);
        saveRecipe(p.get("klasik"), m.get("lazura"), 1);
        saveRecipe(p.get("klasik"), m.get("vruty"), 2);
        saveRecipe(p.get("klasik"), m.get("patky"), 4);

        // Dřevník L
        saveRecipe(p.get("elko"), m.get("ocel"), 26);
        saveRecipe(p.get("elko"), m.get("prkna"), 40);
        saveRecipe(p.get("elko"), m.get("strecha"), 2);
        saveRecipe(p.get("elko"), m.get("lazura"), 2);
        saveRecipe(p.get("elko"), m.get("vruty"), 2);
        saveRecipe(p.get("elko"), m.get("patky"), 4);

        // Dřevník XXL
        saveRecipe(p.get("xxl"), m.get("ocel"), 34);
        saveRecipe(p.get("xxl"), m.get("prkna"), 60);
        saveRecipe(p.get("xxl"), m.get("strecha"), 4);
        saveRecipe(p.get("xxl"), m.get("lazura"), 3);
        saveRecipe(p.get("xxl"), m.get("vruty"), 3);
        saveRecipe(p.get("xxl"), m.get("patky"), 6);
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
        materials.values().forEach(m -> stockMovementRepository.save(StockMovement.builder().product(m).performedBy(admin).quantity(m.getStockQuantity()).type(StockMovement.MovementType.RECEIPT).note("Příjem materiálu - dodavatel Kovohutě a Pila a.s.").build()));
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
                Map.of(p.get("xxl"), 1, p.get("kompakt"), 1));
        createOrder("ORD-2024-00002", customers.get("c2"), s.get("SHIP"), now.minusDays(5),
                Map.of(p.get("klasik"), 2));
        createOrder("ORD-2024-00003", customers.get("c4"), s.get("PROD"), now.minusDays(2),
                Map.of(p.get("elko"), 1));
        createOrder("ORD-2024-00004", customers.get("c5"), s.get("PAY"), now.minusDays(1),
                Map.of(p.get("kompakt"), 1));
        createOrder("ORD-2024-00005", customers.get("c6"), s.get("NEW"), now.minusHours(4),
                Map.of(p.get("klasik"), 1, p.get("elko"), 1));
        createOrder("ORD-2024-00006", customers.get("c3"), s.get("CANCEL"), now.minusDays(10),
                Map.of(p.get("xxl"), 2));
    }

    private void createOrder(String orderNumber, User customer, OrderStatus status, LocalDateTime createdAt, Map<Product, Integer> items) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .deliveryAddress(customer.getCompanyName() != null ? customer.getCompanyName() + ", Panská 15, Brno" : "Doma, Květinová 12, Praha")
                .billingAddress(customer.getCompanyName() != null ? customer.getCompanyName() + ", Panská 15, Brno" : "Doma, Květinová 12, Praha")
                .status(status)
                .shippingCost(new BigDecimal("150.00"))
                .totalAmount(BigDecimal.ZERO)
                .createdAt(createdAt)
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .taxMode(TaxMode.STANDARD)
                .affidavitSigned(false)
                .build();

        BigDecimal total = order.getShippingCost();

        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            BigDecimal price = p.getPriceWithTax();
            BigDecimal taxRateValue = p.getTaxRate() != null ? p.getTaxRate().getRate() : BigDecimal.ZERO;

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(qty)
                    .unitPrice(price)
                    .actualTaxRate(taxRateValue)
                    .build();

            // Automatické navolení variant u testovacích objednávek pro ukázku
            if (p.getName().contains("Dřevník")) {
                item.setSelectedLazure("Ořech");
                item.setSelectedRoofColor("Antracit");
            }

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

    private void seedInstallationPosts() {
        InstallationPost xxlPost = InstallationPost.builder()
                .title("Montáž Dřevníku XXL v moderní zahradě")
                .productName("Dřevník XXL")
                .assemblyDate(LocalDate.of(2026, 7, 18))
                .content("Dokončili jsme montáž prostorného Dřevníku XXL u rodinného domu. Dřevník byl usazen na připravený pevný podklad a zákazník zvolil lazuru Ořech s antracitovou střešní krytinou.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        xxlPost.addImage(InstallationImage.builder().imageUrl("realizace/6.webp").displayOrder(0).build());
        xxlPost.addImage(InstallationImage.builder().imageUrl("realizace/7.webp").displayOrder(1).build());
        xxlPost.addImage(InstallationImage.builder().imageUrl("realizace/8.webp").displayOrder(2).build());
        xxlPost.addImage(InstallationImage.builder().imageUrl("realizace/9.webp").displayOrder(3).build());

        installationPostRepository.save(xxlPost);

        InstallationPost klasikPost = InstallationPost.builder()
                .title("Dřevník Klasik na připraveném podkladu")
                .productName("Dřevník Klasik")
                .assemblyDate(LocalDate.of(2026, 7, 11))
                .content("Dokončili jsme montáž Dřevníku Klasik na zákazníkem připravené betonové ploše. Součástí realizace bylo kompletní sestavení a bezpečné ukotvení konstrukce.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        klasikPost.addImage(InstallationImage.builder().imageUrl("realizace/1.webp").displayOrder(0).build());
        klasikPost.addImage(InstallationImage.builder().imageUrl("realizace/2.webp").displayOrder(1).build());
        klasikPost.addImage(InstallationImage.builder().imageUrl("realizace/3.webp").displayOrder(2).build());

        installationPostRepository.save(klasikPost);
    }

    private void seedFinalAudit(User admin) {
        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .module("SYSTÉM")
                .action("INITIAL_SEED")
                .details("Založena deterministická datová sada - pevně definovaní zákazníci, 6x surovina, 4x dřevník, kusovníky a 6 objednávek.")
                .timestamp(LocalDateTime.now())
                .ipAddress("127.0.0.1")
                .build());
    }
}