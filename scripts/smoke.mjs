#!/usr/bin/env node
/**
 * 接口冒烟测试：验证后端 × 数据库整条链路
 *   用法：node scripts/smoke.mjs [baseUrl]
 *   默认 baseUrl = http://localhost:8080
 * 只读为主，写接口会自行还原（点赞/收藏成对调用，不留下脏数据）
 */

const BASE = process.argv[2] || 'http://localhost:8080';
const USER = 2; // 测试用登录用户（九号线）

const results = [];
const pad = (s, n) => (s + ' '.repeat(n)).slice(0, n);

async function api(method, path, { body, user, expect = 200 } = {}) {
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (user) headers['X-User-Id'] = String(user);
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非 JSON */ }
  return { status: res.status, json, text, expect };
}

async function check(name, fn) {
  try {
    const detail = await fn();
    results.push({ name, ok: true, detail });
  } catch (e) {
    results.push({ name, ok: false, detail: e.message });
  }
}

const must = (cond, msg) => { if (!cond) throw new Error(msg); };

await check('GET  /api/meta', async () => {
  const { status, json } = await api('GET', '/api/meta');
  must(status === 200 && json?.code === 0, `HTTP ${status}`);
  const d = json.data;
  return `流派${d.genres.length} 频道${d.channels.length} 分类${d.categories.length} 标签${d.tags.length}`;
});

await check('GET  /api/posts', async () => {
  const { status, json } = await api('GET', '/api/posts?page=1&size=5');
  must(status === 200 && json?.code === 0, `HTTP ${status}`);
  const d = json.data;
  must(Array.isArray(d.list) && d.list.length > 0, '列表为空');
  must(d.list[0].author?.name, '缺少内嵌 author');
  return `${d.list.length} 条 / 共 ${d.total}，首条「${d.list[0].author.name}」`;
});

await check('GET  /api/posts/{id}', async () => {
  const { json } = await api('GET', '/api/posts/1');
  must(json?.code === 0, '返回失败');
  const d = json.data;
  return `#${d.id} 回复 ${d.replies.length} 条，附件 ${d.attachment ? d.attachment.kind : '无'}`;
});

await check('GET  /api/posts?channel=', async () => {
  const { json } = await api('GET', '/api/posts?channel=' + encodeURIComponent('闲聊'));
  must(json?.code === 0, '返回失败');
  must(json.data.list.every((x) => x.channel === '闲聊'), '频道过滤不生效');
  return `${json.data.list.length} 条全部属于「闲聊」`;
});

await check('POST /api/posts/{id}/like 开关', async () => {
  const on = await api('POST', '/api/posts/1/like', { user: USER });
  const off = await api('POST', '/api/posts/1/like', { user: USER });
  must(on.json?.data?.liked === true, '第一次未点赞');
  must(off.json?.data?.liked === false, '第二次未取消');
  must(on.json.data.likes === off.json.data.likes + 1, '计数未正确回滚');
  return `赞 → ${on.json.data.likes} → 取消 ${off.json.data.likes}（已还原）`;
});

await check('POST /api/posts/{id}/save 开关', async () => {
  const on = await api('POST', '/api/posts/1/save', { user: USER });
  const off = await api('POST', '/api/posts/1/save', { user: USER });
  must(on.json?.data?.saved === true && off.json?.data?.saved === false, '收藏开关异常');
  return '收藏/取消均正常（已还原）';
});

await check('POST /api/posts 校验拦截', async () => {
  // 约定：业务失败也走 HTTP 200，靠响应体 code 区分（见 README「统一响应体」）
  const { json } = await api('POST', '/api/posts', { user: USER, body: { body: '' } });
  must(json?.code === 400, `期望 code=400，实际 ${json?.code}`);
  return `空正文被拦截：${json.message}`;
});

await check('GET  /api/tracks?sort=plays', async () => {
  const { json } = await api('GET', '/api/tracks?sort=plays');
  must(json?.code === 0, '返回失败');
  const plays = json.data.list.map((t) => t.plays);
  const sorted = plays.every((v, i) => i === 0 || plays[i - 1] >= v);
  must(sorted, '未按播放量降序');
  return `${json.data.list.length} 首，首位播放 ${plays[0]}`;
});

await check('GET  /api/tracks/{id} spec', async () => {
  const { json } = await api('GET', '/api/tracks/3');
  must(json?.code === 0, '返回失败');
  must(json.data.spec && typeof json.data.spec === 'object', 'spec 未透传为对象');
  return `spec.bpm=${json.data.spec.bpm}`;
});

await check('GET  /api/plugins', async () => {
  const { json } = await api('GET', '/api/plugins');
  must(json?.code === 0, '返回失败');
  const p = json.data.list[0];
  return `${json.data.list.length} 个，首个「${p.name}」formats=${p.formats.join('/')}`;
});

await check('GET  /api/plugins?category=', async () => {
  const { json } = await api('GET', '/api/plugins?category=' + encodeURIComponent('效果器'));
  must(json?.code === 0, '返回失败');
  must(json.data.list.every((x) => x.category === '效果器'), '分类过滤不生效');
  return `${json.data.list.length} 个全部属于「效果器」`;
});

// ---- 输出 ----
const width = Math.max(...results.map((r) => r.name.length));
console.log(`\n  Undercurrent API 冒烟测试  ${BASE}\n`);
for (const r of results) {
  console.log(`  ${r.ok ? '\x1b[32mPASS\x1b[0m' : '\x1b[31mFAIL\x1b[0m'}  ${pad(r.name, width)}  ${r.detail}`);
}
const failed = results.filter((r) => !r.ok).length;
console.log(`\n  合计 ${results.length} 项，通过 ${results.length - failed}，失败 ${failed}\n`);
process.exit(failed ? 1 : 0);
