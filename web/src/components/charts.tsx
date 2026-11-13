import { LIFE_ENMO_THRESHOLD_MG, TIME_ZONE, type Epoch } from "@/lib/activity";

/**
 * グラフはアプリと同じ考え方で、外部のライブラリを使わず SVG で描く。
 * 「家のこと」は色だけでなく斜線の模様でも区別する（色の見分けにくい人のため）。
 */

/** 積み上げ棒の1本。 */
export type Bar = {
  label: string;
  sub?: string;
  walk: number;
  life: number;
  /** 目標をたっせいした日なら true（棒の上に ★）。 */
  achieved?: boolean;
};

const CHART_HEIGHT = 180;

/** 歩いた（藍の塗り）と家のこと（若竹の斜線）を積み上げた棒グラフ。 */
export function StackedBarChart({ bars, goal }: { bars: Bar[]; goal?: number }) {
  const max = Math.max(1, goal ?? 0, ...bars.map((b) => b.walk + b.life));
  const width = 100 / bars.length;
  const goalY = goal ? CHART_HEIGHT - (goal / max) * CHART_HEIGHT : null;
  return (
    <figure className="w-full">
      <svg viewBox={`0 0 100 ${CHART_HEIGHT + 24}`} preserveAspectRatio="none" className="h-56 w-full" role="img"
        aria-label={bars.map((b) => `${b.label} 歩行${b.walk}pt 生活活動${b.life}pt`).join("、")}>
        <defs>
          <pattern id="hatch" width="3" height="3" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
            <rect width="3" height="3" className="fill-green-soft" />
            <line x1="0" y1="0" x2="0" y2="3" className="stroke-green" strokeWidth="1.4" />
          </pattern>
        </defs>
        {bars.map((bar, i) => {
          const x = i * width + width * 0.18;
          const w = width * 0.64;
          const walkH = (bar.walk / max) * CHART_HEIGHT;
          const lifeH = (bar.life / max) * CHART_HEIGHT;
          return (
            <g key={bar.label}>
              <rect x={x} y={CHART_HEIGHT - walkH} width={w} height={walkH} className="fill-primary" rx="0.8" />
              <rect x={x} y={CHART_HEIGHT - walkH - lifeH} width={w} height={lifeH} fill="url(#hatch)" rx="0.8" />
            </g>
          );
        })}
        {goalY !== null && (
          <line x1="0" x2="100" y1={goalY} y2={goalY} className="stroke-vermilion" strokeWidth="0.6"
            strokeDasharray="2 1.5" vectorEffect="non-scaling-stroke" />
        )}
        <line x1="0" x2="100" y1={CHART_HEIGHT} y2={CHART_HEIGHT} className="stroke-muted" strokeWidth="0.4" />
      </svg>
      {/* 文字は SVG の外に出す（SVG を横に伸ばしても文字がつぶれないように）。 */}
      <div className="-mt-6 grid text-center text-xs" style={{ gridTemplateColumns: `repeat(${bars.length}, 1fr)` }}>
        {bars.map((bar) => (
          <div key={bar.label}>
            <div className="font-bold">
              {bar.achieved && <span className="text-vermilion">★ </span>}
              {bar.label}
            </div>
            <div className="text-muted">{bar.walk + bar.life}pt</div>
            {bar.sub && <div className="text-muted">{bar.sub}</div>}
          </div>
        ))}
      </div>
      <figcaption className="mt-3 flex flex-wrap gap-4 text-xs text-muted">
        <Legend swatch={<rect width="14" height="14" className="fill-primary" rx="3" />} text="歩行" />
        <Legend
          swatch={
            <>
              <defs>
                <pattern id="hatch-legend" width="4" height="4" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
                  <rect width="4" height="4" className="fill-green-soft" />
                  <line x1="0" y1="0" x2="0" y2="4" className="stroke-green" strokeWidth="1.6" />
                </pattern>
              </defs>
              <rect width="14" height="14" fill="url(#hatch-legend)" rx="3" />
            </>
          }
          text="生活活動（斜線）"
        />
        {goal && (
          <Legend
            swatch={<line x1="0" x2="14" y1="7" y2="7" className="stroke-vermilion" strokeWidth="2" strokeDasharray="3 2" />}
            text={`目標 ${goal}pt`}
          />
        )}
      </figcaption>
    </figure>
  );
}

