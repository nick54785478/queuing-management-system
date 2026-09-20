package com.example.qms.presentation.assembler;

import com.example.qms.application.dto.QueueConfigGottenResult;
import com.example.qms.presentation.resource.out.QueueConfigRetrievedResource;
import org.springframework.stereotype.Component;

@Component
public class QueueConfigResourceAssembler {

    public QueueConfigRetrievedResource toResource(QueueConfigGottenResult result) {
        if (result == null) {
            return null;
        }
        return new QueueConfigRetrievedResource(
                result.tenantId(),
                result.activityId(),
                result.dequeueBatchSize(),
                result.mode() != null ? result.mode().name() : "AUTOMATIC",
                result.resetDaily()
        );
    }
}
