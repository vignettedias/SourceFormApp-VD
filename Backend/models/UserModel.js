const admin =
    require("../firebaseAdmin")

const db =
    admin.firestore()

class UserModel {

    static async findByEmail(email) {

        const snapshot =

            await db
                .collection("users")
                .where(
                    "email",
                    "==",
                    email
                )
                .limit(1)
                .get()

        if (snapshot.empty)
            return null

        const doc =
            snapshot.docs[0]

        return {

            id: doc.id,

            ...doc.data()
        }
    }

    static async findById(id) {

        const doc =

            await db
                .collection("users")
                .doc(id)
                .get()

        if (!doc.exists)
            return null

        return {

            id: doc.id,

            ...doc.data()
        }
    }

    static async create(data) {

        const ref =

            await db
                .collection("users")
                .add(data)

        return {

            id: ref.id,

            ...data
        }
    }
}

module.exports =
    UserModel