# Spring Boot와 주고받는 추천 요청, 응답 구조를 정의
# 필터링 X, 데이터의 필드, 자료형, 필수 여부와 유효성만 검사함

from datetime import date
from decimal import Decimal
from enum import Enum

from pydantic import (
    BaseModel, 
    ConfigDict, 
    Field, 
    model_validator,)

def to_camel(value: str) -> str:
    # 파이썬의 snake_case 필드명을 자바의 camelCase JSON 필드명으로 자동 반환
    # pet_id = petID, life_stage = lifeStage
    
    first, *rest = value.split("_")
    
    return first + "".join(
        word.capitalize() # 각 단어의 첫글자는 대문자, 나머지 소문자로 변환
        for word in rest
    )
    
# 추천 요청과 응답 스키마가 공통으로 상속받는 기본 클래스, 공통 설정값 지정
class ApiModel(BaseModel):
    model_config = ConfigDict(
        alias_generator= to_camel,
        populate_by_name= True, # 파이싼의 스네이크 케이스와 자바의 카멜케이스를 모두 입력 가능함
        str_strip_whitespace= True, # " dog "처럼 앞뒤에 공백이 포함된 문자열을 자동으로 정리함
    )
    
# PETS.pet_type 컬럼에서 사용할 반려동물 종류
# Enum을 사용하면 dog와 cat 이의의 값이 들어오는 걸 파이썬이 자동으로 차단
class PetType(str, Enum):
    DOG = "DOG"
    CAT = "CAT"
    
# 추천에서 사용할 공통 생애주기 코드
# 생애주기 계산 결과 X, 급여 가능한 상품에 사용하는 생애주기 코드
class LifeStage(str, Enum):
    PUPPY = "PUPPY"
    JUNIOR = "JUNIOR"
    ADULT = "ADULT"
    SENIOR = "SENIOR"
    ALL_AGES = "ALL_AGES"

# Spring Boot가 PETS 테이블에서 조회해 FastAPI로 전달하는 반려동물 정보
# Life_stage 필드가 없는 이유: 반려동물의 생애주기는 DB에 저장된 고정값이 아니고 birthday와 현재 날짜를 이용해 파이썬에서 계산해서
