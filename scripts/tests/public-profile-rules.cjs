// Local Firestore emulator only. Never contacts a production project.
const assert = require('node:assert/strict');
const project = 'demo-kasiguru-ui';
const host = process.env.FIRESTORE_EMULATOR_HOST;
assert.ok(host && /^(127\.0\.0\.1|localhost):\d+$/.test(host), 'Start a local Firestore emulator first');
const base = `http://${host}/v1/projects/${project}/databases/(default)/documents`;
const encode = value => Array.isArray(value) ? {arrayValue: {values: value.map(encode)}}
  : value && typeof value === 'object' ? {mapValue: {fields: Object.fromEntries(Object.entries(value).map(([k,v]) => [k,encode(v)]))}}
  : typeof value === 'number' ? {integerValue: String(value)} : typeof value === 'boolean' ? {booleanValue: value} : {stringValue: value};
// The emulator accepts unsigned test ID tokens; these are never valid outside the emulator.
const token = (uid, provider = 'password') => [
  {alg:'none', typ:'JWT'}, {sub:uid, user_id:uid, aud:project, iss:`https://securetoken.google.com/${project}`,
    iat:Math.floor(Date.now()/1000), exp:Math.floor(Date.now()/1000)+3600, firebase:{sign_in_provider:provider}}
].map(v => Buffer.from(JSON.stringify(v)).toString('base64url')).join('.')+'.';
const request = (method, path, data, uid, provider) => fetch(`${base}/${path}`, {
  method, headers: {'content-type':'application/json', ...(uid ? {Authorization:`Bearer ${token(uid,provider)}`} : {})},
  ...(data ? {body:JSON.stringify({fields:Object.fromEntries(Object.entries(data).map(([k,v])=>[k,encode(v)]))})} : {})
});
const valid = {displayName:'Kiko', profileIconId:1, profileBackgroundId:'forest', level:3, totalXp:420,
 currentStreak:7, wordsLearned:12, lessonsCompleted:3, weeklyXp:100, weekId:'2026-W40', createdAt:1, updatedAt:2,
 badgeIds:['badge:word_explorer:1'], sections:{pagbati:2}, sectionTotals:{pagbati:4}, masteredSections:[], unlockedSections:['pagbati']};
(async () => {
 let checks=0;
 const expect = async (method,path,data,uid,status,provider) => {
   const response=await request(method,path,data,uid,provider);
   const body=await response.text();
   assert.equal(response.status,status,`${method} ${path}: ${body}`); checks++;
 };
 await expect('PATCH','public_profiles/alice',valid,'alice',200);
 await expect('GET','public_profiles/alice',null,null,200);
 await expect('PATCH','public_profiles/alice',valid,'bob',403);
 await expect('PATCH','public_profiles/guest',valid,'guest',403,'anonymous');
 await expect('PATCH','public_profiles/nobody',valid,null,403);
 for (const key of ['email','fullName','age','address','password','recoveryCode']) {
   await expect('PATCH','public_profiles/alice',{...valid,[key]:'private'},'alice',403);
 }
 for (const change of [{level:31},{totalXp:-1},{profileBackgroundId:'unknown'},
   {badgeIds:['unknown:badge']},{sections:{privateField:1}},{sectionTotals:{pagbati:'private'}},
   {masteredSections:['private']},{unlockedSections:['private']}]) {
   await expect('PATCH','public_profiles/alice',{...valid,...change},'alice',403);
 }
 await expect('GET','users/alice/progress/main',null,'bob',403);
 await expect('DELETE','public_profiles/alice',null,'bob',403);
 await expect('DELETE','public_profiles/alice',null,'alice',200);
 console.log(`${checks} public-profile security checks passed on the local emulator.`);
})().catch(error=>{console.error(error);process.exitCode=1;});
