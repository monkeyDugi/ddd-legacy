package kitchenpos.application;

import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuGroupRepository;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.MenuProductRepository;
import kitchenpos.domain.MenuRepository;
import kitchenpos.domain.Product;
import kitchenpos.infra.PurgomalumClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class ProductServiceTest {

    @Autowired
    private ProductService sut;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuGroupRepository menuGroupRepository;

    @Autowired
    private MenuProductRepository menuProductRepository;

    @MockBean
    private PurgomalumClient purgomalumClient;

    private static final String MYSQL_VERSION = "mysql:8.0.30";

    @Container
    private static MySQLContainer mySQLContainer = new MySQLContainer(MYSQL_VERSION);

    @Nested
    class 성공_케이스 {

        @ValueSource(ints = {0, 17_000})
        @ParameterizedTest
        void 상품_등록하기(int price) {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(price));
            product.setName("강정치킨");

            given(purgomalumClient.containsProfanity("강정치킨")).willReturn(Boolean.FALSE);

            // when
            Product actualProduct = sut.create(product);

            // then
            assertThat(actualProduct.getId()).isNotNull();
            assertThat(actualProduct.getPrice()).isEqualTo(BigDecimal.valueOf(price));
            assertThat(actualProduct.getName()).isEqualTo("강정치킨");
        }

        @ValueSource(ints = {0, 20_000})
        @ParameterizedTest
        void 상품_가격_변경하기(int changePrice) {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(17_000));
            product.setName("강정치킨");

            given(purgomalumClient.containsProfanity("강정치킨")).willReturn(Boolean.FALSE);
            Product createProduct = sut.create(product);

            Product changeProduct = new Product();
            changeProduct.setPrice(BigDecimal.valueOf(changePrice));

            // when
            Product actualProduct = sut.changePrice(createProduct.getId(), changeProduct);

            // then
            assertThat(actualProduct.getId()).isEqualTo(createProduct.getId());
            assertThat(actualProduct.getPrice()).isEqualTo(BigDecimal.valueOf(changePrice));
            assertThat(actualProduct.getName()).isEqualTo("강정치킨");
        }

        @DisplayName("메뉴에 등록된 상품일 경우 메뉴 가격이 메뉴 상품의 합보다 작으면 해당 메뉴는 비활성화 된다.")
        @Test
        void 상품_가격_변경하기_메뉴에_등록된_상품() {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(17_000));
            product.setName("강정치킨");
            given(purgomalumClient.containsProfanity("강정치킨")).willReturn(Boolean.FALSE);
            Product createProduct = sut.create(product);

            MenuGroup menuGroup = createMenuGroup("한마리메뉴");
            Menu menu = createMenu(createProduct, menuGroup, "후라이드치킨", 17_000, true);

            Product changeProduct = new Product();
            changeProduct.setPrice(BigDecimal.valueOf(10_000));

            // when
            Product actualProduct = sut.changePrice(createProduct.getId(), changeProduct);

            // then
            assertThat(actualProduct.getPrice()).isEqualTo(BigDecimal.valueOf(10_000));

            Menu findMenu = menuRepository.findById(menu.getId()).get();
            assertThat(findMenu.isDisplayed()).isFalse();
        }
    }

    @Nested
    class 실패_케이스 {

        @Test
        void 상품_등록하기_가격_0원_미만() {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(-1));
            product.setName("강정치킨");

            BDDMockito.given(purgomalumClient.containsProfanity("강정치킨")).willReturn(Boolean.FALSE);

            // when
            assertThatThrownBy(() -> sut.create(product))
                    // then
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 상품_등록하기_상품_이름_비속어() {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(17_000));
            product.setName("강정치킨fuck");

            given(purgomalumClient.containsProfanity("강정치킨fuck")).willReturn(Boolean.TRUE);

            // when
            assertThatThrownBy(() -> sut.create(product))
                    // then
                    .isInstanceOf(IllegalArgumentException.class);

        }

        @Test
        void 상품_가격_변경하기_가격_0원_미만() {
            // given
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(17_000));
            product.setName("강정치킨");

            Product createProduct = sut.create(product);

            Product changeProduct = new Product();
            changeProduct.setPrice(BigDecimal.valueOf(-1));

            // when
            assertThatThrownBy(() -> sut.changePrice(createProduct.getId(), changeProduct))
                    // then
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @NotNull
    private Menu createMenu(Product createProduct, MenuGroup menuGroup, String name, int price, boolean isDisplayed) {
        MenuProduct menuProduct = new MenuProduct();
        menuProduct.setProduct(createProduct);
        menuProduct.setQuantity(1);

        Menu menu = new Menu();
        menu.setId(UUID.randomUUID());
        menu.setMenuGroup(menuGroup);
        menu.setDisplayed(isDisplayed);
        menu.setName(name);
        menu.setPrice(BigDecimal.valueOf(price));
        menu.setMenuProducts(List.of(menuProduct));
        menuRepository.save(menu);
        return menu;
    }

    @NotNull
    private MenuGroup createMenuGroup(String name) {
        MenuGroup menuGroup = new MenuGroup();
        menuGroup.setId(UUID.randomUUID());
        menuGroup.setName(name);
        menuGroupRepository.save(menuGroup);
        return menuGroup;
    }
}