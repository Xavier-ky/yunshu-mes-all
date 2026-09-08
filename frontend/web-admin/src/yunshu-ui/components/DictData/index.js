// vue2 removed
import DataDict from '@/yunshu-ui/utils/dict'
import { getDicts as getDicts } from '@/yunshu-ui/api/system/dict/data'

function install() {
  Vue.use(DataDict, {
    metas: {
      '*': {
        labelField: 'dictLabel',
        valueField: 'dictValue',
        request(dictMeta) {
          return getDicts(dictMeta.type).then(res => res.data)
        },
      },
    },
  })
}

export default {
  install,
}