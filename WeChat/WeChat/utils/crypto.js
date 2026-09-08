

export function encryptPayload(payload) {

  return payload
}

export function decryptPayload(payload) {

  return payload
}

export function generateNonce() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let nonce = ''
  for (let i = 0; i < 16; i++) {
    nonce += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return nonce
}

export function signRequest(payload) {

  return ''
}