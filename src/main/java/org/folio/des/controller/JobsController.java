package org.folio.des.controller;

import static java.util.Objects.isNull;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.des.domain.dto.ExportType.E_HOLDINGS;
import static org.hibernate.internal.util.StringHelper.isBlank;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.rest.resource.JobsApi;
import org.folio.des.service.JobService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
public class JobsController implements JobsApi {

  private final JobService service;

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    return ResponseEntity.ok(service.get(offset, limit, query));
  }

  @Override
  public ResponseEntity<Job> upsertJob(@RequestHeader("X-Okapi-Tenant") String tenantId, @Valid Job job) {
    job.setTenant(tenantId);
    if (isMissingRequiredParameters(job)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(service.upsertAndSendToKafka(job, true), job.getId() == null ? HttpStatus.CREATED : HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> downloadExportedFileByJobId(UUID id) {
    return ResponseEntity.ok(new InputStreamResource(service.downloadExportedFile(id)));
  }

  private boolean isMissingRequiredParameters(Job job) {
    var exportTypeParameters = job.getExportTypeSpecificParameters();
    var eHoldingsExportConfig = exportTypeParameters.geteHoldingsExportConfig();
    return (BULK_EDIT_QUERY == job.getType() && (isNull(job.getEntityType()) || isBlank(exportTypeParameters.getQuery()))) ||
      (BULK_EDIT_IDENTIFIERS == job.getType() && (isNull(job.getIdentifierType()) || isNull(job.getEntityType()))) ||
      (E_HOLDINGS == job.getType() && (isNull(eHoldingsExportConfig.getRecordId()) || isNull(eHoldingsExportConfig.getRecordType())));
  }

}
