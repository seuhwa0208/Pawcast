# -*- coding: utf-8 -*-
# gen_pawcast_seed.py 끝에서 exec 됨 (같은 변수 공간 사용). AI 학습/검증용 CSV 내보내기.
import os, csv
import pandas as pd
os.makedirs(A.ai_dir, exist_ok=True)
def save(df, name):
    df.to_csv(os.path.join(A.ai_dir, name), index=False, encoding='utf-8-sig'); print('  ', name, df.shape)
TR_END, VA_END = dt.date(2026, 3, 31), dt.date(2026, 7, 31)          # train ≤ 2026-03, valid 2026-04~07, test 2026-08~
def split_of(d): return 'train' if d <= TR_END else ('valid' if d <= VA_END else 'test')
GROUP_OF = lambda pid: TAGS[prod_tag[pidx[pid]]]
PTYPE = {p: ptype_of_cat(products[p]['cat']) for p in PID}
LAST_FULL = END - dt.timedelta(days=1)          # 마지막 날은 부분 집계라 제외

# ---------- 1) 일별 매출 + 달력/이벤트 특성
ev_name = {}; ev_disc = {}
for e in EV:
    if e['notice'] is None and e['mult'] == 1.0: continue
    for i in range((e['e'] - e['s']).days + 1):
        d = e['s'] + dt.timedelta(days=i); ev_name.setdefault(d, []).append(e['name'])
def ev_group(n):
    for k, g in (('블랙프라이데이', 'blackfriday'), ('주년', 'anniversary'), ('11.11', 'd1111'), ('크리스마스', 'xmas'), ('신년', 'newyear'), ('가정의달', 'family_month'), ('오픈', 'open'),
                 ('연휴전', 'holiday_pre'), ('연휴후', 'holiday_post'), ('연휴', 'holiday'), ('여름', 'summer'), ('서버', 'outage'), ('SNS', 'viral'), ('포토리뷰', 'photo_review')):
        if k in n: return g
    return 'other'
rows = []
dly = {}
for o in ORDERS:
    r = dly.setdefault(o['ts'].date(), dict(orders=0, cancelled=0, net_sales=0, units=0, guest=0))
    r['orders'] += 1
    if o['status'] == 'CANCELLED': r['cancelled'] += 1
    else:
        r['net_sales'] += o['final']; r['units'] += sum(q for _, _, q in o['items'])
    r['guest'] += o['guest']
signups = Counter(DAYS[d] for d in created_day.tolist())
for d in DAYS:
    if d > LAST_FULL: continue
    r = dly.get(d, dict(orders=0, cancelled=0, net_sales=0, units=0, guest=0)); gs = sorted({ev_group(n) for n in ev_name.get(d, [])})
    rows.append(dict(date=d, split=split_of(d), orders=r['orders'], cancelled_orders=r['cancelled'], net_sales=r['net_sales'], units=r['units'], guest_orders=r['guest'], new_members=signups.get(d, 0),
                     dow=d.weekday(), month=d.month, day_of_month=d.day, is_weekend=int(d.weekday() >= 5), is_payday_window=int(25 <= d.day <= 27 or d.day <= 2),
                     is_event=int(bool(gs)), event_groups='|'.join(gs), event_names='|'.join(ev_name.get(d, []))))
sales = pd.DataFrame(rows); save(sales, 'ai_sales_daily.csv')

# ---------- 2) 상품군(11)/타입별 일별 판매 (품목 단위)
rows = []
agg = {}
for o in ORDERS:
    if o['status'] == 'CANCELLED' or o['ts'].date() > LAST_FULL: continue
    for (pid, vid, q) in o['items']:
        k = (o['ts'].date(), PTYPE[pid], GROUP_OF(pid)); a = agg.setdefault(k, [0, 0]); a[0] += q; a[1] += variants[vid]['price'] * q
cat = pd.DataFrame([dict(date=k[0], split=split_of(k[0]), product_type=k[1], product_group=k[2], units=v[0], gross_sales=v[1]) for k, v in sorted(agg.items())]); save(cat, 'ai_sales_daily_by_group.csv')

# ---------- 3) 미래 달력 (예측 대상 구간, 달력/계획 이벤트만; 정답 없음)
fut = []
PLAN = [('블랙프라이데이', '2026-11-27', '2026-12-01'), ('크리스마스', '2026-12-18', '2026-12-25'), ('신년', '2027-01-01', '2027-01-10'), ('설연휴전', '2027-01-27', '2027-02-04'), ('설연휴', '2027-02-05', '2027-02-08')]
for i in range(1, 101):
    d = END + dt.timedelta(days=i); nm = [n for n, s, e in PLAN if D(s) <= d <= D(e)]
    fut.append(dict(date=d, dow=d.weekday(), month=d.month, day_of_month=d.day, is_weekend=int(d.weekday() >= 5), is_payday_window=int(25 <= d.day <= 27 or d.day <= 2), is_event=int(bool(nm)),
                    event_groups='|'.join(sorted({ev_group(n) for n in nm})), event_names='|'.join(nm)))
save(pd.DataFrame(fut), 'ai_calendar_future.csv')

# ---------- 4) 리뷰 감성 (평가 분할: 시간 + OOD)
pname = {p: products[p]['name'] for p in PID}
rv = []
for r in reviews:
    g = r['g']; d = r['ts'].date()
    sp = 'ood_test' if g['holdout'] else split_of(d)
    rv.append(dict(review_id=r['id'], product_id=r['pid'], product_name=pname[r['pid']], product_type=PTYPE[r['pid']], product_group=GROUP_OF(r['pid']), member_id=r['mi'] + 1,
                   rating=r['rating'], content=r['text'], created_at=r['ts'].strftime('%Y-%m-%d %H:%M:%S'), label=g['label'], label_en=g['label_en'], aspects=g['aspects'],
                   rating_text_mismatch=int(g['mismatch']), has_photo=int(r['photo']), split=sp))
