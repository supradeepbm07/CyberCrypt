const $ = id => document.getElementById(id);

function showTab(id){
  document.querySelectorAll('.tab').forEach(x=>x.classList.add('hidden'));
  $(id).classList.remove('hidden');
  if(id==='admin') loadCases();
}

$('caseForm').addEventListener('submit', async e=>{
  e.preventDefault();
  const fd=new FormData(e.target), body={};
  fd.forEach((v,k)=>body[k]=v);
  body.anonymous=fd.get('anonymous')==='on';

  const error=validateComplaint(body);
  if(error){
    $('reportResult').innerHTML=`<div class="form-error">⚠ ${escapeHtml(error)}</div>`;
    $('reportResult').scrollIntoView({behavior:'smooth',block:'center'});
    return;
  }

  try{
    const r=await fetch('/api/cases',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
    const c=await r.json();
    if(!r.ok) throw new Error(c.error||'Unable to register complaint');
    $('reportResult').innerHTML=`
      <div class="card"><h3>Complaint Registered ✓</h3>
      <p>Your secure tracking ID is:</p><h2>${c.trackingId}</h2>
      <p>Priority: <span class="pill ${String(c.priority||'LOW').toLowerCase()}">${c.priority}</span>
      &nbsp; Risk: <b>${c.riskScore}/100</b></p>
      <p>Save this tracking ID. You can use it without revealing your identity.</p>
      <div class="actions"><button onclick="openTrack('${c.trackingId}')">Track Case</button>
      <a href="/api/cases/${c.id}/qr" target="_blank"><button>Open QR Receipt</button></a>
      <a href="/api/cases/${c.id}/report.pdf"><button>PDF Report</button></a></div></div>`;
    e.target.reset();
  }catch(err){$('reportResult').innerHTML=`<div class="form-error">⚠ ${escapeHtml(err.message||'Unable to register complaint. Please check the server and try again.')}</div>`}
});

function validateComplaint(body){
  const desc=String(body.description||'').trim();
  const email=String(body.reporterEmail||'').trim();
  const phone=String(body.phone||'').replace(/[^0-9]/g,'');
  const loss=Number(body.financialLoss||0);
  if(desc.length<20) return 'Please enter at least 20 characters in the incident description.';
  if(email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Please enter a valid email address.';
  if(phone && (phone.length<10 || phone.length>15)) return 'Please enter a valid phone number (10–15 digits).';
  if(!Number.isFinite(loss) || loss<0) return 'Financial loss cannot be negative.';
  if(!body.anonymous && !String(body.reporterName||'').trim()) return 'Enter the reporter name or choose Submit Anonymously.';
  return '';
}

async function trackCase(){
  const id=$('trackId').value.trim();
  if(!id){
    $('trackResult').innerHTML='<div class="card">Please enter a tracking ID.</div>';
    return;
  }
  try{
    const r=await fetch('/api/track/'+encodeURIComponent(id));
    const d=await r.json();
    if(!r.ok){
      $('trackResult').innerHTML=`<div class="card"><h3>Case Not Found</h3><p>${escapeHtml(d.error||'Tracking ID not found')}</p></div>`;
      return;
    }
    renderDetail(d,false);
  }catch(err){
    console.error(err);
    $('trackResult').innerHTML='<div class="card"><h3>Connection Error</h3><p>Could not contact the CyberCrypt server. Make sure the Maven terminal is still running.</p></div>';
  }
}
function openTrack(id){$('trackId').value=id;showTab('track');trackCase()}

async function loadCases(){
  try{
    const r=await fetch('/api/cases');
    const cases=await r.json();
    if(!r.ok) throw new Error('Unable to load cases');
    renderDashboardStats(cases);
    $('caseList').innerHTML=cases.length?cases.sort((a,b)=>(b.riskScore||0)-(a.riskScore||0)).map(c=>`
      <div class="case" onclick="openCase('${c.id}')">
        <span class="pill ${String(c.priority||'LOW').toLowerCase()}">${c.priority||'LOW'}</span>
        ${String(c.trackingId||'').includes('DEMO')?'<span class="demo-tag">SAMPLE DATA</span>':''}
        <h3>${escapeHtml(c.trackingId)}</h3><p>${escapeHtml(c.crimeType)}</p>
        <b>Risk ${c.riskScore||0}/100</b><p>${formatStatus(c.status)}</p>
      </div>`).join(''):'<div class="card">No cases yet. Submit a test complaint from Report Crime.</div>';
  }catch(err){
    $('dashboardStats').innerHTML='';
    $('caseList').innerHTML='<div class="card"><h3>Dashboard unavailable</h3><p>Start the CyberCrypt Spring Boot server and refresh this page.</p></div>';
  }
}

function renderDashboardStats(cases){
  const total=cases.length;
  const closed=cases.filter(c=>String(c.status).toUpperCase()==='CLOSED').length;
  const active=cases.filter(c=>['INVESTIGATION_ASSIGNED','UNDER_REVIEW','IN_PROGRESS'].includes(String(c.status).toUpperCase())).length;
  const pending=Math.max(0,total-active-closed);
  $('dashboardStats').innerHTML=`
    <div class="stat-card"><span>Total Complaints</span><strong>${total}</strong><small>All registered cases</small></div>
    <div class="stat-card pending"><span>Pending</span><strong>${pending}</strong><small>Awaiting investigator action</small></div>
    <div class="stat-card active"><span>Under Investigation</span><strong>${active}</strong><small>Assigned / being reviewed</small></div>
    <div class="stat-card closed"><span>Resolved</span><strong>${closed}</strong><small>Cases marked closed</small></div>`;
}
async function openCase(id){
  const r=await fetch('/api/track/'+encodeURIComponent(await getTracking(id))); 
  const d=await r.json(); renderDetail(d,true);
}
async function getTracking(id){
  const r=await fetch('/api/cases/'+id); const c=await r.json(); return c.trackingId;
}
function renderDetail(d,admin){
  const c=d.case, ev=d.evidence||[], cu=d.custody||[];
  const target = admin ? $('caseDetail') : $('trackResult');
  if(!target) return;
  target.innerHTML=`
  <div class="card"><div class="toolbar"><div><p class="eyebrow">CASE ${escapeHtml(c.trackingId)}</p><h2>${escapeHtml(c.crimeType)}</h2></div>
  <span class="pill ${String(c.priority||'LOW').toLowerCase()}">${c.priority||'LOW'} • ${c.riskScore||0}/100</span></div>
  ${renderCaseProgress(c,ev)}
  <div class="detail-grid"><div>
    <p><b>Status:</b> ${formatStatus(c.status)}</p><p><b>Reporter:</b> ${c.anonymous?'ANONYMOUS':escapeHtml(c.reporterName||'-')}</p>
    <p><b>Financial loss:</b> ₹${Number(c.financialLoss||0).toLocaleString('en-IN')}</p><p><b>Officer:</b> ${escapeHtml(c.assignedOfficer||'Not assigned')}</p>
    <p><b>Verdict:</b> ${escapeHtml(c.verdict||'Pending')}</p>
    <h3>Description</h3><p>${escapeHtml(c.description)}</p>
    ${admin?`<div class="actions">
      <button onclick="assignCase('${c.id}')">Assign Investigator</button>
      <button onclick="scanKeywords('${c.id}')">Keyword Scan</button>
      <button onclick="closeCase('${c.id}')">Close / Verdict</button>
      <a href="/api/cases/${c.id}/report.pdf"><button>PDF Report</button></a>
    </div>`:`<div class="actions"><a href="/api/cases/${c.id}/report.pdf"><button>Download Case Report</button></a></div>`}
  </div>
  <div><h3>Digital Evidence Vault</h3>
    <form onsubmit="uploadEvidence(event,'${c.id}')"><input name="file" type="file" required><button class="primary">Upload & SHA-256</button></form>
    ${ev.map(e=>`<div class="evidence"><b>${escapeHtml(e.originalFileName)}</b><div>${e.sizeBytes} bytes</div><div class="hash">${e.sha256}</div><button onclick="verifyEvidence('${c.id}','${e.id}')">Verify Integrity</button>
      <a href="/api/cases/${c.id}/evidence/${e.id}/download"><button>Download</button></a></div>`).join('')||'<p>No evidence uploaded.</p>'}
  </div></div>
  <hr><h3>Chain of Custody</h3><div class="timeline">${cu.map(e=>`<div class="event"><b>${escapeHtml(e.action)}</b><span>${escapeHtml(e.actor)} • ${formatDateTime(e.eventTime)}</span><div>${escapeHtml(e.details||'')}</div></div>`).join('')||'No custody events.'}</div>
  </div>`;
}

function renderCaseProgress(c,ev){
  const status=String(c.status||'REGISTERED').toUpperCase();
  const investigation=['INVESTIGATION_ASSIGNED','UNDER_REVIEW','IN_PROGRESS','CLOSED'].includes(status) || !!c.assignedOfficer;
  const resolved=status==='CLOSED';
  const evidence=(ev||[]).length>0 || investigation || resolved;
  const steps=[
    {label:'Registered',sub:'Complaint received',done:true},
    {label:'Evidence',sub:evidence?'Evidence secured':'Awaiting upload',done:evidence},
    {label:'Investigation',sub:investigation?'Investigator action':'Awaiting assignment',done:investigation},
    {label:'Resolved',sub:resolved?'Case closed':'Pending verdict',done:resolved}
  ];
  let current=steps.findIndex(x=>!x.done);
  if(current<0) current=steps.length-1;
  return `<div class="case-status-flow">${steps.map((x,i)=>`<div class="status-step ${x.done?'done':(i===current?'current':'')}"><div class="status-dot">${x.done?'✓':i+1}</div><b>${x.label}</b><small>${x.sub}</small></div>`).join('')}</div>`;
}

function formatStatus(status){
  return String(status||'REGISTERED').replaceAll('_',' ').toLowerCase().replace(/\b\w/g,m=>m.toUpperCase());
}

function formatDateTime(value){
  if(!value) return '-';
  const d=new Date(value);
  return Number.isNaN(d.getTime())?escapeHtml(value):d.toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'});
}

async function uploadEvidence(e,id){
  e.preventDefault();const fd=new FormData(e.target);
  const r=await fetch('/api/cases/'+id+'/evidence',{method:'POST',body:fd});const d=await r.json();
  alert(d.message||d.error);openCase(id);
}
async function verifyEvidence(cid,eid){
  const r=await fetch(`/api/cases/${cid}/verify/${eid}`,{method:'POST'});const d=await r.json();
  alert(d.verified?'✓ SHA-256 verified. Evidence unchanged.':'⚠ HASH MISMATCH — possible tampering.');
  openCase(cid);
}
async function assignCase(id){
  const officer=prompt('Investigator name','Cyber Crime Investigation Desk');if(!officer)return;
  await fetch('/api/cases/'+id+'/assign',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({officer,actor:'ADMIN'})});
  openCase(id);
}
async function scanKeywords(id){
  const r=await fetch('/api/cases/'+id+'/keywords');const d=await r.json();
  alert(d.keywords.length?'Detected: '+d.keywords.join(', ')+(d.emergency?'\\n\\nEmergency keyword detected.':''):'No suspicious keywords detected.');
}
async function closeCase(id){
  const verdict=prompt('Verdict / closure note','Closed after investigation');if(verdict===null)return;
  await fetch('/api/cases/'+id+'/status',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({status:'CLOSED',verdict,actor:'INVESTIGATOR'})});
  openCase(id);
}
function escapeHtml(s){return String(s||'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));}

async function requestOtp(){
  const destination=$('otpDestination').value.trim();
  if(!destination){setOtpMessage('Enter an officer phone number or email first.',false);return;}
  try{
    const r=await fetch('/api/auth/request-otp',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({destination})});
    const d=await r.json();
    setOtpMessage(d.otp ? `Demo OTP: ${d.otp} — ${d.note}` : (d.error||d.message),!!d.otp);
  }catch(err){setOtpMessage('Could not request OTP. Check that the server is running.',false);}
}
async function verifyOtp(){
  const destination=$('otpDestination').value.trim(), otp=$('otpValue').value.trim();
  if(!destination){setOtpMessage('Enter the same officer contact used to request the OTP.',false);return;}
  if(!/^\d{6}$/.test(otp)){setOtpMessage('Enter the 6-digit OTP.',false);return;}
  try{
    const r=await fetch('/api/auth/verify-otp',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({destination,otp})});
    const d=await r.json();
    if(d.authenticated){
      setOtpMessage('✓ OTP verified. Investigator dashboard access enabled.',true);
      setTimeout(()=>showTab('admin'),450);
    } else setOtpMessage('✗ Invalid or expired OTP.',false);
  }catch(err){setOtpMessage('Could not verify OTP. Check that the server is running.',false);}
}
function setOtpMessage(message,success){
  const el=$('otpMessage');
  el.textContent=message;
  el.className='otp-message inline-message '+(success?'success':'error');
}

$('trackId').addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();trackCase();}});
