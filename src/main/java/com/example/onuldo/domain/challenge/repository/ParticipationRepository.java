package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    List<Participation> findAllByUser_IdOrderByIdDesc(Long userId);

    List<Participation> findAllByUser_IdAndStatusOrderByIdDesc(Long userId, ParticipationStatus status);
}
