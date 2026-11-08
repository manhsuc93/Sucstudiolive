import { cp, mkdir, access, rm } from 'node:fs/promises';
import { constants } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourceHtml = path.join(root, 'Studio_Livestream.html');
const webDir = path.join(root, 'www');

try {
  await access(sourceHtml, constants.R_OK);
} catch {
  console.error('ERROR: Không tìm thấy Studio_Livestream.html ở thư mục gốc.');
  process.exit(1);
}

await rm(webDir, { recursive: true, force: true });
await mkdir(webDir, { recursive: true });
await cp(sourceHtml, path.join(webDir, 'index.html'));

for (const name of ['manifest.json', 'sw.js', 'icons', 'screenshots', 'src']) {
  const from = path.join(root, name);
  try {
    await access(from, constants.R_OK);
    await cp(from, path.join(webDir, name), { recursive: true, force: true });
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    if (['manifest.json', 'sw.js'].includes(name)) {
      console.warn(`WARNING: Không có ${name}; bỏ qua.`);
    }
  }
}

console.log('Web assets prepared successfully: www/index.html');
