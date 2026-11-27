import re
import unicodedata
from functools import lru_cache
from pathlib import Path


RESOURCES_DIR = Path(__file__).resolve().parent
BAD_WORDS_FILE = RESOURCES_DIR / "bad_words.txt"
ALLOWED_WORDS_FILE = RESOURCES_DIR / "allowed_words.txt"

# 글자 사이에 특수문자·숫자·영문자를 끼워 넣은 명백한 우회 표현만 탐지합니다.
# '씨*'처럼 욕설의 일부만 있는 표현은 정상 문맥일 수도 있어 차단하지 않습니다.
_GAP = r"[\s._*@#!~\-0-9xX]+"
OBFUSCATED_PROFANITY_PATTERNS = tuple(
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        rf"[시씨씌]{_GAP}[발빨벌펄](?![가-힣])",
        rf"[병빙]{_GAP}[신씬](?![가-힣])",
        rf"지{_GAP}랄(?![가-힣])",
        rf"[존졸]{_GAP}[나라](?![가-힣])",
        rf"[좆좃좇]{_GAP}같(?![가-힣])",
        rf"개{_GAP}[새세섀쉐셰]{_GAP}[끼기키히](?![가-힣])",
    )
)


def normalize_for_profanity_check(text: str) -> str:
    """띄어쓰기·특수문자·반복 글자를 정리해 우회 표현을 비교합니다."""
    normalized = unicodedata.normalize("NFKC", text).lower()

    # 'ㅅ ㅂ', '시.발'처럼 글자 사이에 문자를 넣는 우회를 제거합니다.
    normalized = re.sub(r"[\s.\-_]", "", normalized)

    # 같은 글자를 세 번 이상 반복한 우회를 하나로 압축합니다.
    normalized = re.sub(
        r"([가-힣ㄱ-ㅎㅏ-ㅣa-z])\1{2,}",
        r"\1",
        normalized,
    )

    return re.sub(
        r"[^가-힣ㄱ-ㅎㅏ-ㅣ\u1100-\u11ffa-z0-9]",
        "",
        normalized,
    )


def _load_word_file(path: Path) -> frozenset[str]:
    """주석과 빈 줄을 제외하고 단어 파일을 정규화해 읽습니다."""
    if not path.exists():
        raise FileNotFoundError(f"단어 파일을 찾을 수 없습니다: {path}")

    with path.open(encoding="utf-8") as file:
        words = {
            normalized_word
            for line in file
            if (word := line.strip()) and not word.startswith("#")
            if (normalized_word := normalize_for_profanity_check(word))
        }

    return frozenset(words)


@lru_cache
def load_bad_words() -> frozenset[str]:
    """차단할 욕설·인신공격 표현을 서버 실행 중 한 번만 읽습니다."""
    return _load_word_file(BAD_WORDS_FILE)


@lru_cache
def load_allowed_words() -> frozenset[str]:
    """욕설 일부를 포함하지만 정상적으로 사용할 수 있는 표현을 읽습니다."""
    return _load_word_file(ALLOWED_WORDS_FILE)


def contains_prohibited_word(text: str) -> bool:
    """허용 표현을 보호한 뒤 실제 금지어가 남아 있는지 검사합니다."""
    if not text or not text.strip():
        return False

    # 완성된 욕설을 특수문자 등으로 분리한 경우만 별도 규칙으로 차단합니다.
    if any(pattern.search(text) for pattern in OBFUSCATED_PROFANITY_PATTERNS):
        return True

    normalized_text = normalize_for_profanity_check(text)

    # '시발점', '쓰레기통' 같은 정상 표현 때문에 부분 문자열이 오탐되지 않게 합니다.
    for allowed_word in sorted(load_allowed_words(), key=len, reverse=True):
        normalized_text = normalized_text.replace(allowed_word, "")

    return any(
        bad_word in normalized_text
        for bad_word in load_bad_words()
    )
