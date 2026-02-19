package es.terencio.erp.organization.domain.model;

public enum FiscalRegime {
    COMMON, // Régimen general
    SII, // Suministro Inmediato de Información (facturación inmediata AEAT)
    CANARIAS_IGIC, // Islas Canarias (IGIC en lugar de IVA)
    RECARGO // Recargo de Equivalencia
}
