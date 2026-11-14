// 웹앱(index.html)을 앱에 넣을 www 폴더로 복사한다. 웹앱 원본은 저장소 맨 위 index.html 하나다.
import { mkdirSync, copyFileSync } from 'node:fs';
mkdirSync('www', { recursive: true });
copyFileSync('index.html', 'www/index.html');
console.log('www/index.html 준비됨');
