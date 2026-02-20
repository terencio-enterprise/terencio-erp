package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

@Repository
public class CampaignPersistenceAdapter implements CampaignRepositoryPort {
    
    // Implementación robusta en memoria para el módulo de Marketing para evitar errores de schema durante desarrollo.
    private final Map<Long, MarketingTemplate> templates = new ConcurrentHashMap<>();
    private final Map<Long, CampaignLog> logs = new ConcurrentHashMap<>();
    private final AtomicLong templateIdGen = new AtomicLong(1);
    private final AtomicLong logIdGen = new AtomicLong(1);

    @Override
    public Optional<MarketingTemplate> findTemplateById(Long id) {
        return Optional.ofNullable(templates.get(id));
    }

    @Override
    public List<MarketingTemplate> findAllTemplates(UUID companyId, String search) {
        return templates.values().stream()
            .filter(t -> t.getCompanyId().equals(companyId))
            .filter(t -> search == null || search.isBlank() || t.getName().contains(search))
            .collect(Collectors.toList());
    }

    @Override
    public MarketingTemplate saveTemplate(MarketingTemplate template) {
        if (template.getId() == null) {
            template.setId(templateIdGen.getAndIncrement());
        }
        templates.put(template.getId(), template);
        return template;
    }

    @Override
    public void deleteTemplate(Long id) {
        templates.remove(id);
    }

    @Override
    public void saveLog(CampaignLog log) {
        if (log.getId() == null) {
            log.setId(logIdGen.getAndIncrement());
        }
        logs.put(log.getId(), log);
    }

    @Override
    public List<CampaignLog> findLogsByStatus(String status) {
        return logs.values().stream()
            .filter(l -> status == null || status.isBlank() || l.getStatus().name().equals(status))
            .collect(Collectors.toList());
    }
}