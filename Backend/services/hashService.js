const crypto = require("crypto")

function generateSHA256(buffer) {

    return crypto
        .createHash("sha256")
        .update(buffer)
        .digest("hex")
}

module.exports = {
    generateSHA256
}