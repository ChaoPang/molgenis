<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=[]>
<#assign js=['biobank-universes.js','bootbox.min.js']>
<@header css js/>
<@createNewBiobankUniverseModal />
<div class="row">
	<div class="col-md-10">
		<h1>Biobank universe overview</h1>
		<p>Create and view Biobank universes.</p>
		<#if biobankSampleCollections?has_content>
			<div class="btn-group" role="group">
				<button type="button" id="add-mapping-project-btn" class="btn btn-primary" data-toggle="modal" data-target="#create-new-biobank-universe-modal"><span class="glyphicon glyphicon-plus"></span>&nbsp;Add Biobank universe</button>
			</div>
		</#if>	
		<hr/>
	</div>
</div>
<div class="row">
	<div class="col-md-10">
		<#if biobankUniverses?has_content>
			<table class="table table-bordered" id="biobank-universes-tbl">
	 			<thead>
	 				<tr>
	 					<th></th>
	 					<th>Biobank universe</th>
	 					<th>Owner</th>
	 					<th>Members</th>
	 					<th>Key concepts</th>
	 					<th>Progress</th>
	 				</tr>
	 			</thead>
	 			<tbody>
	 				<#list biobankUniverses as universe>
					<tr>	
	 					<td>
	 						<form method="post" action="${context_url}/removeBiobankuniverse" class="pull-left verify">
								<input type="hidden" name="biobankUniverseId" value="${universe.identifier}"/>
								<button type="submit" class="btn btn-danger btn-xs"><span class="glyphicon glyphicon-trash"></span></button>
							</form>
	 					</td> 					
	 					<td><a href="${context_url}/universe/${universe.identifier}">${universe.name?html}</a></td>
	 					<td>${universe.owner.username?html}</td>
	 					<td>
							<a id="add-new-member-btn" href="#" class="btn btn-primary btn-xs" data-toggle="modal" data-target="#model-${universe.identifier}"><span class="glyphicon glyphicon-plus"></span></a>
							<!-- add new member dialog -->
							<div class="modal" id="model-${universe.identifier}" tabindex="-1" role="dialog">
								<div class="modal-dialog">
							    	<div class="modal-content">
							    		<form id="create-new-members-form" method="post" action="${context_url}/addUniverseMembers">
								        	<div class="modal-header">
								        		<button type="button" class="close" data-dismiss="modal">&times;</button>
								        		<h4 class="modal-title" id="create-new-source-column-modal-label">Add new members</h4>
								        	</div>
								        	<div class="modal-body">
												<div class="form-group">
								            		<label>Add new members to the universe: ${universe.name?html}</label>
							  						<select name="biobankSampleCollectionNames" class="form-control" multiple="multiple">
								    					<#assign existingMembers = [] />
														<#list universe.members as member>
										 						<#assign existingMembers = existingMembers + [member.name] /><#if member_has_next>, </#if> 
									 					</#list>
														<#list biobankSampleCollections as sampleCollection>
															<#if !existingMembers?seq_contains(sampleCollection.name)>
							    							<option value="${sampleCollection.name?html}">${sampleCollection.name?html}</option>
							    							</#if>
								    					</#list>
													</select>
												</div>
												<input type="hidden" name="biobankUniverseId" value="${universe.identifier}">
								    		</div>
								        	<div class="modal-footer">
								        		<button type="submit" id="submit-new-member-column-btn" class="btn btn-primary">Add members</button>
								                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
								    		</div>	 
							    		</form>   				
									</div>
								</div>
							</div>
		 					<#list universe.members as member>
		 						${member.name}<#if member_has_next>, </#if> 
	 						</#list>
	 					</td>
	 					<td>
	 						<a id="add-new-key-concepts-btn" href="#" class="btn btn-primary btn-xs" data-toggle="modal" data-target="#model-key-concepts-${universe.identifier}"><span class="glyphicon glyphicon-plus"></span></a>
							<!-- add new member dialog -->
							<div class="modal" id="model-key-concepts-${universe.identifier}" tabindex="-1" role="dialog">
								<div class="modal-dialog">
							    	<div class="modal-content">
							    		<form id="create-new-key-concepts-form" method="post" action="${context_url}/addKeyConcepts">
								        	<div class="modal-header">
								        		<button type="button" class="close" data-dismiss="modal">&times;</button>
								        		<h4 class="modal-title" id="create-new-source-column-modal-label">Add new key concepts</h4>
								        	</div>
								        	<div class="modal-body">
												<div class="form-group">
								            		<label>Add new key concepts to the universe: ${universe.name?html}</label>
							  						<select name="semanticTypes" class="form-control" multiple="multiple">
								    					<#assign existingKeyConceptGroups = [] />
														<#list universe.keyConcepts as keyConcept>
										 					<#assign existingKeyConceptGroups = existingKeyConceptGroups + [keyConcept.name] /><#if keyConcept_has_next>, </#if> 
									 					</#list>
														<#list semanticTypeGroups as semanticTypeGroup>
															<#if !existingKeyConceptGroups?seq_contains(semanticTypeGroup)>
							    							<option value="${semanticTypeGroup?html}">${semanticTypeGroup?html}</option>
							    							</#if>
								    					</#list>
													</select>
												</div>
												<input type="hidden" name="biobankUniverseId" value="${universe.identifier}">
								    		</div>
								        	<div class="modal-footer">
								        		<button type="submit" id="submit-new-member-column-btn" class="btn btn-primary">Add key concepts</button>
								                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
								    		</div>	 
							    		</form>   				
									</div>
								</div>
							</div>
							<#assign existingKeyConceptGroups=[]>
	 						<#list universe.keyConcepts as keyConcept>
	 							<#if !existingKeyConceptGroups?seq_contains(keyConcept.name)>
									<#assign existingKeyConceptGroups = existingKeyConceptGroups + [keyConcept.name] />
								</#if>
	 						</#list>
							<#list existingKeyConceptGroups as semanticTypeGroup>
								${semanticTypeGroup}<#if semanticTypeGroup_has_next>, </#if>
							</#list>
	 					</td>	
	 					<td><div id="progress-bar-${universe.identifier}" name="progress-bar" biobank-universe="${universe.identifier}"></div></td>
	 				</tr>
	 				</#list>
	 			</tbody>
			</table>
		</#if>
	</div>
