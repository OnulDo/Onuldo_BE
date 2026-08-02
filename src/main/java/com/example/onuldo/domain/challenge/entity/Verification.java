package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "verification",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_verification_photo_url",
                        columnNames = {"photo_url"}
                )
        }
)
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @Column(name = "verification_date", nullable = false)
    private LocalDate verificationDate;

    @Column(name = "photo_url", nullable = false, length = 255)
    private String photoUrl;

    @Column(name = "exif_data", columnDefinition = "TEXT")
    private String exifData;

    @Column(name = "rekognition_result", columnDefinition = "TEXT")
    private String rekognitionResult;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationReviewStatus review = VerificationReviewStatus.PENDING;

    @Column(name = "day_score", precision = 5, scale = 2)
    private BigDecimal dayScore;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
