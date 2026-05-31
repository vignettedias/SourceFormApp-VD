const admin = require("firebase-admin")

admin.initializeApp({
    storageBucket: "sourceformcloud.firebasestorage.app"
})

module.exports = admin