const admin = require("firebase-admin");

// Uses Cloud Run's attached service account automatically
admin.initializeApp();

module.exports = admin;