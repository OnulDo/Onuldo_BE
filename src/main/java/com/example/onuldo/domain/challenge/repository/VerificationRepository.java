package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // POI-08: 파티 정산 전 직접검토(MANUAL_REVIEW) 유예 여부 판단용 — verifiedAt은 검토 "요청" 시각
    boolean existsByParticipation_IdInAndReviewAndVerifiedAtAfter(
            Collection<Long> participationIds,
            VerificationReviewStatus review,
            LocalDateTime cutoff
    );

    // 파티 진행 피드: 오늘 PASS 처리된 인증만 "인증 완료"로 집계
    @Query("""
            SELECT v
            FROM Verification v
            JOIN FETCH v.participation p
            JOIN FETCH p.user u
            WHERE p.party.id = :partyId
            AND v.verificationDate = :date
            AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS
            """)
    List<Verification> findTodayAutoPassVerificationsByPartyId(
            @Param("partyId") Long partyId,
            @Param("date") LocalDate date
    );

    // 나의 파티 목록: 파티원별 오늘 인증 배지 표시를 위한 배치 조회 (findTodayAutoPassVerificationsByPartyId의 IN 버전)
    @Query("""
            SELECT v
            FROM Verification v
            JOIN FETCH v.participation p
            JOIN FETCH p.user u
            WHERE p.party.id IN :partyIds
            AND v.verificationDate = :date
            AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS
            """)
    List<Verification> findTodayAutoPassVerificationsByPartyIdIn(
            @Param("partyIds") Collection<Long> partyIds,
            @Param("date") LocalDate date
    );

    // 홈 "함께하는 파티" 섹션: 리뷰 상태(PENDING/검토대기 포함) 구분 표시를 위해 PASS 여부와 무관하게 오늘 인증 전체 조회
    @Query("""
            SELECT v
            FROM Verification v
            JOIN FETCH v.participation p
            JOIN FETCH p.user u
            WHERE p.party.id = :partyId
            AND v.verificationDate = :date
            """)
    List<Verification> findTodayVerificationsByPartyId(
            @Param("partyId") Long partyId,
            @Param("date") LocalDate date
    );

    long countByParticipation_IdAndReview(
            Long participationId,
            com.example.onuldo.domain.challenge.enums.VerificationReviewStatus review
    );

    @Query("""
            SELECT DISTINCT p.challenge.id
            FROM Verification v
            JOIN v.participation p
            WHERE p.user.id = :userId
            AND v.verificationDate = :date
            """)
    List<Long> findVerifiedChallengeIdsByUserIdAndVerificationDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT v
        FROM Verification v
        JOIN FETCH v.participation p
        WHERE p.user.id = :userId
            AND v.verificationDate = :date
            AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS
    """)
    List<Verification> findVerifiedVerificationsByUserIdAndVerificationDate(
            @Param("userId")
            Long userId,
            @Param("date")
            LocalDate date
    );

    @Query("""
        SELECT new com.example.onuldo.domain.challenge.repository.PartyCountProjection(p.party.id, COUNT(DISTINCT p.user.id))
        FROM Verification v
        JOIN v.participation p
        WHERE p.party.id IN :partyIds
        AND v.verificationDate = :date
        AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS
        AND p.status = com.example.onuldo.domain.challenge.enums.ParticipationStatus.ONGOING
        GROUP BY p.party.id
    """)
    List<PartyCountProjection> findAutoPassVerificationCountsByPartyIdInAndVerificationDate(
            @Param("partyIds") Collection<Long> partyIds,
            @Param("date") LocalDate date
    );

    boolean existsByPhotoUrl(String photoUrl);

    List<Verification> findAllByParticipation_IdIn(Collection<Long> participationIds);

    List<Verification> findAllByParticipation_IdInAndVerificationDate(
            Collection<Long> participationIds,
            LocalDate verificationDate
    );

    Optional<Verification> findByParticipation_IdAndVerificationDateAndReview(
            Long participationId,
            LocalDate verificationDate,
            VerificationReviewStatus review
    );
}
