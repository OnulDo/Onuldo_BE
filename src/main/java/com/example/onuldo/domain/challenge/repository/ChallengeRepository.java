package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            order by c.participantCount desc, c.id desc
            """)
    Page<Challenge> findChallenges(
            @Param("status") ChallengeStatus status,
            @Param("category") ChallengeCategory category,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
