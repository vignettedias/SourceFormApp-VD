const admin = require("firebase-admin")

admin.initializeApp({
    storageBucket: "sourceformcloud.firebasestorage.app"
})

console.log("Firebase Admin Initialized")
console.log("Storage Bucket:", admin.app().options.storageBucket)

module.exports = admin