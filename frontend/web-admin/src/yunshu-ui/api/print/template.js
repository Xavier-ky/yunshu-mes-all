import request from '@/yunshu-ui/api/request'

// 查询打印模板配置列表
export function listTemplate(query) {
  return request({
    url: '/print/template/list',
    method: 'get',
    params: query
  })
}

// 查询打印模板配置详细
export function getTemplate(templateId) {
  return request({
    url: '/print/template/' + templateId,
    method: 'get'
  })
}

// 新增打印模板配置
export function addTemplate(data) {
  return request({
    url: '/print/template',
    method: 'post',
    data: data
  })
}

// 修改打印模板配置
export function updateTemplate(data) {
  return request({
    url: '/print/template',
    method: 'put',
    data: data
  })
}

// 删除打印模板配置
export function delTemplate(templateId) {
  return request({
    url: '/print/template/' + templateId,
    method: 'delete'
  })
}

//上传缩略图
export function uploadShotCut(data){
  return request({
    url: '/common/uploadMinio',
    method: 'post',
    data: data
  })
}

// 根据打印模板类型查询打印模板配置详细
export function getByTemplateType(templateType) {
  return request({
    url: '/print/template/templateType/' + templateType,
    method: 'get'
  })
}