function Legend({ swatch, text }: { swatch: React.ReactNode; text: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <svg width="14" height="14" aria-hidden>
        {swatch}
      </svg>
      {text}
    </span>
  );
}

/**
 * 10 秒ごとの揺れ（ENMO）のグラフ。横は 0〜24 時、縦は揺れの強さ（mg）。
 *
 * 歩いた区切りは藍、家のことの区切りは若竹、どちらでもない区切りは灰色の点。
 * 「家のこと」のしきい値に朱色の線を引く。しきい値をいくつにするか決めるのに使う。
 */
export function EnmoChart({ epochs }: { epochs: Epoch[] }) {
  if (epochs.length === 0) {
    return <p className="py-10 text-center text-sm text-muted">この日の 10 秒ごとの記録はありません（未受信、または 30 日経過で削除済み）</p>;
  }
  const W = 960;
  const H = 220;
  // 揺れの大きな値に引っぱられて小さな値が見えなくならないよう、上限を決めてそれ以上は上の端に描く。
  const yMax = Math.max(LIFE_ENMO_THRESHOLD_MG * 4, Math.min(400, Math.max(...epochs.map((e) => e.enmo_mg))));
  const hourOf = (iso: string) => {
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone: TIME_ZONE, hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false,
    }).formatToParts(new Date(iso));
    const get = (t: string) => Number(parts.find((p) => p.type === t)?.value ?? 0);
    return get("hour") + get("minute") / 60 + get("second") / 3600;
  };
  const y = (v: number) => H - (Math.min(v, yMax) / yMax) * H;
  const thresholdY = y(LIFE_ENMO_THRESHOLD_MG);
  return (
    <figure className="w-full">
      <svg viewBox={`0 0 ${W} ${H + 20}`} className="w-full" role="img"
        aria-label={`10秒ごとの揺れのグラフ。${epochs.length}件。しきい値 ${LIFE_ENMO_THRESHOLD_MG}mg`}>
        {[0, 6, 12, 18, 24].map((h) => (
          <g key={h}>
            <line x1={(h / 24) * W} x2={(h / 24) * W} y1="0" y2={H} className="stroke-border" strokeWidth="1" />
            <text x={(h / 24) * W} y={H + 16} textAnchor={h === 0 ? "start" : h === 24 ? "end" : "middle"}
              className="fill-muted text-[12px]">
              {h}時
            </text>
          </g>
        ))}
        {epochs.map((e) => (
          <circle key={e.recorded_at} cx={(hourOf(e.recorded_at) / 24) * W} cy={y(e.enmo_mg)} r="1.8"
            className={e.walking ? "fill-primary" : e.life_seconds > 0 ? "fill-green" : "fill-muted"}
            opacity={e.walking || e.life_seconds > 0 ? 0.9 : 0.4} />
        ))}
        <line x1="0" x2={W} y1={thresholdY} y2={thresholdY} className="stroke-vermilion" strokeWidth="1.5" strokeDasharray="6 4" />
        <text x={W - 4} y={thresholdY - 4} textAnchor="end" className="fill-vermilion text-[12px] font-bold">
          しきい値 {LIFE_ENMO_THRESHOLD_MG}mg
        </text>
      </svg>
      <figcaption className="mt-2 flex flex-wrap gap-4 text-xs text-muted">
        <span>● <span className="text-primary">藍</span> = 歩行</span>
        <span>● <span className="text-green">緑</span> = 生活活動</span>
        <span>● 灰 = 静止</span>
        <span>縦軸上端 = {Math.round(yMax)}mg 以上</span>
      </figcaption>
    </figure>
  );
}
