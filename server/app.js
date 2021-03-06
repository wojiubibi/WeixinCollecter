var express = require('express');
var {newArtical,updateArtical,getAuthorInfo} = require("./db/DbAdapter")

var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');

var index = require('./routes/index');
var users = require('./routes/users');

var expressWs = require('express-ws')(express());  

var app = expressWs.app;

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

// uncomment after placing your favicon in /public
//app.use(favicon(path.join(__dirname, 'public', 'favicon.ico')));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// app.use('/', index);
// app.use('/users', users);

let author = "lengtoo";
let currentArtical = null;

app.get('/ws',function(req,res,next){
  res.end();
})
app.ws('/ws', function(ws, req) {  
  ws.on('ping',function(msg){
    console.log('ping received');
    ws.pong();
  });
  ws.on('pong',function(msg){
    
  });
  ws.on('message', function(msg) {  
    console.log(msg);  
    try{
      var data = JSON.parse(msg);
      if(data.type === 'cmd' && data.content) {
        var content = data.content;
        currentArtical = {
          name:data.data[0],
          time:data.data[1]
        }
        newArtical(author,data.data[0],data.data[1]);
        require("./tools/process").exec(content);
      }else if(data.type === 'insert' && data.data) {
        if(currentArtical) {
          updateArtical(author,currentArtical.name,currentArtical.time,data.data);
        }
      }
    }catch(e) {
      // console.log(e);
    }
    ws.send('echo:');  
  });  
})  

// getAuthorInfo(author).then(authorInfo => {
//   const articals = authorInfo.articals;
//   console.log(articals);
// }).catch(err => {
//   console.log(err);
// })

// // catch 404 and forward to error handler
// app.use(function(req, res, next) {
//   var err = new Error('Not Found');
//   err.status = 404;
//   next(err);
// });

// error handler
// app.use(function(err, req, res, next) {
//   // set locals, only providing error in development
//   res.locals.message = err.message;
//   res.locals.error = req.app.get('env') === 'development' ? err : {};

//   // render the error page
//   res.status(err.status || 500);
//   res.render('error');
// });

module.exports = app;
