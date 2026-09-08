const state={dashboard:null,customers:[],assets:[],workOrders:[],deliveries:[],inventory:[],health:null};

async function api(url,options={}){
  const headers={...(options.body?{'Content-Type':'application/json'}:{}),...(options.headers||{})};
  const res=await fetch(url,{credentials:'same-origin',...options,headers});
  if(res.status===401){location.replace('/login.html');throw new Error('Session expired');}
  if(!res.ok){let m='Request failed';try{const body=await res.json();m=body.message||body.error||m}catch{}throw new Error(m)}
  if(res.status===204)return null;
  const type=res.headers.get('content-type')||'';
  return type.includes('application/json')?res.json():res.text();
}

const badgeClass=s=>{if(['COMPLETED','DELIVERED','INSTALLED','READY_FOR_DISPATCH','VERIFIED','UP','CLOSED','AVAILABLE','PAID','APPROVED'].includes(s))return'green';if(['BLOCKED','DELAYED','CANCELLED','DOWN','OPEN','OVERDUE'].includes(s))return'red';if(['IN_PROGRESS','IN_TRANSIT','DISPATCHED','NEAR_DESTINATION'].includes(s))return'blue';if(['QUALITY_CHECK','CUSTOMER_APPROVAL_PENDING','INSPECTION_PENDING','ESTIMATE_PENDING','DEGRADED'].includes(s))return'amber';return'gray'};
const pretty=s=>(s||'').toString().replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
const money=v=>v==null?'—':new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(v);
const date=v=>v?new Date(v).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'}):'—';
const escapeHtml=v=>String(v??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));

function toast(msg){const el=document.getElementById('toast');el.textContent=msg;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),2400)}
function openModal(id){document.getElementById(id).classList.add('open');syncSelects()}
function closeModal(id){document.getElementById(id).classList.remove('open')}
function showView(id){document.querySelectorAll('.view').forEach(v=>v.classList.remove('active-view'));document.getElementById(id).classList.add('active-view');document.querySelectorAll('.nav-item[data-view]').forEach(b=>b.classList.toggle('active',b.dataset.view===id));document.getElementById('page-title').textContent=id==='system'?'System Health':id==='search'?'Global Search':pretty(id);if(id==='system')loadHealth();}

document.querySelectorAll('.nav-item[data-view]').forEach(b=>b.addEventListener('click',()=>showView(b.dataset.view)));
function openQuickCreate(){openModal('work-modal')}

async function refreshAll(){
  try{
    const[d,c,a,w,del,i]=await Promise.all([api('/api/dashboard'),api('/api/customers'),api('/api/assets'),api('/api/work-orders'),api('/api/deliveries'),api('/api/inventory')]);
    Object.assign(state,{dashboard:d,customers:c,assets:a,workOrders:w,deliveries:del,inventory:i});
    renderAll();syncSelects();loadHealth(true);
  }catch(e){toast(e.message)}
}

function renderAll(){renderDashboard();renderWorkOrders();renderAssets();renderDeliveries();renderInventory();renderCustomers()}

function renderDashboard(){
  const d=state.dashboard;if(!d)return;
  const k=[['Customers',d.customers,'◎'],['Assets',d.assets,'▣'],['Active Work',d.activeWorkOrders,'⌁'],['In Transit',d.deliveriesInTransit,'➜'],['Low Stock',d.lowStockItems,'!']];
  document.getElementById('kpis').innerHTML=k.map(x=>`<div class="kpi"><div class="kpi-top"><span class="label">${x[0]}</span><span class="kpi-icon">${x[2]}</span></div><div class="value">${x[1]??0}</div><div class="label">Live operational count</div></div>`).join('');
  const active=state.workOrders.filter(w=>!['COMPLETED','CANCELLED'].includes(w.status)).slice(0,6);
  document.getElementById('active-work').innerHTML=active.length?active.map(w=>`<div class="work-row"><div><span class="code">${escapeHtml(w.orderNumber)}</span><span class="sub">${escapeHtml(w.assetCode)} · ${escapeHtml(w.customerName)}</span></div><div><span class="badge ${badgeClass(w.status)}">${pretty(w.status)}</span><span class="sub">${pretty(w.workType)}</span></div><div><span class="sub">${w.progressPercent??0}% complete</span><div class="progress"><span style="width:${Math.max(0,Math.min(100,w.progressPercent??0))}%"></span></div></div><button class="mini-btn" onclick="editWork(${w.id})">Update</button></div>`).join(''):'<div class="empty">No active work orders</div>';
  const alerts=d.alerts||[];
  document.getElementById('alerts').innerHTML=alerts.length?alerts.map(a=>`<div class="alert ${a.severity==='HIGH'?'high':''}"><div class="alert-dot"></div><div><strong>${pretty(a.type)} · ${escapeHtml(a.reference)}</strong><p>${escapeHtml(a.message)}</p></div></div>`).join(''):'<div class="empty">No exceptions right now</div>';
  document.getElementById('delivery-preview').innerHTML=deliveryTable(state.deliveries.slice(0,5),true);
  const low=state.inventory.filter(i=>i.lowStock).slice(0,6);
  document.getElementById('low-stock').innerHTML=low.length?low.map(i=>`<div class="low-stock-item"><div><strong>${escapeHtml(i.name)}</strong><span class="sub">${escapeHtml(i.sku)}</span></div><span class="badge red">${i.quantityOnHand} ${escapeHtml(i.unit||'')}</span></div>`).join(''):'<div class="empty">Stock levels look healthy</div>';
}

