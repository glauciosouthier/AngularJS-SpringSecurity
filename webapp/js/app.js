
var logEnabled =true;
angular.module('exampleApp', ['ngRoute', 'ngCookies', 'exampleApp.services'])
	.config(
		[ '$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
			
			$routeProvider.when('/create', {
				templateUrl: 'partials/create.html',
				controller: CreateController
			});
			
			$routeProvider.when('/edit/:id', {
				templateUrl: 'partials/edit.html',
				controller: EditController
			});
			
			$routeProvider.when('/delete/:id', {
				templateUrl: 'partials/index.html',
				controller: IndexController
			});

			$routeProvider.when('/login', {
				templateUrl: 'partials/login.html',
				controller: LoginController
			});
			
			$routeProvider.otherwise({
				templateUrl: 'partials/index.html',
				controller: IndexController
			});
			
			$locationProvider.hashPrefix('!');
			
			/*
			 * Register error provider that shows message on failed requests or
			 * redirects to login page on unauthenticated requests
			 */
		    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
			        return {
			        	'responseError': function(rejection) {
			        		var status = rejection.status;
			        		var config = rejection.config;
			        		var method = config.method;
			        		var url = config.url;
			      
			        		if (status == 401) {
			        			$location.path( "/login" );
			        		} else {
			        			$rootScope.error = method + " on " + url + " failed with status " + status;
			        		}
			              
			        		return $q.reject(rejection);
			        	}
			        };
			    }
		    );
		    
		    /*
			 * Registers auth token interceptor, auth token is either passed by
			 * header or by query parameter as soon as there is an authenticated
			 * user
			 */
		    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
		        return {
		        	'request': function(config) {
		        		var isRestCall = config.url.contains('rest');
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
		    }
	    );
		   
		} ]
		
	).run(function($rootScope, $location, $cookieStore, UserService,DataFactory) {
		
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
			//**
			/*
			UserService.get(function(user) {
				$rootScope.user = user;
				$location.path(originalPath);
			});
			*/
			DataFactory.getUser().success(function (user) {
				$rootScope.user = user;
				$location.path(originalPath);
			}) .error(function (error) {
		    	console.log('Unable to load user data: ' + error.message);
		        
		    });
		}
		
		$rootScope.initialized = true;
	});


function IndexController($scope, NewsService,DataFactory) {
	
	//$scope.newsEntries = NewsService.query();
	DataFactory.getNewsList().success(function (news) {
		$scope.newsEntries =news;
		if(logEnabled)console.log(JSON.stringify(news));
    })
    .error(function (error) {
    	console.log('Unable to load news data: ' + error.message);
       
    });
	
	$scope.deleteEntry = function(newsEntry) {
		/*newsEntry.$remove(function() {
			$scope.newsEntries = NewsService.query();
		});
		*/
		DataFactory.deleteNews(newsEntry.id).success(function (result) {
			DataFactory.getNewsList().success(function (news) {
				$scope.newsEntries =news;
				if(logEnabled)console.log(JSON.stringify(news));
		    })
		    .error(function (error) {
		    	console.log('Unable to load news data: ' + error.message);
		       
		    });
	    });
	};
};


function EditController($scope, $routeParams, $location, NewsService,DataFactory) {

	//$scope.newsEntry = NewsService.get({id: $routeParams.id});
	
	DataFactory.getNews($routeParams.id)
	.success(function (news) {
		$scope.newsEntry =news;
		if(logEnabled)console.log(JSON.stringify(news));
    })
    .error(function (error) {
    	console.log('Unable to load news data: ' + error.message);
       
    });
	
	$scope.save = function() {
		/*
		$scope.newsEntry.$save(function() {
			$location.path('/');
		});
		*/
		console.log('News: '+$routeParams.id);
		if(logEnabled)console.log(JSON.stringify($scope.newsEntry));
		
		DataFactory.updateNews($scope.newsEntry)
		.success(function (result,status) {
			$location.path('/');
		})
		.error(function (error) {
	    	console.log('Unable to save news data: ' + error.message);
	        
	    });
	};
};


function CreateController($scope, $location, NewsService,DataFactory) {
	
	$scope.newsEntry = new Object();
	
	$scope.save = function() {
		/*
		$scope.newsEntry.$save(function() {
			$location.path('/');
		});
		*/
		DataFactory.insertNews($scope.newsEntry)
		.success(function (result,status) {
			$location.path('/');
		})
		.error(function (error) {
	    	console.log('Unable to insert news data: ' + error.message);
	        
	    });
		
	};
};


function LoginController($scope, $rootScope, $location, $cookieStore, UserService,DataFactory) {
	
	$scope.rememberMe = false;
	
	$scope.login = function() {
		/*
		var params=$.param({username: $scope.username, password: $scope.password});
		console.log(params);
		UserService.authenticate(params, function(authenticationResult) {
			var authToken = authenticationResult.token;
			console.log(authenticationResult.token);
			$rootScope.authToken = authToken;
			if ($scope.rememberMe) {
				$cookieStore.put('authToken', authToken);
			}
			UserService.get(function(user) {
				$rootScope.user = user;
				$location.path("/");
			});
		});
		*/
		DataFactory.authenticateUser($scope.username,$scope.password)
		.success(function (result, status, headers) {
			var authToken = result.token;
			if(authToken == null){
				authToken = headers('X-Auth-Token');
			}
			if(authToken == null){
				authToken = headers('token');
			}
			if(logEnabled)console.log('Token: '+authToken);
			$rootScope.authToken = authToken;
			if ($scope.rememberMe) {
				$cookieStore.put('authToken', authToken);
			}
			DataFactory.getUser($scope.username)
				.success(function (userResult) {
					$rootScope.user = userResult;
					if(logEnabled)console.log('User: '+JSON.stringify(userResult));
					$location.path("/");
				})
				.error(function (error) {
					console.log('Unable to load user data: ' + error.message);  
				});
	    })
	    .error(function (error) {
	    	console.log('Unable to authenticate: ' + error.message);
	       
	    });
		
		
	};
};


