/* Uses the official page's exported client: cookies, refresh and auth stay in the portal. */
window.__campusFetchSchedule = async function(request, loadModule = url => import(url)) {
  const send = (status, extra = {}) => window.CampusScheduleSink.postMessage(JSON.stringify({nonce:request.nonce,status,...extra}));
  if (window.location.origin !== 'https://sv.nctu.edu.vn' || window.top !== window) return;
  if (/^\/(login|auth)(\/|$)/.test(window.location.pathname)) { send('auth'); return; }
  try {
    let api;
    const scripts = Array.from(document.querySelectorAll('script[type="module"][src]'));
    for (const script of scripts.slice(0, 4)) {
      const url = new URL(script.src, window.location.href);
      if (url.origin !== window.location.origin) continue;
      const namespace = await loadModule(url.href);
      api = Object.values(namespace).find(x => x && typeof x.getAllStudentSchedules === 'function' && typeof x.getPeriodTimes === 'function');
      if (api) break;
    }
    if (!api) { send('error', {code:'PORTAL_CHANGED'}); return; }
    const data = await api.getAllStudentSchedules({Page:1,PageSize:1000,ngayBatDau:request.start,ngayKetThuc:request.end,_t:Date.now()});
    if (!data || data.success === false || !Array.isArray(data.sessions)) { send('error',{code:'FORMAT'}); return; }
    const identity = data.studentLookup && (data.studentLookup.studentId || data.studentLookup.resolvedMaSinhVien);
    if (typeof identity !== 'string' || !identity.trim()) { send('auth'); return; }
    if (data.sessions.length >= 1000 || data.hasMore === true || Number(data.totalCount || 0) > data.sessions.length) { send('error',{code:'TRUNCATED'}); return; }
    const keys=['id','explicitDate','startDate','endDate','weekday','startPeriod','endPeriod','startTime','endTime','subjectNameVi','className','roomName','teacherFullName','classCode','isTamNgung'];
    const sessions=data.sessions.map(item => {
      if (!item || typeof item !== 'object') throw new Error('FORMAT');
      const clean={};for(const key of keys) if(Object.prototype.hasOwnProperty.call(item,key)) clean[key]=item[key];return clean;
    });
    const periodTimes={};
    for(let i=1;i<=24;i++) {const p=data.periodTimes && data.periodTimes[i];if(p) periodTimes[i]={start:p.start,end:p.end};}
    send('ok',{identity,start:request.start,end:request.end,data:{sessions,periodTimes}});
  } catch(error) {
    const status=error && error.response && error.response.status;
    send(status===401 || status===403 ? 'auth' : 'error',{code:'NETWORK'});
  }
};
