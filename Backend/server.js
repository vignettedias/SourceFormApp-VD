global.crypto = require("crypto")

const bonjour =
    require("bonjour")()

const qrcode =
    require("qrcode-terminal")

const express =
    require("express")

const mongoose =
    require("mongoose")

const helmet =
    require("helmet")

const rateLimit =
    require("express-rate-limit")

const cors =
    require("cors")

const network =
    require("network")

require("dotenv").config()

// -------------------------------------
// ROUTES
// -------------------------------------

const formRoutes =
    require("./routes/formRoutes")

const authRoutes =
    require("./routes/authRoutes")

const app =
    express()

// -------------------------------------
// SECURITY CHECKS
// -------------------------------------

if (!process.env.JWT_SECRET) {

    console.log("================================")

    console.log(
        "FATAL ERROR: JWT_SECRET missing"
    )

    console.log("================================")

    process.exit(1)
}

if (!process.env.MONGO_URI) {

    console.log("================================")

    console.log(
        "FATAL ERROR: MONGO_URI missing"
    )

    console.log("================================")

    process.exit(1)
}

// -------------------------------------
// SECURITY MIDDLEWARE
// -------------------------------------

app.use(cors())

app.use(

    helmet({

        crossOriginResourcePolicy:
            false
    })
)

app.use(express.json({

    limit: "10mb"
}))

// -------------------------------------
// GLOBAL RATE LIMITER
// -------------------------------------

app.use(

    rateLimit({

        windowMs:
            15 * 60 * 1000,

        max: 100,

        standardHeaders: true,

        legacyHeaders: false,

        message: {

            success: false,

            message:
                "Too many requests"
        }
    })
)

// -------------------------------------
// REQUEST LOGGER
// -------------------------------------

app.use((req, res, next) => {

    console.log("================================")

    console.log("REQUEST")

    console.log(
        "METHOD:",
        req.method
    )

    console.log(
        "URL:",
        req.originalUrl
    )

    console.log(
        "IP:",
        req.ip
    )

    console.log(
        "TIME:",
        new Date()
    )

    console.log("================================")

    next()
})

// -------------------------------------
// API ROUTES
// -------------------------------------

app.use(

    "/api",

    formRoutes
)

app.use(

    "/auth",

    authRoutes
)

// -------------------------------------
// HEALTH CHECK
// -------------------------------------

app.get("/", (req, res) => {

    res.json({

        success: true,

        message:
            "SourceForm Backend Running"
    })
})

// -------------------------------------
// UNKNOWN ROUTES
// -------------------------------------

app.use((req, res) => {

    res.status(404)

        .json({

            success: false,

            message:
                "Route not found"
        })
})

// -------------------------------------
// GLOBAL ERROR HANDLER
// -------------------------------------

app.use((err, req, res, next) => {

    console.log("================================")

    console.log(
        "GLOBAL SERVER ERROR"
    )

    console.log(err)

    console.log("================================")

    res.status(500)

        .json({

            success: false,

            message:
                "Internal Server Error"
        })
})

// -------------------------------------
// MONGODB CONNECTION
// -------------------------------------

mongoose.connect(

    process.env.MONGO_URI
)

.then(() => {

    console.log("================================")

    console.log(
        "MongoDB Atlas Connected"
    )

    console.log("================================")
})

.catch((err) => {

    console.log("================================")

    console.log(
        "MongoDB Connection Error"
    )

    console.log(err)

    console.log("================================")
})

// -------------------------------------
// SERVER START
// -------------------------------------

const PORT =

    process.env.PORT || 5000

network.get_active_interface(

    (err, obj) => {

        if (err) {

            console.log(
                "Network Detection Error"
            )

            console.log(err)

            return
        }

        const localIP =
            obj.ip_address

        app.listen(

            PORT,

            "0.0.0.0",

            () => {

                const backendURL =

                    `http://${localIP}:${PORT}`

                console.log("================================")

                console.log(
                    "SERVER RUNNING"
                )

                console.log(
                    "WiFi IP:"
                )

                console.log(localIP)

                console.log("================================")

                console.log(
                    "Backend URL:"
                )

                console.log(
                    backendURL
                )

                console.log("================================")

                // ---------------------------------
                // NSD SERVICE
                // ---------------------------------

                bonjour.publish({

                    name:
                        "SourceForm Backend",

                    type:
                        "sourceform",

                    port:
                        PORT
                })

                console.log(
                    "NSD SERVICE PUBLISHED"
                )

                console.log("================================")

                console.log(
                    "SCAN QR BELOW"
                )

                qrcode.generate(

                    backendURL,

                    {

                        small: true
                    }
                )
            }
        )
    }
)
