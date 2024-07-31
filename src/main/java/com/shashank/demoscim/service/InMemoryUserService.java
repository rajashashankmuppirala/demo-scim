
package com.shashank.demoscim.service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.apache.directory.scim.core.repository.PatchHandler;
import org.apache.directory.scim.core.repository.Repository;
import org.apache.directory.scim.core.schema.SchemaRegistry;
import org.apache.directory.scim.server.exception.UnableToCreateResourceException;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.extension.EnterpriseExtension;
import org.apache.directory.scim.spec.filter.*;
import org.apache.directory.scim.spec.filter.attribute.AttributeReference;
import org.apache.directory.scim.spec.patch.PatchOperation;
import org.apache.directory.scim.spec.resources.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a singleton (effectively) Provider<User> with a memory-based
 * persistence layer.
 *
 */
@Service
public class InMemoryUserService implements Repository<ScimUser> {

  static final String DEFAULT_USER_ID = UUID.randomUUID().toString();
  static final String DEFAULT_USER_EXTERNAL_ID = "e" + DEFAULT_USER_ID;
  static final String DEFAULT_USER_DISPLAY_NAME = "User " + DEFAULT_USER_ID;
  static final String DEFAULT_USER_EMAIL_VALUE = "test@test.com";
  static final String DEFAULT_USER_EMAIL_TYPE = "work";

  private final Map<String, ScimUser> users = new HashMap<>();

  private final SchemaRegistry schemaRegistry;

  private final PatchHandler patchHandler;

  public InMemoryUserService(SchemaRegistry schemaRegistry, PatchHandler patchHandler) {
    this.schemaRegistry = schemaRegistry;
    this.patchHandler = patchHandler;
  }

  @PostConstruct
  public void init() {
    ScimUser user = new ScimUser();
    user.setId(DEFAULT_USER_ID);
    user.setExternalId(DEFAULT_USER_EXTERNAL_ID);
    user.setUserName(DEFAULT_USER_EXTERNAL_ID);
    user.setDisplayName(DEFAULT_USER_DISPLAY_NAME);
    user.setName(new Name()
        .setGivenName("Developer")
        .setFamilyName("Test"));
    Email email = new Email();
    email.setDisplay(DEFAULT_USER_EMAIL_VALUE);
    email.setValue(DEFAULT_USER_EMAIL_VALUE);
    email.setType(DEFAULT_USER_EMAIL_TYPE);
    email.setPrimary(true);
    user.setEmails(List.of(email));

    EnterpriseExtension enterpriseExtension = new EnterpriseExtension();
    enterpriseExtension.setEmployeeNumber("12345");
    EnterpriseExtension.Manager manager = new EnterpriseExtension.Manager();
    manager.setValue("test");
    enterpriseExtension.setManager(manager);
    user.addExtension(enterpriseExtension);

    users.put(user.getId(), user);
  }

  @Override
  public Class<ScimUser> getResourceClass() {
    return ScimUser.class;
  }

  /**
   * @see Repository#create(ScimResource)
   */
  @Override
  public ScimUser create(ScimUser resource) throws UnableToCreateResourceException {
    String id = UUID.randomUUID().toString();

    // check to make sure the user doesn't already exist
    boolean existingUserFound = users.values().stream()
      .anyMatch(user -> user.getUserName().equals(resource.getUserName()));
    if (existingUserFound) {
      throw new UnableToCreateResourceException(Response.Status.CONFLICT, "User '" + resource.getUserName() + "' already exists.");
    }
    resource.setId(id);
    users.put(id, resource);
    return resource;
  }

  @Override
  public ScimUser update(String id, String version, ScimUser resource, Set<AttributeReference> includedAttributeReferences, Set<AttributeReference> excludedAttributeReferences) throws ResourceException {
    users.put(id, resource);
    return resource;
  }

  @Override
  public ScimUser patch(String id, String version, List<PatchOperation> patchOperations, Set<AttributeReference> includedAttributeReferences, Set<AttributeReference> excludedAttributeReferences) throws ResourceException {
    ScimUser resource = patchHandler.apply(get(id), patchOperations);
    users.put(id, resource);
    return resource;
  }

  /**
   * @see Repository#get(String)
   */
  @Override
  public ScimUser get(String id) {
    return users.get(id);
  }

  /**
   * @see Repository#delete(String)
   */
  @Override
  public void delete(String id) {
    users.remove(id);
  }

  /**
   * @see Repository#find(Filter, PageRequest, SortRequest)
   */
  @Override
  public FilterResponse<ScimUser> find(Filter filter, PageRequest pageRequest, SortRequest sortRequest) {

    long count = pageRequest.getCount() != null ? pageRequest.getCount() : users.size();
    long startIndex = pageRequest.getStartIndex() != null
      ? pageRequest.getStartIndex() - 1 // SCIM is 1-based indexed
      : 0;

    List<ScimUser> result = users.values().stream()
      .skip(startIndex)
      .limit(count)
      .filter(FilterExpressions.inMemory(filter, schemaRegistry.getSchema(ScimUser.SCHEMA_URI)))
      .collect(Collectors.toList());

    return new FilterResponse<>(result, pageRequest, result.size());
  }
}
