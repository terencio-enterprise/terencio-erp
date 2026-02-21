package es.terencio.erp.marketing.application.port.out;

import java.util.Map;

public interface TemplateEnginePort {
    String render(String template, Map<String, String> variables);
}