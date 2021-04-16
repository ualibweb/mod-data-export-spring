package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.des.config.KafkaConfiguration;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobUpdatesService implements AcknowledgingMessageListener<String, Job> {

  private static final Map<BatchStatus, JobStatus> JOB_STATUSES = new EnumMap<>(BatchStatus.class);

  static {
    JOB_STATUSES.put(BatchStatus.COMPLETED, JobStatus.SUCCESSFUL);
    JOB_STATUSES.put(BatchStatus.STARTING, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STARTED, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STOPPING, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STOPPED, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.FAILED, JobStatus.FAILED);
    JOB_STATUSES.put(BatchStatus.ABANDONED, JobStatus.FAILED);
    JOB_STATUSES.put(BatchStatus.UNKNOWN, null);
  }

  private final JobRepository repository;
  private final KafkaConfiguration kafka;

  @EventListener(ContextRefreshedEvent.class)
  public void onContextRefreshed() {
    kafka.startListener(KafkaConfiguration.Topic.JOB_UPDATE, this);
  }

  @Transactional
  @Override
  public void onMessage(ConsumerRecord<String, Job> data, Acknowledgment acknowledgment) {
    Job jobExecutionUpdate = data.value();
    log.info("Received {}.", jobExecutionUpdate);

    if (acknowledgment != null) {
      acknowledgment.acknowledge();
    }

    Optional<Job> jobOptional = repository.findById(jobExecutionUpdate.getId());
    if (jobOptional.isEmpty()) {
      log.error("Update for unknown job {}.", jobExecutionUpdate.getId());
      return;
    }
    Job job = jobOptional.get();

    if (updateJobPropsIfChanged(jobExecutionUpdate, job)) {
      job.setUpdatedDate(new Date());
      log.info("Updating {}.", job);
      job = repository.save(job);
      log.info("Updated {}.", job);
    }
  }

  private boolean updateJobPropsIfChanged(Job jobExecutionUpdate, Job job) {
    boolean result = false;
    if (jobExecutionUpdate.getDescription() != null && !jobExecutionUpdate.getDescription().equals(job.getDescription())) {
      job.setDescription(jobExecutionUpdate.getDescription());
      result = true;
    }
    if (jobExecutionUpdate.getFiles() != null && (job.getFiles() == null || !CollectionUtils.isEqualCollection(
        jobExecutionUpdate.getFiles(), job.getFiles()))) {
      job.setFiles(jobExecutionUpdate.getFiles());
      result = true;
    }
    if (jobExecutionUpdate.getStartTime() != null && !jobExecutionUpdate.getStartTime().equals(job.getStartTime())) {
      job.setStartTime(jobExecutionUpdate.getStartTime());
      result = true;
    }
    if (jobExecutionUpdate.getEndTime() != null && !jobExecutionUpdate.getEndTime().equals(job.getEndTime())) {
      job.setEndTime(jobExecutionUpdate.getEndTime());
      result = true;
    }
    if (jobExecutionUpdate.getErrorDetails() != null && !jobExecutionUpdate.getErrorDetails().equals(job.getErrorDetails())) {
      job.setErrorDetails(jobExecutionUpdate.getErrorDetails());
      result = true;
    }
    if (jobExecutionUpdate.getBatchStatus() != null && jobExecutionUpdate.getBatchStatus() != job.getBatchStatus()) {
      job.setBatchStatus(jobExecutionUpdate.getBatchStatus());
      result = true;

      JobStatus jobStatus = JOB_STATUSES.get(jobExecutionUpdate.getBatchStatus());
      if (jobStatus != null) {
        job.setStatus(jobStatus);
      }
    }
    if (jobExecutionUpdate.getExitStatus() != null && !jobExecutionUpdate.getExitStatus().equals(job.getExitStatus())) {
      job.setExitStatus(jobExecutionUpdate.getExitStatus());
      result = true;
    }
    return result;
  }

}
