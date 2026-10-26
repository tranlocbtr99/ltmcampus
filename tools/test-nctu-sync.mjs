import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const script=readFileSync(new URL('../app/src/main/assets/nctu-sync.js',import.meta.url),'utf8');
async function run({data={studentLookup:{studentId:'student'},sessions:[]},error,origin='https://sv.nctu.edu.vn',pathname='/dashboard',moduleOrigin='https://sv.nctu.edu.vn',hasApi=true}={}) {
 const messages=[];const calls=[];
 const window={location:{origin,pathname,href:origin+pathname},CampusScheduleSink:{postMessage:x=>messages.push(JSON.parse(x))}};window.top=window;
 const context=vm.createContext({window,URL,document:{querySelectorAll:()=>[{src:moduleOrigin+'/assets/main-test.js'}]}});
 vm.runInContext(script,context);
 const loader=async()=>hasApi?{randomMinifiedAlias:{getPeriodTimes(){},async getAllStudentSchedules(params){calls.push(params);if(error)throw error;return data;}}}:{};
 await window.__campusFetchSchedule({nonce:'request-123',start:'2026-11-01',end:'2026-11-30'},loader);
 return {messages,calls};
}
let checks=0;
const success=await run();assert.equal(success.messages[0].status,'ok');assert.equal(success.calls[0].ngayBatDau,'2026-11-01');checks++;
const auth=await run({error:{response:{status:401}}});assert.equal(auth.messages[0].status,'auth');assert.equal(auth.messages[0].data,undefined);checks++;
const network=await run({error:new Error('network')});assert.equal(network.messages[0].status,'error');checks++;
assert.equal((await run({pathname:'/login'})).messages[0].status,'auth');checks++;
assert.equal((await run({hasApi:false})).messages[0].code,'PORTAL_CHANGED');checks++;
assert.equal((await run({moduleOrigin:'https://evil.example'})).calls.length,0);checks++;
assert.equal((await run({origin:'https://evil.example'})).messages.length,0);checks++;
assert.equal((await run({data:{sessions:[]}})).messages[0].status,'auth');checks++;
assert.equal((await run({data:{studentLookup:{studentId:'x'},sessions:[],hasMore:true}})).messages[0].code,'TRUNCATED');checks++;
const clean=await run({data:{studentLookup:{studentId:'s',fullName:'PRIVATE'},access_token:'SECRET',sessions:[{id:'a',explicitDate:'2026-11-11',subjectNameVi:'Toán',password:'SECRET',token:'SECRET',ems:{sensitive:'SECRET'}}]}});
assert.equal(JSON.stringify(clean.messages).includes('SECRET'),false);assert.equal(JSON.stringify(clean.messages).includes('PRIVATE'),false);checks++;
if(process.env.CAMPUS_TEST_SCHEDULE){const fixture=JSON.parse(readFileSync(process.env.CAMPUS_TEST_SCHEDULE,'utf8'));const result=await run({data:fixture.data});assert.equal(result.messages[0].status,'ok');assert.equal(result.messages[0].data.sessions.length,fixture.data.sessions.length);checks++;}
console.log(`${checks} portal integration checks passed`);
