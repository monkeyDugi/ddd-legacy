package kitchenpos.application;

import kitchenpos.domain.*;
import kitchenpos.infra.KitchenridersClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final OrderTableRepository orderTableRepository;
    private final KitchenridersClient kitchenridersClient;

    public OrderService(
        final OrderRepository orderRepository,
        final MenuRepository menuRepository,
        final OrderTableRepository orderTableRepository,
        final KitchenridersClient kitchenridersClient
    ) {
        this.orderRepository = orderRepository;
        this.menuRepository = menuRepository;
        this.orderTableRepository = orderTableRepository;
        this.kitchenridersClient = kitchenridersClient;
    }

    @Transactional
    public Order create(final Order request) {
        final OrderType type = request.getType();
        // 타입 필수
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException();
        }

        // 주문 메뉴 필수
        final List<OrderLineItem> orderLineItemRequests = request.getOrderLineItems();
        if (Objects.isNull(orderLineItemRequests) || orderLineItemRequests.isEmpty()) {
            throw new IllegalArgumentException();
        }

        // 주문 메뉴 찾기
        final List<Menu> menus = menuRepository.findAllByIdIn(
            orderLineItemRequests.stream()
                .map(OrderLineItem::getMenuId)
                .collect(Collectors.toList())
        );

        // 주문 메뉴가 실제 메뉴에 존재하지 않으면 주문 불가
        if (menus.size() != orderLineItemRequests.size()) {
            throw new IllegalArgumentException();
        }


        final List<OrderLineItem> orderLineItems = new ArrayList<>();
        // 주문은 여러 메뉴가 가능하기 때문에 루프
        for (final OrderLineItem orderLineItemRequest : orderLineItemRequests) {
            final long quantity = orderLineItemRequest.getQuantity();
            // 먹고갈 경우 주문 메뉴 중 1개라도 0개 미만이면 안된다. 버그이다. 1개 미만으로 바껴야 한다.
            if (type != OrderType.EAT_IN) {
                if (quantity < 0) {
                    throw new IllegalArgumentException();
                }
            }

            // 존재하지 않는 메뉴 주문 불가
            final Menu menu = menuRepository.findById(orderLineItemRequest.getMenuId())
                .orElseThrow(NoSuchElementException::new);
            // 비활성된 메뉴 주문 불가
            if (!menu.isDisplayed()) {
                throw new IllegalStateException();
            }
            // 메뉴의 가격과 주문 메뉴의 가격이 다르면 주문 불가
            if (menu.getPrice().compareTo(orderLineItemRequest.getPrice()) != 0) {
                throw new IllegalArgumentException();
            }
            final OrderLineItem orderLineItem = new OrderLineItem();
            orderLineItem.setMenu(menu);
            orderLineItem.setQuantity(quantity);
            orderLineItems.add(orderLineItem);
        }

        // 주문 대기 중이르 초기 셋팅
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setType(type);
        order.setStatus(OrderStatus.WAITING);
        order.setOrderDateTime(LocalDateTime.now());
        order.setOrderLineItems(orderLineItems);

        // 배달일 경우
        if (type == OrderType.DELIVERY) {
            final String deliveryAddress = request.getDeliveryAddress();
            // 주소 필수
            if (Objects.isNull(deliveryAddress) || deliveryAddress.isEmpty()) {
                throw new IllegalArgumentException();
            }
            order.setDeliveryAddress(deliveryAddress);
        }

        if (type == OrderType.EAT_IN) {
            // 먹고가려면 비어있는 테이블이 있어야 한다.
            final OrderTable orderTable = orderTableRepository.findById(request.getOrderTableId())
                .orElseThrow(NoSuchElementException::new);
            if (!orderTable.isOccupied()) {
                throw new IllegalStateException();
            }
            order.setOrderTable(orderTable);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order accept(final UUID orderId) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new IllegalStateException();
        }
        if (order.getType() == OrderType.DELIVERY) {
            BigDecimal sum = BigDecimal.ZERO;
            for (final OrderLineItem orderLineItem : order.getOrderLineItems()) {
                sum = orderLineItem.getMenu()
                    .getPrice()
                    .multiply(BigDecimal.valueOf(orderLineItem.getQuantity()));
            }
            kitchenridersClient.requestDelivery(orderId, sum, order.getDeliveryAddress());
        }
        order.setStatus(OrderStatus.ACCEPTED);
        return order;
    }

    @Transactional
    public Order serve(final UUID orderId) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.SERVED);
        return order;
    }

    @Transactional
    public Order startDelivery(final UUID orderId) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);
        if (order.getType() != OrderType.DELIVERY) {
            throw new IllegalStateException();
        }
        // TODO-BD:  ACCEPTED 체크로 변경 필요.
        if (order.getStatus() != OrderStatus.SERVED) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.DELIVERING);
        return order;
    }

    @Transactional
    public Order completeDelivery(final UUID orderId) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.DELIVERED);
        return order;
    }

    @Transactional
    public Order complete(final UUID orderId) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);
        final OrderType type = order.getType();
        final OrderStatus status = order.getStatus();
        // 배달인 경우 주문 상태는 배달 완료 상태여야 한다.
        if (type == OrderType.DELIVERY) {
            if (status != OrderStatus.DELIVERED) {
                throw new IllegalStateException();
            }
        }
        // 포장이거나 먹고갈 경우 주문 상태는 SERVED 상태여야 한다.
        if (type == OrderType.TAKEOUT || type == OrderType.EAT_IN) {
            if (status != OrderStatus.SERVED) {
                throw new IllegalStateException();
            }
        }
        // 이상 없으면 주문 상태는 완료로 변경
        order.setStatus(OrderStatus.COMPLETED);
        // 매장 식사일 경우 주문이 완료되지 않은 테이블이 있으면 자리를 비운다?
        // 애초에 완료 처리를 하는 것이기 떄문에 조건없이 무조건 ooupied를 false로 하는게 맞지 않나?
        if (type == OrderType.EAT_IN) {
            final OrderTable orderTable = order.getOrderTable();
            if (!orderRepository.existsByOrderTableAndStatusNot(orderTable, OrderStatus.COMPLETED)) {
                orderTable.setNumberOfGuests(0);
                orderTable.setOccupied(false);
            }
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
