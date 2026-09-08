import { STATUS_COLOR_MAP } from '../../utils/constants'

Component({
  properties: {
    status: { type: String, value: '' },
    type: { type: String, value: 'default' }
  },

  computed: {},

  observers: {
    'status': function(status) {
      const color = STATUS_COLOR_MAP[status] || 'gray'
      this.setData({ colorClass: color })
    }
  },

  data: {
    colorClass: 'gray'
  },

  lifetimes: {
    attached() {
      const color = STATUS_COLOR_MAP[this.properties.status] || 'gray'
      this.setData({ colorClass: color })
    }
  }
})