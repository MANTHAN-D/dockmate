var express = require('express');
var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
require('shelljs/global');

var routes = require('./routes/index');
var users = require('./routes/users');
var report = require('./routes/report');

var app = express();

var http = require('http');
var server = http.Server(app);

//Socket IO
var io = require('socket.io')();
app.io = io;

io.on('connection', function(socket){
   console.log("client connected");
   socket.on('disconnect', function () {
            console.info('Client disconnected');
        }
   );
   
   var child = exec('./proactiveAlgo.sh', {async:true});
   child.stdout.on('data', function(data) {
      console.log("data:" + data);
      if(data.includes("Prediction")){
        socket.emit('process_prediction', {"req_prediction":data.substr(11,data.indexOf("\n")-1)});
      }
      else{
        socket.emit('process_data', data);
      }
   });    
});

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', routes);
app.use('/users', users);
app.use('/report',report);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// error handlers

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});

module.exports = app;
