package com.techlearner.config;

import com.techlearner.entity.Order;
import com.techlearner.entity.OrderItem;
import com.techlearner.entity.Product;
import com.techlearner.entity.User;
import com.techlearner.repository.OrderRepository;
import com.techlearner.repository.ProductRepository;
import com.techlearner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * Data Initializer - Sample Data Seed
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: IOC Container & Bean Lifecycle
 * ─────────────────────────────────────
 * CommandLineRunner: Application start hone ke baad automatically run hoti hai.
 * @PostConstruct se difference: CommandLineRunner Spring context fully ready hone ke baad.
 *
 * TOPIC: @Transactional
 * ─────────────────────────────────────
 * Sab kuch ek transaction mein. Agar kuch fail hua → sab rollback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already seeded, skipping...");
            return;
        }

        log.info("🌱 Seeding sample data...");

        // ─── Users ───────────────────────────────────────────
        User admin = User.builder()
                .username("admin")
                .email("admin@techlearner.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .build();

        User user1 = User.builder()
                .username("rahul")
                .email("rahul@test.com")
                .password(passwordEncoder.encode("rahul123"))
                .role(User.Role.USER)
                .build();

        User user2 = User.builder()
                .username("priya")
                .email("priya@test.com")
                .password(passwordEncoder.encode("priya123"))
                .role(User.Role.USER)
                .build();

        userRepository.saveAll(List.of(admin, user1, user2));

        // ─── Products ─────────────────────────────────────────
        // TOPIC: DB Indexing - category column pe index hai, ye queries fast hongi
        List<Product> products = List.of(
                Product.builder().name("MacBook Pro M3").description("Apple laptop 14 inch").price(new BigDecimal("199000")).category("ELECTRONICS").stock(5).build(),
                Product.builder().name("Samsung Galaxy S24").description("Android smartphone").price(new BigDecimal("79999")).category("ELECTRONICS").stock(20).build(),
                Product.builder().name("Sony WH-1000XM5").description("Noise cancelling headphones").price(new BigDecimal("28990")).category("ELECTRONICS").stock(15).build(),
                Product.builder().name("Clean Code Book").description("Robert C. Martin").price(new BigDecimal("599")).category("BOOKS").stock(50).build(),
                Product.builder().name("Designing Data-Intensive Apps").description("Martin Kleppmann").price(new BigDecimal("1299")).category("BOOKS").stock(30).build(),
                Product.builder().name("Spring Boot in Action").description("Craig Walls").price(new BigDecimal("799")).category("BOOKS").stock(25).build(),
                Product.builder().name("Nike Air Max").description("Running shoes").price(new BigDecimal("8999")).category("FASHION").stock(100).build(),
                Product.builder().name("Levi's 501 Jeans").description("Classic fit jeans").price(new BigDecimal("3999")).category("FASHION").stock(75).build(),
                Product.builder().name("Whey Protein 2kg").description("Chocolate flavor").price(new BigDecimal("2999")).category("FITNESS").stock(40).build(),
                Product.builder().name("Yoga Mat").description("Non-slip premium mat").price(new BigDecimal("1499")).category("FITNESS").stock(60).build()
        );
        productRepository.saveAll(products);

        // ─── Orders (for N+1 demo) ────────────────────────────
        // TOPIC: N+1 Problem - ye orders create karo, then test /orders/nplusone vs /orders/fixed
        Product laptop = products.get(0);
        Product phone = products.get(1);
        Product book = products.get(3);

        Order order1 = Order.builder()
                .user(user1)
                .totalAmount(laptop.getPrice())
                .status(Order.OrderStatus.CONFIRMED)
                .build();

        OrderItem item1 = OrderItem.builder()
                .order(order1).product(laptop).quantity(1).unitPrice(laptop.getPrice()).build();
        order1.setItems(List.of(item1));
        orderRepository.save(order1);

        Order order2 = Order.builder()
                .user(user2)
                .totalAmount(phone.getPrice().add(book.getPrice()))
                .status(Order.OrderStatus.DELIVERED)
                .build();

        OrderItem item2 = OrderItem.builder()
                .order(order2).product(phone).quantity(1).unitPrice(phone.getPrice()).build();
        OrderItem item3 = OrderItem.builder()
                .order(order2).product(book).quantity(2).unitPrice(book.getPrice()).build();
        order2.setItems(List.of(item2, item3));
        orderRepository.save(order2);

        log.info("✅ Sample data seeded successfully!");
        log.info("👤 Admin: admin@techlearner.com / admin123");
        log.info("👤 User1: rahul@test.com / rahul123");
        log.info("👤 User2: priya@test.com / priya123");
        log.info("📦 {} products created", products.size());
        log.info("🛒 2 orders created for N+1 demo");
    }
}
