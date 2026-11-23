# -*- coding: utf-8 -*-
"""기준선(baseline) 검증: 이 CSV로 학습/검증 파이프라인이 돌아가는지, 대략 어느 수준이 나오는지 확인.
사용: python validate_baselines.py ai_dataset
"""
import sys, json, numpy as np, pandas as pd, warnings; warnings.filterwarnings('ignore')
from sklearn.linear_model import Ridge, LogisticRegression
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import f1_score, accuracy_score, roc_auc_score, confusion_matrix
from sklearn.ensemble import GradientBoostingClassifier
P = sys.argv[1] if len(sys.argv) > 1 else 'ai_dataset'
rd = lambda n: pd.read_csv(f'{P}/{n}', encoding='utf-8-sig')
R = {}
# ---- 1) 매출 예측
s = rd('ai_sales_daily.csv'); s['date'] = pd.to_datetime(s['date']); s = s.sort_values('date').reset_index(drop=True)
def feats(df, t0):
    X = pd.DataFrame(index=df.index)
    X['t'] = (df['date'] - t0).dt.days / 1000.0; X['t2'] = X['t'] ** 2
    for k in range(7): X[f'dow{k}'] = (df['dow'] == k).astype(int)
    for k in range(1, 13): X[f'm{k}'] = (df['month'] == k).astype(int)
    X['payday'] = df['is_payday_window']
    for g in ['blackfriday', 'anniversary', 'd1111', 'xmas', 'newyear', 'family_month', 'holiday_pre', 'holiday', 'holiday_post', 'summer', 'photo_review', 'open']:
        X['ev_' + g] = df['event_groups'].fillna('').str.contains(g).astype(int)
    return X
t0 = s['date'].min(); X = feats(s, t0); y = np.log1p(s['net_sales'].values)
tr = (s['split'] == 'train').values
m = Ridge(alpha=1.0).fit(X[tr], y[tr]); pred = np.expm1(m.predict(X))
def snaive(i):   # 같은 요일 4주 평균(직전 데이터가 있어야 하므로 train 마지막 28일을 반복 — 장기 예측의 단순 기준선)
    last = s[tr].tail(28)['net_sales'].values.reshape(4, 7).mean(0); return last[s.loc[i, 'dow'] - (s[tr].tail(28).iloc[0]['dow'])] if False else None
last28 = s[tr].tail(28); dow_mean = last28.groupby('dow').net_sales.mean()
naive = s['dow'].map(dow_mean).values
def met(a, p): a = np.asarray(a, float); p = np.asarray(p, float); return dict(MAE=round(float(np.abs(a - p).mean())), WAPE=round(float(np.abs(a - p).sum() / a.sum()), 3), sMAPE=round(float(np.mean(2 * np.abs(a - p) / (np.abs(a) + np.abs(p) + 1e-9))), 3))
for sp in ['valid', 'test']:
    k = (s['split'] == sp).values
    R[f'sales_{sp}'] = dict(ridge_calendar_event=met(s.loc[k, 'net_sales'], pred[k]), naive_last4w_dow=met(s.loc[k, 'net_sales'], naive[k]), n_days=int(k.sum()))
# 이벤트일만
ev = (s['is_event'] == 1).values; te = (s['split'] == 'test').values & ev
R['sales_test_event_days'] = dict(ridge=met(s.loc[te, 'net_sales'], pred[te]), naive=met(s.loc[te, 'net_sales'], naive[te]), n_days=int(te.sum()))
# ---- 2) 리뷰 감성
d = rd('ai_review_sentiment.csv')
v = TfidfVectorizer(analyzer='char_wb', ngram_range=(1, 3), min_df=2, sublinear_tf=True)
tr_ = d[d.split == 'train']; Xtr = v.fit_transform(tr_.content); clf = LogisticRegression(max_iter=300, C=3.0).fit(Xtr, tr_.label)
for sp in ['valid', 'test', 'ood_test']:
    k = d[d.split == sp]; p = clf.predict(v.transform(k.content))
    R[f'sentiment_{sp}'] = dict(n=len(k), acc=round(accuracy_score(k.label, p), 3), macro_f1=round(f1_score(k.label, p, average='macro'), 3))
