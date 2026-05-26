const express =
    require("express")

const bcrypt =
    require("bcryptjs")

const jwt =
    require("jsonwebtoken")

const User =
    require("../models/UserModel")

const router =
    express.Router()

// -------------------------------------
// SIGNUP
// -------------------------------------

router.post(

    "/signup",

    async (
        req,
        res
    ) => {

        try {

            console.log("================================")

            console.log("SIGNUP REQUEST RECEIVED")

            console.log(req.body)

            console.log("================================")

            const {
                name,
                email,
                password
            } = req.body

            // ---------------------------------
            // VALIDATION
            // ---------------------------------

            if (

                !name
                ||
                !email
                ||
                !password

            ) {

                console.log(
                    "VALIDATION FAILED"
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
            // CHECK EXISTING USER
            // ---------------------------------

            const existingUser =

                await User.findOne({
                    email
                })

            if (existingUser) {

                console.log(
                    "EMAIL ALREADY EXISTS"
                )

                return res
                    .status(400)
                    .json({

                        success: false,

                        message:
                            "Email already registered"
                    })
            }

            console.log(
                "HASHING PASSWORD"
            )

            // ---------------------------------
            // HASH PASSWORD
            // ---------------------------------

            const hashedPassword =

                await bcrypt.hash(
                    password,
                    12
                )

            console.log(
                "PASSWORD HASHED"
            )

            // ---------------------------------
            // CREATE USER
            // ---------------------------------

            const newUser =

                new User({

                    name,

                    email,

                    password:
                        hashedPassword
                })

            console.log(
                "SAVING USER"
            )

            await newUser.save()

            console.log(
                "USER SAVED"
            )

            // ---------------------------------
            // JWT TOKEN
            // ---------------------------------

            console.log(
                "CREATING JWT"
            )

            const token =

                jwt.sign(

                    {
                        userId:
                            newUser._id
                    },

                    process.env.JWT_SECRET,

                    {
                        expiresIn: "7d"
                    }
                )

            console.log(
                "JWT CREATED"
            )

            // ---------------------------------
            // SUCCESS
            // ---------------------------------

            return res
                .status(201)
                .json({

                    success: true,

                    token,

                    user: {

                        id:
                            newUser._id,

                        name:
                            newUser.name,

                        email:
                            newUser.email
                    }
                })

        } catch (err) {

            console.log("================================")

            console.log(
                "SIGNUP ROUTE ERROR"
            )

            console.log(err)

            console.log(
                err.message
            )

            console.log(
                err.stack
            )

            console.log("================================")

            return res
                .status(500)
                .json({

                    success: false,

                    message:
                        "Signup failed"
                })
        }
    }
)

// -------------------------------------
// LOGIN
// -------------------------------------

router.post(

    "/login",

    async (
        req,
        res
    ) => {

        try {

            console.log("================================")

            console.log("LOGIN REQUEST")

            console.log(req.body)

            console.log("================================")

            const {
                email,
                password
            } = req.body

            const user =

                await User.findOne({
                    email
                })

            if (!user) {

                return res
                    .status(401)
                    .json({

                        success: false,

                        message:
                            "Invalid credentials"
                    })
            }

            const isMatch =

                await bcrypt.compare(

                    password,

                    user.password
                )

            if (!isMatch) {

                return res
                    .status(401)
                    .json({

                        success: false,

                        message:
                            "Invalid credentials"
                    })
            }

            const token =

                jwt.sign(

                    {
                        userId:
                            user._id
                    },

                    process.env.JWT_SECRET,

                    {
                        expiresIn: "7d"
                    }
                )

            return res
                .status(200)
                .json({

                    success: true,

                    token,

                    user: {

                        id:
                            user._id,

                        name:
                            user.name,

                        email:
                            user.email
                    }
                })

        } catch (err) {

            console.log("================================")

            console.log(
                "LOGIN ROUTE ERROR"
            )

            console.log(err)

            console.log(
                err.message
            )

            console.log(
                err.stack
            )

            console.log("================================")

            return res
                .status(500)
                .json({

                    success: false,

                    message:
                        "Login failed"
                })
        }
    }
)

module.exports =
    router
