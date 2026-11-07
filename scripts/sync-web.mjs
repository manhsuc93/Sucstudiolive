import { cp, mkdir } from 'node:fs/promises';
const root = new URL('.', import.meta.url);
const src = new URL('../', root);
const www = new URL('../www/', root);
await mkdir(www, { recursive: true });
for (const name of ['Studio_Livestream.html', 'manifest.json', 'sw.js', 'icons', 'screenshots', 'src']) {
  try { await cp(new URL(name, src), new URL(name === 'Studio_Livestream.html' ? 'index.html' : name, www), { recursive: true, force: true }); }
  catch (e) { if (e.code !== 'ENOENT') throw e; }
}
