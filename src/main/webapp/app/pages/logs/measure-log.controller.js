(function() {
	'use strict';

	angular.module('measurePlatformApp').controller('MeasureLog',
			MeasureLog);

	MeasureLog.$inject = [ '$scope', 'Principal', 'MeasureLogService',
			'$state' ];

	function MeasureLog($scope, Principal, MeasureLogService, $state) {
		var vm = this;

		vm.logs = [];
		
		vm.kibanaadress = null;

		loadAll();
			
		function loadAll() {
			MeasureLogService.alllogs(function(result) {
				vm.logs = result;
			});
			
			MeasureLogService.kibanaadress(function(result) {
				vm.kibanaadress = result;
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
