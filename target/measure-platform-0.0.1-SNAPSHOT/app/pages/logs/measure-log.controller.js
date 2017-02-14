(function() {
	'use strict';

	angular.module('measurePlatformApp').controller('MeasureLog',
			MeasureLog);

	MeasureLog.$inject = [ '$scope', 'Principal', 'MeasureLogService',
			'$state' ];

	function MeasureLog($scope, Principal, MeasureLogService, $state) {
		var vm = this;

		vm.logs = [];

		loadAll();
			
		function loadAll() {
			MeasureLogService.alllogs(function(result) {
				vm.logs = result;
			});
		}
		
		setInterval(function(){
			
			MeasureLogService.alllogs(function(result) {
				
				var lastmeasure = vm.logs[0].exectionDate;
				for(var i = result.length-1 ; i >= 0;i--){
					if(result[i].exectionDate > lastmeasure){
						vm.logs.splice(0, 0, result[i]);
					}
				}
			});
		   
		 }, 500);
	}
})();
