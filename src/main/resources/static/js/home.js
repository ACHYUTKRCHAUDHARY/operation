const grid=document.getElementById('module-grid');
const nameEl=document.getElementById('account-name');
const emailEl=document.getElementById('account-email');
const titleEl=document.getElementById('welcome-title');
const copyEl=document.getElementById('welcome-copy');
const roleEl=document.getElementById('role-pill');

const modules={
  operations:{icon:'▦',title:'Operations Control',copy:'Customers, assets, work orders, production progress, dispatch, inventory and operational alerts.',href:'/'},
  business:{icon:'◆',title:'Business Center',copy:'Fleet, quotations, invoices, payments, procurement, warranty and support workflows.',href:'/management.html'},
  tracking:{icon:'➜',title:'Live Tracking',copy:'Open delivery tracking and monitor movement, ETA and delivery status.',href:'/tracking.html'},
  customer:{icon:'◎',title:'My Customer Portal',copy:'Your assets, workshop progress, deliveries, invoices and payment history.',href:'/customer.html'},
  work:{icon:'⌁',title:'Work Orders',copy:'Jump directly to workshop and production jobs.',href:'/#work-orders'},
  dispatch:{icon:'↗',title:'Dispatch',copy:'Create and monitor delivery operations.',href:'/#deliveries'}
};

function card(m){return `<a class="module-card" href="${m.href}"><div class="module-icon">${m.icon}</div><h2>${m.title}</h2><p>${m.copy}</p><span class="go">Open module →</span></a>`}
function toast(msg){const el=document.getElementById('toast');el.textContent=msg;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),3000)}

async function init(){
  let session=window.yardFlowSession?.();
  if(!session){location.replace('/login.html');return}
  try{
    const r=await fetch('/api/auth/me');
    if(r.status===401){window.yardFlowClearSession?.();location.replace('/login.html?session=expired');return}
    if(r.ok){const me=await r.json();session={...session,...me}}
  }catch(_){toast('Using cached session while backend reconnects')}

  nameEl.textContent=session.fullName||'YardFlow User';
  emailEl.textContent=session.email||'';
  roleEl.textContent=(session.role||'USER').replaceAll('_',' ');

  if(session.role==='CUSTOMER'){
    titleEl.textContent=`Welcome, ${session.fullName||'Customer'}`;
    copyEl.textContent='Track your own assets, active work, delivery movement and commercial status from one place.';
    grid.innerHTML=[modules.customer,modules.tracking].map(card).join('');
    return;
  }

  titleEl.textContent='Operations workspace';
  copyEl.textContent='Run YardFlow from one place. Core operations, workshop jobs, dispatch, fleet, commercial and procurement tools are all available below.';
  const allowed=['ADMIN','OPERATIONS_MANAGER','WORKSHOP_MANAGER','QC_INSPECTOR'];
  grid.innerHTML=allowed.includes(session.role)
    ?[modules.operations,modules.work,modules.dispatch,modules.business,modules.tracking].map(card).join('')
    :[modules.tracking].map(card).join('');
}

document.getElementById('workspace-logout').addEventListener('click',async()=>{try{await fetch('/api/auth/logout',{method:'POST'})}catch{}window.yardFlowClearSession?.();location.replace('/login.html')});
init();
