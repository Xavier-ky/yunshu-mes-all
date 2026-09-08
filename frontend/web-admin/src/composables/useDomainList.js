import { onMounted, ref } from "vue";

/**
 * 通用领域列表加载：优先调用后端 API，失败或空时回退到骨架数据。
 * @param {Object} options
 * @param {Function} options.apiFn 调用后端列表接口的函数
 * @param {string[]} options.apiColumns API 数据对应的表头列
 * @param {Function} options.rowMapper 将单条 API 数据映射为行单元格数组（末列为状态）
 * @param {{ columns: string[], rows: any[][] }} options.fallback 骨架列与行
 */
export function useDomainList({ apiFn, apiColumns, rowMapper, fallback }) {
  const rows = ref(fallback.rows);
  const columns = ref(fallback.columns);
  const source = ref("fallback");

  async function load() {
    try {
      const res = await apiFn();
      const data = res?.data;
      if (Array.isArray(data) && data.length) {
        rows.value = data.map(rowMapper);
        columns.value = apiColumns;
        source.value = "api";
      } else {
        rows.value = fallback.rows;
        columns.value = fallback.columns;
        source.value = "fallback";
      }
    } catch (e) {
      rows.value = fallback.rows;
      columns.value = fallback.columns;
      source.value = "fallback";
    }
  }

  onMounted(load);

  return { rows, columns, source, load };
}
