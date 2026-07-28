package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.dto.ChallengeContentBlockDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private static final List<Integer> DEFAULT_DURATION_OPTIONS = List.of(2, 4, 8, 12);
    private static final List<Integer> DEFAULT_DEPOSIT_OPTIONS = List.of(10000, 20000, 30000, 50000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "explain_content", nullable = false, length = 100)
    private String explainContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private List<ChallengeContentBlockDto> description;

    @Column(name = "caption_img_url", nullable = false, length = 500)
    private String captionImgUrl;

    @Column(name = "verify_method_content", nullable = false, columnDefinition = "TEXT")
    private String verifyMethodContent;

    @Column(name = "verification_example_photo_url", nullable = false, length = 500)
    private String verificationExamplePhotoUrl;

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
    private List<Integer> durationOptionList = DEFAULT_DURATION_OPTIONS;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deposit_option_list", columnDefinition = "json")
    private List<Integer> depositOptionList = DEFAULT_DEPOSIT_OPTIONS;

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
