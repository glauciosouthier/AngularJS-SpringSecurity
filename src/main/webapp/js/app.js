var logEnabled = true;
$(document).bind("mobileinit", function(){
    $.mobile.page.prototype.options.domCache = true ;
    $.mobile.allowCrossDomainPages = true;
});
var services = angular.module('exampleApp.services', [ 'ngResource' ]);

var mapModule = angular.module('MapModule', [ 'ngMap' ]);

mapModule.service('MapService', function() {
	this.show = function($scope) {
		showMap($scope)
	};
});

var exampleApp = angular
		.module('exampleApp',
				[ 'ngRoute', 'ngCookies', 'exampleApp.services', 'MapModule'])
		.config(
				[
						'$routeProvider',
						'$locationProvider',
						'$httpProvider',
						function($routeProvider, $locationProvider,
								$httpProvider) {

							$httpProvider.defaults.useXDomain = true;
							delete $httpProvider.defaults.headers.common['X-Requested-With'];
							// $httpProvider.defaults.headers.common['Access-Control-Allow-Headers']
							// = '*';
							// $httpProvider.defaults.headers.common["Accept"] =
							// "application/json";
							// $httpProvider.defaults.headers.common["Content-Type"]
							// = "application/json";
							configureRoutes($routeProvider, $locationProvider);
							configureInterceptors($httpProvider);

						} ]

		).run(
				function($rootScope, $location, $cookieStore, UserService,
						DataFactory) {
					bootstrap($rootScope, $location, $cookieStore, UserService,
							DataFactory);
				});

function bootstrap($rootScope, $location, $cookieStore, UserService,
		DataFactory) {

	/* Reset error when a new view is loaded */
	$rootScope.$on('$viewContentLoaded', function() {
		delete $rootScope.error;
	});

	$rootScope.hasRole = function(role) {

		if ($rootScope.user === undefined) {
			return false;
		}

		if ($rootScope.user.roles[role] === undefined) {
			return false;
		}

		return $rootScope.user.roles[role];
	};

	$rootScope.logout = function() {
		delete $rootScope.user;
		delete $rootScope.authToken;
		$cookieStore.remove('authToken');
		$location.path("/login");
	};

	/* Try getting valid user from cookie or go to login page */
	var originalPath = $location.path();
	$location.path("/login");
	var authToken = $cookieStore.get('authToken');
	if (authToken !== undefined) {
		$rootScope.authToken = authToken;
		// **
		/*
		 * UserService.get(function(user) { $rootScope.user = user;
		 * $location.path(originalPath); });
		 */
		DataFactory.getUser().success(function(user) {
			$rootScope.user = user;
			$location.path(originalPath);
		}).error(function(error) {
			console.log('Unable to load user data: ' + error.message);

		});
	}

	$rootScope.initialized = true;
}

function configureRoutes($routeProvider, $locationProvider) {
	$routeProvider.when('/create', {
		templateUrl : 'partials/create.html',
		controller : CreateController
	});

	$routeProvider.when('/edit/:id', {
		templateUrl : 'partials/edit.html',
		controller : EditController
	});

	$routeProvider.when('/delete/:id', {
		templateUrl : 'partials/index.html',
		controller : IndexController
	});

	$routeProvider.when('/login', {
		templateUrl : 'partials/login.html',
		controller : LoginController
	});

	$routeProvider.when('/map', {
		templateUrl : 'partials/map.html',
		controller : MapController
	});

	$routeProvider.otherwise({
		templateUrl : 'partials/index.html',
		controller : IndexController
	});

	$locationProvider.hashPrefix('!');
}

