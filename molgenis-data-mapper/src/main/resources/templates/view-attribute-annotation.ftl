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
    	$.map(data, function(hits, name){
    		var row = $('<tr />').appendTo(body);
    		var score = hits.length > 0 ? hits[0].scoreInt / 1000 : 0.0;
    		$('<td>' + name + '</td>').appendTo(row);
    		var column = $('<td />').appendTo(row);
    		for(var i = 0; i < hits.length; i++){
    			var ontologyTerm = hits[i].result.ontologyTerm;
    			column.append(ontologyTerm.IRI + ' : ' + ontologyTerm.label + '<br/>');
    		}	
    		$('<td>' + score + '%</td>').appendTo(row);
    	});
    	table.append(header).append(body);
    	$('#matrix').empty();
    	$('#matrix').append(table);
    }
  
</script>
<@footer />