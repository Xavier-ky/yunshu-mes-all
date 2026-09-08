import { defineStore } from "pinia";
import { fetchDashboardSummary } from "@/api/dashboard";
import { login as loginApi, faceLogin as faceLoginApi } from "@/api/auth";
import { fetchRoles, fetchUsers } from "@/api/system";
import { getAuthUser, getToken, hasAuthToken, setAuthUser, setToken, clearAuthSession } from "@/utils/auth-session";

const fallbackSummary = {
  metrics: [
    { name: "今日计划产量", value: "3,200", unit: "台", trend: "+8.4%", status: "NORMAL" },
    { name: "工单达成率", value: "86.5", unit: "%", trend: "+3.2%", status: "NORMAL" },
    { name: "直通率", value: "97.2", unit: "%", trend: "-0.6%", status: "WARNING" },
    { name: "安灯待处理", value: "6", unit: "件", trend: "+2", status: "ALERT" },
  ],
  workOrders: [
    { workOrderNo: "WO20260706001", productName: "16寸落地扇 FS-16A", lineName: "总装一线", planQty: "1200", completedQty: "860", status: "RUNNING" },
    { workOrderNo: "WO20260706002", productName: "空气循环扇 AC-12B", lineName: "总装二线", planQty: "800", completedQty: "240", status: "DISPATCHED" },
    { workOrderNo: "WO20260706003", productName: "台式风扇 DF-09C", lineName: "包装一线", planQty: "600", completedQty: "600", status: "COMPLETED" },
  ],
  alerts: [
    { alertType: "MATERIAL", alertTitle: "电机批次 MTR-20260706 欠料 320 件", level: "HIGH", owner: "仓库物料员", status: "OPEN" },
    { alertType: "QUALITY", alertTitle: "总装一线噪音检测出现连续不良", level: "MEDIUM", owner: "质检员", status: "PROCESSING" },
    { alertType: "EQUIPMENT", alertTitle: "包装一线封箱机待点检", level: "LOW", owner: "设备维修员", status: "OPEN" },
  ],
};

const fallbackSystemUsers = [
  {
    userId: 1,
    username: "admin",
    realName: "管理员",
    employeeNo: "U0001",
    deptName: "管理部",
    status: "ENABLED",
    roleCodes: ["MANAGER"],
    roles: ["管理人员"],
  },
  {
    userId: 2,
    username: "supervisor01",
    realName: "生产主管",
    employeeNo: "U0002",
    deptName: "生产部",
    status: "ENABLED",
    roleCodes: ["PROD_SUPERVISOR"],
    roles: ["生产主管"],
  },
  {
    userId: 3,
    username: "warehouse01",
    realName: "仓库物料员",
    employeeNo: "U0003",
    deptName: "仓储部",
    status: "ENABLED",
    roleCodes: ["WAREHOUSE_CLERK"],
    roles: ["仓库物料员"],
  },
  {
    userId: 4,
    username: "qc01",
    realName: "质检员",
    employeeNo: "U0004",
    deptName: "质量部",
    status: "ENABLED",
    roleCodes: ["QUALITY_INSPECTOR"],
    roles: ["质检员"],
  },
  {
    userId: 5,
    username: "repair01",
    realName: "设备维修员",
    employeeNo: "U0005",
    deptName: "设备部",
    status: "ENABLED",
    roleCodes: ["EQUIPMENT_MAINTAINER"],
    roles: ["设备维修员"],
  },
  {
    userId: 6,
    username: "worker01",
    realName: "产线操作工人",
    employeeNo: "U0006",
    deptName: "生产部",
    status: "ENABLED",
    roleCodes: ["LINE_OPERATOR"],
    roles: ["产线操作工人"],
  },
];

const fallbackSystemRoles = [
  { roleId: 1, roleCode: "MANAGER", roleName: "管理人员", roleDesc: "查看看板、报表与追溯，并维护用户、角色、权限和系统参数", status: "ENABLED" },
  { roleId: 2, roleCode: "PROD_SUPERVISOR", roleName: "生产主管", roleDesc: "负责订单、工单、排产、齐套、派工和现场生产管理", status: "ENABLED" },
  { roleId: 3, roleCode: "WAREHOUSE_CLERK", roleName: "仓库物料员", roleDesc: "处理备料、领料、发料、退料和库存批次", status: "ENABLED" },
  { roleId: 4, roleCode: "LINE_OPERATOR", roleName: "产线操作工人", roleDesc: "执行工位作业、扫码、报工和发起安灯", status: "ENABLED" },
  { roleId: 5, roleCode: "QUALITY_INSPECTOR", roleName: "质检员", roleDesc: "执行首末件、巡检、成品检验和质量放行", status: "ENABLED" },
  { roleId: 6, roleCode: "EQUIPMENT_MAINTAINER", roleName: "设备维修员", roleDesc: "处理设备点检、保养、报修和维修", status: "ENABLED" },
];

