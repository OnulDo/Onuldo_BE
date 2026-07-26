package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.entity.PartyMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyMemberRepository extends JpaRepository<PartyMember, PartyMemberId> {

    List<PartyMember> findByParty_IdOrderByJoinedAtAsc(Long partyId);

    int countByParty_Id(Long partyId);

    boolean existsByParty_IdAndUser_Id(Long partyId, Long userId);
}
