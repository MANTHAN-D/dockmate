'use strict';

var express = require('express');
var bodyParser = require('body-parser');
var mongoose   = require('mongoose');

mongoose.connect('mongodb://mongo:27017/mainDB');

var Employee = require('./models/employee');
var employeesProjection = { 
    __v: false,
    _id: false
};

// Constants
var PORT = 80;

// App
var app = express();

//app.get('/', function (req, res) {
//  res.send('Welcome to Application 1\n');
//});

// configure app to use bodyParser()
// this will let us get the data from a POST
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

// ROUTES FOR OUR API
// =============================================================================
var router = express.Router();              // get an instance of the express Router

// middleware to use for all requests
router.use(function(req, res, next) {
    // do logging
   // console.log('Serving Request: '+ req.method + ' : ' + req.originalUrl);
    next(); // make sure we go to the next routes and don't stop here
});

router.get('/', function(req, res) {
    res.json({ message: 'Welcome to Application 1!' });   
});

router.get('/runReactive', function(req, res) {
    res.json({ message: 'Fibonnaci for 30 is : '+ recursive(30) });   
});

var recursive = function(n) {
    if(n <= 2) {
        return 1;
    } else {
        return recursive(n - 1) + recursive(n - 2);
    }
};


router.route('/employees')
.post(function(req, res) {
        
        var employee = new Employee();      // create a new instance of the Employee model
        employee.id = req.body.id;  // set the employees id (comes from the request)
        employee.firstName = req.body.firstName;  // set the employess first name (comes from the request)
        employee.lastName = req.body.lastName;  // set the employees last name (comes from the request)

        console.log('Employee will be created with following info');
        console.log('Employee id: ' + employee.id);
        console.log('Employee firstName: ' + employee.firstName);
        console.log('Employee lastName: ' + employee.lastName);
        
        // save the employee and check for errors
        employee.save(function(err) {
            if (err)
                res.send(err);

            res.json({ message: 'Employee created!' });
        });
})
// get all the employees (accessed at GET http://localhost:8080/api/employees)
.get(function(req, res) {
    Employee.find({}, employeesProjection, function(err, employees) {
        if (err)
          res.send(err);
        res.json(employees);
    });
});

router.route('/employee/:id')
    // get the employee with that id (accessed at GET http://localhost:8080/api/employee/:id)
    .get(function(req, res) {
        Employee.find({"id":req.params.id}, employeesProjection, function(err, employee) {
            if (err)
                res.send(err);
            res.json(employee);
        });
    })
    // update the employee with this id (accessed at PUT http://localhost:8080/api/employee/:id)
    .put(function(req, res) {
        
        console.log('Employee with id: '+ req.params.id + ' to be updated');
        console.log('Employee\'s new firstName: ' + req.body.firstName);
        console.log('Employee\'s new lastName: ' + req.body.lastName);
        
        // use our bear model to find the bear we want
        Employee.findOne({"id":req.params.id}, function(err, employee) {

            if (err)
                res.send(err);

            employee.firstName = req.body.firstName;  // update the employees info
            employee.lastName = req.body.lastName;  // update the employees info
            
            // save the employee
            employee.save(function(err) {
                if (err)
                    res.send(err);

                res.json({ message: 'Employee updated!' });
            });

        });
    })
    // delete the employee with this id (accessed at DELETE http://localhost:8080/api/employee/:id)
    .delete(function(req, res) {
        console.log('Employee with id: '+ req.params.id + ' to be deleted');
        Employee.remove({
            id: req.params.id
        }, function(err, employee) {
            if (err)
                res.send(err);

            res.json({ message: 'Successfully deleted' });
        });
    });

// more routes for our API will happen here

// REGISTER OUR ROUTES -------------------------------
// all of our routes will be prefixed with /cmpe281group4
app.use('/cmpe281group4', router);


app.listen(PORT);
console.log('Running on http://localhost:' + PORT);
