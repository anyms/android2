const admin    = require("firebase-admin");
const crypto   = require('crypto');

const episodes = require("./oli_episodes");


var serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://infosec-feed.firebaseio.com"
});

const db = admin.firestore();

const keys = Object.keys(episodes);


keys.forEach(key => {
    const eps = episodes[key];
    const hash = crypto.createHash('md5').update(key).digest('hex')
    
    eps.forEach(async ep => {
        await db.collection("oli").doc("db").collection("channels").doc(hash).collection("episodes").add(ep).then(res => {
            console.log("Success: " + key);
        }).catch(err => {
            console.error(err);
        });
    });
});

