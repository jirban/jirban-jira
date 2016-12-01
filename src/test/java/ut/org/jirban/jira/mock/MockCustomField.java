/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ut.org.jirban.jira.mock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.admin.RenderableProperty;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FieldJsonRepresentation;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfo;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfoContext;
import com.atlassian.jira.issue.fields.rest.RestFieldOperationsHandler;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.JsonType;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import com.atlassian.jira.issue.fields.util.MessagedResult;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.opensymphony.module.propertyset.PropertySet;

import webwork.action.Action;

/**
 * @author Kabir Khan
 */
public class MockCustomField implements CustomField {

    private final long id;
    private final String name;

    public MockCustomField(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean isInScope(Project project, List<String> list) {
        return true;
    }

    @Override
    public boolean isInScope(long l, String s) {
        return true;
    }

    @Override
    public boolean isInScopeForSearch(@Nullable Project project, @Nullable List<String> list) {
        return true;
    }

    @Override
    public boolean isInScope(SearchContext searchContext) {
        return true;
    }

    @Override
    public GenericValue getGenericValue() {
        return null;
    }

    @Override
    public int compare(Issue issue, Issue issue1) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public CustomFieldParams getCustomFieldValues(Map map) {
        return null;
    }

    @Override
    public Object getValue(Issue issue) {
        return null;
    }

    @Override
    public Set<Long> remove() {
        return null;
    }

    @Override
    public Options getOptions(String s, JiraContextNode jiraContextNode) {
        return null;
    }


    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUntranslatedDescription() {
        return null;
    }

    @Override
    public String getFieldName() {
        return name;
    }

    @Override
    public String getUntranslatedName() {
        return null;
    }

    @Nonnull
    @Override
    public RenderableProperty getDescriptionProperty() {
        return null;
    }

    @Nonnull
    @Override
    public RenderableProperty getUntranslatedDescriptionProperty() {
        return null;
    }


    @Override
    public CustomFieldSearcher getCustomFieldSearcher() {
        return null;
    }



    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Long getIdAsLong() {
        return id;
    }

    @Override
    public List<FieldConfigScheme> getConfigurationSchemes() {
        return null;
    }

    @Override
    public Options getOptions(String s, FieldConfig fieldConfig, JiraContextNode jiraContextNode) {
        return null;
    }

    @Override
    public FieldConfig getRelevantConfig(Issue issue) {
        return null;
    }

    @Override
    public void validateFromActionParams(Map map, ErrorCollection errorCollection, FieldConfig fieldConfig) {

    }

    @Override
    public List<Project> getAssociatedProjectObjects() {
        return null;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    public boolean isAllProjects() {
        return false;
    }

    @Override
    public boolean isAllIssueTypes() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public CustomFieldType getCustomFieldType() {
        return null;
    }

    @Override
    public boolean isRelevantForIssueContext(IssueContext issueContext) {
        return false;
    }

    @Override
    public FieldConfig getRelevantConfig(IssueContext issueContext) {
        return null;
    }

    @Override
    public FieldConfig getReleventConfig(SearchContext searchContext) {
        return null;
    }

    @Override
    public ClauseNames getClauseNames() {
        return null;
    }

    @Override
    public PropertySet getPropertySet() {
        return null;
    }

    @Override
    public List<FieldConfigItemType> getConfigurationItemTypes() {
        return null;
    }

    @Override
    public String getColumnHeadingKey() {
        return null;
    }

    @Override
    public String getColumnCssClass() {
        return null;
    }

    @Override
    public String getDefaultSortOrder() {
        return null;
    }

    @Override
    public FieldComparatorSource getSortComparatorSource() {
        return null;
    }

    @Override
    public List<SortField> getSortFields(boolean b) {
        return null;
    }

    @Override
    public LuceneFieldSorter getSorter() {
        return null;
    }

    @Override
    public String getColumnViewHtml(FieldLayoutItem fieldLayoutItem, Map map, Issue issue) {
        return null;
    }

    @Override
    public String getHiddenFieldId() {
        return null;
    }

    @Override
    public String prettyPrintChangeHistory(String s) {
        return null;
    }

    @Override
    public String prettyPrintChangeHistory(String s, I18nHelper i18nHelper) {
        return null;
    }

    @Override
    public String getCreateHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue) {
        return null;
    }

    @Override
    public String getCreateHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map map) {
        return null;
    }

