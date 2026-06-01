const jwt =
    require("jsonwebtoken")

const admin =
    require("../firebaseAdmin")

const User =
    require("../models/UserModel")

async function verifyAuth(
    req,
    res,
    next
) {

    try {

        const authHeader =
            req.headers.authorization

        // ---------------------------------
        // HEADER CHECK
        // ---------------------------------

        if (

            !authHeader
            ||
            !authHeader.startsWith(
                "Bearer "
            )

        ) {

            return res
                .status(401)
                .json({

                    success: false,

                    message:
                        "Authorization token missing"
                })
        }

        const token =

            authHeader.split(
                "Bearer "
            )[1]

        // ---------------------------------
        // TRY MANUAL JWT FIRST
        // ---------------------------------

        try {

            const decodedJWT =

                jwt.verify(

                    token,

                    process.env.JWT_SECRET
                )

            const user =

                await User.findById(
                    decodedJWT.userId
                )

            if (!user) {

                return res
                    .status(401)
                    .json({

                        success: false,

                        message:
                            "User not found"
                    })
            }

            req.user = {

                id:
                    user.id,

                email:
                    user.email,

                authType:
                    "manual"
            }

            console.log(
                "JWT USER AUTHENTICATED:"
            )

            console.log(
                user.email
            )

            return next()

        } catch (jwtError) {

            console.log(
                "JWT verification failed."
            )

            console.log(
                "Trying Firebase token..."
            )
        }

        // ---------------------------------
        // TRY FIREBASE TOKEN
        // ---------------------------------

        try {

            const decodedFirebase =

                await admin
                    .auth()
                    .verifyIdToken(
                        token
                    )

            req.user = {

                id:
                    decodedFirebase.uid,

                email:
                    decodedFirebase.email,

                authType:
                    "firebase"
            }

            console.log(
                "FIREBASE USER AUTHENTICATED:"
            )

            console.log(
                decodedFirebase.email
            )

            return next()

        } catch (firebaseError) {

            console.log(
                "FIREBASE TOKEN ERROR"
            )

            console.log(
                firebaseError
            )

            return res
                .status(401)
                .json({

                    success: false,

                    message:
                        "Invalid or expired token"
                })
        }

    } catch (err) {

        console.log(
            "AUTH MIDDLEWARE ERROR"
        )

        console.log(
            err
        )

        return res
            .status(500)
            .json({

                success: false,

                message:
                    "Authentication Error"
            })
    }
}

module.exports =
    verifyAuth