# 평점만으로 맞추는 기준선(본문 무시)
rate_lab = d.rating.map(lambda r: '긍정' if r >= 4 else ('중립' if r == 3 else '부정'))
k = d.split.isin(['test', 'ood_test']); R['sentiment_rating_rule_on_test+ood'] = dict(acc=round(accuracy_score(d[k].label, rate_lab[k]), 3), macro_f1=round(f1_score(d[k].label, rate_lab[k], average='macro'), 3))
# 평점-본문 불일치 건에 대한 성능
mm = d[(d.split.isin(['test', 'ood_test'])) & (d.rating_text_mismatch == 1)]; R['sentiment_mismatch_rows'] = dict(n=len(mm), text_model_acc=round(accuracy_score(mm.label, clf.predict(v.transform(mm.content))), 3), rating_rule_acc=round(accuracy_score(mm.label, mm.rating.map(lambda r: '긍정' if r >= 4 else ('중립' if r == 3 else '부정'))), 3))
# ---- 3) 재구매
r = rd('ai_member_repurchase.csv'); f = [c for c in r.columns if c not in ('member_id', 'snapshot_date', 'split', 'target_repurchase_90d', 'region', 'primary_species')]
r['sp_dog'] = (r.primary_species == 'DOG').astype(int); f += ['sp_dog']
g = GradientBoostingClassifier(n_estimators=150, max_depth=3, random_state=0).fit(r[r.split == 'train'][f], r[r.split == 'train'].target_repurchase_90d)
for sp in ['valid', 'test']:
    k = r[r.split == sp]; R[f'repurchase_{sp}'] = dict(n=len(k), positive_rate=round(float(k.target_repurchase_90d.mean()), 3), auc=round(roc_auc_score(k.target_repurchase_90d, g.predict_proba(k[f])[:, 1]), 3),
                                                      auc_recency_only=round(roc_auc_score(k.target_repurchase_90d, -k.recency_days.where(k.recency_days >= 0, 9999)), 3))
# ---- 4) 추천 (구매 이력, 인기 vs 공출현)
it = rd('ai_interactions.csv'); it = it[it.event == 'purchase']
trn = it[it.split == 'train']; tst = it[it.split == 'test']
pop = trn.product_id.value_counts(); popl = list(pop.index[:200])
from collections import defaultdict
co = defaultdict(lambda: defaultdict(int)); ub = trn.groupby('member_id').product_id.apply(lambda x: list(dict.fromkeys(x)))
for ps in ub:
    for a in ps:
        for b in ps:
            if a != b: co[a][b] += 1
hit_p = hit_c = n = 0
for mid, grp in tst.groupby('member_id'):
    if mid not in ub.index: continue
    seen = set(ub[mid]); target = set(grp.product_id) - seen
    if not target: continue
    n += 1; hit_p += bool(target & set([x for x in popl if x not in seen][:10]))
    sc = defaultdict(int)
    for a in seen:
        for b, c in co[a].items():
            if b not in seen: sc[b] += c
    top = [b for b, _ in sorted(sc.items(), key=lambda x: -x[1])[:10]]
    for x in popl:
        if len(top) >= 10: break
        if x not in seen and x not in top: top.append(x)
    hit_c += bool(target & set(top))
R['recommend_test'] = dict(users_evaluated=n, hit10_popularity=round(hit_p / max(n, 1), 3), hit10_cooccurrence=round(hit_c / max(n, 1), 3))
json.dump(R, open(f'{P}/BASELINE_RESULTS.json', 'w'), ensure_ascii=False, indent=1)
print(json.dumps(R, ensure_ascii=False, indent=1))