function configureInterceptors($httpProvider) {
	/*
	 * Register error provider that shows message on failed requests or
	 * redirects to login page on unauthenticated requests
	 */
	$httpProvider.interceptors.push(function($q, $rootScope, $location) {
		return {
			'responseError' : function(rejection) {
				var status = rejection.status;
				var config = rejection.config;
				var method = config.method;
				var url = config.url;

				if (status == 401) {
					$location.path("/login");
				} else {
					$rootScope.error = method + " on " + url
							+ " failed with status " + status;
				}

				return $q.reject(rejection);
			}
		};
	});

	/*
	 * Registers auth token interceptor, auth token is either passed by header
	 * or by query parameter as soon as there is an authenticated user
	 */
	$httpProvider.interceptors.push(function($q, $rootScope, $location) {
		return {
			'request' : function(config) {
				var isRestCall = config.url.contains('/rest/');
				if (logEnabled)console.log(config.method);
				if (logEnabled)console.log(config.headers);
				
				if (isRestCall && angular.isDefined($rootScope.authToken)) {
					var authToken = $rootScope.authToken;
					if (exampleAppConfig.useAuthTokenHeader) {
						config.headers['X-Auth-Token'] = authToken;
					} else {
						config.url = config.url + "?token=" + authToken;
					}

				}
				return config || $q.when(config);
			}
		};
	});
}

function IndexController($scope, NewsService, DataFactory) {

	// $scope.newsEntries = NewsService.query();
	DataFactory.getNewsList().success(function(news) {
		$scope.newsEntries = news;
		if (logEnabled)
			console.log(JSON.stringify(news));
	}).error(function(error) {
		console.log('Unable to load news data: ' + error.message);

	});

	$scope.deleteEntry = function(newsEntry) {
		/*
		 * newsEntry.$remove(function() { $scope.newsEntries =
		 * NewsService.query(); });
		 */
		DataFactory.deleteNews(newsEntry.id).success(function(result) {
			DataFactory.getNewsList().success(function(news) {
				$scope.newsEntries = news;
				if (logEnabled)
					console.log(JSON.stringify(news));
			}).error(function(error) {
				console.log('Unable to load news data: ' + error.message);

			});
		});
	};
};

function EditController($scope, $routeParams, $location,$timeout, NewsService,
		DataFactory) {

	// $scope.newsEntry = NewsService.get({id: $routeParams.id});

	DataFactory.getNews($routeParams.id).success(function(news) {
		$scope.newsEntry = news;
		if (logEnabled)
			console.log('News returned: ' +JSON.stringify(news));
	}).error(function(error) {
		console.log('Unable to load news data: ' + error.message);

	});

	$scope.save = function() {

		if (logEnabled)
			console.log('News to update: ' +JSON.stringify($scope.newsEntry));
	
		 DataFactory.updateNews($scope.newsEntry)
			.done(function(data) {
				$timeout(function() {
					console.log('OK');
					$location.path('/');
				}, 500);	
			})
			.fail(function(error) {
				console.log('Unable to save news data: ' + error.message);
			});
		 	
	};
};

function CreateController($scope, $location,$timeout, NewsService, DataFactory) {

	$scope.newsEntry = new Object();

	$scope.save = function() {
		
		DataFactory.insertNews($scope.newsEntry)
		.done(function(data) {
			$timeout(function() {
				console.log('OK');
				$location.path('/');
			}, 500);
		}).fail(function(error) {
			console.log('Unable to insert news data: ' + error.message);
		});

	};
};

function LoginController($scope, $rootScope, $location, $cookieStore,
		UserService, DataFactory) {

	$scope.rememberMe = false;

	$scope.login = function() {

		DataFactory.authenticateUser($scope.username, $scope.password).success(
				function(result, status, headers) {
					var authToken = result.token;
					if (authToken == null) {
						authToken = headers('X-Auth-Token');
					}
					if (authToken == null) {
						authToken = headers('token');
					}
					if (logEnabled)
						console.log('Token: ' + authToken);
					$rootScope.authToken = authToken;
					if ($scope.rememberMe) {
						$cookieStore.put('authToken', authToken);
					}
					DataFactory.getUser($scope.username).success(
							function(userResult) {
								$rootScope.user = userResult;
								if (logEnabled)
									console.log('User: '
											+ JSON.stringify(userResult));
								$location.path("/");
							}).error(
							function(error) {
								console.log('Unable to load user data: '
										+ error.message);
							});
				}).error(function(error) {
			console.log('Unable to authenticate: ' + error.message);

		});

	};
};

function MapController($scope, $location, DataFactory, MapService) {
	/*
	 * MapService.show($scope) .success(function (result) {
	 * //$location.path('/'); }) .error(function (error) { console.log('Unable
	 * to load map data: ' + error.message);
	 * 
	 * });
	 */
};