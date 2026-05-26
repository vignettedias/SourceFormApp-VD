const mongoose = require('mongoose')

const FormSchema = new mongoose.Schema({

    name: {
        type: String,
        required: true,
        trim: true,
        maxlength: 100
    },

    email: {
        type: String,
        required: true,
        trim: true,
        maxlength: 100
    },

    phone: {
        type: String,
        required: true,
        trim: true,
        maxlength: 20
    },

    description: {
        type: String,
        required: true,
        trim: true,
        maxlength: 1000
    },

    filePath: {
        type: String,
        required: true
    }

}, {
    timestamps: true
})

module.exports =
    mongoose.model('Form', FormSchema)