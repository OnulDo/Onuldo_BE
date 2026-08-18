package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("""
            select c
            from Challenge c
            where c.status = :status
              and (:category is null or c.category = :category)
              and (
                    :keyword is null
                    or lower(c.name) like lower(concat('%', :keyword, '%'))
              )
              and (
                    :lastParticipantCount is null
                    or c.participantCount < :lastParticipantCount
                    or (c.participantCount = :lastParticipantCount and c.id < :lastId)
              )
            order by c.participantCount desc, c.id desc
            """)
    List<Challenge> findChallenges(
            @Param("status") ChallengeStatus status,
            @Param("category") ChallengeCategory category,
            @Param("keyword") String keyword,
            @Param("lastParticipantCount") Integer lastParticipantCount,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}
