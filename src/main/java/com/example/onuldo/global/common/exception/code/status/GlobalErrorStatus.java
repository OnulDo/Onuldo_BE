package com.example.onuldo.global.common.exception.code.status;

import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.BaseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorStatus implements BaseCodeInterface {
    // 공통 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    _INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    _TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),

    // Auth 관련 에러
    _DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    _INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "로그인 또는 비밀번호가 일치하지 않습니다."),
    _LOGIN_LOCKED(
            HttpStatus.TOO_MANY_REQUESTS,
            "LOGIN_LOCKED",
            "로그인에 5회 실패하여 계정 로그인이 잠금되었습니다. 잠시 후 다시 시도해주세요."
    ),
    _INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "사용할 수 없는 닉네임이에요."),
    _NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_SHORT", "2자 이상 입력해주세요."),
    _NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_LONG", "8자 이내로 입력해주세요."),
    _PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT", "비밀번호는 8자 이상 입력해주세요."),
    _PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG", "비밀번호는 20자 이내로 입력해주세요."),
    _INVALID_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "INVALID_PASSWORD",
            "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    ),
    _TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS_REQUIRED", "필수 약관에 동의해주세요."),
    _INVALID_SOCIAL_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_SOCIAL_TOKEN", "유효하지 않거나 만료된 소셜 액세스 토큰입니다."),
    _OAUTH_PROVIDER_ERROR(
            HttpStatus.SERVICE_UNAVAILABLE,
            "OAUTH_PROVIDER_ERROR", "소셜 로그인 서버와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // User 관련 에러
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    _SIGNUP_BONUS_ALREADY_GRANTED(
            HttpStatus.CONFLICT,
            "SIGNUP_BONUS_ALREADY_GRANTED", "이미 신규 회원 가입 포인트가 지급되었습니다."),
    _PROFILE_UPDATE_FIELD_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "PROFILE_UPDATE_FIELD_REQUIRED","변경할 닉네임 또는 프로필 사진을 입력해주세요."),
    _WITHDRAWAL_BLOCKED_BY_ONGOING_CHALLENGE(
            HttpStatus.BAD_REQUEST,
            "WITHDRAWAL_BLOCKED_BY_ONGOING_CHALLENGE", "진행 중인 챌린지가 있어 회원 탈퇴할 수 없습니다."),
    _SETTLEMENT_COMPLETE_NOTIFICATION_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "SETTLEMENT_COMPLETE_NOTIFICATION_REQUIRED", "정산/환급 완료 알림은 끌 수 없습니다."),

    // 파일 관련 에러
    _FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),
    _FILE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "FILE_ID_REQUIRED", "fileId는 필수입니다."),
    _FILE_EXISTENCE_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "FILE_EXISTENCE_CHECK_FAILED", "파일 존재 여부 확인에 실패했습니다."),
    _IMAGE_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_FILE_REQUIRED", "업로드할 이미지 파일이 필요합니다."),
    _UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST,
            "UNSUPPORTED_IMAGE_TYPE", "JPEG 또는 PNG 이미지만 업로드할 수 있습니다."),
    _INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST,
            "INVALID_IMAGE_FORMAT", "실제 파일 형식이 JPEG 또는 PNG여야 합니다."),
    _JPEG_CONTENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST,
            "JPEG_CONTENT_TYPE_MISMATCH", "JPEG 파일은 image/jpeg 형식이어야 합니다."),
    _PNG_CONTENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST,
            "PNG_CONTENT_TYPE_MISMATCH", "PNG 파일은 image/png 형식이어야 합니다."),
    _IMAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "IMAGE_READ_FAILED", "업로드 이미지를 읽는 중 오류가 발생했습니다."),
    _S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_UPLOAD_FAILED", "S3 업로드에 실패했습니다."),
    _S3_BUCKET_REQUIRED(HttpStatus.BAD_REQUEST, "S3_BUCKET_REQUIRED", "S3 bucket 정보가 필요합니다."),
    _S3_FILE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "S3_FILE_ID_REQUIRED", "S3 file Id가 필요합니다."),
    _S3_BUCKET_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR,
            "S3_BUCKET_NOT_CONFIGURED", "AWS S3 버킷 설정이 필요합니다."),
    _S3_PUBLIC_BASE_URL_NOT_CONFIGURED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "S3_PUBLIC_BASE_URL_NOT_CONFIGURED", "AWS S3 publicBaseUrl 설정이 필요합니다."),

    // 챌린지 관련 에러
    _CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "존재하지 않는 챌린지입니다."),
    _INVALID_DEPOSIT_OPTION(HttpStatus.BAD_REQUEST, "INVALID_DEPOSIT_OPTION", "선택할 수 없는 도전금입니다."),
    _INVALID_DURATION_OPTION(HttpStatus.BAD_REQUEST, "INVALID_DURATION_OPTION", "선택할 수 없는 진행 기간입니다."),
    _INSUFFICIENT_POINT_FOR_CHALLENGE(HttpStatus.BAD_REQUEST,
            "INSUFFICIENT_POINT_FOR_CHALLENGE", "보유 포인트가 부족합니다."),
    _ALREADY_PARTICIPATING_CHALLENGE(HttpStatus.CONFLICT,
            "ALREADY_PARTICIPATING_CHALLENGE", "이미 참여 중인 챌린지입니다."),
    _CHALLENGE_PARTY_ALREADY_WAITING(HttpStatus.CONFLICT,
            "CHALLENGE_PARTY_ALREADY_WAITING", "이미 이 챌린지로 만든 파티에서 대기 중이에요."),
    _DUPLICATE_VERIFICATION_PHOTO(HttpStatus.CONFLICT,
            "DUPLICATE_VERIFICATION_PHOTO", "이미 인증에 사용한 사진입니다."),
    _ALREADY_VERIFIED_TODAY(HttpStatus.CONFLICT, "ALREADY_VERIFIED_TODAY", "오늘은 이미 인증했습니다."),
    _PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "PARTICIPATION_NOT_FOUND", "해당 챌린지 참여 기록을 찾을 수 없습니다."),
    _CHALLENGE_NOT_STARTED(HttpStatus.CONFLICT, "CHALLENGE_NOT_STARTED", "아직 시작되지 않은 챌린지입니다."),
    _AUTO_FAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTO_FAIL_VERIFICATION_NOT_FOUND", "오늘 자동 실패 처리된 인증 기록을 찾을 수 없습니다."),
    _ALREADY_MANUALLY_REVIEWED(HttpStatus.CONFLICT, "ALREADY_MANUALLY_REVIEWED", "오늘 이미 수동 검토 처리되었습니다."),
    _VERIFICATION_RESULT_SERIALIZATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "VERIFICATION_RESULT_SERIALIZATION_FAILED", "인증 결과 저장 중 JSON 변환에 실패했습니다."),
    _PARTICIPATION_PARTY_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
            "PARTICIPATION_PARTY_NOT_ALLOWED", "개인 참여에는 party가 연결되면 안 됩니다."),
    _PARTICIPATION_PARTY_REQUIRED(HttpStatus.BAD_REQUEST,
            "PARTICIPATION_PARTY_REQUIRED", "party 참여에는 party가 필요합니다."),

    // 파티 관련 에러
    _INSUFFICIENT_POINT_FOR_PARTY(HttpStatus.CONFLICT,
            "INSUFFICIENT_POINT_FOR_PARTY", "보유 포인트가 도전금보다 부족합니다."),
    _INVITE_CODE_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INVITE_CODE_GENERATION_FAILED",
            "초대코드 생성에 반복적으로 실패했습니다. 잠시 후 다시 시도해주세요."
    ),
    _INVALID_MAX_MEMBERS(HttpStatus.BAD_REQUEST,
            "INVALID_MAX_MEMBERS", "모집 인원은 2명 이상 5명 이하로 설정해주세요."),
    _PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY_NOT_FOUND", "존재하지 않는 파티입니다."),
    _NOT_PARTY_MEMBER(HttpStatus.FORBIDDEN, "NOT_PARTY_MEMBER", "해당 파티의 파티원이 아닙니다."),

    // PAR-ERR-01: 초대코드 오류 4종
    _INVALID_INVITE_CODE(
            HttpStatus.BAD_REQUEST,
            "INVALID_INVITE_CODE",
            "잘못된 초대코드예요. 코드를 다시 확인해주세요."
    ),
    _PARTY_ALREADY_STARTED(HttpStatus.CONFLICT, "PARTY_ALREADY_STARTED", "이미 시작된 파티예요."),
    _PARTY_ALREADY_FINISHED(HttpStatus.CONFLICT, "PARTY_ALREADY_FINISHED", "이미 종료된 파티예요."),
    _PARTY_DISSOLVED(HttpStatus.CONFLICT, "PARTY_DISSOLVED", "해체된 파티예요."),
    _PARTY_FULL(HttpStatus.CONFLICT, "PARTY_FULL", "파티 인원이 가득 찼어요."),
    _INVITE_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "INVITE_CODE_EXPIRED", "만료된 초대코드예요."),

    // PAR-04: 초대코드로 이미 참여 중인 파티에 중복 참여 방지 (정책서에 명시되지 않았으나 PK 제약상 필요한 기술적 방어 로직)
    _ALREADY_PARTY_MEMBER(HttpStatus.CONFLICT, "ALREADY_PARTY_MEMBER", "이미 참여 중인 파티입니다."),

    // PAR-05: 파티 시작/준비완료
    _NOT_PARTY_HOST(HttpStatus.FORBIDDEN, "NOT_PARTY_HOST", "방장만 파티를 시작할 수 있습니다."),
    _PARTY_NOT_READY_TO_START(
            HttpStatus.BAD_REQUEST,
            "PARTY_NOT_READY_TO_START",
            "파티원이 2인 이상 모이고 전원 준비완료해야 시작할 수 있습니다."
    ),
    _HOST_CANNOT_READY(HttpStatus.BAD_REQUEST, "HOST_CANNOT_READY", "방장은 준비완료 대상이 아닙니다."),

    // 파티 정산 결과 조회 시, 정산이 아직 처리되지 않은 경우 (정산 계산 로직은 별도 도메인에서 처리 예정)
    _SETTLEMENT_NOT_COMPLETED(
            HttpStatus.BAD_REQUEST,
            "SETTLEMENT_NOT_COMPLETED",
            "아직 정산이 완료되지 않은 파티입니다."
    ),
    // 파티 정산 시 시작일=종료일 등으로 수행일이 0 이하가 되어 일 지분 계산이 불가능한 데이터 이상 상태
    _SETTLEMENT_INVALID_PERIOD(HttpStatus.INTERNAL_SERVER_ERROR, "SETTLEMENT_INVALID_PERIOD", "정산 대상 기간이 올바르지 않습니다."),
    // 개인 챌린지 몰수금 pot 싱글톤 행이 시드되지 않은 데이터 이상 상태
    _CHALLENGE_POT_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "CHALLENGE_POT_NOT_FOUND", "챌린지 몰수금 pot을 찾을 수 없습니다."),

    // 약관 관련 에러
    _TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM_NOT_FOUND", "약관을 찾을 수 없습니다."),
    _INVALID_TERM_TYPE(HttpStatus.BAD_REQUEST, "INVALID_TERM_TYPE", "조회 가능한 약관 종류가 아닙니다."),
    _TERM_CONTENT_PARSING_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TERM_CONTENT_PARSING_FAILED",
            "약관 content JSON 변환에 실패했습니다."
    ),

    // 포인트 관련 에러
    _RETURN_RATE_OUT_OF_RANGE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RETURN_RATE_OUT_OF_RANGE",
            "평균 환급률이 허용 범위를 초과했습니다."
    ),
    _INSUFFICIENT_POINT_FOR_WITHDRAW(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT_FOR_WITHDRAW", "보유 포인트가 출금 요청 금액보다 부족합니다."),

    // 커서 페이지네이션 관련 에러
    _CURSOR_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "CURSOR_INVALID_FORMAT", "cursor 형식이 올바르지 않습니다."),
    _CURSOR_SIZE_INVALID(HttpStatus.BAD_REQUEST, "CURSOR_SIZE_INVALID", "size는 1 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final boolean isSuccess = false;
    private final String code;
    private final String message;

    @Override
    public BaseCodeDto getCode() {
        return BaseCodeDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(isSuccess)
                .code(code)
                .message(message)
                .build();
    }
}
