package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);
    @Query("""
        SELECT COALESCE(SUM(p.depositAmount), 0)
        FROM Participation p
        WHERE p.user.id = :userId
        AND p.status = :status
    """)
    Long sumDepositAmountByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(p.depositAmount), 0)
        FROM Participation p
        WHERE p.user.id = :userId
        AND p.status <> :status
    """)
    Long sumDepositAmountByUserIdAndStatusNot(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status
    );


    List<Participation> findAllByUser_IdOrderByIdDesc(Long userId);

    List<Participation> findAllByUser_IdAndStatusOrderByIdDesc(Long userId, ParticipationStatus status);

    List<Participation> findAllByUser_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
            Long userId,
            ParticipationStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Participation> findTopByUser_IdAndChallenge_IdAndStatusOrderByIdDesc(
            Long userId,
            Long challengeId,
            ParticipationStatus status
    );

    List<Participation> findAllByIdIn(Collection<Long> ids);

    @Query("""
        SELECT new com.example.onuldo.domain.challenge.repository.PartyCountProjection(p.party.id, COUNT(p))
        FROM Participation p
        WHERE p.party.id IN :partyIds
        AND p.status = :status
        GROUP BY p.party.id
    """)
    List<PartyCountProjection> findParticipationCountsByPartyIdInAndStatus(
            @Param("partyIds") Collection<Long> partyIds,
            @Param("status") ParticipationStatus status
    );
}