    @Override
    public String getEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue) {
        return null;
    }

    @Override
    public String getEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map map) {
        return null;
    }

    @Override
    public String getBulkEditHtml(OperationContext operationContext, Action action, BulkEditBean bulkEditBean, Map map) {
        return null;
    }

    @Override
    public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue) {
        return null;
    }

    @Override
    public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Map map) {
        return null;
    }

    @Override
    public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Object o, Map map) {
        return null;
    }

    @Override
    public boolean isShown(Issue issue) {
        return false;
    }

    @Override
    public void populateDefaults(Map<String, Object> map, Issue issue) {

    }

    @Override
    public boolean hasParam(Map<String, String[]> map) {
        return false;
    }

    @Override
    public void populateFromParams(Map<String, Object> map, Map<String, String[]> map1) {

    }

    @Override
    public void populateFromIssue(Map<String, Object> map, Issue issue) {

    }

    @Override
    public void validateParams(OperationContext operationContext, ErrorCollection errorCollection, I18nHelper i18nHelper, Issue issue, FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem) {

    }

    @Nullable
    @Override
    public Object getDefaultValue(Issue issue) {
        return null;
    }

    @Override
    public void createValue(Issue issue, Object o) {

    }

    @Override
    public void updateValue(FieldLayoutItem fieldLayoutItem, Issue issue, ModifiedValue modifiedValue, IssueChangeHolder issueChangeHolder) {

    }

    @Override
    public void updateIssue(FieldLayoutItem fieldLayoutItem, MutableIssue mutableIssue, Map map) {

    }

    @Override
    public void removeValueFromIssueObject(MutableIssue mutableIssue) {

    }

    @Override
    public boolean canRemoveValueFromIssueObject(Issue issue) {
        return false;
    }

    @Override
    public MessagedResult needsMove(Collection collection, Issue issue, FieldLayoutItem fieldLayoutItem) {
        return null;
    }

    @Override
    public void populateForMove(Map<String, Object> map, Issue issue, Issue issue1) {

    }

    @Override
    public boolean hasValue(Issue issue) {
        return false;
    }

    @Override
    public String availableForBulkEdit(BulkEditBean bulkEditBean) {
        return null;
    }

    @Override
    public Object getValueFromParams(Map map) throws FieldValidationException {
        return null;
    }

    @Override
    public void populateParamsFromString(Map<String, Object> map, String s, Issue issue) throws FieldValidationException {

    }

    @Override
    public SearchHandler createAssociatedSearchHandler() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getNameKey() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValueFromIssue(Issue issue) {
        return null;
    }

    @Override
    public boolean isRenderable() {
        return false;
    }

    @Override
    public FieldTypeInfo getFieldTypeInfo(FieldTypeInfoContext fieldTypeInfoContext) {
        return null;
    }

    @Override
    public JsonType getJsonSchema() {
        return null;
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(Issue issue, boolean b, @Nullable FieldLayoutItem fieldLayoutItem) {
        return null;
    }

    @Override
    public RestFieldOperationsHandler getRestFieldOperation() {
        return null;
    }

    @Override
    public JsonData getJsonDefaultValue(IssueContext issueContext) {
        return null;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Nonnull
    @Override
    public List<IssueType> getAssociatedIssueTypes() {
        return null;
    }

    @Override
    public List<IssueType> getAssociatedIssueTypeObjects() {
        return null;
    }
}
