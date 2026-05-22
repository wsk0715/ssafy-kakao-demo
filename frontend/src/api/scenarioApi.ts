// 5-layer architecture: API Layer for Scenario Generation
// Returns mock scenarios for phishing training simulation

export interface Scenario {
  id: string
  type: 'VOICE' | 'SMS' | 'EMAIL'
  title: string
  sender: string
  content: string
  attackerAction: string // What the attacker is asking the user to do (e.g., "송금", "앱 설치")
  steps: {
    dialogue: string
    isAttacker: boolean
    options?: string[]
  }[]
  warningExplanation: string // Explanation shown after simulation completes
}

const mockScenarios: Scenario[] = [
  {
    id: 'voice_prosecutor',
    type: 'VOICE',
    title: '서울중앙지검\n김민수 검사 사칭',
    sender: '02-1301-XXXX\n(서울중앙지검)',
    content: '귀하의 명의로 대포통장이 개설되어 범죄 자금 세탁에 이용되었습니다. 자산 보호를 위해 조치를 취해야 합니다.',
    attackerAction: '금감원 안전 계좌로 이체 유도',
    steps: [
      {
        dialogue: '여보세요? 본인 김철수 씨 맞으십니까? 서울중앙지검 첨단범죄수사부 김민수 검사입니다.',
        isAttacker: true,
        options: ['네, 맞는데요. 무슨 일이시죠?', '검사 사칭 아닌가요? 끊겠습니다.']
      },
      {
        dialogue: '귀하의 명의로 신한은행 대포통장이 개설되어 4,200만 원 규모의 금융 사기 사건에 연루되었습니다. 본인이 직접 개설한 게 맞습니까?',
        isAttacker: true,
        options: ['아니요, 저는 모르는 일입니다!', '대포통장이라니요? 제가 왜요?']
      },
      {
        dialogue: '직접 가담하지 않으셨다면 금융감독원 자산보호 전용 임시 계좌로 예금 전액을 이체하셔서 출처 조사를 받으셔야 혐의를 벗을 수 있습니다. 협조 안 하시면 구속영장이 청구됩니다.',
        isAttacker: true,
        options: ['알겠습니다. 지금 당장 알려주시는 계좌로 이체할게요.', '검찰청인데 왜 바로 이체를 시키죠? 의심스럽습니다.']
      }
    ],
    warningExplanation: '검찰이나 금감원은 어떠한 경우에도 전화상으로 자산 보호나 혐의 입증을 이유로 임시 계좌로의 예금 송금을 요구하지 않습니다. 이 단계에서 돈을 송금하면 100% 보이스피싱 사기입니다.'
  },
  {
    id: 'sms_delivery',
    type: 'SMS',
    title: '[대한택배] 배송 주소지 오류 수정',
    sender: '010-9876-5432',
    content: '[대한택배] 고객님, 택배 배송 주소지가 잘못되어 배송이 일시 보류되었습니다. 아래 링크에서 주소를 확인 및 수정해주세요: http://koreapost-info.xyz',
    attackerAction: '피싱 사이트 접속 후 휴대폰 번호 인증',
    steps: [
      {
        dialogue: '[대한택배] 주소 오류로 반송 예정. 주소 확인: http://koreapost-info.xyz',
        isAttacker: true,
        options: ['링크 클릭하여 주소 확인해보기', '무시하고 차단하기']
      }
    ],
    warningExplanation: '택배사 및 우체국은 문자 메시지에 절대 불분명한 외부 링크(URL) 주소를 전송하지 않으며, 특히 주소 수정을 위해 앱 설치나 전화번호 인증을 강요하지 않습니다. 링크를 누르는 즉시 악성 앱이 다운로드되거나 개인정보가 탈취됩니다.'
  },
  {
    id: 'email_security',
    type: 'EMAIL',
    title: '[보안안내] 비정상적인 로그인 시도 감지',
    sender: 'security@kakoa-login.com',
    content: '고객님의 계정으로 새로운 기기(Windows, 러시아)에서 로그인이 차단되었습니다. 본인의 시도가 아니라면 계정 잠금 해제 및 비밀번호 변경을 진행해주세요: http://kakoa-login.com/security/reset',
    attackerAction: '가짜 로그인 화면에 아이디/비밀번호 입력',
    steps: [
      {
        dialogue: '비정상 로그인 알림 메일: 비밀번호 재설정 http://kakoa-login.com/security/reset',
        isAttacker: true,
        options: ['비밀번호 재설정 링크 클릭하기', '메일 발송 도메인을 먼저 의심하고 스팸 신고하기']
      }
    ],
    warningExplanation: '정상적인 카카오 메일 도메인은 `kakao.com` 또는 `kakaocorp.com` 입니다. 발송자의 이메일 철자(`kakoa-login.com`)가 교묘하게 틀려 있는 전형적인 이메일 피싱입니다. 로그인 페이지에서 비정상 입력을 하면 그대로 사칭 사이트로 정보가 수집되어 전송됩니다.'
  }
]

export const scenarioApi = {
  async getScenarios(): Promise<Scenario[]> {
    return new Promise((resolve) => {
      setTimeout(() => resolve(mockScenarios), 100)
    })
  }
}
