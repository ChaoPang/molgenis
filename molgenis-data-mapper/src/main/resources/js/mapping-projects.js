(function($, molgenis) {	
	"use strict";
	
	var restApiV2 = new molgenis.RestClientV2();
	
	/**
	 * Updates a table's column widths.
	 * 
	 * @param $table
	 *            the scrollable table whose header and body cells should be
	 *            realigned
	 */
	function updateColumnWidths($table) {
		var $bodyCells = $table.find('tbody tr:first').children(),
			colWidth;

		// Get the tbody columns width array
		colWidth = $bodyCells.map(function() {
			return $(this).width();
		}).get();

		// Set the width of thead columns
		$table.find('thead tr').children().each(function(i, v) {
			$(v).width(colWidth[i]);
		});
	};
	
	function updateTableProgressBars($table){
		var reload = false;
		var size = $table.find('tbody tr').length;
		$.each($table.find('tbody tr'), function(index, row){
			var progressBarElement = $(row).find('td:last-child div[name="progress-bar"]:first-child');
			if(progressBarElement && progressBarElement.length > 0){
				var mappingProjectId = progressBarElement.attr('mapping-project');
				var options = {
					'q' : [{
						'field':'mappingProject', 
						'operator':'EQUALS', 
						'value': mappingProjectId
					},{
						'operator':'AND'
					},{
						'field':'status', 
						'operator':'EQUALS', 
						'value': 'RUNNING'
					}]
				}
				restApiV2.get('MappingServiceJobExecution', options).done(function(data){
					var totalMax = 0;
					var totalProgress = 0;
					if(data.items.length > 0){
						for(var i = 0; i < data.items.length; i++){
							totalMax += data.items[i].progressMax;
							totalProgress += data.items[i].progressInt;
						}
						reload = true;
					}else{
						totalMax = 1;
						totalProgress = 1;
					}
					React.render(molgenis.ui.ProgressBar({
						'progressPct': totalProgress / totalMax * 100,
						'progressMessage': '',
						'status': data.items.length > 0 ? 'primary':'info',
						'active': data.items.length > 0
					}), $('#progress-bar-'+ mappingProjectId)[0]);
					
					if(index == size - 1 && reload){
						setTimeout(updateTableProgressBars($table), 2000);
					}
				});
			}
		});
	}
	
	$(function() {
		var $table = $('table.scroll');
		$(window).resize(function(){
			updateColumnWidths($table);
		});
		updateColumnWidths($table);
		
		updateTableProgressBars($('#mapping-projects-tbl'));
		
		$('form.verify').submit(function(e) {
	        var currentForm = this;
	        e.preventDefault();
	        bootbox.confirm("Are you sure?", function(result) {
	            if (result) {
	                currentForm.submit();
	            }
	        });
	    });

		
		$('#submit-new-mapping-project-btn').click(function() {
			$('#create-new-mapping-project-form').submit();
		});
		
		$('#create-new-mapping-project-form').validate();
		
		$('select[name="target-entity"]').select2();
		
		$('select[name="source"]').select2();
	});
		
}($, window.top.molgenis = window.top.molgenis || {}));