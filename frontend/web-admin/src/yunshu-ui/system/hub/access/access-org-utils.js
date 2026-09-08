export function findNode(nodes, id) {
  if (id == null || id === "") return null;
  for (const node of nodes || []) {
    if (String(node.id) === String(id)) return node;
    const found = findNode(node.children, id);
    if (found) return found;
  }
  return null;
}

export function findPath(nodes, id, trail = []) {
  for (const node of nodes || []) {
    const next = [...trail, node];
    if (String(node.id) === String(id)) return next;
    const found = findPath(node.children, id, next);
    if (found) return found;
  }
  return null;
}

export function countNodes(nodes) {
  let n = 0;
  for (const node of nodes || []) {
    n += 1;
    n += countNodes(node.children);
  }
  return n;
}

export function maxDepth(nodes, depth = 1) {
  if (!nodes?.length) return Math.max(0, depth - 1);
  let max = depth;
  for (const node of nodes) {
    max = Math.max(max, maxDepth(node.children, depth + 1));
  }
  return max;
}

export function subtreeUserTotal(node, counts) {
  if (!node) return 0;
  let total = Number(counts[String(node.id)] || 0);
  for (const child of node.children || []) {
    total += subtreeUserTotal(child, counts);
  }
  return total;
}

export function getTopLevelDepts(deptTree) {
  const root = (deptTree || [])[0];
  if (root?.children?.length) return root.children;
  return deptTree || [];
}

export function getCorpRoot(deptTree) {
  return (deptTree || [])[0] ?? null;
}

export function isCorpRoot(deptTree, data) {
  if (!data) return false;
  return (deptTree || []).some((n) => String(n.id) === String(data.id));
}
