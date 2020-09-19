const functions         = require("firebase-functions");
const admin             = require("firebase-admin");
const crypto            = require('crypto');

admin.initializeApp();
const db = admin.firestore();



function sendNotification(args) {
    let payload = {
        notification: {
            title: args.title,
            body: args.description,
            sound: 'default',
            badge: '1',
            image: args.image
        },
        data: args.data
    };
    return admin.messaging().sendToTopic("MemeCreatorUpdate", payload)
}

module.exports.sendNotification = functions.https.onRequest((request, response) => {
    sendNotification({
        data: {
            title: request.body.title,
            image: request.body.image
        },
        title: request.body.title,
        image: request.body.image,
        description: request.body.description,
        callback: () => {}
    }).then(() => {
        response.send({
            message: "Notification sent",
            code: 0
        });
    }).catch(() => {
        response.send({
            message: "An error occurred",
            code: -1
        });
    });
});



// module.exports.dashboard = functions.https.onRequest((request, response) => {
//     if (request.method == "GET") {
//         db.collection("countries").get().then(snap => {
//             let selectBox = "<select name='country'>";
//             snap.docs.forEach(doc => {
//                 let country = doc.data();
//                 selectBox += `<option value="${country.code}">${country.name}</option>`;
//             });
//             selectBox += "</select>"
//             const html = `
// <style>
//     form {
//         width: 400px;
//         margin: auto;
//     }
//     input, textarea, select {
//         padding: 10px;
//         margin-bottom: 20px;
//         width: 100%;
//     }
//     textarea {
//         height: 300px;
//     }
// </style>
// <form action="" method="post">
//     <input name="title" type="text" placeholder="Template Pack Name"><br>
//     <input name="image" type="text" placeholder="Cover Image URL"><br>
//     <input name="path" type="text" placeholder="Folder Path"><br>
//     ${selectBox}
//     <br>
//     <textarea name="data" placeholder="Template Data"></textarea><br>
//     <input type="submit" value="Post">
// </form>
//             `;
//             response.send(html);
//         }).catch(err => {
//             response.send(err);
//         })
//     } else if (request.method === "POST") {
//         const data = JSON.parse(request.body.data);
//         const hash = crypto.createHash('md5').update(request.body.title).digest('hex');
//         db.collection("packs_meta").doc(hash).set({
//             title: request.body.title,
//             image: request.body.image,
//             path: request.body.path,
//             country: request.body.country
//         }).then(() => {
//             db.collection("packs").doc(hash).set({
//                 data: data
//             }).then(() => {
//                 sendNotification({
//                     data: {
//                         title: request.body.title,
//                         image: request.body.image
//                     },
//                     title: request.body.title,
//                     image: request.body.image,
//                     description: "New meme template is available",
//                     callback: () => {}
//                 });
//                 response.send("<pre>You've successfully posted!</pre>");
//             }).catch(err => {
//                 console.error(err);
//                 response.send(err);
//             });
//         }).catch(err => {
//             console.error(err);
//             response.send(err);
//         });        
//     } else {
//         response.send("<pre>Method not allowed<pre>");
//     }
// });

