package org.openmetadata.service.resources.services.searchIndexes;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.json.JsonPatch;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.openmetadata.schema.api.data.RestoreEntity;
import org.openmetadata.schema.api.services.CreateSearchService;
import org.openmetadata.schema.entity.services.SearchService;
import org.openmetadata.schema.entity.services.ServiceType;
import org.openmetadata.schema.entity.services.connections.TestConnectionResult;
import org.openmetadata.schema.type.EntityHistory;
import org.openmetadata.schema.type.Include;
import org.openmetadata.schema.type.MetadataOperation;
import org.openmetadata.schema.type.SearchConnection;
import org.openmetadata.schema.utils.EntityInterfaceUtil;
import org.openmetadata.service.Entity;
import org.openmetadata.service.jdbi3.SearchServiceRepository;
import org.openmetadata.service.resources.Collection;
import org.openmetadata.service.resources.services.ServiceEntityResource;
import org.openmetadata.service.security.Authorizer;
import org.openmetadata.service.security.policyevaluator.OperationContext;
import org.openmetadata.service.util.JsonUtils;
import org.openmetadata.service.util.ResultList;

@Slf4j
@Path("/v1/services/searchServices")
@Tag(
    name = "Search Services",
    description = "APIs related `Search Service` entities, such as ElasticSearch, OpenSearch.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Collection(name = "searchServices")
public class SearchServiceResource
    extends ServiceEntityResource<SearchService, SearchServiceRepository, SearchConnection> {
  public static final String COLLECTION_PATH = "v1/services/searchServices/";
  static final String FIELDS = "pipelines,owner,tags,domain";

  @Override
  public SearchService addHref(UriInfo uriInfo, SearchService service) {
    super.addHref(uriInfo, service);
    Entity.withHref(uriInfo, service.getPipelines());
    return service;
  }

  public SearchServiceResource(Authorizer authorizer) {
    super(Entity.SEARCH_SERVICE, authorizer, ServiceType.SEARCH);
  }

  @Override
  protected List<MetadataOperation> getEntitySpecificOperations() {
    addViewOperation("pipelines", MetadataOperation.VIEW_BASIC);
    return null;
  }

  public static class SearchServiceList extends ResultList<SearchService> {
    /* Required for serde */
  }

  @GET
  @Operation(
      operationId = "listSearchServices",
      summary = "List search services",
      description = "Get a list of search services.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "List of search service instances",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(implementation = SearchServiceResource.SearchServiceList.class)))
      })
  public ResultList<SearchService> list(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(
              description = "Fields requested in the returned resource",
              schema = @Schema(type = "string", example = FIELDS))
          @QueryParam("fields")
          String fieldsParam,
      @Parameter(
              description = "Filter services by domain",
              schema = @Schema(type = "string", example = "Marketing"))
          @QueryParam("domain")
          String domain,
      @DefaultValue("10") @Min(0) @Max(1000000) @QueryParam("limit") int limitParam,
      @Parameter(
              description = "Returns list of search services before this cursor",
              schema = @Schema(type = "string"))
          @QueryParam("before")
          String before,
      @Parameter(
              description = "Returns list of search services after this cursor",
              schema = @Schema(type = "string"))
          @QueryParam("after")
          String after,
      @Parameter(
              description = "Include all, deleted, or non-deleted entities.",
              schema = @Schema(implementation = Include.class))
          @QueryParam("include")
          @DefaultValue("non-deleted")
          Include include) {
    return listInternal(
        uriInfo, securityContext, fieldsParam, include, domain, limitParam, before, after);
  }

  @GET
  @Path("/{id}")
  @Operation(
      operationId = "getSearchServiceByID",
      summary = "Get an search service",
      description = "Get an search service by `id`.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "search service instance",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class))),
        @ApiResponse(
            responseCode = "404",
            description = "search service for instance {id} is not found")
      })
  public SearchService get(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @PathParam("id") UUID id,
      @Parameter(
              description = "Fields requested in the returned resource",
              schema = @Schema(type = "string", example = FIELDS))
          @QueryParam("fields")
          String fieldsParam,
      @Parameter(
              description = "Include all, deleted, or non-deleted entities.",
              schema = @Schema(implementation = Include.class))
          @QueryParam("include")
          @DefaultValue("non-deleted")
          Include include) {
    SearchService searchService = getInternal(uriInfo, securityContext, id, fieldsParam, include);
    return decryptOrNullify(securityContext, searchService);
  }

  @GET
  @Path("/name/{name}")
  @Operation(
      operationId = "getSearchServiceByFQN",
      summary = "Get search service by name",
      description = "Get a search service by the service `name`.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "search service instance",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class))),
        @ApiResponse(
            responseCode = "404",
            description = "search service for instance {id} is not found")
      })
  public SearchService getByName(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @PathParam("name") String name,
      @Parameter(
              description = "Fields requested in the returned resource",
              schema = @Schema(type = "string", example = FIELDS))
          @QueryParam("fields")
          String fieldsParam,
      @Parameter(
              description = "Include all, deleted, or non-deleted entities.",
              schema = @Schema(implementation = Include.class))
          @QueryParam("include")
          @DefaultValue("non-deleted")
          Include include) {
    SearchService searchService =
        getByNameInternal(
            uriInfo, securityContext, EntityInterfaceUtil.quoteName(name), fieldsParam, include);
    return decryptOrNullify(securityContext, searchService);
  }

  @PUT
  @Path("/{id}/testConnectionResult")
  @Operation(
      operationId = "addTestConnectionResult",
      summary = "Add test connection result",
      description = "Add test connection result to the service.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully updated the service",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class)))
      })
  public SearchService addTestConnectionResult(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(description = "Id of the service", schema = @Schema(type = "UUID"))
          @PathParam("id")
          UUID id,
      @Valid TestConnectionResult testConnectionResult) {
    OperationContext operationContext = new OperationContext(entityType, MetadataOperation.CREATE);
    authorizer.authorize(securityContext, operationContext, getResourceContextById(id));
    SearchService service = repository.addTestConnectionResult(id, testConnectionResult);
    return decryptOrNullify(securityContext, service);
  }

  @GET
  @Path("/{id}/versions")
  @Operation(
      operationId = "listAllSearchServiceVersion",
      summary = "List search service versions",
      description = "Get a list of all the versions of an search service identified by `id`",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "List of search service versions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EntityHistory.class)))
      })
  public EntityHistory listVersions(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(description = "search service Id", schema = @Schema(type = "string"))
          @PathParam("id")
          UUID id) {
    EntityHistory entityHistory = super.listVersionsInternal(securityContext, id);

    List<Object> versions =
        entityHistory.getVersions().stream()
            .map(
                json -> {
                  try {
                    SearchService searchService =
                        JsonUtils.readValue((String) json, SearchService.class);
                    return JsonUtils.pojoToJson(decryptOrNullify(securityContext, searchService));
                  } catch (Exception e) {
                    return json;
                  }
                })
            .collect(Collectors.toList());
    entityHistory.setVersions(versions);
    return entityHistory;
  }

  @GET
  @Path("/{id}/versions/{version}")
  @Operation(
      operationId = "getSpecificSearchServiceVersion",
      summary = "Get a version of the search service",
      description = "Get a version of the search service by given `id`",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "search service",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class))),
        @ApiResponse(
            responseCode = "404",
            description =
                "Object store service for instance {id} and version {version} is not found")
      })
  public SearchService getVersion(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(description = "search service Id", schema = @Schema(type = "string"))
          @PathParam("id")
          UUID id,
      @Parameter(
              description = "search service version number in the form `major`" + ".`minor`",
              schema = @Schema(type = "string", example = "0.1 or 1.1"))
          @PathParam("version")
          String version) {
    SearchService searchService = super.getVersionInternal(securityContext, id, version);
    return decryptOrNullify(securityContext, searchService);
  }

  @POST
  @Operation(
      operationId = "createSearchService",
      summary = "Create search service",
      description = "Create a new search service.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Search service instance",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class))),
        @ApiResponse(responseCode = "400", description = "Bad request")
      })
  public Response create(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Valid CreateSearchService create) {
    SearchService service = getService(create, securityContext.getUserPrincipal().getName());
    Response response = create(uriInfo, securityContext, service);
    decryptOrNullify(securityContext, (SearchService) response.getEntity());
    return response;
  }

  @PUT
  @Operation(
      operationId = "createOrUpdateSearchService",
      summary = "Update search service",
      description = "Update an existing or create a new search service.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Object store service instance",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class))),
        @ApiResponse(responseCode = "400", description = "Bad request")
      })
  public Response createOrUpdate(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Valid CreateSearchService update) {
    SearchService service = getService(update, securityContext.getUserPrincipal().getName());
    Response response = createOrUpdate(uriInfo, securityContext, unmask(service));
    decryptOrNullify(securityContext, (SearchService) response.getEntity());
    return response;
  }

  @PATCH
  @Path("/{id}")
  @Operation(
      operationId = "patchSearchService",
      summary = "Update an search service",
      description = "Update an existing search service using JsonPatch.",
      externalDocs =
          @ExternalDocumentation(
              description = "JsonPatch RFC",
              url = "https://tools.ietf.org/html/rfc6902"))
  @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
  public Response patch(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @PathParam("id") UUID id,
      @RequestBody(
              description = "JsonPatch with array of operations",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_PATCH_JSON,
                      examples = {
                        @ExampleObject("[{op:remove, path:/a},{op:add, path: /b, value: val}]")
                      }))
          JsonPatch patch) {
    return patchInternal(uriInfo, securityContext, id, patch);
  }

  @DELETE
  @Path("/{id}")
  @Operation(
      operationId = "deleteSearchService",
      summary = "Delete an search service",
      description =
          "Delete an search services. If containers belong the service, it can't be deleted.",
      responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
            responseCode = "404",
            description = "SearchService service for instance {id} is not found")
      })
  public Response delete(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(
              description = "Recursively delete this entity and it's children. (Default `false`)")
          @DefaultValue("false")
          @QueryParam("recursive")
          boolean recursive,
      @Parameter(description = "Hard delete the entity. (Default = `false`)")
          @QueryParam("hardDelete")
          @DefaultValue("false")
          boolean hardDelete,
      @Parameter(description = "Id of the search service", schema = @Schema(type = "string"))
          @PathParam("id")
          UUID id) {
    return delete(uriInfo, securityContext, id, recursive, hardDelete);
  }

  @DELETE
  @Path("/name/{fqn}")
  @Operation(
      operationId = "deleteSearchServiceByFQN",
      summary = "Delete an SearchService by fully qualified name",
      description = "Delete an SearchService by `fullyQualifiedName`.",
      responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
            responseCode = "404",
            description = "SearchService for instance {fqn} is not found")
      })
  public Response delete(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Parameter(
              description = "Recursively delete this entity and it's children. (Default `false`)")
          @DefaultValue("false")
          @QueryParam("recursive")
          boolean recursive,
      @Parameter(description = "Hard delete the entity. (Default = `false`)")
          @QueryParam("hardDelete")
          @DefaultValue("false")
          boolean hardDelete,
      @Parameter(description = "Name of the SearchService", schema = @Schema(type = "string"))
          @PathParam("fqn")
          String fqn) {
    return deleteByName(
        uriInfo, securityContext, EntityInterfaceUtil.quoteName(fqn), recursive, hardDelete);
  }

  @PUT
  @Path("/restore")
  @Operation(
      operationId = "restore",
      summary = "Restore a soft deleted SearchService.",
      description = "Restore a soft deleted SearchService.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully restored the SearchService.",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchService.class)))
      })
  public Response restoreSearchService(
      @Context UriInfo uriInfo,
      @Context SecurityContext securityContext,
      @Valid RestoreEntity restore) {
    return restoreEntity(uriInfo, securityContext, restore.getId());
  }

  private SearchService getService(CreateSearchService create, String user) {
    return repository
        .copy(new SearchService(), create, user)
        .withServiceType(create.getServiceType())
        .withConnection(create.getConnection());
  }

  @Override
  protected SearchService nullifyConnection(SearchService service) {
    return service.withConnection(null);
  }

  @Override
  protected String extractServiceType(SearchService service) {
    return service.getServiceType().value();
  }
}
