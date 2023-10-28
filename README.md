# 키친포스

## 퀵 스타트

```sh
cd docker
docker compose -p kitchenpos up -d
```

## 요구 사항
- 키친 포스를 구현한다.
- 외주로 개발된 프로그램으로 만든 개발자가 없어 요구사항을 도출해내야 한다.
### 메뉴
- [ ] 가격은 0원 이상이어야 한다.
- [ ] 메뉴를 등록할 수 있다.
  - [ ] 메뉴 등록을 위해 먼저 상품과 메뉴 그룹 등록이 필요하다.
  - [ ] 메뉴 1개는 메뉴 상품의 여러 개로 이루어진다. 
  - [ ] 등록할 메뉴의 메뉴 상품 개수는 0개보다 커야 한다.  </br>(코드는 0개 이상으로 되어 있지만 0개인 메뉴 상품은 있을 수 없다.)
  - [ ] 등록할 메뉴의 가격이 메뉴 상품 가격의 합보다 크면 안된다.
  - [ ] 메뉴 이름에 비속어가 있으면 안된다.
- [ ] 메뉴 가격을 수정할 수 있다.
  - [ ] 수정할 가격은 메뉴 상품 가격의 합보다 크면 안된다.
- [ ] 메뉴를 판매하지 않기 위해 비활성화할 수 있다.
- [ ] 메뉴를 비활성화 후 다시 판매하도록 활성화 할 수 있다.
  - [ ] 메뉴 가격이 메뉴 상품 가격의 합보다 크면 안된다.
- [ ] 메뉴 목록을 조회할 수 있다.

### 상품
- [ ] 상품 가격은 0원 이상이어야 한다.
- [ ] 상품 등록을 할 수 있다.
  - [ ] 한 번에 1개의 상품만 등록할 수 있다. 
  - [ ] 상품 이름에 비속어가 있으면 안된다.
- [ ] 상품 가격 수정
  - [ ] 메뉴에 등록된 상품이 있다면 메뉴 가격이 수정된 메뉴 상품의 가격의 합보다 크면 안된다.
- [ ] 상품 목록 조회
  - [ ] 상품 목록을 조회할 수 있다.

### 주문
- [ ] 주문 타입은 DELIVERY, TAKEOUT, EAT_IN 3가지가 있다.
- [ ] 주문 상태는 WAITING, ACCEPTED, SERVED, DELIVERING, DELIVERED, COMPLETED가 있다.
- [ ] 주문을 할 수 있다
  - [ ] 주문 타입은 필수이다.
  - [ ] 주문 메뉴는 필수이다.
  - [ ] 각 주문 메뉴의 주문 가격, 주문 메뉴 개수는 필수이다.
  - [ ] 주문 메뉴는 메뉴에 등록되어 있지 않으면 주문할 수 없다.
  - [ ] 주문 메뉴는 여러개일 수 있다.
  - [ ] 먹고갈 경우(EAT IN) 모든 주문 메뉴가 개수가 1개 이상이어야 한다.
  - [ ] 비활성화된 메뉴는 주문이 불가하다.
  - [ ] 등록된 메뉴의 가격과 주문 메뉴의 가격이 다르면 주문이 불가하다.
  - [ ] 주문 시 초기 주문 상태는 대기 상태(WATING)이다.
  - [ ] 주문 타입이 배달(DELIVERED)일 경우 주소는 필수이다.
  - [ ] 먹고갈 경우(EAT IN) 비어있는 테이블이 있어야 한다.
- [ ] 주문 승인
  - [ ] 주문 상태가 대기 상태(WATING)이어야 한다.
  - [ ] 주문 타입이 배달이면, 주문 메뉴 금액의 합으로 해당 주소로 배달 요청을 하고, 주문 상태를 승인(ACCEPTED) 상태가 된다.
  - [ ] 주문 타입이 배달이 아니면 배달 요청을 하지 않고, 주문 상태만 승인(ACCEPTED)상태가 된다.
- [ ] 서빙
  - [ ] 주문 상태가 승인(ACCEPTED)이어야 한다.
  - [ ] 주문 타입이 EAT_IN 또는 TAKEOUT이어야 한다.(코드 추가 필요)
  - [ ] 주문 상태가 승인(ACCEPTED)이면, 주문 상태는 서빙(SERVED)으로 변경된다.
- [ ] 배달 시작
  - [ ] 주문 타입이 DELIVERY여야만 한다.
  - [ ] 주문 상태가 승인(ACCEPTED)이어야 한다.
  - [ ] 주문 상태가 DELIVERING으로 변경 되어야 한다.
- [ ] 배달 완료
  - [ ] 주문 상태가 DELIVERING이어야 한다.
  - [ ] 주문 타입이 DELIVRY여야 한다.(코드 추가 필요)
  - [ ] 주문 상태가 DELIVERED로 변경 되어야 한다.
- [ ] 주문 처리 최종 완료
  - [ ] 배달인 경우 주문 상태는 배달 완료 상태여야 한다.
  - [ ] 포장 또는 매장 식사인 경우 주문 상태는 서빙 상태여야 한다.
  - [ ] 매장 식사의 경우 주문 완료 처리 후 테이블도 다음 사람이 사용할 수 있게 비워야 한다.(코드 변경 필요)
- [ ] 주문 목록 조회

### 테이블
- [ ] 테이블 생성
  - [ ] 테이블 이름은 필수이다.
  - [ ] 아직 특별 손님은 없으므로 손님 번호는 모두 0으로 통일한다.
  - [ ] 테이블을 미사용중으로 만든다.
- [ ] 테이블에 손님 착석
  - [ ] 테이블을 사용중으로 만든다.
- [ ] 테이블 정리
  - [ ] 주문 상태가 완료 처리되지 않았다면 테이블을 정리할 수 없다.
  - [ ] 테이블을 정리하면 사용중 -> 미사용중 상태로 변경된다.
- [ ] 손님 수 변경
  - [ ] 손님 수를 변경할 수 있다.
  - [ ] 테이블을 미사용 중이면 변경할 수 없다.
  - [ ] 손님 수를 변경한다.

## 용어 사전
| 한글명      | 영문명                | 설명                                    |
|----------|--------------------|---------------------------------------|
| 메뉴 그룹    | menu group         | 메뉴의 최상단 카테고리                          |
| 메뉴       | menu               | 메뉴 그룹에 속한 메뉴 정보                       |
| 메뉴 상품    | menu product       | 메뉴 1개와 여러 개의 상품을 기반으로 만들어진 실제 판매되는 상품 |
| 메뉴 상품 가격 | menu product price | 상품 가격 * 상품 개수 |
| 상품       | product            | 상품 자체로 고유한 것                          |
| 주문 메뉴    | order line item    | 주문할 메뉴를 의미한다.                         |
| 서빙       |                    | 고객에게 직접 주문 메뉴를 전달한 상태                 |
| 포장       | TAKE OUT           |                                       |
| 매장 식사    | EAT_IN             |                                       |

## 모델링
