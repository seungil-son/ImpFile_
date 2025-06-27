중요 파일 관리 프로그램 
---------------------------------------------

- 프로그램 개발기간
   2일

- 개발도구 
   1. JAVA
   2. SQLite
   3. Copilot

- 개발 환경
   1. IntelliJ

- 프로그램 설명

   1. 개별 파일 또는 폴더를 Drag & Drop으로 가져와 읽는다.
   
   2. 파일의 이름, 수정시간, 용량을 점수로 환산해
      중요도를 측정하고   
      중요도를 기준으로 가장 중요한 파일을 알려준다.

   3. 프로그램에서 파일을 열 수 있다.

   4. 폴더의 최신화가 이루어 졌을때 프로그램 UI에서도 최신화한다.

   5. 프로그램이 읽어온 파일 또는 폴더를 X버튼을 통해 프로그램에서 더이상 읽지 않게 할 수 있다.

   6. 폴더 또는 파일을 읽었을때 가장 중요도가 높은 파일을 backup폴더에 저장한다.

   7. 폴더 또는 파일을 삭제했을떄 backup폴더에서 파일을 읽어와 복구할 수 있다.

   8. 사용자가 임의로 백업폴더의 내용을 삭제할 수 있다.

- 발전 가능성

  1. 지금은 백업을 프로젝트 폴더 안에 backup폴더를 만들어서 이용

  2. 향후 DB에 파일을 올리고 그 파일을 다운받는 방식으로 변경

  3. 중개 서버를 만들어 중개 서버에 저장하고 다운받는 방식으로 변경

  4. 중개 서버의 접속방식 :  한 계정의 개인 DB(개인목적이용),
                           여러 계정의 공유 DB(단체,회사 내부 이용)
- 실행 이미지
  
  1. 초기 실행 이미지
https://private-user-images.githubusercontent.com/79846539/459822121-c6f7b1b3-f688-4aee-b3c0-72ca095fa1d9.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NTEwMTAyOTUsIm5iZiI6MTc1MTAwOTk5NSwicGF0aCI6Ii83OTg0NjUzOS80NTk4MjIxMjEtYzZmN2IxYjMtZjY4OC00YWVlLWIzYzAtNzJjYTA5NWZhMWQ5LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTA2MjclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUwNjI3VDA3Mzk1NVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWM5OWU0MTk5YzI0NGE3NzNiNjNlODBkNDgxOWJiMmUzNDM1MTJhZGM5NjM0NmIyYWE3N2I4ZTY2OTZjNWQ1NzcmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.JmAbwVRP_7yQvMxbIvru8o_GjWU-kwqxIoDYgdQRNh8
