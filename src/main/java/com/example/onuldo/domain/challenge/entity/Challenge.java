package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "challenge")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "explain_content", nullable = false, length = 100)
    private String explainContent;

    @Column(name = "caption_img_url", nullable = false, length = 500)
    private String captionImgUrl;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ChallengeCategory category;

    @Column(name = "time_start")
    private LocalTime timeStart;

    @Column(name = "time_end")
    private LocalTime timeEnd;

    // 챌린지 진행 기간
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "duration_option_list", columnDefinition = "json")
    private List<Integer> durationOptionList = List.of(2, 4, 8, 12);

    // 상금 리스트
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deposit_option_list", columnDefinition = "json")
    private List<Integer> depositOptionList = List.of(10000, 20000, 30000, 50000);

    // 성공 조건 리스트
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "success_option_list", columnDefinition = "json")
    private List<String> successConditionList;

    // 실패 조건 리스트
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failure_option_list", columnDefinition = "json")
    private List<String> failureConditionList;

    // AWS Rekognition을 통해 받은 라벨 검증 필요 리스트
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_label_list", columnDefinition = "json")
    private List<String> verificationLabelList;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeStatus status = ChallengeStatus.ACTIVE;
}
