var mongoose = require('mongoose');
var Schema = mongoose.Schema;

var EmployeeSchema   = new Schema({
    id: Number,
    firstName: String,
    lastName: String
});

module.exports = mongoose.model('Employee', EmployeeSchema);
