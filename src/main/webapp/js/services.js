var services = angular.module('exampleApp.services', ['ngResource']);

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
	return $http.put(urlBase + 'news/update/'+newsEntry.id ,newsEntry);
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
