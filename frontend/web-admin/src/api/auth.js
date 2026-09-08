import { request } from "./request";
import { encryptLoginPassword } from "@/security/crypto/symmetricCrypto";

export async function login({ username, password }) {
  const encrypted = await encryptLoginPassword(password);
  return request.post("/auth/login", { username, ...encrypted });
}

export async function faceLogin({ imageBase64 }) {
  // DeepFace inference can take longer than ordinary CRUD requests, especially
  // immediately after its local model service restarts. Keep the 8s default
  // for the rest of the MES API and extend only this explicit login action.
  return request.post("/auth/face-login", { imageBase64 }, { timeout: 45000 });
}
