
import { cp, mkdir, access, constants, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
);

const source = path.join(root, 'Studio_Livestream.html');
const web = path.join(root, 'www');

await access(source, constants.R_OK);

await rm(web, { recursive: true, force: true });
await mkdir(web, { recursive: true });

// Giao diện chính chạy trực tiếp tại www/index.html
await cp(source, path.join(web, 'index.html'));

// Sao chép tài nguyên cần thiết
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
    await cp(from, path.join(web, name), {
      recursive: true,
      force: true
    });
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
}

console.log('Live Studio is ready at www/index.html');
