const fernet = require("fernet");

/*
|--------------------------------------------------------------------------
| FERNET ENCRYPTION MODULE
|--------------------------------------------------------------------------
|
| PURPOSE:
| Securely encrypt and decrypt sensitive data using the Fernet standard.
|
| FERNET INTERNALLY USES:
|
| 1. AES-128-CBC
|    - Encrypts plaintext into ciphertext.
|    - Provides confidentiality.
|
| 2. PKCS7 Padding
|    - Pads plaintext to AES block boundaries.
|
| 3. HMAC-SHA256
|    - Generates a cryptographic signature.
|    - Detects tampering or modification.
|
| 4. URL-Safe Base64 Encoding
|    - Converts binary token into a transport-safe string.
|
| TOKEN STRUCTURE:
|
| Version Byte
| Timestamp
| Initialization Vector (IV)
| AES-128-CBC Ciphertext
| HMAC-SHA256 Signature
|
| SECURITY PROPERTIES:
|
| ✓ Confidentiality
| ✓ Integrity
| ✓ Authentication
| ✓ Tamper Detection
|
|--------------------------------------------------------------------------
*/


/*
|--------------------------------------------------------------------------
| STEP 1 : LOAD SECRET KEY
|--------------------------------------------------------------------------
|
| Fernet expects a secret key from environment variables.
| This key is never hardcoded into source code.
|
| Example:
| FERNET_SECRET=generated_fernet_secret_here
|
|--------------------------------------------------------------------------
*/

const secret = new fernet.Secret(
    process.env.FERNET_SECRET
);


/*
|--------------------------------------------------------------------------
| STEP 2 : INPUT NORMALIZATION
|--------------------------------------------------------------------------
|
| Converts all incoming values into strings before encryption.
|
| Examples:
| 123      -> "123"
| true     -> "true"
| "hello"  -> "hello"
|
|--------------------------------------------------------------------------
*/

function normalizeInput(value) {

    if (!value) {
        return "";
    }

    return String(value);
}


/*
|--------------------------------------------------------------------------
| STEP 3 : CREATE ENCRYPTION TOKEN
|--------------------------------------------------------------------------
|
| Creates a Fernet token object.
|
| Internally prepares:
| - AES encryption context
| - HMAC signing context
| - Timestamp handling
|
|--------------------------------------------------------------------------
*/

function createEncryptionToken() {

    return new fernet.Token({
        secret: secret
    });
}


/*
|--------------------------------------------------------------------------
| STEP 4 : PERFORM ENCRYPTION
|--------------------------------------------------------------------------
|
| INTERNAL FERNET OPERATIONS:
|
| 1. Generate current timestamp.
|
| 2. Generate random 128-bit IV.
|
| 3. Apply PKCS7 padding.
|
| 4. Encrypt padded plaintext using AES-128-CBC.
|
| 5. Generate HMAC-SHA256 signature.
|
| 6. Assemble Fernet token structure.
|
| 7. Convert token to URL-safe Base64.
|
|--------------------------------------------------------------------------
*/

function performEncryption(token, plaintext) {

    return token.encode(plaintext);
}


/*
|--------------------------------------------------------------------------
| COMPLETE ENCRYPTION WORKFLOW
|--------------------------------------------------------------------------
*/

function encrypt(text) {

    const normalizedText =
        normalizeInput(text);

    if (!normalizedText) {
        return "";
    }

    const token =
        createEncryptionToken();

    const encryptedToken =
        performEncryption(
            token,
            normalizedText
        );

    return encryptedToken;
}


/*
|--------------------------------------------------------------------------
| STEP 5 : CREATE DECRYPTION TOKEN
|--------------------------------------------------------------------------
|
| Creates a Fernet token object configured for decoding.
|
|--------------------------------------------------------------------------
*/

function createDecryptionToken(tokenText) {

    return new fernet.Token({
        secret: secret,
        token: tokenText,
        ttl: 0
    });
}


/*
|--------------------------------------------------------------------------
| STEP 6 : PERFORM DECRYPTION
|--------------------------------------------------------------------------
|
| INTERNAL FERNET OPERATIONS:
|
| 1. Decode Base64 token.
|
| 2. Extract version byte.
|
| 3. Extract timestamp.
|
| 4. Extract IV.
|
| 5. Verify HMAC-SHA256 signature.
|
| 6. Reject token if tampered.
|
| 7. AES-128-CBC decrypt ciphertext.
|
| 8. Remove PKCS7 padding.
|
| 9. Return original plaintext.
|
|--------------------------------------------------------------------------
*/

function performDecryption(token) {

    return token.decode();
}


/*
|--------------------------------------------------------------------------
| COMPLETE DECRYPTION WORKFLOW
|--------------------------------------------------------------------------
*/

function decrypt(tokenText) {

    if (!tokenText) {
        return "";
    }

    const token =
        createDecryptionToken(
            tokenText
        );

    const decryptedText =
        performDecryption(
            token
        );

    return decryptedText;
}


/*
|--------------------------------------------------------------------------
| SECURITY INFORMATION
|--------------------------------------------------------------------------
|
| Returns cryptographic details used by Fernet.
| Useful for logging, documentation, and viva demonstrations.
|
|--------------------------------------------------------------------------
*/

function getSecurityMetadata() {

    return {
        library: "fernet",
        encryptionAlgorithm: "AES-128-CBC",
        authenticationAlgorithm: "HMAC-SHA256",
        paddingScheme: "PKCS7",
        encoding: "URL-Safe Base64",
        keyManagement: "Environment Variable",
        tamperProtection: true
    };
}


/*
|--------------------------------------------------------------------------
| EXPORTS
|--------------------------------------------------------------------------
*/

module.exports = {
    encrypt,
    decrypt,
    getSecurityMetadata
};