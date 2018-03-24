var exec = require('child_process').exec;

exports.exec = (cmd) => {
    exec(cmd,(err,stdout,stderr) => {
        if(err) {
            console.log('get weather api error:'+stderr);
        } else {
            /*
            the content of stdout is liking bellows：
            {"weatherinfo":{"city":"Hongkong","cityid":"101","temp":"3","WSE":"3","qy":"1019"}}
            */
            console.log(stdout);
        }
    })
}