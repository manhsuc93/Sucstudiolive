
import { cp, mkdir, access, constants, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
);

const sourceHtml = path.join(root, 'Studio_Livestream.html');
const webDir = path.join(root, 'www');

try {
  await access(sourceHtml, constants.R_OK);
} catch {
  console.error('ERROR: Không tìm thấy Studio_Livestream.html.');
  process.exit(1);
}

await rm(webDir, { recursive: true, force: true });
await mkdir(webDir, { recursive: true });

// Tạo trang chính và giữ đường dẫn giao diện gốc.
await cp(sourceHtml, path.join(webDir, 'index.html'));
await cp(sourceHtml, path.join(webDir, 'Studio_Livestream.html'));

// Sao chép tài nguyên ứng dụng.
for (const name of [
  'manifest.json',
  'sw.js',
  'icons',
  'screenshots',
  'src'
]) {
  const from = path.join(root, name);

  try {
    await access(from, constants.R_OK);
    await cp(from, path.join(webDir, name), {
      recursive: true,
      force: true
    });
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    console.warn(`WARNING: Không tìm thấy ${name}; bỏ qua.`);
  }
}

console.log('SUCCESS: Đã chuẩn bị www/index.html');
console.log('SUCCESS: Đã chuẩn bị www/Studio_Livestream.html');
