# Spring Microservices EKS Template

Spring Cloud 마이크로서비스를 AWS EKS에 배포하고 관리할 수 있도록 구성된 템플릿입니다.
GitHub Actions 기반의 CI/CD 파이프라인과 Cluster Autoscaler를 지원합니다.

## 🏗️ 아키텍처 개요

### 전체 시스템 아키텍처

<img width="6696" height="5542" alt="Image" src="https://github.com/user-attachments/assets/acb2a317-0231-4835-aad9-15ce983ed031" />

### 서비스 디스커버리 구성

- 로컬: Eureka 서버를 통한 마이크로서비스 디스커버리 및 로드 밸런싱
- EKS: Kubernetes Service 리소스를 통한 네이티브 서비스 라우팅

## 🚀 배포 가이드

### 1. 도메인 설정 및 AWS 기본 환경 구성

#### 1.1 가비아 도메인 구매 및 Route 53 설정

**AWS Route 53 연동**

1. AWS 콘솔에서 Route 53 서비스 접속
2. 호스팅 영역 생성:
   - 도메인 이름: 구매한 도메인 입력
   - 호스팅 영역 생성 클릭
3. NS 레코드 확인 및 가비아에 등록:
   - Route 53에서 생성된 4개의 NS 주소 확인
   - 가비아 My가비아 → 서비스관리 → 도메인 관리 → 네임서버 설정
   - 4개의 NS 주소를 각각 등록

#### 1.2 IAM 사용자 생성 및 권한 설정

**IAM 사용자 생성**

1. AWS 콘솔에서 IAM 서비스 접속
2. 사용자 추가:
   - 사용자 이름 지정
   - AWS Management Console 액세스 선택
   - 액세스 키 - 프로그래밍 방식 액세스 체크
3. 권한 설정:
   - 기존 정책 직접 연결 선택
   - `AdministratorAccess` 정책 연결
4. Access Key ID와 Secret Access Key 안전하게 보관

### 2. AWS EKS 클러스터 구성

#### 2.1 EKS 클러스터 생성

1. 새로 생성한 IAM 계정으로 AWS 콘솔 로그인
2. EKS 서비스에서 클러스터 생성:
   - 클러스터 이름 지정
   - Kubernetes 버전 선택
   - 나머지 설정은 기본값 사용
3. 노드 그룹 생성:
   - 컴퓨팅 탭에서 노드 그룹 생성
   - 인스턴스 유형, 최소/최대 노드 수 설정

#### 2.2 로컬 환경 설정

**AWS CLI 설치 및 구성**

```bash
# AWS CLI 설치
# https://docs.aws.amazon.com/ko_kr/cli/latest/userguide/getting-started-install.html#getting-started-install-instructions

# AWS CLI 구성
aws configure
# Access Key ID 입력
# Secret Access Key 입력
# Default region: ap-northeast-2
# Default output format: json
```

**kubectl 설치**

```bash
# Windows
winget install -e --id Kubernetes.kubectl

# 설치 확인
kubectl version --client
```

**kubeconfig 설정**

```bash
# EKS 클러스터와 연결
aws eks update-kubeconfig --region ap-northeast-2 --name <your-cluster-name>

# 설정 확인
kubectl config current-context
kubectl get nodes
```

### 3. 인프라 리소스 생성

#### 3.1 RDS MySQL 데이터베이스 생성

1. AWS RDS 서비스에서 MySQL 인스턴스 생성
2. 보안 그룹 설정:
   - 인바운드 규칙에 포트 3306 추가
   - 소스: EKS 클러스터 보안 그룹
3. 퍼블릭 액세스 허용 설정
4. 데이터베이스 스키마 생성:
   ```sql
   CREATE DATABASE ordermsa;
   ```

#### 3.2 ECR 레포지토리 생성

각 마이크로서비스용 ECR 레포지토리 생성:

```bash
aws ecr create-repository --repository-name apigateway --region ap-northeast-2
aws ecr create-repository --repository-name member-service --region ap-northeast-2
aws ecr create-repository --repository-name product-service --region ap-northeast-2
aws ecr create-repository --repository-name ordering-service --region ap-northeast-2
```

### 4. Kubernetes 리소스 배포

#### 4.1 네임스페이스 생성

```bash
kubectl create namespace <your-namespace>
```

#### 4.2 Secret 생성

