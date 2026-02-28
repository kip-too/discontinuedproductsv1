package com.northwind.discontinuedproducts.listener;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

//log summarries , dend notis & clean up resources after job completion
@Component
public class JobCompletionListener implements JobExecutionListener {
    @Override
    public void beforeJob(org.springframework.batch.core.JobExecution jobExecution) {
        System.out.println("=========================");
        System.out.println("BATCH JOB STARTING...");
        System.out.println(" Job: " + jobExecution.getJobInstance().getJobName());
        System.out.println("=========================");
    }

    @Override
    public void afterJob(org.springframework.batch.core.JobExecution jobExecution) {
        System.out.println("=========================");
        System.out.println("BATCH JOB COMPLETED.");
        System.out.println(" Job: " + jobExecution.getJobInstance().getJobName());
        System.out.println(" Status: " + jobExecution.getStatus());
        System.out.println(" start : " + jobExecution.getStartTime());
        System.out.println(" end : " + jobExecution.getEndTime());
        long itemsRead = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum();
        long itemsWritten = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())            .sum();
        long itemsSkipped = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getSkipCount())
                .sum(); 
        System.out.println(" Items Read: " + itemsRead + " Products");
        System.out.println(" Items Written: " + itemsWritten + " records");
        System.out.println(" Items Skipped: " + itemsSkipped + " records");
        System.out.println("=========================");
    }
    
}
