<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<@header />
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<div class="form-group">
			<button id="button-one" type="button" class="btn btn-primary">${entity_one}</button>
		</div>
		<div class="form-group">
			<button id="button-two" type="button" class="btn btn-primary">${entity_two}</button>
		</div>
	</div>
</div>

<div class="row">
	<div id="matrix" class="col-md-12" style="overflow:scroll;">
		
	</div>
</div>

<script>
	$(function (){
		$.each($('button'), function(i, button){
			$(button).click(function(){
				var entityName = $(this).html();
				var request = {'entityName':entityName};
				$.ajax({
					type : 'POST',
					url : molgenis.getContextUrl() + '/annotate',
					data : JSON.stringify(request),
					contentType : 'application/json'
				}).done(function(data) {
					console.log(data);
					builtMatrix(data);
				});
			});
		});
    });
    function builtMatrix(data){;
    	var table = $('<table />').addClass('table table-bordered');
    	var header = $('<thead><tr><th>Attribute Name</th><th>Ontology Term</th><th>Score</th></tr></thead>');
    	var body = $('<tbody />');
    	$.map(data, function(hit, name){
    		var score = hit.scoreInt / 1000;
    		var result = hit.result;
    		var row = $('<tr />').appendTo(body);
    		$('<td>' + name + '</td>').appendTo(row);
    		$('<td>' + result.IRI + '<br />' + result.label + '</td>').appendTo(row);
    		$('<td>' + score + '%</td>').appendTo(row);
    	});
    	table.append(header).append(body);
    	$('#matrix').empty();
    	$('#matrix').append(table);
    }
  
</script>
<@footer />