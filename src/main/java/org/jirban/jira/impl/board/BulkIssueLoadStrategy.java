package org.jirban.jira.impl.board;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jirban.jira.impl.config.CustomFieldConfig;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.osgi.framework.BundleReference;

/**
 * <p>Bulk loads up things like custom fields using a direct sql query.</p>
 * <p>Normally if using the Jira provided classes, this is done lazily for each issue, and is
 * fine when we are handling events to create or update entities.</p>
 * <p>But is not suitable for loading the full board, since the lazy loading results in an extra sql
 * query behind the scenes for every single custom field, for every single issue. So, when loading the full board
 * we instead do a bulk load to avoid this performance overhead.</p>
 * <p>For unit tests we currently use the lazy loading mechanism to load the custom fields, this is mainly
 * to avoid having to set up the mocks at present.</p>
 *
 * @author Kabir Khan
 */
class BulkIssueLoadStrategy implements IssueLoadStrategy {

    //The size of the batch of issues to do a bulk load for
    private static final int BATCH_SIZE = 100;

    private final String dataSourceName = "defaultDS";
    private final BoardProject.Builder project;
    private final Map<Long, CustomFieldConfig> customFields = new HashMap<>();
    private final List<Long> ids = new ArrayList<>();
    private final Map<Long, String> issues = new HashMap<>();
    private final Map<Long, Issue.Builder> builders = new HashMap<>();
    private boolean finished = false;

    public BulkIssueLoadStrategy(BoardProject.Builder project) {
        this.project = project;
        for (String cfName : project.getConfig().getCustomFieldNames()) {
            CustomFieldConfig customFieldConfig =
                    project.getBoard().getConfig().getCustomFieldConfigForJirbanName(cfName);
            customFields.put(customFieldConfig.getId(), customFieldConfig);
        }
    }

    static BulkIssueLoadStrategy create(BoardProject.Builder project) {
        if (project.getConfig().getCustomFieldNames().size() == 0) {
            //There are no custom fields so we are not needed
            return null;
        }
        final ClassLoader cl = RawSqlLoader.class.getClassLoader();
        if (cl instanceof BundleReference) {
            return new BulkIssueLoadStrategy(project);
        }
        //We are running in a unit test, so we don't use this strategy (see class javadoc)
        return null;

    }

    @Override
    public void handle(com.atlassian.jira.issue.Issue issue, Issue.Builder builder) {
        ids.add(issue.getId());
        issues.put(issue.getId(), issue.getKey());
        builders.put(issue.getId(), builder);
    }

    @Override
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        final SQLProcessor sqlProcessor = new SQLProcessor(dataSourceName);
        try {
            bulkLoadData(sqlProcessor);
        } finally {
            try {
                sqlProcessor.close();
            } catch (Exception ignore) {

            }
        }
    }

    private void bulkLoadData(SQLProcessor sqlProcessor) {
        final List<Long> idBatch = new ArrayList<>();
        for (int i = 0 ; i < ids.size() ; i++) {
            idBatch.add(ids.get(i));
            if (idBatch.size() == BATCH_SIZE) {
                loadDataForBatch(sqlProcessor, customFields, idBatch);
                idBatch.clear();
            }
        }
        if (idBatch.size() > 0) {
            loadDataForBatch(sqlProcessor, customFields, idBatch);
        }
    }

    private void loadDataForBatch(SQLProcessor sqlProcessor,
                                  Map<Long, CustomFieldConfig> customFields, List<Long> idBatch) {
        final String sql = createSql(customFields, idBatch);

        try (final ResultSet rs = sqlProcessor.executeQuery(sql)){
            while (rs.next()) {
                Long issueId = rs.getLong(1);
                Long customFieldId = rs.getLong(2);
                String stringValue = rs.getString(3);
                Long numValue = rs.getLong(4);
                if (rs.wasNull()) {
                    numValue = null;
                }

                processCustomFieldValue(customFields, issueId, customFieldId, stringValue, numValue);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processCustomFieldValue(Map<Long, CustomFieldConfig> customFields, Long issueId, Long customFieldId, String stringValue, Long numValue) {
        Issue.Builder builder = builders.get(issueId);
        CustomFieldConfig customFieldConfig = customFields.get(customFieldId);

        System.out.println("Loaded for " + builder.getIssueKey() + " " + stringValue + " " + numValue);

        //TODO load the data
        //How we do this depends on the custom field type (i.e. use the util)
        //For the user fields, we should check the existing users in the board, and use that first. If it does not exist, we need to load it
        //For the version fields, we need a cache of id->string. Check existing versions in the board first, if it does not exist we need to load it.
        //CustomFieldValue value = customFieldConfig.getUtil().loadBulkLoadedValue(project, customFieldConfig, stringValue, numValue);
    }

    private String createSql(Map<Long, CustomFieldConfig> customFields, List<Long> idBatch) {
        StringBuilder sb = new StringBuilder()
                .append("select j.id, cv.customfield, cv.stringvalue, cv.numbervalue ")
                .append("from project p, jiraissue j, customfieldvalue cv ")
                .append("where ")
                .append("p.id=j.project and j.id=cv.issue and ")
                .append("p.pkey='" + project.getCode() + "' and ")
                .append("customfield in (");

        boolean first = true;
        for (Long cfId : customFields.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(cfId.toString());
        }
        sb.append(") and ");
        sb.append("j.id in (");
        first = true;
        for (Long issueId : ids) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(issueId.toString());
        }
        sb.append(")");

        return sb.toString();
    }

}