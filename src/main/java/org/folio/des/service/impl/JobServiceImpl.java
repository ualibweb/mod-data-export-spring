package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.*;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobRepository;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private final JobExecutionService jobExecutionService;
  private final JobRepository repository;

  @Override
  public org.folio.des.domain.dto.Job get(UUID id) {
    Optional<Job> jobDtoOptional = repository.findById(id);
    if (jobDtoOptional.isEmpty()) {
      throw new NotFoundException(String.format("Job %s not found", id));
    }
    return entityToDto(jobDtoOptional.get());
  }

  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    List<org.folio.des.domain.dto.Job> jobDtos = repository.findAll(new OffsetRequest(offset, limit))
        .map(JobServiceImpl::entityToDto)
        .getContent();
    JobCollection result = new JobCollection();
    result.setJobRecords(jobDtos);
    result.setTotalRecords(jobDtos.size());
    return result;
  }

  @Override
  public org.folio.des.domain.dto.Job upsert(org.folio.des.domain.dto.Job jobDto) {
    log.info("Upserting {}.", jobDto);
    Job job = dtoToEntity(jobDto);

    if (StringUtils.isBlank(job.getName())) {
      job.setName("Job #TBD");
    }
    if (job.getIsSystemSource() == null) {
      job.setIsSystemSource(false);
    }
    if (job.getStatus() == null) {
      job.setStatus(JobStatus.SCHEDULED);
    }
    Date now = new Date();
    if (job.getCreatedDate() == null) {
      job.setCreatedDate(now);
    }
    job.setUpdatedDate(now);
    if (job.getBatchStatus() != null) {
      job.setBatchStatus(BatchStatus.UNKNOWN);
    }
    if (job.getExitStatus() != null) {
      job.setExitStatus(ExitStatus.UNKNOWN);
    }

    jobExecutionService.startJob(prepareStartJobCommand(job));

    Job result = repository.save(job);
    log.info("Upserted {}.", result);
    return entityToDto(result);
  }

  @Override
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  private StartJobCommandDto prepareStartJobCommand(Job job) {
    if (job.getType() == ExportType.BURSAR_FEES_FINES && job.getExportTypeSpecificParameters().getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
          String.format("Job of %s type should contain %s parameters", job.getType(), BursarFeeFines.class.getSimpleName()));
    }

    StartJobCommandDto result = new StartJobCommandDto();
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setType(job.getType());

    Map<String, JobParameterDto> params = new HashMap<>();
    if (job.getType() == ExportType.CIRCULATION_LOG) {
      params.put("query", new JobParameterDto(job.getExportTypeSpecificParameters().getQuery()));
    } else if (job.getType() == ExportType.BURSAR_FEES_FINES) {
      BursarFeeFines bursarFeeFines = job.getExportTypeSpecificParameters().getBursarFeeFines();
      params.put("daysOutstanding", new JobParameterDto((long) bursarFeeFines.getDaysOutstanding()));
      params.put("patronGroups", new JobParameterDto(String.join(",", bursarFeeFines.getPatronGroups())));
    }
    result.setJobInputParameters(params);

    return result;
  }

  public static org.folio.des.domain.dto.Job entityToDto(Job entity) {
    org.folio.des.domain.dto.Job result = new org.folio.des.domain.dto.Job();

    result.setId(entity.getId());
    result.setName(entity.getName());
    result.setDescription(entity.getDescription());
    result.setSource(entity.getSource());
    result.setIsSystemSource(entity.getIsSystemSource());
    result.setType(entity.getType());

    if (entity.getExportTypeSpecificParameters() != null) {
      ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();

      if (entity.getExportTypeSpecificParameters().getBursarFeeFines() != null) {
        BursarFeeFines bursarFeeFines = new BursarFeeFines();
        bursarFeeFines.setFtpUrl(entity.getExportTypeSpecificParameters().getBursarFeeFines().getFtpUrl());
        bursarFeeFines.setDaysOutstanding(entity.getExportTypeSpecificParameters().getBursarFeeFines().getDaysOutstanding());
        bursarFeeFines.setPatronGroups(entity.getExportTypeSpecificParameters().getBursarFeeFines().getPatronGroups());
        exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
      }

      exportTypeSpecificParameters.setQuery(entity.getExportTypeSpecificParameters().getQuery());
      result.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    }

    result.setStatus(entity.getStatus());
    result.setFiles(entity.getFiles());
    result.setStartTime(entity.getStartTime());
    result.setEndTime(entity.getEndTime());

    Metadata metadata = new Metadata();
    metadata.setCreatedDate(entity.getCreatedDate());
    metadata.setCreatedByUserId(entity.getCreatedByUserId());
    metadata.setCreatedByUsername(entity.getCreatedByUsername());
    metadata.setUpdatedDate(entity.getUpdatedDate());
    metadata.setUpdatedByUserId(entity.getUpdatedByUserId());
    metadata.setUpdatedByUsername(entity.getUpdatedByUsername());
    result.setMetadata(metadata);

    result.setOutputFormat(entity.getOutputFormat());
    result.setErrorDetails(entity.getErrorDetails());

    return result;
  }

  public static Job dtoToEntity(org.folio.des.domain.dto.Job dto) {
    Job result = new Job();

    result.setId(dto.getId());
    result.setName(dto.getName());
    result.setDescription(dto.getDescription());
    result.setSource(dto.getSource());
    result.setIsSystemSource(dto.getIsSystemSource());
    result.setType(dto.getType());

    if (dto.getExportTypeSpecificParameters() != null) {
      ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();

      if (dto.getExportTypeSpecificParameters().getBursarFeeFines() != null) {
        BursarFeeFines bursarFeeFines = new BursarFeeFines();
        bursarFeeFines.setFtpUrl(dto.getExportTypeSpecificParameters().getBursarFeeFines().getFtpUrl());
        bursarFeeFines.setDaysOutstanding(dto.getExportTypeSpecificParameters().getBursarFeeFines().getDaysOutstanding());
        bursarFeeFines.setPatronGroups(dto.getExportTypeSpecificParameters().getBursarFeeFines().getPatronGroups());
        exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
      }

      exportTypeSpecificParameters.setQuery(dto.getExportTypeSpecificParameters().getQuery());
      result.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    }

    result.setStatus(dto.getStatus());
    result.setFiles(dto.getFiles());
    result.setStartTime(dto.getStartTime());
    result.setEndTime(dto.getEndTime());

    if (dto.getMetadata() != null) {
      result.setCreatedDate(dto.getMetadata().getCreatedDate());
      result.setCreatedByUserId(dto.getMetadata().getCreatedByUserId());
      result.setCreatedByUsername(dto.getMetadata().getCreatedByUsername());
      result.setUpdatedDate(dto.getMetadata().getUpdatedDate());
      result.setUpdatedByUserId(dto.getMetadata().getUpdatedByUserId());
      result.setUpdatedByUsername(dto.getMetadata().getUpdatedByUsername());
    }

    result.setOutputFormat(dto.getOutputFormat());
    result.setErrorDetails(dto.getErrorDetails());

    return result;
  }

}