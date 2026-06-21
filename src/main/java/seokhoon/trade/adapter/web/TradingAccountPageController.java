package seokhoon.trade.adapter.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TradingAccountPageController {
    @GetMapping(value = "/operations/accounts", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String page() {
        return """
                <!doctype html><html lang="ko"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>TradeGuard Trading Accounts</title><style>
                :root{--bg:#f3f6f8;--ink:#17212b;--muted:#607080;--line:#d9e1e7;--brand:#175d8f;--bad:#a12622}*{box-sizing:border-box}
                body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",sans-serif}main{max-width:980px;margin:auto;padding:28px 18px 48px}
                a{color:var(--brand)}.card{background:white;border:1px solid var(--line);border-radius:10px;padding:16px;margin:16px 0}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
                label{font-weight:700}input,select,button{width:100%;margin-top:5px;padding:9px;border:1px solid #aebbc5;border-radius:6px;background:white}button{cursor:pointer;font-weight:700}.actions{display:flex;gap:8px}.actions button{width:auto}.account{border-top:1px solid var(--line);padding:12px 0}.account:first-child{border-top:0}.muted{color:var(--muted)}.error{color:var(--bad);font-weight:700}.badge{display:inline-block;padding:3px 8px;border-radius:999px;background:#e7f6ee;font-weight:750;margin-left:6px}
                @media(max-width:700px){.grid{grid-template-columns:1fr}}
                </style></head><body><main>
                <p><a href="/operations/dashboard?view=trading">← 주문·포지션</a></p><h1>거래 계좌 관리</h1>
                <p class="muted">계좌번호는 AES-256-GCM 암호문으로만 저장되고 목록에는 끝 2자리만 표시됩니다.</p>
                <section class="card"><h2>계좌 등록</h2><form id="create"><div class="grid">
                <label>별칭<input name="alias" required maxlength="100" autocomplete="off"></label>
                <label>환경<select name="environment"><option value="DEMO">모의투자</option><option value="REAL">실전투자</option></select></label>
                <label>계좌번호 앞 8자리<input name="accountNumber" required pattern="[0-9]{8}" inputmode="numeric" autocomplete="off"></label>
                <label>상품코드 2자리<input name="productCode" required pattern="[0-9]{2}" inputmode="numeric" autocomplete="off"></label>
                </div><label><input name="primaryAccount" type="checkbox" style="width:auto"> 현재 거래 기본 계좌로 선택</label>
                <button type="submit">암호화하여 등록</button><p id="message" aria-live="polite"></p></form></section>
                <section class="card"><h2>등록 계좌</h2><div id="accounts">불러오는 중…</div></section>
                <section class="card"><h2>KIS 모의·실전 API 설정</h2><p class="muted">환경별 App Key/Secret과 base URL을 암호화해 저장합니다.</p>
                <form id="kis"><div class="grid"><label>환경<select name="environment"><option value="DEMO">모의투자</option><option value="REAL">실전투자</option></select></label>
                <label>Base URL<input name="baseUrl" type="url" required value="https://openapivts.koreainvestment.com:29443" autocomplete="off"></label>
                <label>App Key<input name="appKey" type="password" required autocomplete="new-password"></label>
                <label>App Secret<input name="appSecret" type="password" required autocomplete="new-password"></label></div>
                <label><input name="active" type="checkbox" checked style="width:auto"> 활성</label><button type="submit">KIS 설정 암호화 저장</button><p id="kisMessage" aria-live="polite"></p></form>
                <div id="kisConfigs"></div></section>
                <section class="card"><h2>DART API 설정</h2><form id="dart"><div class="grid">
                <label>Base URL<input name="baseUrl" type="url" required value="https://opendart.fss.or.kr" autocomplete="off"></label>
                <label>API Key<input name="apiKey" type="password" required autocomplete="new-password"></label></div>
                <label><input name="active" type="checkbox" checked style="width:auto"> 활성</label><button type="submit">DART 설정 암호화 저장</button><p id="dartMessage" aria-live="polite"></p></form><div id="dartConfig"></div></section>
                <script>
                const box=document.getElementById('accounts'),msg=document.getElementById('message');
                const request=async(url,options={})=>{const r=await fetch(url,options);if(!r.ok)throw new Error((await r.text())||('HTTP '+r.status));return r.json()};
                const load=async()=>{try{const rows=await request('/api/trading-accounts');box.replaceChildren(...rows.map(row=>{const el=document.createElement('div');el.className='account';
                const title=document.createElement('strong');title.textContent=row.alias+' · '+row.environment+' · '+row.maskedAccountNumber+'-'+row.productCode;el.append(title);
                if(row.primaryAccount){const b=document.createElement('span');b.className='badge';b.textContent='기본';el.append(b)}
                const state=document.createElement('p');state.className='muted';state.textContent=row.active?'활성':'비활성';el.append(state);
                const actions=document.createElement('div');actions.className='actions';
                if(row.active&&!row.primaryAccount){const primary=document.createElement('button');primary.textContent='기본 계좌 선택';primary.onclick=async()=>{await request('/api/trading-accounts/'+row.id+'/primary',{method:'POST'});await load()};actions.append(primary)}
                const active=document.createElement('button');active.textContent=row.active?'비활성화':'활성화';active.onclick=async()=>{await request('/api/trading-accounts/'+row.id+'/active?value='+(!row.active),{method:'POST'});await load()};actions.append(active);el.append(actions);return el}));if(!rows.length)box.textContent='등록된 계좌가 없습니다.'}catch(e){box.textContent='계좌 목록을 불러오지 못했습니다.'}};
                document.getElementById('create').addEventListener('submit',async e=>{e.preventDefault();msg.className='';msg.textContent='';const f=e.currentTarget;const body={alias:f.alias.value,environment:f.environment.value,accountNumber:f.accountNumber.value,productCode:f.productCode.value,primaryAccount:f.primaryAccount.checked};try{await request('/api/trading-accounts',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});f.accountNumber.value='';msg.textContent='등록했습니다.';await load()}catch(err){f.accountNumber.value='';msg.className='error';msg.textContent='등록하지 못했습니다. 암호화 키와 입력값을 확인하세요.'}});
                const loadKis=async()=>{const box=document.getElementById('kisConfigs');try{const rows=await request('/api/external-api-configurations/kis');box.textContent=rows.length?rows.map(x=>x.environment+' · '+x.maskedAppKey+' · '+x.baseUrl+' · '+(x.active?'활성':'비활성')).join(' / '):'저장된 KIS 설정이 없습니다.'}catch(e){box.textContent='KIS 설정을 불러오지 못했습니다.'}};
                document.getElementById('kis').environment.addEventListener('change',e=>{document.getElementById('kis').baseUrl.value=e.target.value==='REAL'?'https://openapi.koreainvestment.com:9443':'https://openapivts.koreainvestment.com:29443'});
                document.getElementById('kis').addEventListener('submit',async e=>{e.preventDefault();const f=e.currentTarget,m=document.getElementById('kisMessage');try{await request('/api/external-api-configurations/kis/'+f.environment.value,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({appKey:f.appKey.value,appSecret:f.appSecret.value,baseUrl:f.baseUrl.value,active:f.active.checked})});f.appKey.value='';f.appSecret.value='';m.textContent='저장했습니다.';await loadKis()}catch(err){f.appKey.value='';f.appSecret.value='';m.className='error';m.textContent='저장하지 못했습니다.'}});
                const loadDart=async()=>{const box=document.getElementById('dartConfig');try{const x=await request('/api/external-api-configurations/dart');box.textContent=x&&x.apiKeyConfigured?x.maskedApiKey+' · '+x.baseUrl+' · '+(x.active?'활성':'비활성'):'저장된 DART 설정이 없습니다.'}catch(e){box.textContent='DART 설정을 불러오지 못했습니다.'}};
                document.getElementById('dart').addEventListener('submit',async e=>{e.preventDefault();const f=e.currentTarget,m=document.getElementById('dartMessage');try{await request('/api/external-api-configurations/dart',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({apiKey:f.apiKey.value,baseUrl:f.baseUrl.value,active:f.active.checked})});f.apiKey.value='';m.textContent='저장했습니다.';await loadDart()}catch(err){f.apiKey.value='';m.className='error';m.textContent='저장하지 못했습니다.'}});load();loadKis();loadDart();
                </script></main></body></html>
                """;
    }
}
