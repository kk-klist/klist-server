package com.kk.klist.domain.bucketlist.domain.entity;

import com.kk.klist.domain.bucketlist.domain.exception.BucketListErrorCode;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListException;
import com.kk.klist.global.util.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bucket_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BucketList extends BaseTimeEntity {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String placeName;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 2048)
    private String imageUrl;

    @Column(nullable = false)
    private boolean completed;

    private LocalDateTime completedAt;

    @Builder
    private BucketList(Long memberId, Category category, String title, String description,
            String placeName, String address, BigDecimal latitude, BigDecimal longitude, String imageUrl) {
        validateCoordinates(latitude, longitude);
        this.memberId = memberId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.completed = false;
        this.completedAt = null;
    }

    public static BucketList create(Long memberId, Category category, String title, String description,
            String placeName, String address, BigDecimal latitude, BigDecimal longitude, String imageUrl) {
        return BucketList.builder()
                .memberId(memberId)
                .category(category)
                .title(title)
                .description(description)
                .placeName(placeName)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .imageUrl(imageUrl)
                .build();
    }

    public void update(Category category, String title, String description, String placeName,
            String address, BigDecimal latitude, BigDecimal longitude, String imageUrl) {
        validateCoordinates(latitude, longitude);
        this.category = category;
        this.title = title;
        this.description = description;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }

    public void complete(LocalDateTime completionTime) {
        this.completed = true;
        this.completedAt = completionTime;
    }

    public void cancelCompletion() {
        this.completed = false;
        this.completedAt = null;
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BucketListException(BucketListErrorCode.INCOMPLETE_COORDINATES);
        }
        if (latitude == null) {
            return;
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0
                || longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new BucketListException(BucketListErrorCode.INVALID_COORDINATES);
        }
    }
}
