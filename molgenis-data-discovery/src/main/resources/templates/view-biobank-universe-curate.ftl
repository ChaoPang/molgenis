<#include 'molgenis-header.ftl'>
<#include 'molgenis-footer.ftl'>
<#assign css=['jasny-bootstrap.min.css','biobank-universe-curate.css']>
<#assign js=['jasny-bootstrap.min.js', 'biobank-universe-curate.js']>
<@header css js/>

<div class="jumbotron">
    <h2 class="display-3">Curate matches</h2>
    <hr class="my-2">
    <p>The matches have been generated by BiobankUniverse, you can decide on the final matches.</p>
</div>

<div class="row">
    <div class="col-md-12">
        <div class="panel panel-primary">
            <div class="panel-heading"><h3 class="panel-title">Matching result</h3></div>
            <div class="panel-body">
                <div class="row">
                    <div class="col-md-offset-3 col-md-6">
                        <div class="panel panel-default">
                            <div class="panel-body">
                                <form id="downloadForm" method="get"
                                      action="${context_url}/universe/download/${biobankUniverse.identifier?html}">
                                    <input type="hidden" name="targetSampleCollectionName"
                                           value="${targetSampleCollection.name?html}"/>
                                </form>
                                <form method="get" action="${context_url}/universe/${biobankUniverse.identifier?html}">
                                    <label for="targetSampleCollectionName">Select a target to curate matches</label>
                                    <select id="targetSampleCollectionName" name="targetSampleCollectionName"
                                            class="form-control">
                                    <#list sampleCollections as member>
                                        <option value="${member.name?html}"
                                                <#if member.name==targetSampleCollection.name>selected</#if>>${member.name?html}</option>
                                    </#list>
                                    </select><br>
                                    <button typle="submit" class="btn btn-default">Retrieve</button>
                                    <button typle="submit" form="downloadForm" class="btn btn-primary">Download</button>
                                </form>

                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="panel panel-default">
                            <div class="panel-body">
                                <div class="form-group">
                                    <button type="button" class="btn btn-default btn-xs disabled">
                                        <span class="glyphicon glyphicon-pencil"></span>
                                    </button>
                                    <label class="col-form-label pull-right">To be curated</label>
                                </div>
                                <div class="form-group">
                                    <button type="button" class="btn btn-success btn-xs disabled">
                                        <span class="glyphicon glyphicon-ok"></span>
                                    </button>
                                    <label class="col-form-label pull-right">Curated matches</label>
                                </div>
                                <div class="form-group">
                                    <button type="button" class="btn btn-danger btn-xs disabled">
                                        <span class="glyphicon glyphicon-ok"></span>
                                    </button>
                                    <label class="col-form-label pull-right">Curated no matches</label>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-md-12">
                <#if (attributeMappingTablePager.totalPage > 0)>
                    <nav aria-label="Page navigation" class="pull-right attribute-mapping-table-pager">
                        <ul class="pagination">
                            <#if attributeMappingTablePager.currentPage == 1>
                                <#assign previousPageNumber = 1>
                                <#assign previousPagerClass = "disabled">
                            <#else>
                                <#assign previousPageNumber = attributeMappingTablePager.currentPage - 1>
                                <#assign previousPagerClass = "">
                            </#if>
                            <li class="${previousPagerClass}">
                                <a href="${context_url}/universe/${biobankUniverse.identifier?html}?targetSampleCollectionName=${targetSampleCollection.name?html}&page=${previousPageNumber}"
                                   aria-label="Previous">
                                    <span aria-hidden="true">&laquo;</span>
                                </a>
                            </li>
                            <#list 1..attributeMappingTablePager.totalPage as pageNumber>
                                <li class="<#if attributeMappingTablePager.currentPage == pageNumber>active</#if>">
                                    <a href="${context_url}/universe/${biobankUniverse.identifier?html}?targetSampleCollectionName=${targetSampleCollection.name?html}&page=${pageNumber}">${pageNumber?html}</a>
                                </li>
                            </#list>
                            <#if attributeMappingTablePager.currentPage == attributeMappingTablePager.totalPage>
                                <#assign nextPageNumber = attributeMappingTablePager.currentPage>
                                <#assign nextPagerClass = "disabled">
                            <#else>
                                <#assign nextPageNumber = attributeMappingTablePager.currentPage + 1>
                                <#assign nextPagerClass = "">
                            </#if>
                            <li class="${nextPagerClass}">
                                <a href="${context_url}/universe/${biobankUniverse.identifier?html}?targetSampleCollectionName=${targetSampleCollection.name?html}&page=${nextPageNumber}"
                                   aria-label="Next">
                                    <span aria-hidden="true">&raquo;</span>
                                </a>
                            </li>
                        </ul>
                    </nav>
                </#if>
                </div>
            </div>
        </div>
    <#if candidateMappingCandidates??>
        <table class="table table-bordered">
            <#if (candidateMappingCandidates?keys?size > 0)>
                <#assign firstAttributeName = candidateMappingCandidates?keys[0] />
                <thead>
                <tr>
                    <th>Target attributes</th>
                    <#list candidateMappingCandidates[firstAttributeName]?keys as sampleCollection>
                        <th>Source: ${sampleCollection?html}</th>
                    </#list>
                </tr>
                </thead>
                <tbody>
                    <#list candidateMappingCandidates?keys as attributeName>
                    <tr>
                        <td><strong>${attributeName?html}</strong></td>
                        <#assign candidateMatcheMap =candidateMappingCandidates[attributeName]/>
                        <#list candidateMatcheMap?keys as sourceSampleCollectionName>
                            <#assign attributeMatchingCell = candidateMatcheMap[sourceSampleCollectionName]>
                            <td>
                                <!-- This is what is shown in the cell of the overview table -->
                                <#if attributeMatchingCell == "CURATED_MATCHES">
                                    <button type="button" class="btn btn-success btn-xs" data-toggle="modal"
                                            data-target="#${attributeName}-${sourceSampleCollectionName}">
                                        <span class="glyphicon glyphicon-ok"></span>
                                    </button>
                                <#elseif attributeMatchingCell == "CURATED_NO_MATCHES">
                                    <button type="button" class="btn btn-danger btn-xs" data-toggle="modal"
                                            data-target="#${attributeName}-${sourceSampleCollectionName}">
                                        <span class="glyphicon glyphicon-ok"></span>
                                    </button>
                                <#else>
                                    <button type="button" class="btn btn-default btn-xs" data-toggle="modal"
                                            data-target="#${attributeName}-${sourceSampleCollectionName}">
                                        <span class="glyphicon glyphicon-pencil"></span>
                                    </button>
                                </#if>

                                <!-- This is the popup where users can make decisions on the candidate matches -->
                                <div class="modal fade modal-wide attribute-candidate-match-modal"
                                     id="${attributeName}-${sourceSampleCollectionName}"
                                     tabindex="-1"
                                     role="dialog"
                                     aria-labelledby="myModalLabel" aria-hidden="true">
                                    <#assign attribute = biobankSampleAttributeMap[attributeName]>
                                    <form method="post"
                                          action="${context_url}/universe/${biobankUniverse.identifier?html}/curate">
                                        <input type="hidden" name="biobankUniverse"
                                               value="${biobankUniverse.identifier?html}">
                                        <input type="hidden" name="targetAttribute"
                                               value="${attribute.identifier?html}">
                                        <input type="hidden" name="sourceAttributes" , value="">
                                        <input type="hidden" name="targetSampleCollection"
                                               value="${targetSampleCollection.name}">
                                        <input type="hidden" name="sourceSampleCollection"
                                               value="${sourceSampleCollectionName}">
                                        <input type="hidden" name="page"
                                               value="${attributeMappingTablePager.currentPage?html}">
                                        <div class="modal-dialog" role="document">
                                            <div class="modal-content">
                                                <div class="modal-header">
                                                    <button type="button" class="close" data-dismiss="modal"
                                                            aria-label="Close">
                                                        <span aria-hidden="true">&times;</span>
                                                    </button>
                                                    <h4 class="modal-title" id="myModalLabel">
                                                        Curate ${attributeName?html} in the
                                                        Source: ${sourceSampleCollectionName?html}</h4>
                                                </div>
                                                <div class="modal-body">
                                                    <div>
                                                        Target attribute: </br>
                                                        <table class="table table-borded">
                                                            <tr>
                                                                <th>Name</th>
                                                                <td>${attribute.name?html}</td>
                                                            </tr>
                                                            <tr>
                                                                <th>Label</th>
                                                                <td>${attribute.label?html}</td>
                                                            </tr>
                                                            <tr>
                                                                <th>Data type</th>
                                                                <td>${attribute.biobankAttributeDataType?html}</td>
                                                            </tr>
                                                        </table>
                                                    </div>
                                                    <div name="attribute-candidate-match-container">
                                                    </div>
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-secondary"
                                                            data-dismiss="modal">Close
                                                    </button>
                                                    <button type="submit" class="btn btn-primary">Submit
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </form>
                                </div>
                            </td>
                        </#list>
                    </tr>
                    </#list>
                </tbody>
            </#if>
        </table>
    </#if>

    </div>
</div>
</div>
<@footer/>