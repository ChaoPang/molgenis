<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<@header />
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<div class="form-group">
			<label for="entity_one">${entity_one}</label>
		    <input type="text" class="form-control" id="entity_one" name="entity_one" value="${entity_one}">
		</div>
		<div class="form-group">
			<label for="entity_two">${entity_two}</label>
			<input type="text" class="form-control" id="entity_two" name="entity_two" value="${entity_two}">
		</div>
	</div>
</div>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<h3><center>Please be patient...</center></h3>
	</div>
</div>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
    	<div id="entitySimilarityMatrix"></div>
	</div>
</div>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<button id="update-button" type="button" class="btn btn-primary">Update result</button>
	</div>
</div>

<div class="row">
	<div id="matrix" class="col-md-12" style="overflow:scroll;">
		
	</div>
</div>

<script>
	$(function (){
		$('#update-button').click(function(e){
			retriveResult(function(data){
				builtMatrix(data);
			});
		});
		var finished = false;
 		var ProgressBar = React.render(molgenis.ui.ProgressBar({
    	 	'progressPct' : 0,
			'progressMessage' : 'Computing the distances...',    
			'status' : 'info',
			'active' : true
		}), $('#entitySimilarityMatrix')[0]);
			if(!finished){
	            setInterval(
	                    function ()
	                    {
	                    	retriveResult(function(data) {
								console.log(data);
								var progress = data.progress * 100;
								finished = data.finished;
								ProgressBar.setProps({
	                                'progressPct' : progress
	                            });
							});
	                    }
	           , 1000000);
           }
    });
    
    function builtMatrix(data){
    	var distanceMetrics = data.distanceMetrics;
    	var previosuAttr = null;
    	var rowNum = 0;
    	var table = $('<table />').addClass('table table-bordered');
    	var header = $('<thead />');
    	var headerRow = $('<tr />').appendTo(header);
    	$.map(distanceMetrics, function(val, key){
    		headerRow.append('<th>' + key + '</th>');
    		if(rowNum < val.length) rowNum = val.length;
    	});
    	var body = $('<tbody />');
    	$.map(distanceMetrics, function(scores, key){
    		$.each(scores, function(i, score){
    			var row = body.find('tr:eq(' + i + ')');
    			if(row === null || row.length === 0){
    				row = $('<tr />').appendTo(body);
    			}
    			var cell = $('<td>' + score.attrTwo.name + ':' + score.attrTwo.label + '</br>' + score.logDistance.toFixed(2) + '</td>');
    			if(score.logDistance > 0.7){
    				cell.css({'background':'green'});
    			}
    			row.append(cell);
    		});
    	});
    	
    	table.append(header).append(body);
    	$('#matrix').empty();
    	$('#matrix').append(table);
    }
    
    function retriveResult(callback){
    	var request = {
			'entity_one' : $('#entity_one').val(),
			'entity_two' : $('#entity_two').val()
		};
    	 $.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/report',
			data : JSON.stringify(request),
			contentType : 'application/json'
		}).done(function(data) {
			if(callback && typeof callback === "function"){
				callback(data);
			}
		});
    }
    
</script>
<@footer />