export const useAppStore = defineStore("app", {
  state: () => {
    const authUser = getAuthUser();
    return {
      token: getToken(),
      authUser,
      user: authUser
        ? {
            name: authUser.realName,
            role: authUser.roles?.[0] || authUser.roleCode || "",
            department: authUser.deptName || "",
          }
        : { name: "管理员", role: "管理人员", department: "管理部" },
      backendStatus: "offline",
      backendMessage: "当前使用前端骨架数据",
      dashboardSummary: fallbackSummary,
      users: fallbackSystemUsers,
      roles: fallbackSystemRoles,
      systemDataSource: "fallback",
    };
  },
  getters: {
    isAuthenticated: (state) => !!state.token,
  },
  actions: {
    async login(credentials) {
      try {
        const res = await loginApi(credentials);
        const data = res?.data;
        if (!data?.token) {
          throw new Error("登录失败，未收到令牌");
        }
        this.token = data.token;
        this.authUser = {
          userId: data.userId,
          username: data.username,
          realName: data.realName,
          deptName: data.deptName,
          roleCode: data.roleCode || data.roleCodes?.[0] || "",
          roleCodes: data.roleCodes || (data.roleCode ? [data.roleCode] : []),
          roles: data.roles || [],
          permissions: data.permissions || [],
        };
        this.user = {
          name: data.realName || data.username,
          role: data.roles?.[0] || data.roleCode || "",
          department: data.deptName || "",
        };
        setToken(this.token);
        setAuthUser(this.authUser);
        this.backendStatus = "online";
        this.backendMessage = "已登录";
        return data;
      } catch (e) {
        // [DEV PREVIEW] 后端不可用或为旧版本时，用骨架身份进入预览，便于查看界面。
        // 正式部署前删除此回退，并确保后端 /api/auth/login 可用。
        this.token = "dev-preview-token";
        this.authUser = {
          username: credentials.username || "admin",
          realName: "管理员",
          deptName: "管理部",
          roleCode: "MANAGER",
          roleCodes: ["MANAGER"],
          roles: ["管理人员"],
          permissions: [],
        };
        this.user = { name: "管理员", role: "管理人员", department: "管理部" };
        setToken(this.token);
        setAuthUser(this.authUser);
        this.backendStatus = "offline";
        this.backendMessage = "后端未连接（预览模式）";
      }
    },
    async faceLogin(imageBase64) {
      const res = await faceLoginApi({ imageBase64 });
      const data = res?.data;
      if (!data?.token) {
        throw new Error("人脸核验通过，但未收到登录令牌");
      }
      this.token = data.token;
      this.authUser = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        deptName: data.deptName,
        roleCode: data.roleCode || data.roleCodes?.[0] || "",
        roleCodes: data.roleCodes || (data.roleCode ? [data.roleCode] : []),
        roles: data.roles || [],
        permissions: data.permissions || [],
      };
      this.user = {
        name: data.realName || data.username,
        role: data.roles?.[0] || data.roleCode || "",
        department: data.deptName || "",
      };
      setToken(this.token);
      setAuthUser(this.authUser);
      this.backendStatus = "online";
      this.backendMessage = "人脸核验登录成功";
      return data;
    },
    logout() {
      this.token = "";
      this.authUser = null;
      this.user = { name: "访客", role: "", department: "" };
      clearAuthSession();
      this.backendStatus = "offline";
      this.backendMessage = "已退出登录";
    },
    async loadDashboard() {
      try {
        const response = await fetchDashboardSummary();
        this.dashboardSummary = response?.data || fallbackSummary;
        this.backendStatus = "online";
        this.backendMessage = "后端 API 已连接";
      } catch (error) {
        this.dashboardSummary = fallbackSummary;
        this.backendStatus = "offline";
        this.backendMessage = "后端未启动，当前使用前端骨架数据";
      }
    },
    async loadSystemData() {
      try {
        const [users, roles] = await Promise.all([fetchUsers(), fetchRoles()]);
        this.users = users?.data?.length ? users.data : fallbackSystemUsers;
        this.roles = roles?.data?.length ? roles.data : fallbackSystemRoles;
        this.systemDataSource = users?.data?.length && roles?.data?.length ? "api" : "fallback";
      } catch {
        this.users = fallbackSystemUsers;
        this.roles = fallbackSystemRoles;
        this.systemDataSource = "fallback";
      }
    },
  },
});
