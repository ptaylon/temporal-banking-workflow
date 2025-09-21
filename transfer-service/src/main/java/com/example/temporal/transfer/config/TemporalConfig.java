package com.example.temporal.transfer.config;

import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import com.example.temporal.transfer.activity.MoneyTransferActivitiesImpl;
import com.example.temporal.transfer.workflow.MoneyTransferWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    @Bean(destroyMethod = "shutdown")
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker worker(WorkerFactory factory, MoneyTransferActivitiesImpl activities) {
        Worker worker = factory.newWorker(MoneyTransferWorkflow.QUEUE_NAME,
                io.temporal.worker.WorkerOptions.newBuilder()
                        .setMaxConcurrentWorkflowTaskExecutions(1)
                        .setMaxConcurrentActivityExecutions(10)
                        .setWorkflowTaskTimeout(java.time.Duration.ofMinutes(1))
                        .build());
        worker.registerWorkflowImplementationTypes(MoneyTransferWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        factory.start();
        return worker;
    }
}