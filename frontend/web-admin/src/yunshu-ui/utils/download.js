import axios from "axios";
import { getToken } from "@/yunshu-ui/utils/auth";
import { blobValidate } from "@/yunshu-ui/utils/yunshu-utils";
import { ElMessage } from "element-plus";

const baseURL = import.meta.env.VITE_APP_BASE_API || "/api";

/** 去掉空值，供导出等接口作为 URL 查询参数（Spring @RequestParam 绑定） */
function cleanParams(params) {
  const cleaned = {};
  for (const [key, value] of Object.entries(params || {})) {
    if (value !== undefined && value !== null && value !== "") {
      cleaned[key] = value;
    }
  }
  return cleaned;
}

function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * POST 下载 Excel 等二进制文件（RuoYi 兼容）
 * @param {string} url 相对路径，如 system/user/importTemplate
 * @param {object} params 请求体参数
 * @param {string} filename 保存文件名
 */
export function download(url, params = {}, filename = "download.xlsx") {
  const token = getToken();
  const query = cleanParams(params);
  return axios({
    method: "post",
    url: baseURL + (url.startsWith("/") ? url : "/" + url),
    params: query,
    responseType: "blob",
    headers: token ? { Authorization: "Bearer " + token } : {},
  })
    .then(async (res) => {
      const blob = res.data;
      const valid = await blobValidate(blob.slice());
      if (valid) {
        saveBlob(blob, filename);
        return;
      }
      const text = await res.data.text();
      let msg = "下载失败";
      try {
        const json = JSON.parse(text);
        msg = json.msg || json.message || msg;
      } catch {
        msg = text || msg;
      }
      ElMessage.error(msg);
    })
    .catch((err) => {
      ElMessage.error(err?.message || "下载失败");
    });
}

export default download;
