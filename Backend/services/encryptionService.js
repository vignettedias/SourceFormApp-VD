/**
 * encryptionService.js
 * ============================================================
 * Custom Fernet-compatible cryptographic implementation
 * Built from scratch using only Node.js built-in `crypto` module.
 *
 * Fernet Specification (RFC-style):
 *   https://github.com/fernet/spec/blob/master/Spec.md
 *
 * Token wire format (before base64url):
 *   Version    [0]       : 1 byte  — always 0x80
 *   Timestamp  [1..8]    : 8 bytes — unsigned 64-bit big-endian UNIX seconds
 *   IV         [9..24]   : 16 bytes — AES-128-CBC initialisation vector
 *   Ciphertext [25..N-32]: variable — AES-128-CBC(PKCS7-padded plaintext)
 *   HMAC       [N-32..N] : 32 bytes — HMAC-SHA256 over all preceding bytes
 *
 * Key format (URL-safe base64, 32 bytes decoded):
 *   Signing key   [0..15]  : 16 bytes — used for HMAC-SHA256
 *   Encryption key[16..31] : 16 bytes — used for AES-128-CBC
 * ============================================================
 */

"use strict";

const crypto = require("crypto");

// ─────────────────────────────────────────────────────────────
// §1  CONSTANTS & FERNET TOKEN STRUCTURE
// ─────────────────────────────────────────────────────────────

/**
 * VERSION_BYTE — The only version Fernet has ever defined.
 * Must be the first byte of every token (pre-base64).
 */
const VERSION_BYTE = 0x80;

/**
 * FIELD_SIZES — byte lengths of every fixed-size field in the
 * binary token, used throughout encode/decode to slice buffers
 * without magic numbers.
 */
const FIELD_SIZES = {
  VERSION:    1,   // 0x80
  TIMESTAMP:  8,   // 64-bit big-endian UNIX seconds
  IV:         16,  // AES-128-CBC initialisation vector
  HMAC:       32,  // HMAC-SHA256 output
  KEY_TOTAL:  32,  // full decoded Fernet key
  SIGN_KEY:   16,  // first half  → HMAC signing key
  ENC_KEY:    16,  // second half → AES encryption key
  AES_BLOCK:  16,  // AES block size (used in PKCS7)
};

/** Minimum offset at which ciphertext starts inside the binary token. */
const CIPHERTEXT_OFFSET = FIELD_SIZES.VERSION + FIELD_SIZES.TIMESTAMP + FIELD_SIZES.IV;


// ─────────────────────────────────────────────────────────────
// §2  URL-SAFE BASE64 ENCODING / DECODING
// ─────────────────────────────────────────────────────────────

/**
 * toBase64Url(buffer) → string
 *
 * Converts a Buffer to URL-safe base64 (RFC 4648 §5):
 *   standard '+' → '-'
 *   standard '/' → '_'
 *   trailing '=' padding stripped
 *
 * Fernet tokens MUST use URL-safe base64 so they are safe to
 * embed in HTTP headers, query strings, and JSON values.
 */
