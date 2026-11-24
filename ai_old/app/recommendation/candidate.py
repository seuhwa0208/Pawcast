# Spring Boot가 전달한 상품 목록에서 추천 후보를 생성하는 모듈
# 전체 상품 중에서 이미 산 것, 품절된 것, 가격 오류인 것, 중복된 것 들을 1차로 필터링

from app.recommendation.schemas import (
    RecommendationCandidate,
    RecommendationRequest,
)

def create_candidates(
    request: RecommendationRequest,
    max_candidates: int = 100, # 100개로 최대수를 지정하긴 했는데 변경해도 문제는 없음
) -> list[RecommendationCandidate]:
    # 추천 점수를 계산할 1차 상품 후보 목록을 생성하는 함수
    
    # 최대 후보 수가 잘못 전달되면 빈 목록을 반환
    if max_candidates <= 0:
        return []
    
    # 이미 구매한 상품 ID를 빠르게 검색할 수 있도록 set으로 반환
    purchased_product_ids = set(
        request.purchased_product_ids
    )
    
    # 같은 상품이 여러번 추가되는 것을 방지하기 위한 상품 ID 집합
    seen_product_ids: set[int] = set()
    
    # 조건을 통과한 상품을 저장할 추천 후보 목록
    candidates: list[RecommendationCandidate] = []
    
    # Spring Boot가 전달한 전체 상품을 순서대로 검사
    for product in request.products:
        
        # 같은 상품이 이미 추가됐다면 중복으로 추가하지 않음 -> 옵션 때문에 생긴 함수
        if product.product_id in seen_product_ids:
            continue
        
        # 회원이 이미 구매한 상품은 신규 상품 추천에서 제외 -> 재구매가 아니라 새 제품을 구매 유도하는 부분이기에 생긴 함수
        if product.product_id in purchased_product_ids:
            continue
        
        # 품절 상품은 추천 후보에서 제외
        if product.sold_out:
            continue
        
        # 판매 가격이 0원 이하인 비정상 상품은 제외
        if product.sale_price <= 0:
            continue
        
        # 중복 검사를 위해 현재 상품 ID를 저장
        seen_product_ids.add(product.product_id)
        
        # 모든 기본 조건을 통과한 상품을 후보 목록에 추가
        candidates.append(product)
        
        # 후보수가 최대값(위에서 지정한 100)에 도달하면 더 이상 상품을 검색하지 않음
        if len(candidates) >= max_candidates:
            break
    
    # 정리된 추천 후보 상품 목록을 반환함
    return candidates