```bash
# Secret 생성
kubectl create secret generic app-secrets \
  --from-literal=DB_HOST=<your-rds-endpoint> \
  --from-literal=DB_PW=<your-db-password> \
  --from-literal=JWT_SECRET=<your-jwt-secret> \
  --from-literal=JWT_SECRET_RT=<your-jwt-refresh-secret>
  -n <your-namespace>

# Secret 목록 조회
kubectl get secrets -n <your-namespace>

# Secret 상세 조회 (인코딩된 상태)
kubectl get secret app-secrets -o yaml -n <your-namespace>

# Secret 삭제
kubectl delete secret app-secrets -n <your-namespace>
```

#### 4.3 미들웨어 및 Ingress Controller 배포

```bash
kubectl apply -f k8s/redis.yml -n <your-namespace>
kubectl apply -f k8s/kafka.yml -n <your-namespace>
kubectl apply -f k8s/zookeeper.yml -n <your-namespace>
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml
kubectl apply -f k8s/ingress.yml -n <your-namespace>
```

#### 4.4 마이크로서비스 배포

```bash
kubectl apply -f member/k8s/depl_svc.yml -n <your-namespace>
kubectl apply -f product/k8s/depl_svc.yml -n <your-namespace>
kubectl apply -f ordering/k8s/depl_svc.yml -n <your-namespace>
kubectl apply -f apigateway/k8s/depl_svc.yml -n <your-namespace>
```

#### 4.5 배포 상태 확인

```bash
# Pod 상태 확인
kubectl get pods -n <your-namespace>

# 서비스 상태 확인
kubectl get services -n <your-namespace>

# Ingress 상태 확인
kubectl get ingress -n <your-namespace>
```

### 5. HTTPS 인증서 설정

#### 5.1 HTTPS 통신을 위한 SSL/TLS 인증서 발급

**인증서 발급 과정**

1. **공개키/비밀키 생성**: 서버에서 암호화 키 쌍 생성
2. **CSR(인증서 서명 요청서) 생성**: 공개키와 서버 정보를 조합하여 생성
3. **CA(인증기관) 검증**: 도메인 소유 여부 확인 후 디지털 서명
4. **인증서 발급**: 검증 완료 후 SSL/TLS 인증서 발급

#### 5.2 cert-manager 설치 및 구성

**cert-manager 개요**

cert-manager는 Kubernetes에서 TLS 인증서를 자동으로 발급, 갱신 및 관리하는 컨트롤러입니다.

**설치 과정**

```bash
# 1. cert-manager 네임스페이스 생성
kubectl create namespace cert-manager

# 2. CRDs 설치 (Issuer, ClusterIssuer)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.5.0/cert-manager.crds.yaml

# 3. cert-manager 컨트롤러 설치
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.5.0/cert-manager.yaml
```

#### 5.3 ClusterIssuer 및 Certificate 생성

**HTTPS 인증서 리소스 배포**

```bash
kubectl apply -f k8s/https.yaml
```

**동작 원리**

1. **ClusterIssuer**: 인증 기관(CA) 및 인증서 발급 방식 정의
2. **Certificate**: 공개키와 도메인 정보로 CSR 생성
3. **Secret 저장**: 발급받은 인증서와 비밀키를 Secret에 저장
4. **Ingress 연동**: Ingress가 해당 Secret을 참조하여 HTTPS(443) 자동 활성화

**인증서 발급 상태 확인**

```bash
# Certificate 상태 확인
kubectl describe certificate <certificate-name> -n <your-namespace>

# Certificate status가 True로 변경되면 발급 완료
kubectl get certificate -n <your-namespace>
```

### 6. 오토스케일링 설정

#### 6.1 Cluster Autoscaler 설정

**Auto Scaling Group 태그 설정**

EKS 노드 그룹의 Auto Scaling Group에 다음 태그를 추가합니다:

- `k8s.io/cluster-autoscaler/enabled`: `true`
  - 해당 ASG가 클러스터 오토스케일러의 관리 대상임을 표시
- `k8s.io/cluster-autoscaler/<cluster-name>`: `owned`
  - 특정 클러스터에 속해 있고 해당 클러스터의 오토스케일러가 관리함을 표시

**OIDC 제공자 설정**

1. EKS 클러스터에서 OIDC 제공자 URL 확인
2. IAM에서 ID 제공업체 추가:
   - 유형: OpenID Connect
   - 공급자 URL: EKS 클러스터의 OIDC 제공자 URL 입력
   - 오디언스: `sts.amazonaws.com`
     - AWS의 보안 토큰 서비스(STS)
     - AssumeRole, AssumeRoleWithWebIdentity 같은 임시 보안 자격 증명 발급을 담당
     - cluster-autoscaler Pod가 STS에 Role 사용 요청시 필요

