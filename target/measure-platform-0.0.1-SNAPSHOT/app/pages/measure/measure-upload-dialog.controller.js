(function() {
	'use strict';

	angular.module('measurePlatformApp').controller('MeasureUploadController',
			MeasureUploadController);

	MeasureUploadController.$inject = [ '$scope', '$uibModalInstance',
			'Measure' ];

	function MeasureUploadController($scope, $uibModalInstance, Measure) {
		var vm = this;
		vm.isUpload = false;
		vm.uploadFile = uploadFile;

		function uploadFile() {
			Measure.upload($scope.fileread, onUploadSuccess, onUploadError);
		}

		function onUploadSuccess(result) {
			vm.isUpload = true;
			$uibModalInstance.close(true);
		}

		function onUploadError() {
			vm.isUpload = false;
		}

		vm.clear = clear;
		function clear() {
			$uibModalInstance.dismiss('cancel');
		}
	}
})();
