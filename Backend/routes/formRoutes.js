const verifyAuth =
    require("../middleware/authMiddleware")

const express =
    require("express")

const multer =
    require("multer")

const path =
    require("path")

const crypto =
    require("crypto")

const fs =
    require("fs")

const rateLimit =
    require("express-rate-limit")
const admin =
    require("../firebaseAdmin")
const bucket =
    admin.storage().bucket()
const router =
    express.Router()

// -------------------------------------
// UPLOAD RATE LIMITER
// -------------------------------------

const uploadLimiter =
    rateLimit({

        windowMs:
            15 * 60 * 1000,

        max: 10,

        message: {

            success: false,

            message:
                "Too many upload attempts. Try again later."
        },

        standardHeaders: true,

        legacyHeaders: false
    })

// -------------------------------------
// SAFE FILENAME GENERATOR
// -------------------------------------

function generateSafeFileName(
    originalName
) {

    const extension =

        path.extname(
            originalName
        ).toLowerCase()

    const randomName =

        crypto
            .randomBytes(16)
            .toString("hex")

    return `${Date.now()}_${randomName}${extension}`
}

// -------------------------------------
// INPUT SANITIZATION
// -------------------------------------

function sanitizeInput(input) {

    return String(input)

        .trim()

        .replace(/[<>$]/g, "")
}

// -------------------------------------
// EMAIL VALIDATION
// -------------------------------------

function isValidEmail(email) {

    const regex =

        /^[^\s@]+@[^\s@]+\.[^\s@]+$/

    return regex.test(email)
}

// -------------------------------------
// PHONE VALIDATION
// -------------------------------------

function isValidPhone(phone) {

    const regex =

        /^[0-9]{10}$/

    return regex.test(phone)
}

// -------------------------------------
// WORD COUNT
// -------------------------------------

function countWords(text) {

    return text

        .trim()

        .split(/\s+/)

        .filter(
            word =>
                word.length > 0
        )

        .length
}

// -------------------------------------
// SUSPICIOUS ACTIVITY LOGGER
// -------------------------------------

function logSuspiciousActivity(
    reason,
    req
) {

    console.log(
        "================================"
    )

    console.log(
        "SUSPICIOUS ACTIVITY DETECTED"
    )

    console.log(
        "Reason:",
        reason
    )

    console.log(
        "IP:",
        req.ip
    )

    console.log(
        "Time:",
        new Date()
            .toISOString()
    )

    console.log(
        "================================"
    )
}

// -------------------------------------
// FILE SIGNATURE VALIDATION
// -------------------------------------

function validateFileSignature(
    filePath,
    extension
) {

    try {

        const fileBuffer =

            fs.readFileSync(
                filePath
            )

        // ---------------------------------
        // PDF
        // ---------------------------------

        if (extension === ".pdf") {

            const pdfHeader =

                fileBuffer
                    .slice(0, 4)
                    .toString()

            return pdfHeader === "%PDF"
        }

        // ---------------------------------
        // DOC / DOCX
        // ---------------------------------

        if (

            extension === ".doc"
            ||
            extension === ".docx"

        ) {

            const zipHeader =

                fileBuffer
                    .slice(0, 2)
                    .toString("hex")

            return zipHeader === "504b"
        }

        return false

    } catch (err) {

        console.log(
            "SIGNATURE VALIDATION ERROR"
        )

        console.log(err)

        return false
    }
}

// -------------------------------------
// EXECUTABLE SIGNATURE DETECTION
// -------------------------------------

function containsExecutableSignature(
    filePath
) {

    try {

        const fileBuffer =

            fs.readFileSync(
                filePath
            )

        const hexHeader =

            fileBuffer
                .slice(0, 2)
                .toString("hex")

        // MZ executable signature

        return hexHeader === "4d5a"

    } catch (err) {

        console.log(
            "EXECUTABLE CHECK ERROR"
        )

        console.log(err)

        return true
    }
}

// -------------------------------------
// ALLOWED TYPES
// -------------------------------------

const allowedExtensions = [

    ".pdf",

    ".doc",

    ".docx"
]

const allowedMimeTypes = [

    "application/pdf",

    "application/msword",

    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

    "application/octet-stream",

    "*/*"
]
// -------------------------------------
// CLOUD RUN UPLOAD DIRECTORY
// -------------------------------------

const uploadsDir = "/tmp/uploads"

if (!fs.existsSync(uploadsDir)) {

    fs.mkdirSync(
        uploadsDir,
        {
            recursive: true
        }
    )
}

// -------------------------------------
// MULTER STORAGE
// -------------------------------------

const storage =
    multer.diskStorage({

        destination: (
            req,
            file,
            cb
        ) => {

            cb(
                null,
                uploadsDir
            )
        },

        filename: (
            req,
            file,
            cb
        ) => {

            const safeName =
                generateSafeFileName(
                    file.originalname
                )

            cb(
                null,
                safeName
            )
        }
    })

// -------------------------------------
// MULTER CONFIG
// -------------------------------------

