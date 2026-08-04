package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

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

    // HOME-03/04/07: 홈 화면에서는 PASS 외에 PENDING/MANUAL_REVIEW/AUTO_FAIL도
    // "검토대기"/"실패" 상태 표시에 필요해서, #15 피드용 쿼리(PASS만 조회)와 달리
    // review 상태 무관하게 오늘의 인증 기록 전체를 조회함
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

}
