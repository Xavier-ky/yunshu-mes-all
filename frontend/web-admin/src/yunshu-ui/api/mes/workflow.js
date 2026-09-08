import request from "@/yunshu-ui/api/request";

export function fetchWorkflowTodos(role) {
  return request({
    url: "/planning/workflow/todos",
    method: "get",
    params: role ? { role } : {},
  });
}

export function fetchWorkflowPipeline(key) {
  return request({
    url: `/planning/workflow/pipeline/${encodeURIComponent(key)}`,
    method: "get",
  });
}

export function traceWorkOrder(workOrderNo) {
  return request({
    url: `/traceability/work-order/${encodeURIComponent(workOrderNo)}`,
    method: "get",
  });
}
