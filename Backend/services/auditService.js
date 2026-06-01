const admin = require("../firebaseAdmin")

async function logSecurityEvent(
    eventType,
    userEmail,
    details = {}
) {

    try {

        await admin
            .firestore()
            .collection("security_logs")
            .add({

                eventType,

                userEmail,

                details,

                timestamp:
                    admin.firestore.FieldValue.serverTimestamp()
            })

    } catch (err) {

        console.error(
            "Audit Log Error:",
            err.message
        )
    }
}

module.exports = {
    logSecurityEvent
}