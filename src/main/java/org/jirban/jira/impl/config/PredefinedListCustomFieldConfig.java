package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.Constants.CONFIG;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jirban.jira.JirbanValidationException;

/**
 * @author Kabir Khan
 */
public class PredefinedListCustomFieldConfig extends CustomFieldConfig {
    private final List<String> list;
    private PredefinedListCustomFieldConfig(String name, Type type, long fieldId, List<String> list) {
        super(name, type, fieldId);
        this.list = list;
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

    static PredefinedListCustomFieldConfig load(String name, Type type, long fieldId, ModelNode customFieldCfgNode) {
        if (!customFieldCfgNode.hasDefined(CONFIG)) {
            throw new JirbanValidationException("\"custom\" field configuration \"" + name + "\" does not have the required \"config\" array element");
        }
        final ModelNode cfgNode = customFieldCfgNode.get(CONFIG);
        if (cfgNode.getType() != ModelType.LIST) {
            throw new JirbanValidationException("The \"config\" element is not an array for \"custom\" field configuration \"" + name + "\"");
        }

        List<String> list = new ArrayList<>();
        for (ModelNode entry : cfgNode.asList()) {
            list.add(entry.asString());
        }
        return new PredefinedListCustomFieldConfig(name, type, fieldId, list);
    }
}
