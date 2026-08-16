package com.kk.klist.domain.bucketlist.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kk.klist.domain.bucketlist.domain.exception.BucketListErrorCode;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListException;
import com.kk.klist.domain.bucketlist.fixture.BucketListFixture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BucketListTest {

    @Test
    @DisplayName("정상적인 정보로 버킷리스트를 생성하면 미완료 상태로 생성된다")
    void create_whenValidInput_createsIncompleteBucketList() {
        // given

        // when
        BucketList bucketList = BucketListFixture.incompleteBucketList();

        // then
        assertThat(bucketList.getMemberId()).isEqualTo(1L);
        assertThat(bucketList.getCategory().getCode()).isEqualTo("K_DRAMA");
        assertThat(bucketList.isCompleted()).isFalse();
        assertThat(bucketList.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("위도만 입력하면 IncompleteCoordinates 예외가 발생된다")
    void create_whenOnlyLatitudeProvided_throwsIncompleteCoordinatesException() {
        // given
        Category category = Category.create("K_DRAMA", "K-drama");

        // when & then
        assertThatThrownBy(() -> BucketList.create(
                1L, category, "title", null, null, null,
                new BigDecimal("37.5"), null, null
        ))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.INCOMPLETE_COORDINATES));
    }

    @Test
    @DisplayName("위도 범위를 벗어나면 InvalidCoordinates 예외가 발생된다")
    void create_whenLatitudeOutOfRange_throwsInvalidCoordinatesException() {
        // given
        Category category = Category.create("K_DRAMA", "K-drama");

        // when & then
        assertThatThrownBy(() -> BucketList.create(
                1L, category, "title", null, null, null,
                new BigDecimal("91"), new BigDecimal("126"), null
        ))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.INVALID_COORDINATES));
    }

    @Test
    @DisplayName("유효한 정보로 수정하면 버킷리스트 정보가 변경된다")
    void update_whenValidInput_updatesBucketList() {
        // given
        BucketList bucketList = BucketListFixture.incompleteBucketList();
        Category category = Category.create("K_BEAUTY", "K-beauty");

        // when
        bucketList.update(
                category, "Updated bucket list", "Updated description.",
                "Seongsu-dong", "Seongsu-dong, Seoul",
                new BigDecimal("37.5446000"), new BigDecimal("127.0557000"),
                "https://example.com/images/updated.jpg"
        );

        // then
        assertThat(bucketList.getTitle()).isEqualTo("Updated bucket list");
        assertThat(bucketList.getCategory()).isEqualTo(category);
        assertThat(bucketList.getPlaceName()).isEqualTo("Seongsu-dong");
    }

    @Test
    @DisplayName("위도만 전달하여 수정하면 IncompleteCoordinates 예외가 발생된다")
    void update_whenOnlyLatitudeProvided_throwsIncompleteCoordinatesException() {
        // given
        BucketList bucketList = BucketListFixture.incompleteBucketList();

        // when & then
        assertThatThrownBy(() -> bucketList.update(
                Category.create("K_BEAUTY", "K-beauty"), "Updated", null,
                null, null, new BigDecimal("37.5446000"), null, null
        ))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.INCOMPLETE_COORDINATES));
    }

    @Test
    @DisplayName("미완료 버킷리스트를 완료 처리하면 완료 여부와 완료 시각이 변경된다")
    void updateCompletion_whenCompleted_setsCompletedAt() {
        // given
        BucketList bucketList = BucketListFixture.incompleteBucketList();
        LocalDateTime completionTime = LocalDateTime.of(2026, 8, 16, 18, 0);

        // when
        bucketList.complete(completionTime);

        // then
        assertThat(bucketList.isCompleted()).isTrue();
        assertThat(bucketList.getCompletedAt()).isEqualTo(completionTime);
    }

    @Test
    @DisplayName("완료된 버킷리스트를 완료 취소하면 완료 여부가 false이고 완료 시각이 초기화된다")
    void updateCompletion_whenCanceled_clearsCompletedAt() {
        // given
        BucketList bucketList = BucketListFixture.incompleteBucketList();
        bucketList.complete(LocalDateTime.of(2026, 8, 16, 18, 0));

        // when
        bucketList.cancelCompletion();

        // then
        assertThat(bucketList.isCompleted()).isFalse();
        assertThat(bucketList.getCompletedAt()).isNull();
    }
}
