package com.example.mock.presentation.controller;

import com.example.mock.application.port.in.ProcessTaskUseCase;
import com.example.mock.presentation.assembler.TaskResourceAssembler;
import com.example.mock.presentation.resource.in.ProcessTaskResource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mock/business")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class BusinessController {

    private final ProcessTaskUseCase processTaskUseCase;
    private final TaskResourceAssembler assembler;

    public BusinessController(ProcessTaskUseCase processTaskUseCase, TaskResourceAssembler assembler) {
        this.processTaskUseCase = processTaskUseCase;
        this.assembler = assembler;
    }

    @PostMapping("/process")
    public String process(@RequestBody ProcessTaskResource resource) {
        var command = assembler.toCommand(resource);
        
        // Asynchronous processing triggers the background task
        // We return "SUCCESS" immediately to the client to emulate asynchronous processing success
        processTaskUseCase.processTask(command);
        
        return "SUCCESS";
    }
}
