package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.de.entity.Job;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.domain.dto.AuthorityControlExportConfig;
import org.folio.des.domain.dto.EHoldingsExportConfig;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {JacksonConfiguration.class, ServiceConfiguration.class})
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
class JobCommandBuilderResolverTest {

  @Autowired
  private JobCommandBuilderResolver resolver;
  @MockBean
  private ConfigurationClient client;

  @ParameterizedTest
  @DisplayName("Should retrieve builder for specific export type if builder is registered in the resolver")
  @CsvSource({
    "BURSAR_FEES_FINES, BurSarFeeFinesJobCommandBuilder",
    "CIRCULATION_LOG, CirculationLogJobCommandBuilder",
    "BULK_EDIT_QUERY, BulkEditQueryJobCommandBuilder",
    "EDIFACT_ORDERS_EXPORT, EdifactOrdersJobCommandBuilder",
    "E_HOLDINGS, EHoldingsJobCommandBuilder",
    "AUTH_HEADINGS_UPDATES, AuthorityControlJobCommandBuilder"
  })
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver(ExportType exportType,
              String expBuilderClass) {
    Optional<JobCommandBuilder> builder = resolver.resolve(exportType);
    assertEquals(expBuilderClass, builder.get().getClass().getSimpleName());
  }

  @Test
  void shouldNotRetrieveBuilderForFailedLinkedBibUpdates() {
    Optional<JobCommandBuilder> builder = resolver.resolve(ExportType.FAILED_LINKED_BIB_UPDATES);
    assertTrue(builder.isEmpty());
  }

  @Test
  @DisplayName("Should not retrieve builder for specific export type if builder is not registered in the resolver")
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver() {
    Optional<JobCommandBuilder> builder = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    assertTrue(builder.isPresent());
  }

  @ParameterizedTest
  @DisplayName("Should be create jobParameters")
  @CsvSource({
    "BURSAR_FEES_FINES, bursarFeeFines",
    "CIRCULATION_LOG, query",
    "BULK_EDIT_QUERY, query",
    "EDIFACT_ORDERS_EXPORT, edifactOrdersExport",
    "E_HOLDINGS, eHoldingsExportConfig",
    "AUTH_HEADINGS_UPDATES, authorityControlExportConfig"
  })
  void shouldBeCreateJobParameters(ExportType exportType, String paramsKey) {
    Optional<JobCommandBuilder> builder = resolver.resolve(exportType);
    Job job = new Job();
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EHoldingsExportConfig eHoldingsExportConfig = new EHoldingsExportConfig();
    // BursarFeeFines bursarFeeFines = new BursarFeeFines();
    AuthorityControlExportConfig authorityControlExportConfig = new AuthorityControlExportConfig();

    // bursarFeeFines.setDaysOutstanding(1);
    // bursarFeeFines.addPatronGroupsItem("Test");

    vendorEdiOrdersExportConfig.vendorId(UUID.randomUUID());
    vendorEdiOrdersExportConfig.setConfigName("TestConfig");

    eHoldingsExportConfig.setRecordId("packageId");
    eHoldingsExportConfig.setRecordType(EHoldingsExportConfig.RecordTypeEnum.PACKAGE);
    eHoldingsExportConfig.setTitleSearchFilters("titleFilters");
    eHoldingsExportConfig.setPackageFields(List.of("packageField"));
    eHoldingsExportConfig.setTitleFields(List.of("titleField"));

    authorityControlExportConfig.setFromDate(LocalDate.now());
    authorityControlExportConfig.toDate(LocalDate.now());

    exportTypeSpecificParameters.setQuery("TestQuery");
    // exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportTypeSpecificParameters.seteHoldingsExportConfig(eHoldingsExportConfig);
    exportTypeSpecificParameters.setAuthorityControlExportConfig(authorityControlExportConfig);

    job.setEntityType(EntityType.USER);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    JobParameters jobParameters = builder.get().buildJobCommand(job);

    assertNotEquals("null", jobParameters.getParameters().get(paramsKey).getValue());
  }
}
