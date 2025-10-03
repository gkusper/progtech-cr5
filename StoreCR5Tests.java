import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.store.*;

class StoreCR5Tests {
    static Store target;
    static Period normal;

    @BeforeAll
    public static void initStore() {
        target = new Store();
        normal = new Period("Normal");
        // Settings known from CR0/CR1
        normal.setUnitPrice(Product.APPLE, 500.0);
        normal.setUnitPrice(Product.BANANA, 450.0);
        normal.setDiscount(Product.APPLE, 5.0, 0.1);
        normal.setDiscount(Product.APPLE, 20.0, 0.15);
        normal.setDiscount(Product.BANANA, 2.0, 0.1);
        target.addPeriod(normal);
    }

    // --- Helper: rounding to 5 HUF (CR0 rule)
    private double roundTo5(double amount) {
        double remainder = amount % 10.0;
        double base = Math.floor(amount / 10.0) * 10.0;
        if (remainder < 2.5) return base;
        if (remainder < 5.0) return base + 5.0;
        if (remainder < 7.5) return base + 5.0;
        return base + 10.0;
    }

    @Test
    void test_cr5_example1_b5max10_alone() {
        Cart cart = new Cart(List.of(new Item(Product.BANANA, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("B5-MAX10"));
        assertEquals(roundTo5(450.0 * 0.95), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example2_b5_and_b5max10_max10() {
        Cart cart = new Cart(List.of(new Item(Product.BANANA, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("B5", "B5-MAX10"));
        assertEquals(roundTo5(450.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example3_twoMAX_strictest10_return15() {
        Cart cart = new Cart(List.of(new Item(Product.BANANA, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("B5", "B5-MAX15", "B5-MAX10"));
        assertEquals(roundTo5(450.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of("B5-MAX15"), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example4_x5_alone_twoProducts() {
        Cart cart = new Cart(List.of(
                new Item(Product.APPLE, 1.0),
                new Item(Product.BANANA, 1.0)
        ));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5"));
        assertEquals(roundTo5(950.0 * 0.95), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example5_x10_notCombinable() {
        Cart cart = new Cart(List.of(
                new Item(Product.APPLE, 2.0),
                new Item(Product.BANANA, 1.0)
        ));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X10", "A5", "B5", "A5-MAX10", "B5-MAX10"));
        assertEquals(roundTo5(1450.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of("A5", "B5", "A5-MAX10", "B5-MAX10"), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example6_x5_partial_notReturned() {
        Cart cart = new Cart(List.of(new Item(Product.APPLE, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5", "B5"));
        assertEquals(roundTo5(500.0 * 0.95), price.getAmount(), 0.001);
        assertEquals(List.of("B5"), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example7_x5_and_a5max10_exception() {
        Cart cart = new Cart(List.of(
                new Item(Product.APPLE, 1.0),
                new Item(Product.BANANA, 1.0)
        ));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5", "A5-MAX10"));
        double expected = roundTo5(500.0 * 0.90 + 450.0 * 0.95);
        assertEquals(expected, price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example8_x5max10_alone_twoProducts() {
        Cart cart = new Cart(List.of(
                new Item(Product.APPLE, 1.0),
                new Item(Product.BANANA, 1.0)
        ));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5-MAX10"));
        assertEquals(roundTo5(500.0 * 0.95 + 450.0 * 0.95), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example9_x5max10_a5_b5() {
        Cart cart = new Cart(List.of(
                new Item(Product.APPLE, 1.0),
                new Item(Product.BANANA, 1.0)
        ));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5-MAX10", "A5", "B5"));
        assertEquals(roundTo5(500.0 * 0.90 + 450.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example10_x5max10_twoA5_oneReturned() {
        Cart cart = new Cart(List.of(new Item(Product.APPLE, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5-MAX10", "A5", "A5"));
        assertEquals(roundTo5(500.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of("A5"), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example11_x5max10_bSide() {
        Cart cart = new Cart(List.of(new Item(Product.BANANA, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5-MAX10", "B5-MAX10", "B5"));
        assertEquals(roundTo5(450.0 * 0.90), price.getAmount(), 0.001);
        assertEquals(List.of(), price.getUnusedCoupons());
    }

    @Test
    void test_cr5_example12_x5_partialWithA5() {
        Cart cart = new Cart(List.of(new Item(Product.BANANA, 1.0)));
        PriceInfo price = target.getCartPrice(cart, normal, List.of("X5", "A5"));
        assertEquals(roundTo5(450.0 * 0.95), price.getAmount(), 0.001);
        assertEquals(List.of("A5"), price.getUnusedCoupons());
    }
}
