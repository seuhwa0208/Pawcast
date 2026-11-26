
<!-- 회원 본인이 작성한 리뷰의 별점, 내용, 이미지를 수정하는 공통 모달입니다. -->
<script setup>
import { computed, ref, onBeforeUnmount } from "vue";
import { reviewService } from "@/services/reviewService";
import { useAuthSession } from "@/stores/auth";

// 부모 컴포넌트에서 기존 리뷰와 상품명을 전달받습니다.
const props = defineProps({
    review: {
        type: Object,
        required: true,
    },
    productName: {
        type: String,
        default: "",
    },
});

const emit = defineEmits(["close", "updated"]);

const { accessToken, currentUserId } = useAuthSession();

// 기존 리뷰 정보
const originalRating = Number(props.review.rating);
const originalContent = props.review.content ?? "";

const originalImages = ref([
    ...(props.review.images ?? []),
]);

// 수정할 리뷰 정보
const reviewRating = ref(originalRating);
const reviewContent = ref(originalContent);

const reviewSubmitting = ref(false);
const reviewError = ref("");
const imageError = ref("");

// 이미지 첨부 및 미리보기
const reviewImageInput = ref(null);
const reviewImages = ref([]);

// keep: 기존 이미지 유지
// replace: 기존 이미지 전체 교체
// remove: 기존 이미지 전체 삭제
const imageMode = ref("keep");

// 로그인 회원과 리뷰 작성자가 일치하는지 확인합니다.
const isOwner = computed(() =>
    Boolean(
        accessToken.value &&
        currentUserId.value &&
        props.review.writerId === currentUserId.value
    )
);

// 기존 리뷰 본문 변경 여부
const contentChanged = computed(() =>
    reviewContent.value !== originalContent
);

// 별점 또는 리뷰 내용이 바뀐 경우에만 수정하기 버튼을 활성화합니다.
// (이미지 변경만으로는 활성화되지 않습니다.)
const hasChanges = computed(() =>
    reviewRating.value !== originalRating ||
    contentChanged.value
);

// 현재 수정 요청이 가능한지 확인합니다.
const canSubmit = computed(() =>
    isOwner.value &&
    hasChanges.value &&
    !reviewSubmitting.value &&
    Number.isInteger(reviewRating.value) &&
    reviewRating.value >= 1 &&
    reviewRating.value <= 5 &&
    Boolean(reviewContent.value.trim())
);

// 기존 이미지 경로를 화면에서 사용할 수 있도록 처리합니다.
function reviewImageSrc(imagePath) {
    if (!imagePath) {
        return "";
    }

    if (/^https?:\/\//i.test(imagePath)) {
        return imagePath;
    }

    const baseUrl = (
        import.meta.env.VITE_SPRING_BASE_URL || ""
    ).replace(/\/$/, "");

    const path = imagePath.startsWith("/")
        ? imagePath
        : `/${imagePath}`;

    return `${baseUrl}${path}`;
}

// 새롭게 선택한 이미지의 미리보기 데이터를 초기화합니다.
function clearReviewImages() {
    reviewImages.value.forEach((image) => {
        URL.revokeObjectURL(image.previewUrl);
    });

    reviewImages.value = [];

    if (reviewImageInput.value) {
        reviewImageInput.value.value = "";
    }
}

// 이미지 수정 방식을 변경합니다.
function changeImageMode(mode) {
    if (reviewSubmitting.value) {
        return;
    }

    clearReviewImages();

    imageMode.value = mode;
    imageError.value = "";
    reviewError.value = "";
}

// 이미지 첨부 및 유효성 검사
function onReviewImagesChange(event) {
    imageError.value = "";

    const files = Array.from(
        event.target.files || []
    );

    for (const file of files) {
        const extension = file.name
            .split(".")
            .pop()
            ?.toLowerCase();

        const allowedExtensions = [
            "jpg",
            "jpeg",
            "png",
            "webp",
        ];

        if (
            !allowedExtensions.includes(extension) ||
            file.size === 0 ||
            file.size > 5 * 1024 * 1024
        ) {
            imageError.value =
                "리뷰 이미지는 JPG, JPEG, PNG, WebP 형식이며 장당 5MB 이하여야 합니다.";

            continue;
        }

        if (reviewImages.value.length >= 5) {
            imageError.value =
                "리뷰 이미지는 최대 5장까지 등록할 수 있습니다.";

            break;
        }

        reviewImages.value.push({
            file,
            previewUrl: URL.createObjectURL(file),
        });
    }

    event.target.value = "";
}

// 새롭게 첨부한 이미지 개별 삭제
function removeReviewImage(index) {
    if (reviewSubmitting.value) {
        return;
    }

    const [image] = reviewImages.value.splice(
        index,
        1
    );

    if (image) {
        URL.revokeObjectURL(image.previewUrl);
    }

    imageError.value = "";
}

