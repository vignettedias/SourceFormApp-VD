const mongoose =
    require("mongoose")

const validator =
    require("validator")

const UserSchema =
    new mongoose.Schema({

        name: {

            type: String,

            required: true,

            trim: true,

            minlength: 2,

            maxlength: 50
        },

        email: {

            type: String,

            required: true,

            unique: true,

            lowercase: true,

            trim: true,

            validate: [

                validator.isEmail,

                "Invalid email format"
            ]
        },

        password: {

            type: String,

            required: true,

            minlength: 8
        },

        authProvider: {

            type: String,

            enum: [

                "manual",

                "google",

                "github",

                "facebook"
            ],

            default: "manual"
        },

        role: {

            type: String,

            enum: [

                "user",

                "admin"
            ],

            default: "user"
        },

        failedLoginAttempts: {

            type: Number,

            default: 0
        },

        accountLockedUntil: {

            type: Date,

            default: null
        }

    }, {

        timestamps: true
    })

module.exports =
    mongoose.model(
        "User",
        UserSchema
    )
