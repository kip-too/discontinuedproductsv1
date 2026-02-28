package com.northwind.discontinuedproducts.controller;

import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch")
public class JobController {
    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job discontinuedProductJob;

    @GetMapping("/run")
    public String runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addString("runAt",LocalDateTime.now().toString()) 
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(discontinuedProductJob, params);
     
        return "Job Status: " + jobExecution.getStatus() + " |Exit: " + jobExecution.getExitStatus().getExitCode();
    }   
}
