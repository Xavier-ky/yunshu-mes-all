const AES_GCM_NAME = "AES-GCM";
const AES_KEY_LENGTH_BYTES = 32;
const AES_GCM_IV_LENGTH_BYTES = 12;

// This fallback makes the local course demonstration work out of the box.
// It is public by design and MUST be replaced with VITE_LOGIN_AES_KEY for deployment.
const COURSE_DEMO_AES_KEY = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWY=";

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function resolveKeyBytes() {
  const configuredKey = import.meta.env.VITE_LOGIN_AES_KEY || COURSE_DEMO_AES_KEY;
  const keyBytes = fromBase64(configuredKey, "AES 密钥");
  if (keyBytes.length !== AES_KEY_LENGTH_BYTES) {
    throw new Error("AES 密钥必须是 Base64 编码的 32 字节值");
  }
  return keyBytes;
}

async function importAesKey(usages) {
  return crypto.subtle.importKey(
    "raw",
    resolveKeyBytes(),
    { name: AES_GCM_NAME },
    false,
    usages,
  );
}

export async function encryptLoginPassword(password) {
  if (!password) {
    throw new Error("密码不能为空");
  }
  const iv = crypto.getRandomValues(new Uint8Array(AES_GCM_IV_LENGTH_BYTES));
  const cipherText = await crypto.subtle.encrypt(
    { name: AES_GCM_NAME, iv },
    await importAesKey(["encrypt"]),
    encoder.encode(password),
  );
  return {
    encryptedPassword: toBase64(new Uint8Array(cipherText)),
    iv: toBase64(iv),
  };
}

export async function encryptPayload(payload) {
  const { encryptedPassword: cipherText, iv } = await encryptLoginPassword(JSON.stringify(payload));
  return { algorithm: AES_GCM_NAME, cipherText, iv };
}

export async function decryptPayload({ cipherText, iv }) {
  if (!cipherText || !iv) {
    throw new Error("密文或 IV 不能为空");
  }
  const plainText = await crypto.subtle.decrypt(
    { name: AES_GCM_NAME, iv: fromBase64(iv, "IV") },
    await importAesKey(["decrypt"]),
    fromBase64(cipherText, "密文"),
  );
  return JSON.parse(decoder.decode(plainText));
}

// Keep the original exports available for any future callers of the placeholder API.
export const encryptPayloadPlaceholder = encryptPayload;
export const decryptPayloadPlaceholder = decryptPayload;

function fromBase64(value, fieldName) {
  try {
    const binary = window.atob(value);
    return Uint8Array.from(binary, (char) => char.charCodeAt(0));
  } catch {
    throw new Error(`${fieldName}不是合法的 Base64 值`);
  }
}

function toBase64(bytes) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary);
}
