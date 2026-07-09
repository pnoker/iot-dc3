<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <dashboard-card
    :empty="!loading && !hasData"
    :empty-text="$t('home.topology.empty')"
    :height="480"
    :loading="loading"
    :title="$t('home.topology.title')"
    body-mode="chart"
    class="topology-sankey"
    @refresh="load"
  >
    <template #tools>
      <!-- Mode switch: structure vs data volume. The RangeSegmented on
           the right is only meaningful for volume mode, so we hide it
           when the user is in cardinality — keeps the header tidy and
           makes the mutually-exclusive shape of the two toggles visible. -->
      <el-segmented v-model="mode" :options="modeOptions" size="small"/>
      <range-segmented v-if="mode === 'volume'" v-model="rangeKey" size="small"/>
    </template>

    <div ref="chartRef" class="topology-sankey__canvas"></div>

    <template #footer>
      <span>{{
          $t('home.topology.summary', {
            driver: stats.driverCount,
            device: stats.deviceCount,
            profile: stats.profileCount,
            point: stats.pointCount,
          })
        }}</span>
      <span class="topology-sankey__footer-right">
        <span v-if="mode === 'volume' && stats.rangeLabel" class="topology-sankey__range">
          {{ $t('home.topology.volumeWindow', {range: stats.rangeLabel}) }}
        </span>
        <span v-if="updatedLabel">{{ $t('home.liveFeed.updatedAt', {time: updatedLabel}) }}</span>
        <span v-else>-</span>
      </span>
    </template>
  </dashboard-card>

  <!-- Drill-in dialog for collapsed (Others) buckets. The dialog lives at
       the card root (not inside DashboardCard's body) so it escapes the
       scoped flex sizing and overlays cleanly. -->
  <el-dialog v-model="othersDialog.visible" :title="othersDialog.title" append-to-body width="560px">
    <el-table :data="othersDialog.children" height="420" size="small">
      <!-- @vue-generic {TopologyHiddenChild} -->
      <el-table-column :label="$t('home.topology.colType')" prop="type" width="110">
        <template #default="{row}">
          <el-tag :type="tagTypeFor(row.type)" size="small">{{ layerLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('home.topology.colName')" prop="name" show-overflow-tooltip/>
      <!-- @vue-generic {TopologyHiddenChild} -->
      <el-table-column :label="$t('common.operation')" width="110">
        <template #default="{row}">
          <el-button link size="small" type="primary" @click="onChildJump(row)">
            {{ $t('common.detail') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';
import {Chart} from '@antv/g2';

import {topology} from '@/api/dashboard';
import type {
  TopologyHiddenChild,
  TopologyLink,
  TopologyMode,
  TopologyNode,
  TopologyResponse,
  TopologyStats,
} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

const {t} = useI18n();
const router = useRouter();

const loading = ref(false);
const chartRef = ref<HTMLElement>();
const data = ref<TopologyResponse | null>(null);
const lastRefreshed = ref<string>('');

// Cardinality = "who is wired to what" (structural count).
// Volume = "how much data flowed" (point_value samples over rangeKey).
// Default cardinality so the card populates instantly on first render
// even on tenants whose history table is empty.
const mode = ref<TopologyMode>('cardinality');
const rangeKey = ref<RangeKey>('7d');

const modeOptions = computed(() => [
  {label: t('home.topology.modeCardinality'), value: 'cardinality' as const},
  {label: t('home.topology.modeVolume'), value: 'volume' as const},
]);
const othersDialog = reactive<{ visible: boolean; title: string; children: TopologyHiddenChild[] }>({
  visible: false,
  title: '',
  children: [],
});
let chart: Chart | undefined;

const hasData = computed(() => (data.value?.nodes.length ?? 0) > 0);
const stats = computed<TopologyStats>(
  () => data.value?.stats ?? {driverCount: 0, deviceCount: 0, profileCount: 0, pointCount: 0}
);
const updatedLabel = computed(() =>
  lastRefreshed.value ? new Date(lastRefreshed.value).toLocaleTimeString('zh-CN', {hour12: false}) : ''
);

// Per-layer colour matching the LiveDataFeed driver/device/point palette
// and adding orange for profile — chosen to be visually distinct from the
// four other dashboard tones (purple/blue/green/red) already on the page.
const colourFor = (type: string): string => {
  switch (type) {
    case 'driver':
      return '#9059f6';
    case 'device':
      return '#409eff';
    case 'profile':
      return '#e6a23c';
    case 'point':
      return '#67c23a';
    default:
      return '#c0c4cc';
  }
};

const layerLabel = (type: string): string => {
  switch (type) {
    case 'driver':
      return t('home.topology.layerDriver');
    case 'device':
      return t('home.topology.layerDevice');
    case 'profile':
      return t('home.topology.layerProfile');
    case 'point':
      return t('home.topology.layerPoint');
    default:
      return type;
  }
};

const tagTypeFor = (type: string): 'primary' | 'success' | 'warning' | 'info' => {
  if (type === 'driver') return 'info';
  if (type === 'device') return 'primary';
  if (type === 'point') return 'success';
  return 'warning';
};

// ---- G2 render ---------------------------------------------------------

// Derive the type from a prefixed id (e.g. "driver:42" → "driver",
// "others:device:7" → "others"). G2's sankey layout sometimes reshapes
// the node object and drops custom fields like `.type`, so we always
// recover type from the id — that part is guaranteed to survive because
// it's what `nodeId` returns and what links reference.
const typeFromKey = (key: string | undefined | null): string => {
  if (!key) return 'unknown';
  if (key.startsWith('others:')) return 'others';
  const idx = key.indexOf(':');
  return idx < 0 ? key : key.substring(0, idx);
};

// Cache of id → display name, rebuilt every render. Labels and tooltips
// pull from this rather than relying on `.name` surviving G2's internal
// node re-shape.
const nameById = new Map<string, string>();

const render = (payload: TopologyResponse) => {
  const el = chartRef.value;
  if (!el) return;

  nameById.clear();
  for (const n of payload.nodes) nameById.set(n.id, n.name);

  // G2 v5 sankey uses `d.key` as the default label text. If we leave
  // every node keyed by our prefixed id, the diagram prints
  // "driver:42 / device:101" which is garbage. Add a `key` alias equal
  // to the display name so the default label renders the human name,
  // while keeping `id` as the unique match token for link source/target.
  const nodes = payload.nodes.map((n) => ({...n, key: n.name}));

  chart?.destroy();
  chart = new Chart({container: el, autoFit: true});

  chart.options({
    type: 'sankey',
    data: {
      value: {nodes, links: payload.links},
      transform: [
        {
          type: 'custom',
          callback: (d: { nodes: (TopologyNode & { key: string })[]; links: TopologyLink[] }) => ({
            nodes: d.nodes,
            links: d.links,
          }),
        },
      ],
    },
    layout: {
      // nodeId resolves link.source / link.target against the node id.
      nodeId: (d: TopologyNode) => d.id,
      nodeAlign: 'justify',
      nodePadding: 0.02,
      iterations: 30,
    },
    encode: {
      // Derive colour from the id prefix (defensive — see typeFromKey).
      color: (d: { id?: string; key?: string; type?: string }) => colourFor(d.type ?? typeFromKey(d.id ?? d.key)),
    },
    style: {
      labelSpacing: 4,
      labelFontSize: 11,
      labelFontWeight: 500,
      // Kill the node border entirely — G2's default thin stroke turned
      // into a scratchy outline around every coloured tile, so the
      // Sankey read as "boxed" instead of "flowing". The fill colour is
      // already enough to delineate each node from its neighbours.
      nodeStrokeWidth: 0,
      nodeStroke: 'transparent',
      // Link colour follows the source node's layer so each band of the
      // Sankey reads as "flowing out of" its driver/device/profile. Uses
      // the same palette as the nodes, rendered at low opacity so the
      // stripes layer gracefully without overpowering the node tiles.
      linkFill: (d: {
        source?: { id?: string; key?: string }
      }) => colourFor(typeFromKey(d.source?.id ?? d.source?.key)),
      linkFillOpacity: 0.25,
    },
    tooltip: {
      nodeItems: [
        (d: { id?: string; key?: string; type?: string; name?: string }) => {
          const type = d.type ?? typeFromKey(d.id ?? d.key);
          const label = nameById.get(d.id ?? '') ?? d.name ?? d.key ?? '-';
          return {name: layerLabel(type), value: label};
        },
      ],
      linkItems: [
        (d: {
          source: { id?: string; key?: string; name?: string };
          target: { id?: string; key?: string; name?: string };
          value: number;
        }) => {
          const from = nameById.get(d.source.id ?? '') ?? d.source.name ?? d.source.key ?? '-';
          const to = nameById.get(d.target.id ?? '') ?? d.target.name ?? d.target.key ?? '-';
          return {name: `${from} → ${to}`, value: d.value};
        },
      ],
    },
  });

  // element:click fires for both nodes and links. Nodes are recognised
  // by the presence of an `id` field; links carry `source` / `target`.
  chart.on(
    'element:click',
    (e: { data?: { data?: { id?: string; type?: string; hiddenChildren?: TopologyHiddenChild[] } } }) => {
      const raw = e?.data?.data;
      if (!raw || !raw.id) return;
      const type = raw.type ?? typeFromKey(raw.id);
      onNodeClick({
        id: raw.id,
        name: nameById.get(raw.id) ?? raw.id,
        layer: 1,
        type: type as TopologyNode['type'],
        hiddenChildren: raw.hiddenChildren,
      });
    }
  );

  chart.render();
};

// ---- loader ------------------------------------------------------------

const load = async () => {
  loading.value = true;
  try {
    // Only send rangeKey when it's actually meaningful (volume mode).
    // Cardinality doesn't look at it, and omitting keeps the server
    // cache key tight (one entry per tenant instead of one per range).
    const params = mode.value === 'volume' ? {mode: mode.value, rangeKey: rangeKey.value} : {mode: mode.value};
    const res: { data?: TopologyResponse } = await topology(params);
    const payload = res?.data ?? {
      nodes: [],
      links: [],
      stats: {driverCount: 0, deviceCount: 0, profileCount: 0, pointCount: 0},
    };
    data.value = payload;
    lastRefreshed.value = new Date().toISOString();
    if (payload.nodes.length > 0) {
      // Wait a tick so the card body has its final box before G2 measures.
      await new Promise((r) => setTimeout(r, 0));
      render(payload);
    }
  } catch {
    // handled globally
  } finally {
    loading.value = false;
  }
};

// Flipping mode or rangeKey triggers a reload. rangeKey only matters
// when mode=volume, but we watch it regardless — switching modes first
// then nudging the range should not need a manual refresh click.
watch([mode, rangeKey], () => {
  load();
});

// ---- routing -----------------------------------------------------------

const idSuffix = (prefixedId: string): string => {
  const idx = prefixedId.lastIndexOf(':');
  return idx < 0 ? prefixedId : prefixedId.substring(idx + 1);
};

const routeTo = (type: string, id: string) => {
  if (type === 'driver') {
    router.push({name: 'driverDetail', query: {id, active: 'detail'}}).catch(() => {
    });
  } else if (type === 'device') {
    router.push({name: 'deviceDetail', query: {id, active: 'detail'}}).catch(() => {
    });
  } else if (type === 'profile') {
    router.push({name: 'profileDetail', query: {id, active: 'detail'}}).catch(() => {
    });
  } else if (type === 'point') {
    router.push({name: 'pointValue', query: {pointId: id}}).catch(() => {
    });
  }
};

const onNodeClick = (node: TopologyNode) => {
  if (node.type === 'others') {
    // The node id is "others:{layer}:{parentId}" — infer what kind of
    // entities are collapsed so the dialog title reads naturally.
    const inferred = node.hiddenChildren?.[0]?.type ?? 'point';
    othersDialog.title = t('home.topology.othersDialogTitle', {type: layerLabel(inferred)});
    othersDialog.children = node.hiddenChildren ?? [];
    othersDialog.visible = true;
    return;
  }
  routeTo(node.type, idSuffix(node.id));
};

const onChildJump = (child: TopologyHiddenChild) => {
  othersDialog.visible = false;
  routeTo(child.type, idSuffix(child.id));
};

onMounted(load);
onUnmounted(() => chart?.destroy());
</script>

<style lang="scss" scoped>
.topology-sankey {
  // G2 sankey paints labels outside each node's rectangle and can extend
  // past the canvas's nominal width when the rightmost column has long
  // names. Without overflow:hidden that extra width cascades up to the
  // page and forces a horizontal scrollbar (and visually pushes the
  // RangeSegmented buttons on the other Home cards off-screen).
  // `contain: paint` pins the sankey into its own paint area so nothing
  // leaks out, and `overflow: hidden` is the belt-and-braces fallback.
  :deep(.dashboard-card__content) {
    overflow: hidden;
  }

  .topology-sankey__canvas {
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  // Footer right cluster — "Volume: 7d" chip + "Updated at ..." time.
  // Grouped into a flex span so the three-item footer (counts left,
  // chip+time right) still obeys DashboardCard's space-between layout.
  .topology-sankey__footer-right {
    display: inline-flex;
    align-items: center;
    gap: 10px;
  }

  .topology-sankey__range {
    display: inline-flex;
    align-items: center;
    padding: 1px 8px;
    border-radius: 10px;
    background: rgba(230, 162, 60, 0.12); // profile-column orange tint
    color: #e6a23c;
    font-weight: 500;
    font-size: 11px;
  }
}
</style>