function toBase64Url(buffer) {
  return buffer
    .toString("base64")          // standard base64
    .replace(/\+/g, "-")         // '+' → '-'
    .replace(/\//g, "_")         // '/' → '_'
    .replace(/=+$/, "");         // strip trailing '='
}

/**
 * fromBase64Url(str) → Buffer
 *
 * Inverse of toBase64Url.  Re-adds stripped padding before
 * decoding so Node's base64 decoder doesn't choke.
 */
function fromBase64Url(str) {
  // Re-pad to a multiple of 4 characters
  const padded = str + "=".repeat((4 - (str.length % 4)) % 4);
  return Buffer.from(
    padded.replace(/-/g, "+").replace(/_/g, "/"),
    "base64"
  );
}


// ─────────────────────────────────────────────────────────────
// §3  KEY DERIVATION — splitting the 32-byte Fernet key
// ─────────────────────────────────────────────────────────────

/**
 * deriveKeys(fernetKeyB64) → { signingKey: Buffer, encryptionKey: Buffer }
 *
 * A Fernet secret key is a 32-byte value encoded as URL-safe
 * base64.  The spec divides it cleanly in half:
 *
 *   bytes  0–15  → signing key    (HMAC-SHA256)
 *   bytes 16–31  → encryption key (AES-128-CBC)
 *
 * This function decodes the key and returns both halves so
 * that every other function only deals with raw Buffers.
 *
 * @param {string} fernetKeyB64 — URL-safe base64 encoded 32-byte key
 * @throws {Error} if the decoded key is not exactly 32 bytes
 */
function deriveKeys(fernetKeyB64) {
  const raw = fromBase64Url(fernetKeyB64);

  if (raw.length !== FIELD_SIZES.KEY_TOTAL) {
    throw new Error(
      `Invalid Fernet key length: expected ${FIELD_SIZES.KEY_TOTAL} bytes, ` +
      `got ${raw.length} bytes after base64 decoding.`
    );
  }

  return {
    signingKey:    raw.slice(0, FIELD_SIZES.SIGN_KEY),   // bytes  0–15
    encryptionKey: raw.slice(FIELD_SIZES.SIGN_KEY),      // bytes 16–31
  };
}

/**
 * generateKey() → string
 *
 * Helper: creates a fresh, cryptographically random 32-byte
 * Fernet key and returns it as URL-safe base64, ready to be
 * stored in an environment variable.
 *
 * Use this once in a setup script — never call it per-request.
 */
function generateKey() {
  return toBase64Url(crypto.randomBytes(FIELD_SIZES.KEY_TOTAL));
}


// ─────────────────────────────────────────────────────────────
// §4  PKCS7 PADDING
// ─────────────────────────────────────────────────────────────

/**
 * pkcs7Pad(data, blockSize) → Buffer
 *
 * PKCS#7 padding (RFC 5652 §6.3) pads the plaintext to an
 * exact multiple of blockSize.  Each padding byte's VALUE
 * equals the NUMBER of bytes added.
 *
 * Examples for blockSize = 16:
 *   "A"        (1 byte)  → append 15 bytes of 0x0F
 *   "AAAAAAAAAAAAAAAA" (16 bytes) → append 16 bytes of 0x10
 *   ""         (0 bytes) → append 16 bytes of 0x10
 *
 * IMPORTANT: even if the data is already a multiple of blockSize,
 * a full block of padding is ALWAYS appended.  This lets the
 * unpadder unambiguously distinguish padding from data.
 *
 * @param {Buffer} data      — raw plaintext bytes
 * @param {number} blockSize — cipher block size in bytes (16 for AES)
 */
function pkcs7Pad(data, blockSize) {
  const padLength = blockSize - (data.length % blockSize);
  const padding   = Buffer.alloc(padLength, padLength); // every byte = padLength
  return Buffer.concat([data, padding]);
}

/**
 * pkcs7Unpad(data, blockSize) → Buffer
 *
 * Strips PKCS#7 padding from decrypted data and validates it.
 * Throws if:
 *   • the last byte value (padLength) is 0 or > blockSize
 *   • not all of the last padLength bytes equal padLength
 *
 * @param {Buffer} data      — decrypted (still padded) bytes
 * @param {number} blockSize — cipher block size in bytes
 */
function pkcs7Unpad(data, blockSize) {
  if (data.length === 0) {
    throw new Error("PKCS7 unpad: input is empty.");
  }

  const padLength = data[data.length - 1]; // last byte tells us padding length

  if (padLength === 0 || padLength > blockSize) {
    throw new Error(
      `PKCS7 unpad: invalid padding byte value 0x${padLength.toString(16).padStart(2, "0")}.`
    );
  }

  // Verify every padding byte
  for (let i = data.length - padLength; i < data.length; i++) {
    if (data[i] !== padLength) {
      throw new Error(
        `PKCS7 unpad: padding byte at index ${i} is 0x${data[i].toString(16).padStart(2, "0")}, ` +
        `expected 0x${padLength.toString(16).padStart(2, "0")}.`
      );
    }
  }

  return data.slice(0, data.length - padLength);
}


// ─────────────────────────────────────────────────────────────
// §5  AES-128-CBC ENCRYPTION & DECRYPTION
// ─────────────────────────────────────────────────────────────

/**
 * aesCbcEncrypt(plaintext, key, iv) → Buffer
 *
 * Encrypts a PKCS7-padded plaintext with AES-128-CBC.
 *
 * AES-128 requires:
 *   key  — exactly 16 bytes
 *   iv   — exactly 16 bytes (must be unique per message)
 *
 * CBC (Cipher Block Chaining) XORs each plaintext block with
 * the previous ciphertext block before encryption, so identical
 * plaintext blocks produce different ciphertext blocks as long
 * as the IV is unique.
 *
 * Node's `crypto.createCipheriv` disables automatic padding by
 * default when we call setAutoPadding(false) — we handle PKCS7
 * ourselves in §4 so we have full visibility into each step.
 *
 * @param {Buffer} plaintext — raw, UN-padded plaintext
 * @param {Buffer} key       — 16-byte AES key
 * @param {Buffer} iv        — 16-byte initialisation vector
 */
function aesCbcEncrypt(plaintext, key, iv) {
  // Step 1: apply PKCS7 padding before feeding to the cipher
  const padded = pkcs7Pad(plaintext, FIELD_SIZES.AES_BLOCK);

  // Step 2: create the AES-128-CBC cipher and disable Node's
  //         built-in padding (we already padded above)
  const cipher = crypto.createCipheriv("aes-128-cbc", key, iv);
  cipher.setAutoPadding(false);

  // Step 3: encrypt and return
  return Buffer.concat([cipher.update(padded), cipher.final()]);
}

/**
 * aesCbcDecrypt(ciphertext, key, iv) → Buffer
 *
 * Decrypts AES-128-CBC ciphertext and strips PKCS7 padding.
 *
 * @param {Buffer} ciphertext — raw encrypted bytes (multiple of 16)
 * @param {Buffer} key        — 16-byte AES key (must match encrypt)
 * @param {Buffer} iv         — 16-byte IV     (must match encrypt)
 */
function aesCbcDecrypt(ciphertext, key, iv) {
  if (ciphertext.length % FIELD_SIZES.AES_BLOCK !== 0) {
    throw new Error(
      `AES-CBC decrypt: ciphertext length ${ciphertext.length} is not ` +
      `a multiple of block size ${FIELD_SIZES.AES_BLOCK}.`
    );
  }

  const decipher = crypto.createDecipheriv("aes-128-cbc", key, iv);
  decipher.setAutoPadding(false); // we unpad manually below

  const padded = Buffer.concat([decipher.update(ciphertext), decipher.final()]);

  // Strip PKCS7 padding and return plaintext
  return pkcs7Unpad(padded, FIELD_SIZES.AES_BLOCK);
}


// ─────────────────────────────────────────────────────────────
// §6  HMAC-SHA256 AUTHENTICATION
// ─────────────────────────────────────────────────────────────

/**
 * computeHmac(signingKey, data) → Buffer
 *
 * Computes HMAC-SHA256 over `data` using `signingKey`.
 *
 * In the Fernet token the HMAC covers EVERYTHING that precedes
 * it in the binary token:
 *   version (1) + timestamp (8) + iv (16) + ciphertext (variable)
 *
 * This ensures an attacker cannot forge or tamper with any
 * field without invalidating the HMAC.
 *
 * @param {Buffer} signingKey — 16-byte signing sub-key
 * @param {Buffer} data       — bytes to authenticate
 */
function computeHmac(signingKey, data) {
  return crypto
    .createHmac("sha256", signingKey)
    .update(data)
    .digest(); // returns a 32-byte Buffer
}

/**
 * verifyHmac(signingKey, data, expectedHmac) → void
 *
 * Verifies the HMAC with a constant-time comparison to prevent
 * timing attacks.  Throws an error on mismatch so callers do
 * not need to check a return value.
 *
 * crypto.timingSafeEqual requires both buffers to be the same
 * length; we always pass 32-byte HMACs so that invariant holds.
 *
 * @param {Buffer} signingKey   — same key used during encrypt
 * @param {Buffer} data         — all token bytes except the HMAC itself
 * @param {Buffer} expectedHmac — the 32 bytes extracted from the token
 * @throws {Error} on HMAC mismatch
 */
function verifyHmac(signingKey, data, expectedHmac) {
  const computed = computeHmac(signingKey, data);

  // Both buffers must be exactly FIELD_SIZES.HMAC bytes
  if (
    computed.length !== FIELD_SIZES.HMAC ||
    expectedHmac.length !== FIELD_SIZES.HMAC
  ) {
    throw new Error("HMAC verification: unexpected buffer length.");
  }

  if (!crypto.timingSafeEqual(computed, expectedHmac)) {
    throw new Error("HMAC verification failed: token has been tampered with or key is wrong.");
  }
}


// ─────────────────────────────────────────────────────────────
// §7  TIMESTAMP UTILITIES
// ─────────────────────────────────────────────────────────────

/**
 * encodeTimestamp(unixSeconds) → Buffer (8 bytes)
 *
 * Serialises a UNIX timestamp as a big-endian 64-bit unsigned
 * integer.  JavaScript's native integers are only 53-bit safe,
 * so we split the value across two 32-bit writes:
 *
 *   high32 = Math.floor(unixSeconds / 2^32)
 *   low32  = unixSeconds % 2^32
 *
 * For any plausible date in the next several millennia, high32
 * will be 0, making this mathematically identical to simply
 * writing the seconds into the lower 4 bytes.
 *
 * @param {number} unixSeconds — seconds since epoch (integer)
 */
function encodeTimestamp(unixSeconds) {
  const buf   = Buffer.alloc(FIELD_SIZES.TIMESTAMP);
  const high32 = Math.floor(unixSeconds / 0x100000000); // upper 32 bits
  const low32  = unixSeconds >>> 0;                     // lower 32 bits

  buf.writeUInt32BE(high32, 0); // bytes 0–3
  buf.writeUInt32BE(low32,  4); // bytes 4–7
  return buf;
}

/**
 * decodeTimestamp(buf) → number
 *
 * Reconstructs a UNIX timestamp from the 8-byte big-endian
 * representation written by encodeTimestamp.
 *
 * @param {Buffer} buf — exactly 8 bytes
 */
function decodeTimestamp(buf) {
  const high32 = buf.readUInt32BE(0);
  const low32  = buf.readUInt32BE(4);
  return high32 * 0x100000000 + low32;
}

/**
 * currentUnixSeconds() → number
 *
 * Returns the current time as an integer number of seconds
 * since the UNIX epoch (1970-01-01 00:00:00 UTC).
 */
function currentUnixSeconds() {
  return Math.floor(Date.now() / 1000);
}

/**
 * checkTTL(tokenTimestamp, ttl) → void
 *
 * Validates that a token has not expired.
 * If ttl is 0 or falsy, the check is skipped (infinite TTL).
 *
 * @param {number} tokenTimestamp — UNIX seconds from the token
 * @param {number} ttl            — max age in seconds (0 = no check)
 * @throws {Error} if the token is older than ttl seconds
 */
function checkTTL(tokenTimestamp, ttl) {
  if (!ttl) return; // ttl === 0 means "do not check"
  const age = currentUnixSeconds() - tokenTimestamp;
  if (age > ttl) {
    throw new Error(
      `Token has expired: age is ${age}s, TTL is ${ttl}s.`
    );
  }
}


// ─────────────────────────────────────────────────────────────
// §8  FERNET TOKEN STRUCTURE — ASSEMBLE & PARSE
// ─────────────────────────────────────────────────────────────

/**
 * assembleBinaryToken(iv, ciphertext, timestamp) → Buffer
 *
 * Builds the raw (pre-HMAC, pre-base64) Fernet token layout:
 *
 *   [VERSION(1)] [TIMESTAMP(8)] [IV(16)] [CIPHERTEXT(variable)]
 *
 * The HMAC is appended by the caller after this buffer is built,
 * because the HMAC itself covers these bytes.
 *
 * @param {Buffer} iv         — 16-byte AES IV
 * @param {Buffer} ciphertext — encrypted payload
 * @param {number} timestamp  — UNIX seconds
 */
function assembleBinaryToken(iv, ciphertext, timestamp) {
  const versionBuf   = Buffer.from([VERSION_BYTE]);
  const timestampBuf = encodeTimestamp(timestamp);
  return Buffer.concat([versionBuf, timestampBuf, iv, ciphertext]);
}

/**
 * parseBinaryToken(rawBuf) → { version, timestamp, iv, ciphertext, hmac, dataForHmac }
 *
 * Slices a decoded binary token into its constituent fields.
 * Does NOT verify the HMAC or timestamp — that is left to the
 * decryption pipeline in §10.
 *
 * @param {Buffer} rawBuf — decoded (from base64url) token bytes
 * @throws {Error} if the buffer is too short to be a valid token
 */
function parseBinaryToken(rawBuf) {
  const minLength =
    FIELD_SIZES.VERSION   +   // 1
    FIELD_SIZES.TIMESTAMP +   // 8
    FIELD_SIZES.IV        +   // 16
    FIELD_SIZES.AES_BLOCK +   // 16  (minimum 1 block of ciphertext)
    FIELD_SIZES.HMAC;         // 32

  if (rawBuf.length < minLength) {
    throw new Error(
      `Token is too short: ${rawBuf.length} bytes (minimum ${minLength}).`
    );
  }

  let offset = 0;

  const version = rawBuf.readUInt8(offset);
  offset += FIELD_SIZES.VERSION;

  if (version !== VERSION_BYTE) {
    throw new Error(
      `Unknown Fernet version: 0x${version.toString(16).padStart(2, "0")}. ` +
      `Expected 0x${VERSION_BYTE.toString(16)}.`
    );
  }

  const timestampBuf = rawBuf.slice(offset, offset + FIELD_SIZES.TIMESTAMP);
  const timestamp    = decodeTimestamp(timestampBuf);
  offset += FIELD_SIZES.TIMESTAMP;

  const iv = rawBuf.slice(offset, offset + FIELD_SIZES.IV);
  offset += FIELD_SIZES.IV;

  // Everything between the IV and the final 32 bytes is ciphertext
  const ciphertext = rawBuf.slice(offset, rawBuf.length - FIELD_SIZES.HMAC);
  const hmac       = rawBuf.slice(rawBuf.length - FIELD_SIZES.HMAC);

  // The HMAC was computed over all bytes EXCEPT the HMAC itself
  const dataForHmac = rawBuf.slice(0, rawBuf.length - FIELD_SIZES.HMAC);

  return { version, timestamp, iv, ciphertext, hmac, dataForHmac };
}


// ─────────────────────────────────────────────────────────────
// §9  COMPLETE ENCRYPTION PIPELINE
// ─────────────────────────────────────────────────────────────

/**
 * encrypt(text, fernetKeyB64, options) → string
 *
 * Full Fernet encryption pipeline:
 *
 *   1. Decode the 32-byte Fernet key → signingKey + encryptionKey
 *   2. Generate a cryptographically random 16-byte IV
 *   3. UTF-8 encode the plaintext string → raw bytes
 *   4. AES-128-CBC encrypt (with PKCS7 padding applied internally)
 *   5. Record the current UNIX timestamp
 *   6. Assemble binary token: [0x80][timestamp][IV][ciphertext]
 *   7. Compute HMAC-SHA256 over the assembled bytes
 *   8. Append the 32-byte HMAC to form the complete token
 *   9. URL-safe base64 encode the entire token
 *
 * @param {string}  text          — plaintext to encrypt
 * @param {string}  fernetKeyB64  — URL-safe base64 Fernet key
 * @param {object}  [options]
 * @param {number}  [options.timestamp] — override timestamp (for testing)
 * @param {Buffer}  [options.iv]        — override IV        (for testing)
 * @returns {string} Fernet token (URL-safe base64)
 */
function encrypt(text, fernetKeyB64, options = {}) {
  if (!text && text !== 0) return "";

  // ── Step 1: key derivation ──────────────────────────────────
  const { signingKey, encryptionKey } = deriveKeys(fernetKeyB64);

  // ── Step 2: random IV (or injected for testing) ─────────────
  const iv = options.iv || crypto.randomBytes(FIELD_SIZES.IV);

  // ── Step 3: encode plaintext to UTF-8 bytes ──────────────────
  const plaintextBytes = Buffer.from(String(text), "utf8");

  // ── Step 4: AES-128-CBC encryption (PKCS7 padding inside) ────
  const ciphertext = aesCbcEncrypt(plaintextBytes, encryptionKey, iv);

  // ── Step 5: timestamp ─────────────────────────────────────────
  const timestamp = options.timestamp !== undefined
    ? options.timestamp
    : currentUnixSeconds();

  // ── Step 6: assemble binary token (without HMAC yet) ─────────
  const tokenWithoutHmac = assembleBinaryToken(iv, ciphertext, timestamp);

  // ── Step 7: compute HMAC over the assembled bytes ─────────────
  const hmac = computeHmac(signingKey, tokenWithoutHmac);

  // ── Step 8: append HMAC ───────────────────────────────────────
  const fullToken = Buffer.concat([tokenWithoutHmac, hmac]);

  // ── Step 9: URL-safe base64 encode ───────────────────────────
  return toBase64Url(fullToken);
}


// ─────────────────────────────────────────────────────────────
// §10  COMPLETE DECRYPTION PIPELINE
// ─────────────────────────────────────────────────────────────

/**
 * decrypt(tokenText, fernetKeyB64, options) → string
 *
 * Full Fernet decryption pipeline:
 *
 *   1. URL-safe base64 decode the token string → raw bytes
 *   2. Slice the binary token into its fields (§8)
 *   3. Decode the 32-byte Fernet key → signingKey + encryptionKey
 *   4. Verify the HMAC-SHA256 (constant-time) — throws on failure
 *   5. Check the TTL if options.ttl > 0              — throws if expired
 *   6. AES-128-CBC decrypt the ciphertext
 *   7. PKCS7 unpad the decrypted bytes (inside aesCbcDecrypt)
 *   8. UTF-8 decode the result → plaintext string
 *
 * @param {string}  tokenText     — Fernet token (URL-safe base64)
 * @param {string}  fernetKeyB64  — URL-safe base64 Fernet key
 * @param {object}  [options]
 * @param {number}  [options.ttl] — max token age in seconds (0 = no check)
 * @returns {string} decrypted plaintext
 * @throws {Error}   on any authentication, format, or TTL failure
 */
function decrypt(tokenText, fernetKeyB64, options = {}) {
  if (!tokenText) return "";

  // ── Step 1: base64url decode ──────────────────────────────────
  const rawBuf = fromBase64Url(tokenText);

  // ── Step 2: parse binary token fields ────────────────────────
  const { timestamp, iv, ciphertext, hmac, dataForHmac } =
    parseBinaryToken(rawBuf);

  // ── Step 3: key derivation ────────────────────────────────────
  const { signingKey, encryptionKey } = deriveKeys(fernetKeyB64);

  // ── Step 4: HMAC verification (throws on mismatch) ───────────
  verifyHmac(signingKey, dataForHmac, hmac);

  // ── Step 5: TTL check (throws if expired) ─────────────────────
  checkTTL(timestamp, options.ttl || 0);

  // ── Step 6 & 7: AES-128-CBC decrypt + PKCS7 unpad ────────────
  const plaintextBytes = aesCbcDecrypt(ciphertext, encryptionKey, iv);

  // ── Step 8: UTF-8 decode ──────────────────────────────────────
  return plaintextBytes.toString("utf8");
}


// ─────────────────────────────────────────────────────────────
// §11  ENV-KEY CONVENIENCE WRAPPERS  (matches original API)
// ─────────────────────────────────────────────────────────────

/**
 * createService(fernetKeyB64) → { encrypt, decrypt }
 *
 * Returns an encrypt/decrypt pair bound to the given key.
 * This mirrors the original fernet-library API so the rest of
 * the codebase can call service.encrypt(text) unchanged.
 *
 * @param {string} fernetKeyB64 — URL-safe base64 32-byte key
 */
function createService(fernetKeyB64) {
  if (!fernetKeyB64) {
    throw new Error(
      "FERNET_SECRET environment variable is not set. " +
      "Generate one with encryptionService.generateKey()."
    );
  }

  // Validate key format at startup rather than per-call
  deriveKeys(fernetKeyB64);

  return {
    /**
     * Encrypts a value and returns a Fernet token string.
     * Returns "" for empty / falsy input.
     * @param {*} value — anything that can be stringified
     */
    encrypt(value) {
      return encrypt(value, fernetKeyB64);
    },

    /**
     * Decrypts a Fernet token and returns the original string.
     * Returns "" for empty / falsy input.
     * @param {string} token      — Fernet token
     * @param {number} [ttl=0]    — optional max age in seconds
     */
    decrypt(token, ttl = 0) {
      return decrypt(token, fernetKeyB64, { ttl });
    },
  };
}


// ─────────────────────────────────────────────────────────────
// §12  DROP-IN COMPATIBILITY LAYER
//      Mirrors the exact export surface of the original
//      fernet-library-based encryptionService.js so that every
//      require("./encryptionService") call site works without
//      any changes.
// ─────────────────────────────────────────────────────────────

/**
 * _service — a key-bound instance created once at module load,
 * exactly as the original file did with `new fernet.Secret(...)`.
 * All env-key validation happens here, at startup.
 */
const _service = createService(process.env.FERNET_SECRET);

/**
 * getSecurityMetadata() → object
 *
 * Present in the original file.  Returns a static descriptor
 * of the cryptographic primitives used — useful for logging,
 * documentation, and viva demonstrations.
 */
function getSecurityMetadata() {
  return {
    library:                 "custom (no external dependencies)",
    encryptionAlgorithm:     "AES-128-CBC",
    authenticationAlgorithm: "HMAC-SHA256",
    paddingScheme:           "PKCS7",
    encoding:                "URL-Safe Base64",
    keyManagement:           "Environment Variable (FERNET_SECRET)",
    tamperProtection:        true,
  };
}

module.exports = {

  // ── Original public API (exact same names & behaviour) ─────

  /** Encrypts any value; returns "" for falsy input. */
  encrypt: (text) => _service.encrypt(text),

  /** Decrypts a Fernet token; returns "" for falsy input. */
  decrypt: (tokenText) => _service.decrypt(tokenText),

  /** Cryptographic metadata descriptor (matches original). */
  getSecurityMetadata,

  // ── Low-level primitives (bonus; useful for unit tests) ────

  generateKey,
  deriveKeys,
  toBase64Url,
  fromBase64Url,
  pkcs7Pad,
  pkcs7Unpad,
  aesCbcEncrypt,
  aesCbcDecrypt,
  computeHmac,
  verifyHmac,
  encodeTimestamp,
  decodeTimestamp,
  currentUnixSeconds,
  checkTTL,
  assembleBinaryToken,
  parseBinaryToken,

  /**
   * createService(key) — factory for key-bound encrypt/decrypt pairs.
   * Useful when different parts of the app need different keys.
   */
  createService,
};


// ─────────────────────────────────────────────────────────────
// §13  QUICK SELF-TEST  (runs only when executed directly)
// ─────────────────────────────────────────────────────────────

if (require.main === module) {
  console.log("=== Fernet Custom Implementation — Self-Test ===\n");

  // Generate a fresh key
  const key = generateKey();
  console.log("Generated key  :", key);
  console.log("Key length     :", fromBase64Url(key).length, "bytes (expected 32)\n");

  // Create a bound service (mirrors original API)
  const service = createService(key);

  const testCases = [
    "Hello, Fernet!",
    "Special chars: <>&\"'",
    "Unicode: こんにちは 🔐",
    "Numbers: 42 | 3.14",
    "Empty-ish: ",
  ];

  for (const plaintext of testCases) {
    const token     = service.encrypt(plaintext);
    const recovered = service.decrypt(token);
    const ok        = recovered === plaintext;
    console.log(`[${ok ? "PASS" : "FAIL"}] "${plaintext}"`);
    if (!ok) {
      console.error(`      Expected : "${plaintext}"`);
      console.error(`      Got      : "${recovered}"`);
    }
  }

  // TTL test
  console.log("\n--- TTL expiry test ---");
  const oldToken = encrypt("old data", key, { timestamp: currentUnixSeconds() - 120 });
  try {
    decrypt(oldToken, key, { ttl: 60 });
    console.log("[FAIL] Should have thrown expiry error");
  } catch (e) {
    console.log("[PASS] Expired token correctly rejected:", e.message);
  }

  // Tamper detection
  console.log("\n--- Tamper detection test ---");
  const goodToken  = service.encrypt("sensitive");
  const tampered   = goodToken.slice(0, -4) + "XXXX"; // corrupt last chars
  try {
    service.decrypt(tampered);
    console.log("[FAIL] Should have thrown HMAC error");
  } catch (e) {
    console.log("[PASS] Tampered token correctly rejected:", e.message);
  }

  console.log("\n=== Self-Test Complete ===");
}