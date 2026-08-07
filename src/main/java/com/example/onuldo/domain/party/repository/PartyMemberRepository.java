package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.entity.PartyMemberId;
import com.example.onuldo.domain.party.enums.PartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartyMemberRepository extends JpaRepository<PartyMember, PartyMemberId> {

    List<PartyMember> findByParty_IdOrderByJoinedAtAsc(Long partyId);

    List<PartyMember> findByParty_IdInOrderByJoinedAtAsc(List<Long> partyIds);

    int countByParty_Id(Long partyId);

    boolean existsByParty_IdAndUser_Id(Long partyId, Long userId);

    // 파티가 아직 시작 전(WAITING)이면 Participation이 없으므로, 같은 챌린지의 다른 파티 중복 소속 여부는
    // PartyMember-PartyChallenge를 직접 조인해 확인한다.
    @Query("""
        SELECT COUNT(pm) > 0
        FROM PartyMember pm
        JOIN PartyChallenge pc ON pc.party.id = pm.party.id
        WHERE pm.user.id = :userId
        AND pc.challenge.id = :challengeId
        AND pm.party.status IN :statuses
    """)
    boolean existsActiveMembershipByUser_IdAndChallenge_Id(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("statuses") Collection<PartyStatus> statuses
    );
}