// 리뷰 수정 모달 닫기
function closeReview() {
    if (reviewSubmitting.value) {
        return;
    }

    emit("close");
}

// 리뷰 수정 요청
async function submitReview() {
    reviewError.value = "";

    if (!isOwner.value) {
        reviewError.value =
            "본인이 작성한 리뷰만 수정할 수 있습니다.";

        return;
    }

    if (!reviewContent.value.trim()) {
        reviewError.value =
            "리뷰 내용을 입력해주세요.";

        return;
    }

    if (!canSubmit.value) {
        return;
    }

    let images = null;

    // 기존 이미지 전체 삭제
    if (imageMode.value === "remove") {
        images = [];
    }

    // 기존 이미지 전체 교체
    if (imageMode.value === "replace") {
        images = reviewImages.value.map(
            (image) => image.file
        );
    }

    // 수정하지 않은 기존 본문은 그대로 유지합니다.
    const content = contentChanged.value
        ? reviewContent.value.trim()
        : originalContent;

    reviewSubmitting.value = true;

    let updatedReview;

    try {
        updatedReview = await reviewService.update(
            props.review.id,
            {
                rating: reviewRating.value,
                content,
                images,
                contentChanged:
                    content !== originalContent,
            }
        );
    } catch (error) {
        reviewError.value =
            error.message ||
            "리뷰를 수정하지 못했습니다.";

        return;
    } finally {
        reviewSubmitting.value = false;
    }

    // 수정된 리뷰 정보를 부모 컴포넌트에 전달합니다.
    emit("updated", updatedReview);

    // 수정 요청이 완료된 이후 모달을 닫습니다.
    emit("close");
}

// 컴포넌트 제거 시 이미지 미리보기 메모리를 정리합니다.
onBeforeUnmount(() => {
    clearReviewImages();
});
</script>