function renderWorkOrders(){const q=(document.getElementById('work-search')?.value||'').toLowerCase(),f=document.getElementById('work-filter')?.value||'';const rows=state.workOrders.filter(w=>(!f||w.status===f)&&[w.orderNumber,w.assetCode,w.customerName].join(' ').toLowerCase().includes(q));document.getElementById('work-table').innerHTML=`<table><thead><tr><th>Work Order</th><th>Asset</th><th>Type</th><th>Status</th><th>Progress</th><th>Team</th><th>Expected</th><th></th></tr></thead><tbody>${rows.map(w=>`<tr><td><strong>${escapeHtml(w.orderNumber)}</strong><span class="sub">${escapeHtml(w.customerName)}</span></td><td>${escapeHtml(w.assetCode)}</td><td>${pretty(w.workType)}</td><td><span class="badge ${badgeClass(w.status)}">${pretty(w.status)}</span></td><td>${w.progressPercent??0}%</td><td>${escapeHtml(w.assignedTeam||'—')}</td><td>${date(w.expectedCompletionAt)}</td><td><button class="mini-btn" onclick="editWork(${w.id})">Update</button></td></tr>`).join('')}</tbody></table>`}
function renderAssets(){const q=(document.getElementById('asset-search')?.value||'').toLowerCase(),f=document.getElementById('asset-filter')?.value||'';const rows=state.assets.filter(a=>(!f||a.type===f)&&[a.assetCode,a.customerName,a.currentYardLocation].join(' ').toLowerCase().includes(q));document.getElementById('asset-table').innerHTML=rows.map(a=>`<div class="asset-card"><span class="badge ${a.type==='CONTAINER'?'blue':'green'}">${pretty(a.type)}</span><h3>${escapeHtml(a.assetCode)}</h3><span class="badge ${badgeClass(a.status)}">${pretty(a.status)}</span><div class="asset-meta"><span>Customer: <strong>${escapeHtml(a.customerName)}</strong></span><span>Size: ${escapeHtml(a.sizeDescription||'—')}</span><span>Location: ${escapeHtml(a.currentYardLocation||'—')}</span></div></div>`).join('')||'<div class="empty">No assets found</div>'}
function deliveryTable(rows){return `<table><thead><tr><th>Delivery</th><th>Asset</th><th>Vehicle</th><th>Driver</th><th>Status</th><th>ETA</th><th></th></tr></thead><tbody>${rows.map(d=>`<tr><td><strong>${escapeHtml(d.deliveryNumber)}</strong><span class="sub">${escapeHtml(d.orderNumber)}</span></td><td>${escapeHtml(d.assetCode)}</td><td>${escapeHtml(d.vehicleNumber||'—')}</td><td>${escapeHtml(d.driverName||'—')}</td><td><span class="badge ${badgeClass(d.status)}">${pretty(d.status)}</span></td><td>${date(d.expectedDeliveryAt)}</td><td><a class="mini-btn" href="/tracking.html?id=${d.id}">Track</a></td></tr>`).join('')}</tbody></table>`}
function renderDeliveries(){const q=(document.getElementById('delivery-search')?.value||'').toLowerCase();document.getElementById('delivery-table').innerHTML=deliveryTable(state.deliveries.filter(d=>[d.deliveryNumber,d.assetCode,d.vehicleNumber,d.driverName].join(' ').toLowerCase().includes(q)))}
function renderInventory(){const q=(document.getElementById('inventory-search')?.value||'').toLowerCase();const rows=state.inventory.filter(i=>[i.sku,i.name,i.preferredSupplier].join(' ').toLowerCase().includes(q));document.getElementById('inventory-table').innerHTML=`<table><thead><tr><th>SKU</th><th>Material</th><th>On Hand</th><th>Reorder</th><th>Cost</th><th>Supplier</th><th>Status</th></tr></thead><tbody>${rows.map(i=>`<tr><td><strong>${escapeHtml(i.sku)}</strong></td><td>${escapeHtml(i.name)}</td><td>${i.quantityOnHand} ${escapeHtml(i.unit||'')}</td><td>${i.reorderLevel}</td><td>${money(i.unitCost)}</td><td>${escapeHtml(i.preferredSupplier||'—')}</td><td><span class="badge ${i.lowStock?'red':'green'}">${i.lowStock?'Low stock':'Healthy'}</span></td></tr>`).join('')}</tbody></table>`}
function renderCustomers(){const q=(document.getElementById('customer-search')?.value||'').toLowerCase();const rows=state.customers.filter(c=>[c.name,c.companyName,c.phone,c.email].join(' ').toLowerCase().includes(q));document.getElementById('customer-table').innerHTML=`<table><thead><tr><th>Name</th><th>Company</th><th>Phone</th><th>Email</th><th>Address</th></tr></thead><tbody>${rows.map(c=>`<tr><td><strong>${escapeHtml(c.name)}</strong></td><td>${escapeHtml(c.companyName||'—')}</td><td>${escapeHtml(c.phone||'—')}</td><td>${escapeHtml(c.email||'—')}</td><td>${escapeHtml(c.billingAddress||'—')}</td></tr>`).join('')}</tbody></table>`}

