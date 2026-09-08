

export {
  getMaterialCalls,
  getMaterialCallDetail,
  createMaterialCall,
  processMaterialCall,
  completeMaterialCall
} from './inventory'

export function upgradeToAndon(id) {

  const { getMaterialCallDetail } = require('./inventory')
  const { createEvent } = require('./andon')
  return getMaterialCallDetail(id).then(call => {
    if (!call) return Promise.reject(new Error('呼叫单不存在'))
    return createEvent({
      eventType: 'MATERIAL',
      eventTypeName: '物料异常',
      reasonCode: 'MATERIAL_SHORTAGE',
      description: `缺料升级：${call.materialName} 需求${call.needQty}，剩余${call.remainQty}`,
      lineName: call.lineName || ''
    })
  })
}
