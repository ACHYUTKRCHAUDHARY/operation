import React,{useDeferredValue,useMemo,useState} from 'react'
import {createRoot} from 'react-dom/client'
import {QueryClient,QueryClientProvider,useMutation,useQuery,useQueryClient} from '@tanstack/react-query'
import {Activity,Box,BriefcaseBusiness,ChevronRight,CircleDollarSign,Factory,Menu,PackageCheck,RefreshCw,Search,Truck,Users,X,Zap} from 'lucide-react'
import './styles.css'

const qc=new QueryClient({defaultOptions:{queries:{staleTime:45_000,gcTime:10*60_000,refetchOnWindowFocus:false,retry:1}}})
const API=(import.meta.env.VITE_API_BASE||'').replace(/\/$/,'')
const tokenKey='yardflow_access_token'
async function api(path,options={}){
  const headers=new Headers(options.headers||{})
  if(options.body&&!headers.has('Content-Type'))headers.set('Content-Type','application/json')
  const token=sessionStorage.getItem(tokenKey)
  if(token)headers.set('Authorization',`Bearer ${token}`)
  const res=await fetch(`${API}${path}`,{...options,headers})
  if(res.status===401){sessionStorage.removeItem(tokenKey);location.href='/login.html';throw new Error('Session expired')}
  if(!res.ok){let msg='Request failed';try{const b=await res.json();msg=b.message||b.error||msg}catch{}throw new Error(msg)}
  if(res.status===204)return null
  return res.json()
}
const pretty=s=>(s||'').replaceAll('_',' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase())
const money=v=>v==null?'—':new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(v)
const tone=s=>['COMPLETED','DELIVERED','AVAILABLE','APPROVED','PAID','READY_FOR_DISPATCH'].includes(s)?'ok':['BLOCKED','CANCELLED','OVERDUE'].includes(s)?'bad':['IN_PROGRESS','IN_TRANSIT','DISPATCHED'].includes(s)?'info':'warn'
function useOps(){
  const dashboard=useQuery({queryKey:['dashboard'],queryFn:()=>api('/api/dashboard')})
  const customers=useQuery({queryKey:['customers'],queryFn:()=>api('/api/customers')})
  const assets=useQuery({queryKey:['assets'],queryFn:()=>api('/api/assets')})
  const workOrders=useQuery({queryKey:['workOrders'],queryFn:()=>api('/api/work-orders')})
  const deliveries=useQuery({queryKey:['deliveries'],queryFn:()=>api('/api/deliveries')})
  const inventory=useQuery({queryKey:['inventory'],queryFn:()=>api('/api/inventory')})
  return {dashboard,customers,assets,workOrders,deliveries,inventory}
}
const nav=[['overview','Overview',Activity],['work','Work Orders',BriefcaseBusiness],['assets','Assets',Box],['dispatch','Dispatch',Truck],['inventory','Inventory',PackageCheck],['customers','Customers',Users]]
function App(){
  const data=useOps(), client=useQueryClient()
  const [view,setView]=useState('overview'),[mobile,setMobile]=useState(false),[q,setQ]=useState('')
  const dq=useDeferredValue(q.toLowerCase())
  const loading=Object.values(data).some(x=>x.isPending)
  const refresh=()=>client.invalidateQueries()
  const works=data.workOrders.data||[],delivs=data.deliveries.data||[],assets=data.assets.data||[],inv=data.inventory.data||[],customers=data.customers.data||[]
  const filtered=useMemo(()=>works.filter(w=>JSON.stringify(w).toLowerCase().includes(dq)),[works,dq])
  const d=data.dashboard.data||{}
  return <div className="shell">
    <aside className={mobile?'sidebar open':'sidebar'}>
      <div className="brand"><div className="mark"><Factory size={19}/></div><div><b>YardFlow</b><span>Operations OS</span></div><button className="icon mobile" onClick={()=>setMobile(false)}><X/></button></div>
      <nav>{nav.map(([id,label,Icon])=><button key={id} className={view===id?'nav active':'nav'} onClick={()=>{setView(id);setMobile(false)}}><Icon size={18}/><span>{label}</span></button>)}</nav>
      <div className="sidebar-card"><Zap size={18}/><div><b>Realtime ready</b><span>Cached API + resilient backend</span></div></div>
    </aside>
    <main>
      <header><button className="icon mobile" onClick={()=>setMobile(true)}><Menu/></button><div><p>ANCHOR CONTAINER SERVICE · LIVE OPERATIONS</p><h1>{nav.find(n=>n[0]===view)?.[1]}</h1></div><div className="actions"><button className="soft" onClick={refresh}><RefreshCw size={16}/> Refresh</button><button className="primary" onClick={()=>setView('work')}>New work order <ChevronRight size={16}/></button></div></header>
      {view==='overview'&&<Overview d={d} works={works} deliveries={delivs} inventory={inv} loading={loading}/>}
      {view==='work'&&<ListPage title="Work orders" q={q} setQ={setQ}><WorkTable rows={filtered}/></ListPage>}
      {view==='assets'&&<ListPage title="Assets" q={q} setQ={setQ}><AssetGrid rows={assets.filter(x=>JSON.stringify(x).toLowerCase().includes(dq))}/></ListPage>}
      {view==='dispatch'&&<ListPage title="Dispatch" q={q} setQ={setQ}><DeliveryTable rows={delivs.filter(x=>JSON.stringify(x).toLowerCase().includes(dq))}/></ListPage>}
      {view==='inventory'&&<ListPage title="Inventory" q={q} setQ={setQ}><InventoryTable rows={inv.filter(x=>JSON.stringify(x).toLowerCase().includes(dq))}/></ListPage>}
      {view==='customers'&&<ListPage title="Customers" q={q} setQ={setQ}><CustomerGrid rows={customers.filter(x=>JSON.stringify(x).toLowerCase().includes(dq))}/></ListPage>}
    </main>
  </div>
}
function Overview({d,works,deliveries,inventory,loading}){
 const stats=[['Active work',d.activeWorkOrders??works.filter(x=>x.status==='IN_PROGRESS').length,BriefcaseBusiness],['Assets',d.assets??'—',Box],['In transit',d.deliveriesInTransit??deliveries.filter(x=>x.status==='IN_TRANSIT').length,Truck],['Low stock',d.lowStockItems??inventory.filter(x=>(x.quantityOnHand??0)<=(x.reorderLevel??0)).length,PackageCheck]]
 return <section className="page">
   <div className="hero"><div><span className="pill">LIVE CONTROL CENTER</span><h2>Move work from yard to delivery, without the noise.</h2><p>One view for repairs, production, inventory, customers and dispatch. Fast reads are cached; updates stay live.</p></div><div className="hero-orb"><Factory size={42}/></div></div>
   <div className="stats">{stats.map(([l,v,I])=><article className="stat" key={l}><div className="stat-icon"><I/></div><div><span>{l}</span><strong>{loading?'…':v}</strong></div></article>)}</div>
   <div className="grid2"><article className="panel"><PanelHead eyebrow="WORKSHOP" title="Active work"/><WorkTable rows={works.slice(0,6)}/></article><article className="panel"><PanelHead eyebrow="DISPATCH" title="Latest movement"/><DeliveryTable rows={deliveries.slice(0,6)}/></article></div>
 </section>
}
function ListPage({title,q,setQ,children}){return <section className="page"><div className="toolbar"><div><span className="eyebrow">OPERATIONS</span><h2>{title}</h2></div><div className="search"><Search size={16}/><input value={q} onChange={e=>setQ(e.target.value)} placeholder={`Search ${title.toLowerCase()}…`}/></div></div><article className="panel">{children}</article></section>}
function PanelHead({eyebrow,title}){return <div className="panel-head"><div><span className="eyebrow">{eyebrow}</span><h3>{title}</h3></div></div>}
function Status({value}){return <span className={`status ${tone(value)}`}>{pretty(value)||'—'}</span>}
function WorkTable({rows=[]}){return <div className="table"><div className="tr th"><span>Work order</span><span>Asset</span><span>Status</span><span>Progress</span></div>{rows.length?rows.map(r=><div className="tr" key={r.id}><b>{r.referenceNumber||r.workOrderNumber||`WO-${r.id}`}</b><span>{r.assetCode||r.asset?.assetCode||'—'}</span><span><Status value={r.status}/></span><span>{r.progressPercent??0}%</span></div>):<Empty/>}</div>}
function DeliveryTable({rows=[]}){return <div className="table"><div className="tr th"><span>Delivery</span><span>Vehicle</span><span>Status</span><span>Destination</span></div>{rows.length?rows.map(r=><div className="tr" key={r.id}><b>{r.referenceNumber||`DL-${r.id}`}</b><span>{r.vehicleNumber||'—'}</span><span><Status value={r.status}/></span><span>{r.destinationAddress||'—'}</span></div>):<Empty/>}</div>}
function InventoryTable({rows=[]}){return <div className="table"><div className="tr th"><span>SKU</span><span>Material</span><span>On hand</span><span>Reorder</span></div>{rows.length?rows.map(r=><div className="tr" key={r.id}><b>{r.sku||'—'}</b><span>{r.name||'—'}</span><span>{r.quantityOnHand??0} {r.unit||''}</span><span>{r.reorderLevel??0}</span></div>):<Empty/>}</div>}
function AssetGrid({rows=[]}){return <div className="cards">{rows.length?rows.map(r=><article className="card" key={r.id}><div className="card-icon"><Box/></div><div><span className="eyebrow">{pretty(r.type)}</span><h3>{r.assetCode}</h3><p>{r.currentYardLocation||'Yard location not set'}</p></div><Status value={r.status}/></article>):<Empty/>}</div>}
function CustomerGrid({rows=[]}){return <div className="cards">{rows.length?rows.map(r=><article className="card" key={r.id}><div className="avatar">{(r.name||'?')[0]}</div><div><span className="eyebrow">{r.companyName||'CUSTOMER'}</span><h3>{r.name}</h3><p>{r.email||r.phone||'No contact added'}</p></div></article>):<Empty/>}</div>}
function Empty(){return <div className="empty">No records found.</div>}
createRoot(document.getElementById('root')).render(<QueryClientProvider client={qc}><App/></QueryClientProvider>)