const upload =
    multer({

        storage: storage,

        limits: {

            fileSize:
                5 * 1024 * 1024
        },

        fileFilter: (
            req,
            file,
            cb
        ) => {

            console.log(
                "FILE MIME TYPE:"
            )

            console.log(
                file.mimetype
            )

            console.log(
                "FILE ORIGINAL NAME:"
            )

            console.log(
                file.originalname
            )

            const extension =

                path.extname(
                    file.originalname
                ).toLowerCase()

            const validExtension =

                allowedExtensions.includes(
                    extension
                )

            const validMime =

                allowedMimeTypes.includes(
                    file.mimetype
                )

            if (

                validExtension
                &&
                validMime

            ) {

                console.log(
                    "UPLOAD ACCEPTED"
                )

                cb(
                    null,
                    true
                )

            } else {

                logSuspiciousActivity(
                    "Blocked invalid upload type",
                    req
                )

                console.log(
                    "UPLOAD BLOCKED"
                )

                console.log(
                    "Mime:",
                    file.mimetype
                )

                console.log(
                    "Extension:",
                    extension
                )

                cb(

                    new Error(
                        "Only PDF/DOC/DOCX allowed"
                    )
                )
            }
        }
    })

// -------------------------------------
// SUBMIT ROUTE
// -------------------------------------

router.post(

    "/submit",

    uploadLimiter,

    verifyAuth,

    upload.single("file"),

    async (
        req,
        res
    ) => {

        try {

            // ---------------------------------
            // FILE REQUIRED
            // ---------------------------------

            if (!req.file) {

                logSuspiciousActivity(
                    "Upload without file",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "File upload required"
                    })
            }

            // ---------------------------------
            // EXECUTABLE CHECK
            // ---------------------------------

            if (

                containsExecutableSignature(
                    req.file.path
                )

            ) {

                fs.unlinkSync(
                    req.file.path
                )

                logSuspiciousActivity(
                    "Executable upload attempt",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Executable files are blocked"
                    })
            }

            // ---------------------------------
            // SIGNATURE CHECK
            // ---------------------------------

            const extension =

                path.extname(
                    req.file.originalname
                ).toLowerCase()

            const validSignature =

                validateFileSignature(
                    req.file.path,
                    extension
                )

            if (!validSignature) {

                fs.unlinkSync(
                    req.file.path
                )

                logSuspiciousActivity(
                    "Invalid file signature",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Invalid file signature"
                    })
            }

            // ---------------------------------
            // SANITIZE INPUTS
            // ---------------------------------

            const name =

                sanitizeInput(
                    req.body.name
                )

            const email =

                sanitizeInput(
                    req.body.email
                )

            const phone =

                sanitizeInput(
                    req.body.phone
                )

            const description =

                sanitizeInput(
                    req.body.description
                )

            // ---------------------------------
            // REQUIRED FIELDS
            // ---------------------------------

            if (

                !name
                ||
                !email
                ||
                !phone
                ||
                !description

            ) {

                logSuspiciousActivity(
                    "Missing required fields",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "All fields required"
                    })
            }

            // ---------------------------------
            // NAME VALIDATION
            // ---------------------------------

            if (
                name.length > 50
            ) {

                logSuspiciousActivity(
                    "Oversized name field",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Name exceeds 50 characters"
                    })
            }

            // ---------------------------------
            // EMAIL VALIDATION
            // ---------------------------------

            if (
                !isValidEmail(email)
            ) {

                logSuspiciousActivity(
                    "Invalid email format",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Invalid email format"
                    })
            }

            // ---------------------------------
            // PHONE VALIDATION
            // ---------------------------------

            if (
                !isValidPhone(phone)
            ) {

                logSuspiciousActivity(
                    "Invalid phone number",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Phone must contain exactly 10 digits"
                    })
            }

            // ---------------------------------
            // DESCRIPTION VALIDATION
            // ---------------------------------

            const wordCount =

                countWords(
                    description
                )

            if (
                wordCount > 140
            ) {

                logSuspiciousActivity(
                    "Oversized description",
                    req
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Description exceeds 140 words"
                    })
            }

            // ---------------------------------
            // VERIFIED USER LOG
            // ---------------------------------

            console.log(
                "================================"
            )

            console.log(
                "AUTHENTICATED USER:"
            )

            console.log(
                req.user.email
            )

            console.log(
                "AUTH TYPE:"
            )

            console.log(
                req.user.authType
            )

            console.log(
                "================================"
            )

 await admin
    .firestore()
    .collection("forms")
    .add({

        name,

        email,

        phone,

        description,

        fileName:
            req.file.filename,

        authEmail:
            req.user.email,

        authType:
            req.user.authType,

        uploadedAt:
            new Date()
    })

console.log(
    "Firestore document created"
)
            // ---------------------------------
            // SUCCESS
            // ---------------------------------

            return res
                .status(201)
                .json({

                    success: true,

                    message:
                        "Form submitted securely"
                })

        } catch (err) {

            console.log(
                "================================"
            )

            console.log(
                "SECURE ROUTE ERROR"
            )

            console.log(err)

            console.log(
                "================================"
            )

            return res
                .status(500)
                .json({

                    success: false,

                    message:
                        "Internal server error"
                })
        }
    }
)

module.exports = router
