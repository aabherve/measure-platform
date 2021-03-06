(function() {
    'use strict';

    angular
        .module('measurePlatformApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('catalogue', {
            parent: 'app',
            url: '/catalogue',
            data: {
                authorities: []
            },
            views: {
                'content@': {
                    templateUrl: 'app/pages/catalogue/catalogue.html',
                    controller: 'CatalogueController',
                    controllerAs: 'vm'
                }
            }
        }).state('catalogue.newproject', {
            parent: 'catalogue',
            url: 'newproject',
            data: {
                authorities: []
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/pages/catalogue/newproject-dialog.html',
                    controller: 'NewProjectDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                description: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('catalogue', null, { reload: 'catalogue' });
                }, function() {
                    $state.go('catalogue');
                });
            }]
        }).state('catalogue.addgraphic', {
            parent: 'catalogue',
            url: '/graphic/:id',
            data: {
                authorities: []
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/pages/graphics/graphic-dialog.html',
                    controller: 'GraphicDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {   
                    	entity: function () {
	                        return {
	                            viewData: null,
	                            id: null,
	                            name:null,
	                            description:null,
	                            size:"Medium",
	                            type:"Line chart",
	                            auto:true,
	                            interval:"Last Hour",
	                            custom:false
	                        };
	                    },
                    	 data: function () {
                             return {
                                 isOverview: true
                             };
                         },
                    	  project: ['Project', function(Project) {
                              return Project.get({id : $stateParams.id}).$promise;
                          }],
                          phase:null,
                          dashboard:null 
                    }
                }).result.then(function() {
                    $state.go('catalogue', null, { reload: 'catalogue' });
                }, function() {
                    $state.go('catalogue');
                });
            }]
        }).state('catalogue.editgraphic', {
            parent: 'catalogue',
            url: '/graphic/:id/edit/',
            data: {
                authorities: []
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/pages/graphics/graphic-dialog.html',
                    controller: 'GraphicDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {   
                    	  entity: ['MeasureView', function(MeasureView) {
                              return MeasureView.get({id : $stateParams.id}).$promise;
                          }],
                    	 data: function () {
                             return {
                                 isOverview: true
                             };
                         },
                         project: null,
                         phase:null,
                         dashboard:null 
                    }
                }).result.then(function() {
                    $state.go('catalogue', null, { reload: 'catalogue' });
                }, function() {
                    $state.go('catalogue');
                });
            }]
        });
    }
})();
