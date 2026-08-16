package com.kk.klist.domain.bucketlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kk.klist.domain.bucketlist.dto.request.BucketListCreateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListCompletionUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.request.BucketListUpdateRequest;
import com.kk.klist.domain.bucketlist.dto.response.BucketListCreateResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListDetailResponse;
import com.kk.klist.domain.bucketlist.dto.response.BucketListSummaryResponse;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListErrorCode;
import com.kk.klist.domain.bucketlist.domain.exception.BucketListException;
import com.kk.klist.domain.bucketlist.fixture.BucketListFixture;
import com.kk.klist.domain.bucketlist.service.BucketListService;
import com.kk.klist.global.response.PageResponse;
import com.kk.klist.global.security.auth.CustomUserDetails;
import com.kk.klist.global.security.auth.Role;
import com.kk.klist.global.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BucketListController.class)
class BucketListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BucketListService bucketListService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    @DisplayName("PATCH /api/v1/bucket-lists/{id}/completion 요청이 유효하면 204가 반환된다")
    void updateBucketListCompletion_whenValidRequest_returns204() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;

        // when & then
        mockMvc.perform(patch("/api/v1/bucket-lists/{bucketListId}/completion", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isCompleted\":true}"))
                .andExpect(status().isNoContent());
        then(bucketListService).should(times(1)).updateBucketListCompletion(
                eq(memberId), eq(bucketListId), any(BucketListCompletionUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/bucket-lists/{id}/completion의 완료 여부가 없으면 400이 반환된다")
    void updateBucketListCompletion_whenCompletedMissing_returns400() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;

        // when & then
        mockMvc.perform(patch("/api/v1/bucket-lists/{bucketListId}/completion", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"))
                .andExpect(jsonPath("$.errors[0].field").value("isCompleted"));
    }

    @Test
    @DisplayName("PATCH /api/v1/bucket-lists/{id} 요청이 유효하면 204가 반환된다")
    void updateBucketList_whenValidRequest_returns204() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;

        // when & then
        mockMvc.perform(patch("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestBody()))
                .andExpect(status().isNoContent());
        then(bucketListService).should(times(1))
                .updateBucketList(eq(memberId), eq(bucketListId), any(BucketListUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/bucket-lists/{id}의 제목이 비어 있으면 400이 반환된다")
    void updateBucketList_whenTitleBlank_returns400() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;

        // when & then
        mockMvc.perform(patch("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestBody().replace("Updated bucket list", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bucket-lists/{id} 요청이 유효하면 204가 반환된다")
    void deleteBucketList_whenValidRequest_returns204() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;

        // when & then
        mockMvc.perform(delete("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        then(bucketListService).should(times(1)).deleteBucketList(memberId, bucketListId);
    }

    @Test
    @DisplayName("GET /api/v1/bucket-lists/{id} 요청이 유효하면 200과 상세 정보가 반환된다")
    void findBucketList_whenValidRequest_returns200WithDetail() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketListDetailResponse response = BucketListDetailResponse.from(
                BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId),
                "500m"
        );
        given(bucketListService.findBucketList(
                memberId,
                bucketListId,
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557")
        )).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId)))
                        .param("latitude", "37.5446")
                        .param("longitude", "127.0557"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bucketListId").value(bucketListId))
                .andExpect(jsonPath("$.data.category").value("K_DRAMA"))
                .andExpect(jsonPath("$.data.distance").value("500m"));
        then(bucketListService).should(times(1)).findBucketList(
                memberId,
                bucketListId,
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557")
        );
    }

    @Test
    @DisplayName("GET /api/v1/bucket-lists/{id}의 버킷리스트가 없으면 404가 반환된다")
    void findBucketList_whenNotFound_returns404() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 999L;
        given(bucketListService.findBucketList(memberId, bucketListId, null, null))
                .willThrow(new BucketListException(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUCKET_LIST_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/bucket-lists/{id}가 다른 사용자의 버킷리스트이면 403이 반환된다")
    void findBucketList_whenOwnedByOtherMember_returns403() throws Exception {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        given(bucketListService.findBucketList(memberId, bucketListId, null, null))
                .willThrow(new BucketListException(BucketListErrorCode.ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/bucket-lists/{bucketListId}", bucketListId)
                        .with(authentication(createAuthentication(memberId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/v1/bucket-lists 요청이 유효하면 200과 내 목록이 반환된다")
    void findBucketLists_whenValidRequest_returns200WithPage() throws Exception {
        // given
        Long memberId = 1L;
        BucketListSummaryResponse summary = BucketListSummaryResponse.from(
                BucketListFixture.incompleteBucketListWithId(21L));
        PageResponse<BucketListSummaryResponse> response = PageResponse.of(
                new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));
        given(bucketListService.findBucketLists(eq(memberId), eq("K_DRAMA"), eq(false), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/bucket-lists")
                        .with(authentication(createAuthentication(memberId)))
                        .param("category", "K_DRAMA")
                        .param("completed", "false")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].bucketListId").value(21L))
                .andExpect(jsonPath("$.data.content[0].category").value("K_DRAMA"))
                .andExpect(jsonPath("$.data.content[0].isCompleted").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(0));
        then(bucketListService).should(times(1))
                .findBucketLists(eq(memberId), eq("K_DRAMA"), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/bucket-lists 요청의 카테고리가 유효하지 않으면 400이 반환된다")
    void findBucketLists_whenCategoryInvalid_returns400() throws Exception {
        // given
        Long memberId = 1L;
        given(bucketListService.findBucketLists(eq(memberId), eq("K_STAR"), eq(null), any(Pageable.class)))
                .willThrow(new BucketListException(BucketListErrorCode.CATEGORY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/bucket-lists")
                        .with(authentication(createAuthentication(memberId)))
                        .param("category", "K_STAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUCKET_LIST_CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/bucket-lists 요청이 유효하면 201과 생성 정보가 반환된다")
    void createBucketList_whenValidRequest_returns201WithBody() throws Exception {
        // given
        Long memberId = 1L;
        BucketListCreateResponse response = BucketListCreateResponse.from(
                BucketListFixture.incompleteBucketListWithId(21L));
        given(bucketListService.createBucketList(eq(memberId), any(BucketListCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bucket-lists")
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bucketListId").value(21L))
                .andExpect(jsonPath("$.data.category").value("K_DRAMA"))
                .andExpect(jsonPath("$.data.isCompleted").value(false));
        then(bucketListService).should(times(1))
                .createBucketList(eq(memberId), any(BucketListCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/bucket-lists 요청의 제목이 비어 있으면 400이 반환된다")
    void createBucketList_whenTitleBlank_returns400() throws Exception {
        // given
        Long memberId = 1L;

        // when & then
        mockMvc.perform(post("/api/v1/bucket-lists")
                        .with(authentication(createAuthentication(memberId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody().replace("Explore a K-drama filming spot", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    private UsernamePasswordAuthenticationToken createAuthentication(Long memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, Role.USER);
        return new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
    }

    private String validRequestBody() {
        return """
                {
                  "title": "Explore a K-drama filming spot",
                  "description": "Visit famous K-drama shooting locations.",
                  "category": "K_DRAMA",
                  "placeName": "Bukchon Hanok Village",
                  "address": "Bukchon, Seoul",
                  "latitude": 37.5826,
                  "longitude": 126.9830,
                  "imageUrl": "https://example.com/images/bukchon.jpg"
                }
                """;
    }

    private String validUpdateRequestBody() {
        return """
                {
                  "title": "Updated bucket list",
                  "description": "Updated description.",
                  "category": "K_BEAUTY",
                  "placeName": "Seongsu-dong",
                  "address": "Seongsu-dong, Seoul",
                  "latitude": 37.5446,
                  "longitude": 127.0557,
                  "imageUrl": "https://example.com/images/updated.jpg"
                }
                """;
    }
}
