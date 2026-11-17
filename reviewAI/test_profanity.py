import unittest

from app.services.profanity_service import (
    contains_prohibited_word,
    load_allowed_words,
    load_bad_words,
)


class ProfanityFilterTest(unittest.TestCase):
    def setUp(self):
        # 테스트 중 사전 파일이 변경되어도 매번 최신 내용을 읽습니다.
        load_bad_words.cache_clear()
        load_allowed_words.cache_clear()

    def test_blocks_clear_profanity_and_full_obfuscation(self):
        blocked_reviews = [
            "배송이 왜 이래 씨발",
            "개빡치네 진짜",
            "이건 병*신 같은 제품이다",
            "시@발 배송을 이렇게 하냐",
            "개.새.끼 같은 대응",
            "존1나 별로다",
        ]
        for review in blocked_reviews:
            with self.subTest(review=review):
                self.assertTrue(contains_prohibited_word(review))

    def test_allows_normal_complaints_and_ambiguous_words(self):
        allowed_reviews = [
            "배송이 늦고 포장이 엉망입니다.",
            "사기당한 기분이라 소비자원에 문의했습니다.",
            "제품 안에 쓰레기가 들어있어 교환을 요청합니다.",
            "시바견이 사용하기에는 크기가 작아요.",
            "쌍둥이 강아지가 함께 잘 사용하고 있어요.",
            "물걸레로 닦으니 얼룩이 없어졌어요.",
            "미친 가성비라는 후기를 보고 구매했어요.",
            "장난하냐고 묻고 싶을 만큼 배송이 늦었어요.",
            "씨*라고만 적힌 문구는 욕설인지 확정할 수 없어요.",
        ]
        for review in allowed_reviews:
            with self.subTest(review=review):
                self.assertFalse(contains_prohibited_word(review))


if __name__ == "__main__":
    unittest.main()
