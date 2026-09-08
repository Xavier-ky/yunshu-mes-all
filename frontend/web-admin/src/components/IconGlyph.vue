<template>
  <component :is="iconComponent" class="icon-glyph" aria-hidden="true" :size="18" />
</template>

<script setup>
import { computed } from "vue";
import {
  Bell,
  Bot,
  Boxes,
  ChartColumnBig,
  ClipboardList,
  Download,
  Factory,
  Gauge,
  GitBranch,
  LayoutDashboard,
  LogIn,
  PackageCheck,
  PackageSearch,
  Plus,
  RadioTower,
  RefreshCw,
  ScanBarcode,
  Search,
  Settings,
  ShieldCheck,
  Truck,
  Wrench,
} from "lucide-vue-next";

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
});

const iconMap = {
  workbench: LayoutDashboard,
  system: Settings,
  factory: Factory,
  "master-data": Boxes,
  process: GitBranch,
  barcode: ScanBarcode,
  planning: ClipboardList,
  inventory: Truck,
  production: Gauge,
  quality: ShieldCheck,
  andon: RadioTower,
  equipment: Wrench,
  traceability: PackageSearch,
  reporting: ChartColumnBig,
  integration: PackageCheck,
  agent: Bot,
  plus: Plus,
  refresh: RefreshCw,
  download: Download,
  search: Search,
  bell: Bell,
  login: LogIn,
  chart: ChartColumnBig,
};

const iconComponent = computed(() => iconMap[props.name] || LayoutDashboard);
</script>
