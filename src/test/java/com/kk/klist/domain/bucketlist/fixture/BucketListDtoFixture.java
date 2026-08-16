package com.kk.klist.domain.bucketlist.fixture;

import com.kk.klist.domain.bucketlist.dto.request.BucketListCreateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListCompletionUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListUpdateRequest;
import java.math.BigDecimal;

public class BucketListDtoFixture {

    public static BucketListCreateRequest createRequest() {
        return new BucketListCreateRequest(
                "Explore a K-drama filming spot",
                "Visit famous K-drama shooting locations.",
                "K_DRAMA",
                "Bukchon Hanok Village",
                "Bukchon, Seoul",
                new BigDecimal("37.5826000"),
                new BigDecimal("126.9830000"),
                "https://example.com/images/bukchon.jpg"
        );
    }

    public static BucketListUpdateRequest updateRequest() {
        return new BucketListUpdateRequest(
                "Updated bucket list", "Updated description.", "K_BEAUTY",
                "Seongsu-dong", "Seongsu-dong, Seoul",
                new BigDecimal("37.5446000"), new BigDecimal("127.0557000"),
                "https://example.com/images/updated.jpg"
        );
    }

    public static BucketListCompletionUpdateRequest completionUpdateRequest(boolean isCompleted) {
        return new BucketListCompletionUpdateRequest(isCompleted);
    }
}
