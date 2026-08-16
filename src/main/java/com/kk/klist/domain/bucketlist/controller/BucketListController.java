package com.kk.klist.domain.bucketlist.controller;

import com.kk.klist.domain.bucketlist.dto.request.BucketListCreateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListCompletionUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.response.BucketListCreateResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListDetailResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListSummaryResponse;
import com.kk.klist.domain.bucketlist.service.BucketListService;
import com.kk.klist.global.response.ApiResponse;
import com.kk.klist.global.response.PageResponse;
import com.kk.klist.global.security.auth.LoginUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bucket-lists")
@RequiredArgsConstructor
public class BucketListController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final BucketListService bucketListService;

    @PatchMapping("/{bucketListId}/completion")
    public ResponseEntity<Void> updateBucketListCompletion(
            @LoginUser Long userId,
            @PathVariable Long bucketListId,
            @Valid @RequestBody BucketListCompletionUpdateRequest request
    ) {
        bucketListService.updateBucketListCompletion(userId, bucketListId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{bucketListId}")
    public ResponseEntity<Void> updateBucketList(
            @LoginUser Long userId,
            @PathVariable Long bucketListId,
            @Valid @RequestBody BucketListUpdateRequest request
    ) {
        bucketListService.updateBucketList(userId, bucketListId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bucketListId}")
    public ResponseEntity<Void> deleteBucketList(
            @LoginUser Long userId,
            @PathVariable Long bucketListId
    ) {
        bucketListService.deleteBucketList(userId, bucketListId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bucketListId}")
    public ResponseEntity<ApiResponse<BucketListDetailResponse>> findBucketList(
            @LoginUser Long userId,
            @PathVariable Long bucketListId,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude
    ) {
        BucketListDetailResponse response =
                bucketListService.findBucketList(userId, bucketListId, latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BucketListSummaryResponse>>> findBucketLists(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(required = false) Boolean completed,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PageResponse<BucketListSummaryResponse> response =
                bucketListService.findBucketLists(userId, category, completed, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BucketListCreateResponse>> createBucketList(
            @LoginUser Long userId,
            @Valid @RequestBody BucketListCreateRequest request
    ) {
        BucketListCreateResponse response = bucketListService.createBucketList(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
