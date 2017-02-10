(function() {
	'use strict';

	angular.module('measurePlatformApp').controller(
			'TestInstanceDialogController', TestInstanceDialogController);

	TestInstanceDialogController.$inject = [ '$timeout', '$scope',
			'$stateParams', '$uibModalInstance', 'entity','ProjectInstances'];

	function TestInstanceDialogController($timeout, $scope, $stateParams,
			$uibModalInstance, entity,ProjectInstances) {
		var vm = this;

		vm.close = close;
		
		vm.measureInstance = entity;

		testMeasure(vm.measureInstance.id);
		vm.testResult = null;

		function testMeasure(id) {
			ProjectInstances.testMeasure({
				id : id
			}, function(result) {
				vm.testResult = result;
			});
		}
		
		function close() {
			$uibModalInstance.dismiss('cancel');
		}

	}
})();