</div>
<@footer/>
<#macro createNewBiobankUniverseModal>
<div class="modal fade" id="create-new-biobank-universe-modal" tabindex="-1" role="dialog" aria-labelledby="create-new-biobank-universe-modal" aria-hidden="true">
	<div class="modal-dialog">
    	<div class="modal-content">
			<form method="post" action="${context_url}/addUniverse">
	        	<div class="modal-header">
	        		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	        		<h4 class="modal-title" id="create-new-mapping-project-label">Create a new biboank universe</h4>
	        	</div>
	        	<div class="modal-body">
					<div class="form-group">
	            		<label>Biobank universe name</label>
	  					<input name="universeName" type="text" class="form-control" placeholder="Universe name" required="required">
					</div>
					<hr></hr>	
					<div class="form-group">
						<label>Select biobank universe members</label>
						<select name="biobankSampleCollectionNames" class="form-control" multiple="multiple">
	    					<#list biobankSampleCollections as biobankSampleCollection>
	    						<option value="${biobankSampleCollection.name?html}">${biobankSampleCollection.name?html}</option>
	    					</#list>
						</select>
					</div>
					<hr></hr>	
					<div class="form-group">
						<label>Add semantic types as the key concepts</label>
						<select name="semanticTypes" class="form-control" multiple="multiple">
	    					<#list semanticTypeGroups as semanticTypeGroup>
	    						<option value="${semanticTypeGroup?html}">${semanticTypeGroup?html}</option>
	    					</#list>
						</select>
					</div>
	    		</div>
	        	<div class="modal-footer">
	        		<button type="submit" class="btn btn-primary">Create project</button>
	                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
	    		</div>
    		</form>	    				
		</div>
	</div>
</div>
</#macro>