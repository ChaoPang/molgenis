<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=["jasny-bootstrap.min.css"]>
<#assign js=["jasny-bootstrap.min.js", 'biobank-universes.js', 'bootbox.min.js']>
<@header css js/>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<form method="post" action="${context_url}/test/upload", enctype="multipart/form-data">	
			<div class="form-group">
				<label>Select the target</label>
				<select name="target" class="form-control">
					<#list biobankSampleCollections as biobankSampleCollection>
						<option value="${biobankSampleCollection.name?html}">${biobankSampleCollection.name?html}</option>
					</#list>
				</select>
			</div>
			<hr></hr>
			<div class="form-group">
				<label>Select the sources</label>
				<select name="sources" class="form-control" multiple="multiple">
					<#list biobankSampleCollections as biobankSampleCollection>
						<option value="${biobankSampleCollection.name?html}">${biobankSampleCollection.name?html}</option>
					</#list>
				</select>
			</div>
			<hr></hr>
			<div class="fileinput fileinput-new" data-provides="fileinput">
				<div class="group-append">
					<div class="uneditable-input">
						<i class="icon-file fileinput-exists"></i>
						<span class="fileinput-preview"></span>
					</div>
					<span class="btn btn-file btn-info">
						<span class="fileinput-new">Select file</span>
						
						<span class="fileinput-exists">Change</span>
						<input type="file" id="file" name="file" required/>
					</span>
					<a href="#" class="btn btn-danger fileinput-exists" data-dismiss="fileinput">Remove</a>
					<button id="upload-button" type="submit" class="btn btn-primary">Upload</button>
				</div>
			</div>
		</form>
	</div>
</div>
<div class="row">
	<div class="col-md-offset-1 col-md-10">
		<form method="get" action="${context_url}/test/calculate">
			<div class="form-group">
				<label>Select the biobank universe</label>
				<select name="biobankUniverseIdentifier" class="form-control" multiple="multiple">
					<#list biobankUniverses as biobankUniverse>
						<option value="${biobankUniverse.identifier?html}">${biobankUniverse.name?html}</option>
					</#list>
				</select>
			</div>
			<button id="calculate-button" type="submit" class="btn btn-primary">calculate</button><br/><br/>
			<#if collectionSimilarityMap??>
				<table class="table table-striped table-condensed">
				<tr>
					<th></th>
					<#list collectionSimilarityMap?keys?reverse as collection>
						<th>${collection}</th>
					</#list>
				</tr>
				<#list collectionSimilarityMap?keys as collection>
					<tr>
						<td>${collection}</td>
						<#assign collectionSimilarities = collectionSimilarityMap[collection]>
						<#list collectionSimilarities?reverse as collectionSimilarity>
							<td>
							<#if collectionSimilarity.similarity != 0.0>${collectionSimilarity.similarity}</#if>
							</td>
						</#list>
					</tr>
				</#list>
				</table>
			</#if>
		</form>
	</div>
</div>
<script>
	$(document).ready(function() {
	    $('select').select2();
	});
</script>
<@footer/>