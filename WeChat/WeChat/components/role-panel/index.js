import { hasPermission } from '../../utils/auth'

Component({
  properties: {
    roles: { type: String, value: '' }
  },

  data: {
    visible: false
  },

  lifetimes: {
    attached() {
      this.checkPermission()
    }
  },

  observers: {
    'roles': function() {
      this.checkPermission()
    }
  },

  methods: {
    checkPermission() {
      const roles = this.properties.roles
      const visible = hasPermission(roles)
      this.setData({ visible })
    }
  }
})