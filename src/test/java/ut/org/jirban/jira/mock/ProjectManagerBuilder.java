package ut.org.jirban.jira.mock;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.project.ProjectManager;

/**
 * @author Kabir Khan
 */
public class ProjectManagerBuilder {
    private final ProjectManager projectManager = mock(ProjectManager.class);

    private ProjectManagerBuilder() {
    }

    public static ProjectManager getAnyProjectManager() {
        ProjectManagerBuilder builder = new ProjectManagerBuilder();
        return builder.buildAnyProject();
    }

    private ProjectManager buildAnyProject() {
        when(projectManager.getProjectByCurrentKey(anyString())).then(invocation -> {
            String key = (String)invocation.getArguments()[0];
            return new MockProject(key);
        });
        return projectManager;
    }
}
