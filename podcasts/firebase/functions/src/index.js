const functions         = require("firebase-functions");
const admin             = require("firebase-admin");
const crypto            = require('crypto');
const _                 = require('domlang');

const EpisodeFetcher    = require("./spiders/EpisodeFetcher");
const oliChannels       = require("./data/oli_channels");
const cyberwireChannels = require("./data/cyberwire_channels");


admin.initializeApp();
const db = admin.firestore();


/* methods */

function oliSendNotification(args) {
    db.collection("oli").doc("db").collection("tokens").get().then(snapshot => {
        const docs = snapshot.docs.map(doc => {
            return {token: doc.data().token, id: doc.id};
        });
        const tokens = docs.map(doc => doc.token);
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

        let count = 0;
        admin.messaging().sendToDevice(tokens, payload).then(res => {
            const devices = res.results;

            devices.forEach(device => {
                if (device.error !== undefined) {
                    console.error("Device", "is dead : " + docs[count].id);
                    db.collection("oli").doc("db").collection("tokens").doc(docs[count].id).delete();
                }
                count++;
            });
            args.callback();
        }).catch(err => {
            args.callback();
            console.error("Device", err);
        });
    }).catch(err => {
        args.callback();
        console.error(err);
    });
}


function cyberwireSendNotification(args) {
    console.log(args.data);
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
    admin.messaging().sendToTopic("Cyberwire", payload).then(res => {
        console.log("Notification sent")
        args.callback();
    }).catch(err => {
        args.callback();
        console.error(err);
    });
}


/* HTTP METHODS */

module.exports.oliAddToken = functions.https.onRequest((request, response) => {    
    db.collection("oli").doc("db").collection("tokens").add({
        "token": request.query.token
    }).then(() => {
        response.send({
            code: 0,
            message: "success"
        });
    }).catch(err => {
        response.send({
            code: -1,
            message: err.message
        });
    });
});


/* JOBS */

module.exports.oliPodcastUpdater = functions.pubsub
    .schedule("0 */12 * * *").onRun(async context => {
        return await new Promise((resolve, reject) => {
            const updatedEps = [];
            let count = 0;

            oliChannels.forEach(async channel => {
                const hash = crypto.createHash('md5').update(channel.url).digest('hex');
                const snapshot = await db.collection("oli").doc("db").collection("channels").doc(hash).collection("episodes").get();
                const docsLength = snapshot.docs.length;
                await new EpisodeFetcher(channel).run(ep => {
                    console.log(ep);

                    let hasEp = false;
                    for (let i = 0; i < docsLength; i++) {
                        if (snapshot.docs[i].data().audio == ep.audio) {
                            hasEp = true;
                            break;
                        }
                    }

                    if (!hasEp) {
                        const tmpEp = _.clone(ep);
                        tmpEp.image = channel.image;
                        tmpEp.channelName = channel.title;
                        tmpEp.channelUrl = channel.url;
                        updatedEps.push(tmpEp);
                        db.collection("oli").doc("db").collection("channels").doc(hash).collection("episodes").add(ep).then(res => {
                            console.log("Success:", channel);
                        }).catch(err => {
                            console.error(err);
                        });
                    }
                });

                if (count >= oliChannels.length - 1) {
                    // const notiEp = _.random(updatedEps);
                    // console.log("CHECK", notiEp);
                    // if (notiEp !== undefined) {
                        // oliSendNotification({
                        //     title: notiEp.title,
                        //     description: notiEp.channelName,
                        //     image: notiEp.image,
                        //     data: notiEp,
                        //     callback: () => {

                        //     }
                        // });
                    // }

                    for (let i = 0; i < updatedEps.length; i++) {
                        oliSendNotification({
                            title: updatedEps[i].title,
                            description: updatedEps[i].channelName,
                            image: updatedEps[i].image,
                            data: updatedEps[i],
                            callback: () => {

                            }
                        });
                    }
                    if (updatedEps.length == 0) {
                        reject();
                    } else {
                        resolve();
                    }
                }
                count++;
            });
        });
    });



module.exports.cyberwirePodcastUpdater = functions.pubsub
    .schedule("0 */12 * * *").onRun(async context => {
        return await new Promise((resolve, reject) => {
            const updatedEps = [];
            let count = 0;

            cyberwireChannels.forEach(async channel => {
                const hash = crypto.createHash('md5').update(channel.url).digest('hex');
                const snapshot = await db.collection("cyberwire").doc("db").collection("channels").doc(hash).collection("episodes").get();
                const docsLength = snapshot.docs.length;
                await new EpisodeFetcher(channel).run(ep => {
                    console.log(ep);

                    let hasEp = false;
                    for (let i = 0; i < docsLength; i++) {
                        if (snapshot.docs[i].data().audio == ep.audio) {
                            hasEp = true;
                            break;
                        }
                    }

                    if (!hasEp) {
                        const tmpEp = _.clone(ep);
                        tmpEp.image = channel.image;
                        tmpEp.channelName = channel.title;
                        tmpEp.channelUrl = channel.url;
                        updatedEps.push(tmpEp);
                        db.collection("cyberwire").doc("db").collection("channels").doc(hash).collection("episodes").add(ep).then(res => {
                            console.log("Success:", channel);
                        }).catch(err => {
                            console.error(err);
                        });
                    }
                });

                if (count >= cyberwireChannels.length - 1) {
                    // const notiEp = _.random(updatedEps);
                    // if (notiEp !== undefined) {
                        // cyberwireSendNotification({
                        //     title: notiEp.title,
                        //     description: notiEp.channelName,
                        //     image: notiEp.image,
                        //     data: notiEp,
                        //     callback: () => {

                        //     }
                        // });
                    // }

                    for (let i = 0; i < updatedEps.length; i++) {
                        cyberwireSendNotification({
                            title: updatedEps[i].title,
                            description: updatedEps[i].channelName,
                            image: updatedEps[i].image,
                            data: updatedEps[i],
                            callback: () => {

                            }
                        });
                    }
                    if (updatedEps.length == 0) {
                        reject();
                    } else {
                        resolve();
                    }
                }
                count++;
            });
        });
    });
