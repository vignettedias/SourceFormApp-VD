const fernet = require("fernet")

const secret = new fernet.Secret(
    process.env.FERNET_SECRET
)

function encrypt(text) {

    if (!text) return ""

    const token =
        new fernet.Token({
            secret
        })

    return token.encode(
        String(text)
    )
}

function decrypt(tokenText) {

    if (!tokenText) return ""

    const token =
        new fernet.Token({
            secret,
            token: tokenText,
            ttl: 0
        })

    return token.decode()
}

module.exports = {
    encrypt,
    decrypt
}