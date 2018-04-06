var {put,get,del,find} = require("./dbHelper")
var moment = require("moment");
const artivalPrefix = "artical_"
const authorPrefix = "author_"

//公众号id相关
/**
 * 查询公众号信息
 * @param {*} authorId 
 */
module.exports.getAuthorInfo = getAuthorInfo = (authorId) => {
    let key = `${authorPrefix}${authorId}`;
    return new Promise((reslove,reject)=>{
        get(key,(err,val)=>{
            if(err!=null) {
                reject(err);
            }else {
                reslove(JSON.parse(val));
            }
        })
    });
}

/**
 * 保存公众号信息
 * @param {} param0 
 */
module.exports.saveAuthorInfo = saveAuthorInfo = ({author,authorId,authorDesc,articals,...other}) => {
    let key = `${authorPrefix}${authorId}`;
    let value = JSON.stringify({
        author,
        authorId,
        authorDesc,
        articals,
        ...other
    });
    put(key,value,(err)=>{
        if(err!=null) {
            console.log(err);
        }
    })
}

// 文章相关
module.exports.newArtical = (author,articalName,articalTime,articalUrl) => {
    let key = `${artivalPrefix}${author}_${articalName}_${articalTime}`;
    let value = JSON.stringify({
        author: author,
        name: articalName,
        time:articalTime,
        url: articalUrl
    });
    put(key,value,(err)=>{
        if(err) {
            console.log('err1',err);
        }else {
            getAuthorInfo(author).then((authorInfo)=>{
                const dateStart = authorInfo.dateStart || '';
                const dateEnd = authorInfo.dateEnd || '';
                const newtimem = moment(articalTime,"YYYY年MM月DD日");
                const timestamp = newtimem.format("YYYYMMDD");
                if(!dateStart || moment(dateStart,"YYYY年MM月DD日").isBefore(newtimem)) {
                    authorInfo.dateStart = articalTime;
                }
                if(!dateEnd || moment(dateEnd,"YYYY年MM月DD日").isAfter(newtimem)) {
                    authorInfo.dateEnd = articalTime;
                }
                if(!authorInfo.articals) {
                    authorInfo.articals = {};
                }
                if(!authorInfo.articals[timestamp]) {
                    authorInfo.articals[timestamp]=[];
                }
                authorInfo.articals[timestamp].push(key);
                authorInfo.articals[timestamp] = [...new Set(authorInfo.articals[timestamp])];
                saveAuthorInfo(authorInfo);
            }).catch((err)=>{
                console.log('err2',err);
                let authorInfo = {
                    authorId:author
                }
                const newtimem = moment(articalTime,"YYYY年MM月DD日");
                const timestamp = newtimem.format("YYYYMMDD");
                authorInfo.dateStart = articalTime;
                authorInfo.dateEnd = articalTime;
                if(!authorInfo.articals) {
                    authorInfo.articals = {};
                }
                if(!authorInfo.articals[timestamp]) {
                    authorInfo.articals[timestamp]=[];
                }
                authorInfo.articals[timestamp].push(key);
                authorInfo.articals[timestamp] = [...new Set(authorInfo.articals[timestamp])];
                saveAuthorInfo(authorInfo);
            });
        }
    })
    
}

module.exports.updateArtical = (author,articalName,articalTime,articalUrl) => {
    let key = `${artivalPrefix}${author}_${articalName}_${articalTime}`;
    let value = JSON.stringify({
        author: author,
        name: articalName,
        time:articalTime,
        url: articalUrl
    });
    put(key,value,(err)=>{
        if(err!=null) {
            console.log(err);
        }
    })
}

module.exports.getArticalByKey = (key) => {
    return new Promise((reslove,reject)=>{
        get(key,(err,val)=>{
            if(err!=null) {
                reject(err);
            }else {
                reslove(JSON.parse(val));
            }
        })
    });
}

