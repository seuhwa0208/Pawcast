package com.project.pawcast.common.image.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class  ImageStorage {

    // 이미지 저장 경로
    private final Path uploadPath = Paths.get("uploads/images");

    // 이미지 저장
    public String save(MultipartFile file) throws IOException {
        // 저장 디렉터리가 없으면 생성
        Files.createDirectories(uploadPath);
        // 원본 파일명
        String originalFilename = file.getOriginalFilename();
        // 확장자 추출
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 중복되지 않는 파일명 생성
        String storedFilename = UUID.randomUUID() + extension;
        // 실제 파일 저장
        Path targetPath = uploadPath.resolve(storedFilename);
        file.transferTo(targetPath);
        return storedFilename;
    }

    // 이미지 삭제
    public void delete(String filename) throws IOException {
        Path targetPath = uploadPath.resolve(filename).normalize();
        // 저장 경로 내부의 파일인지 확인
        if (!targetPath.startsWith(uploadPath.toAbsolutePath().normalize())) {throw new IllegalArgumentException("잘못된 파일 경로입니다.");}
        Files.deleteIfExists(targetPath);
    }
}