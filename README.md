# Onuldo (오늘도) Backend

챌린지를 파티(그룹) 단위로 인증하고, 함께 습관을 만들어가는 서비스 오늘두(Onuldo)의 백엔드 서버입니다.

## 📌 프로젝트 소개

Onuldo는 사용자가 파티를 만들어 챌린지에 참여하고, 매일 인증(사진/포인트 등)을 통해 습관을 이어가도록 돕는 서비스입니다. 백엔드는 아래 기능을 제공합니다.

- **인증/인가**: 이메일 회원가입·로그인, 소셜(OAuth) 로그인, JWT 기반 Access/Refresh 토큰 발급 및 재발급
- **파티(Party)**: 파티 생성/참여/탈퇴, 파티 홈·피드 조회, 파티 결과·정산
- **챌린지(Challenge)**: 챌린지 목록/참여, 일일 인증 및 인증 이미지 검수(수동/자동), 완료 챌린지 기록 조회
- **사용자(User)**: 마이페이지, 프로필 수정, 포인트 충전/사용, 알림 설정, 약관 조회
- **파일(File)**: 이미지 업로드(S3) 및 이미지 라벨 인식(AWS Rekognition)을 통한 인증 사진 자동 검수

## 🛠 기술 스택

- **Language / Framework**: Java 21, Spring Boot 4.1 (Spring Web MVC, Spring Data JPA, Spring Security, Validation, Actuator)
- **DB**: MySQL
- **Auth**: JWT (jjwt), OAuth
- **Infra/외부 연동**: AWS S3, AWS Rekognition, Firebase Admin(FCM), Discord Webhook(알림)
- **API 문서화**: springdoc-openapi (Swagger UI)
- **기타**: Lombok, Bucket4j + Caffeine(Rate Limit)
- **Build**: Gradle
- **CI/CD**: GitHub Actions → EC2 배포

## 📂 폴더 구조

```
src/main/java/com/example/onuldo
├── domain
│   ├── auth        # 회원가입/로그인/OAuth/JWT 재발급
│   ├── party        # 파티 생성/참여/홈/피드/정산
│   ├── challenge     # 챌린지 조회/참여/인증
│   ├── user        # 마이페이지/프로필/포인트/알림/약관
│   └── file        # S3 업로드, Rekognition 이미지 검수
└── global
    ├── config       # Security, Swagger 등 전역 설정
    ├── security      # JWT 인증 필터, 인증 사용자 정보
    ├── aws        # S3, Rekognition 연동
    ├── ratelimit      # 요청 Rate Limit
    └── common       # 공통 응답, 예외, 커서 페이징 등
```

각 도메인은 `controller / dto / entity / repository / service` 계층으로 구성되어 있습니다.

## 🚀 시작하기

### 1. 요구 사항

- JDK 21
- MySQL (로컬 또는 원격 인스턴스)

### 2. 저장소 클론

```bash
git clone https://github.com/OnulDo/Onuldo_BE.git
cd Onuldo_BE
```

### 3. 환경 변수 설정

`.env.example`을 참고하여 `.env` 파일을 만들고 값을 채워주세요. (Spring Boot가 직접 `.env`를 읽지 않는 프로젝트라면, 아래 값들을 실행 환경변수로 주입하거나 IDE Run Configuration에 등록해주세요.)

```bash
cp .env.example .env
```

주요 환경 변수:

| 변수 | 설명 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 실행 프로필 (`dev` / `prod`) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL 접속 정보 |
| `JWT_SECRET_KEY`, `JWT_TOKEN_REFRESH_KEY` | JWT 서명 키 (Access/Refresh) |
| `JWT_TOKEN_EXPIRATION_ACCESS`, `JWT_TOKEN_EXPIRATION_REFRESH` | 토큰 만료 시간(ms) |
| `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | AWS 자격 증명 |
| `AWS_S3_BUCKET`, `AWS_S3_PUBLIC_BASE_URL`, `AWS_S3_UPLOAD_PREFIX` | S3 업로드 설정 |
| `AWS_REKOGNITION_MAX_LABELS`, `AWS_REKOGNITION_MIN_CONFIDENCE` | Rekognition 라벨 인식 옵션 |
| `DISCORD_WEBHOOK_URL` | 서버 알림용 Discord Webhook |

> Firebase(FCM) 연동을 위해 `src/main/resources/firebase-service-key.json` 서비스 계정 키 파일이 필요합니다. (레포에는 포함되어 있지 않으므로 별도로 전달받아 추가하세요.)

### 4. 데이터베이스 준비

MySQL에 애플리케이션이 사용할 DB와 계정을 생성합니다. (`.env`의 `DB_URL`에 지정한 스키마명과 일치해야 합니다.)

**로컬 개발 환경 예시** (아래 명령은 로컬 MySQL 전용입니다. `<password>`는 직접 정한 값으로 바꿔주세요.)

```sql
CREATE DATABASE oneuldo CHARACTER SET utf8mb4;
CREATE USER 'oneuldo-user'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON oneuldo.* TO 'oneuldo-user'@'localhost';
```

**원격/운영 환경**에서는 위 명령을 그대로 사용하지 마세요. 아래 원칙을 따라주세요.

- 호스트를 `%`(모든 호스트 허용)로 두지 말고, 애플리케이션 서버의 고정 IP 또는 내부망 CIDR로 제한하세요. (예: `'oneuldo-user'@'10.0.0.0/24'`)
- `GRANT ALL PRIVILEGES` 대신 애플리케이션에 실제로 필요한 최소 권한만 부여하세요. (예: `GRANT SELECT, INSERT, UPDATE, DELETE ON oneuldo.* TO ...`)
- 비밀번호는 예제 값을 그대로 쓰지 말고 별도로 생성한 강력한 값을 비밀 관리 도구(예: AWS Secrets Manager, GitHub Actions Secrets)로 관리하세요.

### 5. 애플리케이션 실행

```bash
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

기본적으로 `http://localhost:8080`에서 서버가 실행됩니다.

## 📑 API 문서 (Swagger)

아래 주소로 접속하면 API 명세를 확인하고 직접 호출해볼 수 있습니다.

- **배포 서버**: https://onuldo.site/swagger-ui/index.html
- **로컬 실행 시**: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 인증(JWT)이 필요한 API 테스트 방법

1. `POST /api/auth/login`(이메일 로그인) 또는 `/api/auth/oauth/login` API를 호출해 `accessToken`을 발급받습니다.
2. Swagger UI 우측 상단(또는 화면 상단)의 **Authorize 🔓** 버튼을 클릭합니다.
3. `bearerAuth` 항목에 `accessToken` 값만 입력합니다. (`Bearer ` 접두사는 자동으로 붙으므로 토큰 값만 넣으면 됩니다.)
4. **Authorize** → **Close**를 누르면 이후 요청에 `Authorization: Bearer {accessToken}` 헤더가 자동으로 포함됩니다.
5. Access Token이 만료되면 `POST /api/auth/refresh`에 Refresh Token을 보내 재발급받은 뒤 다시 Authorize 해주세요.

## ✅ 테스트

```bash
# Windows
gradlew.bat test

# macOS / Linux
./gradlew test
```

## 🔄 CI/CD

`release` 브랜치에 push되면 GitHub Actions(`.github/workflows/deploy.yml`)가 Spring Boot JAR를 빌드해 EC2 서버로 배포합니다.
