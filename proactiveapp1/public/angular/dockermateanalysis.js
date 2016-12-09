//Made by Manthan and Darshil
console.log("docker mate controller loaded");

var dockermate = angular.module('dockermate',["ng-fusioncharts"]);

dockermate.factory('socket', function ($rootScope) {
    var socket = io.connect('http://'+self.location.host);

    return {
        on: function (eventName, callback) {
            socket.on(eventName, function () {
                var args = arguments;
                $rootScope.$apply(function () {
                    callback.apply(socket, args);
                });
            });
        },
        emit: function (eventName, data, callback) {
            socket.emit(eventName, data, function () {
                var args = arguments;
                $rootScope.$apply(function () {
                    if (callback) {
                        callback.apply(socket, args);
                    }
                });
            })
        }
    };
});

dockermate.controller('dockermate',function($scope, $filter, $http, socket) {

    $scope.dataSource = {
            "chart": {
                "caption": "Request Trend",
                "yAxisMinValue": "0",
                "xAxisName":"Interval (Seconds)",
                "yAxisName":"No. of Requests",
                "theme":"fint",
                "labelDisplay":"auto",
                "palette": "3",
                "divLineDashed": "1",
                "divLineDashLen": "5",
                "divLineDashGap": "6"
            },
            "categories": [{
                "category": [{
                    "label": "5"
                }, {
                    "label": "10"
                }, {
                    "label": "15"
                }, {
                    "label": "20"
                }, {
                    "label": "25"
                }, {
                    "label": "30"
                }, {
                    "label": "35"
                }, {
                    "label": "40"
                }, {
                    "label": "45"
                }, {
                    "label": "50"
                },{
                    "label": "55"
                }, {
                    "label": "60"
                }, {
                    "label": "65"
                }, {
                    "label": "70"
                }, {
                    "label": "75"
                }, {
                    "label": "80"
                }, {
                    "label": "85"
                }, {
                    "label": "90"
                }, {
                    "label": "95"
                }, {
                    "label": "100"
                }]
            }],
            "dataset": [{
                "seriesname":"Actual",
                "data": []
            },
            {
                "seriesname":"Predicted",
                "data": []
            }]
        };
        
    socket.on('process_data', function (data) {
        console.log("data is:" + data);

        var dataLength = $scope.dataSource.dataset[0].data.length;
        console.log("data length at data: " + data  + ": is : " + dataLength);

        if(dataLength==0){
            console.log("data is equal to 0!");
            $scope.dataSource.dataset[0].data[0] = {
            "value":data
            }
        } else if(dataLength<20 && dataLength!=0){
          console.log("data is not equal to 0 and less than 20!");
          $scope.dataSource.dataset[0].data[dataLength] = {
            "value" : data
          }
        } else if(dataLength>=20){
            console.log("data is equal or more than 20!");
            for(var index=0;index<(dataLength-1);index++){
               $scope.dataSource.dataset[0].data[index] = {
                "value" : $scope.dataSource.dataset[0].data[index+1].value
               }
            }
            $scope.dataSource.dataset[0].data[19] = {
              "value" : data
            }
            console.log($scope.dataSource.dataset[0].data[19].value);
      }
    });
    
    socket.on('process_prediction', function (data) {
        console.log("Prediction is:" + JSON.stringify(data));
        
        //For T=50
        var dataLength = $scope.dataSource.dataset[1].data.length;
        console.log("pred length at data: " + data  + ": is : " + dataLength);

        if(dataLength==0){
            console.log("pred data is equal to 0!");
            for(var index=0; index<10; index++){
              $scope.dataSource.dataset[1].data[index] = {
              "value":0
              };
            }
            for(var index=10; index<20; index++){
              $scope.dataSource.dataset[1].data[index] = {
              "value":data.req_prediction
              };
            }
        } else{
            console.log("pred data is equal to 20!");
            for(var index=0;index<10;index++){
               $scope.dataSource.dataset[1].data[index] = {
                "value" : $scope.dataSource.dataset[1].data[index+10].value
               }
            }
            for(var index=10; index<20; index++){
              $scope.dataSource.dataset[1].data[index] = {
              "value":data.req_prediction
              };
            }
            console.log($scope.dataSource.dataset[1].data[19].value);
      }
        /* For T=100
        for(var index=0; index<10; index++){
            $scope.dataSource.dataset[1].data[index] = {
            "value":data.req_prediction
            };
        }
        */
    });
})
