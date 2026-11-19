package com.project.powcast.review.filter;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ProfanityFilter {

    @Value("${profanity.bad-words-file:profanity/bad_words.txt}")
    private String badWordsFile;

    @Value("${profanity.allowed-words-file:profanity/allowed_words.txt}")
    private String allowedWordsFile;

    // 중복 제거를 위한 set, 정렬을 위한 list
    private Set<String> badWords;
    private List<String> allowedWords;

    // 욕설 사이에 삽입된 특수문자, 숫자 등의 우회 표현
    private static final String GAP = "[\\s._*@#!~0-9xX\\\\-]+";

    // 패턴 만들어서 Matches로 검사
    private static final List<Pattern> OBFUSCATED_PATTERNS = List.of(
            Pattern.compile("[시씨씌]" + GAP + "[발빨벌펄](?![가-힣])"),
            Pattern.compile("[병빙]" + GAP + "[신씬](?![가-힣])"),
            Pattern.compile("지" + GAP + "랄(?![가-힣])"),
            Pattern.compile("[존졸]" + GAP + "[나라](?![가-힣])"),
            Pattern.compile("[좆좃좇]" + GAP + "같(?![가-힣])"),
            Pattern.compile("개" + GAP + "[새세섀쉐셰]" + GAP + "[끼기키히](?![가-힣])")
    );

    // 정규화 패턴
    // 공백제거
    private static final Pattern REMOVE_SPACES =
            Pattern.compile("[\\s.\\-_]", Pattern.UNICODE_CHARACTER_CLASS);

    // 3회 이상 반복 -> 문자 하나만 남김 -> ex) 시이이이발 -> 시이발
    private static final Pattern REPEATED_CHARACTERS =
            Pattern.compile("([가-힣ㄱ-ㅎㅏ-ㅣa-z])\\1{2,}");

    // 특수 문자 제거
    private static final Pattern REMOVE_INVALID_CHARACTERS =
            Pattern.compile("[^가-힣ㄱ-ㅎㅏ-ㅣ\\x{1100}-\\x{11FF}a-z0-9]");

    // 서버 시작 시 단어 파일 로드
    @PostConstruct
    public void init() {
        // 긴 단어부터 정렬해야 처리할때 문제가 생김 방지
        badWords = loadWordFile(badWordsFile);
        allowedWords = loadWordFile(allowedWordsFile)
                .stream().distinct().sorted(Comparator.comparingInt(String::length).reversed()).toList();
    }

    // 텍스트 정규화
    public String normalizeForProfanityCheck(String text) {

        // 유니코드 형태로 통일 및 소문자로 통일
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);

        // 띄어쓰기 및 일부 특수문자 제거
        normalized = REMOVE_SPACES.matcher(normalized).replaceAll("");

        // 같은 글자 3회 이상 반복 시 하나로 압축
        normalized = REPEATED_CHARACTERS.matcher(normalized).replaceAll("$1");

        // 허용 문자 이외의 문자 제거
        return REMOVE_INVALID_CHARACTERS.matcher(normalized).replaceAll("");
    }

    // 금칙어 파일 로드
    private Set<String> loadWordFile(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {throw new IllegalStateException("단어 파일을 찾을 수 없습니다: " + filePath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)))
            {
                return reader.lines()
                        .map(String::strip)
                        .filter(word -> !word.isEmpty())
                        .filter(word -> !word.startsWith("#"))
                        .map(this::normalizeForProfanityCheck)
                        .filter(word -> !word.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
            }

        } catch (IOException e) {throw new IllegalStateException("단어 파일 로드에 실패했습니다: " + filePath, e);
        }
    }

    // 욕설 포함 여부 검사
    public boolean containsProhibitedWord(String text) {

        if (text == null || text.isBlank()) {return false;}

        // 특수문자 등을 삽입한 욕설 검사
        if (OBFUSCATED_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(text).find())) {return true;}

        String normalizedText = normalizeForProfanityCheck(text);

        // 허용 표현을 먼저 제거
        for (String allowedWord : allowedWords) {normalizedText = normalizedText.replace(allowedWord, "");}

        // 금칙어 포함 여부 검사
        return badWords.stream().anyMatch(normalizedText::contains);
    }
}