**ID 제공업체 추가 목적**

- AWS가 신뢰할 수 있는 주체가 누구인지를 식별하는 요소
- EKS 내의 Pod들이 IAM Role을 사용할 수 있도록 하는 과정
- 제공업체로 추가함으로써 신뢰할 수 있는 주체에게 Role 사용 권한 부여

**IAM 역할 생성**

신뢰할 엔터티 직접 설정으로 다음 신뢰 정책 추가:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<YOUR_AWS_ACCOUNT_ID>:oidc-provider/<YOUR_EKS_OIDC_PROVIDER>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "<YOUR_EKS_OIDC_PROVIDER>:sub": "system:serviceaccount:kube-system:cluster-autoscaler"
        }
      }
    }
  ]
}
```

필요한 정책 연결:

- `AmazonEKSClusterPolicy`
- `AutoScalingFullAccess`
- `AmazonEC2FullAccess`

**Cluster Autoscaler 배포**

```bash
kubectl apply -f k8s/autoscaler.yml
```

**오토스케일링 동작 원리**

- cluster-autoscaler는 Pod 스케줄링 불가 상황 감지시 Auto Scaling Group에 노드 수 증가 요청
- 부하 감소시 약 10분 후 불필요한 노드 제거
- EC2 인스턴스가 자동으로 증가/감소하여 비용 최적화

### 7. GitHub Actions CI/CD 파이프라인 설정

#### 7.1 GitHub 레포지토리 설정

**새로운 레포지토리 생성**

1. GitHub에서 새로운 레포지토리 생성
2. 기존 `.git` 폴더 삭제 후 새로운 레포지토리로 연결:

```bash
# 기존 .git 폴더 삭제
rm -rf .git

# 새로운 Git 저장소 초기화
git init

# 새로운 원격 저장소 추가
git remote add origin <your-new-repo-url>

# main 브랜치로 체크아웃
git checkout -b main
```

#### 7.2 GitHub Secrets 설정

**필수 Secrets 추가**

Repository Settings → Secrets and variables → Actions에서 다음 secrets 추가:

```
AWS_KEY: <your-aws-access-key>
AWS_SECRET: <your-aws-secret-key>
```

#### 7.3 CI/CD 파이프라인 실행

**코드 업로드 및 파이프라인 실행**

```bash
git push origin main
```

**파이프라인 동작 과정**

1. 코드 변경 시 자동으로 GitHub Actions 워크플로우 실행
2. 각 마이크로서비스의 Docker 이미지 빌드
3. ECR에 이미지 푸시
4. EKS 클러스터에 자동 배포
5. 배포 상태 확인 및 알림

**배포 확인**

```bash
# 배포 상태 확인
kubectl get pods -n <your-namespace>
kubectl get services -n <your-namespace>

# 로그 확인
kubectl logs -f deployment/<service-name> -n <your-namespace>

# API 테스트 (your-domain.com을 실제 도메인으로 변경)
curl -X POST https://your-domain.com/member-service/member/doLogin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@naver.com",
    "password": "12341234"
  }'
```

## 📋 배포 전 체크 리스트

배포 전 다음 사항들을 확인하세요:

- [ ] 가비아 도메인 구매 및 Route 53 NS 레코드 등록
- [ ] IAM 사용자 생성 및 AdministratorAccess 권한 부여
- [ ] EKS 클러스터 및 노드 그룹 생성
- [ ] RDS MySQL 인스턴스 생성 및 보안 그룹 설정
- [ ] ECR 레포지토리 생성 (4개 서비스)
- [ ] kubectl 및 AWS CLI 설치
- [ ] kubeconfig 설정
- [ ] 모든 YAML 파일의 `CHANGE_ME` 부분 수정
- [ ] GitHub Secrets 설정 (AWS_KEY, AWS_SECRET)
- [ ] Auto Scaling Group 태그 설정
- [ ] OIDC 제공자 및 IAM 역할 생성

## 🔧 트러블슈팅

### 인증서 발급 실패

```bash
kubectl describe certificate <certificate-name> -n <your-namespace>
```

### Pod 시작 실패

```bash
kubectl logs <pod-name> -n <your-namespace>
kubectl describe pod <pod-name> -n <your-namespace>
```

### 서비스 접근 불가

```bash
kubectl get ingress -n <your-namespace>
kubectl describe ingress <ingress-name> -n <your-namespace>
```
