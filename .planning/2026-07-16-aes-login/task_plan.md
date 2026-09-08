# Task Plan: AES-GCM login encryption

## Goal

Add real AES-GCM protection to the web login password without changing the existing BCrypt, authentication, JWT, role, or permission logic.

## Phases

### Phase 1: Implement compatible crypto boundary

- [x] Replace crypto placeholders with AES-GCM implementations and key configuration.
- [x] Decrypt encrypted login requests before the existing authentication service; retain plaintext-request compatibility.
- **Status:** complete

### Phase 2: Connect the browser client

- [x] Encrypt the password with browser Web Crypto before posting the login request.
- [x] Document the required course-demo key configuration.
- **Status:** complete

### Phase 3: Verify

- [x] Add focused crypto/auth tests.
- [x] Run backend tests and frontend validation.
- **Status:** complete (frontend build environment timed out; JavaScript syntax validation passed)