rvdf = pd.DataFrame(rv); save(rvdf, 'ai_review_sentiment.csv')
# 사람이 검수할 골드 후보 400건 (test+ood 층화 추출) — label 숨김
gold = rvdf[rvdf.split.isin(['test', 'ood_test'])].groupby('label', group_keys=False).apply(lambda x: x.sample(min(len(x), 134), random_state=1)).copy()
gold['human_label'] = ''; save(gold[['review_id', 'content', 'rating', 'human_label']], 'ai_review_gold_candidates_blank.csv')

# ---------- 5) 회원 재구매(90일) 스냅샷
SNAP = [('2025-05-31', 'train'), ('2025-08-31', 'train'), ('2025-11-30', 'train'), ('2026-02-28', 'valid'), ('2026-05-31', 'test')]
mem_orders = [[] for _ in range(M)]
for o in ORDERS:
    if o['mi'] is not None and o['status'] != 'CANCELLED': mem_orders[o['mi']].append(o)
mem_cancel = [[] for _ in range(M)]
for o in ORDERS:
    if o['mi'] is not None and o['status'] == 'CANCELLED': mem_cancel[o['mi']].append(o['ts'].date())
mem_rev = [[] for _ in range(M)]
for r in reviews: mem_rev[r['mi']].append((r['ts'].date(), r['rating']))
rows = []
for sd, sp_ in SNAP:
    C = D(sd); H = C + dt.timedelta(days=90)
    for i in range(3, M):
        if created_at[i].date() > C: continue
        if deleted_at[i] and deleted_at[i].date() <= C: continue
        past = [o for o in mem_orders[i] if o['ts'].date() <= C]; fut_o = [o for o in mem_orders[i] if C < o['ts'].date() <= H]
        pr = [r for r in mem_rev[i] if r[0] <= C]
        gaps = [(b['ts'] - a['ts']).days for a, b in zip(past, past[1:])]
        dev = sum(1 for o in past for (p_, _, _) in o['items'] if PTYPE[p_] == 'DEVICE'); tot = sum(len(o['items']) for o in past)
        rows.append(dict(member_id=i + 1, snapshot_date=sd, split=sp_, tenure_days=(C - created_at[i].date()).days, n_orders=len(past), recency_days=(C - past[-1]['ts'].date()).days if past else -1,
                         monetary=sum(o['final'] for o in past), avg_order=round(sum(o['final'] for o in past) / len(past)) if past else 0, avg_gap_days=round(float(np.mean(gaps)), 1) if gaps else -1,
                         orders_last90=sum(1 for o in past if (C - o['ts'].date()).days <= 90), n_cancel=sum(1 for d_ in mem_cancel[i] if d_ <= C), n_reviews=len(pr),
                         avg_rating=round(float(np.mean([x[1] for x in pr])), 2) if pr else 0, device_ratio=round(dev / tot, 3) if tot else 0, primary_species=species1[i], has_second_pet=int(species2[i] is not None),
                         region=REG[region[i]][0], is_social=int(social[i]), target_repurchase_90d=int(len(fut_o) > 0)))
mdf = pd.DataFrame(rows); save(mdf, 'ai_member_repurchase.csv')

# ---------- 6) 추천용 상호작용 + 상품/회원 특성
inter = []
for o in ORDERS:
    if o['mi'] is None or o['status'] == 'CANCELLED' or o['ts'].date() > LAST_FULL: continue
    for (pid, vid, q) in o['items']: inter.append(dict(member_id=o['mi'] + 1, product_id=pid, event='purchase', qty=q, ts=o['ts'].strftime('%Y-%m-%d %H:%M:%S')))
for (m1, pid, ts) in wl: inter.append(dict(member_id=m1, product_id=pid, event='wishlist', qty=1, ts=ts.strftime('%Y-%m-%d %H:%M:%S')))
idf = pd.DataFrame(inter).sort_values('ts').reset_index(drop=True)
idf['split'] = idf.ts.str[:10].map(lambda s: split_of(D(s))); save(idf, 'ai_interactions.csv')
rt = rvdf.groupby('product_id').rating.agg(['mean', 'count'])
pf = pd.DataFrame([dict(product_id=p, name=pname[p], brand=products[p]['brand'], product_type=PTYPE[p], product_group=GROUP_OF(p), median_price=int(pprice[pidx[p]]),
                        pet_types='|'.join(sorted(ptypes.get(p, []))) or 'ALL', avg_rating=round(float(rt['mean'].get(p, 0)), 2), review_count=int(rt['count'].get(p, 0))) for p in PID]); save(pf, 'ai_products.csv')
mf = pd.DataFrame([dict(member_id=i + 1, primary_species=species1[i], second_species=species2[i] or '', region=REG[region[i]][0], signup_date=created_at[i].date(), status=status[i]) for i in range(3, M)]); save(mf, 'ai_members.csv')

# ---------- 7) 설계 정답(학습에 사용 금지): 생성기가 의도한 기대 주문수
truth = pd.DataFrame(dict(date=DAYS, expected_orders=np.round(base_orders, 2), event_multiplier=np.round(order_mult, 3)))
truth = truth[truth.date <= LAST_FULL]; save(truth, 'DESIGN_TRUTH_do_not_train.csv')
