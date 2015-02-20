


services.factory('UserService', function($resource) {
	
	return $resource(exampleAppConfig.service+'rest/user/:action', {},
			{
				authenticate: {
					method: 'POST',
					params: {'action' : 'authenticate'},
					headers : {'Content-Type': 'application/x-www-form-urlencoded'}
				}
			}
		);
});



services.factory('NewsService', function($resource) {
	return $resource(exampleAppConfig.service+'rest/news/:id', {id: '@id'});
});




services.factory('DataFactory', ['$http', function($http) {

var urlBase = exampleAppConfig.service+'rest/';
var dataFactory = {};

dataFactory.getNews = function (id) {
    return $http.get(urlBase+'news/'+id);
};

dataFactory.getNewsList = function () {
    return $http.get(urlBase+'news/list');
};

dataFactory.insertNews = function (newsEntry) {
    return $http.post(urlBase + 'news/create',newsEntry );
};

dataFactory.updateNews = function (newsEntry) {
	var config = {
            method: 'PUT',
            url: urlBase + 'news/update/'+newsEntry.id ,
            headers : {'Content-Type': 'application/x-www-form-urlencoded'},
            data: JSON.stringify(newsEntry)
            //params: JSON.stringify(newsEntry),
       };
    return $http(config);
	//return $http.put(urlBase + 'news/update/'+newsEntry.id ,newsEntry);
	//return $http.post(urlBase + 'news/update/'+newsEntry.id ,newsEntry);
};

dataFactory.deleteNews = function (id) {
	return $http.delete(urlBase + 'news/delete/' + id);
};

dataFactory.getUser = function (id) {
    return $http.get(urlBase + 'user/' + id);
};
dataFactory.authenticateUser = function (username,password) {
	var config = {
            method: 'POST',
            url: urlBase + 'user/authenticate',
            headers : {'Content-Type': 'application/x-www-form-urlencoded'},
            data: $.param({username: username, password: password}),
       };
    return $http(config);
};
return dataFactory;
}
]);


mapModule.controller('EventSimpleCtrl', ['$scope', '$timeout', function($scope, $timeout) {
	var marker, map,infoWindow; 
	
	$scope.$on('mapInitialized', function(evt, evtMap) { 
		map = evtMap; 
		if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(onSuccess, onError,{'enableHighAccuracy':true,'timeout':20000});
		}
		}); 
	
	function onError(error){
	    alert('code: ' + error.code + '\n' + 'message: ' + error.message + '\n');
	}
	function onSuccess(position) {
		map.center = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
		showMarkersNear(map,position);
	}
	$scope.openInfoWindow = function(e, selectedMarker){
        e.defaultPrevented();
        google.maps.event.trigger(selectedMarker, 'click');
    }
	$scope.placeMarker = function(e) { 
		marker = new google.maps.Marker({position: e.latLng, map: map}); 
		map.panTo(e.latLng); 
		} 
	$scope.centerChanged = function(event) { 
		$timeout(function() { 
			map.panTo(marker.getPosition()); 
			}, 3000); 
		} 
	$scope.click = function(event) { 
		map.setZoom(8); 
		map.setCenter(marker.getPosition()); 
	} 
}
]);


var cities = [
              {
                  city : 'Toronto',
                  desc : 'This is the best city in the world!',
                  lat : 43.7000,
                  long : -79.4000
              },
              {
                  city : 'New York',
                  desc : 'This city is aiiiiite!',
                  lat : 40.6700,
                  long : -73.9400
              },
              {
                  city : 'Chicago',
                  desc : 'This is the second best city in the world!',
                  lat : 41.8819,
                  long : -87.6278
              },
              {
                  city : 'Los Angeles',
                  desc : 'This city is live!',
                  lat : 34.0500,
                  long : -118.2500
              },
              {
                  city : 'Las Vegas',
                  desc : 'Sin City...\'nuff said!',
                  lat : 36.0800,
                  long : -115.1522
              }
          ];


function showMarkersNear(map,position){
	//add a $http request to the service here
	for (i = 0; i < cities.length; i++){
		var info = '<div class="infoWindowContent">' + cities[i].desc + '</div>';
	        map.markers[i]=addMarker(map, cities[i].lat, cities[i].long, info) ;
	    }
}
var currentPopup;
function addMarker(map, lat, lng, info) {
	 var pt = new google.maps.LatLng(lat, lng);
	 //var bounds = new google.maps.LatLngBounds();
	 //bounds.extend(pt);
	 var marker = new google.maps.Marker({
	 id: 1,
	 position: pt,
	 //icon: icon,
	 map: map
	 });
	 var popup = new google.maps.InfoWindow({
	 content: info,
	 maxWidth: 400
	 });
	 google.maps.event.addListener(marker, "click", function() {
	 if (currentPopup != null) {
	 currentPopup.close();
	 currentPopup = null;
	 }
	 popup.open(map, marker);
	 currentPopup = popup;
	 });
	 google.maps.event.addListener(popup, "closeclick", function() {
	 currentPopup = null;
	 });
	 return marker;
};
	 


function showMap($scope){
    var mapOptions = {
        zoom: 4,
        center: new google.maps.LatLng(40.0000, -98.0000),
        mapTypeId: google.maps.MapTypeId.TERRAIN
    }
    $scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);
    $scope.markers = [];
    var infoWindow = new google.maps.InfoWindow();
    var createMarker = function (info){
        var marker = new google.maps.Marker({
            map: $scope.map,
            position: new google.maps.LatLng(info.lat, info.long),
            title: info.city
        });
        marker.content = '<div class="infoWindowContent">' + info.desc + '</div>';
        google.maps.event.addListener(marker, 'click', function(){
            infoWindow.setContent('<h2>' + marker.title + '</h2>' + marker.content);
            infoWindow.open($scope.map, marker);
        });
        $scope.markers.push(marker);
    }   
    for (i = 0; i < cities.length; i++){
        createMarker(cities[i]);
    }
    $scope.openInfoWindow = function(e, selectedMarker){
        e.defaultPrevented();
        google.maps.event.trigger(selectedMarker, 'click');
    }	
}
