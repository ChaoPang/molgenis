<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=["jasny-bootstrap.min.css"]>
<#assign js=["jasny-bootstrap.min.js", 'biobank-universes.js', 'bootbox.min.js']>
<@header css js/>
<div class="row">
	<center><h2>Manage biobank sample collections</h2></center>
</div>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<form action="${context_url}/importSample" method="post" enctype="multipart/form-data">
			<br><br>
			<div class="row">
				<div class="col-md-12 well">
					<div class="form-group">
				    	<label for="sampleName">Sample name: </label>
				    	<input type="text" class="form-control" id="sampleName" name="sampleName" placeholder="Prevned">
				  	</div>
					<div class="fileinput fileinput-new" data-provides="fileinput">
						<label>Sample attribute file:</label>
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
							<button id="upload-button" type="button" class="btn btn-primary">Upload</button>
						</div>
					</div>
					<div class="form-group">
				    	<label for="separator">File separator: </label>
				    	<select class="form-control" id="separator" name="separator">
				    		<option value=",">,</option>
				    		<option value=";">;</option>
				    		<option value="|">|</option>
				    	<select>
				  	</div>
					<hr/>
					<div>
						<button type="submit" class="btn btn-default">Import data</button>
					</div>
				</div>
			</div>
		</form>
	</div>	
</div>
<div class="row">
	<div class="col-md-offset-3 col-md-6">
		<#if biobankSampleCollections?has_content>
			<table class="table table-striped">
				<tr><th>Biobank samples</th><th>Remove all tags</th><th>Delete</th></tr>
				<#list biobankSampleCollections as biobankSampleCollection>
					<tr>
						<td>${biobankSampleCollection.name?html}</td>
						<td>
							<form method="post" action="${context_url}/removeTagGroups" class="verify">
								<input type="hidden" name="sampleName" value="${biobankSampleCollection.name}"/>
								<button type="submit" class="btn btn-danger btn-xs"><span class="glyphicon glyphicon-trash"></span></button>
							</form>
						</td>
						<td>
							<form method="post" action="${context_url}/removeBiobankSampleCollection" class="verify">
								<input type="hidden" name="sampleName" value="${biobankSampleCollection.name}"/>
								<button type="submit" class="btn btn-danger btn-xs"><span class="glyphicon glyphicon-trash"></span></button>
							</form>
						</td>
					</tr>
				</#list>
			</table>
		</#if>
	</div>
</div>

<@footer/>