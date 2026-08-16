package com.kk.klist.domain.bucketlist.service;

import com.kk.klist.domain.bucketlist.domain.entity.BucketList;
import com.kk.klist.domain.bucketlist.domain.entity.Category;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListErrorCode;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListException;
import com.kk.klist.domain.bucketlist.dto.request.BucketListCreateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListCompletionUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.response.BucketListCreateResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListDetailResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListSummaryResponse;
import com.kk.klist.domain.bucketlist.repository.BucketListRepository;
import com.kk.klist.domain.bucketlist.repository.BucketListSearchCondition;
import com.kk.klist.domain.bucketlist.repository.CategoryRepository;
import com.kk.klist.global.response.PageResponse;
import com.kk.klist.global.util.TimeProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BucketListService {

    private static final String ALL_CATEGORIES = "ALL";
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final BucketListRepository bucketListRepository;
    private final CategoryRepository categoryRepository;
    private final TimeProvider timeProvider;

    @Transactional
    public BucketListCreateResponse createBucketList(Long memberId, BucketListCreateRequest request) {
        Category category = categoryRepository.findByCode(request.category())
                .orElseThrow(() -> new BucketListException(BucketListErrorCode.CATEGORY_NOT_FOUND));

        BucketList bucketList = BucketList.create(
                memberId,
                category,
                request.title(),
                request.description(),
                request.placeName(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.imageUrl()
        );

        return BucketListCreateResponse.from(bucketListRepository.save(bucketList));
    }

    public PageResponse<BucketListSummaryResponse> findBucketLists(Long memberId, String category,
            Boolean completed, Pageable pageable) {
        String categoryCode = resolveCategoryCode(category);
        BucketListSearchCondition condition = new BucketListSearchCondition(
                memberId,
                categoryCode,
                completed,
                pageable
        );
        Page<BucketListSummaryResponse> bucketLists = bucketListRepository.searchBucketList(condition)
                .map(BucketListSummaryResponse::from);
        return PageResponse.of(bucketLists);
    }

    public BucketListDetailResponse findBucketList(Long memberId, Long bucketListId,
            BigDecimal latitude, BigDecimal longitude) {
        validateCoordinates(latitude, longitude);
        BucketList bucketList = findOwnedBucketList(memberId, bucketListId);

        return BucketListDetailResponse.from(bucketList, calculateDistance(bucketList, latitude, longitude));
    }

    @Transactional
    public void updateBucketList(Long memberId, Long bucketListId, BucketListUpdateRequest request) {
        BucketList bucketList = findOwnedBucketList(memberId, bucketListId);
        Category category = categoryRepository.findByCode(request.category())
                .orElseThrow(() -> new BucketListException(BucketListErrorCode.CATEGORY_NOT_FOUND));
        bucketList.update(
                category,
                request.title(),
                request.description(),
                request.placeName(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.imageUrl()
        );
    }

    @Transactional
    public void deleteBucketList(Long memberId, Long bucketListId) {
        BucketList bucketList = findOwnedBucketList(memberId, bucketListId);
        bucketListRepository.delete(bucketList);
    }

    @Transactional
    public void updateBucketListCompletion(Long memberId, Long bucketListId,
            BucketListCompletionUpdateRequest request) {
        BucketList bucketList = findOwnedBucketList(memberId, bucketListId);
        if (request.isCompleted()) {
            bucketList.complete(timeProvider.now());
            return;
        }
        bucketList.cancelCompletion();
    }

    private BucketList findOwnedBucketList(Long memberId, Long bucketListId) {
        BucketList bucketList = bucketListRepository.findById(bucketListId)
                .orElseThrow(() -> new BucketListException(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));
        if (!bucketList.getMemberId().equals(memberId)) {
            throw new BucketListException(BucketListErrorCode.ACCESS_DENIED);
        }
        return bucketList;
    }

    private String resolveCategoryCode(String category) {
        if (ALL_CATEGORIES.equalsIgnoreCase(category)) {
            return null;
        }
        return categoryRepository.findByCode(category)
                .map(Category::getCode)
                .orElseThrow(() -> new BucketListException(BucketListErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BucketListException(BucketListErrorCode.INCOMPLETE_COORDINATES);
        }
        if (latitude == null) {
            return;
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BucketListException(BucketListErrorCode.INVALID_COORDINATES);
        }
    }

    private String calculateDistance(BucketList bucketList, BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || bucketList.getLatitude() == null) {
            return null;
        }

        double userLatitude = latitude.doubleValue();
        double userLongitude = longitude.doubleValue();
        double placeLatitude = bucketList.getLatitude().doubleValue();
        double placeLongitude = bucketList.getLongitude().doubleValue();
        double latitudeDifference = Math.toRadians(placeLatitude - userLatitude);
        double longitudeDifference = Math.toRadians(placeLongitude - userLongitude);
        double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(Math.toRadians(userLatitude)) * Math.cos(Math.toRadians(placeLatitude))
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        long distanceMeters = Math.round(EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine),
                Math.sqrt(1 - haversine)));

        if (distanceMeters < 1_000) {
            return distanceMeters + "m";
        }
        return BigDecimal.valueOf(distanceMeters)
                .divide(BigDecimal.valueOf(1_000), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "km";
    }
}
