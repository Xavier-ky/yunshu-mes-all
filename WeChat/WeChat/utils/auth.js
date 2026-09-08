import { getToken, getUserInfo } from './storage'

export function isLoggedIn() {
  return !!getToken()
}

export function getCurrentUser() {
  return getUserInfo()
}

export function getRoleCode() {
  const user = getUserInfo()
  return user ? user.roleCode : ''
}

export function hasPermission(allowedRoles) {
  if (!allowedRoles) return true
  const roleCode = getRoleCode()
  const roles = allowedRoles.split(',').map(r => r.trim())
  return roles.includes(roleCode)
}