(function() {
	'use strict';
	angular.module('measurePlatformApp').factory('MeasureLogService', Measure);

	Measure.$inject = [ '$resource' ];

	function Measure($resource) {
		var resourceUrl = 'api/measure-logger/';

		return $resource(resourceUrl, {}, {
			'alllogs' : {
				url : 'api/measure-logger/measure-execution',
				method : 'GET',
				isArray : true
			},'kibanaadress' : {
				url : 'api/configuration/kibana-adress',
				method : 'GET',
				isArray : true
			}
		});

	}
})();
