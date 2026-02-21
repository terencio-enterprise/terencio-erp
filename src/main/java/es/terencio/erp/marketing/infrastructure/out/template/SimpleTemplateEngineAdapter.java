package es.terencio.erp.marketing.infrastructure.out.template;

import java.util.Map;

import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;

public class SimpleTemplateEngineAdapter implements TemplateEnginePort {
    @Override
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) return template;
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}