<template>
    <Teleport to="body">
        <div
            class="common-modal-backdrop"
            @click.self="closeReview"
            @keydown.esc="closeReview"
        >
            <form
                class="common-modal review-write-modal review-edit-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="review-edit-title"
                @submit.prevent="submitReview"
            >
                <button
                    type="button"
                    class="common-modal-close"
                    aria-label="닫기"
                    :disabled="reviewSubmitting"
                    @click="closeReview"
                >
                    ×
                </button>

                <small>EDIT REVIEW</small>

                <h2 id="review-edit-title">
                    리뷰 수정
                </h2>

                <p v-if="productName">
                    {{ productName }}
                </p>

                <!-- 별점 수정 -->
                <fieldset
                    class="review-rating-field"
                    :disabled="reviewSubmitting || !isOwner"
                >
                    <legend>별점</legend>

                    <div
                        class="review-rating-stars"
                        role="group"
                        aria-label="별점 선택"
                    >
                        <button
                            v-for="score in [1, 2, 3, 4, 5]"
                            :key="score"
                            type="button"
                            :class="{
                                selected: score <= reviewRating
                            }"
                            :aria-label="`${score}점`"
                            :aria-pressed="score === reviewRating"
                            @click="reviewRating = score"
                        >
                            ★
                        </button>

                        <strong>
                            {{ reviewRating }}점
                        </strong>
                    </div>
                </fieldset>

                <!-- 리뷰 내용 수정 -->
                <label>
                    리뷰 내용

                    <textarea
                        v-model="reviewContent"
                        rows="5"
                        placeholder="상품에 대한 리뷰를 남겨주세요."
                        :disabled="reviewSubmitting || !isOwner"
                    ></textarea>
                </label>

                <!-- 리뷰 이미지 수정 -->
                <section class="review-image-upload">
                    <header class="review-image-upload-head">
                        <div>
                            <b>리뷰 사진</b>

                            <small>
                                선택 · 최대 5장 / 장당 5MB
                            </small>
                        </div>
                    </header>

                    <!-- 이미지 수정 방식 선택 -->
                    <fieldset
                        class="review-image-mode"
                        :disabled="reviewSubmitting || !isOwner"
                    >
                        <legend class="review-edit-sr-only">
                            이미지 수정 방식
                        </legend>

                        <button
                            type="button"
                            :class="{
                                selected: imageMode === 'keep'
                            }"
                            :aria-pressed="imageMode === 'keep'"
                            @click="changeImageMode('keep')"
                        >
                            기존 사진 유지
                        </button>

                        <button
                            type="button"
                            :class="{
                                selected: imageMode === 'replace'
                            }"
                            :aria-pressed="imageMode === 'replace'"
                            @click="changeImageMode('replace')"
                        >
                            전체 교체
                        </button>

                        <button
                            v-if="originalImages.length"
                            type="button"
                            :class="{
                                selected: imageMode === 'remove'
                            }"
                            :aria-pressed="imageMode === 'remove'"
                            @click="changeImageMode('remove')"
                        >
                            전체 삭제
                        </button>
                    </fieldset>

                    <!-- 기존 이미지 유지 -->
                    <template v-if="imageMode === 'keep'">
                        <ul
                            v-if="originalImages.length"
                            class="review-image-preview-list"
                        >
                            <li
                                v-for="image in originalImages"
                                :key="image.id"
                            >
                                <img
                                    :src="reviewImageSrc(image.imagePath)"
                                    alt="기존 리뷰 이미지"
                                />
                            </li>
                        </ul>

                        <p
                            v-else
                            class="review-image-notice"
                        >
                            등록된 사진이 없습니다.
                        </p>
                    </template>

                    <!-- 기존 이미지 전체 교체 -->
                    <template v-else-if="imageMode === 'replace'">
                        <p class="review-image-notice">
                            수정 완료 시 기존 사진은 모두 삭제되고
                            새롭게 선택한 사진으로 교체됩니다.
                        </p>

                        <button
                            type="button"
                            class="review-image-upload-button"
                            :disabled="reviewSubmitting || !isOwner"
                            @click="reviewImageInput?.click()"
                        >
                            사진 선택
                        </button>

                        <input
                            ref="reviewImageInput"
                            type="file"
                            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
                            multiple
                            :disabled="reviewSubmitting || !isOwner"
                            @change="onReviewImagesChange"
                        />

                        <ul
                            v-if="reviewImages.length"
                            class="review-image-preview-list"
                        >
                            <li
                                v-for="(image, index) in reviewImages"
                                :key="image.previewUrl"
                            >
                                <img
                                    :src="image.previewUrl"
                                    :alt="image.file.name"
                                />

                                <button
                                    type="button"
                                    :disabled="reviewSubmitting"
                                    :aria-label="`${image.file.name} 삭제`"
                                    @click="removeReviewImage(index)"
                                >
                                    ×
                                </button>
                            </li>
                        </ul>

                        <small class="review-image-count">
                            {{ reviewImages.length }} / 5장
                        </small>
                    </template>

                    <!-- 기존 이미지 전체 삭제 -->
                    <p
                        v-else
                        class="review-image-notice delete-notice"
                    >
                        수정 완료 시 기존 사진이 모두 삭제됩니다.
                    </p>

                    <p
                        v-if="imageError"
                        class="review-edit-error"
                        role="alert"
                    >
                        {{ imageError }}
                    </p>
                </section>

                <!-- 오류 메시지 -->
                <p
                    v-if="!isOwner"
                    class="review-edit-error"
                    role="alert"
                >
                    본인이 작성한 리뷰만 수정할 수 있습니다.
                </p>

                <p
                    v-if="reviewError"
                    class="review-edit-error"
                    role="alert"
                >
                    {{ reviewError }}
                </p>

                <!-- 수정 및 취소 버튼 -->
                <div class="common-modal-actions">
                    <button
                        type="button"
                        class="secondary"
                        :disabled="reviewSubmitting"
                        @click="closeReview"
                    >
                        취소
                    </button>

                    <button
                        type="submit"
                        class="primary"
                        :disabled="!canSubmit"
                    >
                        {{
                            reviewSubmitting
                                ? "수정 중..."
                                : "수정 완료"
                        }}
                    </button>
                </div>
            </form>
        </div>
    </Teleport>
</template>

<style scoped>
.review-edit-modal {
    max-height: calc(100dvh - 40px);
    overflow-y: auto;
}

.review-image-mode {
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    margin: 12px 0 0;
    padding: 0;
    border: 0;
}

.review-image-mode button {
    flex: 1;
    min-height: 36px;
    border: 1px solid #ddc9b7;
    border-radius: 7px;
    background: #fff;
    padding: 7px 10px;
    color: #815037;
    font: inherit;
    font-size: 12px;
    font-weight: 800;
    cursor: pointer;
}

.review-image-mode button.selected {
    border-color: #95643d;
    background: #95643d;
    color: #fff;
}

.review-image-notice {
    margin: 12px 0;
    color: #8a7768;
    font-size: 12px;
    line-height: 1.6;
    text-align: left;
}

.review-image-notice.delete-notice {
    color: #c65b49;
}

.review-image-count {
    display: block;
    margin-top: 9px;
    color: #8a7768;
    font-size: 11px;
    text-align: right;
}

.review-edit-error {
    margin: 12px 0 0;
    color: #c65b49;
    font-size: 12px;
    line-height: 1.6;
    text-align: left;
}

.review-edit-modal button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
}

.review-edit-sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
}
</style>
