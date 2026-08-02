package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    boolean existsByPhotoUrl(String fileId);

    Optional<Verification> findTopByParticipation_IdAndVerificationDateOrderByIdDesc(
            Long participationId,
            LocalDate verificationDate
    );

    List<Verification> findAllByParticipation_IdIn(Collection<Long> participationIds);

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

}
