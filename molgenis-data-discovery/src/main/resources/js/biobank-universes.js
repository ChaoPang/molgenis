(function($, molgenis) {	
	"use strict";
	
	var restApiV2 = new molgenis.RestClientV2();
	
	function updateTableProgressBars($table){
		var reload = false;
		var size = $table.find('tbody tr').length;
		$.each($table.find('tbody tr'), function(index, row){
			var progressBarElement = $(row).find('td:last-child div[name="progress-bar"]:first-child');
			if(progressBarElement && progressBarElement.length > 0){
				var biobankUniverseId = progressBarElement.attr('biobank-universe');
				var options = {
					'q' : [{
						'field':'universe', 
						'operator':'EQUALS', 
						'value': biobankUniverseId
					}]
				}
				restApiV2.get('BiobankUniverseJobExecution', options).done(function(data){
					var totalMax = 0;
					var totalProgress = 0;
					if(data.items.length > 0){
						reload = false;
						for(var i = 0; i < data.items.length; i++){
							totalMax += data.items[i].progressMax;
							totalProgress += data.items[i].progressInt;
							reload = reload || (data.items[i].status == "RUNNING");
						}
					}
					
					if(!reload){
						totalMax = 1;
						totalProgress = 1;
					}
					React.render(molgenis.ui.ProgressBar({
						'progressPct': totalProgress / totalMax * 100,
						'progressMessage': '',
						'status': reload ? 'primary':'info',
						'active': reload
					}), $('#progress-bar-'+ biobankUniverseId)[0]);
					
					if(index == size - 1 && reload){
						setTimeout(function(){
							updateTableProgressBars($table);
						}, 5000);
					}
				});
			}
		});
	}
	
	$(function() {
		
		$('form.verify').submit(function(e) {
	        var currentForm = this;
	        e.preventDefault();
	        bootbox.confirm("Are you sure?", function(result) {
	            if (result) {
	                currentForm.submit();
	            }
	        });
	    });
		
		updateTableProgressBars($('#biobank-universes-tbl'));

		$('select[name="biobankSampleCollectionNames"]').select2();
		$('select[name="semanticTypes"]').select2();
	});
		
}($, window.top.molgenis = window.top.molgenis || {}));