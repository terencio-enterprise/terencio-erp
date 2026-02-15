package es.terencio.erp;

import org.junit.jupiter.api.Test;

/**
 * Basic application context test.
 * Extends AbstractIntegrationTest to use the shared PostgreSQL container.
 */
class ErpApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // This test ensures the Spring Context loads correctly
        // using the shared container defined in AbstractIntegrationTest
    }

}