function syncSelects(){document.querySelectorAll('.customer-select').forEach(s=>s.innerHTML=state.customers.map(c=>`<option value="${c.id}">${escapeHtml(c.name)}${c.companyName?' · '+escapeHtml(c.companyName):''}</option>`).join(''));document.querySelectorAll('.asset-select').forEach(s=>s.innerHTML=state.assets.map(a=>`<option value="${a.id}">${escapeHtml(a.assetCode)} · ${pretty(a.type)}</option>`).join(''));document.querySelectorAll('.work-select').forEach(s=>s.innerHTML=state.workOrders.map(w=>`<option value="${w.id}">${escapeHtml(w.orderNumber)} · ${escapeHtml(w.assetCode)}</option>`).join(''))}
function formJson(form){return Object.fromEntries(new FormData(form).entries())}
function editWork(id){const w=state.workOrders.find(x=>x.id===id);if(!w)return;const f=document.getElementById('status-form');f.elements.id.value=id;f.elements.status.value=w.status;f.elements.progressPercent.value=w.progressPercent??0;f.elements.blockedReason.value=w.blockedReason||'';openModal('status-modal')}
async function submitJson(form,url,method,modal,transform=x=>x){const data=transform(formJson(form));try{await api(url,{method,headers:{'Idempotency-Key':crypto.randomUUID?.()||`${Date.now()}-${Math.random()}`},body:JSON.stringify(data)});form.reset();closeModal(modal);toast('Saved successfully');await refreshAll()}catch(e){toast(e.message)}}

document.getElementById('customer-form').addEventListener('submit',e=>{e.preventDefault();submitJson(e.target,'/api/customers','POST','customer-modal')});
document.getElementById('asset-form').addEventListener('submit',e=>{e.preventDefault();submitJson(e.target,'/api/assets','POST','asset-modal',d=>({...d,customerId:Number(d.customerId)}))});
document.getElementById('work-form').addEventListener('submit',e=>{e.preventDefault();submitJson(e.target,'/api/work-orders','POST','work-modal',d=>({...d,customerId:Number(d.customerId),assetId:Number(d.assetId),estimatedCost:d.estimatedCost?Number(d.estimatedCost):null}))});
document.getElementById('inventory-form').addEventListener('submit',e=>{e.preventDefault();submitJson(e.target,'/api/inventory','POST','inventory-modal',d=>({...d,quantityOnHand:Number(d.quantityOnHand),reorderLevel:Number(d.reorderLevel),unitCost:d.unitCost?Number(d.unitCost):null}))});
document.getElementById('delivery-form').addEventListener('submit',e=>{e.preventDefault();submitJson(e.target,'/api/deliveries','POST','delivery-modal',d=>({...d,workOrderId:Number(d.workOrderId),assetId:Number(d.assetId),destinationLatitude:Number(d.destinationLatitude),destinationLongitude:Number(d.destinationLongitude)}))});
document.getElementById('status-form').addEventListener('submit',e=>{e.preventDefault();const d=formJson(e.target),id=d.id;delete d.id;d.progressPercent=d.progressPercent?Number(d.progressPercent):null;submitJson(e.target,`/api/work-orders/${id}`,'PATCH','status-modal',()=>d)});

