package com.project.pawcast.common.image.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.common.image.storage.ImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class ImageService {

    private final ImageStorage imageStorage;

    // 허용 이미지 확장자
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public ImageService(ImageStorage imageStorage) {this.imageStorage = imageStorage;}

    // 이미지 업로드
    public String upload(MultipartFile file){

        validateUpload(file);
        try {
            return imageStorage.save(file);
        }catch (IOException e){
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /** 상품 이미지는 공개 상품 전용 디렉터리에 저장합니다. */
    public String uploadProduct(MultipartFile file){
        validateUpload(file);
        try {
            return imageStorage.saveProduct(file);
        }catch (IOException e){
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateUpload(MultipartFile file) {

        // 파일 존재 여부 확인
        if (file == null || file.isEmpty()) {throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");}
        // 파일 크기 확인
        if (file.getSize() > MAX_FILE_SIZE) {throw new IllegalArgumentException("이미지는 최대 5MB까지 업로드할 수 있습니다.");}

        // 확장자 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {throw new IllegalArgumentException("올바르지 않은 파일입니다.");}
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");}

    }

    // 이미지 삭제
    public void delete(String filename) throws IOException {
        imageStorage.delete(filename);
    }
}
