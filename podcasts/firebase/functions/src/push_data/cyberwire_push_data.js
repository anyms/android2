const admin    = require("firebase-admin");
const crypto   = require('crypto');
const fs       = require('fs');
const readline = require('readline');

const episodes = require("./cyberwire_episodes");


var serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://infosec-feed.firebaseio.com"
});

const db = admin.firestore();

const keys = Object.keys(episodes);
const sent = [];


async function main() {
    const fileStream = fs.createReadStream('sent.txt');
    const rl = readline.createInterface({
        input: fileStream,
        crlfDelay: Infinity
    });
    for await (const line of rl) {
        sent.push(line.trim());
    }

    keys.forEach(async key => {
        const eps = episodes[key];
        const hash = crypto.createHash('md5').update(key).digest('hex')
        
        eps.forEach(async ep => {
            if (sent.indexOf(ep.audio) === -1) {
                await db.collection("cyberwire").doc("db").collection("channels").doc(hash).collection("episodes").add(ep).then(res => {
                    fs.appendFileSync("sent.txt", ep.audio + "\n");
                    console.log("Success: " + ep.audio);
                }).catch(err => {
                    console.error(err);
                });
            }
        });
    });

}

main();
