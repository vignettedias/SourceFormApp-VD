global.crypto = require("crypto")

require("dotenv").config()
const path = require("path")
// -------------------------------------
// IMPORTS
// -------------------------------------

const express = require("express")
const helmet = require("helmet")
const rateLimit = require("express-rate-limit")
const cors = require("cors")

// -------------------------------------
// ROUTES
// -------------------------------------

const formRoutes = require("./routes/formRoutes")
const authRoutes = require("./routes/authRoutes")

// -------------------------------------
// APP INIT
// -------------------------------------

const app = express()

// -------------------------------------
// CLOUD RUN SETTINGS
// -------------------------------------

app.set("trust proxy", 1)

// -------------------------------------
// UPLOADS DIRECTORY
// -------------------------------------
const uploadsDir = path.join(__dirname, "uploads")
console.log("================================")
console.log("Uploads directory ready")
console.log(uploadsDir)
console.log("================================")

// -------------------------------------
// SECURITY CHECKS
// -------------------------------------

if (!process.env.JWT_SECRET) {

    console.log("================================")
    console.log("FATAL ERROR: JWT_SECRET missing")
    console.log("================================")

    process.exit(1)
}
// -------------------------------------
// SECURITY MIDDLEWARE
// -------------------------------------

app.use(cors())

app.use(
    helmet({
        crossOriginResourcePolicy: false
    })
)

app.use(
    express.json({
        limit: "10mb"
    })
)

// -------------------------------------
// RATE LIMITER
// -------------------------------------

app.use(
    rateLimit({

        windowMs: 15 * 60 * 1000,

        max: 100,

        standardHeaders: true,

        legacyHeaders: false,

        message: {

            success: false,

            message: "Too many requests"
        }
    })
)

// -------------------------------------
// REQUEST LOGGER
// -------------------------------------

app.use((req, res, next) => {

    console.log("================================")

    console.log("REQUEST")

    console.log("METHOD:", req.method)

    console.log("URL:", req.originalUrl)

    console.log("IP:", req.ip)

    console.log("TIME:", new Date())

    console.log("================================")

    next()
})

// -------------------------------------
// ROOT ROUTE
// -------------------------------------

app.get("/", (req, res) => {

    res.json({

        success: true,

        message: "SourceForm Cloud Backend Running"
    })
})

// -------------------------------------
// HEALTH CHECK
// -------------------------------------

app.get("/health", (req, res) => {

    res.status(200).json({

        success: true,

        status: "ok",

        service: "SourceForm Backend",

        uptime: process.uptime(),

        timestamp: new Date()
    })
})

// -------------------------------------
// API ROUTES
// -------------------------------------

app.use("/api", formRoutes)

app.use("/auth", authRoutes)

// -------------------------------------
// 404 HANDLER
// -------------------------------------

app.use((req, res) => {

    res.status(404).json({

        success: false,

        message: "Route not found"
    })
})

// -------------------------------------
// GLOBAL ERROR HANDLER
// -------------------------------------

app.use((err, req, res, next) => {

    console.log("================================")

    console.log("GLOBAL SERVER ERROR")

    console.log(err)

    console.log("================================")

    res.status(500).json({

        success: false,

        message: err.message || "Internal Server Error"
    })
})

// -------------------------------------
// PORT
// -------------------------------------

const PORT = process.env.PORT || 8080

// -------------------------------------
// START SERVER
// -------------------------------------

async function startServer() {

    try {

        app.listen(
            PORT,
            "0.0.0.0",
            () => {

                console.log("================================")

                console.log("SOURCEFORM CLOUD BACKEND RUNNING")

                console.log("PORT:", PORT)

                console.log("================================")

                console.log("Health Endpoint:")
                console.log("/health")

                console.log("================================")
            }
        )

    } catch (err) {

        console.log("================================")

        console.log("SERVER STARTUP ERROR")

        console.log(err)

        console.log("================================")

        process.exit(1)
    }
}

startServer()