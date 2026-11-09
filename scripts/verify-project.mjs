import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const required = [
  'Studio_Livestream.html',
  'www/index.html',
  'www/native-bridge.js',
  'native/rtmp-publisher/src/index.ts',
  'native/rtmp-publisher/android/src/main/java/vn/livestudio/rtmp/RtmpPublisherPlugin.kt',
  'native/rtmp-publisher/ios/RtmpPublisherPlugin.swift',
  'native/rtmp-publisher/RtmpPublisher.podspec'
];
for (const file of required) {
  await access(path.join(root, file));
}
const html = await readFile(path.join(root, 'Studio_Livestream.html'), 'utf8');
const index = await readFile(path.join(root, 'www/index.html'), 'utf8');
if (!html.includes('buildNativeOverlaySnapshot') || !html.includes('RtmpPublisher')) {
  throw new Error('RTMP UI/plugin bridge markers are missing.');
}
if (!index.includes('native-bridge.js')) {
  throw new Error('Native JS bridge is not loaded by www/index.html.');
}
if (/\bbgm\b|screen.?share|chia sẻ màn hình|RTCPeerConnection/i.test(html)) {
  throw new Error('A removed feature (BGM or screen sharing) is still present.');
}
console.log('SUCCESS: source tree, native plugin and RTMP bridge verified.');
