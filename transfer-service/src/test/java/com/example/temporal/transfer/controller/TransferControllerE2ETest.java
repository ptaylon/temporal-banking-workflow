package com.example.temporal.transfer.controller;

import com.example.temporal.common.dto.TransferControlAction;
import com.example.temporal.common.dto.TransferControlRequest;
import com.example.temporal.common.dto.TransferControlResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.transfer.TransferServiceApplication;
import com.example.temporal.transfer.service.FeatureFlagService;
import com.example.temporal.transfer.service.TransferControlService;
import com.example.temporal.transfer.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TransferServiceApplication.class)
@AutoConfigureMockMvc
class TransferControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureFlagService featureFlagService;

    @MockBean
    private TransferControlService transferControlService;

    @MockBean
    private TransferService transferService;

    private TransferControlStatus buildStatus(boolean paused, boolean cancelled, TransferControlAction action) {
        TransferControlStatus status = new TransferControlStatus();
        status.setPaused(paused);
        status.setCancelled(cancelled);
        status.setLastControlAction(action);
        status.setLastControlTimestamp(LocalDateTime.now());
        status.setWorkflowStatus("RUNNING");
        return status;
    }

    @Test
    @DisplayName("Pause - success")
    void pauseTransfer_success() throws Exception {
        String workflowId = "transfer-100";
        given(featureFlagService.isControlEnabled()).willReturn(true);
        TransferControlStatus status = buildStatus(true, false, TransferControlAction.PAUSE);
        given(transferControlService.pauseTransfer(eq(workflowId)))
                .willReturn(TransferControlResponse.success(workflowId, status, "Transfer paused successfully"));

        mockMvc.perform(post("/api/transfers/{workflowId}/pause", workflowId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.workflowId", is(workflowId)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status.paused", is(true)))
                .andExpect(jsonPath("$.message", containsString("paused")));
    }

    @Test
    @DisplayName("Pause - feature disabled")
    void pauseTransfer_featureDisabled() throws Exception {
        String workflowId = "transfer-101";
        given(featureFlagService.isControlEnabled()).willReturn(false);

        mockMvc.perform(post("/api/transfers/{workflowId}/pause", workflowId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("temporarily disabled")))
                .andExpect(jsonPath("$.workflowId", is(workflowId)));
    }

    @Test
    @DisplayName("Resume - success")
    void resumeTransfer_success() throws Exception {
        String workflowId = "transfer-200";
        given(featureFlagService.isControlEnabled()).willReturn(true);
        TransferControlStatus status = buildStatus(false, false, TransferControlAction.RESUME);
        given(transferControlService.resumeTransfer(eq(workflowId)))
                .willReturn(TransferControlResponse.success(workflowId, status, "Transfer resumed successfully"));

        mockMvc.perform(post("/api/transfers/{workflowId}/resume", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status.paused", is(false)))
                .andExpect(jsonPath("$.message", containsString("resumed")));
    }

    @Test
    @DisplayName("Cancel - success with body")
    void cancelTransfer_success() throws Exception {
        String workflowId = "transfer-300";
        given(featureFlagService.isControlEnabled()).willReturn(true);
        TransferControlStatus status = buildStatus(false, true, TransferControlAction.CANCEL);
        status.setCancelReason("User");
        given(transferControlService.cancelTransfer(eq(workflowId), eq("User")))
                .willReturn(TransferControlResponse.success(workflowId, status, "Transfer cancelled successfully"));

        String body = "{\"action\":\"CANCEL\",\"reason\":\"User\"}";
        mockMvc.perform(post("/api/transfers/{workflowId}/cancel", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status.cancelReason", is("User")));
    }

    @DisplayName("Cancel - feature disabled")
    void cancelTransfer_featureDisabled() throws Exception {
        String workflowId = "transfer-301";
        given(featureFlagService.isControlEnabled()).willReturn(false);

        String body = "{\"action\":\"CANCEL\",\"reason\":\"User\"}";
        mockMvc.perform(post("/api/transfers/{workflowId}/cancel", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("temporarily disabled")));
    }

    @Test
    @DisplayName("Control status - success")
    void getControlStatus_success() throws Exception {
        String workflowId = "transfer-400";
        given(featureFlagService.isControlEnabled()).willReturn(true); // not required for GET, but harmless
        TransferControlStatus status = buildStatus(true, false, TransferControlAction.PAUSE);
        given(transferControlService.getControlStatus(eq(workflowId)))
                .willReturn(status);

        mockMvc.perform(get("/api/transfers/{workflowId}/control-status", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paused", is(true)))
                .andExpect(jsonPath("$.workflowStatus", is("RUNNING")));
    }
}
