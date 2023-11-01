package kitchenpos.application;

import kitchenpos.domain.Product;
import kitchenpos.infra.PurgomalumClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class ProductServiceTest {

    @Autowired
    private ProductService sut;

    @MockBean
    private PurgomalumClient purgomalumClient;

    private static final String MYSQL_VERSION = "mysql:8.0.30";

    @Container
    private static MySQLContainer mySQLContainer = new MySQLContainer(MYSQL_VERSION);

    @Nested
    class 성공_케이스{

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
    }

    @Nested
    class 실패_케이스{

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
    }
}