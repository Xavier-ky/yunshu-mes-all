# Findings

- Existing login flow: LoginView -> Pinia app.login -> `/api/auth/login` -> AuthController -> AuthService.
- BCrypt verification, login audit logging, role validation, permission lookup and JWT issuance are all downstream in AuthService and will remain unchanged.
- The existing frontend and backend crypto utilities are placeholders, not encryption implementations.
- Compatibility approach: accept `encryptedPassword` + `iv` for AES-GCM, but retain `password` for existing callers/tests.
- The course-demo AES key must be a shared Base64-encoded 32-byte key supplied through configuration. It must not replace HTTPS in a real deployment.

