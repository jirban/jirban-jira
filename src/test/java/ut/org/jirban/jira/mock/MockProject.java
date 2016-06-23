package ut.org.jirban.jira.mock;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class MockProject implements Project {
    private final String key;

    public MockProject(String key) {
        this.key = key;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public User getLead() {
        return null;
    }

    @Override
    public User getLeadUser() {
        return null;
    }

    @Override
    public String getLeadUserName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Long getAssigneeType() {
        return null;
    }

    @Override
    public Long getCounter() {
        return null;
    }

    @Override
    public Collection<GenericValue> getComponents() {
        return null;
    }

    @Override
    public Collection<ProjectComponent> getProjectComponents() {
        return null;
    }

    @Override
    public Collection<Version> getVersions() {
        return null;
    }

    @Override
    public Collection<IssueType> getIssueTypes() {
        return null;
    }

    @Override
    public GenericValue getProjectCategory() {
        return null;
    }

    @Override
    public ProjectCategory getProjectCategoryObject() {
        return null;
    }

    @Override
    public GenericValue getGenericValue() {
        return null;
    }

    @Nonnull
    @Override
    public Avatar getAvatar() {
        return null;
    }

    @Override
    public ApplicationUser getProjectLead() {
        return null;
    }

    @Override
    public String getLeadUserKey() {
        return null;
    }

    @Override
    public String getOriginalKey() {
        return null;
    }
}
