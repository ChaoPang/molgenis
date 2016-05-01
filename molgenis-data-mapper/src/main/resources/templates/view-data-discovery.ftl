<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=['data-discovery.css']>
<#assign js=['data-discovery.js']>
<@header css js/>
<form method="post" action="${context_url}/searchTerm">
	<div class="row">
		<br><br>
		<div class="col-md-12">
			<center><h2>Deep data discovery</h2></center>
		</div>	
	</div>
	<div class="row">
		<br>
		<div class="col-md-offset-4 col-md-4 well">
			<div class="form-group">
				<label>Select datasets to discover data items</label>
				<select id="entityNames" name="entityNames" multiple="multiple" class="form-control">
					<#list entityMetaDatas as entityMetaData>
						<option value="${entityMetaData.name?html}" selected>${entityMetaData.name?html}</option>
					</#list>
				</select>
			</div>
			<div class="form-group">
			    <label>
					<input name="exactMatch" type="checkbox"> Exact match
			    </label>
			</div>
			<div class="form-group">
			    <label>Select datasets to discover data items</label>
				<select id="ontologyLevel" name="ontologyLevel" class="form-control">
					<#list [1,2,3,4,5,6,7,8,9,10] as level>
						<option value="${level?html}" <#if level == 3>selected</#if>>${level?html}</option>
					</#list>
				</select>
			</div>
		</div>	
	</div>
	<div class="row">
		<br>
		<div class="col-md-offset-3 col-md-6">
			<div class="form-group">
				<div class="input-group">
					<input type="text" class="form-control" id="searchTerm" name="searchTerm" placeholder="Search data items"/>
					<span class="input-group-btn"><button type="submit" class="btn btn-primary">Search</button></span>
					<span class="input-group-btn"><button type="button" class="btn btn-default">Clear</button></span>
				</div>
			</div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-offset-1 col-md-10">
		<#if searchResult??>
		<#assign keys = searchResult?keys>
		<#list keys as key>
			<#assign index = keys?seq_index_of(key)>
			<#if index == 0>
			<div class="row">
			<#elseif index % 3 == 0>
			</div><div class="row">
			</#if>
			<div class="col-md-4 well data-discovery-panel">
				Dataset: ${key}<br>
				Matched attribtues: ${searchResult[key]?size}<br><br>
				<#if (searchResult[key]?size > 0)>
					<table class="table">
						<tr>
							<th>Name</th>
							<th>Label</th>
						</tr>
						<#list searchResult[key] as matchedAttribute>
							<#if searchResult[key]?seq_index_of(matchedAttribute) == 3>
								<#break>
							</#if>
						<tr>
							<td>${matchedAttribute.attributeMetaData.name}</td>
							<td>${matchedAttribute.attributeMetaData.label}</td>
						</tr>
						</#list>
					</table>
				</#if>
			</div>
			<#if index==keys?size - 1>
			</div>
			</#if>
		</#list>
		</#if>
		</div>
	</div>
</form>
<@footer />