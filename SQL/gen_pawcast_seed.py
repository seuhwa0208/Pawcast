#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Pawcast 3년치 더미데이터 생성기
- 입력: 기존 시드(pawcast_final_seed.sql)의 카탈로그(카테고리/상품/옵션/이미지/펫타입) 블록을 그대로 재사용
- 출력: 회원~QnA까지 3년치 데이터를 포함한 단일 SQL (+ 선택용 리뷰 감성 라벨 SQL)
- 재현성: --rng 값이 같으면 항상 같은 결과
사용: python3 gen_pawcast_seed.py --seed-file pawcast_final_seed.sql --end 2026-11-20 --now "2026-11-20 10:30"
"""
import re, argparse, math, random, datetime as dt, json
import numpy as np

ap = argparse.ArgumentParser()
ap.add_argument('--seed-file', required=True)
ap.add_argument('--out', default='pawcast_seed_3y.sql')
ap.add_argument('--sent-out', default='pawcast_review_sentiments_optional.sql')
ap.add_argument('--stats-out', default='stats.json')
ap.add_argument('--end', default='2026-11-20')
ap.add_argument('--now', default='2026-11-20 10:30')
ap.add_argument('--members', type=int, default=4000)
ap.add_argument('--rng', type=int, default=20261120)
A = ap.parse_args()

rng = np.random.default_rng(A.rng)
random.seed(A.rng)
D = lambda s: dt.date.fromisoformat(s)
END = D(A.end)
START = END - dt.timedelta(days=1095)          # 양끝 포함 1096일
NOW = dt.datetime.strptime(A.now, '%Y-%m-%d %H:%M')
T = (END - START).days + 1
M = A.members
DAYS = [START + dt.timedelta(days=i) for i in range(T)]
def dix(d): return (d - START).days

# ------------------------------------------------------------------ 1. 카탈로그 파싱
lines = open(A.seed_file, encoding='utf-8').read().split('\n')
i0 = next(i for i, l in enumerate(lines) if l.startswith('INSERT INTO categories'))
i1 = next(i for i, l in enumerate(lines) if l.startswith('UPDATE product_variants SET created_at'))
catalog_lines = [l for l in lines[i0:i1] if not l.startswith('UPDATE ')]
products, variants, ptypes, images, cats = {}, {}, {}, {}, {}
cur = None
for l in catalog_lines:
    m = re.match(r'INSERT INTO (\w+)', l)
    if m: cur = m.group(1); continue
    if cur == 'categories':
        m = re.match(r"^\((\d+),'((?:[^']|'')*)',(NULL|\d+)\)", l)
        if m: cats[int(m[1])] = (m[2], None if m[3] == 'NULL' else int(m[3]))
    elif cur == 'products':
        m = re.match(r"^\((\d+),(\d+),'((?:[^']|'')*)','((?:[^']|'')*)','(FOOD|DEVICE)'", l)
        if m: products[int(m[1])] = dict(cat=int(m[2]), name=m[3].replace("''", "'"), brand=m[4].replace("''", "'"), type=m[5])
    elif cur == 'product_variants':
        m = re.match(r"^\((\d+),(\d+),'((?:[^']|'')*)','((?:[^']|'')*)','((?:[^']|'')*)',(NULL|\d+),(\d+),(\d+),(\d+),'(\w+)'\)", l)
        if m: variants[int(m[1])] = dict(pid=int(m[2]), label=m[5].replace("''", "'"), price=int(m[7]))
    elif cur == 'product_pet_types':
        m = re.match(r"^\((\d+),'(DOG|CAT)'\)", l)
        if m: ptypes.setdefault(int(m[1]), set()).add(m[2])
    elif cur == 'product_images':
        m = re.match(r"^\((\d+),'([^']*)'\)", l)
        if m: images.setdefault(int(m[1]), []).append(m[2])
# ERD 변경: products.product_type = DOG / CAT / DEVICE (최상위 카테고리 기준: 강아지관/고양이관/식기·급수관)
ROOT_TYPE = {'강아지관': 'DOG', '고양이관': 'CAT', '식기·급수관': 'DEVICE'}
def ptype_of_cat(c):
    while cats[c][1] is not None: c = cats[c][1]
    return ROOT_TYPE[cats[c][0]]
for _p in products.values():
    new_t = ptype_of_cat(_p['cat'])
    assert (new_t == 'DEVICE') == (_p['type'] == 'DEVICE'), '카테고리와 기존 product_type 불일치'
    _p['type'] = new_t
_cur = None; _new = []
for l in catalog_lines:
    m = re.match(r'INSERT INTO (\w+)', l)
    if m: _cur = m.group(1); _new.append(l); continue
    if _cur == 'products':
        m = re.match(r"^\((\d+),(\d+),", l)
        if m: l = re.sub(r",'(FOOD|DEVICE)',", ",'%s'," % ptype_of_cat(int(m[2])), l, count=1)
    _new.append(l)
catalog_lines = _new
PID = sorted(products); P = len(PID); pidx = {p: i for i, p in enumerate(PID)}
assert P > 0 and len(variants) > 0, '카탈로그 파싱 실패'
pvars = {p: [] for p in PID}
for vid, v in variants.items(): pvars[v['pid']].append(vid)

TAG = {}
for c in (3, 19): TAG[c] = 'dry'
for c in (4, 20): TAG[c] = 'wet'
for c in (5, 21): TAG[c] = 'func'
for c in (6, 22): TAG[c] = 'age'
for c in (8, 9, 10, 11, 24, 25, 26, 27): TAG[c] = 'treat'
for c in (13, 14, 15, 16, 29, 30, 31, 32): TAG[c] = 'supp'
for c in (35, 36, 37): TAG[c] = 'feeder'
for c in (39, 40): TAG[c] = 'water'
for c in (42, 43, 44): TAG[c] = 'bowl'
for c in (46, 47, 48): TAG[c] = 'filter'
TAGS = ['dry', 'wet', 'func', 'age', 'treat', 'supp', 'feeder', 'water', 'bowl', 'filter', 'other']
tag_i = {t: i for i, t in enumerate(TAGS)}
prod_tag = np.array([tag_i[TAG.get(products[p]['cat'], 'other')] for p in PID])
def domain_of(p):
    t = TAGS[prod_tag[pidx[p]]]
    return 'supp' if t == 'supp' else ('food' if t in ('dry', 'wet', 'func', 'age', 'treat') else 'device')

# 상품 인기/가용기간/품질
tag_pop = dict(dry=1.5, wet=1.0, func=0.8, age=1.1, treat=1.4, supp=0.7, feeder=0.5, water=0.6, bowl=0.6, filter=0.5, other=0.6)
brands = sorted({products[p]['brand'] for p in PID})
brand_f = {b: math.exp(rng.normal(0, 0.45)) for b in brands}
pop = np.array([math.exp(rng.normal(0, 1.0)) * brand_f[products[p]['brand']] * tag_pop[TAGS[prod_tag[pidx[p]]]] for p in PID])
start_day = np.zeros(P, int); end_day = np.full(P, 10 ** 6)
late = rng.random(P) < 0.28
start_day[late] = rng.integers(20, T - 20, late.sum())
disc = (rng.random(P) < 0.035) & (start_day + 150 < T - 20)
for i in np.where(disc)[0]:
    end_day[i] = int(rng.integers(start_day[i] + 120, T - 5))
quality = np.clip(rng.normal(4.15, 0.35, P), 3.2, 4.8)
pet_ok = {sp: np.array([(not ptypes.get(p)) or (sp in ptypes[p]) for p in PID]) for sp in ('DOG', 'CAT')}
pprice = np.array([np.median([variants[v]['price'] for v in pvars[p]]) for p in PID])
logp = np.log(pprice); mu_p, sd_p = logp.mean(), logp.std()
vweights = {p: np.array([math.exp(rng.normal(0, 0.5)) for _ in pvars[p]]) for p in PID}

# ------------------------------------------------------------------ 2. 이벤트 캘린더
EV = []
def ev(name, s, e, mult=1.0, shape='flat', signup=1.0, disc=0.0, delay=0.0, boost=None, cancel=1.0, notice=None):
    EV.append(dict(name=name, s=D(s), e=D(e), mult=mult, shape=shape, signup=signup, disc=disc, delay=delay, boost=boost or {}, cancel=cancel, notice=notice))
def shp(m, shape, i, n):
    if shape == 'flat': return m
    if shape == 'ramp': return 1 + (m - 1) * (i + 1) / n
    return 1 + (m - 1) * math.exp(-3.0 * i / n)    # decay

ev('오픈 기념', '2023-11-21', '2023-12-10', 1.6, 'decay', 7.0, 0.10, notice=(0, 'EVENT', '포캐스트 그랜드 오픈 기념 이벤트', '포캐스트 오픈을 기념해 전 상품 최대 10% 할인과 신규 가입 쿠폰을 드립니다. 이벤트 기간: 오픈일부터 3주간.'))
for y, bf, ann in ((2023, '2023-11-24', None), (2024, '2024-11-29', '2024-11-20'), (2025, '2025-11-28', '2025-11-20'), (2026, None, '2026-11-20')):
    if y == 2023:
        ev('블랙프라이데이2023', '2023-11-24', '2023-11-27', 2.0, 'flat', 2.0, 0.12, notice=(1, 'EVENT', '블랙프라이데이 특가 시작', '블랙프라이데이를 맞아 사료, 간식, 용품을 최대 12% 할인합니다. 11월 24일부터 27일까지 진행됩니다.'))
    if ann:
        n3 = y - 2023
        ev(f'{n3}주년 세일', ann, f'{y}-11-26', 1.9, 'flat', 2.2, 0.12, notice=(0, 'EVENT', f'포캐스트 {n3}주년 감사 세일', f'{n3}주년을 맞아 감사 세일을 진행합니다. 11월 20일부터 26일까지 전 상품 최대 12% 할인.'))
    if y in (2024, 2025):
        ev(f'블랙프라이데이{y}', f'{y}-11-28' if y == 2025 else '2024-11-28', f'{y}-12-02', 2.3, 'flat', 2.4, 0.13, notice=(1, 'EVENT', '블랙프라이데이 특가 시작', '연중 최대 할인! 블랙프라이데이 특가를 놓치지 마세요. 사이버먼데이까지 진행됩니다.'))
    if y >= 2024:
        ev(f'11.11 세일{y}', f'{y}-11-08', f'{y}-11-11', 1.6, 'flat', 1.6, 0.11, notice=(2, 'EVENT', '11.11 데이 특가', '11월 8일부터 11일까지 11.11 데이 특가를 진행합니다.'))
for y in (2023, 2024, 2025):
    ev(f'크리스마스{y}', f'{y}-12-18', f'{y}-12-25', 1.35, 'ramp', 1.0, 0.05, boost=dict(treat=1.5, supp=1.2), notice=(3, 'EVENT', '크리스마스 간식 선물 기획전', '우리 아이에게 주는 크리스마스 선물! 덴탈 간식, 트릿, 츄르를 특별 구성으로 만나보세요.'))
for y in (2024, 2025, 2026):
    ev(f'신년{y}', f'{y}-01-01', f'{y}-01-10', 1.2, 'decay', 1.5, 0.05, boost=dict(supp=1.7), notice=(0, 'EVENT', '새해 반려 건강 기획전', '새해에는 더 건강하게! 영양제와 기능성 사료를 기획전으로 만나보세요.'))
    ev(f'가정의달{y}', f'{y}-05-01', f'{y}-05-08', 1.3, 'flat', 1.4, 0.08, boost=dict(treat=1.4), notice=(2, 'EVENT', '가정의 달 반려가족 감사 페스타', '5월 반려가족 감사 페스타를 진행합니다. 간식과 용품을 특별가로 만나보세요.'))
    ev(f'여름{y}', f'{y}-07-01', f'{y}-08-31', 1.05, 'flat', 1.0, 0.0, boost=dict(water=2.2, feeder=1.3, bowl=1.3, filter=1.4), notice=(3, 'EVENT', '여름 쿨링 & 급수 기획전', '무더운 여름, 자동 급수기와 휴대용 급수용품 기획전을 진행합니다.'))
H = {2024: ('2024-01-31', '2024-02-08', '2024-02-09', '2024-02-12', '2024-02-13', '2024-02-16', '2024-09-07', '2024-09-15', '2024-09-16', '2024-09-18', '2024-09-19', '2024-09-22'),
     2025: ('2025-01-19', '2025-01-27', '2025-01-28', '2025-01-30', '2025-01-31', '2025-02-03', '2025-09-25', '2025-10-02', '2025-10-03', '2025-10-09', '2025-10-10', '2025-10-14'),
     2026: ('2026-02-07', '2026-02-15', '2026-02-16', '2026-02-18', '2026-02-19', '2026-02-22', '2026-09-14', '2026-09-23', '2026-09-24', '2026-09-26', '2026-09-27', '2026-09-30')}
for y, h in H.items():
    for k, nm, gift in ((0, '설', '설 선물세트 기획전'), (6, '추석', '추석 선물세트 기획전')):
        ps, pe, hs, he, bs, be = h[k:k + 6]
        ev(f'{nm}연휴전{y}', ps, pe, 1.55, 'ramp', 1.3, 0.07, boost=dict(supp=1.6, treat=1.5), notice=(2, 'EVENT', gift, f'{nm} 명절을 맞아 반려동물 영양제와 간식 선물세트를 준비했습니다.'))
        long_hol = (y == 2025 and nm == '추석')
        ev(f'{nm}연휴{y}', hs, he, 0.5, 'flat', 0.8, 0.0, delay=3.0 if long_hol else 2.0, cancel=1.6 if long_hol else 1.2,
           notice=(5, 'GENERAL', f'{nm} 연휴 배송 휴무 안내', f'{nm} 연휴({hs}~{he}) 동안 출고와 고객센터가 휴무입니다. 연휴 전후 주문은 배송이 지연될 수 있습니다.'))
        ev(f'{nm}연휴후{y}', bs, be, 1.15 if not long_hol else 1.2, 'flat', 1.0)
ev('서버 장애', '2025-03-14', '2025-03-14', 0.2, 'flat', 0.1, 0.0, cancel=2.0, notice=(-1, 'GENERAL', '서비스 일시 장애 사과 안내', '3월 14일 서비스 접속 장애가 발생했습니다. 불편을 드려 죄송하며 복구가 완료되었습니다.'))
ev('장애 복구 후', '2025-03-15', '2025-03-16', 1.3, 'flat', 1.2)
ev('SNS 화제', '2025-04-17', '2025-04-24', 2.0, 'decay', 3.5, 0.0, delay=1.0, notice=(1, 'GENERAL', '주문 폭주에 따른 출고 지연 안내', '주문이 몰려 출고가 평소보다 1~2일 지연될 수 있습니다. 양해 부탁드립니다.'))
for y in (2024, 2025, 2026):
    ev(f'포토리뷰{y}', f'{y}-03-01', f'{y}-03-31', 1.0, 'flat', 1.0, notice=(0, 'EVENT', '봄맞이 포토리뷰 이벤트', '3월 한 달간 포토리뷰를 남겨주신 분들께 적립금을 드립니다.'))
REVIEW_BOOST_MONTHS = {(2024, 3), (2025, 3), (2026, 3)}

order_mult = np.ones(T); signup_mult = np.ones(T); disc_rate = np.zeros(T); delay_d = np.zeros(T); cancel_mult = np.ones(T)
tag_boost = np.ones((T, len(TAGS)))
for e in EV:
    n = (e['e'] - e['s']).days + 1
    for i in range(n):
        d = dix(e['s']) + i
        if d < 0 or d >= T: continue
        order_mult[d] *= shp(e['mult'], e['shape'], i, n); signup_mult[d] *= shp(e['signup'], e['shape'], i, n)
        disc_rate[d] = max(disc_rate[d], e['disc']); delay_d[d] = max(delay_d[d], e['delay']); cancel_mult[d] *= e['cancel']
        for t, b in e['boost'].items(): tag_boost[d, tag_i[t]] *= b
order_mult = np.minimum(order_mult, 3.6)

DOW = [0.86, 1.18, 1.10, 1.05, 1.02, 1.00, 0.82]     # 일~토 (python weekday 월=0 → 아래서 변환)
MONTH = [0.92, 0.95, 1.08, 1.02, 1.05, 0.98, 1.12, 1.14, 1.00, 1.03, 1.22, 1.06]
HOUR = np.array([1.2, .7, .4, .3, .3, .5, 1.0, 2.0, 3.2, 4.2, 4.8, 5.0, 5.6, 5.4, 4.8, 4.4, 4.4, 4.6, 5.0, 5.8, 6.8, 7.6, 7.0, 3.5])
def dow_f(d): return DOW[(d.weekday() + 1) % 7]
def payday_f(d): return 1.12 if 25 <= d.day <= 27 else (1.05 if d.day <= 2 else 1.0)

ar = np.zeros(T)
for i in range(1, T): ar[i] = 0.88 * ar[i - 1] + rng.normal(0, 0.035)
base_orders = np.array([(8 + 30 * ((i / (T - 1)) ** 0.8)) * dow_f(DAYS[i]) * MONTH[DAYS[i].month - 1] * payday_f(DAYS[i]) * order_mult[i] * math.exp(ar[i]) for i in range(T)])
last_frac = (HOUR[:10].sum() + HOUR[10] * 0.5) / HOUR.sum() if NOW.date() == END else 1.0
base_orders[T - 1] *= last_frac

# ------------------------------------------------------------------ 3. 회원
sw = np.array([(1.3 + 1.7 * (i / (T - 1))) * dow_f(DAYS[i]) * signup_mult[i] for i in range(T)])
sw[T - 1] *= last_frac
signup_day = np.sort(rng.choice(T, size=M - 3, p=sw / sw.sum()))
created_day = np.concatenate([[0, 0, 0], signup_day])
def rand_time(d, last_cap=False):
    h = rng.choice(24, p=HOUR / HOUR.sum())
    if last_cap and d == T - 1: h = min(h, 9)
    return dt.datetime.combine(DAYS[d], dt.time(int(h), int(rng.integers(0, 60)), int(rng.integers(0, 60))))
created_at = [dt.datetime.combine(DAYS[0], dt.time(9, i, 0)) if i < 3 else rand_time(created_day[i], True) for i in range(M)]
spike = signup_mult[created_day] > 1.8
conv = np.where(rng.random(M) < np.where(spike, 0.36, 0.62), 1.0, 0.02)
activity = np.exp(rng.normal(-0.2, 0.9, M)); cycle = np.clip(np.exp(rng.normal(math.log(38), 0.45, M)), 14, 120)
zpref = rng.normal(0, 1, M)
immortal = rng.random(M) < 0.35
death_day = np.where(immortal, 10 ** 6, created_day + np.maximum(rng.exponential(480, M), 20).astype(int))
buyer = np.ones(M, bool); buyer[0] = False; buyer[2] = False       # 관리자/매니저는 구매 안 함
activity[1] = 4.0; cycle[1] = 21; death_day[1] = 10 ** 6; conv[1] = 1.0; created_day[1] = 0
species1 = np.where(rng.random(M) < 0.60, 'DOG', 'CAT')
species2 = np.array([('CAT' if s == 'DOG' else 'DOG') if rng.random() < 0.15 else None for s in species1], dtype=object)
REG = [('서울특별시', .19, '0'), ('경기도', .26, '1'), ('인천광역시', .06, '2'), ('부산광역시', .06, '46'), ('대구광역시', .04, '41'), ('대전광역시', .03, '34'), ('광주광역시', .03, '61'),
       ('울산광역시', .02, '44'), ('세종특별자치시', .01, '30'), ('강원특별자치도', .03, '25'), ('충청북도', .03, '27'), ('충청남도', .04, '31'), ('전북특별자치도', .03, '54'),
       ('전라남도', .03, '58'), ('경상북도', .04, '36'), ('경상남도', .05, '51'), ('제주특별자치도', .02, '63')]
rp = np.array([r[1] for r in REG]); rp /= rp.sum()
region = rng.choice(len(REG), size=M, p=rp)
def zipcode(ri):
    pre = REG[ri][2]; return pre + ''.join(str(random.randint(0, 9)) for _ in range(5 - len(pre)))
tel = ['010-0000-%04d' % random.randint(0, 9999) for _ in range(M)]
social = rng.random(M) < 0.28; social[:3] = False
HASH = '$2a$10$OUH/D6QUL0dDNU5YO5NSgur2TBdcNetWZWZApSflqrT5PmU1U53A.'

# ------------------------------------------------------------------ 4. 주문 시뮬레이션
n_orders = np.zeros(M, int); last_order = np.full(M, -10 ** 6)
hist = [[] for _ in range(M)]           # (pid, vid) 소모품 재구매용
ORDERS = []; ITEMS = []
k_nb = 30.0
guest_seq = 0
for d in range(T):
    mu = base_orders[d]
    n = int(rng.negative_binomial(k_nb, k_nb / (k_nb + mu))) if mu > 0 else 0
    if d == T - 1: n = max(n, 3)
    if n == 0: continue
    alive = (created_day <= d) & (death_day > d) & buyer
    w = np.zeros(M)
    new_m = alive & (n_orders == 0)
    old_m = alive & (n_orders > 0)
    w[new_m] = activity[new_m] * conv[new_m] * (0.05 + 1.6 * np.exp(-(d - created_day[new_m]) / 9.0))
    g = d - last_order[old_m]
    w[old_m] = activity[old_m] * (0.05 + 1.0 * np.exp(-0.5 * ((g - cycle[old_m]) / (0.35 * cycle[old_m])) ** 2) + 0.06 * np.minimum(g, 120) / 120)
    ng = int(rng.binomial(n, 0.06))
    nm = n - ng
    cand = np.where(w > 0)[0]
    if len(cand) == 0: ng, nm = n, 0
    members_pick = []
    if nm > 0:
        p = w[cand] / w[cand].sum()
        members_pick = list(rng.choice(cand, size=nm, replace=(nm > len(cand)), p=p))
    picks = [int(x) for x in members_pick] + [-1] * ng
    random.shuffle(picks)
    tb = tag_boost[d]; boostvec = tb[prod_tag]
    avail = ((start_day <= d) & (end_day >= d)).astype(float)
    times = sorted(rand_time(d, True) for _ in picks)
    if d == T - 1: times = [min(t, NOW - dt.timedelta(minutes=2)) for t in times]
    for mi, ts in zip(picks, times):
        if mi >= 0 and ts < created_at[mi]:      # 가입 시각 이전 주문 방지: 가입 직후로 보정
            ts = min(created_at[mi] + dt.timedelta(minutes=int(rng.integers(3, 240))), dt.datetime.combine(DAYS[d], dt.time(23, 59, 50)))
            if d == T - 1: ts = min(ts, NOW - dt.timedelta(minutes=2))
        guest = mi < 0
        if guest: guest_seq += 1
        sp = 'DOG' if (guest and rng.random() < 0.6) else ('CAT' if guest else (species2[mi] if (species2[mi] and rng.random() < 0.25) else species1[mi]))
        z = 0.0 if guest else zpref[mi]
        ni = int(rng.choice([1, 2, 3], p=[.62, .27, .11]))
        chosen = []
        base_w = pop * avail * boostvec * pet_ok[sp] * np.exp(-0.5 * ((logp - (mu_p + 0.55 * sd_p * z)) / (0.9 * sd_p)) ** 2)
        for k in range(ni):
            vid = None
            if not guest and k == 0 and hist[mi] and rng.random() < 0.38:
                cands = [(p_, v_) for (p_, v_) in hist[mi] if avail[pidx[p_]] and pet_ok[sp][pidx[p_]] and p_ not in [c[0] for c in chosen]]
                if cands: p_, vid = random.choice(cands[-6:]); pid = p_
            if vid is None:
                bw = base_w.copy()
                for c in chosen: bw[pidx[c[0]]] = 0
                if bw.sum() <= 0: continue
                pid = PID[int(rng.choice(P, p=bw / bw.sum()))]
                vs = pvars[pid]; vid = vs[int(rng.choice(len(vs), p=vweights[pid] / vweights[pid].sum()))]
            qty = int(rng.choice([1, 2, 3], p=[.78, .17, .05]))
            chosen.append((pid, vid, qty))
        if not chosen: continue
        ORDERS.append(dict(d=d, ts=ts, mi=None if guest else int(mi), guest=guest, sp=sp, items=chosen, gseq=guest_seq))
        if not guest:
            n_orders[mi] += 1; last_order[mi] = d
            for (p_, v_, q_) in chosen:
                if domain_of(p_) != 'device': hist[mi].append((p_, v_))

ORDERS.sort(key=lambda o: o['ts'])
# ------------------------------------------------------------------ 5. 주문 상세
def pick_status_times(o):
    d = o['d']; ts = o['ts']
    cancelled = rng.random() < 0.04 * cancel_mult[d]
    shipped = delivered = None; st = 'PAID'
    delay = delay_d[d]
    if cancelled: return 'CANCELLED', ts + dt.timedelta(minutes=int(rng.integers(10, 1200))), None, None
    shipped = ts + dt.timedelta(days=float(rng.uniform(0.3, 1.2) + delay * rng.uniform(0.7, 1.0)))
    delivered = shipped + dt.timedelta(days=float(rng.uniform(1.0, 2.6)))
    if delivered <= NOW: st = 'DELIVERED'
    elif shipped <= NOW: st = 'SHIPPING'
    elif ts + dt.timedelta(hours=8) <= NOW: st = 'PREPARING'
    elif (NOW - ts) < dt.timedelta(minutes=90) and rng.random() < 0.3: st = 'PENDING_PAYMENT'
    if st in ('SHIPPING',): delivered = None
    if st in ('PAID', 'PREPARING', 'PENDING_PAYMENT'): shipped = delivered = None
    return st, None, shipped, delivered

REFUND_REASON = ['단순 변심', '주문 실수', '배송지 오류', '상품 품절', '중복 주문', '가격 변동']
PAYM = ['CARD', 'EASY_PAY', 'BANK', 'VIRTUAL_ACCOUNT']; PAYP = [.55, .30, .10, .05]
oid = iid = 0
orders_rows, items_rows, pay_rows, ref_rows, ship_rows = [], [], [], [], []
first_order_done = set()
for o in ORDERS:
    oid += 1; o['id'] = oid
    mi = o['mi']
    sub = 0
    for (pid, vid, qty) in o['items']: sub += variants[vid]['price'] * qty
    discount = 0
    if disc_rate[o['d']] > 0 and rng.random() < 0.75: discount = int(round(sub * disc_rate[o['d']] / 100.0)) * 100
    if mi is not None and mi not in first_order_done and rng.random() < 0.35: discount = min(discount + 3000, int(sub * 0.3 // 100 * 100))
    if mi is not None: first_order_done.add(mi)
    ship = 0 if sub >= 50000 else 3000
    final = sub - discount + ship
    st, cancel_at, shipped, delivered = pick_status_times(o)
    o.update(status=st, shipped=shipped, delivered=delivered, sub=sub, final=final)
    if mi is None:
        name = '가상 비회원'; email = f'guest{oid}@example.com'; phone = '010-0000-0000'; ri = int(rng.choice(len(REG), p=rp))
    else:
        name = '기본 관리자' if mi == 0 else ('기본 테스트 회원' if mi == 1 else ('기본 매니저' if mi == 2 else f'테스트회원{mi + 1}'))
        email = {0: 'admin', 1: 'test001', 2: 'manager'}.get(mi, f'demo{mi + 1}') + '@example.com'; phone = tel[mi]; ri = region[mi]
    z = zipcode(ri)
    orders_rows.append((oid, mi + 1 if mi is not None else None, 'DEMO-' + str(oid).zfill(12), st, sub, discount, ship, final, name, phone, email, z,
                        f'{REG[ri][0]} 테스트구 테스트로 {1 + (oid * 7) % 99}', '가상 주소', o['ts']))
    for (pid, vid, qty) in o['items']:
        iid += 1
        items_rows.append((iid, oid, vid, products[pid]['name'][:50], variants[vid]['label'][:50], qty, variants[vid]['price'], variants[vid]['price'] * qty))
        o.setdefault('item_ids', []).append((iid, pid, vid))
    # 결제
    method = PAYM[int(rng.choice(4, p=PAYP))]
    if st == 'CANCELLED':
        failed = rng.random() < 0.28
        pst = 'FAILED' if failed else 'CANCELLED'
        paid_at = None if failed else o['ts'] + dt.timedelta(seconds=int(rng.integers(20, 240)))
        pay_rows.append((oid, oid, method, 'DEMO_ONLY', None if failed else f'DEMO-NOT-A-PG-KEY-{oid}', f'DEMO-IDEMPOTENCY-{oid}', final, pst, paid_at, o['ts']))
        if not failed:
            rc = cancel_at; done = rc + dt.timedelta(hours=float(rng.uniform(1, 6)))
            pend = done > NOW
            ref_rows.append((oid, f'DEMO-REFUND-{oid}', final, random.choice(REFUND_REASON), 'PENDING' if pend else 'COMPLETED', rc, None if pend else done))
    elif st == 'PENDING_PAYMENT':
        pay_rows.append((oid, oid, method, 'DEMO_ONLY', None, f'DEMO-IDEMPOTENCY-{oid}', final, 'PENDING', None, o['ts']))
    else:
        pay_rows.append((oid, oid, method, 'DEMO_ONLY', f'DEMO-NOT-A-PG-KEY-{oid}', f'DEMO-IDEMPOTENCY-{oid}', final, 'PAID', o['ts'] + dt.timedelta(seconds=int(rng.integers(20, 240))), o['ts']))
    if st in ('SHIPPING', 'DELIVERED'): ship_rows.append((oid, st, shipped, delivered))

# ------------------------------------------------------------------ 6. 리뷰 (본문-평점 일관)
POS = {
 'food': {
  '기호성': ["{pet}가 정말 잘 먹어요.", "기호성이 아주 좋아서 그릇을 싹 비워요.", "처음 먹이는데도 거부감 없이 잘 먹네요.", "편식하는 아이인데 이건 잘 먹어서 놀랐어요."],
  '소화': ["변 상태가 좋아졌어요.", "먹고 나서 배변이 편안해 보여요.", "속이 편한지 잔병치레가 줄었어요."],
  '알갱이/냄새': ["알갱이 크기가 적당해요.", "냄새가 심하지 않아서 좋아요."],
  '포장': ["포장이 꼼꼼해서 안심이 됐어요.", "지퍼백이라 보관하기 편해요."],
  '가격': ["이 가격에 이 정도면 만족해요.", "할인할 때 사서 가성비가 좋았어요."],
  '배송': ["배송이 빨라서 좋았어요.", "주문 다음 날 도착했어요."],
  '모질': ["털에 윤기가 도는 것 같아요.", "꾸준히 먹이니 모질이 좋아졌어요."],
  '양': ["용량이 넉넉해서 오래 먹일 수 있어요."]},
 'supp': {
  '효과': ["꾸준히 먹이니 눈에 띄게 활발해졌어요.", "관절이 좋아진 느낌이에요.", "털 빠짐이 줄었어요."],
  '기호성': ["간식처럼 잘 먹어요.", "알약도 거부 없이 잘 먹어요."],
  '부작용': ["부작용 없이 잘 맞아요."],
  '포장': ["포장이 꼼꼼해서 좋았어요."], '가격': ["꾸준히 먹이기에 부담 없는 가격이에요."], '배송': ["배송이 빨라서 좋았어요."]},
 'device': {
  '디자인': ["디자인이 깔끔하고 집에 잘 어울려요.", "색상이 예뻐요."],
  '소음': ["소음이 거의 없어요.", "조용해서 밤에도 괜찮아요."],
  '세척': ["분리해서 세척하기 편해요.", "물세척이 쉬워요."],
  '내구성': ["재질이 튼튼해 보여요.", "마감이 깔끔해요."],
  '사용성': ["설치가 간단해서 금방 썼어요.", "사용법이 쉬워요."],
  '용량': ["용량이 넉넉해요."], '포장': ["포장이 꼼꼼해서 좋았어요."], '가격': ["가격 대비 만족스러워요."], '배송': ["배송이 빨라서 좋았어요."]}}
NEG = {
 'food': {
  '기호성': ["{pet}가 냄새만 맡고 안 먹어요.", "기호성이 별로인지 잘 안 먹네요.", "먹다가 남기는 양이 많아요.", "기존 사료보다 덜 먹는 것 같아요."],
  '소화': ["먹이고 나서 변이 묽어졌어요.", "살짝 설사 기미가 있어서 양을 줄였어요.", "먹고 나서 구토를 해서 걱정이에요."],
  '알갱이/냄새': ["알갱이가 너무 커서 먹기 힘들어해요.", "냄새가 좀 강해요.", "가루가 많이 남아 있어요."],
  '포장': ["포장이 찢어진 채로 왔어요.", "박스가 눌려서 와서 속상했어요.", "포장이 부실해서 새어 나올 것 같아요."],
  '가격': ["가격이 조금 부담돼요.", "용량 대비 비싼 것 같아요."],
  '배송': ["배송이 너무 늦었어요.", "배송이 지연돼서 사료가 떨어질 뻔했어요.", "택배가 생각보다 오래 걸렸어요."],
  '모질': ["아직 효과는 잘 모르겠어요."], '양': ["생각보다 양이 적어요."]},
 'supp': {
  '효과': ["한 달 먹였는데 효과를 모르겠어요.", "효과가 별로 없는 것 같아요."],
  '기호성': ["냄새 때문에 안 먹으려고 해요.", "억지로 먹여야 해서 힘들어요."],
  '부작용': ["먹이고 나서 설사를 했어요.", "먹고 나서 구토를 해서 중단했어요."],
  '포장': ["포장이 파손된 채로 도착했어요."], '가격': ["가격이 조금 비싸요."], '배송': ["배송이 너무 늦었어요.", "택배가 생각보다 오래 걸렸어요."]},
 'device': {
  '디자인': ["사진과 색이 달라요.", "디자인이 생각보다 투박해요."],
  '소음': ["소음이 커서 {pet}가 놀라요.", "모터 소리가 신경 쓰여요."],
  '세척': ["틈새 세척이 어려워요.", "세척이 번거로워요."],
  '내구성': ["플라스틱 재질이 약해 보여요.", "사용 몇 주 만에 고장 났어요.", "마감이 거칠어요."],
  '사용성': ["설명서가 부실해서 설치가 어려웠어요.", "작동이 번거로워요."],
  '용량': ["용량이 생각보다 작아요."], '포장': ["포장이 파손된 채로 도착했어요.", "박스가 찌그러져 왔어요."], '가격': ["가격이 좀 비싸요."], '배송': ["배송이 너무 늦었어요.", "택배가 생각보다 오래 걸렸어요."]}}
INCIDENT = ["물이 새서 바닥이 다 젖었어요.", "펌프에서 이상한 소리가 나요.", "며칠 쓰다가 작동이 멈췄어요.", "센서 오류로 물이 잘 안 나와요."]
OPEN = {'pos': ["재구매했어요!", "만족스러워요.", "추천합니다.", "배송 잘 받았습니다."], 'neu': ["무난해요.", "그냥 보통이에요.", "써보고 있어요."], 'neg': ["아쉬워요.", "실망했어요.", "기대했는데 별로네요."]}
CLOSE = {'pos': ["앞으로도 계속 이용할게요.", "주변에도 추천하려고요.", "다음에도 구매할 예정이에요.", "만족합니다 ㅎㅎ"], 'neu': ["좀 더 써보고 다시 남길게요.", "나쁘지 않아요."], 'neg': ["재구매는 고민돼요.", "다른 제품을 알아보려고요.", "개선이 되었으면 좋겠어요.", "아쉽네요 ㅠㅠ"]}
SHORT = {'pos': ["좋아요", "만족합니다", "굿", "잘 쓰고 있어요", "재구매 의사 있어요"], 'neu': ["보통이에요", "그냥 그래요", "무난합니다"], 'neg': ["별로예요", "실망이에요", "비추합니다"]}
def make_review(domain, sp, rating, delay_flag, incident):
    pet = '강아지' if sp == 'DOG' else '고양이'
    sent = 'pos' if rating >= 4 else ('neu' if rating == 3 else 'neg')
    if rng.random() < 0.12: return random.choice(SHORT[sent]), sent
    pos_a = list(POS[domain]); neg_a = list(NEG[domain]); sents = []
    def pickp(): a = random.choice(pos_a); pos_a.remove(a); return random.choice(POS[domain][a])
    def pickn(): a = random.choice(neg_a); neg_a.remove(a); return random.choice(NEG[domain][a])
    k = random.choice([1, 2, 2, 3])
    if sent == 'pos':
        sents = [pickp() for _ in range(min(k, len(pos_a)))]
        if rating == 4 and rng.random() < 0.35: sents.append('다만 ' + pickn())
    elif sent == 'neu':
        sents = [pickp(), '그런데 ' + pickn()] if rng.random() < 0.6 else [pickn(), '그래도 ' + pickp()]
    else:
        sents = [pickn() for _ in range(min(random.choice([1, 2]), len(neg_a)))]
        if rng.random() < 0.25 and pos_a: sents.append('그래도 ' + pickp())
    if delay_flag: sents = [random.choice(NEG[domain]['배송'])] + [s for s in sents if not s.startswith('다만')][:1]
    if incident: sents = [random.choice(INCIDENT)] + sents[:1]
    text = ' '.join(sents)
    if rng.random() < 0.4: text = random.choice(OPEN[sent]) + ' ' + text
    if rng.random() < 0.5: text = text + ' ' + random.choice(CLOSE[sent])
    return text.replace('{pet}', pet), sent

member_bias = rng.normal(0, 0.3, M)
all_img = sorted({p for v in images.values() for p in v})
reviews = []
for o in ORDERS:
    if o['status'] != 'DELIVERED' or o['mi'] is None: continue
    late = (o['delivered'] - o['ts']).total_seconds() / 86400 > 3.6
    for (iid_, pid, vid) in o['item_ids']:
        pi = pidx[pid]
        inc = (TAGS[prod_tag[pi]] == 'water' and dt.date(2025, 7, 10) <= o['delivered'].date() <= dt.date(2025, 8, 31))
        s = quality[pi] + member_bias[o['mi']] + rng.normal(0, 0.95) - (1.0 if late else 0) - (1.4 if inc else 0)
        rating = int(min(5, max(1, round(s))))
        p_rev = (0.15 if rating in (3, 4) else (0.30 if rating == 5 else 0.32)) * (1.15 if late else 1.0)
        delay_days = max(0.4, rng.gamma(2.0, 2.5))
        rt = o['delivered'] + dt.timedelta(days=float(delay_days))
        if (rt.year, rt.month) in REVIEW_BOOST_MONTHS: p_rev *= 1.6
        if rng.random() >= p_rev or rt > NOW: continue
        text, sent = make_review(domain_of(pid), o['sp'], rating, late and rng.random() < 0.6, inc and rng.random() < 0.7)
        reviews.append(dict(mi=o['mi'], pid=pid, iid=iid_, rating=rating, text=text, ts=rt, sent={'pos': '긍정', 'neu': '중립', 'neg': '부정'}[sent],
                            photo=rng.random() < (0.14 + (0.25 if (rt.year, rt.month) in REVIEW_BOOST_MONTHS else 0))))
reviews.sort(key=lambda r: r['ts'])
for i, r in enumerate(reviews): r['id'] = i + 1

# ------------------------------------------------------------------ 7. 회원 상태/주소/펫
last_activity = np.array([max(created_day[i], last_order[i]) for i in range(M)])
status = ['ACTIVE'] * M; deleted_at = [None] * M
churned = np.where((death_day < T - 1) & buyer)[0]
for i in churned:
    r = rng.random()
    if r < 0.14:
        dd = int(death_day[i] + rng.integers(1, 120))
        if dd < T - 1: deleted_at[i] = rand_time(dd, True); status[i] = 'DELETED'
    elif r < 0.17: status[i] = 'KICKED'
for i in range(3, M):
    if status[i] == 'ACTIVE' and (T - 1 - last_activity[i]) >= 365 and rng.random() < 0.85: status[i] = 'INACTIVE'
updated_at = []
for i in range(M):
    u = created_at[i]
    if deleted_at[i]: u = deleted_at[i]
    elif status[i] == 'INACTIVE': u = min(NOW, dt.datetime.combine(DAYS[int(last_activity[i])], dt.time(3, 0)) + dt.timedelta(days=365))
    elif rng.random() < 0.3: u = min(NOW, created_at[i] + dt.timedelta(days=float(rng.uniform(1, 200))))
    updated_at.append(max(u, created_at[i]))

# ------------------------------------------------------------------ 8. SQL 출력
def q(v):
    if v is None: return 'NULL'
    if isinstance(v, (bool, np.bool_)): return 'true' if v else 'false'
    if isinstance(v, (int, np.integer)): return str(int(v))
    if isinstance(v, (float, np.floating)): return repr(round(float(v), 2))
    if isinstance(v, dt.datetime): return "'" + v.strftime('%Y-%m-%d %H:%M:%S') + "'"
    if isinstance(v, dt.date): return "'" + v.isoformat() + "'"
    return "'" + str(v).replace("'", "''") + "'"
out = open(A.out, 'w', encoding='utf-8')
def W(s=''): out.write(s + '\n')
def ins(table, cols, rows, batch=400):
    rows = list(rows)
    for i in range(0, len(rows), batch):
        W(f'INSERT INTO {table}({",".join(cols)}) VALUES')
        W(',\n'.join('(' + ','.join(q(c) for c in r) + ')' for r in rows[i:i + batch]) + ';')

W(f'''-- 포캐스트 3년치 개발용 더미데이터 ({START} ~ {END}, {T}일)
-- 기존 pawcast_final_seed.sql의 카탈로그(상품 {P}개/옵션 {len(variants)}개)를 그대로 쓰고, 회원/주문/결제/리뷰/찜/장바구니/공지/문의를 새로 생성합니다.
-- 가입·주문은 요일/월 계절성, 성장 추세, 명절·세일·블랙프라이데이·주년행사 등 이벤트일에 몰리도록 가중했습니다.
-- 빈 개발 DB(pawcast_init.sql로 만든 직후)에서만 실행하세요. 기존 데이터는 삭제하지 않습니다.
-- 가상 회원은 비밀번호가 없어 로그인 불가입니다. admin / test001 / manager 는 공통 개발용 비밀번호 해시가 들어 있습니다.
-- 합성 데이터이므로 이 데이터로 얻은 모델 성능을 실제 서비스 성능으로 해석하지 마세요.
SET client_encoding TO 'UTF8';
BEGIN;
DO $$
BEGIN
 IF EXISTS(SELECT 1 FROM members) OR EXISTS(SELECT 1 FROM products)
    OR EXISTS(SELECT 1 FROM categories) OR EXISTS(SELECT 1 FROM orders) THEN
  RAISE EXCEPTION '빈 개발 DB에서만 실행하세요. 기존 데이터는 삭제하지 않습니다.';
 END IF;
END $$;''')
mrows = []
for i in range(M):
    uid = {0: 'admin', 1: 'test001', 2: 'manager'}.get(i, None if social[i] else f'demo_{i + 1}')
    nm = {0: '기본 관리자', 1: '기본 테스트 회원', 2: '기본 매니저'}.get(i, f'테스트회원{i + 1}')
    em = {0: 'admin', 1: 'test001', 2: 'manager'}.get(i, f'demo{i + 1}') + '@example.com'
    mrows.append((i + 1, uid, nm, em, tel[i], HASH if i < 3 else None, 'ADMIN' if i == 0 else ('MANAGER' if i == 2 else 'USER'), status[i], True, True, True,
                  bool(rng.random() < 0.42), deleted_at[i], created_at[i], updated_at[i]))
ins('members', 'id,user_id,name,email,tel,password_hash,role,status,is_agreed_terms,is_agreed_privacy,is_agreed_efinance,is_agreed_marketing,deleted_at,created_at,updated_at'.split(','), mrows)
prov_rows = [(i + 1, 'google' if rng.random() < 0.55 else 'kakao', str(int(rng.integers(10 ** 17, 10 ** 18))), created_at[i]) for i in range(M) if social[i]]
ins('oauth_accounts', 'member_id,provider,provider_user_id,created_at'.split(','), prov_rows)
arows = []
for i in range(M):
    arows.append((i + 1, '집', mrows[i][2], tel[i], zipcode(region[i]), f'{REG[region[i]][0]} 테스트구 테스트로 {1 + (i * 7) % 99}', '가상 주소', True))
    if rng.random() < 0.15: arows.append((i + 1, '회사', mrows[i][2], tel[i], zipcode(region[i]), f'{REG[region[i]][0]} 테스트구 업무로 {1 + (i * 11) % 99}', '가상 주소', False))
ins('address', 'member_id,label,recipient,tel,zipcode,address1,address2,is_default'.split(','), arows)
BREED = dict(DOG=[('말티즈', 3.5), ('푸들', 5.0), ('포메라니안', 3.0), ('비숑프리제', 6.5), ('웰시코기', 12.0), ('골든리트리버', 29.0), ('시츄', 6.5), ('믹스견', 8.0)],
             CAT=[('코리안숏헤어', 4.3), ('러시안블루', 4.5), ('브리티시숏헤어', 5.2), ('페르시안', 4.8), ('스코티시폴드', 4.4), ('믹스묘', 4.2)])
ALLERGY = ['CHICKEN', 'BEEF', 'GRAIN', 'DAIRY', 'FISH', 'EGG']
prow = []; pid_ = 0
for i in range(3, M) if False else range(1, M):
    if i == 2: continue
    sps = [species1[i]] + ([species2[i]] if species2[i] else [])
    for k, sp in enumerate(sps):
        pid_ += 1
        age = float(rng.uniform(0.3, 14)); bd = (created_at[i] - dt.timedelta(days=age * 365.25)).date()
        br, w = random.choice(BREED[sp]); wt = round(w * float(rng.uniform(0.8, 1.25)), 1)
        al = ','.join(random.sample(ALLERGY, random.choice([1, 1, 2]))) if rng.random() < 0.22 else None
        ca = created_at[i] + dt.timedelta(hours=float(rng.uniform(0.2, 72)))
        prow.append((pid_, i + 1, ('몽이', '초코', '콩이', '보리', '나비', '루나', '두부', '모카', '별이', '해피')[(i + k * 3) % 10], sp, bd, br, wt, al, min(ca, NOW), min(ca, NOW)))
ins('pets', 'id,member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at'.split(','), prow)
W('\n'.join(catalog_lines))
# 상품 출시일/단종
base_ts = lambda d: dt.datetime.combine(DAYS[d], dt.time(int(rng.integers(0, 6)), int(rng.integers(0, 60)), 0)) if d > 0 else dt.datetime.combine(DAYS[0], dt.time(0, 0, 0))
pc = {p: base_ts(int(start_day[pidx[p]])) for p in PID}
for i in range(0, P, 500):
    W('UPDATE products p SET created_at=v.c,updated_at=v.c FROM (VALUES ' + ','.join(f"({p},'{pc[p].strftime('%Y-%m-%d %H:%M:%S')}'::timestamp)" for p in PID[i:i + 500]) + ') v(id,c) WHERE p.id=v.id;')
W('UPDATE product_variants v SET created_at=p.created_at,updated_at=p.created_at FROM products p WHERE p.id=v.product_id;')
W('UPDATE product_images i SET is_main=true,created_at=p.created_at,updated_at=p.created_at FROM products p WHERE p.id=i.product_id;')
ended = [(PID[i], int(end_day[i])) for i in range(P) if end_day[i] < 10 ** 6]
so = [p for p, e in ended if e > T - 60]; dc = [p for p, e in ended if e <= T - 60]
if dc: W(f"UPDATE product_variants SET product_variant_status='DISCONTINUED',stock=0 WHERE product_id IN ({','.join(map(str, dc))});")
if so: W(f"UPDATE product_variants SET product_variant_status='SOLD_OUT',stock=0 WHERE product_id IN ({','.join(map(str, so))});")
ins('orders', 'id,member_id,order_number,status,total_amount,discount_amount,shipping_fee,final_amount,recipient_name,recipient_phone,recipient_email,delivery_zipcode,delivery_address1,delivery_address2,created_at'.split(','), orders_rows)
ins('order_items', 'id,order_id,product_variant_id,product_name,option_label,quantity,unit_price,total_price'.split(','), items_rows, 600)
ins('payments', 'id,order_id,payment_method,provider,payment_key,idempotency_key,amount,status,paid_at,created_at'.split(','), pay_rows, 600)
ins('refunds', 'payment_id,idempotency_key,refund_amount,reason,status,created_at,completed_at'.split(','), ref_rows)
ins('shipments', 'order_id,status,shipped_at,delivered_at'.split(','), ship_rows, 600)
ins('product_reviews', 'id,member_id,product_id,order_item_id,rating,content,created_at,updated_at'.split(','), [(r['id'], r['mi'] + 1, r['pid'], r['iid'], r['rating'], r['text'], r['ts'], r['ts']) for r in reviews], 500)
rimg = []
for r in reviews:
    if r['photo']:
        paths = images.get(r['pid']) or all_img
        for k in range(random.choice([1, 1, 2])): rimg.append((r['id'], random.choice(paths), k + 1))
ins('review_images', 'review_id,image_path,sort_order'.split(','), rimg)

# 찜
wl = []; seen = set()
for i in range(1, M):
    if i == 2 or status[i] in ('DELETED', 'KICKED'): continue
    nw = int(rng.poisson(3.5 if i == 1 else 2.0))
    hi = int(min(death_day[i], T - 1)); lo = int(created_day[i])
    for _ in range(nw):
        wd = int(rng.integers(lo, hi + 1)); sp = species1[i]
        bw = pop * ((start_day <= wd) & (end_day >= wd)) * pet_ok[sp]
        if bw.sum() <= 0: continue
        pid = PID[int(rng.choice(P, p=bw / bw.sum()))]
        if (i, pid) in seen: continue
        seen.add((i, pid)); wts = min(rand_time(wd, True), NOW)
        wl.append((i + 1, pid, max(wts, created_at[i])))
ins('wishlists', 'member_id,product_id,created_at'.split(','), wl)
# 장바구니 (최근 45일 내 담은 ACTIVE 카트)
cart_rows, ci_rows = [], []; cid = 0
for i in range(1, M):
    if i == 2 or status[i] != 'ACTIVE' or death_day[i] <= T - 1 or rng.random() > (1.0 if i == 1 else 0.14): continue
    cid += 1; cd = int(rng.integers(max(int(created_day[i]), T - 45), T)); cts = min(rand_time(cd, True), NOW); cts = max(cts, created_at[i])
    cart_rows.append((cid, i + 1, 'ACTIVE', cts))
    bw = pop * ((start_day <= cd) & (end_day >= cd)) * pet_ok[species1[i]]; used = set()
    for _ in range(int(rng.integers(1, 5))):
        pid = PID[int(rng.choice(P, p=bw / bw.sum()))]; vid = random.choice(pvars[pid])
        if vid in used: continue
        used.add(vid); ci_rows.append((cid, vid, int(rng.choice([1, 1, 2]))))
ins('carts', 'id,member_id,status,created_at'.split(','), cart_rows)
ins('cart_items', 'cart_id,variant_id,quantity'.split(','), ci_rows)
# 공지
nrows = []
for e in EV:
    if not e['notice']: continue
    lead, typ, title, body = e['notice']
    nd = dix(e['s']) - lead if lead >= 0 else dix(e['e']) + 1
    if nd < 0 or nd >= T: continue
    ts = dt.datetime.combine(DAYS[nd], dt.time(10, 0, int(rng.integers(0, 60))))
    if ts > NOW: continue
    nrows.append((typ, title, body, e['s'] <= END <= e['e'] and typ == 'EVENT', ts))
nrows.append(('GENERAL', '배송 및 교환/환불 안내', '배송은 결제 완료 후 1~3일 소요되며, 교환/환불은 수령 후 7일 이내 고객센터로 문의해 주세요.', True, dt.datetime.combine(DAYS[0], dt.time(9, 30, 0))))
nrows.append(('GENERAL', '개인정보 처리방침 개정 안내', '개인정보 처리방침이 일부 개정되었습니다. 자세한 내용은 하단 링크에서 확인해 주세요.', False, dt.datetime.combine(DAYS[min(T - 1, 400)], dt.time(10, 0, 0))))
nrows.sort(key=lambda r: r[4])
ins('notices', 'member_id,notice_type,title,content,is_pinned,created_at,updated_at'.split(','), [(1 if k % 3 else 3, a, b, c, d_, ts, ts) for k, (a, b, c, d_, ts) in enumerate(nrows)])
# QnA
QP = [("원재료 문의", "{p} 원재료에 닭고기가 들어가나요? 알러지가 있어서 문의드려요.", "안녕하세요. 해당 상품의 원재료는 상세 페이지 성분표에서 확인하실 수 있으며, 알러지가 있는 경우 급여 전 수의사와 상담을 권장드립니다."),
      ("재입고 문의", "{p} 옵션 중 품절된 구성이 있어서 재입고 일정이 궁금합니다.", "문의 감사합니다. 재입고 일정은 입고 확정 후 상품 페이지에 안내드리겠습니다."),
      ("급여량 문의", "체중 5kg 아이는 하루에 얼마나 급여하면 되나요?", "포장 뒷면의 급여 가이드를 기준으로 체중과 활동량에 맞춰 조절해 주세요."),
      ("유통기한 문의", "{p} 유통기한이 어떻게 되나요?", "출고 시점 기준 유통기한이 충분히 남은 제품으로 발송해 드립니다."),
      ("사용법 문의", "{p} 처음 사용하는데 세척 방법이 궁금해요.", "분리 가능한 부품은 미온수로 세척하시고, 전원부는 물에 닿지 않게 주의해 주세요.")]
QG = [("배송 문의", "주문한 상품이 아직 도착하지 않았어요. 언제쯤 받을 수 있나요?", "안녕하세요. 주문 내역에서 배송 상태를 확인하실 수 있으며, 지연 시 개별 연락드리겠습니다."),
      ("교환/환불 문의", "상품을 받았는데 교환하고 싶어요. 절차가 어떻게 되나요?", "수령 후 7일 이내 고객센터로 접수해 주시면 교환/환불 절차를 안내드립니다."),
      ("결제 오류 문의", "결제 중 오류가 나서 중복으로 결제된 것 같아요.", "결제 내역 확인 후 중복 결제는 영업일 기준 3일 이내 취소 처리해 드립니다."),
      ("회원정보 변경 문의", "휴대폰 번호를 변경하고 싶은데 어디서 하나요?", "마이페이지 > 회원정보 수정에서 변경하실 수 있습니다."),
      ("쿠폰 문의", "신규 가입 쿠폰이 보이지 않아요.", "쿠폰은 가입 후 마이페이지 쿠폰함에서 확인하실 수 있으며, 이벤트 기간에 따라 상이할 수 있습니다."),
      ("세일 기간 문의", "이번 세일은 언제까지 진행되나요?", "공지사항에 안내된 기간 동안 진행되며, 재고 소진 시 조기 종료될 수 있습니다.")]
qw = np.array([(base_orders[i] / max(1.0, order_mult[i] * 0.0 + 1.0)) * (1 + 0.9 * (delay_d[i] > 0)) for i in range(T)]); qw[T - 1] *= 0.5
qdays = np.sort(rng.choice(T, size=min(900, int(T * 0.8)), p=qw / qw.sum()))
qrows, arows2 = [], []; qid = 0
for qd in qdays:
    cand = np.where((created_day <= qd) & (death_day > qd) & buyer)[0]
    if len(cand) == 0: continue
    mi = int(random.choice(cand)); qid += 1; qts = min(rand_time(int(qd), True), NOW); qts = max(qts, created_at[mi])
    if rng.random() < 0.45:
        bw = pop * ((start_day <= qd) & (end_day >= qd)); pid = PID[int(rng.choice(P, p=bw / bw.sum()))]
        t, c, a = random.choice(QP); typ = 'PRODUCT'; priv = False
        t, c = t, c.replace('{p}', products[pid]['name'])
    else:
        pid = None; typ = 'GENERAL'; t, c, a = random.choice(QG); priv = rng.random() < 0.35
    dele = qts + dt.timedelta(days=float(rng.uniform(3, 60))) if rng.random() < 0.02 else None
    if dele and dele > NOW: dele = None
    qrows.append((qid, mi + 1, pid, typ, t, c, priv, dele, qts, qts))
    if rng.random() < 0.91:
        ats = qts + dt.timedelta(hours=float(min(120, rng.lognormal(1.6, 0.9))))
        if ats <= NOW: arows2.append((qid, 1 if rng.random() < 0.5 else 3, a, ats, ats))
ins('qnas', 'id,member_id,product_id,type,title,content,is_private,deleted_at,created_at,updated_at'.split(','), qrows)
ins('qna_answers', 'qna_id,member_id,content,created_at,updated_at'.split(','), arows2)
W('''-- 집계값은 실제 생성된 상세 레코드에서 계산.
UPDATE product_variants v SET sales_count=coalesce((SELECT sum(oi.quantity) FROM order_items oi JOIN orders o ON o.id=oi.order_id WHERE oi.product_variant_id=v.id AND o.status<>'CANCELLED'),0);
UPDATE products p SET sales_count=coalesce((SELECT sum(v.sales_count) FROM product_variants v WHERE v.product_id=p.id),0),
 wish_count=(SELECT count(*) FROM wishlists w WHERE w.product_id=p.id),
 review_count=(SELECT count(*) FROM product_reviews r WHERE r.product_id=p.id),
 rating_sum=coalesce((SELECT sum(r.rating) FROM product_reviews r WHERE r.product_id=p.id),0);
-- 명시적 ID 적재 이후 identity 시퀀스를 실제 최댓값으로 전진.
DO $$ DECLARE t record; sequence_name text; maximum bigint;
BEGIN
 FOR t IN SELECT table_name FROM information_schema.columns WHERE table_schema='public' AND column_name='id' AND is_identity='YES' LOOP
  sequence_name:=pg_get_serial_sequence(format('public.%I',t.table_name),'id');
  EXECUTE format('SELECT max(id) FROM public.%I',t.table_name) INTO maximum;
  IF maximum IS NOT NULL THEN PERFORM setval(sequence_name,maximum,true); END IF;
 END LOOP;
 IF EXISTS(SELECT 1 FROM orders o WHERE o.total_amount<>(SELECT sum(total_price) FROM order_items WHERE order_id=o.id) OR o.final_amount<>o.total_amount-o.discount_amount+o.shipping_fee) THEN
  RAISE EXCEPTION '주문 금액 검증 실패';
 END IF;
END $$;
COMMIT;
SELECT 'members' AS entity,count(*) FROM members UNION ALL SELECT 'products',count(*) FROM products UNION ALL SELECT 'orders',count(*) FROM orders
UNION ALL SELECT 'order_items',count(*) FROM order_items UNION ALL SELECT 'reviews',count(*) FROM product_reviews UNION ALL SELECT 'qnas',count(*) FROM qnas;''')
W(f'''
-- 적재 후 검수: 다음 결과는 모두 0이어야 합니다.
SELECT 'orders_outside_period' check_name,count(*) violations FROM orders WHERE created_at<DATE '{START}' OR created_at>=DATE '{END + dt.timedelta(days=1)}'
UNION ALL SELECT 'review_before_delivery_or_future',count(*) FROM product_reviews r JOIN order_items oi ON oi.id=r.order_item_id JOIN orders o ON o.id=oi.order_id
 LEFT JOIN shipments s ON s.order_id=o.id WHERE s.delivered_at IS NULL OR r.created_at<s.delivered_at OR r.created_at>TIMESTAMP '{A.now}'
UNION ALL SELECT 'order_before_member_signup',count(*) FROM orders o JOIN members m ON m.id=o.member_id WHERE o.created_at<m.created_at
UNION ALL SELECT 'order_after_member_deleted',count(*) FROM orders o JOIN members m ON m.id=o.member_id WHERE m.deleted_at IS NOT NULL AND o.created_at>m.deleted_at;
-- 월별 요약
SELECT date_trunc('month',created_at)::date AS month,count(*) orders,sum(final_amount) FILTER(WHERE status<>'CANCELLED') net_sales FROM orders GROUP BY 1 ORDER BY 1;''')
out.close()

# 리뷰 감성 라벨(선택)
with open(A.sent_out, 'w', encoding='utf-8') as f:
    f.write("-- 선택 실행: 리뷰 본문 생성 시 의도한 감성 라벨(평점 5·4=긍정, 3=중립, 2·1=부정)을 review_sentiments에 넣습니다.\n"
            "-- AI 감성 분석 결과를 직접 채울 계획이면 실행하지 말고, 평가용 정답으로만 쓰려면 model_name='SEED_RULE_LABEL' 행을 비교 기준으로 사용하세요.\n"
            "-- 삭제: DELETE FROM review_sentiments WHERE model_name='SEED_RULE_LABEL';\nSET client_encoding TO 'UTF8';\n")
    rows = [(r['id'], r['sent'], round(float(rng.uniform(0.78, 0.99)), 2), 'SEED_RULE_LABEL', r['ts'] + dt.timedelta(minutes=int(rng.integers(1, 30)))) for r in reviews]
    for i in range(0, len(rows), 500):
        f.write('INSERT INTO review_sentiments(review_id,sentiment,confidence,model_name,analyzed_at) VALUES\n')
        f.write(',\n'.join('(' + ','.join(q(c) for c in r) + ')' for r in rows[i:i + 500]) + ';\n')

# 통계
mon = {}
for o in ORDERS:
    k = o['ts'].strftime('%Y-%m'); m_ = mon.setdefault(k, [0, 0]); m_[0] += 1
    if o['status'] != 'CANCELLED': m_[1] += o['final']
daily = {}
for o in ORDERS: daily[o['ts'].date().isoformat()] = daily.get(o['ts'].date().isoformat(), 0) + 1
top = sorted(daily.items(), key=lambda x: -x[1])[:12]
from collections import Counter
st = dict(period=[str(START), str(END)], days=T, members=M, orders=len(ORDERS), items=len(items_rows), reviews=len(reviews), wishlists=len(wl), carts=len(cart_rows),
          qnas=len(qrows), notices=len(nrows), refunds=len(ref_rows), shipments=len(ship_rows), monthly=mon, top_days=top,
          rating=dict(Counter(r['rating'] for r in reviews)), sentiment=dict(Counter(r['sent'] for r in reviews)),
          member_status=dict(Counter(status)), signup_top=[(str(DAYS[d]), int(c)) for d, c in Counter(created_day.tolist()).most_common(8)],
          order_status=dict(Counter(o['status'] for o in ORDERS)), guest_orders=sum(1 for o in ORDERS if o['guest']))
json.dump(st, open(A.stats_out, 'w'), ensure_ascii=False, indent=1, default=str)
print(json.dumps({k: v for k, v in st.items() if k not in ('monthly',)}, ensure_ascii=False, default=str))
