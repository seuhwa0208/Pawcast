from app.services.profanity_service import contains_prohibited_word
from app.services.sentiment_service import get_sentiment_analyzer

analyzer = get_sentiment_analyzer()

reviews = [
    # 기존 정상 리뷰
    "배송도 빠르고 제품도 정말 좋아요.",
    "가격 대비 품질이 너무 훌륭합니다.",
    "다음에도 꼭 재구매하겠습니다.",
    "제품이 너무 불편하고 품질도 형편없습니다.",
    "배송이 늦고 포장도 엉망입니다.",
    "돈이 아까워서 다시는 구매하지 않겠습니다.",
    
    # 새로 추가된 욕설/비속어 및 우회 표현 테스트 리뷰
    "아 진짜 짜증나네 제품 개판으로 보내네 쓰레기같음",
    "ㅅㅂ 배송 왜 이렇게 늦냐 돈 아까워 죽겠네",
    "이거 만든 사람 뇌가 없나 진짜 좆같네",
    "포장 상태가 아주 개새끼 수준이네 환불해줘"
]

for review in reviews:
    # 실제 API와 동일하게 욕설 검사를 먼저 수행합니다.
    if contains_prohibited_word(review):
        print("=" * 60)
        print("리뷰:", review)
        print("결과: [차단됨] 욕설 및 비방 목적의 리뷰는 등록할 수 없습니다.")
        continue

    # 욕설이 없는 리뷰만 감정 분석 모델로 전달합니다.
    result = analyzer.predict(review)
    print("=" * 60)
    print("리뷰:", review)
    print("감정:", result["sentiment"])
    print(f'신뢰도: {result["confidence"] * 100:.2f}%')
