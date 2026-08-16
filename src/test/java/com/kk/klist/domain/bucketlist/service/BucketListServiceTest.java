package com.kk.klist.domain.bucketlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import com.kk.klist.domain.bucketlist.fixture.BucketListDtoFixture;
import com.kk.klist.domain.bucketlist.fixture.BucketListFixture;
import com.kk.klist.domain.bucketlist.repository.BucketListRepository;
import com.kk.klist.domain.bucketlist.repository.BucketListSearchCondition;
import com.kk.klist.domain.bucketlist.repository.CategoryRepository;
import com.kk.klist.global.response.PageResponse;
import com.kk.klist.global.util.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BucketListServiceTest {

    @InjectMocks
    private BucketListService bucketListService;

    @Mock
    private BucketListRepository bucketListRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TimeProvider timeProvider;

    @Test
    @DisplayName("본인의 버킷리스트를 완료 처리하면 완료 시각이 저장된다")
    void updateBucketListCompletion_whenCompleted_setsCompletionTime() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        LocalDateTime completionTime = LocalDateTime.of(2026, 8, 16, 18, 0);
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        BucketListCompletionUpdateRequest request = BucketListDtoFixture.completionUpdateRequest(true);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));
        given(timeProvider.now()).willReturn(completionTime);

        // when
        bucketListService.updateBucketListCompletion(memberId, bucketListId, request);

        // then
        assertThat(bucketList.isCompleted()).isTrue();
        assertThat(bucketList.getCompletedAt()).isEqualTo(completionTime);
        then(timeProvider).should(times(1)).now();
    }

    @Test
    @DisplayName("본인의 버킷리스트를 완료 취소하면 완료 시각이 초기화된다")
    void updateBucketListCompletion_whenCanceled_clearsCompletionTime() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        bucketList.complete(LocalDateTime.of(2026, 8, 16, 18, 0));
        BucketListCompletionUpdateRequest request = BucketListDtoFixture.completionUpdateRequest(false);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when
        bucketListService.updateBucketListCompletion(memberId, bucketListId, request);

        // then
        assertThat(bucketList.isCompleted()).isFalse();
        assertThat(bucketList.getCompletedAt()).isNull();
        then(timeProvider).should(never()).now();
    }

    @Test
    @DisplayName("다른 사용자의 버킷리스트 완료 상태를 변경하면 AccessDenied 예외가 발생된다")
    void updateBucketListCompletion_whenOwnedByOtherMember_throwsAccessDeniedException() {
        // given
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, 2L);
        BucketListCompletionUpdateRequest request = BucketListDtoFixture.completionUpdateRequest(true);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when & then
        assertThatThrownBy(() -> bucketListService.updateBucketListCompletion(1L, bucketListId, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.ACCESS_DENIED));
        then(timeProvider).should(never()).now();
    }

    @Test
    @DisplayName("존재하지 않는 버킷리스트 완료 상태를 변경하면 BucketListNotFound 예외가 발생된다")
    void updateBucketListCompletion_whenNotFound_throwsBucketListNotFoundException() {
        // given
        Long bucketListId = 999L;
        BucketListCompletionUpdateRequest request = BucketListDtoFixture.completionUpdateRequest(true);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.updateBucketListCompletion(1L, bucketListId, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));
        then(timeProvider).should(never()).now();
    }

    @Test
    @DisplayName("본인의 버킷리스트를 수정하면 요청 정보가 반영된다")
    void updateBucketList_whenOwnedBucketListExists_updatesBucketList() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        BucketListUpdateRequest request = BucketListDtoFixture.updateRequest();
        Category category = Category.create("K_BEAUTY", "K-beauty");
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));
        given(categoryRepository.findByCode(request.category())).willReturn(Optional.of(category));

        // when
        bucketListService.updateBucketList(memberId, bucketListId, request);

        // then
        assertThat(bucketList.getTitle()).isEqualTo(request.title());
        assertThat(bucketList.getCategory()).isEqualTo(category);
        assertThat(bucketList.getLatitude()).isEqualByComparingTo(request.latitude());
        then(bucketListRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 버킷리스트를 수정하면 BucketListNotFound 예외가 발생된다")
    void updateBucketList_whenNotFound_throwsBucketListNotFoundException() {
        // given
        Long bucketListId = 999L;
        BucketListUpdateRequest request = BucketListDtoFixture.updateRequest();
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.updateBucketList(1L, bucketListId, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));
        then(categoryRepository).should(never()).findByCode(any());
    }

    @Test
    @DisplayName("다른 사용자의 버킷리스트를 수정하면 AccessDenied 예외가 발생된다")
    void updateBucketList_whenOwnedByOtherMember_throwsAccessDeniedException() {
        // given
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, 2L);
        BucketListUpdateRequest request = BucketListDtoFixture.updateRequest();
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when & then
        assertThatThrownBy(() -> bucketListService.updateBucketList(1L, bucketListId, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.ACCESS_DENIED));
        then(categoryRepository).should(never()).findByCode(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 수정하면 CategoryNotFound 예외가 발생된다")
    void updateBucketList_whenCategoryNotFound_throwsCategoryNotFoundException() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        BucketListUpdateRequest request = BucketListDtoFixture.updateRequest();
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));
        given(categoryRepository.findByCode(request.category())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.updateBucketList(memberId, bucketListId, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.CATEGORY_NOT_FOUND));
        assertThat(bucketList.getTitle()).isNotEqualTo(request.title());
    }

    @Test
    @DisplayName("본인의 버킷리스트를 삭제하면 Repository 삭제가 호출된다")
    void deleteBucketList_whenOwnedBucketListExists_deletesBucketList() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when
        bucketListService.deleteBucketList(memberId, bucketListId);

        // then
        then(bucketListRepository).should(times(1)).delete(bucketList);
    }

    @Test
    @DisplayName("다른 사용자의 버킷리스트를 삭제하면 AccessDenied 예외가 발생된다")
    void deleteBucketList_whenOwnedByOtherMember_throwsAccessDeniedException() {
        // given
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, 2L);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when & then
        assertThatThrownBy(() -> bucketListService.deleteBucketList(1L, bucketListId))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.ACCESS_DENIED));
        then(bucketListRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 버킷리스트를 삭제하면 BucketListNotFound 예외가 발생된다")
    void deleteBucketList_whenNotFound_throwsBucketListNotFoundException() {
        // given
        Long bucketListId = 999L;
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.deleteBucketList(1L, bucketListId))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));
        then(bucketListRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("본인의 버킷리스트 상세 정보를 조회하면 상세 응답이 반환된다")
    void findBucketList_whenOwnedBucketListExists_returnsDetail() {
        // given
        Long memberId = 1L;
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, memberId);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when
        BucketListDetailResponse response = bucketListService.findBucketList(
                memberId,
                bucketListId,
                new BigDecimal("37.5826000"),
                new BigDecimal("126.9830000")
        );

        // then
        assertThat(response.bucketListId()).isEqualTo(bucketListId);
        assertThat(response.category()).isEqualTo("K_DRAMA");
        assertThat(response.distance()).isEqualTo("0m");
        then(bucketListRepository).should(times(1)).findById(bucketListId);
    }

    @Test
    @DisplayName("존재하지 않는 버킷리스트 상세 정보를 조회하면 BucketListNotFound 예외가 발생된다")
    void findBucketList_whenNotFound_throwsBucketListNotFoundException() {
        // given
        Long bucketListId = 999L;
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.findBucketList(1L, bucketListId, null, null))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.BUCKET_LIST_NOT_FOUND));
    }

    @Test
    @DisplayName("다른 사용자의 버킷리스트 상세 정보를 조회하면 AccessDenied 예외가 발생된다")
    void findBucketList_whenOwnedByOtherMember_throwsAccessDeniedException() {
        // given
        Long bucketListId = 21L;
        BucketList bucketList = BucketListFixture.incompleteBucketListWithIdAndMemberId(bucketListId, 2L);
        given(bucketListRepository.findById(bucketListId)).willReturn(Optional.of(bucketList));

        // when & then
        assertThatThrownBy(() -> bucketListService.findBucketList(1L, bucketListId, null, null))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("현재 위치의 위도만 전달하면 IncompleteCoordinates 예외가 발생된다")
    void findBucketList_whenOnlyLatitudeProvided_throwsIncompleteCoordinatesException() {
        // when & then
        assertThatThrownBy(() -> bucketListService.findBucketList(
                1L,
                21L,
                new BigDecimal("37.5826000"),
                null
        ))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.INCOMPLETE_COORDINATES));
        then(bucketListRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("전체 카테고리로 내 버킷리스트를 조회하면 페이징된 목록이 반환된다")
    void findBucketLists_whenCategoryAll_returnsPagedBucketLists() {
        // given
        Long memberId = 1L;
        PageRequest pageable = PageRequest.of(0, 10);
        BucketList bucketList = BucketListFixture.incompleteBucketListWithId(21L);
        given(bucketListRepository.searchBucketList(any(BucketListSearchCondition.class)))
                .willReturn(new PageImpl<>(List.of(bucketList), pageable, 1));

        // when
        PageResponse<BucketListSummaryResponse> response =
                bucketListService.findBucketLists(memberId, "ALL", false, pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().bucketListId()).isEqualTo(21L);
        assertThat(response.getContent().getFirst().isCompleted()).isFalse();
        ArgumentCaptor<BucketListSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(BucketListSearchCondition.class);
        then(bucketListRepository).should(times(1)).searchBucketList(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue().memberId()).isEqualTo(memberId);
        assertThat(conditionCaptor.getValue().categoryCode()).isNull();
        assertThat(conditionCaptor.getValue().completed()).isFalse();
    }

    @Test
    @DisplayName("존재하는 카테고리로 내 버킷리스트를 조회하면 해당 카테고리 조건이 전달된다")
    void findBucketLists_whenCategoryExists_passesCategoryCondition() {
        // given
        Long memberId = 1L;
        PageRequest pageable = PageRequest.of(0, 10);
        Category category = Category.create("K_DRAMA", "K-drama");
        given(categoryRepository.findByCode("K_DRAMA")).willReturn(Optional.of(category));
        given(bucketListRepository.searchBucketList(any(BucketListSearchCondition.class)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        bucketListService.findBucketLists(memberId, "K_DRAMA", null, pageable);

        // then
        ArgumentCaptor<BucketListSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(BucketListSearchCondition.class);
        then(bucketListRepository).should(times(1)).searchBucketList(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue().categoryCode()).isEqualTo("K_DRAMA");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 내 버킷리스트를 조회하면 CategoryNotFound 예외가 발생된다")
    void findBucketLists_whenCategoryNotFound_throwsCategoryNotFoundException() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        given(categoryRepository.findByCode("K_STAR")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.findBucketLists(1L, "K_STAR", null, pageable))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.CATEGORY_NOT_FOUND));
        then(bucketListRepository).should(never()).searchBucketList(any(BucketListSearchCondition.class));
    }

    @Test
    @DisplayName("정상적인 요청으로 버킷리스트를 생성하면 저장 결과가 반환된다")
    void createBucketList_whenValidRequest_savesBucketListAndReturnsResponse() {
        // given
        Long memberId = 1L;
        BucketListCreateRequest request = BucketListDtoFixture.createRequest();
        Category category = Category.create("K_DRAMA", "K-drama");
        given(categoryRepository.findByCode(request.category())).willReturn(Optional.of(category));
        given(bucketListRepository.save(any(BucketList.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        BucketListCreateResponse response = bucketListService.createBucketList(memberId, request);

        // then
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.category()).isEqualTo(request.category());
        assertThat(response.isCompleted()).isFalse();
        then(bucketListRepository).should(times(1)).save(any(BucketList.class));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 생성하면 CategoryNotFound 예외가 발생된다")
    void createBucketList_whenCategoryNotFound_throwsCategoryNotFoundException() {
        // given
        BucketListCreateRequest request = BucketListDtoFixture.createRequest();
        given(categoryRepository.findByCode(request.category())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.createBucketList(1L, request))
                .isInstanceOf(BucketListException.class)
                .satisfies(error -> assertThat(((BucketListException) error).getErrorCode())
                        .isEqualTo(BucketListErrorCode.CATEGORY_NOT_FOUND));
        then(bucketListRepository).should(never()).save(any(BucketList.class));
    }
}
