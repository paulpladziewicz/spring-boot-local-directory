db.getCollection('subscribers').find({}).forEach(function(doc) {
    let emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    let localPart = doc.email.split('@')[0];
    let ipPattern = /^(?:\d{1,3}\.){3}\d{1,3}$/;

    let emailsToRemove = [];

    if (!emailRegex.test(doc.email) || ipPattern.test(localPart)) {
        emailsToRemove.push(doc.email);
        db.getCollection('subscribers').deleteOne({ _id: doc._id });
        print("Deleted document with _id: " + doc._id + " due to invalid or suspicious email: " + doc.email);
    }
});