async function runGlobalSearch(){
  const q=document.getElementById('global-search').value.trim();
  const type=document.getElementById('global-search-type').value;
  const status=document.getElementById('global-search-status').value.trim();
  if(!q){document.getElementById('search-results').innerHTML='<div class="empty">Enter something to search.</div>';return}
  const params=new URLSearchParams({q,limit:'50'});if(type)params.set('type',type);if(status)params.set('status',status);
  const target=document.getElementById('search-results');target.innerHTML='<div class="empty">Searching…</div>';
  try{
    const results=await api(`/api/search?${params}`);
    document.getElementById('search-source').textContent='Resilient search';document.getElementById('search-source').className='badge green';
    target.innerHTML=results.length?results.map(r=>`<article class="search-result"><div class="search-result-main"><span class="badge gray">${pretty(r.type)}</span><strong>${escapeHtml(r.title||r.reference)}</strong><span>${escapeHtml(r.subtitle||'')}</span></div><div class="search-result-meta"><code>${escapeHtml(r.reference||'')}</code>${r.status?`<span class="badge ${badgeClass(r.status)}">${pretty(r.status)}</span>`:''}</div></article>`).join(''):'<div class="empty">No matching records.</div>';
  }catch(e){target.innerHTML=`<div class="empty">${escapeHtml(e.message)}</div>`;document.getElementById('search-source').textContent='Unavailable';document.getElementById('search-source').className='badge red';}
}
document.getElementById('global-search-btn').addEventListener('click',runGlobalSearch);
document.getElementById('global-search').addEventListener('keydown',e=>{if(e.key==='Enter')runGlobalSearch()});

function normalizeDependencies(health){
  const ft=health?.components?.faultTolerance||health?.details?.faultTolerance||health?.faultTolerance;
  const deps=ft?.details?.dependencies||ft?.dependencies||{};
  return Object.entries(deps).map(([name,value])=>({name,...value}));
}

async function loadHealth(silent=false){
  try{
    const health=await api('/actuator/health');state.health=health;
    const overall=health.status||'UNKNOWN';const deps=normalizeDependencies(health);const degraded=overall==='DEGRADED'||deps.some(d=>d.state==='OPEN');
    document.getElementById('sidebar-status').textContent=degraded?'Degraded Mode':overall==='UP'?'System Online':pretty(overall);
    document.getElementById('sidebar-status-detail').textContent=degraded?'Fallbacks are active':'Dependencies monitored';
    document.getElementById('sidebar-status-dot').classList.toggle('warn',degraded||overall!=='UP');
    const banner=document.getElementById('system-banner');
    if(degraded){banner.classList.remove('hidden');banner.innerHTML='<strong>Degraded mode active.</strong> One or more optional dependencies are unavailable; YardFlow is using configured fallbacks.'}else banner.classList.add('hidden');
    document.getElementById('health-overview').innerHTML=`<div class="health-state"><span class="status-orb ${degraded?'warn':'ok'}"></span><div><strong>${degraded?'Degraded but operational':pretty(overall)}</strong><span>${degraded?'Fallback policies are protecting core operations.':'Core application health is normal.'}</span></div></div>`;
    document.getElementById('dependency-health').innerHTML=deps.length?deps.map(d=>`<div class="dependency-card"><div><span class="section-label">DEPENDENCY</span><h3>${escapeHtml(d.name)}</h3></div><span class="badge ${badgeClass(d.state)}">${pretty(d.state)}</span><div class="dependency-meta"><span>Failures <strong>${d.consecutiveFailures??0}</strong></span><span>Permits <strong>${d.availablePermits??'—'}</strong></span><span>Open until <strong>${d.openUntil?date(d.openUntil):'—'}</strong></span></div></div>`).join(''):'<div class="empty">No circuit activity yet. Dependency circuits appear as they are used.</div>';
  }catch(e){
    document.getElementById('sidebar-status').textContent='Health unavailable';document.getElementById('sidebar-status-detail').textContent='Check application status';document.getElementById('sidebar-status-dot').classList.add('warn');
    if(!silent)toast(e.message);
  }
}

async function logout(){try{await api('/api/auth/logout',{method:'POST'});}catch{}finally{location.replace('/login.html')}}
document.getElementById('logout-btn').addEventListener('click',logout);

refreshAll();
setInterval(()=>loadHealth(true),30000);
setInterval(()=>refreshAll(),60000);
