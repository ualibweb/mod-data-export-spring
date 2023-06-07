package org.folio.des.service.bursarlegacy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.de.entity.bursarlegacy.LegacyJob;
import org.folio.des.domain.dto.LegacyJobCollection;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class BursarExportLegacyJobServiceTest {

  @Mock
  private BursarExportLegacyJobRepository repository;

  @Mock
  private CQLService cqlService;

  @Test
  void testGetBlankQuery() {
    List<LegacyJob> legacyJobs = new ArrayList<>();
    LegacyJob legacyJob = new LegacyJob();
    legacyJob.setId(UUID.fromString("0000-00-00-00-000000"));
    legacyJobs.add(legacyJob);

    Page<LegacyJob> page = new PageImpl<LegacyJob>(
      legacyJobs,
      new OffsetRequest(0, 1),
      1L
    );

    when(repository.findAll(new OffsetRequest(0, 1))).thenReturn(page);
    BursarExportLegacyJobService service = new BursarExportLegacyJobService(
      repository,
      cqlService
    );

    LegacyJobCollection legacyJobCollection = service.get(0, 1, "");

    assertEquals(1, legacyJobCollection.getJobRecords().size());
    assertEquals(
      UUID.fromString("0000-00-00-00-000000"),
      legacyJobCollection.getJobRecords().get(0).getId()
    );
  }
}