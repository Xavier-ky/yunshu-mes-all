/* yunshu-ui 全局组件与实例属性 — 供 Volar/IDE 识别（运行时由 YunshuUiHost 注册） */
declare module 'vue' {
  export interface GlobalComponents {
    Pagination: (typeof import('@/yunshu-ui/components/Pagination/index.vue'))['default'];
    RightToolbar: (typeof import('@/yunshu-ui/components/RightToolbar/index.vue'))['default'];
    DictTag: (typeof import('@/yunshu-ui/components/DictTag/index.vue'))['default'];
    pagination: (typeof import('@/yunshu-ui/components/Pagination/index.vue'))['default'];
    'right-toolbar': (typeof import('@/yunshu-ui/components/RightToolbar/index.vue'))['default'];
    'dict-tag': (typeof import('@/yunshu-ui/components/DictTag/index.vue'))['default'];
  }

  export interface ComponentCustomProperties {
    parseTime: (time: unknown, pattern?: string) => string;
    resetForm: (refName: string) => void;
    handleTree: (data: unknown[], id?: string, parentId?: string, children?: string) => unknown[];
    getDicts: (dictType: string) => Promise<{ data: unknown[] }>;
    dict: {
      type: Record<string, Array<{ label: string; value: string; raw?: Record<string, unknown> }>>;
      label: Record<string, Record<string, string>>;
    };
    checkPermission: (value: string[]) => boolean;
    $modal: {
      msgSuccess: (msg: string) => void;
      msgWarning: (msg: string) => void;
      msgError: (msg: string) => void;
      confirm: (msg: string) => Promise<unknown>;
    };
  }
}

export {};
