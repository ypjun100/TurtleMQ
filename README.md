# TurtleMQ &middot; [![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/facebook/react/blob/main/LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://reactjs.org/docs/how-to-contribute.html#your-first-pull-request)
![mechanism](https://github.com/ypjun100/TurtleMQ/assets/35624367/14194360-d37e-4396-b901-281d3f04e5bf)

TurtleMQ는 Spring Boot를 기반하여 제작된 메시지 브로커입니다.
> 단일 컴퓨팅 환경에서 병목현상이 발생하는 것을 방지하기 위해 작업을 제공하는 `Client`와 작업을 처리하는 `Worker`로 분리한 뒤, 이들과 웹소켓 방식으로 연결되어 병렬 컴퓨팅 환경을 구성합니다.

계층 간 신뢰성 있는 통신을 제공하기 위해 다음과 같은 기능들을 제공합니다.

* **Client와 Worker 관리** : TurtleMQ에서는 Client와 Worker들의 상태를 전반적으로 관리합니다.
* **유휴 상태인 Worker에게 작업 요청** : 작업 요청이 들어오면 해당 작업을 현재 유휴 상태인 Worker에게 전달합니다.
* **자동 작업 이관** : 만약 현재 작업 중인 Worker의 연결이 끊긴 경우, 해당 Worker의 작업을 다른 Worker에게 이관합니다.
* **웹소켓을 기반한 통신** : 통신 프로토콜로 웹소켓을 사용하기 때문에 어느 환경이든 TurtleMQ에 쉽게 접근할 수 있습니다.


## 통신 절차
[![TurtleMQ 시연 및 설명 영상](https://img.youtube.com/vi/V-reI9cmSIw/0.jpg)](https://youtu.be/V-reI9cmSIw?si=rh4dVn0c-G0CKWTi)
> _상단의 이미지를 클릭하면 시연 영상을 시청하실 수 있습니다._

TurtleMQ는 Client와 Worker를 구분하여 웹소켓 통신을 진행합니다. 그리고 계층 간에 작업을 주고받기 위해서는 사전 등록 절차가 필요합니다.

### Client, Worker 등록
```json
// Client의 경우
{
  "type" : "REGISTER_CLIENT",
  "data" : ""
}

// Worker의 경우
{
  "type" : "REGISTER_WORKER",
  "data" : ""
}
```
* Client는 `REGISTER_CLIENT` 메시지, Worker는 `REGISTER_WORKER` 메시지를 전송합니다.
* 정상적으로 등록이 완료되면, TurtleMQ는 요청 메시지를 그대로 반환합니다.

### 작업 요청 (Client)
```json
{
  "type"      : "REQUEST_TASK",
  "messageId" : "해당 작업을 식별하기 위한 ID",
  "data"      : "처리할 데이터 문자열"
}
```
* Client에서는 `REQUEST_TASK` 메시지와 함께 처리할 데이터를 보내 작업을 요청할 수 있습니다.

### 작업 요청 (Worker)
```json
{
  "type"   : "REQUEST_TASK",
  "taskId" : "클라이언트에서 전송한 messageId",
  "data"   : "클라이언트에서 전송한 데이터 문자열"
}
```
* 작업 요청이 들어오면 Worker는 `REQUEST_TASK` 메시지를 받게 됩니다.

### 작업 결과 반환 (Worker)
```json
{
  "type"   : "RESPONSE_TASK",
  "taskId" : "클라이언트에서 전송한 messageId",
  "data"   : "계산한 결과 문자열"
}
```
* 작업이 완료되면 Worker는 `RESPONSE_TASK` 메시지를 TurtleMQ로 전송합니다.

### 작업 결과 반환 (Client)
```json
{
  "type"   : "RESPONSE_TASK",
  "messageId" : "클라이언트에서 전송한 messageId",
  "data"   : "계산한 결과 문자열"
}
```
* 최종적으로 Client는 `RESPONSE_TASK` 메시지와 함께 결과값을 받을 수 있습니다. 
