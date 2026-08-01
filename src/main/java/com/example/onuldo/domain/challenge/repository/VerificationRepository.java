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

    // 파티 진행 피드: 오늘 AUTO_PASS(자동 통과)된 인증만 "인증 완료"로 집계
    @Query("""
            SELECT v
            FROM Verification v
            JOIN FETCH v.participation p
            JOIN FETCH p.user u
            WHERE p.party.id = :partyId
            AND v.verificationDate = :date
            AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.AUTO_PASS
            """)
    List<Verification> findTodayAutoPassVerificationsByPartyId(
            @Param("partyId") Long partyId,
            @Param("date") LocalDate date
    );

    boolean existsByParticipation_IdAndVerificationDate(Long participationId, LocalDate verificationDate);

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

    boolean existsByPhotoUrl(String photoUrl);

    Optional<Verification> findTopByParticipation_IdAndVerificationDateOrderByIdDesc(
            Long participationId,
            LocalDate verificationDate
    );

    List<Verification> findAllByParticipation_IdIn(Collection<Long> participationIds);

    List<Verification> findAllByParticipation_IdInAndVerificationDate(
            Collection<Long> participationIds,
            LocalDate verificationDate
    );
}
