require("dotenv").config()

const {
    encrypt,
    decrypt
} = require("./encryptionService")

const encrypted =
    encrypt("Hello SourceForm")

console.log(encrypted)

console.log(
    decrypt(encrypted)
)