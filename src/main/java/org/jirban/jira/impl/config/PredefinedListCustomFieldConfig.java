package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.Constants.CONFIG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.board.CustomFieldUtil;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class PredefinedListCustomFieldConfig extends CustomFieldConfig {
    private final List<String> list;
    private final Map<String, Integer> indexByKey;

    private PredefinedListCustomFieldConfig(String name, Type type, CustomField customField, List<String> list, Map<String, Integer> indexByKey) {
        super(name, type, customField);
        this.list = list;
        this.indexByKey = indexByKey;
    }

    @Override
    public ModelNode serializeForConfig() {
        ModelNode config = super.serializeForConfig();
        ModelNode cfg = config.get(CONFIG);
        for (String entry : list) {
            cfg.add(entry);
        }
        return config;
    }

    @Override
    public CustomFieldUtil getUtil() {
        return CustomFieldUtil.PREDEFINED_LIST;
    }

    @Override
    public void sortCustomFields(Map<String, CustomField> customFields) {

    }

    static PredefinedListCustomFieldConfig load(String name, Type type, CustomField customField, ModelNode customFieldCfgNode) {
        if (!customFieldCfgNode.hasDefined(CONFIG)) {
            throw new JirbanValidationException("\"custom\" field configuration \"" + name + "\" does not have the required \"config\" array element");
        }
        final ModelNode cfgNode = customFieldCfgNode.get(CONFIG);
        if (cfgNode.getType() != ModelType.LIST) {
            throw new JirbanValidationException("The \"config\" element is not an array for \"custom\" field configuration \"" + name + "\"");
        }

        Map<String, Integer> indices = new HashMap<>();
        List<String> list = new ArrayList<>();
        int i = 0;
        for (ModelNode entry : cfgNode.asList()) {
            if (indices.put(entry.asString(), i++) != null) {
                throw new JirbanValidationException("List entry \"" + entry.asString() + "\" is not unique in custom field configuration \"" + name + "\"");
            }
            list.add(entry.asString());
        }
        return new PredefinedListCustomFieldConfig(name, type, customField,
                Collections.unmodifiableList(list), Collections.unmodifiableMap(indices));
    }
}
